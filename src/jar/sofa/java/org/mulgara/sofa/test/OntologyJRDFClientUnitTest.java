package org.mulgara.sofa.test;

import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import junit.framework.*;

import org.jrdf.graph.*;

import net.java.dev.sofa.impl.*;
import net.java.dev.sofa.model.*;

import org.mulgara.server.*;
import org.mulgara.server.driver.JRDFGraphFactory;
import org.mulgara.server.driver.SessionFactoryFinder;
import org.mulgara.sofa.*;

/**
 * TODO ONE LINE DESC <p>
 *
 * TODO MORE DETAILED DESC
 * </p>
 *
 * @created Sep 1, 2004
 *
 * @author Keith Ahern
 *
 * @version $Revision: 1.12 $
 *
 * @modified $Date: 2005/01/28 00:44:55 $
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
public class OntologyJRDFClientUnitTest extends OntologyMemoryImplUnitTest {

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
   * Description of the Field
   */
  protected Session session = null;

  /**
   *
   * @param arg0 String
   */
  public OntologyJRDFClientUnitTest(String arg0) {
    super(arg0);
    // TODO Auto-generated constructor stub

  }

  public static void main(String[] args) {
    junit.textui.TestRunner.run(OntologyJRDFClientUnitTest.class);
  }

  /**
   * Returns a test suite containing the tests to be run.
   *
   * @return the test suite
   */
  public static Test suite() throws Exception {

    TestSuite suite = new TestSuite();
    suite.addTest(new OntologyJRDFClientUnitTest("testBasics"));
    suite.addTest(new OntologyJRDFClientUnitTest("testClasses"));
    suite.addTest(new OntologyJRDFClientUnitTest("testProperties"));
    suite.addTest(new OntologyJRDFClientUnitTest("testRemoveThings"));
    suite.addTest(new OntologyJRDFClientUnitTest("testThings"));
    return suite;
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    initGraph();
    OntologyModel ontoModel = new OntologyJRDFModel(graph);
    onto = OntoConnector.getInstance().createOntology(ontoModel, ONTO_NAMESPACE);
    initOntology();
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
    } catch (Exception exception) {
      exception.printStackTrace();
      throw exception;
    }
  }

  /**
   * @throws Exception
   */
  protected void initGraph() throws Exception {
    try {
      //get a Session and graph
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      graphURI = new URI("rmi", hostname, "/" + SERVER_NAME, "testClient");
      //get session
      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverURI, true);
      session = sessionFactory.newSession();
      //model must already exist
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

    session.createModel(modelURI, new URI("http://mulgara.org/mulgara#Model"));
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
