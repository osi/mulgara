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
import java.util.*;

// Apache Log4J
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

// Locally written classes
import org.kowari.query.*;
import org.kowari.query.rdf.*;
import org.kowari.jrdf.*;
import org.kowari.server.*;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.query.TreeQueryPlan}.
 *
 * @created 2004-07-07
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/07 09:37:07 $ by $Author: newmana $
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
public class MulgaraTreeQueryPlan implements TreeQueryPlan {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(MulgaraTreeQueryPlan.class.getName());

  /**
   * The patter to use as a query.
   */
  private Graph pattern;

  /**
   * The graph to query.
   */
  private Graph target;

  /**
   * The database session to query against.
   */
  private LocalJenaSession session;

  /**
   * The JRDF Graph to use to globalize objects.
   */
  private org.jrdf.graph.Graph jrdfGraph;

  /**
   * Hashmap of nodes to variables.
   */
  private HashMap nodesToVariables = new HashMap();

  /**
   * Variable factory.
   */
  private VariableFactory varFactory = new VariableFactoryImpl();

  /**
   * The URI of the graph we are querying.
   */
  private URI modelURI;

  public MulgaraTreeQueryPlan(Graph newTarget, Graph newPattern,
      LocalJenaSession newSession, URI newModelURI) {
    target = newTarget;
    pattern = newPattern;
    session = newSession;
    modelURI = newModelURI;

    // Create JRDF graph to convert Jena nodes into JRDF objects.
    try {
      jrdfGraph = new JRDFGraph((LocalJRDFSession) newSession, modelURI);
    }
    catch (Exception e) {
      logger.error("Failed to create JRDF graph", e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @throws JenaException if there is any problems executing the query.
   */
  public Graph executeTree() throws JenaException {

    // If there are no patterns return an empty Graph.
    if (pattern.size() == 0) {
      return Graph.emptyGraph;
    }

    // Construct a new query object representing the TreeQuery.
    org.kowari.query.Query query = new org.kowari.query.Query(
      toList(),                                               // SELECT
      toModelExpression(),                                    // FROM
      toConstraintExpression(),                               // WHERE
      null,                                                   // HAVING
      Collections.EMPTY_LIST,                                 // ORDER BY
      null,                                                   // LIMIT
      0,                                                      // OFFSET
      new UnconstrainedAnswer()                               // GIVEN
    );

    Answer answer = null;
    Graph result = new GraphMem();

    try {

      // Execute the query.
      answer = session.query(query);
      answer.beforeFirst();

      // Calculate the number of triples across.
      int numberOfTriples = answer.getNumberOfVariables() / 3;

      // Return the constraints given if there were no variables.
      if (!toList().isEmpty()) {

        // Assume lots of s,p,o
        while (answer.next()) {

          // Answer contains 1 or more triples across.
          for (int index = 0; index < numberOfTriples; index++) {
            SubjectNode s = (SubjectNode) answer.getObject(index*3);
            PredicateNode p = (PredicateNode) answer.getObject(index*3 + 1);
            ObjectNode o  = (ObjectNode) answer.getObject(index*3 + 2);
            Triple triple = session.getJenaFactory().convertTriple(new TripleImpl(
                s, p, o));
            result.add(triple);
          }
        }
      }
      else {

        // Working on a fully constrainted query.
        // If unconstrained then we found the fully constrained query.
        if (answer.isUnconstrained()) {
          result = pattern;
        }
      }
    }
    catch (Exception e) {
      logger.error(e);
    }

    return result;
  }

  /**
   * Converts the in memory patterns to a list of ordered variables.
   *
   * @return the ordered variables.
   */
  private List toList() {
    List vars = new LinkedList();
    ClosableIterator constraints = GraphUtil.findAll(pattern);
    while (constraints.hasNext()) {
      Triple triple = (Triple) constraints.next();

      // Only add triples that contain either variables or blank nodes.
      if (triple.getSubject().isVariable() || triple.getSubject().isBlank() ||
          triple.getPredicate().isVariable() || triple.getPredicate().isBlank() ||
          triple.getObject().isVariable() || triple.getObject().isBlank()) {
        vars.add(convertNode(triple.getSubject()));
        vars.add(convertNode(triple.getPredicate()));
        vars.add(convertNode(triple.getObject()));
      }
    }

    return vars;
  }

  /**
   * Converts a Jena node into an appropriate object: variable, URI, literal,
   * etc.
   *
   * @param node the node convert.
   * @throws JenaException if there's a problem converting the Jena node to
   *   a JRDF node.
   * @return the converted Jena node.
   */
  private Object convertNode(com.hp.hpl.jena.graph.Node node) throws JenaException {

    try {
      if (node.isVariable()) {
        return new Variable(node.getName());
      }
      else if (node.isBlank()) {
        return blankToVariable(node);
      }
      else if (node.isURI()) {
        Value value = new URIReferenceImpl(new URI(node.getURI()));
        return new ConstantValue(varFactory.newVariable(), value);
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

        //cannot have noth a language and datatype
        if ((language != null) && (dataTypeURI != null)) {
          throw new JenaException("Literal had both a data type URI and a " +
              "language type");
        }

        //Instantiate label depending on it's properties
        if (language != null) {

          //create Literal with language
          element = new LiteralImpl(label.getLexicalForm(), language);
        }
        else if (dataTypeURI != null) {

          //create Literal with datatype
          element = new LiteralImpl(label.getLexicalForm(), dataTypeURI);
        }
        else {

          //create regular Literal
          element = new LiteralImpl(label.getLexicalForm());
        }
        return new ConstantValue(varFactory.newVariable(), element);
      }
    }
    catch (Exception e) {
      logger.error("Failed to convert node", e);
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

  /**
   * Create the constraint expression object based on the triples in the given
   * query graph.  All triples in the query graph are ANDed together.
   *
   * @return the newly created constraint expressions.
   */
  private ConstraintExpression toConstraintExpression() {
    ClosableIterator constraints = null;
    ConstraintExpression constraintExpression = null;
    try {
      constraints = GraphUtil.findAll(pattern);

      if (constraints.hasNext()) {
        Triple triple = (Triple) constraints.next();
        constraintExpression = toConstraint(triple);

        while (constraints.hasNext()) {
          triple = (Triple) constraints.next();
          constraintExpression = new ConstraintConjunction(constraintExpression,
              toConstraint(triple));
        }
      }
    }
    finally {
      if (constraints != null) {
        constraints.close();
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
    JRDFFactory jrdfFactory = session.getJenaFactory().getJrdfFactory();

    try {
      ConstraintElement subject, predicate, object;
      subject = null;
      predicate = null;
      object = null;

      if (triple.getSubject().isBlank()) {
        subject = blankToVariable(triple.getSubject());
      }
      else {
        subject = (ConstraintElement) jrdfFactory.convertNodeToSubject(
            jrdfGraph, triple.getSubject());
      }

      predicate = (ConstraintElement) jrdfFactory.convertNodeToPredicate(
          jrdfGraph, triple.getPredicate());

      if (triple.getObject().isBlank()) {
        object = blankToVariable(triple.getObject());
      }
      else {
        object = (ConstraintElement) jrdfFactory.convertNodeToObject(jrdfGraph,
            triple.getObject());
      }

      return new ConstraintImpl(subject, predicate, object);
    }
    catch (Exception e) {
      logger.error("Failed to create constraint", e);
      return null;
    }
  }

  /**
   * Converts a Jena blank node into a variable.  These are used to bind
   * various variables in conjuctions.  The same blank node will be bound to
   * the same variable.
   *
   * @param blankNode the blank node to convert.
   * @return the existing or newly created variable.
   */
  private ConstraintElement blankToVariable(Node blankNode) {

    Node_Blank bnode = (Node_Blank) blankNode;
    if (nodesToVariables.containsKey(bnode)) {
      return (ConstraintElement) nodesToVariables.get(bnode);
    }
    else {
      Variable newVar = varFactory.newVariable();
      nodesToVariables.put(bnode, newVar);
      return newVar;
    }
  }

  /**
   * An implementation of variable factory.
   */
  private class VariableFactoryImpl implements VariableFactory {

    /**
     * Index to keep track.
     */
    private int index = 0;

    /**
     * Returns a new variable an increment index.
     *
     * @return Variable a new variable.
     */
    public Variable newVariable() {
      return new Variable("k" + Integer.toString(index++));
    }

    /**
     * Reset the index to 0.
     */
    public void reset() {
      index = 0;
    }
  }
}
