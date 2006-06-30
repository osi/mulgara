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

// Standard APIs
import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;

// Jena APIs
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.test.*;
import com.hp.hpl.jena.shared.*;

// JUnit.
import junit.framework.*;
import org.apache.log4j.*;

// Internal Kowari APIs
import org.kowari.server.*;
import org.kowari.query.*;
import org.kowari.server.driver.*;

/**
 * Test case for {@link GraphKowariMaker}.
 *
 * @created 2003-02-27
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
public class GraphKowariMakerUnitTest extends AbstractTestGraphMaker {

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
  private GraphKowariMaker graphMaker;

  /**
   * The session being used for the tests.
   */
  private LocalJenaSession session;

  public GraphKowariMakerUnitTest(String name) {

    super(name);
//    Logger.getRootLogger().setLevel(Level.ERROR);
  }

  public static TestSuite suite() {

    return new TestSuite(GraphKowariMakerUnitTest.class);
//    TestSuite result = new TestSuite();
//    result.addTest(new GraphKowariMakerUnitTest("testListAfterDelete"));
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
   * Create a new graph maker.
   */
  public GraphMaker getGraphMaker() {
    graphMaker = new GraphKowariMaker(session, serverURI,
        ReificationStyle.Minimal);
    return graphMaker;
  }

  /**
   * Setups up the test for JUnit - creates the database and graph ready for
   * use.
   *
   * @throws Exception if there was an error creating the database - always
   *     fatal.
   */
  public void setUp() {

    boolean exceptionOccurred = true;

    try {

      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      graphURITest = new URI("rmi", hostname, "/" + SERVER_NAME, "test");

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverURI, false);
      this.session = (LocalJenaSession) sessionFactory.newJenaSession();
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
   * Remove any existing graphs.
   */
  public void tearDown() {

    super.tearDown();

    try {
      if (graphMaker != null) {
        graphMaker.removeAll();
      }
    }
    finally {

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
