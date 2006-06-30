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
import java.net.*;
import java.util.*;

// Apache Log4J
import org.apache.log4j.Logger;

// Locally written classes
import org.kowari.query.*;
import org.kowari.query.rdf.*;
import org.kowari.store.*;

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.graph.impl.*;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.query.BindingQueryPlan}.
 *
 * @created 2004-07-07
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
public class MulgaraQueryPlan implements BindingQueryPlan {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(MulgaraQueryPlan.class.getName());

  /**
   * The database session used to perform queries against.
   */
  private LocalJenaSession session;

  /**
   * The URI of the model to query - only supports one graph.
   */
  private URI modelURI;

  /**
   * The object representing a set of constraints and variables to bind.
   */
  private MulgaraQuery kowariQuery;

  /**
   * The graph to query.
   */
  private GraphMulgara graph;

  /**
   * The variables - equivalent to the select in iTQL.
   */
  private Node[] variables;

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
   * A variable factory used by the variable list in the WHERE clause.  The
   * same variable is assumed to map to a variable in the SELECT clause.
   */
  private VariableFactoryImpl constraintVariableFactory = new VariableFactoryImpl();

  /**
   * Used to create VariableConstraints.
   */
  private ConstantVariableFactoryImpl constFactory = new
      ConstantVariableFactoryImpl();

  /**
   * Create a new query plan.
   *
   * @param newGraph the Graph to query.
   * @param newQuery the Query object to use.
   * @param newVariables the nodes to project the constraints to.
   * @param newSession the current session.
   * @param newModelURI the model to query.
   */
  public MulgaraQueryPlan(GraphMulgara newGraph, MulgaraQuery newQuery,
      Node[] newVariables, LocalJenaSession newSession, URI newModelURI) {
    graph = newGraph;
    kowariQuery = newQuery;
    variables = newVariables;
    session = newSession;
    modelURI = newModelURI;

    // Create a hashmap of nodes to Kowari variables.
    for (int index = 0; index < newVariables.length; index++) {
      Node var = newVariables[index];
      if (var.isVariable()) {
        Variable element = new Variable(var.getName());
        nodesToVariables.put(var, element);
      }
    }
  }

  /**
   * Performs the query against the graph and produces an iterator with all the
   * results.
   *
   * @return an iterator which contains all the results from the graph.
   */
  public ExtendedIterator executeBindings() {

    // If the graph is empty, the variables are empty and there are no matches
    // in the query object return an empty domain iterator.
    if ((graph.isEmpty()) && (variables.length == 0) &&
        (kowariQuery.getHashMapTriples().values().size() == 0)) {
      return new EmptyDomainIterator();
    }

    ExtendedIterator iter = null;
    org.kowari.query.Query query = null;
    try {
       query = new org.kowari.query.Query(
        toList(),                                               // SELECT
        toModelExpression(),                                    // FROM
        toConstraintExpression(),                               // WHERE
        null,                                                   // HAVING
        Collections.EMPTY_LIST,                                 // ORDER BY
        null,                                                   // LIMIT
        0,                                                      // OFFSET
        new UnconstrainedAnswer()                               // GIVEN
      );
    }
    catch (IllegalArgumentException iae) {

      // If the construction of the query fails return a domain iterator with
      // a domain with one entry in it that's null.
      return new EmptyDomainIterator(true);
    }

   Answer answer = null;
   Triple triple;

   // If there are no constraints then we are trying to get everything from the
   // graph.
   if (kowariQuery.getHashMapTriples().values().size() == 0) {
     triple = new Triple(Node.ANY, Node.ANY, Node.ANY);
   }
   else {
     Iterator varIter = kowariQuery.getHashMapTriples().values().iterator();
     triple = ((MulgaraQuery.Cons) varIter.next()).getTriple();
   }

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
   * Create the list of variables to be used in a SELECT.
   *
   * @return List the variables and constants which represent the constraints
   *   to match.
   */
  private List toList() {
    List varList = new ArrayList();

    if (variables.length == 0) {
      Triple triple = new Triple(Node.ANY, Node.ANY, Node.ANY);
      varList.add(convertNodeToVariable(triple.getSubject()));
      varList.add(convertNodeToVariable(triple.getPredicate()));
      varList.add(convertNodeToVariable(triple.getObject()));
    }
    else {
      for (int index = 0; index < variables.length; index++) {
        varList.add(convertNodeToVariable(variables[index]));
      }
    }
    return varList;
  }

  /**
   * Creates the constraints to put in the WHERE clause.
   *
   * @return the expression of Constraints to put in the WHERE clause.
   */
  private ConstraintExpression toConstraintExpression() {

    // Iterate through all the matches
    Iterator iter = kowariQuery.getHashMapTriples().values().iterator();
    ConstraintExpression constraintExpression = null;

    // If there are no matches we don't constrain anything.
    if (kowariQuery.getHashMapTriples().size() == 0) {
      constraintExpression = toConstraint(new Triple(Node.ANY, Node.ANY,
          Node.ANY));
    }
    else {

      // Go through each constraint object an create constraint conjunctions
      // for a list of triples.
      int counter = 0;
      while (iter.hasNext()) {
        MulgaraQuery.Cons cons = (MulgaraQuery.Cons) iter.next();
        MulgaraQuery.Cons tmpCons = cons;

        while(tmpCons != null) {
          if (counter == 0) {
            constraintExpression = toConstraint(tmpCons.getTriple());
          }
          else {
            constraintExpression = new ConstraintConjunction(constraintExpression,
                toConstraint(tmpCons.getTriple()));
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
   * @return the new constraint object based on the Jena triple object.
   */
  private ConstraintExpression toConstraint(Triple triple) {

    try {
      return new ConstraintImpl(
          convertNodeToConstraintElement(triple.getSubject()),
          convertNodeToConstraintElement(triple.getPredicate()),
          convertNodeToConstraintElement(triple.getObject())
      );
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
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

  /**
   * Returns the ModelResource of the given modelURI.
   *
   * @return the ModelResource of the given modelURI.
   */
  private ModelResource toModelExpression() {
    return new ModelResource(modelURI);
  }
}
