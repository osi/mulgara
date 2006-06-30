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

package org.kowari.jena;

// Java 2 standard packages
import java.net.*;
import java.util.*;

// Third party packages
import org.apache.log4j.*;       // Apache Log4J
import org.jrdf.graph.*;         // JRDF
import com.hp.hpl.jena.graph.*;  // Jena
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;

// Locally written classes
import org.kowari.query.*;
import org.kowari.query.rdf.Tucana;
import org.kowari.server.*;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.Graph} that extends
 * {@link com.hp.hpl.jena.graph.impl.GraphBase} as a wrapper
 * around an {@link org.kowari.resolver.Database}.
 *
 * @created 2004-02-20
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/01/27 11:45:14 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class GraphKowari extends GraphBase {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(GraphKowari.class.getName());

  /**
   * Description of the Field
   */
  public static final Node_Variable[] VARIABLES = {
      new Node_Variable("Subject"), new Node_Variable("Predicate"),
      new Node_Variable("Object") };

  /**
   * The current session.
   */
  protected LocalJenaSession session;

  /**
   * Model URI.
   */
  protected URI graphURI;

  /**
   * The transaction handler.
   */
  private TransactionHandler transactionHandler;

  /**
   * The bulk update handler.
   */
  private BulkUpdateHandler bulkUpdateHandler;

  /**
   * The query handler.
   */
  private QueryHandler queryHandler = null;

  /**
   * The capabilities.
   */
  private Capabilities capabilities;

  /**
   * Reification graph.
   */
  private GraphKowari reificationGraph;

  /**
   * Iterator handler.
   */
  private IteratorHandler iteratorHandler = new IteratorHandlerImpl();

  /**
   * Create an object mapped to a given graph for an existing database.  It
   * will create the graph in the database if it doesn't exist.  This will
   * always create a new session to the database.  This will use the
   * Minimal reification style.
   *
   * @param existingDatabaseSession the session to use for all graph operations.
   *   Cannot be null.
   * @param newGraphURI the URI of the graph to use and possibly create.
   *   Cannot be null.
   * @throws JenaException if there was a failure (generally I/O) creating
   *   the database.
   * @throws IllegalArgumentException if the either of the given parameters
   *   are null.
   */
  public GraphKowari(LocalJenaSession existingDatabaseSession, URI newGraphURI)
      throws JenaException {
    init(existingDatabaseSession, newGraphURI, false);
  }

  /**
   * Create an object mapped to a given graph for an existing database.  It
   * will create the graph in the database if it doesn't exist.  This will
   * always create a new session to the database.
   *
   * @param existingDatabaseSession the session to use for all graph operations.
   *   Cannot be null.
   * @param newGraphURI the URI of the graph to use and possibly create.
   *   Cannot be null.
   * @param newReificationStyle the reification style to use.
   * @throws JenaException if there was a failure (generally I/O) creating
   *   the database.
   * @throws IllegalArgumentException if the either of the given parameters
   *   are null.
   */
  public GraphKowari(LocalJenaSession existingDatabaseSession, URI newGraphURI,
      ReificationStyle newReificationStyle) throws JenaException {
    super(newReificationStyle);
    init(existingDatabaseSession, newGraphURI, false);
  }

  /**
   * Create an object mapped to a given graph for an existing database.  It
   * will create the graph in the database if it doesn't exist.  This will
   * always create a new session to the database.
   *
   * @param existingDatabaseSession the session to use for all graph operations.
   *   Cannot be null.
   * @param newGraphURI the URI of the graph to use and possibly create.
   *   Cannot be null.
   * @param newReificationStyle the reification style to use.
   * @param isReificationGraph true if reification graph - don't create another
   *   reification graph.
   * @throws JenaException if there was a failure (generally I/O) creating
   *   the database.
   * @throws IllegalArgumentException if the either of the given parameters
   *   are null.
   */
  private GraphKowari(LocalJenaSession existingDatabaseSession, URI newGraphURI,
      ReificationStyle newReificationStyle, boolean isReificationGraph)
      throws JenaException {
    super(newReificationStyle);
    init(existingDatabaseSession, newGraphURI, isReificationGraph);
  }

  /**
   * Create an object mapped to a given graph for an existing database.  It
   * will create the graph in the database if it doesn't exist.  This will
   * always create a new session to the database.
   *
   * @param existingDatabaseSession the session to use for all graph operations.
   *   Cannot be null.
   * @param newGraphURI the URI of the graph to use and possibly create.
   *   Cannot be null.
   * @param isReificationGraph true if reification graph - don't create another
   *   reification graph.
   * @throws JenaException if there was a failure (generally I/O) creating
   *   the database.
   * @throws IllegalArgumentException if the either of the given parameters
   *   are null.
   */
  public void init(LocalJenaSession existingDatabaseSession, URI newGraphURI,
      boolean isReificationGraph) {

    // Session cannot be null.
    if (existingDatabaseSession == null) {
      throw new IllegalArgumentException("Null \"existingDatabaseSession\" " +
          " parameter");
    }

    // Name of the graph cannot be null.
    if (newGraphURI == null) {
      throw new IllegalArgumentException("Null \"newGraphURI\" " +
          " parameter");
    }

    // construct Jena/JRDF structures and tell them about each other
    try {

      // Set the session
      session = existingDatabaseSession;

      // Set model URI
      graphURI = newGraphURI;

      // Create session and model.
      session.createModel(graphURI, new URI(Tucana.NAMESPACE + "Model"));

      if (!isReificationGraph) {
        reificationGraph = new GraphKowari(session, new URI(graphURI + "_ref"),
            ReificationStyle.Minimal, true);
      }
    }
    catch (URISyntaxException use) {
      new JenaException("Bad graph URI", use);
    }
    catch (NullPointerException npe) {
      throw new JenaException("No such graph on server.", npe);
    }
    catch (QueryException qe) {
      throw new JenaException("Failed to get new session and new model", qe);
    }

    // Create Jena specific objects.
    transactionHandler = new KowariTransactionHandler(session);
    bulkUpdateHandler = new KowariBulkUpdateHandler(this);
    capabilities = new KowariCapabilities();
  }

  /**
   * Returns the reification graph.
   *
   * @return the graph for reificiation.
   */
  public GraphKowari getReificationGraph() {
    return reificationGraph;
  }

  /**
   * Returns this Graph's transaction handler - currently
   * {@link org.kowari.jena.KowariTransactionHandler}
   *
   * @return this Graph's transaction handler  - currently
   * {@link org.kowari.jena.KowariTransactionHandler}
   */
  public TransactionHandler getTransactionHandler() {
    return transactionHandler;
  }

  public Reifier getReifier() {
    if (reifier == null) {
      reifier = KowariReifier.create(this, style);
    }
    return reifier;
  }

  /**
   * Returns the URI of the graph.
   *
   * @return the URI of the graph.
   */
  public URI getURI() {
    return graphURI;
  }

  public QueryHandler queryHandler() {
    if (queryHandler == null) {
      queryHandler = new KowariQueryHandler(session, this);
    }
    return queryHandler;
  }

  /**
   * Returns the Graph's bulk update handler - currently
   * {@link org.kowari.jena.KowariBulkUpdateHandler}
   *
   * @return the Graph's bulk update handler - currently
   * {@link org.kowari.jena.KowariBulkUpdateHandler}
   */
  public BulkUpdateHandler getBulkUpdateHandler() {
    return bulkUpdateHandler;
  }

  /**
   * Returns this Graph's capabilities - currently
   * {@link org.kowari.jena.KowariCapabilities}
   *
   * @return this Graph's transaction handler  - currently
   * {@link org.kowari.jena.KowariCapabilities}
   */
  public Capabilities getCapabilities() {
    return capabilities;
  }

  /**
   * Adds a triple to the database.
   *
   * @param t the triple to add to the database.
   * @throws AddDeniedException if there was an error adding the triple from
   *   the store layer.
   */
  public void performAdd(Triple t) throws AddDeniedException {
    if (getReifier().handledAdd(t)) {
      return;
    }
    performDirectAdd(t);
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
   * Returns true if this graph is empty.
   *
   * @return true if this graph is empty.
   */
  public boolean isEmpty() {
    try {
      return session.getNumberOfStatements(graphURI) == 0;
    }
    catch (QueryException qe) {
      logger.error("Failed to find statements", qe);
      return false;
    }
  }

  /**
   * Returns the number of triples in a graph.  Converts the internal version
   * of triples from a long to an int to fit the interface.  If there are more
   * than maximum int size it will only return maximum integer size.
   *
   * @return the number of triples in a graph.
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
   * Returns true if the graph contains a triple that match t; t maybe fluid.
   *
   * @return true if the graph contains a triple that match t; t maybe fluid.
   * @throws JenaException if the graph failed when attempting to find the
   *     statement.
   */
  public boolean contains(Triple t) throws JenaException {
    return contains(t.getSubject(), t.getPredicate(), t.getObject());
  }

  /**
   * Returns true if the graph contains a triple matching (s, p, o).
   * s/p/o may be concrete or fluid. Equivalent to find(s,p,o).hasNext,
   * but an implementation is expected to optimise this in easy cases.
   *
   * @return true if the graph contains a triple matching (s, p, o).
   * @throws JenaException if the graph failed when attempting to find the
   *     statement.
   */
  public boolean contains(Node s, Node p, Node o) throws JenaException {

    try {
      return session.contains(graphURI, s, p, o);
    }
    catch (QueryException qe) {
      logger.error("Failed when attempting to find statement", qe);
      throw new JenaException("Failed when attempting to find statement",
          qe);
    }
  }

  /**
   * Closes the database graph and associated resources.
   */
  public void close() {

    iteratorHandler.close();
    super.close();
  }

  /**
   * Returns an iterator over all the Triples that match the triple pattern.
   * Converts the TripleMatch object to a triple and called the other find
   * method.
   *
   * @param m a Triple[Match] encoding the pattern to look for
   * @return an iterator of all triples in this graph that match m
   * @throws JenaException if the graph failed when attempting to match the
   *     triples.
   */
  public ExtendedIterator find(TripleMatch m) throws JenaException {

    // Convert the triple match to get the nodes.
    Triple tmpTriple = m.asTriple();
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
      iteratorHandler.registerIterator(iter);

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
   * Find all the unique predicates in the graph.
   *
   * @return an iterator containing unique predicates.
   */
  ExtendedIterator findUniqueSubjects() {
    try {

      return (ExtendedIterator) session.findUniqueValues(graphURI,
          VARIABLES[0]);
    }
    catch (QueryException qe) {

      logger.error("Failed when attempting to find statement", qe);
      throw new JenaException("Failed when attempting to find statement",
          qe);
    }
  }

  /**
   * Find all the unique predicates in the graph.
   *
   * @return an iterator containing unique predicates.
   */
  ExtendedIterator findUniquePredicates() {
    try {

      return (ExtendedIterator) session.findUniqueValues(graphURI,
          VARIABLES[1]);
    }
    catch (QueryException qe) {

      logger.error("Failed when attempting to find statement", qe);
      throw new JenaException("Failed when attempting to find statement",
          qe);
    }
  }

  /**
   * Find all the unique predicates in the graph.
   *
   * @return an iterator containing unique predicates.
   */
  ExtendedIterator findUniqueObjects() {
    try {

      return (ExtendedIterator) session.findUniqueValues(graphURI,
          VARIABLES[2]);
    }
    catch (QueryException qe) {

      logger.error("Failed when attempting to find statement", qe);
      throw new JenaException("Failed when attempting to find statement",
          qe);
    }
  }

  /**
   * Returns the session.
   *
   * @return the session used by the graph.
   */
  LocalJenaSession getSession() {
    return session;
  }

  /**
   * Returns true if this graph's content depends on the other graph. May be
   * pessimistic (ie return true if it's not sure). Typically true when a
   * graph is a composition of other graphs, eg union.
   *
   * @param other the graph this graph may depend on
   * @return false if this does not depend on other
   */
  public boolean dependsOn(Graph other) {
    return (other == this);
  }

  /**
   * Compare this graph with another using the method
   * described in
   * <a href="http://www.w3.org/TR/rdf-concepts#section-Graph-syntax">
   * http://www.w3.org/TR/rdf-concepts#section-Graph-syntax
   * </a>
   * @param g Compare against this.
   * @return boolean True if the two graphs are isomorphic.
   */
  public boolean isIsomorphicWith(Graph g) {
    return g != null && GraphMatcher.equals(this, g);
  }

  /**
   * Accessor method for jenaFactory.
   *
   * @return The local JenaFactory.
   */
  public JenaFactory getJenaFactory() {
    return session.getJenaFactory();
  }

  public String toString() {
    if (!closed) {
      return toString("Graph tucana: ", this);
    }
    else {
      return "Graph tucana: closed";
    }
  }
}
