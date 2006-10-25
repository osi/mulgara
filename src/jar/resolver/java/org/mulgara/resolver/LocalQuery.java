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
import org.mulgara.resolver.spi.MutableLocalQuery;
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

  /** The session this query is local to.  */
  private final DatabaseOperationContext context;

  /** The constraint expression. */
  private ConstraintExpression constraintExpression;

  /** The model expression. */
  private ModelExpression modelExpression;

  /** The having clause */
  private ConstraintHaving having;

  /** The various components of the select clause. */
  private final List orderList;
  private final int offset;
  private final Integer limit;
  private Tuples given;

  /** Variable list from select clause */
  private List select;

  private Map cachedResults;

  //
  // Constructor
  //

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
  LocalQuery(Query query, ResolverSession resolverSession, DatabaseOperationContext context)
    throws LocalizeException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Constructing local query for " + query);
    }

    // Validate "query" parameter
    if (query == null) {
      throw new IllegalArgumentException("Null \"query\" parameter");
    }

    // Validate "resolverSession" parameter
    if (resolverSession == null) {
      throw new IllegalArgumentException("Null \"resolverSession\" parameter");
    }

    // Initialize fields
    this.constraintExpression = query.getConstraintExpression();
    this.resolverSession = resolverSession;
    this.context = context;
    this.modelExpression = (ModelExpression)query.getModelExpression().clone();
    this.orderList = query.getOrderList();
    this.offset = query.getOffset();
    this.limit = query.getLimit();
    this.given = new LocalizedTuples(resolverSession, query.getGiven());
    this.having = query.getHavingExpression();
    this.select = query.getVariableList();
    this.cachedResults = new HashMap();

    if (logger.isDebugEnabled()) {
      logger.debug("Constructed local query");
    }
  }

  LocalQuery(LocalQuery localQuery, ConstraintExpression constraintExpression) {
    this.constraintExpression = constraintExpression;
    this.resolverSession = localQuery.resolverSession;
    this.context = localQuery.context;
    this.modelExpression = localQuery.modelExpression;
    this.orderList = localQuery.orderList;
    this.offset = localQuery.offset;
    this.limit = localQuery.limit;
    this.given = (Tuples)localQuery.given.clone();
    this.select = localQuery.select;
    this.cachedResults = new HashMap();
  }

  //
  // API methods
  //

  /**
   * Attempt to apply a symbolic query transformation.
   *
   * Symbolic transformations modify the values of query clauses without
   * resolving any {@link Constraint} into {@link Tuples}.
   *
   * @param symbolicTransformation  the transformation to apply, never
   *   <code>null</code>
   * @return <code>true</code> if the application modified this instance
   */
  boolean apply(SymbolicTransformation symbolicTransformation)
    throws QueryException
  {
    /*
    MutableLocalQuery mutableLocalQuery = this.new MutableLocalQueryImpl();
    symbolicTransformation.apply(mutableLocalQuery);
    return mutableLocalQuery.isModified();
    */
    return false;
  }

  Tuples resolve(Map outerBindings) throws QueryException
  {
    try {
      return context.innerCount(new LocalQuery(this,
          new ConstraintConjunction(ConstraintOperations.bindVariables(outerBindings, constraintExpression),
                                    constrainBindings(outerBindings))));
    } catch (LocalizeException el) {
      throw new QueryException("Failed to resolve inner local query", el);
    }
  }


  // FIXME: This method should be using a LiteralTuples.  Also I believe MULGARA_IS is now preallocated.
  // Someone needs to try making the change and testing.
  private ConstraintExpression constrainBindings(Map bindings) throws LocalizeException {
    List args = new ArrayList();
    Iterator i = bindings.entrySet().iterator();
    logger.info("FIXME:localize should be lookup, need to preallocate MULGARA_IS");
    while (i.hasNext()) {
      Map.Entry entry = (Map.Entry)i.next();
      args.add(ConstraintIs.newLocalConstraintIs(
                  (Variable)entry.getKey(),
                  new LocalNode(resolverSession.localize(ConstraintIs.MULGARA_IS)),
                  (Value)entry.getValue(),
                  null));
    }

    return new ConstraintConjunction(args);
  }


  Tuples resolve(ConstraintExpression whereExtension) throws QueryException {
    return resolve(constraintExpression, whereExtension);
  }


  Tuples resolve(Constraint constraint) throws QueryException {
    return context.resolve(constraint);
  }


  Tuples resolve(ConstraintExpression baseExpression, ConstraintExpression whereExtension) throws QueryException
  {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Resolving query " + modelExpression + " . " + constraintExpression);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Stacktrace: ", new Throwable());
      }

      ConstraintExpression tmpConstraint = new ConstraintConjunction(
          whereExtension, baseExpression);

      Tuples result = resolve(modelExpression, tmpConstraint);

      if (logger.isDebugEnabled()) {
        logger.debug("Tuples result = " + TuplesOperations.formatTuplesTree(result));
        logger.debug("Raw result " + result);
      }

      result = projectSelectClause(result);
      result = appendAggregates(result);
      result = applyHaving(result);
      result = orderResult(result);
      result = offsetResult(result);
      result = limitResult(result);

      return result;
    } catch (TuplesException et) {
      throw new QueryException("Failed to resolve query", et);
    }
  }


  /**
   * @return the solution to this query
   * @throws QueryException if resolution can't be obtained
   */
  Tuples resolve() throws QueryException
  {
    try {
      return resolve(new ConstraintConjunction(new ArrayList()));
    } catch (QueryException eq) {
      logger.warn("QueryException thrown in resolve: ", eq);
      throw eq;
    } catch (Exception e) {
      logger.warn("Exception thrown in resolve: ", e);
      throw new QueryException("Exception thrown in resolve", e);
    }
  }


  private Tuples projectSelectClause(Tuples result) throws TuplesException
  {
    if (result.getRowCardinality() > Cursor.ZERO) {
      Tuples tmp = result;
      try {
        List variables = new ArrayList(select.size());

      /*
       * Note that this code need not concern itself with the order of the select-list,
       * only the contents.  The mapping is handled by the subsequent Answer object,
       * and only becomes important if the row-order is important and is therefore
       * deferred to order-by resolution.
       */
        Variable[] vars = result.getVariables();
        for (int i = 0; i < vars.length; i++) {
          if (select.contains(vars[i])) {
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


  private Tuples appendAggregates(Tuples result) throws TuplesException
  {
    if (result.getRowCardinality() != Tuples.ZERO) {
      Tuples tmp = result;
      result = new AppendAggregateTuples(resolverSession, context, result, filterSubqueries(select));
      tmp.close();
    }

    return result;
  }

  private List filterSubqueries(List select) {
    List result = new ArrayList();
    Iterator i = select.iterator();
    while (i.hasNext()) {
      Object o = i.next();
      if (!(o instanceof Subquery)) {
        result.add(o);
      }
    }

    return result;
  }


  private Tuples applyHaving(Tuples result) throws TuplesException {
    Tuples tmp = result;
    if (having != null) {
      result = TuplesOperations.restrict(
                  result, RestrictPredicateFactory.getPredicate(having, resolverSession));
      tmp.close();
    }

    return result;
  }


  private Tuples orderResult(Tuples result) throws TuplesException, QueryException {
    if (orderList.size() > 0 && result.getRowCardinality() > Cursor.ONE) {
      Tuples tmp = result;
      result = TuplesOperations.sort(result,
                 new OrderByRowComparator(result, orderList, resolverSession));
      tmp.close();
    }

    return result;
  }

  private Tuples offsetResult(Tuples result) throws TuplesException
  {
    if (offset > 0) {
      Tuples tmp = result;
      result = TuplesOperations.offset(result, offset);
      tmp.close();
    }

    return result;
  }


  private Tuples limitResult(Tuples result)  throws TuplesException
  {
    if (limit != null) {
      Tuples tmp = result;
      result = TuplesOperations.limit(result, limit.intValue());
      tmp.close();
    }

    return result;
  }

  //
  // Internal methods
  //

  /**
   * Localize and resolve the <code>FROM</code> and <code>WHERE</code> clause
   * product.
   *
   * @param modelExpression the <code>FROM<code> clause to resolve
   * @param constraintExpression the <code>WHERE</code> clause to resolve
   * @throws QueryException if resolution can't be obtained
   */
  Tuples resolve(ModelExpression      modelExpression,
                 ConstraintExpression constraintExpression)
    throws QueryException
  {
    QueryEvaluationContext context = new LocalQueryResolver(this, resolverSession);

    return ConstraintOperations.resolveConstraintExpression(context, modelExpression, constraintExpression);
  }


  ResolverSession getResolverSession() {
    return resolverSession;
  }


  public Object clone()
  {
    try {
      LocalQuery query = (LocalQuery)super.clone();
      query.modelExpression = (ModelExpression)modelExpression.clone();
      query.given = (Tuples)given.clone();

      return query;
    } catch (CloneNotSupportedException ec) {
      throw new Error("Object threw CloneNotSupportedException", ec);
    }
  }


  public void close() throws QueryException
  {
    try {
      given.close();
    } catch (TuplesException et) {
      throw new QueryException("Failed to close given clause", et);
    }
  }

  public String toString()
  {
    return "where " + constraintExpression;
  }

  /**
   * Mutator for {@link LocalQuery}.
   */
  class MutableLocalQueryImpl implements MutableLocalQuery
  {
    private boolean closed = false;
    private boolean modified = false;

    /**
     * Once called, this instance can no longer be used for modifications.
     */
    void close()
    {
      closed = true;
    }

    /**
     * @return whether this instance has been used to mutate the value of the
     *   outer class
     */
    boolean isModified()
    {
      return modified;
    }

    //
    // Methods implementing LocalQuery
    //

    public ConstraintExpression getConstraintExpression()
    {
      return constraintExpression;
    }

    public void setConstraintExpression(ConstraintExpression constraintExpression)
    {
      // Validate state
      if (closed) {
        throw new IllegalStateException();
      }

      // Validate "constraintExpression" parameter
      if (constraintExpression == null) {
        throw new IllegalArgumentException("Null \"constraintExpression\" parameter");
      }

      // Update fields
      LocalQuery.this.constraintExpression = constraintExpression;
      modified = true;
    }

    //
    // Methods overriding Object
    //

    public String toString()
    {
      return LocalQuery.this.toString();
    }
  }
}
