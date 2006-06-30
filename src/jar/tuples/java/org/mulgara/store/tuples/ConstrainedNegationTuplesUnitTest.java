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

package org.mulgara.store.tuples;

import java.util.*;

// JUnit
import junit.framework.*;

// Log4J
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.store.nodepool.NodePool;

/**
 * Test case for {@link org.mulgara.store.tuples.ConstrainedNegationTuples}.
 *
 * @created 2004-06-15
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:10 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ConstrainedNegationTuplesUnitTest extends TestCase {

  /**
   * Logger.
   */
  private Logger logger =
      Logger.getLogger(ConstrainedNegationTuplesUnitTest.class.getName());

  /**
   * Tuples for testing with.
   */
  private LiteralTuples t1, t2, t3, t4;

  /**
   * Constrained negation test tuples.
   */
  private ConstrainedNegationTuples tuples;

  /**
   * Constructs a new test with the given name.
   *
   * @param name the name of the test
   */
  public ConstrainedNegationTuplesUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   *
   * @return The test suite
   */
  public static Test suite() {

    TestSuite suite = new TestSuite();

    suite.addTest(new ConstrainedNegationTuplesUnitTest("testConstrained"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testUnconstrained"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testSubjects"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testPredicates"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testObjects"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testSubjectPredicates"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testSubjectObjects"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testPredicateObjects"));
    suite.addTest(new ConstrainedNegationTuplesUnitTest("testSubjectPredicateObjects"));

    return suite;
  }

  /**
   * Create test instance.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void setUp() throws Exception {

    String[] var1 = new String[] { "x", "y", "z" };
    t1 = new LiteralTuples(var1);
    long[][] subjectTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    for (int i = 0; i < subjectTuples.length; i++) {
      ((LiteralTuples) t1).appendTuple(subjectTuples[i]);
    }

    String[] var2 = new String[] { "y", "z", "x" };
    t2 = new LiteralTuples(var2);
    long[][] predicateTuples = new long[][] {
        new long[] { 1, 2, 6 },
        new long[] { 2, 1, 4 },
        new long[] { 2, 1, 7 },
        new long[] { 2, 3, 1 },
        new long[] { 2, 4, 1 },
        new long[] { 2, 4, 3 },
        new long[] { 2, 5, 1 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 4 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    for (int i = 0; i < predicateTuples.length; i++) {
      ((LiteralTuples) t2).appendTuple(predicateTuples[i]);
    }

    String[] var3 = new String[] { "z", "x", "y" };
    t3 = new LiteralTuples(var3);
    long[][] objectTuples = new long[][] {
        new long[] { 1, 4, 2 },
        new long[] { 1, 7, 2 },
        new long[] { 2, 6, 1 },
        new long[] { 3, 1, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 3, 2 },
        new long[] { 5, 1, 2 },
        new long[] { 5, 2, 2 },
        new long[] { 6, 2, 3 },
        new long[] { 6, 4, 2 },
        new long[] { 7, 4, 3 },
        new long[] { 8, 4, 4 },
        new long[] { 8, 5, 5 },
        new long[] { 8, 7, 4 },
        new long[] { 9, 7, 5 },
    };

    for (int i = 0; i < objectTuples.length; i++) {
      ((LiteralTuples) t3).appendTuple(objectTuples[i]);
    }

    String[] var4 = new String[] { "x", "z", "y" };
    t4 = new LiteralTuples(var4);
    long[][] subjectObjectTuples = new long[][] {
        new long[] { 1, 3, 2 },
        new long[] { 1, 4, 2 },
        new long[] { 1, 4, 3 },
        new long[] { 1, 5, 2 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 3 },
        new long[] { 3, 4, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 6, 2 },
        new long[] { 4, 7, 3 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 5 },
        new long[] { 5, 8, 5 },
        new long[] { 6, 2, 1 },
        new long[] { 7, 1, 2 },
        new long[] { 7, 8, 4 },
        new long[] { 7, 9, 2 },
        new long[] { 7, 9, 5 },
    };

    for (int i = 0; i < subjectObjectTuples.length; i++) {
      ((LiteralTuples) t4).appendTuple(subjectObjectTuples[i]);
    }
  }

  /**
   * Default text runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) {

    junit.textui.TestRunner.run(suite());
  }

  //
  // Test cases
  //

  /**
   * Test a negation where all are constrained but does not exist in the
   * database.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testConstrained() throws Exception {

    // Not subject = 1, predicate = 2, object = 3.
    tuples = new ConstrainedNegationTuples(1l, 2l, 3l, t1, new int[] { 0, 1, 2, 3});

    // Expected results - false.
    assertFalse("Should be constrained if found", tuples.isUnconstrained());

    // Not subject = 1, predicate = 2, object = 7.
    tuples = new ConstrainedNegationTuples(1l, 2l, 7l, t1, new int[] { 0, 1, 2, 3});

    // Expected results - TRUE/Unconstrained
    assertTrue("Should be unconstrained if not found", tuples.isUnconstrained());
  }

  /**
   * Test a negation based only all unconstrained that is, variables.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testUnconstrained() throws Exception {

    // Not subject = 1.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, NodePool.NONE,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a negation based only on a given subject.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjects() throws Exception {

    // Not subject = 1.
    tuples = new ConstrainedNegationTuples(1l, NodePool.NONE,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject = 4.
    tuples = new ConstrainedNegationTuples(4l, NodePool.NONE,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject = 7.
    tuples = new ConstrainedNegationTuples(7l, NodePool.NONE,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a negation based only on a given predicate.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testPredicates() throws Exception {

    // Not predicate = 1.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, 1l,
        NodePool.NONE, t2, new int[] { 2, 0, 1, 3 });

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 2, 1, 4 },
        new long[] { 2, 1, 7 },
        new long[] { 2, 3, 1 },
        new long[] { 2, 4, 1 },
        new long[] { 2, 4, 3 },
        new long[] { 2, 5, 1 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 4 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    testTuples(expectedTuples, tuples);

    // Not predicate = 2.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, 2l,
        NodePool.NONE, t2, new int[] {  2, 0, 1, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 6 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    testTuples(expectedTuples, tuples);

    // Not predicate = 7.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, 7l,
        NodePool.NONE, t2, new int[] {  2, 0, 1, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 6 },
        new long[] { 2, 1, 4 },
        new long[] { 2, 1, 7 },
        new long[] { 2, 3, 1 },
        new long[] { 2, 4, 1 },
        new long[] { 2, 4, 3 },
        new long[] { 2, 5, 1 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 4 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a negation based only on a given object.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testObjects() throws Exception {

    // Not object = 3.
    tuples = new ConstrainedNegationTuples(NodePool.NONE,
        NodePool.NONE, 3l, t3, new int[] { 1, 2, 0, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 4, 2 },
        new long[] { 1, 7, 2 },
        new long[] { 2, 6, 1 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 3, 2 },
        new long[] { 5, 1, 2 },
        new long[] { 5, 2, 2 },
        new long[] { 6, 2, 3 },
        new long[] { 6, 4, 2 },
        new long[] { 7, 4, 3 },
        new long[] { 8, 4, 4 },
        new long[] { 8, 5, 5 },
        new long[] { 8, 7, 4 },
        new long[] { 9, 7, 5 },
    };

    testTuples(expectedTuples, tuples);

    // Not object = 8.
    tuples = new ConstrainedNegationTuples(NodePool.NONE,
        NodePool.NONE, 8l, t3, new int[] { 1, 2, 0, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 4, 2 },
        new long[] { 1, 7, 2 },
        new long[] { 2, 6, 1 },
        new long[] { 3, 1, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 3, 2 },
        new long[] { 5, 1, 2 },
        new long[] { 5, 2, 2 },
        new long[] { 6, 2, 3 },
        new long[] { 6, 4, 2 },
        new long[] { 7, 4, 3 },
        new long[] { 9, 7, 5 },
    };

    testTuples(expectedTuples, tuples);

    // Not object = 10.
    tuples = new ConstrainedNegationTuples(NodePool.NONE,
        NodePool.NONE, 10l, t3, new int[] { 1, 2, 0, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 4, 2 },
        new long[] { 1, 7, 2 },
        new long[] { 2, 6, 1 },
        new long[] { 3, 1, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 3, 2 },
        new long[] { 5, 1, 2 },
        new long[] { 5, 2, 2 },
        new long[] { 6, 2, 3 },
        new long[] { 6, 4, 2 },
        new long[] { 7, 4, 3 },
        new long[] { 8, 4, 4 },
        new long[] { 8, 5, 5 },
        new long[] { 8, 7, 4 },
        new long[] { 9, 7, 5 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a negation based only on a given subject, predicate.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjectPredicates() throws Exception {

    // Not subject, predicate = 1, 2.
    tuples = new ConstrainedNegationTuples(1l, 2l,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject, predicate = 4, 2.
    tuples = new ConstrainedNegationTuples(4l, 2l,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject, predicate = 7, 6.
    tuples = new ConstrainedNegationTuples(7l, 6l,
        NodePool.NONE, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);
  }


  /**
   * Test a negation based on a given subject, object.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjectObjects() throws Exception {

    // Not subject, object = 1, 2.
    tuples = new ConstrainedNegationTuples(1l, NodePool.NONE, 4l, t4,
        new int[] { 0, 2, 1, 3 });

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 3, 2 },
        new long[] { 1, 5, 2 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 3 },
        new long[] { 3, 4, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 6, 2 },
        new long[] { 4, 7, 3 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 5 },
        new long[] { 5, 8, 5 },
        new long[] { 6, 2, 1 },
        new long[] { 7, 1, 2 },
        new long[] { 7, 8, 4 },
        new long[] { 7, 9, 2 },
        new long[] { 7, 9, 5 },
    };

    testTuples(expectedTuples, tuples);

    // Not predicate, object = 4, 2.
    tuples = new ConstrainedNegationTuples(7l, NodePool.NONE, 9l, t4,
        new int[] { 0, 2, 1, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 3, 2 },
        new long[] { 1, 4, 2 },
        new long[] { 1, 4, 3 },
        new long[] { 1, 5, 2 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 3 },
        new long[] { 3, 4, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 6, 2 },
        new long[] { 4, 7, 3 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 5 },
        new long[] { 5, 8, 5 },
        new long[] { 6, 2, 1 },
        new long[] { 7, 1, 2 },
        new long[] { 7, 8, 4 },
    };

    testTuples(expectedTuples, tuples);

    // Not predicate, object = 7, 6.
    tuples = new ConstrainedNegationTuples(4l, NodePool.NONE, 9l, t4,
        new int[] { 0, 2, 1, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 3, 2 },
        new long[] { 1, 4, 2 },
        new long[] { 1, 4, 3 },
        new long[] { 1, 5, 2 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 3 },
        new long[] { 3, 4, 2 },
        new long[] { 4, 1, 2 },
        new long[] { 4, 6, 2 },
        new long[] { 4, 7, 3 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 5 },
        new long[] { 5, 8, 5 },
        new long[] { 6, 2, 1 },
        new long[] { 7, 1, 2 },
        new long[] { 7, 8, 4 },
        new long[] { 7, 9, 2 },
        new long[] { 7, 9, 5 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a negation based only on a given predicate, object.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testPredicateObjects() throws Exception {

    // Not predicate, object = 1, 2.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, 1l, 2l, t2,
        new int[] { 2, 0, 1, 3 });

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 2, 1, 4 },
        new long[] { 2, 1, 7 },
        new long[] { 2, 3, 1 },
        new long[] { 2, 4, 1 },
        new long[] { 2, 4, 3 },
        new long[] { 2, 5, 1 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 4 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    testTuples(expectedTuples, tuples);

    // Not predicate, object = 4, 2.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, 2l, 4l, t2,
        new int[] { 2, 0, 1, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 6 },
        new long[] { 2, 1, 4 },
        new long[] { 2, 1, 7 },
        new long[] { 2, 3, 1 },
        new long[] { 2, 5, 1 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 4 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    testTuples(expectedTuples, tuples);

    // Not predicate, object = 7, 6.
    tuples = new ConstrainedNegationTuples(NodePool.NONE, 7l, 6l, t2,
        new int[] { 2, 0, 1, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 6 },
        new long[] { 2, 1, 4 },
        new long[] { 2, 1, 7 },
        new long[] { 2, 3, 1 },
        new long[] { 2, 4, 1 },
        new long[] { 2, 4, 3 },
        new long[] { 2, 5, 1 },
        new long[] { 2, 5, 2 },
        new long[] { 2, 6, 4 },
        new long[] { 3, 6, 2 },
        new long[] { 3, 7, 4 },
        new long[] { 4, 8, 4 },
        new long[] { 4, 8, 7 },
        new long[] { 5, 8, 5 },
        new long[] { 5, 9, 7 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a negation based only on a given subject, predicate, object.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjectPredicateObjects() throws Exception {

    // Not subject, predicate, object = 1, 2, 3.
    tuples = new ConstrainedNegationTuples(1l, 2l, 3l, t1,
        new int[] { 0, 1, 2, 3 });

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject, predicate, object = 1, 2, 4.
    tuples = new ConstrainedNegationTuples(1l, 2l, 4l, t1,
        new int[] { 0, 1, 2, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject, predicate, object = 1, 2, 5.
    tuples = new ConstrainedNegationTuples(1l, 2l, 5l, t1,
        new int[] { 0, 1, 2, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject, predicate, object = 7, 5, 9.
    tuples = new ConstrainedNegationTuples(7l, 5l, 9l, t1,
        new int[] { 0, 1, 2, 3});

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
    };

    testTuples(expectedTuples, tuples);

    // Not subject, predicate, object = 7, 5, 7.
    tuples = new ConstrainedNegationTuples(7l, 5l, 7l, t1,
        new int[] { 0, 1, 2, 3 });

    // Expected results
    expectedTuples = new long[][] {
        new long[] { 1, 2, 3 },
        new long[] { 1, 2, 4 },
        new long[] { 1, 2, 5 },
        new long[] { 1, 3, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 6 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 2, 1 },
        new long[] { 4, 2, 6 },
        new long[] { 4, 3, 7 },
        new long[] { 4, 4, 8 },
        new long[] { 4, 5, 8 },
        new long[] { 5, 5, 8 },
        new long[] { 6, 1, 2 },
        new long[] { 7, 2, 1 },
        new long[] { 7, 2, 9 },
        new long[] { 7, 4, 8 },
        new long[] { 7, 5, 9 },
    };

    assertTrue("Should be unconstrained if not found", tuples.isUnconstrained());
    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a constraint negation's results.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testTuples(long[][] expectedTuples, Tuples result)
      throws Exception {

    assertTrue("Expected: " + expectedTuples.length + ", " +
        result.getRowCount(), expectedTuples.length == result.getRowCount());

    if (result.getRowCount() > 0) {
      result.beforeFirst();
      result.next();

      int index = 0;
      do {

        long tuple[] = new long[] {
            result.getColumnValue(0),
            result.getColumnValue(1),
            result.getColumnValue(2)
        };

        assertTrue("Expected tuple result: " + expectedTuples[index][0] + "," +
            expectedTuples[index][1] + "," +
            expectedTuples[index][2] + " but was: " + tuple[0] + "," + tuple[1] +
            "," + tuple[1],
            expectedTuples[index][0] == tuple[0] &&
            expectedTuples[index][1] == tuple[1] &&
            expectedTuples[index][2] == tuple[2]);
        index++;
        result.next();
      }
      while (index < expectedTuples.length);

      assertFalse("Should have no extra results", result.next());
    }
  }
}
