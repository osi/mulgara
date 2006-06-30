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

package org.mulgara.itql.examples;

// Logging
import org.apache.log4j.*;

// Internal packages.
import org.mulgara.itql.ItqlInterpreterBean;
import org.mulgara.query.Answer;

/**
 * A simple Kowari client to demonstrate the Kowari java API. The client will print
 * out the contents of the model specified on the command line.
 *
 * @created 2002-11-02
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:16 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2002 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class SimpleClient {

  /**
   * Get line separator.
   */
  private static final String eol = System.getProperty("line.separator");

  /**
   * iTQL Bean used to query Kowari
   */
  private ItqlInterpreterBean interpreter;

  /**
   * Public constructor.
   */
  public SimpleClient() {

    // Initialise logging
    BasicConfigurator.configure();
    LogManager.getLoggerRepository().setThreshold(Level.OFF);

    // Create the iTQL Bean to use for queries
    interpreter = new ItqlInterpreterBean();
  }

  /**
   * Runs this class from the command line.
   *
   * @param args Takes the name of the model to connect to and print out. eg
   *      <code>rmi://hostname.domain/server1#</code>
   */
  public static void main(String[] args) {

    if ( (args.length != 1) || (args[0].length() == 0)) {

      System.out.println(eol + "Please specify the URI of the model to query." +
          " eg rmi://hostname.domain/server1#" + eol);
      System.exit(0);
    }

    SimpleClient client = new SimpleClient();
    client.displayModel(args[0]);
    client.close();
  }

  /**
   * Prints out the contents of a model.
   *
   * @param modelName The name of the model query.
   */
  public void displayModel(String modelName) {

    try {

      // Query to select all subject-predicate-object statements from the model
      String query =
          "select $s $p $o from <" + modelName + "> where $s $p $o ;";

      // Do the query
      Answer answer = interpreter.executeQuery(query);

      // Print out the results
      System.out.println("\nQuery Results for <" + modelName + ">:" + eol);

      answer.beforeFirst();

      while (answer.next()) {

        Object subject = answer.getObject(0);
        Object predicate = answer.getObject(1);
        Object object = answer.getObject(2);

        System.out.println("Subject: " + subject + ", Predicate: " + predicate +
            ", Object: " + object);
      }
      answer.close();

      System.out.println();
    }
    catch (Exception e) {

      System.out.println(eol + "An Exception occurred: " + eol + e);
    }
  }

  /**
   * Closes the underlying itql interpreter bean.
   */
  public void close() {
    if (interpreter != null) {
      interpreter.close();
    }
  }
}
