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

package org.mulgara.content.rdfxml;

// Java 2 standard packages
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import javax.activation.MimeType;

// logging
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.BlankNode;
import org.jrdf.graph.Literal;
import org.jrdf.graph.Node;

// local
import org.mulgara.content.Content;
import org.mulgara.content.NotModifiedException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.rdf.BlankNodeImpl;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.util.IntFile;
import org.mulgara.util.StringToLongMap;
import org.mulgara.util.TempDir;

// XML
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

//Jea
import com.hp.hpl.jena.rdf.arp.ALiteral;
import com.hp.hpl.jena.rdf.arp.ARP;
import com.hp.hpl.jena.rdf.arp.AResource;
import com.hp.hpl.jena.rdf.arp.StatementHandler;

/**
 * This {@link Runnable}
 *
 * @created 2004-04-02
 * @author <a href="http://staff.pisoftware.com/davidm">David Makepeace</a>
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:02 $ @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class Parser extends Thread implements ErrorHandler, StatementHandler {
  /** Logger. */
  private static final Logger logger =
    Logger.getLogger(Parser.class.getName());

  private final int BUFFER_SIZE = 1000;

  /**
   * Maximum size that {@link #queue} can attain without the
   * parser deliberately blocking and waiting for it to drain.
   */
  private final int QUEUE_MAX_BUFFERS = 10;

  /**
   * The number of statements per batch for performance
   * metrics results.
   */
  private static final double STATEMENT_COUNT_BATCH = 250000;

  /**
   * The ARP parser instance to use.
   */
  private final ARP arp = new ARP();

  /**
   * Map ARP anonymous node IDs to {@link BlankNode}s.
   */
  private final StringToLongMap blankNodeNameMap;

  /**
   * Mapping between blank node IDs generated by ARP and nodes in the
   * {@link #queue}.
   */
  private final IntFile blankNodeIdMap;

  /**
   * Supplied inputstream for reading
   */
  private InputStream inputStream;

  private long[][] headBuffer = null;

  private int headIndex = 0;

  private long[][] tailBuffer = null;

  private int tailIndex = 0;

  /**
   * The queue of <code>long[][3]</code> buffers of triples generated by the
   * RDF/XML parser.
   */
  private LinkedList<long[][]> queue = new LinkedList<long[][]>();

  /**
   * The number of statements parsed so far.
   *
   * When {@link #complete} is <code>true</code>, this will be the number of
   * statements in the RDF/XML document.
   */
  private long statementCount = 0;

  /**
   * true if statementCount is the count of the total number of statements in
   * the entire file because the parser has reached the end of the file without
   * error.
   */
  private boolean statementCountIsTotal = false;

  /**
   * Flag used to indicate that the end of the RDF/XML file has been reached.
   */
  private boolean complete = false;

  /**
   * The exception which interrupted parsing, or <code>null</code> is parsing
   * is successful.
   */
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

  /** The data type on the stream provided. If provided then this should be application/rdf+xml. */
  private MimeType contentType;

  /**
   * The context in which to localize incoming RDF nodes.
   */
  private final ResolverSession resolverSession;

  /** The initial start time for performance metrics results. */
  private double startTime;

  /** The time the last batch of statements inserted */
  private double lastStatementLoadTime;

  private boolean isInfoEnabled = false;


  //
  // Constructor
  //

  /**
   * Sole constructor.
   *
   * @throws NotModifiedException if the <var>content</var> model is already cached
   * @throws TuplesException if the {@link #blankNodeIdMap} or
   *   {@link #blankNodeNameMap} can't be created or the <var>content</var>
   *   can't be read
   */
  Parser(Content content, ResolverSession resolverSession)
    throws NotModifiedException, TuplesException {
    // Validate "content" parameter
    if (content == null) {
      throw new IllegalArgumentException("Null \"content\" parameter");
    }

    // Validate "resolverSession" parameter
    if (resolverSession == null) {
      throw new IllegalArgumentException("Null \"resolverSession\" parameter");
    }

    // Initialize fields
    this.baseURI      = content.getURI() != null ? content.getURI() : URI.create(Mulgara.NAMESPACE);
    try {
      this.blankNodeNameMap = new StringToLongMap();
      this.blankNodeIdMap = IntFile.open(
          TempDir.createTempFile("rdfidmap", null)
      );
      this.blankNodeIdMap.clear();
    } catch (IOException e) {
      throw new TuplesException("Unable to create blank node map", e);
    }

    try {
      this.inputStream  = content.newInputStream();
    } catch (IOException e) {
      throw new TuplesException("Unable to obtain input stream from " + baseURI, e);
    }
    this.contentType = content.getContentType();
    this.resolverSession = resolverSession;

    // Configure the RDF/XML parser
    arp.getOptions().setEmbedding(true);
    arp.getOptions().setLaxErrorMode();
    arp.getHandlers().setErrorHandler(this);
    arp.getHandlers().setStatementHandler(this);

    // is info enabled
    isInfoEnabled = logger.isInfoEnabled();

    // Used for statistics during a load
    startTime = System.currentTimeMillis();
    lastStatementLoadTime = startTime;
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
      queue.clear();
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
      arp.load(inputStream, baseURI == null ? "" : baseURI.toString());
      if (logger.isDebugEnabled()) {
        logger.debug("Parsed RDF/XML");
      }
      return;
    } catch (Throwable th) {
      t = th;
    } finally {
      try {
	      if (blankNodeNameMap != null) {
	        blankNodeNameMap.delete();
	      }
	      if (blankNodeIdMap != null) {
	        blankNodeIdMap.delete();
	      }
      } catch (IOException ioex) {
        logger.warn("Unable to clean up blank node id map", ioex);
      } finally {
        flushQueue(t);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Exception while parsing RDF/XML", exception);
    }
  }

  //
  // Methods implementing StatementHandler
  //

  public void statement(AResource subject,
                        AResource predicate,
                        ALiteral object) {

    // Localize the statement
    long[] triple;
    try {
      triple = new long[] { toLocalNode(subject),
                            toLocalNode(predicate),
                            toLocalNode(object) };
    } catch (IOException e) {
      throw new RuntimeException("Unable to localize parsed triple", e);
    } catch (LocalizeException e) {
      throw new RuntimeException("Unable to localize parsed triple", e);
    }

    // Buffer the statement
    addTriple(triple);
  }

  public void statement(AResource subject,
                        AResource predicate,
                        AResource object) {

    // Localize the statement
    long[] triple;
    try {
      triple = new long[] { toLocalNode(subject),
                            toLocalNode(predicate),
                            toLocalNode(object) };
    } catch (IOException e) {
      throw new RuntimeException("Unable to localize parsed triple", e);
    } catch (LocalizeException e) {
      throw new RuntimeException("Unable to localize parsed triple", e);
    }

    // Buffer the statement
    addTriple(triple);
  }

  //
  // Methods implementing ErrorHandler
  //

  /**
   * Recoverable error.
   * @param e The exception being handled.
   */
  public synchronized void error(SAXParseException e) {
    logger.warn("Recoverable error, line " + e.getLineNumber() + ", column " +
                e.getColumnNumber() + ": " + e.getMessage(), e);
  }

  /**
   * Non-recoverable error.
   * @param e The exception being handled
   */
  public synchronized void fatalError(SAXParseException e)
  {
    exception = e;
    logger.error("Fatal error, line " + e.getLineNumber() + ", column " +
                 e.getColumnNumber() + ": " + e.getMessage(), e);
  }

  /**
   * Warning.
   * @param e The exception being warned about
   */
  public void warning(SAXParseException e) {
    logger.warn("Warning, line " + e.getLineNumber() + ", column " +
                e.getColumnNumber() + ": " + e.getMessage(), e);
  }

  //
  // Internal methods
  //

  /**
   * Create a JRDF {@link Literal} object from an ARP literal object.
   *
   * @param literal  the ARP literal
   * @return a local node corresponding to the literal
   * @throws LocalizeException if the literal can't be localized
   */
  private long toLocalNode(ALiteral literal) throws LocalizeException {

    URI type = null;
    if (literal.getDatatypeURI() != null) {
      try {
        type = new URI(literal.getDatatypeURI());
      } catch (URISyntaxException e) {
        throw new Error("ARP generated datatype for " + literal + " which isn't a URI", e);
      }
    }

    String lang = literal.getLang();
    if (type == null) {
      if (lang == null) lang = "";
    } else {
      lang = null;
    }

    if (type == null) {
      return resolverSession.localize(new LiteralImpl(literal.toString(), lang));
    } else {
      return resolverSession.localize(new LiteralImpl(literal.toString(), type));
    }
  }

  /**
   * Create a JRDF {@link Node} from an ARP resource object.
   *
   * @param resource  the ARP resource.
   * @return a local node corresponding the ARP resource (either a URI
   *   reference or a blank node)
   * @throws IOException if bnode IDs can't be stored in {@link #blankNodeIdMap}
   *   or {@link #blankNodeNameMap}
   * @throws LocalizeException if the resource can't be localized
   */
  private long toLocalNode(AResource resource)
        throws IOException, LocalizeException {
    if (resource.isAnonymous()) {
      String anonIdStr = resource.getAnonymousID();
      long anonId = parseAnonId(anonIdStr);
      try {
        long resourceNodeId;
        if (anonId >= 0) {
          resourceNodeId = blankNodeIdMap.getLong(anonId);
        } else {
          // Try the StringToLongMap instead.
          resourceNodeId = blankNodeNameMap.get(anonIdStr);
        }

        // If it's not found add it.
        if (resourceNodeId == 0) {
          // Create a new blank node.
          resourceNodeId = resolverSession.localize(new BlankNodeImpl());
          if (anonId >= 0) {
            blankNodeIdMap.putLong(anonId, resourceNodeId);
          } else {
            blankNodeNameMap.put(anonIdStr, resourceNodeId);
          }
        }
        return resourceNodeId;
      } catch (IOException e) {
        throw new RuntimeException("Couldn't generate anonymous resource", e);
      }
    } else {
      try {
        assert resource.getURI() != null;
        return resolverSession.localize(new URIReferenceImpl(new URI(resource.getURI())));
      } catch (URISyntaxException e) {
        throw new Error("ARP generated a malformed URI", e);
      }
    }
  }


  /**
   * If an exception occurred in the parser, throws a TuplesException that
   * wraps the exception.
   */
  private void checkForException() throws TuplesException {
    if (exception != null) {
      queue.clear();
      headIndex = 0;
      headBuffer = null;
      if (baseURI == null) throw new TuplesException("Exception while reading stream of type: " + contentType, exception);
      throw new TuplesException("Exception while reading " + baseURI, exception);
    }
  }

  /**
   * @return  a new <code>long[3]</code> triple from the queue or
   *   <code>null</code> if there are no more triples.
   */
  long[] getTriple() throws TuplesException {
    if (headBuffer == null || headIndex >= headBuffer.length) {
      // Get another buffer from the queue.
      headIndex = 0;
      headBuffer = null;
      headBuffer = getBufferFromQueue();
      if (headBuffer == null) {
        // No more triples.
        return null;
      }
      assert headBuffer.length > 0;
    }

    // Get a triple from the headBuffer.
    long[] triple = headBuffer[headIndex];
    headBuffer[headIndex++] = null;
    assert triple != null;
    assert triple.length == 3;
    return triple;
  }

  private synchronized long[][] getBufferFromQueue() throws TuplesException {
    while (queue.isEmpty()) {
      checkForException();
      if (complete) {
        // No more buffers in the queue.
        return null;
      }

      // Wait for a buffer.
      try {
        wait();
      } catch (InterruptedException ex) {
        throw new TuplesException("Abort");
      }
    }
    checkForException();

    notifyAll();
    return queue.removeFirst();
  }

  private void addTriple(long[] triple) {
    assert triple != null;
    if (tailBuffer == null) {
      tailBuffer = new long[BUFFER_SIZE][];
      tailIndex = 0;
    }
    tailBuffer[tailIndex++] = triple;

    if (tailIndex >= tailBuffer.length) {
      // Add the buffer to the queue.
      addBufferToQueue(tailBuffer);
      tailBuffer = null;
      tailIndex = 0;
    }
  }

  private synchronized void flushQueue(Throwable t) {
    if (interrupted()) {
      if (t == null) t = new InterruptedException();
    }

    if (t != null) {
      exception = t;
      queue.clear();
    } else if (exception == null) {
      // End of file has been reached without error.
      if (tailBuffer != null) {
        // There is at least one triple in the tailBuffer.
        assert tailIndex > 0;
        long[][] buf = new long[tailIndex][];
        System.arraycopy(tailBuffer, 0, buf, 0, tailIndex);
        addBufferToQueue(buf);
        logStatementActivity();
      }
      statementCountIsTotal = true;
    } else {
      // An exception has already been reported.
      queue.clear();
    }
    tailBuffer = null;
    tailIndex = 0;
    complete = true;
    notifyAll();
  }

  private synchronized void addBufferToQueue(long[][] buffer) {
    assert buffer != null;
    // Wait for the queue to drain a bit if it's too full
    while (queue.size() >= QUEUE_MAX_BUFFERS) {
      try {
        wait();
      } catch (InterruptedException ex) {
        throw new RuntimeException("Abort");
      }
    }
    queue.addLast(buffer);
    statementCount += buffer.length;
    notifyAll();
  }

  /**
   * Stops the thread.
   */
  synchronized void abort() {
    interrupt();

    // Clear the queue and notify in case ARP uses an internal thread
    // which has become blocked on the list being MAX_TRIPLES in size.
    queue.clear();
    notifyAll();
  }

  private void logStatementActivity() {
    // For very large documents, periodically log activity.
    if (isInfoEnabled) {
      if (statementCount % STATEMENT_COUNT_BATCH == 0) {
        long now = System.currentTimeMillis();
        logger.info("\tbatch timestamp\t" + now +
          "\tstatements\t" + statementCount +
          "\tper second\t" + Math.round((STATEMENT_COUNT_BATCH /
          (now - lastStatementLoadTime))*1000) +
          "\tavg per seconds\t" + Math.round((statementCount /
          (now - startTime))*1000));

        // update the current time for performance logging
        lastStatementLoadTime = now;
      }
    }
  }

  /**
   * Parse the AnonymousID from ARP to get the Id in the form of a long.  We
   * currently make assumptions about the format of the AnonymousID string,
   * namely that the first character of the string is an "A" and that the
   * remaining characters are digits.
   *
   * @param anonIdStr the AnonymousID string
   * @return the Id as a long
   */
  private long parseAnonId(String anonIdStr) {
    assert anonIdStr.length() > 0;
    if (anonIdStr.charAt(0) != 'A') return -1;
    try {
      long anonId = Long.parseLong(anonIdStr.substring(1));
      assert anonId >= 0;
      return anonId;
    } catch (NumberFormatException ex) {
      return -1;
    }
  }

}
