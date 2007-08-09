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

package org.mulgara.util;

// Java 2 standard packages
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.sql.*;
import java.util.*;

// third party packages
import org.apache.log4j.Category;

// Log4J

/**
 * Implementation of {@link ResultSet} suitable for generating test cases. It's
 * not a correct {@link ResultSet} in many respects, the foremost being an
 * absence of column typing.
 *
 * @created 2001-07-12
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:29 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class TestResultSet extends AbstractMulgaraResultSet
  implements Serializable {

 /**
  * Allow newer compiled version of the stub to operate when changes
  * have not occurred with the class.
  * NOTE : update this serialVersionUID when a method or a public member is
  * deleted.
  */
  static final long serialVersionUID = 447953576074487320L;

  /**
   * Logger.
   */
  private static Category logger =
      Category.getInstance(TestResultSet.class.getName());

  /**
   * The metadata for this result set.
   */
  private ResultSetMetaData metaData;

  /**
   * The rows of the result set.
   */
  private List rows = new RowList();

  /**
   * The current row index.
   */
  private int currentRowIndex = 0;

  /**
   * The current row.
   */
  private ResultSetRow currentRow = null;

  //
  // Constructors
  //

  /**
   * Create a result set with named columns and no rows.
   *
   * @param newColumnNames the new column names for this result set.
   * @throws SQLException on failure
   */
  public TestResultSet(String[] newColumnNames) throws SQLException {

    // FIXME: should validate columnNames, checking for duplicate names, etc
    // initialize fields
    columnNames = newColumnNames;
    metaData = new ResultSetMetaDataImpl(columnNames);

    // move the cursor before the first row
    beforeFirst();
  }

  /**
   * Create a result set with specified metadata and no rows.
   *
   * @param metaData PARAMETER TO DO
   * @throws SQLException on failure
   */
  public TestResultSet(ResultSetMetaData metaData) throws SQLException {

    initialiseMetaData(metaData);

    // move the cursor before the first row
    beforeFirst();
  }

  /**
   * Create a result set with content copied from an existing result set.
   *
   * @param resultSet PARAMETER TO DO
   * @throws SQLException on failure
   */
  public TestResultSet(ResultSet resultSet) throws SQLException {

    // Validate "resultSet" parameter
    if (resultSet == null) {

      throw new IllegalArgumentException("Null \"resultSet\" parameter");
    }

    // Copy columns
    initialiseMetaData(resultSet.getMetaData());

    // Copy rows
    if (resultSet.getClass() == TestResultSet.class) {

      rows.addAll( ( (TestResultSet) resultSet).rows);
    }
    else {

      // Don't assume that it hasn't already been read.
      resultSet.beforeFirst();

      // Repeat until we get all of the items from the result sets.
      while (resultSet.next()) {

        ResultSetRow row = new ResultSetRow(resultSet);

        for (int i = 0; i < columnNames.length; i++) {

          row.setObject(columnNames[i], resultSet.getObject(columnNames[i]));
        }

        addRow(row);
      }
    }
  }

  /**
   * Overwrites the existing set of rows available.
   *
   * @param newRows the new rows to set.
   * @throws SQLException EXCEPTION TO DO
   */
  public void setAllRows(List newRows) throws SQLException {

    rows = newRows;
  }

  // next()

  /**
   * Return the object at the given index.
   *
   * @param columnIndex the index of the object to retrieve
   * @return the value at the given index
   * @throws SQLException on failure
   */
  public Object getObject(int columnIndex) throws SQLException {

    // throw an error if the current row is invalid
    if (this.currentRow == null) {

      throw new SQLException("Not on a row.");
    }

    // get the value out of the current row
    return this.currentRow.getObject(columnIndex);
  }

  // getObject()

  /**
   * Return the string at the given index.
   *
   * @param columnIndex the index of the string to retrieve
   * @return the value at the given index (possibly <code>null</code>)
   * @throws SQLException on failure
   */
  public String getString(int columnIndex) throws SQLException {

    // throw an error if the current row is invalid
    if (this.currentRow == null) {

      throw new SQLException("Not on a row.");
    }

    // get the value out of the current row
    Object object = this.currentRow.getObject(columnIndex);

    return (object == null) ? null : object.toString();
  }

  // getString()

  /**
   * Returns the metadata for this result set.
   *
   * @return the metadata for this result set
   * @throws SQLException on failure
   */
  public ResultSetMetaData getMetaData() throws SQLException {

    return metaData;
  }

  /**
   * Returns whether the cursor is before the first row.
   *
       * @return true if the cursor is before the first row in the result set, false
   *      otherwise
   * @throws SQLException on failure
   */
  public boolean isBeforeFirst() throws SQLException {

    return this.currentRowIndex == 0;
  }

  /**
   * Returns true if the cursor is after the last row.
   *
   * @return true if the cursor is after the first row in the result set, false
   *      otherwise
   * @throws SQLException on failure
   */
  public boolean isAfterLast() throws SQLException {

    return this.currentRowIndex > this.rows.size();
  }

  /**
   * Returns true if the cursor is on the first row.
   *
   * @return true if the cursor is on the first row, false otherwise
   * @throws SQLException on failure
   */
  public boolean isFirst() throws SQLException {

    return this.currentRowIndex == 1;
  }

  /**
   * Returns true if the cursor is on the last row.
   *
   * @return true if the cursor is on the las row, false otherwise
   * @throws SQLException on failure
   */
  public boolean isLast() throws SQLException {

    return this.currentRowIndex == this.rows.size();
  }

  /**
   * Returns the entire rows underlying the result set.
   *
   * @return the entire rows underlying the result set.
   * @throws SQLException EXCEPTION TO DO
   */
  public List getAllRows() throws SQLException {

    return rows;
  }

  /**
   * Returns the index of the row the cursor is on.
   *
   * @return the index of the row the cursor is on
   * @throws SQLException on failure
   */
  public int getRow() throws SQLException {

    return this.currentRowIndex;
  }

  /**
   * Returns the total size of the number of rows.
   *
   * @return the total size of the number of rows available.
   */
  public int getTotalRowSize() {

    return rows.size();
  }

  /**
   * Gets the CurrentRow attribute of the TestResultSet object
   *
   * @return The CurrentRow value
   */
  public ResultSetRow getCurrentRow() {

    return currentRow;
  }

  //
  // Methods implementing the ResultSet interface
  //

  /**
   * Moves the cursor down one row from its current position.
   *
   * @return true if the new current row is valid; false if there are no more
   *      rows
   * @throws SQLException on failure
   */
  public boolean next() throws SQLException {

    boolean returnState = false;

    // advance the cursor if we can
    if (!this.isLast()) {

      this.currentRowIndex++;
      this.currentRow = (ResultSetRow)this.rows.get(this.currentRowIndex - 1);
      returnState = true;
    }
    else {

      this.currentRow = null;
      returnState = false;
    }

    // end if
    // return
    return returnState;
  }

  /**
   * Moves the cursor to before the first row in the result set.
   *
   * @throws SQLException on failure
   */
  public void beforeFirst() throws SQLException {

    // return members to their default state
    this.currentRowIndex = 0;
    this.currentRow = null;
  }

  /**
   * Moves the cursor to after the last row in the result set.
   *
   * @throws SQLException on failure
   */
  public void afterLast() throws SQLException {

    this.currentRowIndex = this.rows.size() + 1;
    this.currentRow = null;
  }

  /**
   * Moves the cursor to the first row in this ResultSet object.
   *
       * @return true if the cursor is on a valid row; false if there are no rows in
   *      the result set
   * @throws SQLException always
   */
  public boolean first() throws SQLException {

    boolean returnState = false;

    if (this.rows.size() > 0) {

      this.beforeFirst();
      this.next();
      returnState = true;
    }

    return returnState;
  }

  /**
   * Moves the cursor to the last row in the result set.
   *
   * @return RETURNED VALUE TO DO
   * @throws SQLException always
   */
  public boolean last() throws SQLException {

    boolean returnState = false;

    if ( (this.rows != null) && (this.rows.size() > 0)) {

      this.currentRowIndex = this.rows.size();
      this.currentRow = (ResultSetRow)this.rows.get(this.currentRowIndex - 1);
      returnState = true;
    }

    return returnState;
  }

  /**
   * Adds a new row to the current set of rows.
   *
   * @param row new row to add to the end of the queue.
   */
  public void addRow(ResultSetRow row) {

    rows.add(row);
  }

  /**
   * Moves the cursor to the given row number (takes obsolute value) in this
   * ResultSet object. An attempt to position the cursor beyond the first/last
   * row in the result set leaves the cursor before the first row or after the
   * last row.
   *
   * @param row the number of the row to which the cursor should move. A
   *      positive number indicates the row number counting from the beginning
       *      of the result set; a negative number indicates the row number counting
   *      from the end of the result set
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean absolute(int row) throws SQLException {

    boolean returnState = false;

    // Work forward from the start
    if (row >= 0) {

      if (row == 0) {

        beforeFirst();
      }
      else if (row <= this.rows.size()) {

        this.currentRowIndex = row;
        this.currentRow =
            (ResultSetRow)this.rows.get(this.currentRowIndex - 1);
        returnState = true;
      }
      else {

        afterLast();
      }
    }

    // Work back from the end
    else {

      if ( (this.rows.size() + row) >= 0) {

        this.currentRowIndex = (this.rows.size() + 1) + row;
        this.currentRow =
            (ResultSetRow)this.rows.get(this.currentRowIndex - 1);
        returnState = true;
      }
      else {

        beforeFirst();
      }
    }

    return returnState;
  }

  /**
   * Moves the cursor a relative number of rows, either positive or negative
   * from its current position. Attempting to move beyond the first/last row in
   * the result set positions the cursor before/after the the first/last row.
   * Calling relative(0) is valid, but does not change the cursor position.
   *
   * @param numRows an int specifying the number of rows to move from the
   *      current row; a positive number moves the cursor forward; a negative
   *      number moves the cursor backward
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean relative(int numRows) throws SQLException {

    boolean returnState = false;

    // Work forward from the start
    if (numRows >= 0) {

      if (numRows <= (this.rows.size() - this.currentRowIndex)) {

        this.currentRowIndex += numRows;
        this.currentRow =
            (ResultSetRow)this.rows.get(this.currentRowIndex - 1);
        returnState = true;
      }
      else {

        afterLast();
      }
    }

    // Work back from the end
    else {

      if ( (this.currentRowIndex + numRows) > 0) {

        // Add 1 to size to go to end of list then add the negative row number
        this.currentRowIndex += numRows;
        this.currentRow =
            (ResultSetRow)this.rows.get(this.currentRowIndex - 1);
        returnState = true;
      }
      else {

        beforeFirst();
      }
    }

    return returnState;
  }

  //
  // Relational algebra methods
  //

  /**
   * Perform a <dfn>natural join</dfn> between this result set and another. The
   * join will be performed based on matching column names. See Elmasri &amp;
   * Navathe, <cite>Fundamentals of Database Systems</cite> , p. 158.
   *
   * @param resultSet the other result set to join with
   * @return the result of the join operation
   * @throws SQLException if the join fails
   */
  /*
  public MulgaraResultSet join(MulgaraResultSet resultSet) throws SQLException {

    // Determine the join variables
    List columnNamesList = new ArrayList(Arrays.asList(columnNames));

    String[] mdataColumnNames = resultSet.getColumnNames();

    columnNamesList.remove("score");

    // FIXME: hack for Lucene queries
    columnNamesList.retainAll(Arrays.asList(mdataColumnNames));

    // Delegate execution to the fully parameterized join method
    String[] columnNamesArray =
        (String[]) columnNamesList.toArray(new String[columnNamesList.size()]);

    return join(resultSet, columnNamesArray, columnNamesArray);
  }
  */

  /**
   * Perform a natural join between this result set and another, specifying the
   * column names upon which to join.
   *
   * @param resultSet the other result set to join with
   * @param columnNames the list of column names in <code>this</code> on which
   *      to join; all the named columns must occur in <code>this</code>, and
   *      the length of the array must match <var>resultSetColumnNames</var>
   * @param resultSetColumnNames the list of column names in <var>resultSet
   *      </var> on which to join; all the named columns must occur in <var>
   *      resultSet</var> , and the length of the array must match <var>
   *      columnNames</var>
   * @return the result of the join operation
   * @throws IllegalArgumentException if the <var>columnName</var> and <var>
   *      resultSetColumnNames</var> arguments are incompatible, or if any of
   *      the arguments are <code>null</code>
   * @throws SQLException if the join fails
   */
  /*
  public MulgaraResultSet join(MulgaraResultSet resultSet, String[] columnNames,
      String[] resultSetColumnNames) throws SQLException {

    // Validate parameters
    if (columnNames.length != resultSetColumnNames.length) {

      throw new IllegalArgumentException(
          "columnNames different length than resultSetColumnNames");
    }

    MulgaraResultSet testResultSet = new TestResultSet(resultSet);

    // Calculate unjoined columns of this and resultSet
    List unjoinedColumnNamesList =
        new ArrayList(Arrays.asList(this.columnNames));
    unjoinedColumnNamesList.removeAll(Arrays.asList(columnNames));

    String[] mdColumnNames = testResultSet.getColumnNames();
    List unjoinedResultSetColumnNamesList =
        new ArrayList(Arrays.asList(mdColumnNames));
    unjoinedResultSetColumnNamesList.removeAll(Arrays.asList(columnNames));

    List resultColumnNamesList = new ArrayList(Arrays.asList(columnNames));
    resultColumnNamesList.addAll(unjoinedColumnNamesList);
    resultColumnNamesList.addAll(unjoinedResultSetColumnNamesList);

    String[] resultColumnNames =
        (String[]) resultColumnNamesList.toArray(new String[
        resultColumnNamesList.size()]);

    TestResultSet result = new TestResultSet(resultColumnNames);

    // Pre-calculate column numbers for the columnNames array
    int[] columnIndices = new int[columnNames.length];

    for (int i = 0; i < columnNames.length; i++) {

      columnIndices[i] = columnForName(columnNames[i]);
    }

    int[] resultSetColumnIndices = new int[columnNames.length];

    for (int i = 0; i < columnNames.length; i++) {

      resultSetColumnIndices[i] =
          ( (TestResultSet) resultSet).columnForName(columnNames[i]);
    }

    // Short-circuit execution if the join is going to be empty
    if ( (getTotalRowSize() == 0) || (testResultSet.getTotalRowSize() == 0)) {

      return result;
    }

    if ( (columnNames.length == 0) &&
        logger.isEnabledFor(org.apache.log4j.Priority.WARN) &&
        (getTotalRowSize() > 1) &&
        (testResultSet.getTotalRowSize() > 1)) {

      logger.warn("Performing cartesian product (" + rows.size() + "x" +
          testResultSet.getTotalRowSize() + ")");
    }

    // Iterate through the joined columns
    while (next()) {

      ResultSetRow lhsRow = (ResultSetRow) getCurrentRow();

      rhs:
          for (testResultSet.beforeFirst(); testResultSet.next(); ) {

        ResultSetRow rhsRow = (ResultSetRow) testResultSet.getCurrentRow();

        // Don't create the new row unless the equijoin test succeeds
        for (int i = 0; i < columnNames.length; i++) {

          Object lhsObject = lhsRow.getObject(columnIndices[i]);
          Object rhsObject = rhsRow.getObject(resultSetColumnNames[i]);

          // Nulls are always equal; other values must be tested
          if ( (lhsObject != null) &&
              (rhsObject != null) &&
              !lhsObject.equals(rhsObject)) {

            continue rhs;
          }
        }

        // equijoin succeeded
        // Creating a new row
        ResultSetRow resultRow = new ResultSetRow(result);

        // Set all the join columns
        for (int i = 0; i < columnNames.length; i++) {

          Object lhsValue = lhsRow.getObject(columnIndices[i]);
          Object rhsValue = rhsRow.getObject(resultSetColumnIndices[i]);
          resultRow.setObject(columnNames[i],
              (lhsValue == null) ? rhsValue : lhsValue);
        }

        // Set all unjoined LHS columns
        for (Iterator i = unjoinedColumnNamesList.iterator(); i.hasNext(); ) {

          String ucn = (String) i.next();
          resultRow.setObject(ucn, lhsRow.getObject(ucn));
        }

        // Set all unjoined RHS columns
        for (Iterator i = unjoinedResultSetColumnNamesList.iterator();
            i.hasNext(); ) {

          String ucn = (String) i.next();
          resultRow.setObject(ucn, rhsRow.getObject(ucn));
        }

        result.addRow(resultRow);
      }
    }

    // Not sure what to do here yet
    return result;
  }
  */

  /**
  * Perform a relational algebra <dfn>self join</dfn> operation. This operation
  * filters duplicate rows out of result sets. In this implementation, order is
  * not preserved.
  *
  */
  /*
  public void removeDuplicateRows() {

    // Sort the result sets
    List sortedRows = new RowList(rows.size());
    sortedRows.addAll(rows);
    Collections.sort(sortedRows, this.new ColumnComparator(columnNames));

    // Remove duplicates
    ResultSetRow previousRow = null;
    row:
        for (Iterator i = sortedRows.iterator(); i.hasNext(); ) {

      ResultSetRow row = (ResultSetRow) i.next();

      if (previousRow == null) {

        previousRow = row;
      }
      else {

        for (int j = 0; j < columnNames.length; j++) {

          Object previousField = previousRow.getObject(columnNames[j]);
          Object field = row.getObject(columnNames[j]);

          if ( (previousField != null) && !previousField.equals(field)) {

            previousRow = row;

            continue row;
          }
        }

        // The current element is a duplicate of the previous one; remove it
        i.remove();
      }
    }

    // New rows are the ones without duplicates
    rows = sortedRows;
  }
  */

  /**
   * Perform a relational algebra <dfn>project</dfn> operation. This operation
   * filters columns out of result sets. The columns to be retained are
   * specified by name.
   *
   * @param columnNames the column names to retain, which may or may not exist
   *      in this result set
       * @return a result set containing only columns named in <code>columnNames</code>
   * @throws SQLException if the projection fails
   */
  /*
  public MulgaraResultSet project2(String[] columnNames) throws SQLException {

    // Intersect the projection variables
    List columnNamesList = new ArrayList(Arrays.asList(columnNames));
    columnNamesList.retainAll(Arrays.asList(this.columnNames));

    String[] projectedColumnNames = new String[columnNamesList.size()];
    projectedColumnNames =
        (String[]) columnNamesList.toArray(projectedColumnNames);

    TestResultSet result = new TestResultSet(projectedColumnNames);

    // Start at the start
    beforeFirst();

    while (next()) {

      ResultSetRow row = getCurrentRow();

      // Create the new row
      ResultSetRow resultRow = new ResultSetRow(result);

      // Populate each column of the new row
      for (int j = 0; j < projectedColumnNames.length; j++) {

        resultRow.setObject(projectedColumnNames[j],
            row.getObject(projectedColumnNames[j]));
      }

      // Add the new row to the result
      result.addRow(resultRow);
    }

    return result;
  }
  */

  /**
   * Perform a relational algebra <dfn>project</dfn> operation. This operation
   * filters columns out of result sets. The columns to be retained are
   * specified by name.
   *
   * @param columnNames the column names to retain, all of which must exist in
   *      this result set
       * @return a result set containing only columns named in <code>columnNames</code>
   * @throws SQLException if the projection fails
   */
  /*
  public MulgaraResultSet project(String[] columnNames) throws SQLException {

    TestResultSet result = new TestResultSet(columnNames);

    // Start at the start
    beforeFirst();

    while (next()) {

      ResultSetRow row = (ResultSetRow) getCurrentRow();

      // Create the new row
      ResultSetRow resultRow = new ResultSetRow(result);

      // Populate each column of the new row
      for (int j = 0; j < columnNames.length; j++) {

        resultRow.setObject(columnNames[j], row.getObject(columnNames[j]));
      }

      // Add the new row to the result
      result.addRow(resultRow);
    }

    return result;
  }
  */

  /**
   * Sort according to a passed comparator.
   *
   * @param comparator a comparator for {@link ResultSetRow}s
   */
  /*
  public void sort(Comparator comparator) {

    if (comparator == null) {

      throw new IllegalArgumentException("Null \"comparator\" parameter");
    }

    Collections.sort(rows, comparator);
  }
  */

  //
  // Utility methods to make testing possible
  //

  /**
   * Append all rows from another result set to this one. All the column names
   * in this result set must be present in the other. This implementation
   * doesn't require that the types of the columns match.
   *
   * @param resultSet the result set to append
   * @throws SQLException if <var>resultSet</var> can't be read
   */

  // FIXME: I modify my argument by moving its cursor all over the place,
  //        and I assume that the cursor starts at the beginning
  /*
  public void append(ResultSet resultSet) throws SQLException {

    // Short-circuit test
    if (resultSet == null) {

      return;
    }

    ;

    // Quick hack to avoid infinite looping or cursor side effects
    resultSet = new TestResultSet(resultSet);

    // Add all rows in resultSet to this
    resultSet.beforeFirst();

    while (resultSet.next()) {

      // Create the new row
      ResultSetRow row = new ResultSetRow(this);
      this.addRow(row);

      // Populate each column of the new row
      for (int i = 0; i < columnNames.length; i++) {

        // Hack to skip columns missing from resultSet
        try {

          resultSet.findColumn(columnNames[i]);
        }
        catch (SQLException e) {

          continue;
        }

        row.setObject(columnNames[i], resultSet.getObject(columnNames[i]));
      }
    }
  }
  */

  /**
   * Result sets are equal if their rows are equal. Both row and column ordering
   * is signicant.
   *
   * @param object PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public boolean equals(Object object) {

    if (! (object instanceof ResultSet)) {

      return false;
    }

    try {

      // Convert the other result set into a comparable form
      TestResultSet testResultSet =
          (object instanceof TestResultSet) ? (TestResultSet) object
          : new TestResultSet( (ResultSet) object);

      // Compare the rows
      return rows.equals(testResultSet.rows);
    }
    catch (SQLException e) {

      return false;
    }
  }

  /**
   * Test this result set for equality with another, ignoring any differences
   * between row ordering. Column ordering is still significant.
   *
   * @param object the result set to check to see if it is equal.
   * @return true if the result set is equal ignoring row order.
   */
  /*
  public boolean equalsIgnoreOrder(Object object) {

    if (! (object instanceof ResultSet)) {

      return false;
    }

    try {

      // Convert the other result set into a comparable form
      TestResultSet testResultSet =
          (object instanceof TestResultSet) ? (TestResultSet) object
          : new TestResultSet( (ResultSet) object);

      // Sort the result sets and compare the sorted rows
      List sortedRows = new ArrayList(rows.size());
      sortedRows.addAll(rows);
      Collections.sort(sortedRows);

      List sortedTestResultSetRows = new ArrayList(testResultSet.rows.size());
      sortedTestResultSetRows.addAll(testResultSet.rows);
      Collections.sort(sortedTestResultSetRows);

      return sortedRows.equals(sortedTestResultSetRows);
    }
    catch (SQLException e) {

      return false;
    }
  }
  */

  /**
   * Produce a string version of the result set. Displaying the available
   * columns and rows.
   *
   * @return the string version of the result set.
   */
  public String toString() {

    try {

      StringBuffer buffer =
          new StringBuffer(getColumnNames().length + " columns:");

      // Save the current state of the result set.
      int tmpCurrentRow = getRow();

      // Get the names of the columns
      for (int i = 0; i < columnNames.length; i++) {

        buffer.append(" ").append(columnNames[i]);
      }

      buffer.append(" (").append(rows.size()).append(" rows)");

      // Save the current state
      int currentRowIndex = getRow();

      // Start at the start
      beforeFirst();

      while (next()) {

        buffer.append("\n").append(getCurrentRow());
      }

      // Restore the state of the result set.
      absolute(tmpCurrentRow);

      return buffer.toString();
    }
    catch (SQLException se) {

      logger.error("Failed to convert object to string", se);

      return "";
    }
  }

  /**
   * Truncate trailing rows.
   *
   * @param limit the maximum number of rows to retain
   */
  /*
  public void limit(int limit) {

    if (rows.size() > limit) {

      logger.warn("Removing " + limit + "-" + rows.size());
      ( (RowList) rows).removeRange(limit, rows.size());

      // probably should validate currentRowIndex
    }
  }
  */

  /**
   * Truncate leading rows.
   *
   * @param offset the number of leading rows to truncate
   */
  /*
  public void offset(int offset) {

    if (offset == 0) {

      // do nothing
    }
    else if (offset < rows.size()) {

      logger.warn("Removing 0-" + offset);
      ( (RowList) rows).removeRange(0, offset);
      currentRowIndex -= offset;

      if (currentRowIndex < 0) {

        currentRowIndex = 0;
      }
    }
    else if (offset >= rows.size()) {

      rows.clear();
    }
    else {

      throw new IllegalArgumentException("Illegal offset: " + offset);
    }
  }
  */

  /**
   * Initialises the column names and metadata from the given metadata object.
   *
   * @param newMetaData PARAMETER TO DO
   * @throws SQLException if there was an error getting the metadata attributes.
   */
  private void initialiseMetaData(ResultSetMetaData newMetaData) throws
      SQLException {

    int columnNameCount = newMetaData.getColumnCount();
    columnNames = new String[columnNameCount];

    for (int i = 0; i < columnNameCount; i++) {

      columnNames[i] = newMetaData.getColumnName(i + 1);
    }

    // initialise the metadata field
    metaData = new ResultSetMetaDataImpl(columnNames);
  }

  public int getHoldability() throws SQLException {
    // TODO Auto-generated method stub
    return 0;
  }

  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public NClob getNClob(int columnIndex) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public NClob getNClob(String columnLabel) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public String getNString(int columnIndex) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public String getNString(String columnLabel) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public RowId getRowId(int columnIndex) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public RowId getRowId(String columnLabel) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  public boolean isClosed() throws SQLException {
    // TODO Auto-generated method stub
    return false;
  }

  public void updateAsciiStream(int columnIndex, InputStream x, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateAsciiStream(String columnLabel, InputStream x, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateBinaryStream(int columnIndex, InputStream x, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateBinaryStream(String columnLabel, InputStream x, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateBlob(int columnIndex, InputStream inputStream, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateBlob(String columnLabel, InputStream inputStream,
      long length) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateCharacterStream(int columnIndex, Reader x, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateCharacterStream(String columnLabel, Reader reader,
      long length) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateClob(int columnIndex, Reader reader, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateClob(String columnLabel, Reader reader, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNCharacterStream(int columnIndex, Reader x, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNCharacterStream(String columnLabel, Reader reader,
      long length) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNClob(int columnIndex, NClob clob) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNClob(String columnLabel, NClob clob) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNClob(int columnIndex, Reader reader, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNClob(String columnLabel, Reader reader, long length)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNString(int columnIndex, String string) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNString(String columnLabel, String string)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateSQLXML(int columnIndex, SQLXML xmlObject)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateSQLXML(String columnLabel, SQLXML xmlObject)
      throws SQLException {
    // TODO Auto-generated method stub
    
  }

  public void updateNCharacterStream(int columnIndex, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateAsciiStream(int columnIndex, InputStream inputStream) throws SQLException {
    // Empty stub
  }

  public void updateBinaryStream(int columnIndex, InputStream inputStream) throws SQLException {
    // Empty stub
  }

  public void updateCharacterStream(int columnIndex, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateAsciiStream(String columnLabel, InputStream inputStream) throws SQLException {
    // Empty stub
  }

  public void updateBinaryStream(String columnLabel, InputStream inputStream) throws SQLException {
    // Empty stub
  }

  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    // Empty stub
  }

  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    // Empty stub
  }

  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    // Empty stub
  }

  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    // Empty stub
  }

  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    // TODO Auto-generated method stub
    return false;
  }

  public <T> T unwrap(Class<T> iface) throws SQLException {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Comparator class used by the
   * {@link #join(MulgaraResultSet, String[], String[])} method.
   */
  //class ColumnComparator implements Comparator {

    /**
     * The columns to sort by, 0-indexed.
     *
     */
    //private String[] columnNames;

    /**
     * @param columnNames the names of the columns to use as a sort order
     */
    /*
    public ColumnComparator(String[] columnNames) {

      // Validate columnNames2
      if (columnNames == null) {

        throw new IllegalArgumentException("Null columnNames parameter");
      }

      // Initialize fields
      this.columnNames = columnNames;
    }

    public int compare(Object a, Object b) {

      // Validate parameters
      if (! (a instanceof ResultSetRow && b instanceof ResultSetRow)) {

        throw new RuntimeException("Can't compare " + a.getClass() + " to " +
            b.getClass());
      }

      // Delegate execution to private, type-specific method
      return compare( (ResultSetRow) a, (ResultSetRow) b);
    }
    */

    /**
     * Ordering on {@link ResultSetRow}s. The ordering is by the {@link
     * Object#toString} value of the fields, earlier columns having higher
     * precedence and <code>null</code> ordering before all other field values.
     *
     * @param a PARAMETER TO DO
     * @param b PARAMETER TO DO
     * @return RETURNED VALUE TO DO
         * @deprecated Ordering by {@link Object#toString} value is a terrible idea.
     */
    /*
    private int compare(ResultSetRow a, ResultSetRow b) {

      // Compare each column in turn, returning an ordering if they're unequal
      for (int i = 0; i < columnNames.length; i++) {

        Object a2 = a.getObject(columnNames[i]);
        Object b2 = b.getObject(columnNames[i]);

        // Handle null comparisons
        if ( (a2 == null) && (b2 == null)) {

          continue;
        }

        // A = B
        if (a2 == null) {

          return -1;
        }

        // A < B
        if (b2 == null) {

          return +1;
        }

        // A > B
        // Handle non-null comparisons
        int difference;

        if (a2 instanceof Comparable) {

          difference = ( (Comparable) a2).compareTo(b2);
        }
        else {

          if (logger.isDebugEnabled()) {

            logger.debug("Falling back to lexical comparison for " +
                a2.getClass());
          }

          difference = a2.toString().compareTo(b2.toString());
        }

        // Return if an order has been established
        if (difference != 0) {

          return difference;
        }
      }

      // The rows must be equal (insofar as the named columns go)
      return 0;
    }
    */
  //}
}
