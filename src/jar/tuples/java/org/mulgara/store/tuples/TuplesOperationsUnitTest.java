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

// JUnit
import junit.framework.*; // JUnit

// Log4J
import org.apache.log4j.Logger;

// Mulgara
import org.mulgara.query.Variable;

/**
 * Test case for {@link TuplesOperationsUnitTest}.
 *
 * @created 2003-07-11
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:10 $
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
public class TuplesOperationsUnitTest extends TestCase {

  /**
   * Logger.
   */
  private Logger logger =
      Logger.getLogger(TuplesOperationsUnitTest.class.getName());

  /**
   * Constructs a new test with the given name.
   *
   * @param name the name of the test
   */
  public TuplesOperationsUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   *
   * @return The test suite
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(new TuplesOperationsUnitTest("testReorderedAppend"));

    return suite;
  }

  /**
   * Create test instance.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void setUp() throws Exception {

    // null implementation
  }

  //
  // Test cases
  //

  /**
   * Test {@link TuplesOperations}.
   *
   * @throws Exception if query fails when it should have succeeded
   */
  public void testReorderedAppend() throws Exception {
    LiteralTuples lhs = new LiteralTuples(new String[] {"x"}, true);
    LiteralTuples rhs1 = new LiteralTuples(new String[] {"x", "y"}, true);
    LiteralTuples rhs2 = new LiteralTuples(new String[] {"y", "x"}, true);

    lhs.appendTuple(new long[] { 1 });
    lhs.appendTuple(new long[] { 6 });

    rhs1.appendTuple(new long[] { 1, 2 });
    rhs1.appendTuple(new long[] { 1, 3 });
    rhs1.appendTuple(new long[] { 4, 1 });

    rhs2.appendTuple(new long[] { 5, 1 });
    rhs2.appendTuple(new long[] { 6, 1 });
    rhs2.appendTuple(new long[] { 1, 7 });

    Tuples append = TuplesOperations.append(rhs1, rhs2);
    Tuples join = TuplesOperations.join(lhs, append);
    append.close();

    logger.warn("join - " + TuplesOperations.formatTuplesTree(join));

    Variable[] vars = join.getVariables();
    assertEquals(2, vars.length);
    assertEquals(new Variable("x"), vars[0]);
    assertEquals(new Variable("y"), vars[1]);

    join.beforeFirst();

    TuplesTestingUtil.testTuplesRow(join, new long[] { 1, 2 });
    TuplesTestingUtil.testTuplesRow(join, new long[] { 1, 3 });
    TuplesTestingUtil.testTuplesRow(join, new long[] { 1, 5 });
    TuplesTestingUtil.testTuplesRow(join, new long[] { 1, 6 });

    assertFalse(join.next());

    TuplesTestingUtil.closeTuples(new Tuples[] { join });
  }
}
