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

// Java APIs
import java.io.*;
import java.net.InetAddress;
import java.net.URI;

// Log4j
import org.apache.log4j.*;

// Junit API
import junit.framework.*;

// JRDF API
import org.jrdf.graph.GraphException;

// Jena API
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.test.*;
import com.hp.hpl.jena.shared.*;

// Internal Kowari APIs
import org.kowari.server.*;
import org.kowari.server.driver.*;
import com.hp.hpl.jena.graph.query.QueryHandler;

/**
 * Test case for {@link GraphMulgara}.
 *
 * @created 2003-02-09
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
 * @copyright &copy; 2001-2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class GraphMulgaraUnitTest extends MetaTestGraph {

  /**
   * init the logging class
   */
  private static Logger logger =
      Logger.getLogger(GraphMulgaraUnitTest.class.getName());

  /**
   * The graph.
   */
  private static Graph localGraphKowari;

  /**
   * The URI of the server.
   */
  protected URI serverURI;

  /**
   * The URI of the graph.
   */
  protected static URI graphURITest;

  /**
   * The name of the test model.
   */
  protected final static String SERVER_NAME = "server1";

  /**
   * Creates new graphs.
   */
  protected GraphMulgaraMaker graphMaker;

  /**
   * The session.
   */
  protected static LocalJenaSession sessionTest = null;

  /**
   * The Kowari graph that we're adding statements to.
   */
  protected GraphMulgara graphKowari;

  /**
   * Calls the super classes constructor.
   *
   * @param graphClass the class to create.
   * @param name the name of the JUnit task.
   * @param style the type of reification to use.
   */
  public GraphMulgaraUnitTest(Class graphClass, String name,
      ReificationStyle style) {
    super(graphClass, name, style);
  }

  /**
   * Answer a test suite that runs the Graph and Reifier tests on GraphMem
   * and on  WrappedGraphMem, the latter standing in for testing
   * WrappedGraph.
   */
  public static TestSuite suite() {

    return MetaTestGraph.suite(GraphMulgaraUnitTest.class, LocalGraphKowari.class);
//    TestSuite result = new TestSuite();
//    result.addTest(new GraphKowariUnitTest(LocalGraphKowari.class, "testContainsConcrete",
//        ReificationStyle.Convenient));
//    result.addTest(new GraphKowariUnitTest(LocalGraphKowari.class, "testContainsNode",
//        ReificationStyle.Convenient));
//    return result;
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
   * Setups up the test for JUnit - creates the database and graph ready for
   * use.
   *
   * @throws Exception if there was an error creating the database - always
   *     fatal.
   */
  public void setUp() throws Exception {

    boolean exceptionOccurred = true;
    try {

      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      graphURITest = new URI(serverURI + "#test");

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverURI, false);
      sessionTest = (LocalJenaSession) sessionFactory.newJenaSession();

      graphMaker = new GraphMulgaraMaker((LocalJenaSession)
          sessionTest, serverURI, ReificationStyle.Minimal);

      //localGraphKowari = new LocalGraphKowari(ReificationStyle.Minimal);
      localGraphKowari = graphMaker.createGraph();

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
   * Test nodes can be found in all triple positions.
   * However, testing for literals in subject positions is suppressed
   * at present to avoid problems with InfGraphs which try to prevent
   * such constructs leaking out to the RDF layer.
   */
  public void testContainsNode() {
    Graph g = getGraph();
    graphAdd(g, "a P b; _c Q _d; a R 12");
    QueryHandler qh = g.queryHandler();
    assertTrue(qh.containsNode(node("a")));
    assertTrue(qh.containsNode(node("P")));
    assertTrue(qh.containsNode(node("b")));
    assertTrue(qh.containsNode(node("_c")));
    assertTrue(qh.containsNode(node("Q")));
    assertTrue(qh.containsNode(node("_d")));
//        assertTrue( qh.containsNode( node( "10" ) ) );
    assertTrue(qh.containsNode(node("R")));
    assertTrue(qh.containsNode(node("12")));
    /* */
    assertFalse(qh.containsNode(node("x")));
    assertFalse(qh.containsNode(node("_y")));
    assertFalse(qh.containsNode(node("99")));
  }

  public void testContainsConcrete() {
    Graph g = getGraph();
    graphAdd(g, "s P o; _x R _y; x S 0");
    assertTrue(g.contains(triple("s P o")));
    assertTrue(g.contains(triple("_x R _y")));
    assertTrue(g.contains(triple("x S 0")));
    /* */
    assertFalse(g.contains(triple("s P Oh")));
    assertFalse(g.contains(triple("S P O")));
    assertFalse(g.contains(triple("s p o")));
    assertFalse(g.contains(triple("_x r _y")));
    assertFalse(g.contains(triple("x S 1")));
  }

  /**
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void tearDown() throws Exception {

    localGraphKowari.close();

    try {
      sessionTest.removeModel(graphURITest);
      sessionTest.removeModel(new URI(graphURITest + "_ref"));
    }
    finally {
      sessionTest.close();
    }

    if (graphMaker != null) {
      try {
        graphMaker.removeAll();
        graphMaker.close();
      }
      finally {
        graphMaker = null;
      }
    }
  }

  public static Graph getGraph(Object wrap, Class graphClass,
      ReificationStyle style) {
    try {
      return localGraphKowari;
    }
    catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * A local implementation of GraphKowari that accepts the reification
   * constructor.  Currently, this is ignored.
   */
  public static class LocalGraphKowari extends GraphMulgara {

    /**
     * Create a new LocalGraphKowari with the given reification style.
     *
     * @param style ReificationStyle this is currentl ignored.
     * @throws GraphException if there was a failure in creating the graph.
     */
    public LocalGraphKowari(ReificationStyle style) throws GraphException {
      super(sessionTest, graphURITest);
    }
  }
}
