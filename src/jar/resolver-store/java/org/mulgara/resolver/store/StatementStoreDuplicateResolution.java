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
 * Contributor(s):
 *   getModel() contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver.store;

// Third party packages
import org.apache.log4j.Logger;

// Standard Java packages
import java.util.*;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.statement.StatementStore;
import org.mulgara.store.statement.StatementStoreException;
import org.mulgara.store.tuples.AbstractTuples;
import org.mulgara.store.tuples.StoreTuples;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.tuples.TuplesOperations;

/**
 * Tuples backed by the graph, corresponding to a particular constraint.
 *
 * It differs from {@link StatementStoreResolution} in that it handles
 * constraints with duplicate variable bindings.  That is,
 * <code>$x $x $x</code> which will return all the statements that have the
 * same value in all three places.  The class retains the original constraint
 * so that the graph index it's resolved against can be resolved anew as its
 * variables are bound.
 *
 * TO DO: Support <code>mulgara:is</code> re-binding.
 *
 * @created 2004-08-26
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/05/02 20:07:58 $ by $Author: raboczi $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class StatementStoreDuplicateResolution extends AbstractTuples implements Resolution {

  protected static Logger logger = Logger.getLogger(StatementStoreDuplicateResolution.class);

  /** Indicates ordering by subject */
  private final static int MASK0 = 1;

  /** Indicates ordering by predicate */
  private final static int MASK1 = 2;

  /** Indicates ordering by object */
  private final static int MASK2 = 4;

  /** Indicates ordering by graph */
  private final static int MASK3 = 8;

  /**
   * The constraint these tuples were generated to satisfy.
   */
  protected Constraint constraint;

  /**
   * The graph from which these tuples were generated.
   */
  protected StatementStore store;

  /**
   * The tuples.
   */
  private Tuples baseTuples, uniqueTuples;

  /**
   * Which columns s, p, o are the same.
   */
  private boolean[] sameColumns;

  /**
   * Whether we've calculated the rows.
   */
  private boolean calculatedRowCount;

  /**
   * An array that contains the position in the tuples.  Elements 0, 1, 2, 3
   * map to subject, predicate, object and meta and the value in the array is
   * the column position.
   */
  private int[] columnOrder;

  /** A mapping between the virtual column numbers and the internal column numbers. */
  private int[] columnMap;

  private boolean hasNext = false;

  private List variableList;

  private TuplesEvaluator tuplesEvaluator;

  /**
   * Construct a tuples with node numbers and local tuples objects.
   *
   * @param newTuples a local tuples representing a graph.
   */
  StatementStoreDuplicateResolution(boolean[] newSameColumns, Tuples newTuples,
      int[] newColumnOrder) {

    baseTuples = newTuples;
    sameColumns = newSameColumns;
    columnOrder = newColumnOrder;

    // Get variable list.
    variableList = new LinkedList();
    Variable[] baseVars = baseTuples.getVariables();
    for (int index = 0; index < sameColumns.length; index++) {
      if (sameColumns[index]) variableList.add(baseVars[index]);
    }
    // do 1:1 mapping as this is expected in the tests - variable names are not duplicated
    columnMap = new int[baseVars.length];
    for (int i = 0; i < columnMap.length; i++) columnMap[i] = i;

    // Project for unique variables.
    try {
      uniqueTuples = TuplesOperations.project(baseTuples, variableList);
    }
    catch (TuplesException te) {
      logger.error("Failed to get unique tuples", te);
    }

    columnOrder = newColumnOrder;
    setVariables(newTuples.getVariables());
    setTuplesEvaluator();
  }

  /**
   * Find a graph index that satisfies a constraint.
   *
   * @param newConstraint the constraint to satisfy
   * @param newStore the store to resolve against
   * @throws IllegalArgumentException if <var>constraint</var> or <var>graph
   *      </var> is <code>null</code>
   * @throws TuplesException EXCEPTION TO DO
   */
  StatementStoreDuplicateResolution(Constraint newConstraint, StatementStore newStore)
      throws TuplesException {

    try {
      constraint = newConstraint;
      store = newStore;

      // Setup which columns are duplicates.
      boolean equalSubject = constraint.getElement(0).equals(constraint.getElement(1)) ||
          constraint.getElement(0).equals(constraint.getElement(2));
      boolean equalPredicate = constraint.getElement(1).equals(constraint.getElement(0)) ||
          constraint.getElement(1).equals(constraint.getElement(2));
      boolean equalObject = constraint.getElement(2).equals(constraint.getElement(0)) ||
          constraint.getElement(2).equals(constraint.getElement(1));
      sameColumns = new boolean[] { equalSubject, equalPredicate,
          equalObject };

      // Setup which are the constants and which are the variables.
      long subject = toGraphTuplesIndex(constraint.getElement(0));
      long predicate = toGraphTuplesIndex(constraint.getElement(1));
      long object = toGraphTuplesIndex(constraint.getElement(2));
      long meta = toGraphTuplesIndex(constraint.getModel());

      final long VAR = NodePool.NONE;
      if (meta != VAR && subject == VAR && predicate == VAR && object == VAR) {
        // all variables except the graph. Select explicit ordering.
        int mask = MASK3;
        if (equalSubject) mask |= MASK0;
        if (equalPredicate) mask |= MASK1;
        if (equalObject) mask |= MASK2;
        baseTuples = store.findTuples(mask, subject, predicate, object, meta);
      } else {
        // Return the tuples using the s, p, o hints.
        baseTuples = store.findTuples(subject, predicate, object, meta);
      }

      // Get the unique values for the multiply constrained column.
      variableList = new LinkedList();
      for (int index = 0; index < sameColumns.length; index++) {
        if (sameColumns[index]) {
          variableList.add(StatementStore.VARIABLES[index]);
        }
      }
      uniqueTuples = TuplesOperations.project(baseTuples, variableList);

      // Create column map.
      columnOrder = new int[4];
      int[] inverseColumnOrder = ((StoreTuples)baseTuples).getColumnOrder();
      for (int index = 0; index < inverseColumnOrder.length; index++) {
        columnOrder[inverseColumnOrder[index]] = index;
      }

      // Set the variables. Using an ordered set.
      Set uniqueVariables = new LinkedHashSet();
      columnMap = new int[2];
      for (int index = 0; index < baseTuples.getVariables().length; index++) {
        Object obj = constraint.getElement(index);
        if (obj instanceof Variable) {
          if (!((Variable) obj).equals(Variable.FROM)) {
            if (!uniqueVariables.contains(obj)) columnMap[uniqueVariables.size()] = columnOrder[index];
            uniqueVariables.add(obj);
          }
        }
      }
      setVariables((Variable[]) uniqueVariables.toArray(new Variable[uniqueVariables.size()]));

      // Set up the tuples evaluator.
      setTuplesEvaluator();
    }
    catch (StatementStoreException se) {
      throw new TuplesException("Failed to set-up tuples", se);
    }
  }

  /**
   * Create a new tuples evaluator depending on how many columns to bind.
   */
  public void setTuplesEvaluator() {
    if (variableList.size() == 2) {
      tuplesEvaluator = new TwoConstrainedTuplesEvaluator();
    }
    else {
      tuplesEvaluator = new ThreeConstrainedTuplesEvaluator();
    }
  }

  public long getRowCount() throws TuplesException {
    return tuplesEvaluator.getRowCount();
  }

  public boolean hasNoDuplicates() throws TuplesException {
    return baseTuples.hasNoDuplicates();
  }

  public List getOperands() {
    return new ArrayList();
  }

  public void beforeFirst(long[] prefix, int suffixTruncation) throws TuplesException {
    if (prefix.length > 4) {
      throw new TuplesException("Prefix too long");
    }
    tuplesEvaluator.beforeFirst(prefix, suffixTruncation);
  }

  public void close() throws TuplesException {
    try {
      if (baseTuples != null) {
        baseTuples.close();
      }
    }
    finally {
      if (uniqueTuples != null) {
        uniqueTuples.close();
      }
    }
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public Object clone() {
    StatementStoreDuplicateResolution cloned = (StatementStoreDuplicateResolution) super.clone();
    cloned.baseTuples = (Tuples) baseTuples.clone();
    cloned.uniqueTuples = (Tuples) uniqueTuples.clone();
    cloned.setVariables(getVariables());
    cloned.setTuplesEvaluator();
//    cloned.constraint = constraint;
    return cloned;
  }


  public long getRowUpperBound() throws TuplesException {
    return getRowCount();
  }

  public int getRowCardinality() throws TuplesException {
    long count = getRowCount();
    if (count > 1) {
      return Cursor.MANY;
    }
    switch ((int) count) {
      case 0:
        return Cursor.ZERO;
      case 1:
        return Cursor.ONE;
      default:
        throw new TuplesException("Illegal row count: " + count);
    }
  }

  public Constraint getConstraint() {
    return constraint;
  }

  public long getColumnValue(int column) throws TuplesException {
    return baseTuples.getColumnValue(columnMap[column]);
  }

  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return baseTuples.isColumnEverUnbound(columnMap[column]);
  }

  public boolean isUnconstrained() throws TuplesException {
    return baseTuples.isUnconstrained();
  }

  public boolean isMaterialized() {
    return baseTuples.isMaterialized();
  }

  public boolean next() throws TuplesException {
    return tuplesEvaluator.next();
  }

  public boolean isComplete() {
    return true;
  }


  /**
   * A decorator around the getRowCount(), next() and beforeFirst() methods on
   * a tuples correctly sets the searching tuples and performs the correct next
   * operation.
   */
  private interface TuplesEvaluator {

    /**
     * Returns the row count.
     *
     * @return the row count.
     * @throws TuplesException if there was an error accessing the tuples.
     */
    public long getRowCount() throws TuplesException;

    /**
     * Calls before first using a given prefix.
     *
     * @param prefix long[] the prefix to jump to.
     * @param suffixTruncation the suffixes to truncate.
     * @throws TuplesException if there was a problem accessing the tuples.
     */
    public void beforeFirst(long[] prefix, int suffixTruncation)
        throws TuplesException;

    /**
     * Returns true if there is another tuples.
     *
     * @throws TuplesException if there was a problem accessing the tuples.
     * @return true if there is another tuples.
     */
    public boolean next() throws TuplesException;
  }

  /**
   * Operates on a tuples where two of the constraints are equal.
   */
  private class TwoConstrainedTuplesEvaluator implements TuplesEvaluator {

    public long getRowCount() throws TuplesException {

      // Only calculate rows once.
      if (!calculatedRowCount) {

        // Start with total number of tuples.
        Tuples tmpUniqueTuples = (Tuples) uniqueTuples.clone();
        Tuples tmpTuples = (Tuples) baseTuples.clone();
        tmpUniqueTuples.beforeFirst();
        tmpTuples.beforeFirst();

        rowCount = 0;

        while (tmpUniqueTuples.next()) {
          if (tmpUniqueTuples.getColumnValue(0) == tmpUniqueTuples.getColumnValue(1)) {
            tmpTuples.beforeFirst(new long[] {
                tmpUniqueTuples.getColumnValue(0),
                tmpUniqueTuples.getColumnValue(1) }, 0);
            while (tmpTuples.next()) {
              rowCount++;
            }
          }
        }

        // Ensure we don't calculate the rows again.
        calculatedRowCount = true;
      }
      return rowCount;
    }

    public void beforeFirst(long[] prefix, int suffixTruncation)
        throws TuplesException {

      // create a new prefix for uniqueTuples
      // this only includes columns from the initial prefix that represented duplicate variables
      // takes advantage of the fact that variables came out in the same order as the initial constraint
      // also, sameColumns offsets are same as baseTuples offsets
      long[] duplicatesPrefix = new long[] {};
      for (int i = 0; i < prefix.length; i++) {
        // i is an index into the virtual tuples. Map it to the original columns
        if (sameColumns[columnMap[i]]) {
          // only the first of the duplicated columns was mapped
          duplicatesPrefix = new long[] { prefix[i], prefix[i] };
          break;
        }
      }

      // Get the subject/predicate before first.
      uniqueTuples.beforeFirst(duplicatesPrefix, suffixTruncation);

      hasNext = uniqueTuples.next();
      while (hasNext && (uniqueTuples.getColumnValue(0) != uniqueTuples.getColumnValue(1))) {
        hasNext = uniqueTuples.next();
      }

      // Go to the first tuples. Ignoring suffix truncation.
      if (hasNext) {
        if (prefix.length <= 1) {
          baseTuples.beforeFirst(new long[] { uniqueTuples.getColumnValue(0), uniqueTuples.getColumnValue(1) }, 0);
        } else {
          assert prefix.length == 2;
          baseTuples.beforeFirst(new long[] { uniqueTuples.getColumnValue(0), uniqueTuples.getColumnValue(1), prefix[1] }, 0);
        }
      }
   }

    public boolean next() throws TuplesException {

      // Check that there are more tuples - assume tuples are in order.
      if (hasNext) {

        boolean hasNextBase = baseTuples.next();

        // Check if we still have a match, if not get the next match.
        if (!hasNextBase ||
            !((uniqueTuples.getColumnValue(0) == baseTuples.getColumnValue(0)) &&
            (uniqueTuples.getColumnValue(1) == baseTuples.getColumnValue(1)))) {

          // Get the next item to search.
          while (uniqueTuples.next()) {

            // Check to see that there is a next value.  There will be at least
            // one value as the items to search are based on the original tuples.
            if (uniqueTuples.getColumnValue(0) == uniqueTuples.getColumnValue(1)) {
              baseTuples.beforeFirst(new long[] { uniqueTuples.getColumnValue(0),
                  uniqueTuples.getColumnValue(1) }, 0);
              hasNextBase = baseTuples.next();
              if (hasNextBase) {
                break;
              }
            }
          }
        }
        hasNext = hasNextBase;
      }
      return hasNext;
    }
  }

  /**
   * Operates on a tuples where all three of the constraints are equal.
   */
  private class ThreeConstrainedTuplesEvaluator implements TuplesEvaluator {

    public long getRowCount() throws TuplesException {

      // Only calculate rows once.
      if (!calculatedRowCount) {

        // Start with total number of tuples.
        Tuples tmpSubjPredTuples = (Tuples) uniqueTuples.clone();
        Tuples tmpTuples = (Tuples) baseTuples.clone();
        tmpSubjPredTuples.beforeFirst();
        tmpTuples.beforeFirst();

        rowCount = 0;

        while (tmpSubjPredTuples.next()) {
          if ((tmpSubjPredTuples.getColumnValue(0) == tmpSubjPredTuples.getColumnValue(1)) &&
             (tmpSubjPredTuples.getColumnValue(1) == tmpSubjPredTuples.getColumnValue(2))) {
            tmpTuples.beforeFirst(new long[] {
                tmpSubjPredTuples.getColumnValue(0),
                tmpSubjPredTuples.getColumnValue(1),
                tmpSubjPredTuples.getColumnValue(2) }, 0);
            while (tmpTuples.next()) {
              rowCount++;
            }
          }
        }

        // Ensure we don't calculate the rows again.
        calculatedRowCount = true;
      }
      return rowCount;
    }

    public void beforeFirst(long[] prefix, int suffixTruncation)
        throws TuplesException {

      // Get the subject/predicate before first.
     uniqueTuples.beforeFirst(prefix, suffixTruncation);
     baseTuples.beforeFirst(prefix, suffixTruncation);
     hasNext = uniqueTuples.next();

     while (hasNext &&
         ((uniqueTuples.getColumnValue(0) != uniqueTuples.getColumnValue(1)) ||
          (uniqueTuples.getColumnValue(1) != uniqueTuples.getColumnValue(2)))) {
       hasNext = uniqueTuples.next();
     }

     // Go to the first tuples.
     if (hasNext) {
       baseTuples.beforeFirst(new long[] { uniqueTuples.getColumnValue(0),
           uniqueTuples.getColumnValue(1), uniqueTuples.getColumnValue(2) },
           0);
     }
   }

    public boolean next() throws TuplesException {

      // Check that there are more tuples - assume tuples are in order.
      if (hasNext) {

        boolean hasNextBase = baseTuples.next();

        // Check if we still have a match, if not get the next match.
        if (!hasNextBase ||
            !((uniqueTuples.getColumnValue(0) == baseTuples.getColumnValue(0)) &&
            (uniqueTuples.getColumnValue(1) == baseTuples.getColumnValue(1)) &&
            (uniqueTuples.getColumnValue(2) == baseTuples.getColumnValue(2)))) {

          // Get the next item to search.
          while (uniqueTuples.next()) {

            // Check to see that there is a next value.  There will be at least
            // one value as the items to search are based on the original tuples.
            if ((uniqueTuples.getColumnValue(0) == uniqueTuples.getColumnValue(1)) &&
                (uniqueTuples.getColumnValue(1) == uniqueTuples.getColumnValue(2))) {
              baseTuples.beforeFirst(new long[] { uniqueTuples.getColumnValue(0),
                  uniqueTuples.getColumnValue(1),
                  uniqueTuples.getColumnValue(2) }, 0);
              hasNextBase = baseTuples.next();
              if (hasNextBase) {
                break;
              }
            }
          }
        }
        hasNext = hasNextBase;
      }
      return hasNext;
    }
  }

  /**
   * Returns the long representation of the constraint element or
   * NodePool.NONE if a variable.
   *
   * @param constraintElement the constraint element to resolve.
   * @throws TuplesException if the constraint element is not supported.
   * @return long the long representation of the constraint element.
   */
  protected static long toGraphTuplesIndex(ConstraintElement constraintElement)
      throws TuplesException {
    if (constraintElement instanceof Variable) {
      return NodePool.NONE;
    }
    if (constraintElement instanceof LocalNode) {
      return ((LocalNode) constraintElement).getValue();
    }

    throw new TuplesException("Unsupported constraint element: " +
        constraintElement + " (" + constraintElement.getClass() + ")");
  }
}
