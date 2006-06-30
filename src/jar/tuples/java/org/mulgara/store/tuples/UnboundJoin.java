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
import org.apache.log4j.Category;

// Locally written packages
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.store.tuples.AbstractTuples;

/**
 * Logical conjunction implemented as a relational join operation.
 *
 * The join is performed using a series of nested loops, with the
 * lower-indexed elements of the {@link #operands} array forming the outer loops
 * and the higher-indexed forming the inner loops.  If the sort ordering of the
 * operand columns is such that it can be taken advantage of, this can be very
 * efficient.  If not, it degrades to the equivalent of an inner-outer loop
 * join (Cartesian product).  This class is not responsible for optimizing the
 * order of the operands presented to it; that responsibility falls to
 * {@link TuplesOperations#join}.
 *
 * @created 2003-09-01
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/03/07 19:42:40 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class UnboundJoin extends JoinTuples {

  static {
    logger = Category.getInstance(UnboundJoin.class.getName());
  }

  /**
   * Conjoin a list of propositions.
   *
   * @param operands the propositions to conjoin; the order affects efficiency,
   *      but not the logical value of the result
   * @throws IllegalArgumentException if <var>operands</var> is
   *                                  <code>null</code>
   * @throws TuplesException EXCEPTION TO DO
   */
  UnboundJoin(Tuples[] operands) throws TuplesException {
    init(operands);
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
    if ((column < 0) || (column >= getNumberOfVariables())) {
      throw new TuplesException("Invalid column: " + column);
    }

    long result = operands[mapOperand[column]].getColumnValue(mapColumn[column]);
    if (result != Tuples.UNBOUND) {
      return result;
    }

    // Brute force search for a bound instance of variable in operands.
    // Note: No operands to the left of the mapOperand[column] contain desired variable.
    Variable desired = getVariables()[column];
    for (int i = mapOperand[column] + 1; i < operands.length; i++) {
      Variable[] v = operands[i].getVariables();
      for (int j = 0; j < v.length; j++) {
        if (v[j].equals(desired)) {
          result = operands[i].getColumnValue(j);
          if (result != Tuples.UNBOUND) {
            return result;
          }
        }
      }
    }

    return Tuples.UNBOUND;
  }

  /**
   * @return {@inheritDoc}  This is estimated as the size of the Cartesian
   *   product, by multiplying the row counts of all the {@link #operands}.
   * @throws TuplesException {@inheritDoc}
   */
  public long getRowUpperBound() throws TuplesException {
    if (operands.length == 0) {
      return 0;
    }
    if (operands.length == 1) {
      return operands[0].getRowUpperBound();
    }

    BigInteger rowCount = BigInteger.valueOf(operands[0].getRowUpperBound());

    for (int i = 1; i < operands.length; i++) {
      rowCount = rowCount.multiply(BigInteger.valueOf(
          operands[i].getRowUpperBound()
          ));
      if (rowCount.bitLength() > 63)
        return Long.MAX_VALUE;
    }

    return rowCount.longValue();
  }

  /**
   * This method is not yet implemented, and always returns <code>true</code>.
   *
   * @return <code>true</code>
   */
  public boolean isColumnEverUnbound(int column) throws TuplesException {
    try {
      return columnEverUnbound[column];
    }
    catch (ArrayIndexOutOfBoundsException e) {
      throw new TuplesException("No such column " + column, e);
    }
  }

  /**
   * @return {@inheritDoc}
   * @throws TuplesException {@inheritDoc}
   */
  public boolean next() throws TuplesException {
    // Validate parameters
    if (prefix == null) {
      throw new IllegalArgumentException("Null \"prefix\" parameter");
    }

    // Short-circuit execution if this tuples' cursor is after the last row
    if (isAfterLast) {
      return false;
    }

    if (isBeforeFirst) {
      // Flag that we're no longer before the first row
      isBeforeFirst = false;

      // The first row has to be advanced from leftmost to rightmost operand in
      // order to initialize the leftward dependencies of the operand prefixes
      for (int i = 0; i < operands.length; i++) {
        updateOperandPrefix(i);
        operands[i].beforeFirst(operandBindingPrefix[i], 0);

        if (!advance(i)) {
          return false;
        }
      }

      return true;
    }
    else {
      // We know at this point that we're on a row satisfying the current
      // prefix.  Advance the rightmost operand and let rollover do any
      // right-to-left advancement required
      boolean b = advance(operands.length - 1);
      assert b || isAfterLast;
      return b;
    }
  }

  public boolean hasNoDuplicates() {
    return operandsContainDuplicates == false;
  }

  /**
   * Closes all the {@link #operands}.
   *
   * @throws TuplesException  if any of the {@link #operands} can't be closed
   */
  public void close() throws TuplesException {
    close(operands);
  }

  /**
   * @return {@inheritDoc}
   */
  public Object clone() {
    UnboundJoin cloned = (UnboundJoin)super.clone();

    // Copy immutable fields by reference
    cloned.operandBinding = operandBinding;
    cloned.operandBindingPrefix = operandBindingPrefix;
    cloned.mapOperand = mapOperand;
    cloned.mapColumn = mapColumn;
    cloned.fooOperand = fooOperand;
    cloned.fooColumn = fooColumn;
    cloned.prefix = prefix;

    // Copy mutable fields by value
    cloned.operands = clone(operands);
    cloned.isBeforeFirst = isBeforeFirst;
    cloned.isAfterLast = isAfterLast;

    return cloned;
  }

  //
  // Internal methods
  //

  /**
   * Calculate the correct value for one of the elements of {@link
   * #operandBinding} and its corresponding {@link #operandBindingPrefix}. This
   * method has no return value, only side-effects upon {@link #operandBinding}
   * and {@link #operandBindingPrefix}.
   *
   * @param i  the index of the element in the {@link #operandBinding} array to
   *           calculate
   * @throws TuplesException  if the {@link #operands} can't be accessed
   */
  private void updateOperandPrefix(int i) throws TuplesException {
    assert i >= 0;
    assert i < operandBinding.length;

    for (int j = 0; j < operandBinding[i].length; j++) {
      if (fooOperand[i][j] == PREFIX) {
        // Variable first bound to a next method parameter prefix column passed to beforeFirst.
        operandBinding[i][j] = (j < prefix.length) ? prefix[fooColumn[i][j]] :
            Tuples.UNBOUND;
      }
      else {
        // Variable first bound to a leftward operand column
        operandBinding[i][j] = operands[fooOperand[i][j]].getColumnValue(
            fooColumn[i][j]);
      }
    }

    // Determine the length of the advancement prefix
    int prefixLength = 0;
    while ((prefixLength < operandBinding[i].length) &&
        (operandBinding[i][prefixLength] != Tuples.UNBOUND) &&
        (columnOperandEverUnbound[operandOutputMap[i][prefixLength]] == false)) {
      prefixLength++;
    }

    assert prefixLength >= 0;
    assert prefixLength <= operandBinding[i].length;
//    assert (prefixLength == operandBinding[i].length) ||
//           (operandBinding[i][prefixLength] == Tuples.UNBOUND);

    // Generate the advancement prefix
    assert operandBindingPrefix != null;

    if ((operandBindingPrefix[i] == null) ||
        (operandBindingPrefix[i].length != prefixLength)) {
      operandBindingPrefix[i] = new long[prefixLength];
    }

    System.arraycopy(operandBinding[i], 0, operandBindingPrefix[i], 0,
        prefixLength);
  }

  /**
   * Advance one of the joined operands.
   *
   * @param i  the index of the operand to advance
   * @return whether a row was found to satisfy
   * @throws TuplesException if the {@link #operands} can't be accessed
   */
  private final boolean advance(int i) throws TuplesException {
    assert i >= 0;
    assert i < operands.length;
    assert!isAfterLast;

    B:while (true) {
      if (!operands[i].next()) {
        // Roll this column...
        if (i == 0) {
          isAfterLast = true;
          prefix = null;
          return false;
        }
        else {
          // roll the leftward row
          if (!advance(i - 1)) {
            return false;
          }

          // reset the current row
          updateOperandPrefix(i);
          operands[i].beforeFirst(operandBindingPrefix[i], 0);

          continue B;
        }
      }

      // Check that any suffix conditions are satisfied
      for (int j = operandBindingPrefix[i].length;
          j < operandBinding[i].length; j++) {
        if ((operandBinding[i][j] != Tuples.UNBOUND) &&
            (operandBinding[i][j] != operands[i].getColumnValue(j)) &&
            (operands[i].getColumnValue(j) != Tuples.UNBOUND)) {
          continue B;
        }
      }

      return true;
    }
  }
}
