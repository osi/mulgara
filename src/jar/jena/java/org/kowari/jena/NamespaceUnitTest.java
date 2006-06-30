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
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.graph.test.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.rdf.model.test.*;
import com.hp.hpl.jena.shared.test.*;
import com.hp.hpl.jena.graph.*;

import junit.framework.*;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;

import org.apache.log4j.*;

import org.kowari.server.Session;
import org.kowari.server.driver.SessionFactoryFinder;
import org.kowari.server.SessionFactory;
import org.kowari.query.QueryException;
import org.kowari.util.TempDir;

/**
 * Test case for {@link TestNamespace}.
 *
 * @created 2004-07-14
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
public class NamespaceUnitTest extends ModelTestBase {

  /**
   * Default name to use for the Jena model.
   */
  protected final static String SERVER_NAME = "jenamodeltest";

  /**
   * Init the logging class
   */
  private static Logger logger =
      Logger.getLogger(NamespaceUnitTest.class.getName());

  /**
   * The session to the database.
   */
  protected Session session = null;

  /**
   * The Jena model used in testing.
   */
  protected Model model = null;

  /**
   * Creates new graph.
   */
  protected static GraphKowariMaker graphMaker = null;

  /**
   * Creates new models.
   */
  protected static ModelKowariMaker modelMaker = null;

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
  public NamespaceUnitTest(String name) throws Exception {

    super(name);
    System.setProperty("org.kowari.xml.ResourceDocumentBuilderFactory",
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
    modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, "test");
  }

  public static TestSuite suite() throws Exception {
    return new TestSuite(NamespaceUnitTest.class);
//    TestSuite result = new TestSuite();
//    result.addTest(new NamespaceUnitTest("testReadPrefixes"));
//    result.addTest(new NamespaceUnitTest("testUseEasyPrefix"));
//    result.addTest(new NamespaceUnitTest("testWritePrefixes"));
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

  public void testReadPrefixes() {
    try {
      Model m = getModel();
      URI fileURI = new URI(new File(System.getProperty("cvs.root") +
          "/data/test0014.rdf").toURL().toString());
      m.read(fileURI.toString());
      Map ns = m.getNsPrefixMap();
      // System.err.println( ">> " + ns );
      assertEquals("namespace eg", "http://example.org/", ns.get("eg"));
      assertEquals("namespace rdf",
          "http://www.w3.org/1999/02/22-rdf-syntax-ns#",
          ns.get("rdf"));
      assertEquals("not present", null, ns.get("spoo"));
    }
    catch (Exception e) {

    }
  }

  public void testWritePrefixes() throws Exception {
    Model m = modelMaker.createModel();
    ModelCom.addNamespaces(m, makePrefixes("fred=ftp://net.fred.org/;spoo=http://spoo.net/"));
    File f = TempDir.createTempFile("hedgehog", ".rdf");
    m.add(statement(m, "http://spoo.net/S http://spoo.net/P http://spoo.net/O"));
    m.add(statement(m,
        "http://spoo.net/S ftp://net.fred.org/P http://spoo.net/O"));
    m.write(new FileWriter(f));
    /* */
    Model m2 = modelMaker.createModel();
    m2.read(new URI(f.toURL().toString()).toString());
    Map ns = m2.getNsPrefixMap();
    assertEquals("namespace spoo", "http://spoo.net/", ns.get("spoo"));
    assertEquals("namespace fred", "ftp://net.fred.org/", ns.get("fred"));
    /* */
    f.deleteOnExit();
  }

  public void testUseEasyPrefix() {
    TestPrefixMapping.testUseEasyPrefix
        ("default model", modelMaker.createModel());
  }

  /**
      turn a semi-separated set of P=U definitions into a namespace map.
   */
  private Map makePrefixes(String prefixes) {
    Map result = new HashMap();
    StringTokenizer st = new StringTokenizer(prefixes, ";");
    while (st.hasMoreTokens()) {
      String def = st.nextToken();
      // System.err.println( "| def is " + def );
      int eq = def.indexOf('=');
      result.put(def.substring(0, eq), set(def.substring(eq + 1)));
    }
    // result.put( "spoo", set( "http://spoo.net/" ) );
    return result;
  }

  private Set set(String element) {
    Set s = new HashSet();
    s.add(element);
    return s;
  }

  /**
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

  private void removeContents(File dir) {
    File[] files = dir.listFiles();
    if (files != null)
      for (int i = 0; i < files.length; ++i)
        if (files[i].isFile()) files[i].delete();
  }
}
