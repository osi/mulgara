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

// Locally written packages
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;

/**
 * Convenient {@link Tuples} for use in test cases.
 *
 * @created 2003-01-10
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:10 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class TestTuples extends AbstractTuples {

  /**
   * A tuples with zero rows and zero columns.
   */
  static Tuples EMPTY = new TestTuples();

  /**
   * Description of the Field
   */
  private int row = 0;

  /**
   * Description of the Field
   */
  private final List termList;

  /**
   * Description of the Field
   */
  private final List variableList;

  /**
   * Description of the Field
   */
  private Map rowMap = null;

  /**
   * Description of the Field
   */
  private Map lastMap = new HashMap();

  /**
   * The number of columns in common for sort order.
   */
  private long[] prefix;

  private Tuples tuples;

  /**
   * Generate an empty expression.
   */
  public TestTuples() {

    termList = new ArrayList();
    variableList = new ArrayList();
  }

  /**
   * Generate a new tuples.
   *
   * @param variable Variable variable
   */
  public TestTuples(Variable variable) {

    this();

    // Add the variables to the list if it doesn't already.
    if (!variableList.contains(variable)) {

      variableList.add(variable);
      setVariables(variableList);
    }
  }

  /**
   * Generate a new assignment.
   *
   * @param variable PARAMETER TO DO
   * @param value PARAMETER TO DO
   */
  public TestTuples(Variable variable, long value) {
    this();
    or(variable, value);
  }

  /**
   * Copy constructor.
   *
   * @param tuples the instance to copy
   * @throws TuplesException if the <var>tuples</var> can't be read
   */
  TestTuples(Tuples tuples) throws TuplesException {
    this();

    Variable[] v = tuples.getVariables();
    assert v != null;
    tuples.beforeFirst();

    boolean hasNext = tuples.next();

    while (hasNext) {

      or();

      for (int i = 0; i < v.length; i++) {

        long l = tuples.getColumnValue(i);

        if (l != Tuples.UNBOUND) {

          and(v[i], l);
        }
      }

      hasNext = tuples.next();
    }

    this.tuples = tuples;
  }

  /**
   * Clone constructor.
   *
   * @param testTuples PARAMETER TO DO
   */
  private TestTuples(TestTuples testTuples) {

    row = testTuples.row;
    termList = testTuples.termList;
    variableList = testTuples.variableList;

    if (testTuples.getVariables() != null) {

      setVariables(testTuples.getVariables());
    }

    rowMap = testTuples.rowMap;
    lastMap = testTuples.lastMap;
    this.tuples = null;
  }

  /**
   * Test tuples are always materialized as they represent in memory tuples.
   *
   * @return always true as test tuples are represented in memory.
   */
  public boolean isMaterialized() {

    return true;
  }

  public List getOperands() {
    if (tuples != null) {
      return Collections.singletonList(tuples);
    } else {
      return new ArrayList(0);
    }
  }

  /**
   * Gets the ColumnValue attribute of the TestTuples object
   *
   * @param column PARAMETER TO DO
   * @return The ColumnValue value
   * @throws TuplesException EXCEPTION TO DO
   */
  public long getColumnValue(int column) throws TuplesException {

    if (rowMap == null) {

      throw new TuplesException("No column values; not on a row");
    }

    if ( (column < 0) || (column >= variableList.size())) {

      throw new TuplesException("Invalid column: " + column);
    }

    Long value = (Long) rowMap.get(variableList.get(column));

    return (value == null) ? Tuples.UNBOUND : value.longValue();
  }

  /**
   * Gets the RowCount attribute of the TestTuples object
   *
   * @return The RowCount value
   */
  public long getRowCount() {

    return termList.size();
  }

  public long getRowUpperBound() {

    return getRowCount();
  }

  /**
  * This method isn't really implemented; it just assumes all columns might be
  * unbound.
  *
  * @return <code>true</code>
  */
  public boolean isColumnEverUnbound(int column) throws TuplesException {

    return true;
  }

  /**
   * Generate a new assignment and conjoin it with the current final term.
   *
   * @param variable PARAMETER TO DO
   * @param value PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public TestTuples and(Variable variable, long value) {

    if (value == Tuples.UNBOUND) {

      // do nothing
      return this;
    }

    if (lastMap.containsKey(variable)) {

      throw new IllegalArgumentException(variable + " already assigned!");
    }

    // Add the variables to the list if it doesn't already.
    if (!variableList.contains(variable)) {

      variableList.add(variable);
      setVariables(variableList);
    }

    assert value != Tuples.UNBOUND;
    lastMap.put(variable, new Long(value));

    return this;
  }

  /**
   * METHOD TO DO
   *
   * @param variable PARAMETER TO DO
   * @param value PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public TestTuples or(Variable variable, long value) {

    return or().and(variable, value);
  }

  //
  // Methods implementing Tuples interface
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

    if (suffixTruncation != 0) {

      throw new TuplesException("Suffix truncation not supported");
    }

    row = 0;
    rowMap = null;
    this.prefix = prefix;
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   * @throws TuplesException EXCEPTION TO DO
   */
  public boolean next() throws TuplesException {

    A:while (advance()) {

      for (int i = 0; i < prefix.length; i++) {

        assert prefix[i] != Tuples.UNBOUND;

        if (prefix[i] != getColumnValue(getColumnIndex(getVariables()[i]))) {

          continue A;
        }
      }

      return true;
    }

    return false;
  }

  public boolean hasNoDuplicates() {
    return false;
  }

  //
  // Methods overriding the Object class
  //

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public Object clone() {

    return new TestTuples(this);
  }

  /**
   * METHOD TO DO
   *
   */
  public void close() {

    // NO-OP
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public int hashCode() {

    return termList.hashCode();
  }

  /**
   * Generate a new assignment and disjoin it as the new final term.
   *
   * @return RETURNED VALUE TO DO
   */
  private TestTuples or() {

    lastMap = new HashMap();
    termList.add(lastMap);

    return this;
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  private boolean advance() {

    assert row >= 0;
    assert row <= termList.size();

    if (row == termList.size()) {

      rowMap = null;
      return false;
    }
    else {

      assert row < termList.size();
      rowMap = (Map) termList.get(row);
      row++;

      return true;
    }
  }
}
