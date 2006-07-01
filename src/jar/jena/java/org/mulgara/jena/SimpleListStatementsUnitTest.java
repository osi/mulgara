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

import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.graph.test.*;
import com.hp.hpl.jena.vocabulary.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.rdf.model.test.*;
import com.hp.hpl.jena.graph.*;

import junit.framework.*;

import java.io.*;
import java.net.InetAddress;
import java.net.URI;
import java.util.*;

import org.apache.log4j.*;

import org.mulgara.query.QueryException;
import org.mulgara.server.Session;
import org.mulgara.server.driver.SessionFactoryFinder;
import org.mulgara.server.SessionFactory;

/**
 * Test case for {@link TestList}.
 *
 * @created 2003-12-01
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
public class SimpleListStatementsUnitTest extends ModelTestBase {

  /**
   * Default name to use for the Jena model.
   */
  protected final static String SERVER_NAME = "jenamodeltest";

  /**
   * Init the logging class
   */
  private static Logger logger =
      Logger.getLogger(SimpleListStatementsUnitTest.class.getName());

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
  protected static GraphMulgaraMaker graphMaker = null;

  /**
   * Creates new models.
   */
  protected static ModelMulgaraMaker modelMaker = null;

  /**
   * The server URI.
   */
  protected URI serverURI;

  /**
   * The model URI.
   */
  protected URI modelURI;

  static boolean booleanValue = true;
  static char    charValue   = 'c';
  static long    longValue   = 456;
  static float   floatValue  = 5.67F;
  static double  doubleValue = 6.78;
  static String   stringValue ="stringValue";
  static String   langValue   = "en";

  /**
   * CONSTRUCTOR JenaModelAbstractTest TO DO
   *
   * @param name PARAMETER TO DO
   * @throws Exception EXCEPTION TO DO
   */
  public SimpleListStatementsUnitTest(String name) {

    super(name);

//    Logger.getRootLogger().setLevel(Level.ERROR);

    System.setProperty("org.mulgara.xml.ResourceDocumentBuilderFactory",
        "org.apache.xerces.jaxp.DocumentBuilderFactoryImpl");

    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, "test");
    }
    catch (Exception e) {} ;
  }

  public static TestSuite suite() {
    return new TestSuite(SimpleListStatementsUnitTest.class);
//    TestSuite result = new TestSuite();
//    result.addTest(new SimpleListStatementsUnitTest("testListStatementsClever"));
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
   * The JUnit setup method
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void setUp() {

    boolean exceptionOccurred = true;
    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
          serverURI, false);
      LocalJenaSession session = (LocalJenaSession) sessionFactory.
          newJenaSession();

      // Create model
      graphMaker = new GraphMulgaraMaker((LocalJenaSession) session, serverURI,
          ReificationStyle.Minimal);
      modelMaker = new ModelMulgaraMaker((GraphMulgaraMaker) graphMaker);
      model = modelMaker.createModel();

      // Add default statements to model
      model.createResource("http://example.org/boolean")
          .addProperty(RDF.value, booleanValue);
      model.createResource("http://example.org/char")
          .addProperty(RDF.value, charValue);
      model.createResource("http://example.org/long")
          .addProperty(RDF.value, longValue);
      model.createResource("http://example.org/float")
          .addProperty(RDF.value, floatValue);
      model.createResource("http://example.org/double")
          .addProperty(RDF.value, doubleValue);
      model.createResource("http://example.org/string")
          .addProperty(RDF.value, stringValue);
      model.createResource("http://example.org/langString")
          .addProperty(RDF.value, stringValue, langValue);

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

  public void testBoolean() {
    StmtIterator iter = model.listStatements(null, null, booleanValue);
    int i = 0;
    while (iter.hasNext()) {
      i++;
      assertEquals(iter.nextStatement().getSubject().getURI(),
          "http://example.org/boolean");
    }
    assertEquals(1, i);
  }

  public void testChar() {
    StmtIterator iter = model.listStatements(null, null, charValue);
    int i = 0;
    while (iter.hasNext()) {
      i++;
      assertEquals(iter.nextStatement().getSubject().getURI(),
          "http://example.org/char");
    }
    assertEquals(1, i);
  }

  public void testLong() {
    StmtIterator iter = model.listStatements(null, null, longValue);
    int i = 0;
    while (iter.hasNext()) {
      i++;
      assertEquals(iter.nextStatement().getSubject().getURI(),
          "http://example.org/long");
    }
    assertEquals(1, i);
  }

  public void testFloat() {
    StmtIterator iter = model.listStatements(null, null, floatValue);
    int i = 0;
    while (iter.hasNext()) {
      i++;
      assertEquals(iter.nextStatement().getSubject().getURI(),
          "http://example.org/float");
    }
    assertEquals(1, i);
  }

  public void testDouble() {
    StmtIterator iter = model.listStatements(null, null, doubleValue);
    int i = 0;
    while (iter.hasNext()) {
      i++;
      assertEquals(iter.nextStatement().getSubject().getURI(),
          "http://example.org/double");
    }
    assertEquals(1, i);
  }

//  We don't support language.
//  public void testString() {
//    StmtIterator iter = model.listStatements(null, null, stringValue);
//    int i = 0;
//    while (iter.hasNext()) {
//      i++;
//      assertEquals(iter.nextStatement().getSubject().getURI(),
//          "http://example.org/string");
//    }
//    assertEquals(1, i);
//  }
//
//  We don't support language.
//  public void testLangString() {
//    StmtIterator iter = model.listStatements(null, null,
//        stringValue, langValue);
//    int i = 0;
//    while (iter.hasNext()) {
//      i++;
//      String uri = iter.nextStatement().getSubject().getURI();
//      assertEquals(uri, "http://example.org/langString");
//    }
//    assertEquals(1, i);
//  }

  public void testAll() {
    StmtIterator iter = model.listStatements(null, null, (RDFNode)null);
    int i = 0;
    while (iter.hasNext()) {
      i++;
      iter.next();
    }
    assertEquals(7, i);
  }

  public Model modelWithStatements(StmtIterator it) {
    Model m = modelMaker.createModel();
    while (it.hasNext()) {
      Statement st = it.nextStatement();
      m.add(st);
    };
    return m;
  }

  public static Model modelWithStatements(ReificationStyle style, String facts) {
    Model m = new ModelMulgara((GraphMulgara) graphMaker.createGraph());
    modelAdd(m, facts);
    return m;
  }

  public static Model modelWithStatements(String facts) {
    Model m = modelWithStatements(ReificationStyle.Standard, facts);
    return m;
  }

  public void checkReturns(String things, StmtIterator it) {
    Model wanted = modelWithStatements(things);
    Model got = modelWithStatements(it);
    if (wanted.isIsomorphicWith(got) == false)
      fail("wanted " + wanted + " got " + got);
  }


  public void testListStatementsSPO() {
    Model m = modelMaker.createModel();
    Resource A = resource(m, "A"), X = resource(m, "X");
    Property P = property(m, "P"), P1 = property(m, "P1");
    RDFNode O = resource(m, "O"), Y = resource(m, "Y");
    String S1 = "S P O; S1 P O; S2 P O";
    String S2 = "A P1 B; A P1 B; A P1 C";
    String S3 = "X P1 Y; X P2 Y; X P3 Y";
    modelAdd(m, S1);
    modelAdd(m, S2);
    modelAdd(m, S3);
    checkReturns(S1, m.listStatements(null, P, O));
    checkReturns(S2, m.listStatements(A, P1, (RDFNode)null));
    checkReturns(S3, m.listStatements(X, null, Y));
    m.close();
  }

  public void testListStatementsClever() {
    Model m = modelMaker.createModel();
    modelAdd(m, "S P O; S P O2; S P2 O; S2 P O");
    Selector sel = new SimpleSelector(null, null, (RDFNode) null) {
      public boolean test(Statement st) {
        return
            st.getSubject().toString().length()
            + st.getPredicate().toString().length()
            + st.getObject().toString().length()
            == 12; /* eh:S + eh:P + eh:O */
      }

      public boolean isSimple() {
        return false;
      }
    };
    checkReturns("S P O", m.listStatements(sel));
  }

  private static Model standardModel() {
    Model result = modelMaker.createModel();
    result.setNsPrefixes(PrefixMapping.Standard);
    return result;
  }

  /**
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void tearDown() {

    graphMaker.removeAll();
    if (model != null) {
      try {
        model.close();
      }
      finally {
        model = null;
      }
    }

    if (session != null) {
      try {
        session.close();
      }
      catch (QueryException qe) {}
      finally {
        session = null;
      }
    }
  }
}
