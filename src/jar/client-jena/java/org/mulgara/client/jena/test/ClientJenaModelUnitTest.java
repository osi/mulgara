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

import java.io.*;

import org.jrdf.graph.*;
import org.jrdf.vocabulary.*;

// Third party packages
import junit.framework.*;
import java.net.*;
import java.util.*;
import org.mulgara.server.*;
import org.mulgara.server.driver.*;
import org.mulgara.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import org.mulgara.query.ModelResource;
import org.mulgara.client.jena.AbstractJenaFactory;

import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;

/**
 * Unit test for client-side Jena Model representing a mulgara model (modelURI)
 * and uses an ItqlInterpreterBeean and a Session.
 *
 * @created 2004-08-24
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/27 20:12:02 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ClientJenaModelUnitTest extends TestCase {

  /** name used for the server */
  private static String SERVER_NAME = "server1";

  /** name of the model */
  private static String MODEL_NAME = "clientJenaModel";

  /** Test data file to be loaded into model */
  private static final String TEST_RDF_FILE = System.getProperty("cvs.root") +
      "/data/camera.owl";

  /** URI for the mulgara server */
  private static URI serverURI = null;

  /** URI for the test model */
  private static URI modelURI = null;

  /** The session used by the graph (and setting up) */
  private Session session = null;

  /**
   * Constructs a new test with the given name.
   *
   * @param name the name of the test
   */
  public ClientJenaModelUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   *
   * @return The test suite
   */
  public static Test suite() {

    TestSuite suite = new TestSuite();
    suite.addTest(new ClientJenaModelUnitTest("testKModelFind"));
    suite.addTest(new ClientJenaModelUnitTest("testCreateKModels"));
    suite.addTest(new ClientJenaModelUnitTest("testCreateModels"));
    suite.addTest(new ClientJenaModelUnitTest("testCloseKModelManyTimes"));
    //this causes problems inside Jena's Ontology Models
    //suite.addTest(new ClientJenaModelUnitTest("testOntModel"));
    return suite;
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    junit.textui.TestRunner.run(suite());
  }

  /**
   * Runs Jena tests on the Model.
   *
   * @throws Exception
   */
  public void testKModelFind() throws Exception {
    try {
      //Create Client Side  Jena Model
      Model model = AbstractJenaFactory.newKModel(serverURI, modelURI);
      //List All Subjects
      ResIterator resIterator = model.listSubjects();
      while (resIterator.hasNext()) {
        Resource resource = (Resource) resIterator.next();
      }
      resIterator.close();
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Tests the creation (and closure) of multiple Jena models.
   *
   * @throws Exception
   */
  public void testCreateKModels() throws Exception {
    try {
      for (int i = 0; i < 10; i++) {
        //Create Client Side Jena Model and close
        Model model = AbstractJenaFactory.newKModel(serverURI, modelURI);
        model.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Tests the creation (and closure) of multiple Jena models.
   *
   * @throws Exception
   */
  public void testCreateModels() throws Exception {
    try {
      for (int i = 0; i < 10; i++) {
        //Create Client Side Jena Model and close
        Model model = AbstractJenaFactory.newModel(serverURI, modelURI);
        model.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Tests the creation (and closure) of multiple Jena models.
   *
   * @throws Exception
   */
  public void testCloseKModelManyTimes() throws Exception {
    try {
      //Create Client Side Jena Model and close multiple times
      Model model = AbstractJenaFactory.newKModel(serverURI, modelURI);
      for (int i = 0; i < 10; i++) {
        model.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Tests the creation (and closure) of an OntModel.
   *
   * @throws Exception
   */
  public void testOntModel() throws Exception {
    try {
      for (int i = 0; i < 2; i++) {
        //Create Client Side Jena Ontology Model and close
        Model model = AbstractJenaFactory.newModel(serverURI, modelURI);
        OntModel dbOntModel = ModelFactory.createOntologyModel(OntModelSpec.
            OWL_MEM, model);
        dbOntModel.close();
        model.close();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw e;
    }
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

      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      this.serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      this.modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, MODEL_NAME);

      //get session
      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
          this.serverURI, true);
      this.session = sessionFactory.newSession();

      //initialize model
      this.createModel(this.modelURI);
      this.populateModel(this.modelURI);

      //let superclass set up too
      super.setUp();
    }
    catch (Exception exception) {

      exception.printStackTrace();

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

    this.dropModel(this.modelURI);

    //allow super to close down too
    super.tearDown();
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @param modelURI URI
   * @throws Exception
   */
  private void createModel(URI modelURI) throws Exception {

    this.session.createModel(modelURI, new URI("http://mulgara.org/mulgara#Model"));
  }

  /**
   * Loads Test data into the model
   *
   * @param modelURI URI
   * @throws Exception
   */
  private void populateModel(URI modelURI) throws Exception {

    File file = new File(TEST_RDF_FILE);
    URI fileURI = file.toURI();
    this.session.setModel(modelURI, new ModelResource(fileURI));
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @param modelURI URI
   * @throws Exception
   */
  private void dropModel(URI modelURI) throws Exception {

    this.session.removeModel(modelURI);
  }
}
