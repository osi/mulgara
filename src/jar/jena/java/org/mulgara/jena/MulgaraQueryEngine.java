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

import java.util.* ;

import com.hp.hpl.jena.rdql.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.util.iterator.*;
import org.apache.log4j.Logger;

/**
 * An extension of the concrete {@link com.hp.hpl.jena.rdql.QueryEngine} that
 * uses {@link MulgaraQuery} instead.
 *
 * @created 2004-08-26
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
public class MulgaraQueryEngine extends com.hp.hpl.jena.rdql.QueryEngine {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
    Logger.getLogger(MulgaraQueryEngine.class.getName());

  /**
   * Results.
   */
  private MulgaraResultsIterator iter;

  /**
   * Representation of the RDQL query.
   */
  private RdqlQuery query;

  /**
   * Create a new Kowari Query Engine.
   *
   * @param q object representing the Jena RDQL Query object.
   */
  public MulgaraQueryEngine(RdqlQuery q) {
    super(q);
    query = q;
  }

  public QueryResults exec(ResultBinding startBinding) {
    init();
    iter = new MulgaraResultsIterator(query, startBinding);
    return new QueryResultsStream(query, this, iter);
  }

  public void abort() {
    iter.close();
  }

  public void close() {
    iter.close();
  }

  static class MulgaraResultsIterator implements ClosableIterator {

    /**
     * Variables to project to.
     */
    Node[] projectionVars;

    /**
     * The RDQL query object.
     */
    RdqlQuery query;

    /**
     * The binding to results.
     */
    ResultBinding nextBinding = null;

    /**
     * Whether we've come to the end.
     */
    boolean finished = false;

    /**
     * The results.
     */
    ClosableIterator planIter;

    /**
     * The initial binding of results.
     */
    ResultBinding initialBindings;

    /**
     * Construct a new results iterator.
     *
     * @param q the RDQL query object.
     * @param presets the results to bind the results to.
     */
    MulgaraResultsIterator(RdqlQuery q, ResultBinding presets) {
      query = q;
      initialBindings = presets;

      // Build the graph query plan etc.
      Graph graph = query.getSource().getGraph();

      QueryHandler queryHandler = graph.queryHandler();
      MulgaraQuery graphQuery = new MulgaraQuery();

      for (Iterator iter = query.getTriplePatterns().listIterator();
           iter.hasNext(); ) {
        Triple t = (Triple) iter.next();
        t = substituteIntoTriple(t, presets);
        graphQuery.addMatch(t);
      }

      projectionVars = new Node[query.getBoundVars().size()];

      for (int i = 0; i < projectionVars.length; i++) {
        projectionVars[i] = Node.createVariable((String) query.getBoundVars().
            get(i));
      }

      BindingQueryPlan plan = queryHandler.prepareBindings(graphQuery,
          projectionVars);
      planIter = plan.executeBindings();
    }

    public boolean hasNext() {
      if (finished)
        return false;
      // Loop until we get a binding that is satifactory
      // or we run out of candidates.
      while (nextBinding == null) {
        if (!planIter.hasNext())
          break;
        // Convert from graph form to model form
        Domain d = (Domain) planIter.next();

        nextBinding = new ResultBinding(initialBindings);
        nextBinding.setQuery(query);
        for (int i = 0; i < projectionVars.length; i++) {
          String name = projectionVars[i].toString().substring(1);

          Node n = (Node) d.get(i);

          if (n == null) {
            // There was no variable of this name
            // May have been prebound.
            // (Later) may have optionally bound variables.
            // Otherwise, should not occur but this is safe.
            continue;
          }

          // Convert graph node to model RDFNode
          RDFNode rdfNode = convertNodeToRDFNode(n, query.getSource());
          nextBinding.add(name, rdfNode);
        }

        // Verify constriants
        boolean passesTests = true;
        for (Iterator cIter = query.getConstraints().iterator(); cIter.hasNext(); ) {
          Constraint constraint = (Constraint) cIter.next();
          if (!constraint.isSatisfied(query, nextBinding)) {
            passesTests = false;
            break;
          }
        }
        if (!passesTests) {
          nextBinding = null;
          continue;
        }
      }

      if (nextBinding == null) {
        close();
        return false;
      }
      return true;
    }

    public Object next() {
      if (nextBinding == null)
        throw new NoSuchElementException("QueryEngine.ResultsIterator");
      ResultBinding x = nextBinding;
      nextBinding = null;
      return x;
    }

    public void remove() {
      throw new UnsupportedOperationException(
          "QueryEngine.ResultsIterator.remove");
    }

    public void close() {
      if (!finished) {
        planIter.close();
        finished = true;
      }
    }
  }

  static Node convertValueToNode(Value value) {
    if (value.isRDFLiteral())
      return Node.createLiteral(
          value.getRDFLiteral().getLexicalForm(),
          value.getRDFLiteral().getLanguage(),
          value.getRDFLiteral().getDatatype());

    if (value.isRDFResource()) {
      if (value.getRDFResource().isAnon())
        return Node.createAnon(value.getRDFResource().getId());
      return Node.createURI(value.getRDFResource().getURI());
    }

    if (value.isURI())
      return Node.createURI(value.getURI());

    return Node.createLiteral(value.asUnquotedString(), null, null);
  }

  static RDFNode convertNodeToRDFNode(Node n, Model model) {
    if (n.isLiteral())
      return new LiteralImpl(n, model);

    if (n.isURI() || n.isBlank())
      return new ResourceImpl(n, model);

    if (n.isVariable()) {
      // Hack
      logger.error("Variable unbound: " + n);
      //binding.add(name, n) ;
      return null;
    }

    logger.error("Unknown node type for node: " + n);
    return null;

  }

  static Triple substituteIntoTriple(Triple t, ResultBinding binding) {
    if (binding == null)
      return t;

    boolean keep = true;
    Node subject = substituteNode(t.getSubject(), binding);
    Node predicate = substituteNode(t.getPredicate(), binding);
    Node object = substituteNode(t.getObject(), binding);

    if (subject == t.getSubject() &&
        predicate == t.getPredicate() &&
        object == t.getObject())
      return t;

    return new Triple(subject, predicate, object);
  }

  static Node substituteNode(Node n, ResultBinding binding) {
    if (!n.isVariable())
      return n;

    String name = ((Node_Variable) n).getName();
    Object obj = binding.get(name);
    if (obj == null)
      return n;

    if (obj instanceof RDFNode)
      return ((RDFNode) obj).asNode();

    if (obj instanceof Value)
      return convertValueToNode((Value) obj);

    logger.error("Unknown object in binding: ignored: " +
        obj.getClass().getName());
    return n;
  }
}
