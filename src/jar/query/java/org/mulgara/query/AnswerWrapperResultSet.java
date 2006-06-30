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

package org.mulgara.query;

// Java 2 standard packages
import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.*;

// Third party packages
import org.apache.log4j.*;

// Local packages
import org.mulgara.util.AbstractResultSet;

/**
 * Wrapper to convert an {@link Answer} into a {@link ResultSet}.
 *
 * @created 2004-03-08
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class AnswerWrapperResultSet extends AbstractResultSet {

  /**
   * Logger.
   *
   * This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(AnswerWrapperResultSet.class.getName());

  /**
   * The wrapped instance.
   */
  private final Answer answer;

  //
  // Constructor
  //

  /**
   * Wrap an {@link Answer} so that it functions as a {@link ResultSet}.
   *
   * @param answer  the instance to wrap
   */
  public AnswerWrapperResultSet(Answer answer) {
    // Validate "answer" parameter
    if (answer == null) {
      throw new IllegalArgumentException("Null \"answer\" parameter");
    }

    // Initialize fields
    this.answer = answer;
  }

  //
  // Methods for accessing results by column index
  //

  /**
   * @param columnIndex PARAMETER TO DO
   * @return The String value
   * @throws SQLException on failure
   */

  public String getString(int columnIndex) throws SQLException {
    try {
      return answer.getObject(columnIndex - 1).toString();
    }
    catch (TuplesException e) {
      throw new SQLException(e.toString());
    }
  }

  //
  // Advanced features:
  //

  /**
   * @return The MetaData value
   * @throws SQLException on failure
   */
  public ResultSetMetaData getMetaData() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @param columnIndex PARAMETER TO DO
   * @return The Object value
   * @throws SQLException on failure
   */
  public Object getObject(int columnIndex) throws SQLException {
    try {
      return answer.getObject(columnIndex - 1);
    }
    catch (TuplesException e) {
      throw new SQLException(e.toString());
    }
  }

  //--------------------------JDBC 2.0-----------------------------------

  //
  // Traversal/Positioning
  //

  /**
   * @return The BeforeFirst value
   * @throws SQLException on failure
   */
  public boolean isBeforeFirst() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return The AfterLast value
   * @throws SQLException on failure
   */
  public boolean isAfterLast() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return The First value
   * @throws SQLException on failure
   */
  public boolean isFirst() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return The Last value
   * @throws SQLException on failure
   */
  public boolean isLast() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return The Row value
   * @throws SQLException on failure
   */
  public int getRow() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean next() throws SQLException {
    try {
      return answer.next();
    }
    catch (TuplesException e) {
      throw new SQLException(e.toString());
    }
  }

  /**
   * @throws SQLException on failure
   */
  public void close() throws SQLException {
    try {
      answer.close();
    }
    catch (TuplesException e) {
      throw new SQLException(e.toString());
    }
  }

  //----------------------------------------------------------------

  /**
   * @throws SQLException on failure
   */
  public void beforeFirst() throws SQLException {
    try {
      answer.beforeFirst();
    }
    catch (TuplesException e) {
      throw new SQLException(e.toString());
    }
  }

  /**
   * @throws SQLException on failure
   */
  public void afterLast() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean first() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean last() throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @param row PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean absolute(int row) throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }

  /**
   * @param rows PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws SQLException on failure
   */
  public boolean relative(int rows) throws SQLException {
    throw new SQLException(NOT_IMPLEMENTED);
  }
}
