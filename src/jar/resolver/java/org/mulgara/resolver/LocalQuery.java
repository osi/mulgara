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

package org.mulgara.resolver;

// Java 2 standard packages
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.Node;
import org.jrdf.graph.URIReference;

// Local packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.BlankNodeImpl;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.GlobalizeException;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.LocalizedTuples;
import org.mulgara.resolver.spi.QueryEvaluationContext;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.SymbolicTransformation;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.tuples.RestrictPredicateFactory;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.tuples.TuplesOperations;

/**
 * Localized version of a global {@link Query}.
 *
 * As well as providing coordinate transformation from global to local
 * coordinates, this adds methods to partially resolve the query.
 *
 * @created 2004-05-06
 * @author <a href="http://www.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.13 $
 * @modified $Date: 2005/06/09 09:26:02 $
 * @maintenanceAuthor $Author: raboczi $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class LocalQuery implements Cloneable
{
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(LocalQuery.class.getName());

  /** The current localisation/globalisation session.  */
  private final ResolverSession resolverSession;

  private LocalQueryResolver context;

  /**
   * Construct a database.
   *
   * @param query  the query to localize
   * @param resolverSession  the database session to localize the
   *   <var>query</var> against
   * @throws IllegalArgumentException if <var>query</var> or
   *   <var>resolverSession</var> are <code>null</code>
   * @throws LocalizeException if the <var>query</var> can't be localized
   */
  LocalQuery(ResolverSession resolverSession, DatabaseOperationContext context)
    throws LocalizeException, TuplesException
  {
    // Validate "resolverSession" parameter
    if (resolverSession == null) {
      throw new IllegalArgumentException("Null \"resolverSession\" parameter");
    }

    // Initialize fields
    this.context = new LocalQueryResolver(context, resolverSession);
    this.resolverSession = resolverSession;
    if (logger.isDebugEnabled()) {
      logger.debug("Constructed local query");
    }
  }

  /**
   * @return the solution to this query
   * @throws QueryException if resolution can't be obtained
   */
  Tuples resolveE(Query query) throws QueryException
  {
    if (query == null) {
      throw new IllegalArgumentException("Query null in LocalQuery::resolveE");
    }

    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Resolving query " + query);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Stacktrace: ", new Throwable());
      }

      Tuples result = ConstraintOperations.resolveConstraintExpression(context,
          query.getModelExpression(), query.getConstraintExpression());

      if (logger.isDebugEnabled()) {
        logger.debug("Tuples result = " + TuplesOperations.formatTuplesTree(result));
      }

      result = projectSelectClause(query, result);
      result = appendAggregates(query, result);
      result = applyHaving(query, result);
      result = orderResult(query, result);
      result = offsetResult(query, result);
      result = limitResult(query, result);

      return result;
    } catch (TuplesException et) {
      throw new QueryException("Failed to resolve query", et);
    }
  }


  private Tuples projectSelectClause(Query query, Tuples result) throws TuplesException
  {
    if (result.getRowCardinality() > Cursor.ZERO) {
      Tuples tmp = result;
      try {
        List variables = new ArrayList();

      /*
       * Note that this code need not concern itself with the order of the select-list,
       * only the contents.  The mapping is handled by the subsequent Answer object,
       * and only becomes important if the row-order is important and is therefore
       * deferred to order-by resolution.
       */
        Variable[] vars = result.getVariables();
        for (int i = 0; i < vars.length; i++) {
          if (query.getVariableList().contains(vars[i])) {
            variables.add(vars[i]);
          }
        }

        result = TuplesOperations.project(result, variables);
      } finally {
        tmp.close();
      }
    }

    return result;
  }


  private Tuples appendAggregates(Query query, Tuples result) throws TuplesException
  {
    if (result.getRowCardinality() != Tuples.ZERO) {
      Tuples tmp = result;
      result = new AppendAggregateTuples(resolverSession, context, result,
          filterSubqueries(query.getVariableList()));
      tmp.close();
    }

    return result;
  }

  private List filterSubqueries(List select) {
    List result = new ArrayList();
    for (Object o : select) {
      if (!(o instanceof Subquery)) {
        result.add(o);
      }
    }

    return result;
  }


  private Tuples applyHaving(Query query, Tuples result) throws TuplesException {
    ConstraintHaving having = query.getHavingExpression();
    Tuples tmp = result;
    if (having != null) {
      result = TuplesOperations.restrict(
                  result, RestrictPredicateFactory.getPredicate(having, resolverSession));
      tmp.close();
    }

    return result;
  }


  private Tuples orderResult(Query query, Tuples result) throws TuplesException, QueryException {
    List orderList = query.getOrderList();
    if (orderList.size() > 0 && result.getRowCardinality() > Cursor.ONE) {
      Tuples tmp = result;
      result = TuplesOperations.sort(result,
                 new OrderByRowComparator(result, orderList, resolverSession));
      tmp.close();
    }

    return result;
  }

  private Tuples offsetResult(Query query, Tuples result) throws TuplesException
  {
    int offset = query.getOffset();
    if (offset > 0) {
      Tuples tmp = result;
      result = TuplesOperations.offset(result, offset);
      tmp.close();
    }

    return result;
  }


  private Tuples limitResult(Query query, Tuples result)  throws TuplesException
  {
    Integer limit = query.getLimit();
    if (limit != null) {
      Tuples tmp = result;
      result = TuplesOperations.limit(result, limit.intValue());
      tmp.close();
    }

    return result;
  }
}
