/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */

package org.mulgara.resolver.nullres;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.mulgara.query.Constraint;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.store.tuples.Annotation;
import org.mulgara.store.tuples.RowComparator;
import org.mulgara.store.tuples.Tuples;

/**
 * An empty set of results, equivalent to {@link org.mulgara.store.tuples.EmptyTuples}.
 *
 * @created May 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class NullResolution implements Resolution {

  /** Logger. */
  private static Logger logger = Logger.getLogger(NullResolver.class.getName());

  /** The constraint.  */
  private final Constraint constraint;

  /**
   * Constructs this resolution around a constraint.
   * @param constraint The constraint that resulted in this empty resolution.
   */
  NullResolution(Constraint constraint) {
    this.constraint = constraint;
  }

  /** @see org.mulgara.resolver.spi.Resolution#getConstraint() */
  public Constraint getConstraint() {
    return constraint;
  }

  /** @see org.mulgara.resolver.spi.Resolution#isComplete() */
  public boolean isComplete() {
    return true;
  }

  /** @see org.mulgara.store.tuples.Tuples#beforeFirst(long[], int) */
  public void beforeFirst(long[] prefix, int suffixTruncation) throws TuplesException {
    // no-op
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getAnnotation(java.lang.Class)
   * @return Always <code>null</code>.
   */
  public Annotation getAnnotation(Class<?> annotationClass) throws TuplesException {
    return null;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getColumnIndex(org.mulgara.query.Variable)
   * @throws TuplesException Always thrown, as this result has no variables.
   */
  public int getColumnIndex(Variable variable) throws TuplesException {
    throw new TuplesException("variable doesn't match any column");
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getColumnValue(int)
   * @throws TuplesException Always thrown, as this result has no columns.
   */
  public long getColumnValue(int column) throws TuplesException {
    throw new TuplesException("Column index out of range: " + column);
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getComparator()
   * @return Always <code>null</code>.
   */
  public RowComparator getComparator() {
    return null;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getOperands()
   * @return An empty list.
   */
  public List<Tuples> getOperands() {
    return Collections.emptyList();
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getRawColumnValue(int)
   * @throws TuplesException Always thrown, as this result has no columns.
   */
  public long getRawColumnValue(int column) throws TuplesException {
    throw new TuplesException("Column index out of range: " + column);
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getRowCount()
   * @return Always 0 rows.
   */
  public long getRowCount() throws TuplesException {
    return 0;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#getVariables()
   * @return an empty {@link Variable} array.
   */
  public Variable[] getVariables() {
    return new Variable[0];
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#hasNoDuplicates()
   * @return Always <code>true</code> as there are no duplicates when there is no data.
   */
  public boolean hasNoDuplicates() throws TuplesException {
    return true;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#isColumnEverUnbound(int)
   * @return Always <code>false</code> as there are no variables.
   */
  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return false;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#isMaterialized()
   * @return Always <code>true</code> as this is no need to materialize.
   */
  public boolean isMaterialized() {
    return true;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#isUnconstrained()
   * @return Always <code>false</code> as there are no variables.
   */
  public boolean isUnconstrained() throws TuplesException {
    return false;
  }

  /**
   * @see org.mulgara.store.tuples.Tuples#next()
   * @return Always <code>false</code> to indicate no data.
   */
  public boolean next() throws TuplesException {
    return false;
  }

  /** @see org.mulgara.store.tuples.Tuples#renameVariables(org.mulgara.query.Constraint) */
  public void renameVariables(Constraint constraint) { /* no-op */ }

  /** @see org.mulgara.query.Cursor#beforeFirst() */
  public void beforeFirst() throws TuplesException { /* no-op */ }

  /** @see org.mulgara.query.Cursor#close() */
  public void close() throws TuplesException { /* no-op */ }

  /** @see org.mulgara.query.Cursor#getNumberOfVariables() */
  public int getNumberOfVariables() {
    return 0;
  }

  /** @see org.mulgara.query.Cursor#getRowCardinality() */
  public int getRowCardinality() throws TuplesException {
    return 0;
  }

  /** @see org.mulgara.query.Cursor#getRowUpperBound() */
  public long getRowUpperBound() throws TuplesException {
    return 0;
  }

  /**
   * All implementations must support cloning.
   * @return the cloned instance
   */
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      logger.error("Unexpected cloning error", e);
      return new NullResolution(constraint);
    }
  }

}
