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

import java.io.*;
import java.net.InetAddress;
import java.net.*;
import java.util.*;

import com.hp.hpl.jena.ontology.impl.test.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.graph.test.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.rdf.model.test.*;

import junit.framework.*;

import org.apache.log4j.*;

import org.kowari.server.Session;
import org.kowari.query.QueryException;
import org.kowari.server.driver.SessionFactoryFinder;
import org.kowari.server.SessionFactory;
import org.kowari.server.*;

/**
 * Test case for {@link TestOntModel}.
 *
 * @created 2004-07-12
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/07 09:37:07 $
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
public class OntModelUnitTest extends TestOntModel {

  /**
   * Default name to use for the Jena model.
   */
  protected final static String SERVER_NAME = "jenamodeltest";

  /**
   * Init the logging class
   */
  private static Logger logger =
      Logger.getLogger(OntModelUnitTest.class.getName());

  /**
   * The session to the database.
   */
  protected Session session = null;

  /**
   * Ontology specification.
   */
  protected OntModelSpec spec = null;

  /**
   * Creates new graph.
   */
  protected GraphMulgaraMaker graphMaker = null;

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
  public OntModelUnitTest(String name) throws Exception {

    super(name);
    System.setProperty("org.kowari.xml.ResourceDocumentBuilderFactory",
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

    String hostname = InetAddress.getLocalHost().getCanonicalHostName();
    serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
    modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, "test");
  }

  public static TestSuite suite() throws Exception {
//    return new TestSuite(OntModelUnitTest.class);
    TestSuite result = new TestSuite();
//    result.addTest(new OntModelUnitTest("testGetAllValuesFromRestriction"));
    result.addTest(new OntModelUnitTest("testGetImportedModel"));
    result.addTest(new OntModelUnitTest("testGetSubgraphs"));
    result.addTest(new OntModelUnitTest("testListImportedModels"));
    result.addTest(new OntModelUnitTest("testListImportURIs"));
    return result;
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) throws Exception {
    junit.textui.TestRunner.run(suite());
  }

  public void testListImportURIs() {
    try {
      Model baseModel = modelMaker.createModel("foo");
      OntModel m = ModelFactory.createOntologyModel(spec, baseModel);
      m.read(new File(System.getProperty("cvs.root")).toURL().toString() + "/jxtest/ontology/a.owl");
      Collection c = m.listImportedOntologyURIs();

      assertEquals("Should be two non-closed import URI's", 2, c.size());
      assertTrue("b should be imported ",
          c.contains("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/b.owl"));
      assertFalse("c should not be imported ",
          c.contains("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/c.owl"));
      assertTrue("d should be imported ",
          c.contains("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/d.owl"));

      c = m.listImportedOntologyURIs(true);

      assertEquals("Should be two non-closed import URI's", 3, c.size());
      assertTrue("b should be imported ",
          c.contains("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/b.owl"));
      assertTrue("c should be imported ",
          c.contains("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/c.owl"));
      assertTrue("d should be imported ",
          c.contains("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/d.owl"));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the cvs.root property taht can be used as an URI.
   *
   * @return String
   */
  private String getCvsRoot() {

    String cvsRoot = System.getProperty("cvs.root");

    //replace back slashes with forward slashes
    cvsRoot = cvsRoot.replaceAll("\\\\", "/");

    return cvsRoot;
  }

  public void testListImportedModels() {
    try {
      logger.info("Starting list imported models");
      Model baseModel = modelMaker.createModel();
      OntModel m = ModelFactory.createOntologyModel(spec, baseModel);
      m.read(new File(System.getProperty("cvs.root") + "/jxtest/ontology/a.owl").toURI().toString());
      assertEquals("Marker count not correct", 4,
          TestOntDocumentManager.countMarkers(m));

      List importModels = new ArrayList();
      for (Iterator j = m.listImportedModels(); j.hasNext();
           importModels.add(j.next()));

      assertEquals("n import models should be ", 3, importModels.size());

      boolean isOntModel = true;
      int nImports = 0;

      for (Iterator i = importModels.iterator(); i.hasNext(); ) {
        Object x = i.next();
        if (!(x instanceof OntModel)) {
          isOntModel = false;
        }
        else {
          // count the number of imports of each sub-model
          OntModel mi = (OntModel) x;
          nImports += mi.listImportedOntologyURIs().size();
        }
      }

      assertTrue("All import models should be OntModels", isOntModel);
      assertEquals("Wrong number of sub-model imports", 2, nImports);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void testGetAllValuesFromRestriction() {
    try {
      Model baseModel = modelMaker.createModel();
      OntModel m = ModelFactory.createOntologyModel(spec, modelMaker, baseModel);
      Property p = m.createProperty(NS + "p");
      OntClass c = m.createClass(NS + "c");
      Resource r = m.getResource(NS + "r");
      m.add(r, RDF.type, r);
      Resource s = m.createAllValuesFromRestriction(NS + "s", p, c);
      assertEquals("Result of get s", s, m.getAllValuesFromRestriction(NS + "s"));
      assertNull("result of get q", m.getAllValuesFromRestriction(NS + "q"));
      assertNull("result of get r", m.getAllValuesFromRestriction(NS + "r"));
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void testGetImportedModel() {
    try {
      Model baseModel = modelMaker.createModel();
      OntModel m = ModelFactory.createOntologyModel(spec, modelMaker, baseModel);
      m.read(new File(System.getProperty("cvs.root") + "/jxtest/ontology/a.owl").toURI().toString());

      OntModel m0 = m.getImportedModel("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/b.owl");
      OntModel m1 = m.getImportedModel("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/c.owl");
      OntModel m2 = m.getImportedModel("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/d.owl");
      OntModel m3 = m.getImportedModel("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/b.owl");
      m3.union((m3.getImportedModel("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/c.owl")));
      OntModel m4 = m.getImportedModel("file:///" + this.getCvsRoot() +
          "/jxtest/ontology/a.owl");

      assertNotNull("Import model b should not be null", m0);
      assertNotNull("Import model c should not be null", m1);
      assertNotNull("Import model d should not be null", m2);
      assertNotNull("Import model b-c should not be null", m3);
      assertNull("Import model a should be null", m4);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void testGetSubgraphs() {
    try {
      Model baseModel = modelMaker.createModel();
      OntModel m = ModelFactory.createOntologyModel(spec, modelMaker, baseModel);
      m.read(new File(System.getProperty("cvs.root") + "/jxtest/ontology/a.owl").toURI().toString());
      assertEquals("Marker count not correct", 4,
          TestOntDocumentManager.countMarkers(m));

      List subs = m.getSubGraphs();

      assertEquals("n subgraphs should be ", 3, subs.size());

      boolean isGraph = true;
      for (Iterator i = subs.iterator(); i.hasNext(); ) {
        Object x = i.next();
        if (!(x instanceof Graph)) {
          isGraph = false;
        }
      }
      assertTrue("All sub-graphs should be graphs", isGraph);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
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

      graphMaker = new GraphMulgaraMaker((LocalJenaSession)
          session, serverURI, ReificationStyle.Minimal);
      modelMaker = new ModelMulgaraMaker(graphMaker);

      spec = new OntModelSpec(OntModelSpec.OWL_MEM);
      spec.setModelMaker(modelMaker);

      // Clear the cache between uses to prevent dealing with closed
      // graphs/models etc.
      spec.getDocumentManager().clearCache();

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

    spec = null;

    try {
      if (session != null) {
        session.close();
      }
    }
    catch (QueryException qe) {
    }
    finally {
      session = null;
    }

    if (graphMaker != null) {
      graphMaker.close();
      graphMaker = null;
    }

    if (modelMaker != null) {
      modelMaker.close();
      modelMaker = null;
    }
  }

  private void removeContents(File dir) {
    File[] files = dir.listFiles();
    if (files != null)
      for (int i = 0; i < files.length; ++i)
        if (files[i].isFile()) files[i].delete();
  }
}
