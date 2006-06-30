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

package org.mulgara.client.drools;

import org.mulgara.itql.ItqlInterpreterBean;
import org.mulgara.itql.ItqlInterpreterException;
import org.mulgara.query.Answer;
import org.mulgara.query.TuplesException;


/**
 * Rule object for implementing RDFS in Drools configuration.
 *
 * @created 2004-07-09
 *
 * @author Paul Gearon
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:33 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2004
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class RDFSRule {

  /** Used to tag the position in a query where the source model should go. */
  protected static final String SOURCE_TAG = "@@SOURCE@@";

  /** The name of this rule. */
  protected String name;

  /** The string of the query to execute for this rule. */
  protected String queryString;

  /** The current database itql interpreter bean. */
  protected ItqlInterpreterBean itql;

  /** The name of the inference model. */
  protected String inferenceModel;

  /** The name of the base model. */
  protected String sourceModel;

  /** The last count of results from the query for this rule. */
  protected long cachedRows;
  
  /** The number of rows that an insertion is expected to update a dataset to */
  protected long newRows;


  /**
   * Main constructor.  This is called in the bootstrap rule of a Drools configuration.
   *
   * @param init The bootstrap object, contains all info needed for a session.
   * @param name The name of this rule object.
   * @param query The iTQL query which generates the rules.
   */
  public RDFSRule(Bootstrap init, String name, String query) {
    // get initialization data from the bootstrap object
    this.itql = init.getItqlInterpreterBean();
    this.inferenceModel = init.getInferenceModel();
    this.sourceModel = init.getSourceModel();

    this.name = name;
    // update the query string with the name of the source model
    this.queryString = query.replaceAll(SOURCE_TAG, "<" + sourceModel + "> or <" + inferenceModel + ">");
    // initialise the number of rows read from the query
    cachedRows = 0;
    newRows = 0;
  }


  /**
   * Retrieves the name of the current rule.
   *
   * @return The name of this rule.
   */
  public String getName() {
    return name;
  }


  /**
   * Checks for the number of entries that an inference will return.  If this number has changed
   * since the last check, then return true, otherwise return false.
   *
   * @return true if a rule needs to be re-executed
   * @throws ItqlInterpreterException There was an error in the iTQL used for the test query
   * @throws TuplesException There was an error processing the query
   */
  public boolean updateNeeded() throws ItqlInterpreterException, TuplesException, Throwable {
    System.out.println("[" + name + "] Checking for update");

    // look for the rows relevant to this rule
    Answer ans;
    try {
      ans = itql.executeQuery(queryString);  // catch
    } catch (ItqlInterpreterException ite) {
      System.err.println("[" + name + "] Interpreter Exception: " + ite.getMessage());
      ite.printStackTrace();
      throw ite;
    }

    // check if the count is different to what we thought it was
    long rows = ans.getRowCount();  // catch

    System.out.println("[" + name + "] old count = " + cachedRows + ", new count = " + rows);
    if (cachedRows < rows) {
      newRows = rows;
      return true;
    }
    assert cachedRows == rows;
    return false;
  }


  /**
   * Insert the data for the query into the inference model.
   *
   * @throws ItqlInterpreterException There was an error in the iTQL used for the insert
   */
  abstract public void insertData() throws ItqlInterpreterException;


  /**
   * Update the number of rows to remember when looking to see if changes have occurred.
   */
  protected void updateRowCache() {
    cachedRows = newRows;
  }

}

