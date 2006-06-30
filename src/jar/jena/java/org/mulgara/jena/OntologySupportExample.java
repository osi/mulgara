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

// Standard APIs
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;

// Jena APIs
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.ontology.*;
import com.hp.hpl.jena.shared.*;

import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

// Internal Kowari APIs
import com.hp.hpl.jena.util.iterator.*;
//import org.kowari.client.jena.*;
import org.kowari.server.driver.SessionFactoryFinder;
import org.kowari.server.SessionFactory;

/**
 * Test case for {@link GraphMulgaraMaker}.
 *
 * @created 2003-02-27
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
public class OntologySupportExample {

  /**
   * The name of the test model.
   */
  protected final static String SERVER_NAME = "server1";

  /**
   * The URI of the server.
   */
  protected URI serverURI;

  /**
   * The URI of the graph.
   */
  protected static URI graphURITest;

  /**
   * The graphMaker used in the tests.
   */
  private GraphMulgaraMaker graphMaker;

  /**
   * The database session being used for the tests.
   */
  private LocalJenaSession session;

  public OntologySupportExample() {

    Logger.getRootLogger().setLevel(Level.ERROR);
//    DOMConfigurator.configure(System.getProperty("log4j.configuration"));

    try {

      long startTime = System.currentTimeMillis();

//      ModelMaker maker = createMaker(ReificationStyle.Minimal);

      // OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM_RULE_INF);
      OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM);
      //spec.setModelMaker(maker);

//      Model baseModel = AbstractJenaFactory.newModel("rmi://localhost/server1#camera");
      Model baseModel = null;
      OntModel m = ModelFactory.createOntologyModel(spec, baseModel);

      System.err.println("Ont model: " + m.getClass());
      System.err.println("Ont model: " + baseModel);

      File dataFile = new File(System.getProperty("cvs.root") +
          "/data/camera.owl");
      InputStream in = new FileInputStream(dataFile);
      m.read(in, null);

      // Print out time taken to load.
      System.err.println("Time taken to load: " +
          (System.currentTimeMillis() - startTime) + "ms");
      startTime = System.currentTimeMillis();

      String camNS = "http://www.xfront.com/owl/ontologies/camera/#";
      Resource r = m.getResource(camNS + "Camera");
      OntClass camera = m.getOntClass(camNS + "Camera");

      // List all cameras.
      for (ExtendedIterator i = m.listNamedClasses(); i.hasNext(); ) {
//      for (ExtendedIterator i = m.listClasses(); i.hasNext(); ) {

        OntClass c = (OntClass) i.next();
        System.err.println("Class: " + c.getLocalName());
      }

      // Print out time taken to do query.
      System.err.println("Time taken to query: " +
          (System.currentTimeMillis() - startTime) + "ms");

      m.close();

      // Get all of the data from the camera.
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      URI cameraURI = new URI("rmi", hostname, "/" + SERVER_NAME, "camera");
      GraphMulgara graph = new GraphMulgara(session, cameraURI);

      // Get number of triples
      System.err.println("Total number of triples: " + graph.size());

      // Find all triples.
      ExtendedIterator iter = graph.find(null, null, null);
      while (iter.hasNext()) {

        Triple triple = (Triple) iter.next();
        System.err.println("Has triple: " + triple);
      }
      iter.close();

      tearDown();
    }
    catch (Exception e) {

      e.printStackTrace();
    }
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) throws Exception {

    new OntologySupportExample();
  }

  /**
   * Setups up the test for JUnit - creates the database and graph ready for
   * use.
   *
   * @throws Exception if there was an error creating the database - always
   *     fatal.
   */
  public ModelMaker createMaker(ReificationStyle reificationStyle) {

    boolean exceptionOccurred = true;

    try {

      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverURI, false);
      this.session = (LocalJenaSession) sessionFactory.newJenaSession();

      graphMaker = new GraphMulgaraMaker((LocalJenaSession)
          session, serverURI, ReificationStyle.Minimal);
      ModelMaker modelMaker = new ModelMulgaraMaker(graphMaker);

      exceptionOccurred = false;

      return modelMaker;
    }
    catch (Exception e) {

      e.printStackTrace();
      return null;
    }
    finally {
      if (exceptionOccurred)
        tearDown();
    }
  }

  /**
   * Remove any existing graphs.
   */
  public void tearDown() {

    if (graphMaker != null) {

      graphMaker.removeAll();
    }
  }

  /**
   * Remove the contents of the given directory.
   *
   * @param dir the file handle to the directory to remove.
   */
  private void removeContents(File dir) {
    File[] files = dir.listFiles();
    if (files != null)
      for (int i = 0; i < files.length; ++i)
        if (files[i].isFile()) files[i].delete();
  }
}
