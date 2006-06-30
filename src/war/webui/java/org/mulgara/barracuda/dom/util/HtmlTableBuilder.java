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
 * The Original Code is the Mulgara Metadata Store.
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

package org.mulgara.barracuda.dom.util;

import org.w3c.dom.Node;
import org.w3c.dom.html.*;

/**
 * This class provides utility methods to work with HTML tables as a DOM.
 *
 * @created 2002-01-17
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:48 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class HtmlTableBuilder {

  // ===================== Row based ======================

  /**
   * Insert a cell into row.
   *
   * @param row The row to insert into.
   * @param cell The cell to insert. NOTE: This cell must be a clone and not an
   *      existing cell.
   * @param index The index to insert at starting at 0
   */
  public static void insertCell(HTMLTableRowElement row,
    HTMLTableCellElement cell, int index) {

    // Insert a placeholder cell.
    HTMLElement tmpCell = row.insertCell(index);

    // Import node if required.
    Node cellNode = cell;

    if (cell.getOwnerDocument() != row.getOwnerDocument()) {

      cellNode = row.getOwnerDocument().importNode(cell, true);
    }

    // Replace placeholder cell
    row.replaceChild(cellNode, tmpCell);
  }

  /**
   * Prepend a cell to a row. (Same as inserting a cell at an index of 0)
   *
   * @param row The row to prepend the cell to.
   * @param cell The cell to prepend. NOTE: This cell must be a clone and not an
   *      existing cell.
   */
  public static void prependCell(HTMLTableRowElement row,
    HTMLTableCellElement cell) {

    insertCell(row, cell, 0);
  }

  /**
   * Append a cell to a row. (Same as inserting a cell at an index equal to the
   * lenght of the row)
   *
   * @param row The row to append the cell to.
   * @param cell The cell to append. NOTE: This cell must be a clone and not an
   *      existing cell.
   */
  public static void appendCell(HTMLTableRowElement row,
    HTMLTableCellElement cell) {

    int rowLength = row.getCells().getLength();
    insertCell(row, cell, rowLength);
  }

  /**
   * Extend a row horizontally to the right using the last cell as a template.
   *
   * @param row The row to extend.
   * @param numCells The number of cells to add.
   */
  public static void extendRow(HTMLTableRowElement row, int numCells) {

    // Get the last cell
    HTMLCollection cellCollection = row.getCells();
    int rowLength = cellCollection.getLength();
    HTMLTableCellElement cell =
      (HTMLTableCellElement) cellCollection.item(rowLength - 1);

    // Append it until there are enough
    for (int i = 0; i < numCells; i++) {

      HTMLTableCellElement newCell =
        (HTMLTableCellElement) cell.cloneNode(true);
      appendCell(row, newCell);
    }
  }

  // ===================== Table based ======================

  /**
   * Insert a row into a table.
   *
   * @param table The table to insert into.
   * @param row The row to insert. NOTE: This row must be a clone and not an
   *      existing row.
   * @param index The index to insert at starting at 0
   */
  public static void insertRow(HTMLTableElement table, HTMLTableRowElement row,
    int index) {

    // Import node if required.
    if (row.getOwnerDocument() != table.getOwnerDocument()) {

      row =
        (HTMLTableRowElement) table.getOwnerDocument().importNode(row, true);
    }

    // Insert a placeholder row.
    HTMLElement tmpRow = table.insertRow(index);

    // Replace placeholder row
    table.replaceChild(row, tmpRow);
  }

  /**
   * Prepend a row to a table. (Same as inserting a row at an index of 0)
   *
   * @param table The table to prepend the row to.
   * @param row The row to prepend. NOTE: This row must be a clone and not an
   *      existing row.
   */
  public static void prependRow(HTMLTableElement table, HTMLTableRowElement row) {

    insertRow(table, row, 0);
  }

  /**
   * Append a row to a table. (Same as inserting a row at an index equal to the
   * lenght of the table)
   *
   * @param table The table to append the row to.
   * @param row The row to append. NOTE: This row must be a clone and not an
   *      existing row.
   */
  public static void appendRow(HTMLTableElement table, HTMLTableRowElement row) {

    int tableLength = table.getRows().getLength();
    insertRow(table, row, tableLength);
  }

  /**
   * Add columns to a table. The last cell in each row is used as the template
   * cell for extending the row.
   *
   * @param table The table to add columns to.
   * @param numColumns The number of columns.
   */
  public static void addColumns(HTMLTableElement table, int numColumns) {

    // Get the rows
    HTMLCollection rowCollection = table.getRows();
    int rowLength = rowCollection.getLength();

    // Extend each row
    for (int i = 0; i < rowLength; i++) {

      HTMLTableRowElement row = (HTMLTableRowElement) rowCollection.item(i);
      extendRow(row, numColumns);
    }
  }

  /**
   * Add rows to a table. The last row is used as the template row for extending
   * the table.
   *
   * @param table The table to add rows to.
   * @param numRows The number of rows to add.
   */
  public static void addRows(HTMLTableElement table, int numRows) {

    // Get the last row
    HTMLCollection rowCollection = table.getRows();
    int tableHeight = rowCollection.getLength();
    HTMLTableRowElement row =
      (HTMLTableRowElement) rowCollection.item(tableHeight - 1);

    // Append it until there are enough
    for (int i = 0; i < numRows; i++) {

      HTMLTableRowElement newRow = (HTMLTableRowElement) row.cloneNode(true);
      appendRow(table, newRow);
    }
  }
}
