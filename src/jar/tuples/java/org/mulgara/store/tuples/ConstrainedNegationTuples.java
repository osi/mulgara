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
 *   getModel() contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.store.tuples;

// Third party packages
import org.apache.log4j.Logger;

// Standard Java packages
import java.util.*;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.statement.StatementStore;
import org.mulgara.store.statement.StatementStoreException;

/**
 * Tuples backed by the graph, corresponding the tuples that don't match
 * a particular constraint. This class retains the original constraint so that
 * the graph index it's resolved against can be resolved anew as its variables
 * are bound.
 *
 * @created 2004-08-04
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/02/22 08:17:04 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class ConstrainedNegationTuples extends AbstractTuples {

  protected static final Logger logger = Logger.getLogger(ConstrainedNegationTuples.class);

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
  private Tuples tuples;

  /**
   * The long representation of the subject node or NodePool.NONE
   */
  private long subject;

  /**
   * The long representation of the predicate node or NodePool.NONE
   */
  private long predicate;

  /**
   * The long representation of the object node or NodePool.NONE
   */
  private long object;

  /**
   * The long representation of the meta node or NodePool.NONE
   */
  private long metanode;

  /**
   * Prefix.  The prefix into the tuples to the section to exclude.
   */
  private long[] excludePrefix;

  /**
   * The not evaluator for a given tuples.
   */
  private TuplesEvaluator tuplesEvaluator;

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

  /**
   * True when we have skipped over the block in the store.
   */
  private boolean skipped = false;

  /**
   * True if all constraints are fixed.
   */
  private boolean allFixedConstraints;

  /**
   * True if a single statement given by the constraints exists.
   */
  private boolean singleStatementExists;

  /**
   * Construct a tuples with node numbers and local tuples objects.
   *
   * @param newSubject the subject number.
   * @param newPredicate the predicate number.
   * @param newObject the object number.
   * @param newTuples a local tuples representing a graph.
   * @param newColumnOrder the column bindings - element 0 says which column
   *   the subject is in, element 1 says which column the predicate is in and
   *   so forth.
   */
  ConstrainedNegationTuples(long newSubject, long newPredicate,
      long newObject, Tuples newTuples, int[] newColumnOrder) {

    subject = newSubject;
    predicate = newPredicate;
    object = newObject;

    allFixedConstraints = (subject != NodePool.NONE) &&
        (predicate != NodePool.NONE) && (object != NodePool.NONE);

    tuples = newTuples;
    try {
      if (allFixedConstraints) {
        tuples.beforeFirst(new long[] { subject, predicate, object}, 0);
        singleStatementExists = tuples.next();
      }
      else {
        tuples.beforeFirst();
      }
    }
    catch (TuplesException te) {
      logger.error(te);
    }
    columnOrder = newColumnOrder;
    setTuplesEvaluator();
    setVariables(newTuples.getVariables());
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
  ConstrainedNegationTuples(Constraint newConstraint, StatementStore newStore)
      throws TuplesException {

    if (newStore == null) {
      throw new IllegalArgumentException("Cannot give null statementstore");
    }

    constraint = newConstraint;
    store = newStore;

    subject = toGraphTuplesIndex(constraint.getElement(0));
    predicate = toGraphTuplesIndex(constraint.getElement(1));
    object = toGraphTuplesIndex(constraint.getElement(2));
    metanode = toGraphTuplesIndex(constraint.getModel());

    allFixedConstraints = (subject != NodePool.NONE) &&
        (predicate != NodePool.NONE) && (object != NodePool.NONE);

    try {

      if (allFixedConstraints) {
        singleStatementExists = store.existsTriples(subject, predicate, object,
            metanode);
      }

      // Return the tuples using the s, p, o hints.
      tuples = store.findTuples(subject != NodePool.NONE,
          predicate != NodePool.NONE,
          object != NodePool.NONE,
          metanode != NodePool.NONE);

      tuples.beforeFirst();

      // Get the column order - this is column index -> tuples index we want
      // tuple index -> column index
      int[] inverseColumnOrder = ((StoreTuples) tuples).getColumnOrder();

      columnOrder = new int[4];

      // Create column map.
      for (int index = 0; index < inverseColumnOrder.length; index++) {
        columnOrder[inverseColumnOrder[index]] = index;
      }

      // Prepopulate variable list.
      List variableList = new ArrayList(Arrays.asList(StatementStore.VARIABLES));
      boolean changedList = false;

      // Create correct variable bindings in order.
      for (int index = 0; index < 4; index++) {
        if (constraint.getElement(index) instanceof Variable) {
          Variable var = (Variable) constraint.getElement(index);
          if (!var.equals(Variable.FROM)) {

            // Remove it from it's position in the list and re-add it with the
            // correct value.
            changedList = true;
            variableList.remove(columnOrder[index]);
            variableList.add(columnOrder[index], var);
          }
        }
      }

      setTuplesEvaluator();
      setVariables(variableList);
    }
    catch (StatementStoreException se) {
      throw new TuplesException("Failed to set-up tuples", se);
    }
  }

  /**
   * Create the appropriate tuples evaluator.
   */
  private void setTuplesEvaluator() {

    if (((subject != NodePool.NONE) && (predicate == NodePool.NONE) && (object == NodePool.NONE)) ||
      ((subject != NodePool.NONE) && (predicate != NodePool.NONE) && (object == NodePool.NONE)) ||
      ((subject != NodePool.NONE) && (predicate != NodePool.NONE) && (object != NodePool.NONE))) {
      tuplesEvaluator = new SPOTuplesEvaluator();

      if ((subject != NodePool.NONE) && (predicate == NodePool.NONE) && (object == NodePool.NONE)) {
        excludePrefix = new long[] { subject };
      }
      else if ((subject != NodePool.NONE) && (predicate != NodePool.NONE) && (object == NodePool.NONE)) {
        excludePrefix = new long[] { subject, predicate };
      }
      else if ((subject != NodePool.NONE) && (predicate != NodePool.NONE) && (object != NodePool.NONE)) {
        excludePrefix = new long[] { subject, predicate, object };
      }
    }
    else if (((subject == NodePool.NONE) && (predicate != NodePool.NONE) && (object == NodePool.NONE)) ||
      ((subject == NodePool.NONE) && (predicate != NodePool.NONE) && (object != NodePool.NONE))) {
      tuplesEvaluator = new POSTuplesEvaluator();
      if ((subject == NodePool.NONE) && (predicate != NodePool.NONE) && (object == NodePool.NONE)) {
        excludePrefix = new long[] { predicate };
      }
      else if ((subject == NodePool.NONE) && (predicate != NodePool.NONE) && (object != NodePool.NONE)) {
        excludePrefix = new long[] { predicate, object };
      }
    }
    else if ((subject == NodePool.NONE) && (predicate == NodePool.NONE) && (object != NodePool.NONE)) {
      tuplesEvaluator = new OSPTuplesEvaluator();
      excludePrefix = new long[] { object };
    }
    else if ((subject != NodePool.NONE) && (predicate == NodePool.NONE) && (object != NodePool.NONE)) {
      tuplesEvaluator = new SOPTuplesEvaluator();
      excludePrefix = new long[] { subject, object };
    }
    else if ((subject == NodePool.NONE) && (predicate == NodePool.NONE) && (object == NodePool.NONE)) {
      tuplesEvaluator = new UnconstrainedTuplesEvaluator();
      excludePrefix = new long[] {};
    }
  }

  public long getRowCount() throws TuplesException {

    // Only calculate rows once.
    if (!calculatedRowCount) {

      // Start with total number of tuples.
      rowCount = tuples.getRowCount();

      // A counter of what to subtract from total number of tuples.
      long counter = 0;

      // If the exclude prefix is not empty calculate otherwise we've asked
      // for a NOT ($s $p $o) which is 0.
      if (excludePrefix.length > 0) {

        Tuples tmpTuples = null;
        try {

          // Clone the tuples and start at the exclusion prefix.
          tmpTuples = (Tuples) tuples.clone();
          tmpTuples.beforeFirst(excludePrefix, 0);

          // Start at the prefix and count how many tuples there are.
          for (int prefixIndex = 0; prefixIndex < excludePrefix.length;
              prefixIndex++) {
            long columnValue = excludePrefix[prefixIndex];

            while (tmpTuples.next() &&
                tmpTuples.getColumnValue(prefixIndex) == columnValue) {
              counter++;
            }
          }
        }
        finally {
          if (tmpTuples != null) {
            tmpTuples.close();
          }
        }

        rowCount = rowCount - counter;
      }
      else {
        rowCount = 0;
      }

      // Ensure we don't calculate the rows again.
      calculatedRowCount = true;
    }
    return rowCount;
  }

  public boolean hasNoDuplicates() throws TuplesException {
    return tuples.hasNoDuplicates();
  }

  public List getOperands() {
    return new ArrayList();
  }

  public void beforeFirst(long[] prefix, int suffixTruncation) throws TuplesException {
    if (prefix.length > 4) {
      throw new TuplesException("Prefix too long");
    }

    // Reset skipped.
    skipped = false;

    // Move to the
    tuples.beforeFirst(prefix, suffixTruncation);
  }

  public void close() throws TuplesException {
    if (tuples != null) {
      tuples.close();
    }
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public Object clone() {
    ConstrainedNegationTuples cloned = (ConstrainedNegationTuples) super.clone();
    cloned.tuples = (Tuples) tuples.clone();
    cloned.setVariables(getVariables());

    // Create a new tuples evaluator.
    cloned.setTuplesEvaluator();
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

  public long getColumnValue(int column) throws TuplesException {
    return tuples.getColumnValue(column);
  }

  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return tuples.isColumnEverUnbound(column);
  }

  public boolean isUnconstrained() throws TuplesException {

    // If we have a least one variable return the tuples isUnconstrainedValue.
    if (!allFixedConstraints) {
      return tuples.isUnconstrained();
    }
    else {

      // If the statement exists we are constrained.
      if (singleStatementExists) {
        return false;
      }
      else {
        return true;
      }
    }
  }

  public boolean isMaterialized() {
    return tuples.isMaterialized();
  }

  public boolean next() throws TuplesException {
    boolean hasNext = tuplesEvaluator.next();
    return hasNext;
  }

  public String toString() {
    return "not " + tuples.toString() + " from constraint " + constraint;
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

  /**
   * A decorator around the next() method on a tuples that skips a set of
   * statements if they are given in the constraint.
   */
  private interface TuplesEvaluator {

    /**
     * Returns true if there is another tuples.  Calling next may skip a set
     * of tuples based on the constraint.
     *
     * @throws TuplesException if there was a problem accessing the tuples.
     * @return true if there is another tuples.
     */
    public boolean next() throws TuplesException;
  }

  /**
   * The evaluator for the S, P, O constrained tuples.
   */
  private class SPOTuplesEvaluator implements TuplesEvaluator {

    public boolean next() throws TuplesException {

      // Iterate forward one.
      boolean moreTuples = tuples.next();

      // If we've skipped over the tuples then just call the normal tuples.next.
      if (!skipped) {

        // Assume that the subject is always bound - otherwise wouldn't be used.
        // If the predicate is also constrained iterate through these.
        if (predicate != NodePool.NONE) {

          // If the predicate is also constrained iterate through these.
          if (object != NodePool.NONE) {

            // If we match then skip over the totally constrained tuple.
            if ((moreTuples) &&
                (tuples.getColumnValue(columnOrder[0]) == subject) &&
                (tuples.getColumnValue(columnOrder[1]) == predicate) &&
                (tuples.getColumnValue(columnOrder[2]) == object)) {
              moreTuples = tuples.next();
              skipped = true;
            }
          }
          else {

            // Skip over all the objects for the given subject, predicate.
            while ((moreTuples) &&
                (tuples.getColumnValue(columnOrder[0]) == subject) &&
                (tuples.getColumnValue(columnOrder[1]) == predicate)) {
              moreTuples = tuples.next();
              skipped = true;
            }
          }
        }
        else {

          // Skip over all the predicate, objects for the given subject.
          while ((moreTuples) &&
              (tuples.getColumnValue(columnOrder[0]) == subject)) {
            moreTuples = tuples.next();
            skipped = true;
          }
        }
      }
      return moreTuples;
    }
  }

  /**
   * The evaluator for the P, O, S constrained tuples.
   */
  private class POSTuplesEvaluator implements TuplesEvaluator {

    public boolean next() throws TuplesException {

      // Iterate forward one.
      boolean moreTuples = tuples.next();

      // If we've skipped over the tuples then just call the normal tuples.next.
      if (!skipped) {

        // If the object is also constrained iterate through these.
        if (object != NodePool.NONE) {

          // Skip over all the subjects for the given predicate, objects.
          while ((moreTuples) &&
              (tuples.getColumnValue(columnOrder[1]) == predicate) &&
              (tuples.getColumnValue(columnOrder[2]) == object)) {
            moreTuples = tuples.next();
            skipped = true;
          }
        }
        else {

          // Skip over all the object, subjects for the given predicate.
          while ((moreTuples) &&
              (tuples.getColumnValue(columnOrder[1]) == predicate)) {
            moreTuples = tuples.next();
            skipped = true;
          }
        }
      }
      return moreTuples;
    }
  }

  /**
   * The evaluator for the O, S, P constrained tuples.
   */
  private class OSPTuplesEvaluator implements TuplesEvaluator {

    public boolean next() throws TuplesException {

      // Iterate forward one.
      boolean moreTuples = tuples.next();

      // If we've skipped over the tuples then just call the normal tuples.next.
      if (!skipped) {

        // Skip over all the subject, predicates for the given object.
        while ((moreTuples) &&
            (tuples.getColumnValue(columnOrder[2]) == object)) {
          moreTuples = tuples.next();
          skipped = true;
        }
      }
      return moreTuples;
    }
  }

  /**
   * The evaluator for the S, O, P constrained tuples.
   */
  private class SOPTuplesEvaluator implements TuplesEvaluator {

    public boolean next() throws TuplesException {

      // Iterate forward one.
      boolean moreTuples = tuples.next();

      // If we've skipped over the tuples then just call the normal tuples.next.
      if (!skipped) {

        // Skip over all the subject, predicates for the given object.
        while ((moreTuples) &&
            (tuples.getColumnValue(columnOrder[0]) == subject) &&
            (tuples.getColumnValue(columnOrder[2]) == object)) {
          moreTuples = tuples.next();
          skipped = true;
        }
      }
      return moreTuples;
    }
  }

  /**
   * The evaluator when none of the tuples are constrained.
   */
  private class UnconstrainedTuplesEvaluator implements TuplesEvaluator {

    public boolean next() throws TuplesException {
      return false;
    }
  }
}
