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

package org.mulgara.store.tuples;

// Java 2 standard packages
import java.util.*;

// Third party packages
import org.apache.log4j.*;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.query.filter.Context;
import org.mulgara.query.filter.ContextOwner;
import org.mulgara.query.filter.Filter;
import org.mulgara.resolver.spi.QueryEvaluationContext;
import org.mulgara.resolver.spi.TuplesContext;
import org.mulgara.store.tuples.AbstractTuples;

/**
 * Filtering operation. This class wraps another Tuples, removing those elements that don't
 * pass the filter.
 */
public class FilteredTuples extends AbstractTuples implements ContextOwner {

  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(FilteredTuples.class.getName());

  /** The inner tuples to filter. */
  protected Tuples unfiltered;

  /** The filter to apply. */
  protected Filter filter;

  /** The tuples context */
  protected TuplesContext context;

  /**
   * Configure a tuples for filtering.
   *
   * @param unfiltered The original tuples.
   * @param filter The filter to apply.
   * @param queryContext The context to evaluate the tuples in.
   * @throws IllegalArgumentException If the <var>unfiltered</var> tuples is null.
   */
  FilteredTuples(Tuples unfiltered, Filter filter, QueryEvaluationContext queryContext) throws IllegalArgumentException {
    // store the operands
    this.filter = filter;
    this.unfiltered = (Tuples)unfiltered.clone();
    this.context = new TuplesContext(this.unfiltered, queryContext.getResolverSession());
    filter.setContextOwner(this);
    setVariables(this.unfiltered.getVariables());
  }


  /** {@inheritDoc} */
  public long getColumnValue(int column) throws TuplesException {
    return unfiltered.getColumnValue(column);
  }


  /** {@inheritDoc} */
  public long getRowUpperBound() throws TuplesException {
    return unfiltered.getRowUpperBound();
  }


  /** {@inheritDoc} */
  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return unfiltered.isColumnEverUnbound(column);
  }


  /** {@inheritDoc} */
  public Variable[] getVariables() {
    return unfiltered.getVariables();
  }


  /** {@inheritDoc} */
  public int getColumnIndex(Variable variable) throws TuplesException {
    return unfiltered.getColumnIndex(variable);
  }


  /** {@inheritDoc} */
  public boolean isMaterialized() {
    return false;
  }


  /** {@inheritDoc} */
  public boolean hasNoDuplicates() throws TuplesException {
    return unfiltered.hasNoDuplicates();
  }


  /** {@inheritDoc} */
  public RowComparator getComparator() {
    return unfiltered.getComparator();
  }


  /** {@inheritDoc} */
  public List<Tuples> getOperands() {
    return Collections.unmodifiableList(Arrays.asList(new Tuples[] {unfiltered}));
  }


  /** {@inheritDoc} */
  public boolean isUnconstrained() throws TuplesException {
    return unfiltered.isUnconstrained();
  }


  /** {@inheritDoc} */
  public void renameVariables(Constraint constraint) {
    unfiltered.renameVariables(constraint);
  }


  /** {@inheritDoc} */
  public void beforeFirst(long[] prefix, int suffixTruncation) throws TuplesException {
    unfiltered.beforeFirst(prefix, suffixTruncation);
  }

  
  /**
   * @return {@inheritDoc}
   * @throws TuplesException {@inheritDoc}
   */
  public boolean next() throws TuplesException {
    try {
      do {
        // move to the next on the unfiltered
        boolean currentNext = unfiltered.next();
        // Short-circuit execution if this tuples' cursor is after the last row
        if (!currentNext) return false;
        // check if the filter passes the current row on the unfiltered
      } while (!testFilter());
    } catch (QueryException qe) {
      throw new TuplesException("Unable to iterate to the next tuples element while filtering", qe);
    }

    return true;
  }


  /** {@inheritDoc} */
  public void close() throws TuplesException {
    unfiltered.close();
  }


  /** @return {@inheritDoc} */
  public Object clone() {
    FilteredTuples cloned = (FilteredTuples)super.clone();

    // Clone the mutable fields as well
    cloned.unfiltered = (Tuples)unfiltered.clone();
    cloned.context = new TuplesContext(cloned.unfiltered, context);
    return cloned;
  }


  /**
   * Tells a filter what the current context is.
   * @see org.mulgara.query.filter.ContextOwner#getCurrentContext()
   */
  public Context getCurrentContext() {
    return context;
  }


  /**
   * Allows the context to be set manually. This is not expected.
   * @see org.mulgara.query.filter.ContextOwner#setCurrentContext(org.mulgara.query.filter.Context)
   */
  public void setCurrentContext(Context context) {
    if (!(context instanceof TuplesContext)) throw new IllegalArgumentException("FilteredTuples can only accept a TuplesContext.");
    this.context = (TuplesContext)context;
  }

  /**
   * Tests a filter using the current context.
   * @return The test result.
   * @throws QueryException If there was an error accessing data needed for the test.
   */
  private boolean testFilter() throws QueryException {
    // re-root the filter expression to this Tuples
    filter.setContextOwner(this);
    return filter.test(context);
  }
}