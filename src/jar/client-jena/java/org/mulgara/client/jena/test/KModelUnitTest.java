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

package org.mulgara.client.jena.test;

// third party packages
import junit.framework.*;

// Java 2 standard packages
import java.net.*;
import java.io.*;
import java.net.URI;
import javax.xml.parsers.*;

//Log4J
import org.apache.log4j.Category;

// local classes
import org.mulgara.itql.ItqlInterpreterBean;
import org.mulgara.query.Answer;
import org.mulgara.client.jena.*;
import org.mulgara.client.jena.kmodel.*;

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdql.*;

/**
 * Unit test for client-side Jena. <p>
 *
 * @created 2004-08-16
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/27 11:30:01 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 *  @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KModelUnitTest
    extends TestCase {

  /** the category to send logging info to */
  private static Category log =
      Category.getInstance(KModelUnitTest.class.getName());

  /** the ITQL command interpreter. Used to execute iTQL. */
  private static ItqlInterpreterBean interpreterBean = null;

  /** Description of the Field */
  protected URI serverURI = null;

  /** Description of the Field */
  protected String hostname = null;

  /** The URI of the test graph to create. */
  protected URI graphURI = null;

  /** The URI of the test "text" graph to create. */
  protected URI textGraphURI = null;

  /** Description of the Field */
  protected final static String SERVER_NAME = "server1";

  /** Small dataset used to test model */
  private static final String testStatements =
      "<http://mulgara.org/mulgara#test1> <http://mulgara.org/mulgara#test4> <http://mulgara.org/mulgara#test2> " +
      "<http://mulgara.org/mulgara#test1> <http://mulgara.org/mulgara#test5> <http://mulgara.org/mulgara#test3> " +
      "<http://mulgara.org/mulgara#test1> <http://mulgara.org/mulgara#test6> 'Test1' " +
      "<http://mulgara.org/mulgara#test2> <http://mulgara.org/mulgara#test4> <http://mulgara.org/mulgara#test1> " +
      "<http://mulgara.org/mulgara#test2> <http://mulgara.org/mulgara#test5> <http://mulgara.org/mulgara#test3> " +
      "<http://mulgara.org/mulgara#test2> <http://mulgara.org/mulgara#test6> 'Test2' " +
      "<http://mulgara.org/mulgara#test3> <http://mulgara.org/mulgara#test4> <http://mulgara.org/mulgara#test1> " +
      "<http://mulgara.org/mulgara#test3> <http://mulgara.org/mulgara#test5> <http://mulgara.org/mulgara#test2> " +
      "<http://mulgara.org/mulgara#test3> <http://mulgara.org/mulgara#test6> 'Test3' ";

  /**
   * Directory for test files
   */
  private static String TEST_DIR = System.getProperty("cvs.root") + "/test/";

  /**
   * Test file used for memory writer
   */
  private static String CLIENT_TEST_FILE1 = TEST_DIR + "ClientJenaTest1.rdf";

  /**
   * Test file used for memory writer
   */
  private static String CLIENT_TEST_FILE2 = TEST_DIR + "ClientJenaTest2.rdf";

  /**
   * Constructs a new ItqlInterpreter unit test.
   *
   * @param name the name of the test
   * @throws Exception
   */
  public KModelUnitTest(String name) throws Exception {

    // delegate to super class constructor
    super(name);

    //create a testing model

  }

  /**
   * Returns a test suite containing the tests to be run.
   *
   * @return the test suite
   * @throws Exception
   */
  public static Test suite() throws Exception {

    TestSuite suite = new TestSuite();
    suite.addTest(new KModelUnitTest("testCreate"));
    suite.addTest(new KModelUnitTest("testKModel"));
    return suite;
  }

  /**
   * Default text runner.
   *
   * @param args the command line arguments
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    junit.textui.TestRunner.run(suite());
  }

  // suite()
  //
  // Test cases
  //

  /**
   * Tests the creation of a Client Graph by an AbstractGraphFactory.
   *
   * @throws Exception
   */
  public void testCreate() throws Exception {

    Model model = null;

    // log that we're executing the test
    log.debug("Starting Create test");

    try {

      //get a Jena model from the Factory
      model = AbstractJenaFactory.newKModel(serverURI, graphURI);
      model.close();

      //get a Jena model from the Factory with a text backend
      model = AbstractJenaFactory.newKModel(serverURI, graphURI);

      // log that we've completed the test
      log.debug("Completed Create test");
    } finally {

      if (model != null) {

        model.close();
      }
    }
  }

  /**
   * Test from KModelTest class. Slightly modified to compile. Needs refactoring
   * with integration of KModel (add asserts and more tests).
   *
   * @throws Exception
   */
  public void testKModel() throws Exception {

    Model model = null;

    // log that we're executing the test
    log.debug("Starting KModel test");

    try {

      //used to keep count of statements
      long statementCount = 0;

      KModel.DEBUG = true;

      log.debug("Constructing KModel for " + this.graphURI + " . . .");

      model = KModel.getInstance(serverURI, graphURI);

      //count the existing statements
      statementCount = model.size();

      log.debug("There are " + statementCount + " statements in the model.");
      log.debug("Adding a statement.");

      Statement st = model.createStatement(model.createResource(
          "urn:example:Chris"),
                                           model.createProperty("urn:example:",
          "hasWife"),
                                           model.createResource(
          "urn:example:Debbie"));
      model.add(st);

      //check that a statement was added
      statementCount++;
      this.assertEquals("Adding statement did not alter size. Expected: " +
                        statementCount + ", found: " + model.size(),
                        statementCount, model.size());

      //remove
      log.debug("There are " + model.getGraph().size() +
                " statements in the model.");
      log.debug("Deleting it.");

      model.remove(st);

      //check that a statement was added
      statementCount--;
      this.assertEquals("Removing statement did not alter size. Expected: " +
                        statementCount + ", found: " + model.size(),
                        statementCount, model.size());

      log.debug("There are " + model.getGraph().size() +
                " statements in the model.");

      Resource home = model.createResource();
      Statement chrisHasHome = model.createStatement(model.createResource(
          "urn:example:Chris"),
          model.createProperty("urn:example:",
                               "hasHome"), home);

      Statement homeNearLake = model.createStatement(home,
          model.createProperty("urn:example:", "isNear"),
          model.createResource("urn:example:CayugaLake"));

      log.debug("Adding two statements involving an anonymous resource.");
      model.add(new Statement[] {chrisHasHome, homeNearLake});
      log.debug("There are " + model.getGraph().size() +
                " statements in the model.");

      log.debug("Removing those statements involving an anonymous resource.");
      model.remove(new Statement[] {chrisHasHome, homeNearLake});
      log.debug("There are " + model.getGraph().size() +
                " statements in the model.");

      //test that a blank node can be inserted/removed/queried
      Resource blankNode = model.createResource();
      Property predicate = model.createProperty("urn:example:", "blankNode");
      Statement blankNodeTriple = model.createStatement(blankNode, predicate,
          blankNode);
      log.debug("Inserting using blank Nodes");
      model.add(blankNodeTriple);
      log.debug("Selecting using blank Nodes");
      Selector selector = new SimpleSelector(blankNode, predicate, blankNode);
      model.query(selector);
      log.debug("Removing using blank Nodes");
      model.remove(blankNodeTriple);

      // log that we've completed the test
      log.debug("Completed KModel test");
    } finally {

      if (model != null) {

        model.close();
      }
    }
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @return Answer
   * @param modelURI URI
   * @throws Exception
   */
  private Answer createModel(URI modelURI) throws Exception {

    //select ALL query
    String query = "create <" + modelURI + "> ;";

    return this.interpreterBean.executeQuery(query);
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @return Answer
   * @param modelURI URI
   * @throws Exception
   */
  private Answer dropModel(URI modelURI) throws Exception {

    //select ALL query
    String query = "drop <" + modelURI + "> ;";

    return this.interpreterBean.executeQuery(query);
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @return Answer
   * @param modelURI URI
   * @throws Exception
   */
  private Answer populateModel(URI modelURI) throws Exception {

    //select ALL query
    String query = "insert " + this.testStatements +
        "into <" + modelURI + "> ;";

    return this.interpreterBean.executeQuery(query);
  }

  //set up and tear down

  /**
   * Initialise members.
   *
   * @throws Exception if something goes wrong
   */
  public void setUp() throws Exception {

    // Store persistence files in the temporary directory
    try {

      hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      graphURI = new URI("rmi", hostname, "/" + SERVER_NAME, "clientJenaTest");
      this.textGraphURI = new URI("rmi", hostname, "/" + SERVER_NAME,
                                  "textTest");

      //create an iTQLInterpreterBean for executing queries with
      this.interpreterBean = new ItqlInterpreterBean();

      //initialize with server
      this.interpreterBean.setServerURI(this.serverURI.toString());

      //initialize model
      this.createModel(this.graphURI);
      this.createModel(this.textGraphURI);
      this.populateModel(this.graphURI);
      this.populateModel(this.textGraphURI);
    }
    catch (Exception exception) {

      //try to tear down first
      try {

        tearDown();
      }
      finally {

        throw exception;
      }
    }
  }

  /**
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void tearDown() throws Exception {

    this.dropModel(this.graphURI);
    this.dropModel(this.textGraphURI);

    // Close interpreter
    this.interpreterBean.close();
  }

  /**
   * Deletes all files in the directory
   *
   * @param dir directory to clear
   */
  private void removeContents(File dir) {

    File[] files = dir.listFiles();

    if (files != null) {

      for (int i = 0; i < files.length; ++i) {

        if (files[i].isFile()) {

          files[i].delete();
        }
      }
    }
  }
}
