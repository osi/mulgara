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

package org.mulgara.jena;

// Java APIs
import java.net.InetAddress;
import java.net.URI;

// logging and unit testing packages
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

// Jean packages
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.test.AbstractTestReifiedStatements;
import com.hp.hpl.jena.rdf.model.test.TestReifiedStatements;
import com.hp.hpl.jena.shared.ReificationStyle;

// local packages
import org.kowari.server.SessionFactory;
import org.kowari.server.driver.SessionFactoryFinder;


/**
 * Test case for {@link ModelMulgara} testing reification.
 *
 * @created 2004-17-09
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
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ReifiedStatementsUnitTest extends TestReifiedStatements {

  /**
   * init the logging class
   */
  private static Logger logger =
      Logger.getLogger(ReifiedStatementsUnitTest.class.getName());

  /**
   * The URI of the server.
   */
  protected static URI serverURI;

  /**
   * The URI of the graph.
   */
  protected static URI graphURITest;

  /**
   * The name of the test model.
   */
  protected final static String SERVER_NAME = "server1";

  /**
   * The session.
   */
  protected static LocalJenaSession session = null;

  /**
   * The Kowari model that we're adding statements to.
   */
  protected ModelMulgara model;

  /**
   * Creates new graph.
   */
  protected static GraphMulgaraMaker graphMakerMinimal, graphMakerStandard,
      graphMakerConvenient = null;

  /**
   * Creates new models.
   */
  protected static ModelMulgaraMaker modelMakerMinimal, modelMakerStandard,
      modelMakerConvenient = null;


  /**
   * Calls the super classes constructor.
   *
   * @param name the name of the JUnit task.
   */
  public ReifiedStatementsUnitTest(String name) {
    super(name);
  }

  public static TestSuite suite() {
    TestSuite result = new TestSuite();

    result.addTest(new TestSuite(TestStandard.class));
    result.addTest(new TestSuite(TestConvenient.class));
    result.addTest(new TestSuite(TestMinimal.class));
    return result;
  }

  public Model getModel() {
    return modelMakerMinimal.createModel();
  }

  public static class TestStandard extends AbstractTestReifiedStatements {

    String newName;
    public static final ReificationStyle style = ModelFactory.Standard;

    public TestStandard(String name) {
      super(name);
      newName = name;
    }

    public void setUp() {
      try {
        ReifiedStatementsUnitTest.setUpDatabase();
        super.setUp();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void tearDown() {
      try {
        super.tearDown();
        ReifiedStatementsUnitTest.tearDownDatabase();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public Model getModel() {
      return modelMakerStandard.createModel();
    }

    public void testStyle() {
      assertEquals(style, getModel().getReificationStyle());
    }
  }

  public static class TestConvenient extends AbstractTestReifiedStatements {

    public static final ReificationStyle style = ModelFactory.Convenient;

    public TestConvenient(String name) {
      super(name);
    }

    public void setUp() {
      try {
        ReifiedStatementsUnitTest.setUpDatabase();
        super.setUp();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void tearDown() {
      try {
        super.tearDown();
        ReifiedStatementsUnitTest.tearDownDatabase();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public Model getModel() {
      return modelMakerConvenient.createModel();
    }

    public void testStyle() {
      assertEquals(style, getModel().getReificationStyle());
    }
  }

  public static class TestMinimal extends AbstractTestReifiedStatements {

    public static final ReificationStyle style = ModelFactory.Minimal;

    public TestMinimal(String name) {
      super(name);
    }

    public void setUp() {
      try {
        ReifiedStatementsUnitTest.setUpDatabase();
        super.setUp();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public void tearDown() {
      try {
        super.tearDown();
        ReifiedStatementsUnitTest.tearDownDatabase();
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }

    public Model getModel() {
      return modelMakerMinimal.createModel();
    }

    public void testStyle() {
      assertEquals(style, getModel().getReificationStyle());
    }
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
  public static void setUpDatabase() throws Exception {

    boolean exceptionOccurred = true;

    try {     
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
          serverURI, false);
      LocalJenaSession session = (LocalJenaSession) sessionFactory.
          newJenaSession();

      graphMakerMinimal = new GraphMulgaraMaker(session, serverURI,
          ReificationStyle.Minimal);
      graphMakerStandard = new GraphMulgaraMaker(session, serverURI,
          ReificationStyle.Standard);
      graphMakerConvenient = new GraphMulgaraMaker(session, serverURI,
          ReificationStyle.Convenient);

      modelMakerMinimal = new ModelMulgaraMaker(graphMakerMinimal);
      modelMakerStandard = new ModelMulgaraMaker(graphMakerStandard);
      modelMakerConvenient = new ModelMulgaraMaker(graphMakerConvenient);

      exceptionOccurred = false;
    } catch ( Exception ex ) {
      logger.error("Error in database setup", ex);
    }
    finally {
      if (exceptionOccurred) {
        tearDownDatabase();
      }
    }   
  }

  /**
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public static void tearDownDatabase() throws Exception {
    graphMakerMinimal.removeAll();
    graphMakerStandard.removeAll();
    graphMakerConvenient.removeAll();
    session.close();   
  }
}
