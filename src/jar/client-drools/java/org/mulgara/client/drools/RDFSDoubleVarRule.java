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
public class RDFSDoubleVarRule extends RDFSRule {

  /** The property to use to create [$x property $x] triples from. */
  protected String property;

  /**
   * Main constructor.  This is called in the bootstrap rule of a Drools configuration.
   *
   * @param init The bootstrap object, contains all info needed for a session.
   * @param name The name of this rule object.
   * @param query The iTQL query which generates the rules.
   */
  public RDFSDoubleVarRule(Bootstrap init, String name, String query, String property) {
    super(init, name, query);
    System.out.println("Initializing double var Rule \"" + name + "\" with query: " + query);
    this.property = property;
  }


  /**
   * Insert the data for the query into the inference model.
   */
  public void insertData() throws ItqlInterpreterException {
    System.out.println("Insertion triggered for rule: " + name);

    Answer ans;
    try {
        ans = itql.executeQuery(queryString);  // catch
    } catch (ItqlInterpreterException ite) {
        System.err.println("[" + name + "] Interpreter Exception: " + ite.getMessage());
        ite.printStackTrace();
        throw ite;
    }

    StringBuffer cmdQuery = new StringBuffer("insert ");
    String predFragment = "> " + property + " <";

    try {
      ans.beforeFirst();
      while (ans.next()) {
        cmdQuery.append("<").append(ans.getObject(0).toString()).append(predFragment);
        cmdQuery.append(ans.getObject(0).toString()).append("> ");
      }
      cmdQuery.append("into <").append(inferenceModel).append("> ;");
    } catch (TuplesException te) {
      System.err.println("Error processing query response." + te.getMessage());
      throw new ItqlInterpreterException(te);
    }
    System.out.println("[" + name + "] executing: " + cmdQuery);
    
    try {
      itql.executeUpdate(cmdQuery.toString());  // catch
    } catch (ItqlInterpreterException e) {
      System.err.println("Error while inserting: " + cmdQuery);
      System.err.println(e.getMessage());
      e.printStackTrace();
      throw e;
    }

    // update the row count
    updateRowCache();
  }

}

