/*
 * OntologyMemoryImplUnitTest.java
 * -----------------------------------------------------------------------------
 * Project           SOFAImpl
 * Package           net.java.dev.sofa.junit
 * Original author   Alex V. Alishevskikh
 *                   [alexeya@dev.java.net]
 * Created           16.04.2004 17:30:22
 * Revision info     $RCSfile: OntologyMemoryImplUnitTest.java,v $ $Revision: 1.10 $ $State: Exp $
 *
 * Last modified on  $Date: 2005/01/28 00:46:41 $
 *               by  $Author: newmana $
 *
 * Version: 1.0
 *
 * Copyright (c) 2004 Alex Alishevskikh
 *
 * GNU Lesser General Public License (http://www.gnu.org/copyleft/lesser.txt)
 */

package org.mulgara.sofa.test;

import net.java.dev.sofa.*;
import net.java.dev.sofa.impl.*;

import org.mulgara.sofa.*;

import junit.framework.*;

/**
 * JAVADOC: <code>OntologyMemoryImplUnitTest</code>
 *
 * @version $Id: OntologyMemoryImplUnitTest.java,v 1.10 2005/01/28 00:46:41 newmana Exp $
 * @author Alex
 */
public class OntologyMemoryImplUnitTest extends TestCase {

  Ontology onto = null;
  Concept c = null;
  Thing t1 = null;
  Thing t2 = null;

  public static final String ONTO_NAMESPACE =
      "http://sofa.org/net.java.dev.sofa.junit/namespace";
  public static final String ONTO_LABEL = "Test ontology";
  public static final String ONTO_COMMENT =
      "Test ontology for SOFA implementation testing.";
  public static final String ONTO_VERSIONINFO = "v1.0";

  public static void main(String[] args) {
    junit.textui.TestRunner.run(OntologyMemoryImplUnitTest.class);

  }

  /**
   * Returns a test suite containing the tests to be run.
   *
   * @return the test suite
   */
  public static Test suite() throws Exception {

    TestSuite suite = new TestSuite();
    suite.addTest(new OntologyMemoryImplUnitTest("testBasics"));
    suite.addTest(new OntologyMemoryImplUnitTest("testClasses"));
    suite.addTest(new OntologyMemoryImplUnitTest("testProperties"));
    suite.addTest(new OntologyMemoryImplUnitTest("testRemoveThings"));
    suite.addTest(new OntologyMemoryImplUnitTest("testThings"));
    return suite;
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    onto = OntoConnector.getInstance().createOntology(ONTO_NAMESPACE);
    initOntology();
  }

  /**
   * initialises Ontology
   */
  protected void initOntology() {
    onto.setLabel(ONTO_LABEL);
    onto.setComment(ONTO_COMMENT);
    onto.setVersionInfo(ONTO_VERSIONINFO);
    try {
      c = onto.createConcept("class");
      t1 = onto.createThing("thing1", c);
      t2 = onto.createThing("thing2", c);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  /**
   * Constructor for OntologyMemoryImplUnitTest.
   *
   * @param arg0 String
   */
  public OntologyMemoryImplUnitTest(String arg0) {
    super(arg0);
  }

  public void testBasics() {
    assertEquals(onto.getNameSpace(), java.net.URI.create(ONTO_NAMESPACE));
    assertEquals(onto.getLabel(), ONTO_LABEL);
    assertEquals(onto.getComment(), ONTO_COMMENT);
    assertEquals(onto.getVersionInfo(), ONTO_VERSIONINFO);
  }

  public void testThings() {
    assertNotNull(onto);
    assertNotNull(t1);
    assertNotNull(t2);
    assertNotSame(t1, t2);
    assertTrue(onto.hasThing("thing1"));
    assertTrue(onto.hasThing("thing2"));
    assertTrue(onto.hasThing(t1));
    assertTrue(onto.hasThing(t2));
    assertFalse(onto.hasThing("thing_no_exists"));
    assertFalse(t1.isConcept());
    assertFalse(t1.isRelation());
    Thing t1_ = onto.getThing("thing1");
    Thing t2_ = onto.getThing("thing2");
    assertNotNull(t1_);
    assertNotNull(t2_);
    assertNull(onto.getThing("thing_no_exists"));
    assertEquals(t1, t1_);
    assertEquals(t2, t2_);
    //assertEquals(onto.getThings().size(), 2);

  }

  public void testClasses() {
    Concept c1 = null;
    Concept c2 = null;
    try {
      c1 = onto.createConcept("class1");
      c2 = onto.createConcept("class2");
    }
    catch (Exception ex) {
      fail(ex.getMessage());
    }
    assertNotNull(c1);
    assertNotNull(c2);
    assertNotSame(c1, c2);
    assertTrue(c1.isConcept());
    assertTrue(c2.isConcept());
    assertTrue(onto.hasConcept("class1"));
    assertTrue(onto.hasConcept("class2"));
    assertEquals(onto.getConcept("class1"), c1);
    assertEquals(onto.getConcept("class2"), c2);
    //assertEquals(onto.getClasses().size(), 2);
  }

  public void testProperties() {
    Relation p1 = null;
    Relation p2 = null;
    try {
      p1 = onto.createRelation("prop1");
      p2 = onto.createRelation("prop2");
    }
    catch (Exception ex) {
      fail(ex.getMessage());
    }
    assertNotNull(p1);
    assertNotNull(p2);
    assertNotSame(p1, p2);
    assertTrue(p1.isRelation());
    assertTrue(p2.isRelation());
    assertTrue(onto.hasThing("prop1"));
    assertTrue(onto.hasRelation("prop1"));
    assertTrue(onto.hasRelation("prop2"));
    assertEquals(onto.getRelation("prop1"), p1);
    assertEquals(onto.getRelation("prop2"), p2);
    //assertEquals(onto.getProperties().size(), 2);
  }

  public void testRemoveThings() {
    int initSize = onto.getThings().size();
//    System.out.println(onto.getThings().size() + "---------");
//    ((OntologyJRDFModel) ((OntologyImpl) onto).getModel()).dump();
//    System.out.println("---------");
    onto.remove(t1);
    onto.remove("thing2");
    assertFalse(onto.hasThing("thing1"));
    assertFalse(onto.hasThing("thing2"));
    assertFalse(onto.hasThing(t1));
    assertFalse(onto.hasThing(t2));
    assertNull(onto.getThing("thing1"));
    assertNull(onto.getThing("thing2"));
//    System.out.println(onto.getThings().size() + "---------");
//    ((OntologyJRDFModel) ((OntologyImpl) onto).getModel()).dump();
//    System.out.println("---------");
    assertEquals(onto.getThings().size(), initSize - 2);
  }
}
