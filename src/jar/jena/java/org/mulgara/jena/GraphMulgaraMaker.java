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

package org.mulgara.jena;

// Java 2 standard packages
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.Mulgara;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatchFilter;
import com.hp.hpl.jena.graph.impl.BaseGraphMaker;
import com.hp.hpl.jena.shared.AlreadyExistsException;
import com.hp.hpl.jena.shared.DoesNotExistException;
import com.hp.hpl.jena.shared.JenaException;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.Map1;
import com.hp.hpl.jena.util.iterator.WrappedIterator;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.Graph} that extends
 * {@link com.hp.hpl.jena.graph.impl.GraphBase} as a wrapper
 * around an {@link org.mulgara.resolver.DatabaseSession}.
 *
 * @created 2004-02-20
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $ by $Author: newmana $
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
public class GraphMulgaraMaker extends BaseGraphMaker {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(GraphMulgaraMaker.class.getName());

  /**
   * The URI node of all Mulgara models.
   */
  private static Node mulgaraModel =
      Node.createURI(Mulgara.NAMESPACE + "Model");

  /**
   * Map object used to convert the URIs to model names expected by Jena.
   */
  private final Map1 graphMulgaraMakerMap = new GraphMulgaraMakerMap();

  /**
   * The URI of the system model.
   */
  private Node systemModel;

  /**
   * The system model filter.
   */
  private TripleMatchFilter systemModelFilter;

  /**
   * The URI of the security model.
   */
  private Node securityModel;

  /**
   * The security model filter.
   */
  private TripleMatchFilter securityModelFilter;

  /**
   * The default graph for this maker, or null if there isn't one.
   */
  protected Graph defaultGraph = null;

  /**
   * The counter of graphs that are created without a name.
   */
  private int graphCounter = 0;

  /**
   * The graphs that have been created by this GraphMaker.  Will not include
   * all graphs in the system.
   */
  private Map createdGraphs = new HashMap();

  /**
   * The database session that we are using to store statements,
   * create models, etc.
   */
  private LocalJenaSession session;

  /**
   * The database URI.
   */
  private URI databaseURI;

  /**
   * Construct a new GraphMulgaraMake with a given database and reification
   * style.
   *
   * @param newSession the database session that is use to create, list, etc.
   *   models.
   * @param newDatabaseURI the name of the database.
   * @param newReificationStyle the reification style used to create the models
   *   with.
   */
  public GraphMulgaraMaker(LocalJenaSession newSession,
      URI newDatabaseURI, ReificationStyle newReificationStyle) {

    super(newReificationStyle);

    // Session cannot be null.
    if (newSession == null) {

      throw new IllegalArgumentException("Null \"newSession\" parameter");
    }

    // Name of the database cannot be null.
    if (newDatabaseURI == null) {

      throw new IllegalArgumentException("Null \"newDatabaseURI\" " +
          " parameter");
    }

    // Assign member variables.
    session = newSession;
    databaseURI = newDatabaseURI;

    // Creates the system and security model nodes.
    systemModel = Node.createURI(databaseURI + "#");
    securityModel = Node.createURI(databaseURI + "#_");

    // These are used to filter the system and security model from the list of
    // statements.
    systemModelFilter = new TripleMatchFilter(new Triple(systemModel, Node.ANY,
        Node.ANY));
    securityModelFilter = new TripleMatchFilter(new Triple(securityModel,
        Node.ANY, Node.ANY));
  }

  /**
   * Returns the default graph, called default in server1 database.
   *
   * @return the default graph, called default in server1 database.
   */
  public Graph getGraph() {

    if (defaultGraph == null) {
      defaultGraph = createGraph("default", false);
    }
    return defaultGraph;
  }

  /**
   * Returns a newly created a graph with fixed name, i.e. "anon_<digit>+".
   *
   * @return a newly created a graph with fixed name, i.e. "anon_<digit>+".
   */
  public Graph createGraph() {
    Graph g = createGraph("anon_" + graphCounter++ +"", false);
    return g;
  }

  /**
   * Create a new graph associated with the given name. If there is no such
   * association, create one and return it. If one exists but strict  is
   * false, return the associated graph. Otherwise throw an
   * AlreadyExistsException.
   *
   * @param graphName the name to give to the new graph.
   * @param strict true to cause existing bindings to throw an exception.
   * @throws AlreadyExistsException if that name is already bound.
   * @throws JenaException if the creation of the graph failed.
   */
  public Graph createGraph(String graphName, boolean strict)
      throws AlreadyExistsException, JenaException {

    if (graphName == null || graphName.length() == 0) {
      throw new JenaException("Graph is blank or empty.");
    }

    if ((strict) && hasGraph(graphName)) {

      throw new AlreadyExistsException("Graph already exists, creating a " +
          "graph twice is disallowed: " + graphName);
    }

    try {

      // Create the URI for the graph.
      URI graphURI = new URI(databaseURI.getScheme(),
          databaseURI.getSchemeSpecificPart(), graphName);

      // Create the graph and add the name to it.
      GraphMulgara graph = new GraphMulgara(session, graphURI, style);
      createdGraphs.put(graphName, graph);

      return graph;
    }
    catch (URISyntaxException use) {

      logger.fatal("Incorrect graph name: " + graphName, use);
      throw new IllegalArgumentException("Incorrect graph name: " + graphName);
    }
  }

  /**
   * Open an existing graph; if there's no such graph, but failIfAbsent is
   * false, create a new one. In any case, return that graph.
   *
   * @param graphName The name of the graph to find and return.
   * @param strict False to create a new one if one doesn't already exist. True
   *   and the graph does not exist it will throw an exception.
   * @return The graph.
   * @throws DoesNotExistException if there's no such named graph.
   */
  public Graph openGraph(String graphName, boolean strict)
      throws DoesNotExistException {

    if ((strict) && (!hasGraph(graphName))) {
      throw new DoesNotExistException("Graph does not exists and creating " +
          "is disallowed: " + graphName);
    }

    // Non-strict create.
    return createGraph(graphName);
  }

  /**
   * Return true if there's a graph with the given name.
   *
   * @return true if there's a graph with the given name.
   */
  public boolean hasGraph(String name) {
    return createdGraphs.containsKey(name);
  }

  /**
   * Remove a graph from the database - at present, this has to be done by
   * opening it first.
   *
   * @param graphName The name of the graph to find and return.
   * @throws DoesNotExistException if the name is unbound.
   * @throws JenaException if there was a failure in removing the graph
   *     from the system.
   */
  public void removeGraph(String graphName) throws DoesNotExistException {
    if (!hasGraph(graphName)) {
      throw new DoesNotExistException("Graph does not exist: " + graphName);
    }

    try {

      // Create a new session and remove the model from the store.
      session.removeModel(new URI(databaseURI.getScheme(),
          databaseURI.getSchemeSpecificPart(), graphName));
      session.removeModel(new URI(databaseURI.getScheme(),
        databaseURI.getSchemeSpecificPart(), graphName + "_ref"));

      // Remove the model from the in memory hash map.
      createdGraphs.remove(graphName);
      createdGraphs.remove(graphName + "_ref");
    }
    catch (URISyntaxException use) {

      logger.fatal("Incorrect graph name: " + graphName, use);
      throw new IllegalArgumentException("Incorrect graph name: " + graphName);
    }
    catch (QueryException qe) {

      logger.error("Couldn't get a new session to remove graph", qe);
      throw new JenaException("Couldn't get a new session to remove graph", qe);
    }

    createdGraphs.remove(graphName);
    createdGraphs.remove(graphName + "_ref");
  }

  /**
   * Remove all the graphs that have been created by this factory.
   */
  public void removeAll() {

    // Get the iterator for all created graphs.
    Iterator iter = new HashSet(createdGraphs.keySet()).iterator();

    // Iterate over the keys and remove each one.
    while (iter.hasNext()) {
      String graphName = (String) iter.next();
      removeGraph(graphName);
    }
  }

  /**
   * Currently returns null.
   */
  public Node getMakerClass() {

    return null;
  }

  /**
   * Currently not implemented.
   */
  protected void augmentDescription(Graph g, Node self) {

    // No-op.
  }

  /**
   * Currently does nothing.  All resources are memory based.
   */
  public void close() {

    // No-op.
  }

  /**
   * Returns a list of all the graph available.  Currently, very ineffecient
   * as it has to convert the disk based iterator to a memory based one because
   * Jena semantics do not include closing.  There is not caching or
   * checking to see if the state of the available models have changed.
   *
   * @return ExtendedIterator the list of all graphs available.
   * @throws JenaException if there was an exception when trying to list the
   *     graphs.
   */
  public ExtendedIterator listGraphs() throws JenaException {

    try {

      // Connect to system graph.
      URI systemURI = new URI(databaseURI.toString() + "#");
      GraphMulgara graph = new GraphMulgara(session, systemURI);

      // Get all models from the graph.
      ExtendedIterator iterator = graph.find(null,
          Node.createURI(com.hp.hpl.jena.vocabulary.RDF.type.getURI()),
          mulgaraModel);

      // Filter out system model and security models.
      iterator = iterator.filterDrop(systemModelFilter);
      iterator = iterator.filterDrop(securityModelFilter);

      // Convert to an in memory iterator.
      List graphList = new ArrayList();
      while (iterator.hasNext()) {
        Triple graphTriple = (Triple) iterator.next();
        if (!graphTriple.getSubject().toString().endsWith("_ref")) {
          graphList.add(graphTriple);
        }
      }

      // Close the scalable, disk based iterator.
      iterator.close();

      // Transform the subject from a URI to a String.
      iterator = WrappedIterator.create(graphList.iterator());
      iterator = iterator.mapWith(graphMulgaraMakerMap);

      return iterator;
    }
    catch (Exception e) {

      logger.fatal("Failed to list graphs", e);
      throw new JenaException("Failed to list graphs", e);
    }
  }

  /**
   * A class that gets the subject node and processes it so that
   * the URI part is removed which is then converted to a string.  This is for
   * Jena compatability with the unit tests.
   */
  private class GraphMulgaraMakerMap implements Map1 {

    public Object map1(Object o) {

      Triple triple = (Triple) o;
      Node subject = triple.getSubject();
      String subjectString = subject.toString();

      // Ensure that the database URI exists
      if (subjectString.indexOf(databaseURI.toString()) >= 0) {

        subjectString = subjectString.substring(
            databaseURI.toString().length()+1, subjectString.length());
      }

      subject = Node.createLiteral(subjectString, null, false);

      return subject.toString();
    }
  }
}
