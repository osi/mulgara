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

package org.mulgara.resolver.store;

import java.util.*;

// JUnit
import junit.framework.*;

// Log4J
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.store.statement.*;
import org.mulgara.store.tuples.LiteralTuples;
import org.mulgara.store.tuples.Tuples;

/**
 * Test case for {@link StatementStoreDuplicateResolution}.
 *
 * @created 2004-06-15
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.3 $
 *
 * @modified $Date: 2005/01/05 04:58:55 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class StatementStoreDuplicateResolutionUnitTest extends TestCase {

  /**
   * Logger.
   */
  private Logger logger =
      Logger.getLogger(StatementStoreDuplicateResolutionUnitTest.class.getName());

  /**
   * Tuples for testing with.
   */
  private LiteralTuples t1, t2, t3, t4;

  /**
   * Constrained negation test tuples.
   */
  private StatementStoreDuplicateResolution tuples;

  /**
   * Constructs a new test with the given name.
   *
   * @param name the name of the test
   */
  public StatementStoreDuplicateResolutionUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   *
   * @return The test suite
   */
  public static Test suite() {

    TestSuite suite = new TestSuite();

    suite.addTest(new StatementStoreDuplicateResolutionUnitTest("testSubjectPredicate"));
    suite.addTest(new StatementStoreDuplicateResolutionUnitTest("testSubjectObject"));
    suite.addTest(new StatementStoreDuplicateResolutionUnitTest("testPredicateObject"));
    suite.addTest(new StatementStoreDuplicateResolutionUnitTest("testSubjectPredicateObject"));

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
        new long[] { 1, 1, 1 },
        new long[] { 1, 3, 1 },
        new long[] { 1, 3, 4 },
        new long[] { 1, 4, 1 },
        new long[] { 2, 1, 4 },
        new long[] { 2, 2, 2 },
        new long[] { 2, 2, 3 },
        new long[] { 2, 2, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 2, 3, 1 },
        new long[] { 3, 1, 1 },
        new long[] { 3, 1, 3 },
        new long[] { 3, 3, 3 },
        new long[] { 3, 2, 4 },
        new long[] { 4, 1, 1 },
        new long[] { 4, 4, 1 },
        new long[] { 4, 5, 4 },
        new long[] { 4, 5, 6 },
        new long[] { 4, 5, 7 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 7, 7 },
        new long[] { 7, 8, 1 },
    };

    for (int i = 0; i < subjectTuples.length; i++) {
      ((LiteralTuples) t1).appendTuple(subjectTuples[i]);
    }

    String[] var2 = new String[] { "y", "z", "x" };
    t2 = new LiteralTuples(var2);
    long[][] predicateTuples = new long[][] {
        new long[] { 1, 1, 1 },
        new long[] { 1, 1, 3 },
        new long[] { 1, 1, 4 },
        new long[] { 1, 3, 3 },
        new long[] { 1, 4, 2 },
        new long[] { 2, 2, 2 },
        new long[] { 2, 3, 2 },
        new long[] { 2, 4, 2 },
        new long[] { 2, 4, 3 },
        new long[] { 2, 5, 2 },
        new long[] { 3, 1, 1 },
        new long[] { 3, 1, 2 },
        new long[] { 3, 3, 3 },
        new long[] { 3, 4, 1 },
        new long[] { 4, 1, 1 },
        new long[] { 4, 1, 4 },
        new long[] { 5, 4, 4 },
        new long[] { 5, 6, 4 },
        new long[] { 5, 7, 4 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 7, 7 },
        new long[] { 8, 1, 7 },
    };

    for (int i = 0; i < predicateTuples.length; i++) {
      ((LiteralTuples) t2).appendTuple(predicateTuples[i]);
    }

    String[] var4 = new String[] { "x", "z", "y" };
    t4 = new LiteralTuples(var4);
    long[][] subjectObjectTuples = new long[][] {
        new long[] { 1, 1, 1 },
        new long[] { 1, 1, 3 },
        new long[] { 1, 1, 4 },
        new long[] { 1, 4, 3 },
        new long[] { 2, 1, 3 },
        new long[] { 2, 2, 2 },
        new long[] { 2, 3, 2 },
        new long[] { 2, 4, 1 },
        new long[] { 2, 4, 2 },
        new long[] { 2, 5, 2 },
        new long[] { 3, 1, 1 },
        new long[] { 3, 3, 1 },
        new long[] { 3, 3, 3 },
        new long[] { 3, 4, 2 },
        new long[] { 4, 1, 1 },
        new long[] { 4, 1, 4 },
        new long[] { 4, 4, 5 },
        new long[] { 4, 6, 5 },
        new long[] { 4, 7, 5 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 1, 8 },
        new long[] { 7, 7, 7 },
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
   * Test a constraint where the subject and predicate are the same.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjectPredicate() throws Exception {

    // Not subject = 1.
    tuples = new StatementStoreDuplicateResolution(
        new boolean[] { true, true, false }, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 1, 1 },
        new long[] { 2, 2, 2 },
        new long[] { 2, 2, 3 },
        new long[] { 2, 2, 4 },
        new long[] { 2, 2, 5 },
        new long[] { 3, 3, 3 },
        new long[] { 4, 4, 1 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 7, 7 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a constraint where the predicate and object are the same.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testPredicateObject() throws Exception {

    // Not subject = 1.
    tuples = new StatementStoreDuplicateResolution(
        new boolean[] { false, true, true }, t2, new int[] { 1, 2, 0, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 1, 1 },
        new long[] { 1, 1, 3 },
        new long[] { 1, 1, 4 },
        new long[] { 2, 2, 2 },
        new long[] { 3, 3, 3 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 7, 7 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a constraint where the subject and object are the same.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjectObject() throws Exception {

    // Not subject = 1.
    tuples = new StatementStoreDuplicateResolution(
        new boolean[] { true, false, true }, t4, new int[] { 0, 2, 1, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 1, 1 },
        new long[] { 1, 1, 3 },
        new long[] { 1, 1, 4 },
        new long[] { 2, 2, 2 },
        new long[] { 3, 3, 1 },
        new long[] { 3, 3, 3 },
        new long[] { 4, 4, 5 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 7, 7 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test a constraint where the subject, predicate and object are the same.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testSubjectPredicateObject() throws Exception {

    // Not subject = 1.
    tuples = new StatementStoreDuplicateResolution(
        new boolean[] { true, true, true }, t1, new int[] { 0, 1, 2, 3});

    // Expected results
    long[][] expectedTuples = new long[][] {
        new long[] { 1, 1, 1 },
        new long[] { 2, 2, 2 },
        new long[] { 3, 3, 3 },
        new long[] { 5, 5, 5 },
        new long[] { 6, 6, 6 },
        new long[] { 7, 7, 7 },
    };

    testTuples(expectedTuples, tuples);

    String[] vars1a = new String[] { "x", "y", "z" };
    Tuples t1a = new LiteralTuples(vars1a);
    long[][] t1aTuples = new long[][] {
        new long[] { 3897, 3906, 3906 },
        new long[] { 3897, 3908, 3908 },
        new long[] { 3899, 3892, 3900 },
        new long[] { 3900, 3892, 3894 },
        new long[] { 3906, 3892, 3894 },
        new long[] { 3906, 3892, 3907 },
        new long[] { 3906, 3906, 3906 },
        new long[] { 3908, 3892, 3896 },
        new long[] { 3908, 3892, 3908 },
        new long[] { 3908, 3908, 3894 },
        new long[] { 3908, 3908, 3908 },
        new long[] { 3910, 3892, 3902 },
        new long[] { 3910, 3910, 3910 },
        new long[] { 3911, 3892, 3904 },
        new long[] { 3911, 3892, 3911 },
    };

    for (int i = 0; i < t1aTuples.length; i++) {
      ((LiteralTuples) t1a).appendTuple(t1aTuples[i]);
    }

    tuples = new StatementStoreDuplicateResolution(new boolean[] { true, true, true},
        t1a, new int[] { 0, 1, 2, 3});

    expectedTuples = new long[][] {
        new long[] { 3906, 3906, 3906 },
        new long[] { 3908, 3908, 3908 },
        new long[] { 3910, 3910, 3910 },
    };

    testTuples(expectedTuples, tuples);
  }

  /**
   * Test that we have the expected number and values of Tuples.
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
            "," + tuple[2],
            expectedTuples[index][0] == tuple[0] &&
            expectedTuples[index][1] == tuple[1] &&
            expectedTuples[index][2] == tuple[2]);
        index++;
        tuples.next();
      }
      while (index < expectedTuples.length);

      assertFalse("Should be no more result tuples", tuples.next());
    }
  }
}
