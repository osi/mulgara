package org.mulgara.sofa.example;

import java.util.*;
import java.util.Collection;
import java.net.*;
import java.io.*;

import net.java.dev.sofa.*;
import net.java.dev.sofa.impl.*;
import net.java.dev.sofa.model.*;
import org.jrdf.graph.*;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.SessionFactoryFactory;
import org.mulgara.server.driver.JRDFGraphFactory;
import org.mulgara.server.driver.SessionFactoryFinder;
import org.mulgara.sofa.*;
import org.mulgara.sofa.serialize.owl.*;

public class CameraPhoneOntologyApp {

  public static void main(String[] args) {

    //  Create a new Camera Ontology Application
    CameraPhoneOntologyApp app = new CameraPhoneOntologyApp();

    //  SOFA Ontology object
    Ontology ontology = null;

    try {

      System.out.println("Creating empty Ontology");
      String ontologyURI = "http://www.xfront.com/owl/ontologies/camera/";

      // Create in memory based Ontology
      ontology = OntoConnector.getInstance().createOntology(ontologyURI);

      //    uncomment this to..
      // Create Ontology on the client (communicates with Mulgara server)
      //ontology = OntoConnector.getInstance().createOntology(
      //    app.createClientOntologyModel(), ontologyURI);

      // uncomment this to..
      // Create Ontology on the server (same JVM)
      //ontology = OntoConnector.getInstance().createOntology(
      //   app.createServerOntologyModel(), ontologyURI);

      // Populate the ontology data
      app.loadCameraOntology(ontology);

    } catch (Exception exception) {

      System.out
          .println("Failed to create the ontolgy due to the following exception:");
      exception.printStackTrace();
    }

    try {

      // Create the ontology data
      app.populateOntology(ontology);
    } catch (Exception exception) {

      System.out
          .println("Failed to populate the ontolgy due to the following exception:");
      exception.printStackTrace();
    }
  }

  /**
   * Loads the example Camera ontology into the supplied ontology object.
   *
   * @param onto Ontology
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void loadCameraOntology(Ontology onto) throws Exception {

    System.out.println("Loading Camera Ontology");
    OWLReader.getReader().read(onto, "file:Resources/kamera.owl");

    System.out.println("Loaded Ontology");

    // get the Digital Camera Thing, a Concept is like an OWL Class,
    Concept digiCam = onto.getConcept("Digital");

    // Create a new type of camera: camera phone

    // create phone first
    Concept phone = onto.createConcept("Phone");
    // give it a property/relation of GSM or CDMA
    Relation standard = onto.createRelation("standard");
    Set<String> standards = new HashSet<String>();
    standards.add("GSM");
    standards.add("CDMA");
    phone.setRestrictionOn(standard, standards, 1, 2); // 1=minCard, 2=maxCard

    // make phone a sub class of purchaseable item
    Concept purchaseableItem = onto.getConcept("PurchaseableItem");
    phone.addSuperConcept(purchaseableItem);

    // create camera phone
    Concept cameraPhone = onto.createConcept("CameraPhone");
    cameraPhone.addSuperConcept(phone);
    cameraPhone.addSuperConcept(digiCam);

    // Show super classes
    System.out.println("SUPER CLASSES");
    Collection<Concept> superConcepts = cameraPhone.getSuperConcepts(true);

    // show number of superclasses
    System.out.println("Number of superConcepts found: " + superConcepts.size());

    // test a phone is our superclass
    System.out.println("Found phone concept in list of super concepts: "
        + superConcepts.contains(phone));

    for (Concept sc: superConcepts) {
      System.out.println(sc.getId());
    }

    // show properties, including super properties 'true'
    System.out.println("PROPERTIES");

    for (Iterator<Relation> ri = cameraPhone.definedRelations(true); ri.hasNext();) {
      System.out.println(ri.next().getId());
    }

    // test camera phones have 'standard'
    System.out.println("CameraPhone contains the 'standard' relation: "
        + cameraPhone.hasDefinedRelation(standard, true));

    // Write new ontology to Standard out
    OWLWriter.getWriter().write(onto, System.out);
  }

  /**
   * Populates the supplies ontology with Camera & Phone related instances.
   *
   * @param ontology Ontology
   * @throws Exception
   */
  @SuppressWarnings("unchecked")
  public void populateOntology(Ontology ontology) throws Exception {

    // Retrieve the CameraPhone concept
    Concept cameraPhone = ontology.getConcept("CameraPhone");

    // Create a CameraPhone instance called 'Nokia'
    Thing mobile = cameraPhone.createInstance("Nokia");

    // Retrieve the Digital concept
    Concept digital = ontology.getConcept("Digital");

    // Create an instance of the digital camera called 'Olympus'
    Thing camera = digital.createInstance("Olympus");

    // Retrieve the 'standard' relation
    Relation standardRelation = ontology.getRelation("standard");

    // Retrieve the 'lens' relation
    Relation lensRelation = ontology.getRelation("lens");

    // Set the lens and standard type for the mobile phone
    mobile.add(standardRelation, "CDMA");
    mobile.add(lensRelation, "CompanyX");

    // Set the lens for the camera
    camera.add(lensRelation, "Carl Zeiss");

    System.out.println("Listing standards for mobile phone:");

    // Iterate through the standards of the phone
    for (Object obj: mobile.list(standardRelation)) {
      // Print the next standard
      System.out.println(obj);
    }

    System.out.println("Listing lenses for mobile phone:");

    // Iterate through the lenses of the phone
    for (Object obj: mobile.list(lensRelation)) {
      // Print the next lens
      System.out.println(obj);
    }

    System.out.println("Listing lenses for camera:");

    // Iterate through the lenses of the camera
    for (Object obj: camera.list(lensRelation)) {
      // Print the next lens
      System.out.println(obj);
    }

    System.out.println("All Things:");

    // Iterate through all 'Things' in the ontology framework
    for (Iterator<Thing> thing = ontology.things(); thing.hasNext();) {
      System.out.println("\t" + (thing.next()).getId());
    }
  }

  /**
   * Creates a SOFA Ontology Model backed by a mulgara model (in same JVM as
   * Server)
   *
   * @return @throws Exception
   * @throws Exception
   */
  OntologyModel createServerOntologyModel() throws Exception {

    return new OntologyJRDFModel(createServerGraph(createMulgaraDatabase(),
        getGraphURI()));
  }

  /**
   * Creates a SOFA Ontology Model backed by a mulgara model, the mulgara model is
   * running in a different JVM and accessed via RMI.
   *
   * @return @throws Exception
   * @throws Exception
   */
  OntologyModel createClientOntologyModel() throws Exception {

    return new OntologyJRDFModel(createClientGraph(getGraphURI()));
  }

  /**
   * Returns a Graph object residing on the server, note any existing graph
   * models on the server will be dropped.
   *
   * @param database SessionFactory
   * @param graph URI
   * @return @throws Exception
   * @throws Exception
   */
  Graph createServerGraph(SessionFactory database, URI graph) throws Exception {

    return JRDFGraphFactory.newServerGraph(database, graph);
  }

  /**
   * Returns a local graph backed by a graph on the server. note any existing
   * graph models on the server will be dropped.
   *
   * @param graph URI
   * @return @throws Exception
   * @throws Exception
   */
  Graph createClientGraph(URI graph) throws Exception {

    //any existing model/graph must be cleared
    SessionFactory factory = SessionFactoryFinder.newSessionFactory(getServerURI(), true);
    Session session = factory.newSession();
    if (session.modelExists(graph)) {
      session.removeModel(graph);
    }
    session.createModel(graph, URI.create(Mulgara.NAMESPACE + "Model"));
    return JRDFGraphFactory.newClientGraph(getServerURI(), graph);
  }

  /**
   * Creates a Mulgara database.
   *
   * @return @throws Exception
   * @throws Exception
   */
  SessionFactory createMulgaraDatabase() throws Exception {

    boolean exceptionOccurred = true;

    SessionFactory database = null;

    try {

      database = new SessionFactoryFactory("org.mulgara.resolver.Database").newSessionFactory(getServerURI(),
          getServerDir());
      exceptionOccurred = false;
    } finally {

      if (exceptionOccurred) {

        // Close database
        if (database != null) {
          database.close();
        }

      }
    }
    return database;
  }

//  /**
//   * Creates a Mulgara database.
//   *
//   * @return @throws
//   *         Exception
//   */
//  XADatabaseImpl createMulgaraDatabase() throws Exception {
//
//    boolean exceptionOccurred = true;
//
//    XADatabaseImpl database = null;
//
//    try {
//
//      File serverDir = getServerDir();
//      serverDir.mkdirs();
//      removeContents(serverDir);
//      database = new XADatabaseImpl(getServerURI(), serverDir);
//      exceptionOccurred = false;
//    } finally {
//
//      if (exceptionOccurred) {
//
//        // Close database
//        if (database != null) {
//          database.close();
//        }
//
//      }
//    }
//    return database;
//  }


  /**
   * @return the directory the server is running from.
   */
  private File getServerDir() {
    File dir = new File(System.getProperty("java.io.tmpdir"), System
        .getProperty("user.name"));
    File serverDir = new File(dir, "server1");
    return serverDir;
  }

  /**
   *
   * @return java.net.URI
   * @throws Exception
   */
  URI getServerURI() throws Exception {
    URI serverURI = new URI("rmi", getHostname(), "/server1", null);
    return serverURI;
  }

  /**
   * Returns the hostname of this machine.
   *
   * @return @throws Exception
   * @throws Exception
   */
  String getHostname() throws Exception {
    return InetAddress.getLocalHost().getCanonicalHostName();
  }

  /**
   * Returns the GraphURI for based on this server
   *
   * @return @throws Exception
   * @throws Exception
   */
  URI getGraphURI() throws Exception {
    return new URI("rmi", getHostname(), "/server1", "test");
  }

  /**
   * Removes files in a dir, used in server shutdown cleanup.
   *
   * @param dir File
   */
  void removeContents(File dir) {

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
