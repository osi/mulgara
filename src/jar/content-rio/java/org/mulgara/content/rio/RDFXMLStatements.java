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

package org.mulgara.content.rio;

// Java 2 standard packages
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.xml.sax.*;

// Third party packages
import com.hp.hpl.jena.rdf.arp.ARP;  // ARP (Jena RDF/XML parser)
import com.hp.hpl.jena.rdf.arp.ALiteral;
import com.hp.hpl.jena.rdf.arp.AResource;
import com.hp.hpl.jena.rdf.arp.StatementHandler;
import com.hp.hpl.jena.rdf.arp.StatementHandler;
import org.apache.log4j.Logger;      // Apache Log4J
import org.jrdf.graph.*;             // JRDF

// Locally written packages
import org.mulgara.content.Content;
import org.mulgara.content.ContentHandlerException;
import org.mulgara.content.NotModifiedException;
import org.mulgara.content.NotModifiedTuplesException;
import org.mulgara.query.Constraint;
import org.mulgara.query.Cursor;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.query.rdf.*;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;
import org.mulgara.resolver.spi.StatementsWrapperResolution;
import org.mulgara.store.StoreException;
import org.mulgara.store.tuples.AbstractTuples;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.util.*;

/**
 * Parses an {@link InputStream} into {@link Statements}.
 *
 * This particular implementation is complicated by the need to adapt the Jena
 * ARP RDF/XML <q>push</q> parser to be a <q>pull</q> parser instead.
 *
 * @created 2004-04-02
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:04 $ @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class RDFXMLStatements extends AbstractTuples implements Statements
{
  /**
   * Logger.
   */
  private static final Logger logger =
    Logger.getLogger(RDFXMLStatements.class.getName());

  /**
   * The RDF/XML formatted document.
   */
  private Content content;

  /**
   * The session used to globalize the RDF nodes from the stream.
   */
  private ResolverSession resolverSession;

  /**
   * Mapping from blank node labels to local nodes.
   */
  private StringToLongMap labelToLNodeMap;

  /**
   * Mapping from blank node sequence numbers to local nodes.
   */
  private IntFile seqNoToLNodeMap;

  /**
   * The current row.
   *
   * if the cursor is not on a row, this will be <code>null</code>
   */
  private Triple triple;

  private ParserThread parserThread = null;

  private long rowCount;

  private boolean rowCountIsValid = false;

  //
  // Constructors
  //

  /**
   * Construct an RDF/XML stream parser.
   *
   * @param content  the RDF/XML content
   * @param resolverSession  session against which to localize RDF nodes
   * @throws IllegalArgumentException if <var>inputStream</var> or
   *   <var>resolverSession</var> are <code>null</code>
   */
  RDFXMLStatements(Content content, ResolverSession resolverSession)
      throws ContentHandlerException
  {
    // Validate "content" parameter
    if (content == null) {
      throw new IllegalArgumentException( "Null \"content\" parameter");
    }

    // Validate "resolverSession" parameter
    if (resolverSession == null) {
      throw new IllegalArgumentException("Null \"resolverSession\" parameter");
    }

    // Initialize fields
    this.content         = content;
    this.resolverSession = resolverSession;

    // Fix the magical column names for RDF statements
    setVariables(new Variable[] { new Variable("subject"),
                                  new Variable("predicate"),
                                  new Variable("object") });

    try {
      this.labelToLNodeMap = new StringToLongMap();
      this.seqNoToLNodeMap = IntFile.open(
          TempDir.createTempFile("rdfidmap", null)
      );
      this.seqNoToLNodeMap.clear();
    } catch (IOException e) {
      throw new ContentHandlerException("Unable to create blank node map", e);
    }
  }

  //
  // Methods implementing Statements
  //

  public long getSubject() throws TuplesException
  {
    return getColumnValue(0);
  }

  public long getPredicate() throws TuplesException
  {
    return getColumnValue(1);
  }

  public long getObject() throws TuplesException
  {
    return getColumnValue(2);
  }

  //
  // Methods implementing AbstractTuples
  //

  /**
   * {@inheritDoc}
   *
   * Non-zero length <var>prefix</var> values don't need to be supported by
   * this class because prefix filtration is implemented by the
   * {@link StatementsWrapperResolution} which the existing external resolvers
   * always apply to their content before returning it.
   *
   * @param prefix {@inheritDoc}; for this particular implementation, non-zero
   *   length prefixes are not supported
   * @throws {@inheritDoc}; also if <var>prefix</var> is non-zero length
   */
  public void beforeFirst(long[] prefix, int suffixTruncation)
    throws TuplesException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Before first");
    }

    // Validate "prefix" parameter
    if (prefix == null) {
      throw new IllegalArgumentException("Null \"prefix\" parameter");
    }
    if (prefix.length != 0) {
      throw new TuplesException(
        getClass() + ".beforeFirst isn't implemented for non-zero length prefix"
      );
    }

    // Validate "suffixTruncation" parameter
    if (suffixTruncation != 0) {
      throw new IllegalArgumentException("Null \"suffixTruncation\" parameter");
    }

    // Shut down any existing parsing thread
    if (parserThread != null) {
      stopThread();
    }

    // Create the parser and start the parsing thread
    try {
      parserThread = new ParserThread(content);
    }
    catch (NotModifiedException e) {
      throw new NotModifiedTuplesException(e);
    }
    parserThread.start();

    // TODO skip forward to the first triple that matches prefix
  }

  /**
   * The cursor position isn't cloned by this method.
   */
  public Object clone()
  {
    RDFXMLStatements cloned = (RDFXMLStatements) super.clone();

    // Copy immutable fields by reference
    cloned.content         = content;
    cloned.resolverSession = resolverSession;

    // The cursor position is not cloned.
    cloned.triple          = null;
    cloned.parserThread    = null;
    cloned.labelToLNodeMap = null;
    cloned.seqNoToLNodeMap = null;

    try {
      cloned.labelToLNodeMap = new StringToLongMap();
      cloned.seqNoToLNodeMap = IntFile.open(
          TempDir.createTempFile("rdfidmap", null)
      );
      cloned.seqNoToLNodeMap.clear();
    } catch (IOException e) {
      throw new RuntimeException("Unable to create blank node map", e);
    }

    return cloned;
  }

  /**
   * Close the RDF/XML formatted input stream.
   */
  public void close() throws TuplesException
  {
    try {
      stopThread();
    } finally {
      try {
	      if (labelToLNodeMap != null) {
	        labelToLNodeMap.delete();
	      }
	      if (seqNoToLNodeMap != null) {
	        seqNoToLNodeMap.delete();
	      }
      } catch (IOException ioex) {
        logger.warn("Unable to clean up blank node id map", ioex);
      } finally {
        labelToLNodeMap = null;
        seqNoToLNodeMap = null;
      }
    }
  }

  /**
   * @param column  0 for the subject, 1 for the predicate, 2 for the object
   */
  public long getColumnValue(int column) throws TuplesException
  {
    if (triple == null) {
      throw new TuplesException("There is no current row");
    }

    // Pull the appropriate field from the current triple as a JRDF Node
    Node node;
    switch (column) {
    case 0:  node = triple.getSubject();   break;
    case 1:  node = triple.getPredicate(); break;
    case 2:  node = triple.getObject();    break;
    default: throw new TuplesException("No such column " + column);
    }
    assert node != null;

    // Localize the node
    try {
      if (node instanceof ParserBlankNodeImpl) {
        return localizeParserBlankNode((ParserBlankNodeImpl)node);
      }
      return resolverSession.localize(node);
    }
    catch (IOException e) {
      throw new TuplesException("Couldn't get column " + column + " value", e);
    }
    catch (LocalizeException e) {
      throw new TuplesException("Couldn't get column " + column + " value", e);
    }
  }

  private long localizeParserBlankNode(ParserBlankNodeImpl pBlankNode)
        throws LocalizeException, IOException {
    long seqNo = pBlankNode.getSeqNo();
    long lNode;
    if (seqNo == 0) {
      // A labelled blank node.
      lNode = labelToLNodeMap.get(pBlankNode.getLabel());
    } else {
      // An unlabelled blank node.
      lNode = seqNoToLNodeMap.getLong(seqNo);
    }

    if (lNode == 0) {
      lNode = resolverSession.localize(pBlankNode);
      if (seqNo == 0) {
        // A labelled blank node.
        labelToLNodeMap.put(pBlankNode.getLabel(), lNode);
      } else {
        // An unlabelled blank node.
        seqNoToLNodeMap.putLong(seqNo, lNode);
      }
    }

    assert lNode != 0;
    return lNode;
  }

  public List getOperands()
  {
    return Collections.EMPTY_LIST;
  }

  public int getRowCardinality() throws TuplesException
  {
    long statementCount;

    if (rowCountIsValid) {
      statementCount = rowCount;
    } else {
      ParserThread p;
      boolean newParser;
      if (parserThread != null) {
        // Use the existing parser.
        p = parserThread;
        newParser = false;
      } else {
        // Create a new parser.
        try {
          p = new ParserThread(content);
        }
        catch (NotModifiedException e) {
          throw new NotModifiedTuplesException(e);
        }
        newParser = true;
      }

      // We can do this since the queue holds more than two triples.
      try {
        synchronized (p) {
          while (p.getStatementCount() < 2 && !p.isStatementCountTotal()) {
            try {
              // Wait on the parser for changes to the statement count or
              // completion status.
              p.wait();
            } catch (InterruptedException ex) {
              throw new TuplesException("Abort");
            }
          }
          statementCount = p.getStatementCount();
        }
      } catch (TuplesException ex) {
        p.abort();
        if (!newParser) {
          // We just aborted the main parser, so nullify the reference.
          parserThread = null;
        }
        throw ex; // rethrow.
      } finally {
        if (newParser) {
          // Stop the thread.
          p.abort();
        }
      }
    }

    // Convert the statement count into a cardinality class
    return statementCount == 0 ? Cursor.ZERO :
           statementCount == 1 ? Cursor.ONE :
                                 Cursor.MANY;
  }

  public long getRowCount() throws TuplesException
  {
    if (!rowCountIsValid) {
      if (parserThread != null && parserThread.isStatementCountTotal()) {
        // Get the statement count from the parser.
        rowCount = parserThread.getStatementCount();
      } else {
        // Create a new parser.
        ParserThread p;
        try {
          p = new ParserThread(content);
        }
        catch (NotModifiedException e) {
          throw new NotModifiedTuplesException(e);
        }

        // Consume the entire file.
        p.start();
        try {
          rowCount = p.waitForStatementTotal();
        } finally {
          p.abort();
        }
      }
      rowCountIsValid = true;
    }
    return rowCount;
  }

  public long getRowUpperBound() throws TuplesException
  {
    // If the row count isn't yet available, return an absurdly huge value
    return parserThread != null && parserThread.isStatementCountTotal() ?
           parserThread.getStatementCount() : Long.MAX_VALUE;
  }

  public boolean hasNoDuplicates() throws TuplesException
  {
    return false;
  }

  public boolean isColumnEverUnbound(int column) throws TuplesException
  {
    switch (column) {
    case 0: case 1: case 2:
      return false;
    default:
      throw new TuplesException("No such column " + column);
    }
  }

  public boolean next() throws TuplesException
  {
    if (parserThread == null) {
      // no current row
      return false;
    }

    try {
      triple = parserThread.getTriple();
    } catch (TuplesException ex) {
      stopThread();
      throw ex; // rethrow
    }

    if (triple == null) {
      // Hit the end of the file.
      assert parserThread.isStatementCountTotal();
      rowCount = parserThread.getStatementCount();
      rowCountIsValid = true;
      stopThread();
    }
    return triple != null;
  }

  /**
   * Stops the thread if it is running, and clears the current row.
   */
  private void stopThread()
  {
    if (parserThread != null) {
      parserThread.abort();
      parserThread = null;
    }
    triple = null;

    try {
      if (labelToLNodeMap != null) {
        labelToLNodeMap.clear();
      }
      if (seqNoToLNodeMap != null) {
        seqNoToLNodeMap.setSize(0);
      }
    } catch (IOException ioex) {
      logger.warn("Unable to clean up blank node id map", ioex);
    }
  }

}
