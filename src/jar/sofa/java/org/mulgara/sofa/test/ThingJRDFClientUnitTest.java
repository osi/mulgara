package org.mulgara.sofa.test;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import net.java.dev.sofa.impl.OntoConnector;
import net.java.dev.sofa.model.OntologyModel;

import org.jrdf.graph.*;


import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.driver.JRDFGraphFactory;
import org.mulgara.server.driver.SessionFactoryFinder;
import org.mulgara.sofa.OntologyJRDFModel;

/**
 * TODO ONE LINE DESC <p>
 *
 * TODO MORE DETAILED DESC
 * </p>
 *
 * @created Sep 2, 2004
 *
 * @author Keith Ahern
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:04 $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 */
/**
 * @author keith
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class ThingJRDFClientUnitTest extends ThingJRDFMemoryImplUnitTest {

 /**
  * Description of the Field
  */
 protected final static String SERVER_NAME = "server1";


 /**
  * Description of the Field
  */
 protected URI serverURI;

 /**
  * Description of the Field
  */
 protected String hostname;

 /**
  * The current instance of a database graph.
  */
 protected Graph graph;

 public static URI ONTO_NAMESPACE1 = null;
 public static URI ONTO_NAMESPACE2 = null;

 static {
     try {
         ONTO_NAMESPACE1 = new URI("http://sofa.org/test/namespace1");
         ONTO_NAMESPACE2 = new URI("http://sofa.org/test/namespace2");
     }
     catch (URISyntaxException e) {
         e.printStackTrace();
     }
 }

 /**
  * The URI of the test graph to create.
  */
 protected URI graphURI;

 /**
  * The test database to execute queries against. Subclasses must initialize
  * this field.
  */
 protected SessionFactory database = null;

 /**
  * Description of the Field
  */
 protected Session session = null;


 /*
  * @see TestCase#setUp()
  */
 protected void setUp() throws Exception {
   initGraph();
   OntologyModel ontoModel = new OntologyJRDFModel(graph);
   onto = OntoConnector.getInstance().createOntology(ontoModel, OntologyMemoryImplUnitTest.ONTO_NAMESPACE);;
   try {
       c = onto.createConcept("class");
       t = onto.createThing(ID, c);
   } catch (Exception ex) {
       fail(ex.getMessage());
   }
   t.setLabel(LABEL);
   t.setComment(COMMENT);
   t.setVersionInfo(VERSIONINFO);
   /*DEBUG dump
   System.out.println("---------");
   ((OntologyMemoryModel)((OntologyImpl)onto).getModel()).dump();
   System.out.println("---------");
   ((OntologyMemoryModel)((OntologyImpl)SOFA.getSystemOntology()).getModel()).dump();
   System.out.println("---------");
   */

 }

 /**
  * The teardown method for JUnit
  *
  * @throws Exception EXCEPTION TO DO
  */
 public void tearDown() throws Exception {
   try {
     dropModel(graphURI, session);
     if (session != null) {
       session.close();
     }
     session = null;
   }
   catch (Exception exception) {
     exception.printStackTrace();
     throw exception;
   }
 }

  /**
   *
   * @param arg0 String
   */
  public ThingJRDFClientUnitTest(String arg0) {
    super(arg0);
    // TODO Auto-generated constructor stub
  }

  /**
   * @throws Exception
   */
  protected void initGraph() throws Exception {
    try {
      //get a Session and graph
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      graphURI = new URI("rmi", hostname, "/" + SERVER_NAME, "testThingClient");
      database = SessionFactoryFinder.newSessionFactory(serverURI, true);
      session = database.newJRDFSession();

      //initialize model
//      if (session.modelExists(graphURI)) {
//        dropModel(graphURI, session);
//      }
      createModel(graphURI, session);

      //get the graph
      graph = JRDFGraphFactory.newClientGraph(serverURI, graphURI);
    }
    catch (Exception exception) {

      exception.printStackTrace();
      throw exception;
    }
  }

  /**
   * Creates a Model
   *
   * @param modelURI URI
   * @param session Session
   * @throws Exception
   */
  private void createModel(URI modelURI, Session session) throws Exception {

    session.createModel(modelURI, new URI("http://tucana.org/tucana#Model"));
  }

  /**
   * Drops a Model
   *
   * @param modelURI URI
   * @param session Session
   * @throws Exception
   */
  private void dropModel(URI modelURI, Session session) throws Exception {
    if (session.modelExists(modelURI)) {
      session.removeModel(modelURI);
    }
  }

  /**
   * METHOD TO DO
   *
   * @param dir PARAMETER TO DO
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
