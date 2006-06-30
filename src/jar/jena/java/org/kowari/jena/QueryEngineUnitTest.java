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

package org.kowari.jena;

// Java APIs
import java.net.InetAddress;
import java.net.URI;

// Log4j
import org.apache.log4j.*;

// Junit API
import junit.framework.*;

// Jena API
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.rdql.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.shared.*;

// Internal Kowari APIs
import org.kowari.server.SessionFactory;
import org.kowari.server.driver.SessionFactoryFinder;

/**
 * Test case for {@link GraphKowari}.
 *
 * @created 2003-02-09
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class QueryEngineUnitTest extends TestCase {

  /**
   * init the logging class
   */
  private static Logger logger =
      Logger.getLogger(QueryEngineUnitTest.class.getName());

  /**
   * The URI of the server.
   */
  protected URI serverURI;

  /**
   * The name of the test model.
   */
  protected final static String SERVER_NAME = "server1";

  /**
   * The test model.
   */
  protected Model model;

  /**
   * The graph Kowari maker.
   */
  protected GraphKowariMaker graphMaker;

  /**
   * Default constructor for unit test.
   *
   * @param name the unit test name
   * @throws Exception any exception.
   */
  public QueryEngineUnitTest(String name) {
    super(name);
  }

  /**
   * Answer a test suite that runs the Graph and Reifier tests on GraphMem
   * and on  WrappedGraphMem, the latter standing in for testing
   * WrappedGraph.
   */
  public static TestSuite suite() {

    TestSuite suite = new TestSuite();
    suite.addTest(new QueryEngineUnitTest("testSimpleQuery"));
    return suite;
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) throws Exception {

    junit.textui.TestRunner.run(suite());
  }

  /**
   * Setups up the test for JUnit - creates the database and graph ready for
   * use.
   *
   * @throws Exception if there was an error creating the database - always
   *     fatal.
   */
  public void setUp() throws Exception {
    boolean exceptionOccurred = true;
    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
          serverURI, false);
      LocalJenaSession session = (LocalJenaSession) sessionFactory.
          newJenaSession();

      graphMaker = new GraphKowariMaker((LocalJenaSession)
          session, serverURI, ReificationStyle.Minimal);
      ModelKowariMaker modelMaker = new ModelKowariMaker(graphMaker);

      model = modelMaker.createModel("test");

      super.setUp();

      exceptionOccurred = false;
    }
    catch (Exception e) {

      e.printStackTrace();
    }
    finally {
      if (exceptionOccurred)
        tearDown();
    }
  }

  /**
   * Test adding statements using nodes and detecting whether they are in the
   * Graph.
   */
  public void testSimpleQuery() {

    try {
      Statement st = model.createStatement(model.createResource(), RDF.type,
          model.createLiteral("chat"));
      model.add(st);

      RdqlQuery q = new RdqlQuery("select ?x ?y ?z WHERE (?x ?y ?z)");
      q.setSource(model);
      QueryExecution qe = new KowariQueryEngine(q);
      QueryResults results = qe.exec();
      assertTrue(results.hasNext());

      while (results.hasNext()) {
        ResultBinding binding = (ResultBinding) results.next();
        assertTrue((st.getSubject().equals(binding.get("x"))));
        assertTrue(st.getPredicate().equals(binding.get("y")));
        assertTrue(st.getObject().equals(binding.get("z")));
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void tearDown() throws Exception {
    try {
      if (model != null) {
        model.close();
      }
    }
    finally {
      model = null;
    }

    try {
      if (graphMaker != null) {
        graphMaker.removeAll();
        graphMaker.close();
      }
    }
    finally {
      graphMaker = null;
    }
  }
}
