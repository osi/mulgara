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
 * The Initial Developer of the Original Code is Andrew Newman (C) 2005
 * All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.client.jena.model;

//Java 2 standard packages
import java.lang.ref.*;
import java.net.*;
import java.util.*;
import java.io.*;

//Apache packages
import org.apache.log4j.Logger;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;

//Kowari packages
import org.kowari.itql.*;
import org.kowari.itql.lexer.*;
import org.kowari.itql.parser.*;
import org.kowari.query.*;
import org.kowari.query.rdf.Tucana;
import org.kowari.server.*;

/**
 * A Jena Graph backed by a Kowari triplestore.
 *
 * <p>An instance of this class can only be obtained via a ClientModel.</p>
 *
 * @created 2005-01-18
 *
 * @author Chris Wilper
 * @author Andrew Newman
 *
 * @version $Revision: 1.2 $
 *
 * @modified $Date: 2005/02/02 21:12:23 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2005 Andrew Newman
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ClientGraph extends GraphBase implements Graph {

  /** Logger for this class  */
  private final static Logger logger = Logger.getLogger(ClientGraph.class.getName());

  /** The model that this Graph represents */
  private URI graphURI;

  /** Session used to communicate with Model */
  private JenaSession session;

  /** For inserting multiple statements in a single transaction */
  private TransactionHandler transHandler;

  /** For inserting mulitple Statements */
  private BulkUpdateHandler bulkUpdater;

  /** List of ClosableIterators */
  private List iters;

  /** What this graph can do */
  private static Capabilities capabilities = new ClientCapabilities();

  /**
   * Construct a KGraph against the given Kowari model.
   *
   * <p>If <code>textModelURI</code> and <code>textModelSession</code> are
   * non-null, the KGraph will keep that model's literals up-to-date with the
   * regular model's.</p>
   *
   * @see org.mulgara.rdql.client.jena.kmodel.KModel#getInstance(URI, URI)
   * @see org.mulgara.rdql.client.jena.kmodel.KModel#getInstance(URI, URI, URI)
   * @see org.mulgara.rdql.client.jena.kmodel.KModel#getGraph()
   *
   * @param jenaSession
   * @param graphURI
   * @throws JenaException
   */
  public ClientGraph(JenaSession jenaSession, URI graphURI) throws JenaException {

    //validate
    if (jenaSession == null) {
      throw new JenaException("Session cannot be null.");
    }
    if (graphURI == null) {
      throw new JenaException("Model URI cannot be null.");
    }

    //initialize members
    this.graphURI = graphURI;
    this.session = jenaSession;

    //instantiate members
    this.iters = new ArrayList();
    this.transHandler = new ClientTransactionHandler(jenaSession);
    this.bulkUpdater = new ClientBulkUpdateHandler(this);
  }

  /**
   * Returns the BulkUpdateHandler for inserting multiple statements.
   *
   * @return BulkUpdateHandler
   */
  public BulkUpdateHandler getBulkUpdateHandler() {

    return bulkUpdater;
  }

  /**
   * Returns a discription of the actions this model is capable of.
   *
   * @return Capabilities
   */
  public Capabilities getCapabilities() {

    return capabilities;
  }

  /**
   * Returns the TransactionHandler used to insert multiple statements in one
   * transaction.
   *
   * @return TransactionHandler
   */
  public TransactionHandler getTransactionHandler() {
    return this.transHandler;
  }

  /**
   * Returns an Iterator containing the query results.
   *
   * @param match TripleMatch
   * @throws JenaException
   * @return ExtendedIterator
   */
  public ExtendedIterator find(TripleMatch match) throws JenaException {

    // Convert the triple match to get the nodes.
    Triple tmpTriple = match.asTriple();
    return find(tmpTriple.getSubject(), tmpTriple.getPredicate(),
        tmpTriple.getObject());
  }

  /**
   * Returns an iterator over Triple.
   *
   * @return an iterator over Triple.
   * @throws JenaException if the graph failed when attempting to find the
   *     nodes.
   */
  public ExtendedIterator find(Node s, Node p, Node o) throws JenaException {

    try {
      // Convert the nodes, if null to being wildcards that will be accepted
      // by the triple constructor.
      if (s == null) {
        s = Node.ANY;
      }

      if (p == null) {
        p = Node.ANY;
      }

      if (o == null) {
        o = Node.ANY;
      }

      ClosableIterator iter = session.find(graphURI, s, p, o);

      // Construct a triple iterator from the s, p, o and the converted object.
      ExtendedIterator tripleMatchIterator = new FilterIterator(
          new NullFilter(), iter);

      // Store the iterator to ensure it's closed when the graph is closed.
      iters.add(iter);

      // Return iterator.
      return tripleMatchIterator;
    }
    catch (QueryException qe) {
      logger.error("Failed when attempting to find statement", qe);
      throw new JenaException("Failed when attempting to find statement",
          qe);
    }
  }

  /**
   * Adds an array of triples to the database.
   *
   * @param triples the triples to add to the database.
   * @throws AddDeniedException if there was an error adding the triple from
   *   the store layer.
   */
  public void performAdd(List triples) throws AddDeniedException {
    try {

      for (int index = 0; index < triples.size(); index++) {
        Triple t = (Triple) triples.get(index);
        if (getReifier().handledAdd(t)) {
          triples.remove(t);
        }
      }

      session.insert(graphURI, (Triple[]) triples.toArray(
          new Triple[triples.size()]));
    }
    catch (QueryException qe) {
      logger.error("Failed to add triple", qe);
      throw new AddDeniedException("Failed to add triple");
    }
  }

  /**
   * Add one triple.
   *
   * @param triple Triple
   * @throws JenaException
   */
  public void performAdd(Triple triple) throws JenaException {

    if (getReifier().handledAdd(triple)) {
      return;
    }
    performDirectAdd(triple);
  }

  /**
   * Adds a triple to the database without checking reification.
   *
   * @param t the triple to add to the database.
   * @throws AddDeniedException if there was an error adding the triple from
   *   the store layer.
   */
  public void performDirectAdd(Triple t) throws AddDeniedException {
    try {
      session.insert(graphURI, t.getSubject(), t.getPredicate(), t.getObject());
    }
    catch (QueryException qe) {
      logger.error("Failed to add triple ", qe);
      throw new AddDeniedException("Failed to add triple ", t);
    }
  }

  /**
   * Removed a triple from the database.
   *
   * @param t the triple to remove from the database.
   * @throws DeleteDeniedException if there was an error removing the triple
   *   from the store layer.
   */
  public void performDelete(Triple t) throws DeleteDeniedException {
    if (getReifier().handledRemove(t)) {
      return;
    }
    performDirectDelete(t);
  }

  /**
   * Removed a triple from the database without checking for reification.
   *
   * @param t the triple to remove from the database.
   * @throws DeleteDeniedException if there was an error removing the triple
   *   from the store layer.
   */
  public void performDirectDelete(Triple t) throws DeleteDeniedException {
    try {
      session.delete(graphURI, t.getSubject(), t.getPredicate(),
          t.getObject());
    }
    catch (QueryException qe) {
      logger.error("Failed to delete triple", qe);
      throw new DeleteDeniedException("Failed when attempting to find " +
          "statement ", t);
    }
  }

  /**
   * Removed an array of triples from the database.
   *
   * @param triples the triples to remove from the database.
   * @throws DeleteDeniedException if there was an error removing the triple
   *   from the store layer.
   */
  public void performDelete(List triples) throws DeleteDeniedException {

    try {
      for (int index = 0; index < triples.size(); index++) {
        Triple t = (Triple) triples.get(index);
        if (getReifier().handledRemove(t)) {
          triples.remove(t);
        }
      }

      session.delete(graphURI, (Triple[]) triples.toArray(
          new Triple[triples.size()]));
    }
    catch (QueryException qe) {
      logger.error("Failed to delete triple", qe);
      throw new DeleteDeniedException("Failed when attempting to find " +
          "statement");
    }
  }

  /**
   * Return the number of statements in the graph.
   *
   * @return int
   */
  public int size() {
    try {
      int intSize = Integer.MAX_VALUE;
      long longSize = session.getNumberOfStatements(graphURI);
      if (longSize < intSize) {
        intSize = (int) longSize;
      }

      return intSize;
    }
    catch (QueryException qe) {
      logger.error("Failed to find statements", qe);
      return 0;
    }
  }

  /**
   * Callback from a iterator to inform the graph that it was successfully
   * closed.  All iterators must be closed as soon as possible, at in the least
   * when the graph is closed.
   *
   * @param iterator the iterator to find in the list and remove.
   */
  public void iteratorClosed(ClosableIterator iterator) {
    if (iters.contains(iterator)) {
      iters.remove(iterator);
    }
  }

  /**
   * Ensure all iterators are closed, then close the session(s).
   *
   * @throws JenaException
   */
  public void close() throws JenaException {

    // Close any unclosed iterators
    Iterator iter = iters.iterator();
    while (iter.hasNext()) {
      ClosableIterator tmpIter = (ClosableIterator) iter.next();
      if (tmpIter != null) {
//        logger.info("Unclosed iterator, please close iterators immediately " +
//            "after use. Iterator:" + tmpIter);
        tmpIter.close();
      }
    }

    super.close();
  }
}
