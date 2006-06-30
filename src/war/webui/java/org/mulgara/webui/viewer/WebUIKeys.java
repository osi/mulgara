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

package org.kowari.webui.viewer;

/**
 * Stores keys for accessing page, session and request data.
 *
 * @created 2002-01-10
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:50 $
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
public class WebUIKeys {

  /**
   * Key string for model URI query in page, session and requests
   */
  public final static String MODEL_URI = "ModelURI";

  /**
   * Key string for example query in page, session and requests
   */
  public final static String EXAMPLE_QUERY = "ExampleQuery";

  /**
   * Key string for actual query text in page, session and requests
   */
  public final static String QUERY_TEXT = "QueryText";

  /**
   * Key string for node query literal in links and requests
   */
  public final static String QUERY_LITERAL = "QueryLiteral";

  /**
   * Key string for node query resource in links and requests
   */
  public final static String QUERY_RESOURCE = "QueryResource";

  /**
   * Key string for executed query text in the page and session
   */
  public final static String QUERY_EXECUTED = "QueryExecuted";

  /**
   * Key string for ITQL Interpreter
   */
  public final static String ITQL_INT = "ITQLInterpreter";

  /**
   * Key string for results in session data
   */
  public final static String RESULT = "Result";

  /**
   * Key string for result row in page
   */
  public final static String RESULT_ROW = "ResultRow";

  /**
   * Key string for result stats in page and session
   */
  public final static String RESULT_STATS = "ResultStats";

  /**
   * Key string for result index into result list
   */
  public final static String RESULT_INDEX = "ResultIndex";

  /**
   * Key string for result index into result list
   */
  public final static String CONTROLLER_FLAG = "FromController";



}
