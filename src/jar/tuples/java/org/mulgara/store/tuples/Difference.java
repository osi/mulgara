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
import java.math.BigInteger;

// Third party packages
import org.apache.log4j.*;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.store.tuples.AbstractTuples;

/**
 * Difference operation.
 *
 * This difference is defined as all the rows in the minuend which are not matched
 * by rows in the subtrahend.  Matching is defined by rows where each pair of shared
 * variables is bound to the same pair of values.
 *
 * The join is performed by iterating over the minuend, and searching on the
 * subtrahend for matching rows.  For efficient searching, the subtrahend must be
 * ordered according to the matching variables.  This class is not responsible for
 * ensuring the sort order of the subtrahend; that responsibility falls to
 * {@link TuplesOperations#subtract}.
 *
 * @created 2003-09-01
 *
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 *
 * @version $Revision: 1.2 $
 *
 * @modified $Date: 2005/05/02 20:08:00 $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @copyright &copy; 2005 <A href="mailto:pgearon@users.sourceforge.net">Paul Gearon</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Difference extends AbstractTuples {

  private static Logger logger = Logger.getLogger(Difference.class.getName());

  /** The set of tuples to subtract from. */
  protected Tuples minuend;

  /** The set of tuples to remove from the subtrahend. */
  protected Tuples subtrahend;

  /** The set of variables common to both the minuend and the subtrahend. */
  protected Set commonVars;

  /** An array of the matching variables' columns within the minuend, indexed by the subtrahend position. */
  protected int[] varMap;

  /**
   * Configure a subtraction operation for lazy evaluation.
   *
   * @param minuend The original tuples, including the rows to be removed.
   * @param subtrahend The tuples defining the rows to be removed from the minuend.
   * @throws IllegalArgumentException If the <var>minuend</var> and  <var>subtrahend</var>
   *         contain no variables in common.
   */
  Difference(Tuples minuend, Tuples subtrahend) throws TuplesException, IllegalArgumentException {
    // store the operands
    this.minuend = (Tuples)minuend.clone();
    this.subtrahend = (Tuples)subtrahend.clone();

    // get the variables to subtract with
    commonVars = Collections.unmodifiableSet(TuplesOperations.getMatchingVars(minuend, subtrahend));

    if (commonVars.isEmpty()) {
      throw new IllegalArgumentException("tuples must have variables in common for subtraction to occur");
    }

    // initialise the mapping of minuend columns to subtrahend columns
    varMap = new int[commonVars.size()];
    // iterate over the variables to do the mapping
    Iterator iVar = commonVars.iterator();
    while (iVar.hasNext()) {
      Variable var = (Variable)iVar.next();
      // get the index of the variable in the subtrahend
      int si = subtrahend.getColumnIndex(var);
      // check that it is within the prefix columns. If not, then the subtrahend is not properly sorted.
      if (si >= varMap.length) {
        String op = "common= " + commonVars.toString();
        op += "; var= " + var + "; index in sub= " + si +"; subtrahend= [ ";
        Variable[] v = subtrahend.getVariables();
        for (int k = 0; k < v.length; k++) {
          op += v[k] + " ";
        }
        op += "]";
        // usually this would be an assertion, but it's too important to miss
        throw new IllegalArgumentException("Subtracted tuples not sorted correctly: " + op);
      }
      // map the subtrahend index of the variable to the minuend index
      varMap[si] = minuend.getColumnIndex(var);
    }

  }


  //
  // Methods implementing Tuples
  //

  /**
   * @param column {@inheritDoc}
   * @return {@inheritDoc}
   * @throws TuplesException {@inheritDoc}
   */
  public long getColumnValue(int column) throws TuplesException {
    return minuend.getColumnValue(column);
  }


  /**
   * @return {@inheritDoc}  This is estimated as the size of the minuend,
   * though it will probably be smaller.
   * @throws TuplesException {@inheritDoc}
   */
  public long getRowUpperBound() throws TuplesException {
    return minuend.getRowUpperBound();
  }


  /**
   * {@inheritDoc}  Relies on the minuend of the difference.
   */
  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return minuend.isColumnEverUnbound(column);
  }


  /**
   * {@inheritDoc}
   */
  public Variable[] getVariables() {
    return minuend.getVariables();
  }


  /**
   * {@inheritDoc}
   */
  public int getColumnIndex(Variable variable) throws TuplesException {
    return minuend.getColumnIndex(variable);
  }


  /**
   * {@inheritDoc}
   * @return Always <code>false</code>.
   */
  public boolean isMaterialized() {
    return false;
  }


  /**
   * {@inheritDoc}
   */
  public boolean hasNoDuplicates() throws TuplesException {
    return minuend.hasNoDuplicates();
  }


  /**
   * {@inheritDoc}
   */
  public RowComparator getComparator() {
    return minuend.getComparator();
  }


  /**
   * {@inheritDoc}
   */
  public List getOperands() {
    return Collections.unmodifiableList(Arrays.asList(new Tuples[] {minuend, subtrahend}));
  }


  /**
   * {@inheritDoc}
   */
  public boolean isUnconstrained() throws TuplesException {
    return minuend.isUnconstrained();
  }


  /**
   * {@inheritDoc}
   */
  public void renameVariables(Constraint constraint) {
    minuend.renameVariables(constraint);
  }


  /**
   * {@inheritDoc}
   */
  public void beforeFirst(long[] prefix, int suffixTruncation) throws TuplesException {
    minuend.beforeFirst(prefix, suffixTruncation);
  }


  /**
   * @return {@inheritDoc}
   * @throws TuplesException {@inheritDoc}
   */
  public boolean next() throws TuplesException {
    do {
      // move to the next on the minuend
      boolean currentNext = minuend.next();
      // Short-circuit execution if this tuples' cursor is after the last row
      if (!currentNext) {
        return false;
      }
      // check if the subtrahend matches the current row on the minuend
    } while (findMatch());

    return true;
  }


  /**
   * Closes all the operands.
   *
   * @throws TuplesException If either the minuend or the subtrahend can't be closed.
   */
  public void close() throws TuplesException {
    minuend.close();
    subtrahend.close();
  }


  /**
   * @return {@inheritDoc}
   */
  public Object clone() {
    Difference cloned = (Difference)super.clone();

    // Copy mutable fields by value
    cloned.minuend = (Tuples)minuend.clone();
    cloned.subtrahend = (Tuples)subtrahend.clone();

    return cloned;
  }


  //
  // Internal methods
  //

  /**
   * Searches for an entry in the subtrahend that matches the current row in the minuend.
   *
   * @return <code>true</code> if there is a row in the subtrahend that matches the minuend
   *         for all the variables.  <code>false</code> otherwise.
   */
  private boolean findMatch() throws TuplesException {
    long[] prefix = new long[varMap.length];
    // copy the variables from the current minuend row into the prefix
    for (int i = 0; i < varMap.length; i++) {
      prefix[i] = minuend.getColumnValue(varMap[i]);
    }
    // find the entry in the subtrahend
    subtrahend.beforeFirst(prefix, 0);
    // return true if the search found anything
    return subtrahend.next();
  }

}
