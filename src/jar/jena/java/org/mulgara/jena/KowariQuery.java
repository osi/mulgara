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
 * Contributor(s): (c) Copyright 2002, 2003,
 *   Hewlett-Packard Development Company, LP
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.jena;

// Java classes
import java.net.*;
import java.util.*;

// Log4j
import org.apache.log4j.Logger;

// Jena classes
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.graph.impl.*;

// Internal classes
import org.kowari.query.*;
import org.kowari.query.rdf.*;

/**
 * A re-implementation of the concrete {@link com.hp.hpl.jena.graph.query.Query}.
 *
 * @created 2004-07-26
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
public class KowariQuery extends com.hp.hpl.jena.graph.query.Query {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(KowariQuery.class.getName());

  /**
   * The named bunches of triples for graph matching
   */
  private TripleHashMap triples = new TripleHashMap();

  /**
   * The map of Jena nodes to Kowari variables.
   */
  private HashMap nodesToVariables = new HashMap();

  /**
   * A variable factory used by the variable list in the SELECT clause.  The
   * same variable is assumed to map to a variable in the WHERE clause.
   */
  private VariableFactoryImpl listVariableFactory = new VariableFactoryImpl();

  /**
   * Used to create VariableConstraints.
   */
  private ConstantVariableFactoryImpl constFactory = new
      ConstantVariableFactoryImpl();

  /**
   * A variable factory used by the variable list in the WHERE clause.  The
   * same variable is assumed to map to a variable in the SELECT clause.
   */
  private VariableFactoryImpl constraintVariableFactory =
      new VariableFactoryImpl();

  /**
   * Null constructor - call super().
   */
  public KowariQuery() {
    super();
  }

  /**
   * Creates a Query using a graph as a set of patterns.
   *
   * @param pattern the pattern to match against.
   */
  public KowariQuery(Graph pattern) {
    addMatches(pattern);
  }

  /**
   * Add an (S, P, O) match to the query's collection of match triples. Return
   * this query for cascading.
   *
   * Adds it to the internal hashMap before calling the superclass.
   *
   * @param s the node to match the subject
   * @param p the node to match the predicate
   * @param o the node to match the object
   * @return this Query, for cascading
   */
  public com.hp.hpl.jena.graph.query.Query addMatch(Node s, Node p, Node o) {

    // Ensure we call the super method.
    super.addMatch(s, p, o);

    triples.addPattern(NamedTripleBunches.anon, new Triple(s, p, o));
    return this;
  }

  /**
   * Add a triple to the query's collection of match triples. Return this query
   * for cascading.
   *
   * @param t an (S, P, O) triple to add to the collection of matches
   * @return this Query, for cascading
   */
  public com.hp.hpl.jena.graph.query.Query addMatch(Triple t) {

    // Ensure we call the super method.
    super.addMatch(t);

    triples.addPattern(NamedTripleBunches.anon, t);
    return this;
  }

  /**
   * Add an (S, P, O) match triple to this query to match against the graph
   * labelled with <code>name</code>. Return this query for cascading.
   *
   * @param name the name that will identify the graph in the matching
   * @param s the node to match the subject
   * @param p the node to match the predicate
   * @param o the node to match the object
   * @return this Query, for cascading.
   */
  public com.hp.hpl.jena.graph.query.Query addMatch(String name, Node s, Node p, Node o) {

    // Ensure we call the super method.
    super.addMatch(name, s, p, o);

    triples.addPattern(name, new Triple(s, p, o));
    return this;
  }

  /**
   * Not supported by KowariQuery.
   *
   * @param e expression to add.
   * @throws UnsupportedOperationException not supported by KowariQuery.
   * @return Query a new query object with an expression applied.
   */
  public com.hp.hpl.jena.graph.query.Query addConstraint(Expression e) {
    throw new UnsupportedOperationException("Does not support adding " +
        "constraints");
  }

  /**
   * Returns the hash map of triples to match against.
   *
   * @return the hash map of triples to match against.
   */
  public HashMap getHashMapTriples() {
    return triples;
  }

  /**
   * Uses a graph to add matches to the query.
   *
   * @param g the graph to add.
   */
  private void addMatches(Graph g) {
    ClosableIterator it = GraphUtil.findAll(g);
    while (it.hasNext()) {
      Triple triple = (Triple) it.next();
      addMatch(triple);
    }
  }

  public ExtendedIterator executeBindings(List outStages, NamedGraphMap args,
      Node[] nodes) {

    // Seed the node to constraint maps with all variables.
    seedConstraintMap();

    // If the graph is empty, the variables are empty and there are no matches
    // in the query object return an empty domain iterator.
    if (getHashMapTriples().values().size() == 0) {
      return new EmptyDomainIterator();
    }

    Set uniqueGraphs = getUniqueGraphs(args, getHashMapTriples());

    ExtendedIterator iter = null;
    org.kowari.query.Query query = null;
    try {
      query = new org.kowari.query.Query(
        toList(nodes),
        toModelExpression(uniqueGraphs),
        toConstraintExpression(args, getHashMapTriples(), uniqueGraphs),
        null,
        Collections.EMPTY_LIST,
        null,
        0,
        new UnconstrainedAnswer()
      );
    }
    catch (IllegalArgumentException iae) {
      // If the construction of the query fails return a domain iterator with
      // a domain with one entry in it that's null.
      return new EmptyDomainIterator(true);
    }

    Answer answer = null;
    String firstGraph = (String) triples.keySet().iterator().next();
    LocalJenaSession session = ((GraphKowari) args.get(firstGraph)).getSession();

    // Perform the query and produce the results.
    try {
      answer = session.query(query);
      iter = new DomainIterator(answer, session);
    }
    catch (QueryException qe) {
      // Log the error but do nothing else - return an null iterator.
      logger.error("Failed to perform the query: " + query, qe);
    }
    return iter;
  }

  /**
   * Iterates through the list of constraints, converting them to constraint
   * elements and adding them to the constraint map.
   */
  private void seedConstraintMap() {
    Iterator consIter = getHashMapTriples().values().iterator();
    while (consIter.hasNext()) {
      Cons cons = (Cons) consIter.next();
      while(cons != null) {
        Triple tmpTriple = cons.getTriple();
        convertNodeToConstraintElement(tmpTriple.getSubject());
        convertNodeToConstraintElement(tmpTriple.getPredicate());
        convertNodeToConstraintElement(tmpTriple.getObject());
        cons = cons.getTail();
      }
    }
  }

  /**
   * Create a set of unique Jena graphs based on the map of graph names to
   * constraints and the map of graph names to graphs.
   *
   * @param args the string names to graph hash map.
   * @param namedTriples the named graphs to triple constraints.
   * @return the unique set of graphs used by the named triple constraints.
   */
  private Set getUniqueGraphs(NamedGraphMap args, HashMap namedTriples) {

    // Get the set of unique graphs.
    Set uniqueGraphs = new HashSet();
    Iterator iter = namedTriples.keySet().iterator();
    while (iter.hasNext()) {
      String graphName = (String) iter.next();
      uniqueGraphs.add(args.get(graphName));
    }

    return uniqueGraphs;
  }

  /**
   * Create the list of variables to be used in a SELECT.
   *
   * @param nodes the variable nodes to use.
   * @return List the variables and constants which represent the constraints
   *   to match.
   */
  private List toList(Node[] nodes) {
    List varList = new ArrayList();

    if (nodes.length == 0) {
      Triple triple = new Triple(Node.ANY, Node.ANY, Node.ANY);
      varList.add(convertNodeToVariable(triple.getSubject()));
      varList.add(convertNodeToVariable(triple.getPredicate()));
      varList.add(convertNodeToVariable(triple.getObject()));
    }
    else {
      for (int index = 0; index < nodes.length; index++) {
        varList.add(convertNodeToVariable(nodes[index]));
      }
    }
    return varList;
  }


  /**
   * Returns the ModelResource of the given modelURI.
   *
   * @return the ModelResource of the given modelURI.
   */
  private ModelExpression toModelExpression(Set uniqueGraphs) {

    // Create the model expression.
    if (uniqueGraphs.size() == 1) {
      return new ModelResource(((GraphKowari) uniqueGraphs.iterator().next()).
          getURI());
    }
    else {
      Iterator graphIter = uniqueGraphs.iterator();
      GraphKowari tmpGraph;
      ModelExpression exp1, exp2, expression;

      // Create union the first two graphs.
      tmpGraph = (GraphKowari) graphIter.next();
      exp1 = new ModelResource(tmpGraph.getURI());
      tmpGraph = (GraphKowari) graphIter.next();
      exp2 = new ModelResource(tmpGraph.getURI());
      expression = new ModelUnion(exp1, exp2);

      // Union any extra graphs.
      while (graphIter.hasNext()) {
        exp1 = expression;
        tmpGraph = (GraphKowari) graphIter.next();
        exp2 = new ModelResource(tmpGraph.getURI());
        expression = new ModelUnion(exp1, exp2);
      }
      return expression;
    }
  }

  /**
   * Creates the constraints to put in the WHERE clause.
   *
   * @return the expression of Constraints to put in the WHERE clause.
   */
  private ConstraintExpression toConstraintExpression(NamedGraphMap args,
      HashMap namedTriples, Set uniqueGraphs) {

    // Iterate through all the matches
    ConstraintExpression constraintExpression = null;

    // If there are no matches we don't constrain anything.
    if (uniqueGraphs.size() == 1) {
      GraphKowari tmpGraph = (GraphKowari) uniqueGraphs.iterator().next();
      constraintExpression = toConstraint(new Triple(Node.ANY, Node.ANY,
          Node.ANY), new URIReferenceImpl(tmpGraph.getURI()));
    }
    else {
      Iterator iter = namedTriples.keySet().iterator();

      // Go through each constraint object an create constraint conjunctions
      // for a list of triples.
      int counter = 0;
      while (iter.hasNext()) {

        String graphName = (String) iter.next();
        Cons cons = (Cons) namedTriples.get(graphName);
        GraphKowari tmpGraph = (GraphKowari) args.get(graphName);
        Cons tmpCons = cons;

        while(tmpCons != null) {
          if (counter == 0) {
            constraintExpression = toConstraint(tmpCons.getTriple(),
                new URIReferenceImpl(tmpGraph.getURI()));
          }
          else {
            constraintExpression = new ConstraintConjunction(constraintExpression,
                toConstraint(tmpCons.getTriple(),
                new URIReferenceImpl(tmpGraph.getURI())));
          }
          counter++;
          tmpCons = tmpCons.getTail();
        }
      }
    }

    return constraintExpression;
  }

  /**
   * Converts a Jena triple object to a constraint object.
   *
   * @param triple the Jena triple object.
   * @param model the model to query with the given constraint
   * @return the new constraint object based on the Jena triple object.
   */
  private ConstraintExpression toConstraint(Triple triple, URIReferenceImpl model) {

    try {
      return new ConstraintImpl(
          convertNodeToConstraintElement(triple.getSubject()),
          convertNodeToConstraintElement(triple.getPredicate()),
          convertNodeToConstraintElement(triple.getObject()),
          (ConstraintElement) model
      );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }
  /**
   * Returns either a {@link Variable} or a {@link ConstantValue}.
   *
   * @param node the node to convert.
   * @return either a {@link Variable} or {@link ConstantValue}.
   */
  private Object convertNodeToVariable(com.hp.hpl.jena.graph.Node node) {
    try {
      if (node.isVariable()) {
        if (nodesToVariables.containsKey(node)) {
          return nodesToVariables.get(node);
        }
        else {
          return listVariableFactory.newVariable();
        }
      }
      else if (node.isURI()) {
        Value value = new URIReferenceImpl(new URI(node.getURI()));
        return new ConstantValue(constFactory.newVariable(), value);
      }
      else if (node.isLiteral()) {

        LiteralLabel label = node.getLiteral();
        LiteralImpl element;

        // Ensure that we put either null or the URI for the datatype.
        URI dataTypeURI;
        if (label.getDatatypeURI() == null) {
          dataTypeURI = null;
        }
        else {
          dataTypeURI = new URI(label.getDatatypeURI());
        }

        // Determine if language, datatype or neither are specified
        String language = label.language();

        //Instantiate label depending on it's properties
        if (language != null) {

          //create Literal with language
          element = new LiteralImpl(label.getLexicalForm(), language);
        } else if (dataTypeURI != null) {

          //create Literal with datatype
          element = new LiteralImpl(label.getLexicalForm(), dataTypeURI);
        } else {

          //create regular Literal
          element = new LiteralImpl(label.getLexicalForm());
        }
        return new ConstantValue(constFactory.newVariable(), element);
      }
      else if (node == node.ANY) {
        return listVariableFactory.newVariable();
      }
    }
    catch (URISyntaxException e) {
      logger.error("Failed to create a URIReference from: " + node, e);
    }

    return null;
  }

  private ConstraintElement convertNodeToConstraintElement(
      com.hp.hpl.jena.graph.Node node) {
    ConstraintElement element = null;

    try {
      if (node.isVariable()) {
        if (nodesToVariables.containsKey(node)) {
          element = (ConstraintElement) nodesToVariables.get(node);
        }
        else {
          element = constraintVariableFactory.newVariable();
          nodesToVariables.put(node, element);
        }
      }
      else if (node.isURI()) {
        element = new URIReferenceImpl(new URI(node.getURI()));
      }
      else if (node.isLiteral()) {

        LiteralLabel label = node.getLiteral();

        // Ensure that we put either null or the URI for the datatype.
        URI dataTypeURI;
        if (label.getDatatypeURI() == null) {

          dataTypeURI = null;
        }
        else {

          dataTypeURI = new URI(label.getDatatypeURI());
        }

        // Determine if language, datatype or neither are specified
        String language = label.language();

        //Instantiate label depending on it's properties
        if (language != null) {

          //create Literal with language
          element = new LiteralImpl(label.getLexicalForm(), language);
        } else if (dataTypeURI != null) {

          //create Literal with datatype
          element = new LiteralImpl(label.getLexicalForm(), dataTypeURI);
        } else {

          //create regular Literal
          element = new LiteralImpl(label.getLexicalForm());
        }
      }
      else if (node == node.ANY) {
        element = constraintVariableFactory.newVariable();
      }
    }
    catch (URISyntaxException e) {
      logger.error("Failed to create a URIReference from: " + node, e);
    }

    return element;
  }

  /**
   * A HashMap that contains a mapping of a graph to a {@link KowariQuery.Cons}
   * objects.
   */
  private class TripleHashMap extends HashMap {

    public void addPattern(String name, Triple pattern) {
      if (get(name) != null) {
        Cons cons = (Cons) get(name);
        Cons newHead = new Cons(pattern, cons);
        remove(name);
        put(name, newHead);
      }
      else {
        put(name, new Cons(pattern, null));
      }
    }
  }

  /**
   * A linked list of triples representing constraints.
   */
  public static class Cons {

    /**
     * A triple representing a constraints.
     */
    private Triple head;

    /**
     * The next constraint or null if there aren't any.
     */
    private Cons tail;

    /**
     * Create a new constraint object.
     *
     * @param head the triple constraint.
     * @param tail the next constraint.
     */
    public Cons(Triple head, Cons tail) {
      this.head = head;
      this.tail = tail;
    }

    /**
     * Returns the next constraint or null if there aren't any more.
     *
     * @return the next constraint or null if there aren't any more.
     */
    public Cons getTail() {
      return tail;
    }

    /**
     * Returns the triple object representing the constraint.
     *
     * @return the triple object representing the constraint.
     */
    public Triple getTriple() {
      return head;
    }

    /**
     * Recurses through the linked list returning the number of constraints.
     *
     * @param L used to recurse through the list.
     * @return the number of constraints in the list.
     */
    public int size(Cons L) {
      int n = 0;
      while (L != null) {
        n += 1;
        L = L.getTail();
      }
      return n;
    }

    /**
     * String rendering of the object.
     */
    public String toString() {
      return "Triple constraint: " + head;
    }
  }
}
