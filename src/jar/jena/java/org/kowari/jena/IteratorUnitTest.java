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
import com.hp.hpl.jena.shared.*;

import junit.framework.*;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;

import org.apache.log4j.*;

import org.kowari.server.*;
import org.kowari.server.driver.*;

/**
 * Test case for {@link StmtIterator}s.
 *
 * @created 2003-12-01
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
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class IteratorUnitTest extends TestCase {

  /**
   * Default name to use for the Jena model.
   */
  protected final static String SERVER_NAME = "jenamodeltest";

  /**
   * Init the logging class
   */
  private static Logger logger =
      Logger.getLogger(IteratorUnitTest.class.getName());

  /**
   * The session to the database.
   */
  protected Session session = null;

  /**
   * The Jena model used in testing.
   */
  protected Model model = null;

  /**
   * Creates new graphs.
   */
  protected GraphKowariMaker graphMaker;

  /**
   * Creates new models.
   */
  protected ModelMaker modelMaker = null;

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
  public IteratorUnitTest(String name) throws Exception {

    super(name);
    System.setProperty("org.kowari.xml.ResourceDocumentBuilderFactory",
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
    modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, "test");
  }

  public static TestSuite suite() {
    return new TestSuite(IteratorUnitTest.class);
  }

  public Model getModel() {
    return model;
  }

  public void testIterators() {
    Model m = getModel();
    Resource S = m.createResource("local:S");
    Property P = m.createProperty("local:P");
    RDFNode O = m.createResource("local:O");
    m.add(S, P, O);
    StmtIterator it = m.listStatements();
    Statement st = (Statement) it.next();
    assertEquals(m.createStatement(S, P, O), st);
    m.remove(m.createStatement(S, P, O));
    assertEquals("", 0, m.size());
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
        model.close();
      }
      finally {
        model = null;
        graphMaker.removeAll();
        session = null;
      }
    }
  }
}
