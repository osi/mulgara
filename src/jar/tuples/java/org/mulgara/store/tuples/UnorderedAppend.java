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

// Log4J
import org.apache.log4j.Category;

// Locally written packages
import org.mulgara.query.TuplesException;

/**
 * Logical disjunction. The append is performed by iterating through the
 * operands in the order given. It requires that the operands be
 * union-compatible (i.e. matching variable lists). The result will have no sort
 * order.
 *
 * @created 2003-01-10
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:10 $
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
class UnorderedAppend extends AbstractTuples {

  /**
   * Logger.
   */
  private final static Category logger =
      Category.getInstance(UnorderedAppend.class.getName());

  /**
   * The propositions to conjoin.
   */
  private final Tuples[] operands;

  /**
   * The operand currently being iterated through.
   */
  private int operand;

  /**
   * Description of the Field
   */
  private long[] prefix;

  /**
   * Description of the Field
   */
  private int suffixTruncation = -1;

  // initial value is fail-fast

  /**
   * Disjoin a list of propositions.
   *
   * @param operands PARAMETER TO DO
   * @throws IllegalArgumentException if <var>operands</var> is <code>null</code>
   * @throws TuplesException EXCEPTION TO DO
   */
  UnorderedAppend(Tuples[] operands) throws TuplesException {

    // Validate "operands" parameter
    if (operands == null) {

      throw new IllegalArgumentException("Null \"operands\" parameter");
    }

    if (operands.length < 1) {

      operands = new Tuples[] {
          TuplesOperations.empty()};
    }

    // Initialize fields
    this.operands = clone(operands);
    setVariables(operands[0].getVariables());

    for (int i = 1; i < operands.length; i++) {

      if (!Arrays.equals(getVariables(), operands[i].getVariables())) {

        throw new TuplesException("Mismatched variables, operand 0 " +
            Arrays.asList(getVariables()) + " and operand " + i + " " +
            Arrays.asList(operands[i].getVariables()));
      }
    }
  }

  /**
   * CONSTRUCTOR UnorderedAppend TO DO
   *
   * @param unorderedAppend PARAMETER TO DO
   */
  private UnorderedAppend(UnorderedAppend unorderedAppend) {

    setVariables(unorderedAppend.getVariables());
    operands = clone(unorderedAppend.operands);
    operand = unorderedAppend.operand;
    prefix = unorderedAppend.prefix;
    suffixTruncation = unorderedAppend.suffixTruncation;
  }

  /**
   * Gets the ColumnValue attribute of the UnorderedAppend object
   *
   * @param column PARAMETER TO DO
   * @return The ColumnValue value
   * @throws TuplesException EXCEPTION TO DO
   */
  public long getColumnValue(int column) throws TuplesException {

    assert(operand >= 0) || (operand < operands.length):"No column " +
        column;

    return operands[operand].getColumnValue(column);
  }

  /**
   * Gets the RowCount attribute of the UnorderedAppend object
   *
   * @return The RowCount value
   * @throws TuplesException EXCEPTION TO DO
   */
  public long getRowCount() throws TuplesException {
    rowCount = 0;

    for (int i = 0; i < operands.length; i++) {
      rowCount += operands[i].getRowCount();
      if (rowCount < 0)
        return Long.MAX_VALUE;
    }

    return rowCount;
  }

  public long getRowUpperBound() throws TuplesException {
    long bound = 0;

    for (int i = 0; i < operands.length; i++) {
      bound += operands[i].getRowUpperBound();
      if (bound < 0)
        return Long.MAX_VALUE;
    }

    return bound;
  }

  public boolean isColumnEverUnbound(int column) throws TuplesException
  {
    assert(operand >= 0) || (operand < operands.length):"No column " +
        column;

    for (int i = 0; i < operands.length; i++) {
      if (operands[i].isColumnEverUnbound(column)) {
        return true;
      }
    }

    return false;
  }


  public boolean hasNoDuplicates() {
    return false;
  }


  /**
   * @return whether any operand is unconstrained
   * @throws TuplesException EXCEPTION TO DO
   */
  public boolean isUnconstrained() throws TuplesException
  {
    for (int i = 0; i < operands.length; i++) {
      if (operands[i].isUnconstrained()) {
        return true;
      }
    }

    return false;
  }


  public List getOperands() {
    return Arrays.asList(operands);
  }


  //
  // Methods implementing Tuples
  //

  /**
   * METHOD TO DO
   *
   * @param prefix PARAMETER TO DO
   * @param suffixTruncation PARAMETER TO DO
   * @throws TuplesException EXCEPTION TO DO
   */
  public void beforeFirst(long[] prefix,
      int suffixTruncation) throws TuplesException {

    operands[0].beforeFirst(prefix, suffixTruncation);
    operand = 0;
    this.prefix = prefix;
    this.suffixTruncation = suffixTruncation;
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public Object clone() {

    return new UnorderedAppend(this);
  }

  /**
   * METHOD TO DO
   *
   * @throws TuplesException EXCEPTION TO DO
   */
  public void close() throws TuplesException {

    close(operands);
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   * @throws TuplesException EXCEPTION TO DO
   */
  public boolean next() throws TuplesException {

    if (operand >= operands.length) {

      return false;
    }

    if (!operands[operand].next()) {

      operand++;

      if (operand < operands.length) {

        operands[operand].beforeFirst(prefix, suffixTruncation);
      }

      return next();
    }
    else {

      return true;
    }
  }
}
