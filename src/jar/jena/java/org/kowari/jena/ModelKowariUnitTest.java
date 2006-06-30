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

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.graph.test.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.rdf.model.test.*;

import junit.framework.*;

import java.net.InetAddress;
import java.net.URI;

import org.apache.log4j.*;

import org.kowari.server.*;
import org.kowari.server.driver.*;
import org.kowari.query.QueryException;

/**
 * Test case for {@link ModelKowari}.
 *
 * @created 2003-12-01
 *
 * @author <a href="http://staff.pisoftware.com/david">David Makepeace</a>
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
public class ModelKowariUnitTest extends AbstractTestModel {

  /**
   * Default name to use for the Jena model.
   */
  protected final static String SERVER_NAME = "jenamodeltest";

  /**
   * Init the logging class
   */
  private static Logger logger =
      Logger.getLogger(ModelKowariUnitTest.class.getName());

  /**
   * The session to the database.
   */
  protected Session session = null;

  /**
   * The graph maker
   */
  protected GraphKowariMaker graphMaker = null;

  /**
   * The Jena model maker used to create model.
   */
  protected ModelMaker modelMaker;

  /**
   * The Jena model used in testing.
   */
  protected Model model = null;

  /**
   * The server URI.
   */
  protected URI serverURI;

  /**
   * The model URI.
   */
  protected URI modelURI;

  /**
   * CONSTRUCTOR JenaModelAbstractTest TO DO
   *
   * @param name PARAMETER TO DO
   * @throws Exception EXCEPTION TO DO
   */
  public ModelKowariUnitTest(String name) throws Exception {

    super(name);

    System.setProperty("org.kowari.xml.ResourceDocumentBuilderFactory",
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
    modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, "test");
  }

  public static TestSuite suite() throws Exception {

    return new TestSuite(ModelKowariUnitTest.class);
//    TestSuite result = new TestSuite();
//    result.addTest(new ModelKowariUnitTest("testContainsResource"));
//    result.addTest(new ModelKowariUnitTest("testCreateBlankFromNode"));
//    result.addTest(new ModelKowariUnitTest("testCreateLiteralFromNode"));
//    result.addTest(new ModelKowariUnitTest("testCreateResourceFromNode"));
//    result.addTest(new ModelKowariUnitTest("testGetProperty"));
//    result.addTest(new ModelKowariUnitTest("testTransactions"));
//    result.addTest(new ModelKowariUnitTest("testIsEmpty"));
//    return result;
  }

  public Model getModel() {
    return model;
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
   * The JUnit setup method
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void setUp() {

    boolean exceptionOccurred = true;
    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverURI, false);
      this.session = (LocalJenaSession) sessionFactory.newJenaSession();

      graphMaker = new GraphKowariMaker((LocalJenaSession)
          session, serverURI, ReificationStyle.Minimal);
      modelMaker = new ModelKowariMaker(graphMaker);
      model = modelMaker.createModel();

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
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void tearDown() {
    if (model != null) {
      try {
        graphMaker.removeAll();
        model.close();
      }
      finally {
        model = null;

        if (session != null) {
          try {
            session.close();
          }
          catch (QueryException qe) {

          }
          finally {
            session = null;
          }
        }
      }
    }
  }
}
