/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the Kowari Metadata Store.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.content.n3;

// Java 2 standard packages
import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

// Third party packages
import antlr.collections.AST;        // ANTLR compiler-compiler
import com.hp.hpl.jena.n3.N3Parser;  // Jena
import com.hp.hpl.jena.n3.N3ParserEventHandler;
import org.apache.log4j.Logger;      // Apache Log4J
import org.jrdf.graph.BlankNode;     // JRDF
import org.jrdf.graph.Node;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.jrdf.vocabulary.OWL;
import org.jrdf.vocabulary.RDF;

// Locally written packages
import org.mulgara.content.Content;
import org.mulgara.content.NotModifiedException;
import org.mulgara.parser.MulgaraParserException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.rdf.BlankNodeImpl;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.TripleImpl;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.util.IntFile;
import org.mulgara.util.NumberUtil;
import org.mulgara.util.StringToLongMap;
import org.mulgara.util.TempDir;

/**
 * This class parses N3 data. It is implemented as a {@link Runnable} to allow it to be running in
 * the background filling a queue, while a consumer thread drains the queue.
 *
 * <p>Because ResolverSession (and the underlying StringPoolSession) may not be accessed
 * concurrently from multiple threads, there is some extra complication when creating blank nodes,
 * whereby blank-node instances are created in the parser thread but their id's are allocated later
 * in the app-thread.
 *
 * @created 2004-04-02
 * @author <a href="http://staff.pisoftware.com/anewman">Andrew Newman</a>
 * @author <a href="http://staff.pisoftware.com/davidm">David Makepeace</a>
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:02 $ @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class Parser extends Thread implements N3ParserEventHandler {
  /** Logger. */
  private static final Logger logger = Logger.getLogger(Parser.class.getName());

  private static final String ANON_TAG = "_:";

  private static final String LOCAL_ANON_TAG = ANON_TAG + "node";

  /**
   * Maximum size that the {@link #triples} buffer can attain without the
   * parser deliberately blocking and waiting for it to drain.
   */
  private final long MAX_TRIPLES = 1000;

  /** Mapping between parsed blank node IDs and local node numbers. */
  private IntFile blankNodeIdMap;

  /** Mapping between blank node rdf:nodeIDs and local node numbers. */
  private StringToLongMap blankNodeNameMap;

  /** Mapping between blank node IDs and blank-node instances. */
  private Map<Long, BlankNodeImpl> unallocBlankNodeIdMap = new HashMap<Long, BlankNodeImpl>();

  /** Mapping between blank node rdf:nodeIDs and blank-node instances. */
  private Map<String, BlankNodeImpl> unallocBlankNodeNameMap = new HashMap<String, BlankNodeImpl>();

  /** The resolverSession to create new internal identifiers for blank nodes. */
  private ResolverSession resolverSession;

  /** The stream containing the data to be parsed. */
  private InputStream inputStream;

  /** The queue of triples generated by the Notation-3 parser. */
  private LinkedList<Triple> triples = new LinkedList<Triple>();

  /**
   * The number of statements parsed so far.
   *
   * When {@link #complete} is <code>true</code>, this will be the number of
   * statements in the Notation-3 document.
   */
  private long statementCount = 0;

  /**
   * <code>true</code> if statementCount is the count of the total number of statements in
   * the entire file because the parser has reached the end of the file without error.
   */
  private boolean statementCountIsTotal = false;

  /** Flag used to indicate that the end of the RDF/XML file has been reached. */
  private boolean complete = false;

  /** The exception which interrupted parsing, or <code>null</code> is parsing is successful. */
  private Throwable exception = null;

  /**
   * The base URI from which the {@link #inputStream} came and where any
   * relative URI references within the stream should be resolved to absolute
   * form.
   *
   * This field may be <code>null</code> if the origin of the stream is
   * unknown, although in that case all URI references within the stream must
   * be absolute.
   */
  private URI baseURI;

  /**
   * Map of <code>@prefix</code> directives.
   *
   * Keys are {@link String}s of the form <code>p3p:</code>.
   * Values are also {@link String}s, and of the form
   * <code>http://www.example.org/meeting_organization#</code>.
   */
  private final Map<String,String> prefixMap = new HashMap<String,String>();

  //
  // Constructor
  //

  /**
   * Sole constructor.
   */
  Parser(Content content, ResolverSession resolverSession) throws NotModifiedException, TuplesException {
    // Validate parameters
    if (content == null) throw new IllegalArgumentException("Null \"content\" parameter");
    if (resolverSession == null) throw new IllegalArgumentException("Null \"resolverSession\" parameter");

    // Initialize fields
    this.resolverSession = resolverSession;
    this.baseURI = content.getURI();
    try {
      this.blankNodeIdMap = IntFile.open(TempDir.createTempFile("n3idmap", null), true);
      this.blankNodeNameMap = new StringToLongMap();
      this.inputStream = content.newInputStream();
    } catch (IOException e) {
      throw new TuplesException("Unable to obtain input stream from " + baseURI, e);
    }
  }

  /**
   * @return the number of statements parsed so far
   */
  synchronized long getStatementCount() throws TuplesException {
    checkForException();
    return statementCount;
  }

  /**
   * @return the total number of statements in the file
   */
  synchronized long waitForStatementTotal() throws TuplesException {
    while (!complete) {
      checkForException();

      // Keep the LinkedList drained.
      triples.clear();
      unallocBlankNodeIdMap.clear();
      unallocBlankNodeNameMap.clear();
      notifyAll();

      try {
        wait();
      } catch (InterruptedException ex) {
        throw new TuplesException("Abort");
      }
    }
    checkForException();
    assert statementCountIsTotal;
    return statementCount;
  }

  /**
   * Returns true if getStatementCount() would return the total number
   * of statements in the file.
   */
  synchronized boolean isStatementCountTotal() throws TuplesException {
    checkForException();
    return statementCountIsTotal;
  }

  //
  // Method implementing Runnable
  //

  public void run() {
    Throwable t = null;

    try {
      (new N3Parser(inputStream, this)).parse();
      if (logger.isDebugEnabled()) logger.debug("Parsed Notation-3");
      return;
    } catch (Throwable th) {
      t = th;
    } finally {
      synchronized (this) {
        if (t != null) {
          exception = t;
        } else if (exception == null) {
          // End of file has been reached without error.
          statementCountIsTotal = true;
        }
        complete = true;
        notifyAll();
      }
    }

    if (logger.isDebugEnabled()) logger.debug("Exception while parsing RDF/XML", exception);
  }

  //
  // Methods implementing N3ParserEventHandler
  //

  public void startDocument() {
    if (logger.isDebugEnabled()) logger.debug("Start N3 document");
    prefixMap.clear();
  }

  public void endDocument() {
    if (logger.isDebugEnabled()) logger.debug("End N3 document");
  }

  public void error(Exception ex, String message) {
    if (logger.isDebugEnabled()) logger.debug(message, ex);
  }

  public void startFormula(int line, String context) {
    if (logger.isDebugEnabled()) logger.debug("Start formula " + context);
  }

  public void endFormula(int line, String context) {
    if (logger.isDebugEnabled()) logger.debug("End formula " + context);
  }

  public void quad(int    line,
                   AST    subj,
                   AST    pred,
                   AST    obj,
                   String context) {

    if (logger.isDebugEnabled()) {
      logger.debug("Parsing " + subj + " " + pred + " " + obj + " from " + baseURI);
    }

    // convert the triple components to JRDF Nodes
    SubjectNode   subjectNode   = null;
    PredicateNode predicateNode = null;
    ObjectNode    objectNode    = null;
    try {
      subjectNode   = (SubjectNode)   toNode(subj);
      predicateNode = (PredicateNode) toNode(pred);
      objectNode    = (ObjectNode)    toNode(obj);
    } catch (MulgaraParserException e) {
      logger.error("Unable to parse. " + e.getMessage());
      return;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Parsed " + subjectNode + " " + predicateNode + " " + objectNode + " from " + baseURI);
    }

    synchronized (this) {
      // Wait for the triples buffer to drain if it's too full
      while (triples.size() >= MAX_TRIPLES) {
        try {
          wait();
        } catch (InterruptedException ex) {
          throw new RuntimeException("Abort");
        }
      }

      // Buffer the statement
      triples.addLast(new TripleImpl(subjectNode, predicateNode, objectNode));
      statementCount++;
      notifyAll();
    }
  }

  public void directive(int line, AST directive, AST[] args, String context) {
    switch (directive.getType()) {
    case N3Parser.AT_PREFIX:
      assert args.length == 2;
      assert args[0].getType() == N3Parser.QNAME;
      assert args[1].getType() == N3Parser.URIREF;
      prefixMap.put(args[0].toString(), args[1].toString());
      return;

    default:
      logger.warn(
        "Ignoring directive at line " + line +
        ": directive=" + directive + " (type " + directive.getType() + ") " +
        "args=" + Arrays.asList(args) + " (type " + args[0].getType() + ") " +
        "context=" + context
      );
    }
  }

  //
  // Internal methods
  //

  /**
   * Convert and validate an AST object into a node.
   *
   * @param ast  The AST object to convert.
   * @return a {@link Node} matching the AST object.
   * @throws MulgaraParserException An unhandled element was encountered.
   */
  private Node toNode(AST ast) throws MulgaraParserException {
    if (ast == null) throw new IllegalArgumentException("Unable to load NULL nodes");

    switch (ast.getType()) {
      case N3Parser.LITERAL:

        // check if this is a literal type
        URI datatype = null;
        String lang = null;

        // get any modifiers
        AST a1 = ast.getNextSibling();
        AST a2 = (a1 == null ? null : a1.getNextSibling());

        // find the language
        lang = getLang(a1);
        if (lang == null) lang = getLang(a2);
        if (lang == null) lang = "";

        // find the datatype
        datatype = getDatatype(a1);
        if (datatype == null) datatype = getDatatype(a2);

        if (datatype == null) {
          return new LiteralImpl(ast.toString(), lang);
        } else {
          return new LiteralImpl(ast.toString(), datatype);
        }

      case N3Parser.NUMBER:
        datatype = NumberUtil.getXSD(NumberUtil.parseNumber(ast.toString()));
        return new LiteralImpl(ast.toString(), datatype);

      case N3Parser.ANON:
        return getBlankNode(ast);
      case N3Parser.QNAME:
        String s = ast.toString();
        if (isAnonymous(ast)) {
          return getBlankNode(ast);
        } else {
          int colonIndex = s.indexOf(':');
          assert colonIndex != -1;
          String qnamePrefix = s.substring(0, colonIndex + 1);
          String uriPrefix = prefixMap.get(qnamePrefix);
          if (uriPrefix == null) throw new RuntimeException("No @prefix for " + s);
          return toURIReference(uriPrefix + s.substring(colonIndex + 1));
        }
      case N3Parser.URIREF:
        return toURIReference(ast.toString());
      case N3Parser.KW_A:
        return toURIReference(RDF.TYPE);
      case N3Parser.TK_LIST_FIRST:
        return toURIReference(RDF.FIRST);
      case N3Parser.TK_LIST_REST:
        return toURIReference(RDF.REST);
      case N3Parser.TK_LIST_NIL:
        return toURIReference(RDF.NIL);
      case N3Parser.TK_LIST:
        return toURIReference(RDF.LIST);
      case N3Parser.EQUAL:
        return toURIReference(OWL.SAME_AS);
      case N3Parser.FORMULA:
        throw new MulgaraParserException("Formulas are not supported");
      default:
        throw new Error("Unsupported N3 parser token type: " + ast.getType());
    }
  }

  private URIReference toURIReference(String string) {
    try {
      return new URIReferenceImpl(new URI(string));
    } catch (URISyntaxException e) {
      throw new Error("Invalid URI reference generated", e);
    }
  }

  private URIReference toURIReference(URI u) {
    return new URIReferenceImpl(u);
  }

  /**
   * Tests if a node is anonymous.
   *
   * This is done by looking for the {@link #ANON_TAG} prefix.
   *
   * @param node The node to test.
   * @return <code>true</code> if the node is anonymous.
   */
  private boolean isAnonymous(AST node) {
    String idStr = node.toString();
    return idStr.startsWith(ANON_TAG);
  }

  /**
   * Create a blank node from an AST object.
   *
   * @param n The AST node to convert to an anonymous node.
   * @return An anonymous node that the AST node maps to.
   */
  private BlankNode getBlankNode(AST n) {
    // this is anonymous, so parse its ID
    long anonId = parseAnonId(n);
    String anonIdStr = null;
    try {
      synchronized (this) {
        // look up the id in the blank node maps
        long resourceNodeId;
        if (anonId >= 0) {
          resourceNodeId = blankNodeIdMap.getLong(anonId);
        } else {
          // don't expect to use this map
          anonIdStr = n.toString();
          resourceNodeId = blankNodeNameMap.get(anonIdStr);
        }

        // check if the node was found
        BlankNodeImpl blankNode;
        if (resourceNodeId == 0) {
          if (anonId >= 0) {
            blankNode = unallocBlankNodeIdMap.get(resourceNodeId);
          } else {
            blankNode = unallocBlankNodeNameMap.get(n.toString());
          }
        } else {
          // Found the ID, so need to recreate the anonymous resource for it
          blankNode = new BlankNodeImpl(resourceNodeId);
        }

        // check if the node was found
        if (blankNode == null) {
          // need a new anonymous node for this ID
          blankNode = new BlankNodeImpl();
          // need to put this node into a map
          if (anonId >= 0) {
            unallocBlankNodeIdMap.put(anonId, blankNode);
          } else {
            unallocBlankNodeNameMap.put(anonIdStr, blankNode);
          }
        }

        return blankNode;
      }
    } catch (IOException e) {
      throw new RuntimeException("Couldn't generate anonymous resource", e);
    }
  }

  /**
   * Parse out the node ID used by a blank node.
   *
   * @param node The node to get the ID from.
   * @return The number part of the node.
   */
  private long parseAnonId(AST node) {
    String str = node.toString();
    if (!str.startsWith(ANON_TAG)) return -1;
    try {
      int startPoint = node.toString().startsWith(LOCAL_ANON_TAG) ? LOCAL_ANON_TAG.length() : ANON_TAG.length();
      return Long.parseLong(node.toString().substring(startPoint));
    } catch (NumberFormatException nfe) {
      return -1;
    }
  }

  /**
   * Get the language of a node.
   *
   * @param a node to test for language.  May be null.
   * @return The string representing the language, or <code>null</code> if this
   *   is not available.
   */
  private String getLang(AST a) {
    // empty nodes have no info
    if (a == null) return null;
    return a.getType() == N3Parser.AT_LANG ? a.getText() : null;
  }

  /**
   * Get the type of a node.
   *
   * @param a node to test for type.  May be null.
   * @return The URI representing the type, or <code>null</code> if this is not
   *   available.
   */
  private URI getDatatype(AST a) {
    // empty nodes have no info
    if (a == null) return null;
    // check if this is a datatype node
    if (a.getType() != N3Parser.DATATYPE) return null;

    // get the datatype details
    AST dt = a.getFirstChild();
    try {
      return dt == null ? null : new URI(dt.toString());
    } catch (URISyntaxException e) {
      logger.warn("Error parsing N3 datatype: " + dt.toString(), e);
      return null;
    }
  }

  /**
   * If an exception occurred in the parser, throws a TuplesException that
   * wraps the exception.
   */
  private void checkForException() throws TuplesException {
    if (exception != null) {
      throw new TuplesException("Exception while reading " + baseURI, exception);
    }
  }

  /**
   * Returns a new triple from the queue or null if there are no more triples.
   */
  synchronized Triple getTriple() throws TuplesException {
    while (triples.isEmpty()) {
      checkForException();
      if (complete) {
        // No more triples.
        return null;
      }

      // Wait for more triples.
      try {
        wait();
      } catch (InterruptedException ex) {
        throw new TuplesException("Abort");
      }
    }
    checkForException();
    allocateBlankNodes();

    notifyAll();
    return triples.removeFirst();
  }

  /**
   * Allocate the ids for the new blank nodes.
   */
  private synchronized void allocateBlankNodes() {
    try {
      for (Map.Entry<Long, BlankNodeImpl> entry : unallocBlankNodeIdMap.entrySet()) {
        resolverSession.localize(entry.getValue());     // This sets and returns the node ID
        blankNodeIdMap.putLong(entry.getKey(), entry.getValue().getNodeId());
      }
      unallocBlankNodeIdMap.clear();

      for (Map.Entry<String, BlankNodeImpl> entry : unallocBlankNodeNameMap.entrySet()) {
        resolverSession.localize(entry.getValue());     // This sets and returns the node ID
        blankNodeNameMap.put(entry.getKey(), entry.getValue().getNodeId());
      }
      unallocBlankNodeNameMap.clear();
    } catch (LocalizeException le) {
      throw new RuntimeException("Unable to create blank node", le);
    } catch (IOException ioe) {
      throw new RuntimeException("Unable to create blank node", ioe);
    }
  }

  /**
   * Stops the thread.
   */
  synchronized void abort() {
    interrupt();

    // Clear the triples list and notify in case ARP uses an internal thread
    // which has become blocked on the list being MAX_TRIPLES in size.
    triples.clear();
    notifyAll();
  }

}
