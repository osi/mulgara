package org.mulgara.sofa.test;

import net.java.dev.sofa.impl.OntoConnector;
import java.util.*;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;


import org.jrdf.graph.*;

import net.java.dev.sofa.impl.*;
import net.java.dev.sofa.model.*;
import org.mulgara.jrdf.LocalJRDFSession;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.SessionFactoryFactory;
import org.mulgara.server.driver.JRDFGraphFactory;
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
public class OntologyJRDFKowariImplUnitTest extends OntologyMemoryImplUnitTest {

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
   * Session used to construct a kowari Graph.
   *
   */
  protected Session session = null;

  /**
   *
   * @param arg0 String
   */
  public OntologyJRDFKowariImplUnitTest(String arg0) {
    super(arg0);
    // TODO Auto-generated constructor stub

  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    initRDFKowariImpl();
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
      try {

        dropModel(graphURI, session);
      } finally {
        graph = null;
//      !!FIXME!! - UNCOMMENT WHEN close() IS ADDED TO JRDF INTERFACE
//
//        try {
//          if (graph != null) {
//            graph.close();
//          }
//        }
//        finally {
//          graph = null;
//        }
      }
    }
    finally {
      try {
        // Close database
        if (database != null) {
          database.close();
        }
      }
      finally {
        database = null;
      }
    }

  }

  /**
   * @throws Exception
   */
  protected void initRDFKowariImpl() throws Exception {
    boolean exceptionOccurred = true;
    try {

      hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      graphURI = new URI("rmi", hostname, "/" + SERVER_NAME, "testKowari");

      File dir =
          new File(System.getProperty("java.io.tmpdir"),
          System.getProperty("user.name"));
      File serverDir = new File(dir, SERVER_NAME);
      serverDir.mkdirs();
      removeContents(serverDir);
//      database = SessionFactoryFinder.newSessionFactory(serverURI, false);
      database = new SessionFactoryFactory().newSessionFactory(serverURI, serverDir);
      session = database.newSession();
      graph = JRDFGraphFactory.newServerGraph(database, graphURI);
      exceptionOccurred = false;
    }
    finally {

      if (exceptionOccurred) {

        tearDown();
      }
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
