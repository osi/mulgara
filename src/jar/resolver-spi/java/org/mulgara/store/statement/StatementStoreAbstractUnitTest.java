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

package org.mulgara.store.statement;

// Java 2 standard packages
import java.util.*;

// third party packages
import junit.framework.*;
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.Variable;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.tuples.TestTuples;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.xa.XAStatementStore;

/**
 * Test case for {@link StatementStore} implementations.
 *
 * @created 2001-07-12
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:52 $
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
public abstract class StatementStoreAbstractUnitTest extends TestCase {

  /**
   * init the logging class
   */
  private static Logger log =
      Logger.getLogger(StatementStoreAbstractUnitTest.class.getName());

  /**
   * Subclasses must initialize this field.
   */
  protected XAStatementStore store;

  /**
   * CONSTRUCTOR GraphAbstractTest TO DO
   *
   * @param name PARAMETER TO DO
   */
  public StatementStoreAbstractUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain an empty test suite from, because this test
   * can't be run (it's abstract). This must be overridden in subclasses.
   *
   * @return The test suite
   */
  public static Test suite() {

    return new TestSuite();
  }

  //
  // Test cases
  //

  /**
   * Test {@link StatementStore#isEmpty}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testIsEmpty() throws Exception {

    try {

      assertTrue(!store.isEmpty());

      store.addTriple(1, 2, 3, 9);
      store.removeTriples(1, 2, 3, 1);
      store.removeTriples(1, 2, 4, 2);
      store.removeTriples(2, 5, 6, 2);

      assertTrue(!store.isEmpty());

      store.removeTriples(1, 2, 3, 9);

      assertTrue(store.isEmpty());
    }
    catch (UnsupportedOperationException e) {

      log.warn("IsEmpty method unsupported", e);
    }
  }

  /**
   * Test {@link StatementStore#existsTriples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testExists() throws Exception {

    try {

      long n = NodePool.NONE;

      assertTrue(store.existsTriples(1, 2, 3, n));
      assertTrue(store.existsTriples(1, 2, 4, n));
      assertTrue(store.existsTriples(2, 5, 6, n));
      assertTrue(!store.existsTriples(1, 3, 2, n));
      assertTrue(!store.existsTriples(9, 9, 9, n));

      assertTrue(store.existsTriples(1, 2, n, n));
      assertTrue(store.existsTriples(2, 5, n, n));
      assertTrue(!store.existsTriples(1, 3, n, n));
      assertTrue(!store.existsTriples(2, 9, n, n));

      assertTrue(store.existsTriples(n, 2, 3, n));
      assertTrue(store.existsTriples(n, 2, 4, n));
      assertTrue(!store.existsTriples(n, 1, 3, n));
      assertTrue(!store.existsTriples(n, 2, 9, n));

      assertTrue(store.existsTriples(1, n, 3, n));
      assertTrue(store.existsTriples(2, n, 6, n));
      assertTrue(!store.existsTriples(2, n, 4, n));
      assertTrue(!store.existsTriples(9, n, 3, n));

      assertTrue(store.existsTriples(1, n, n, n));
      assertTrue(store.existsTriples(2, n, n, n));
      assertTrue(!store.existsTriples(3, n, n, n));

      assertTrue(store.existsTriples(n, 2, n, n));
      assertTrue(store.existsTriples(n, 5, n, n));
      assertTrue(!store.existsTriples(n, 1, n, n));

      assertTrue(store.existsTriples(n, n, 3, n));
      assertTrue(store.existsTriples(n, n, 6, n));
      assertTrue(!store.existsTriples(n, n, 5, n));
    }
    catch (UnsupportedOperationException e) {

      log.warn("Exists method unsupported", e);
    }
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testDump() throws Exception {

    TestTuples expected = new TestTuples();
    add(expected, store.VARIABLES, new long[] {
        1, 2, 3, 1});
    add(expected, store.VARIABLES, new long[] {
        1, 2, 4, 2});
    add(expected, store.VARIABLES, new long[] {
        2, 5, 6, 2});

    Tuples t = store.findTuples(NodePool.NONE, NodePool.NONE, NodePool.NONE, NodePool.NONE);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#removeTriples}.
   *
   * @throws GraphException on attempted removal of nonexistent triple
   * @throws Exception EXCEPTION TO DO
   */
  public void testRemoveTriples() throws Exception {

    store.removeTriples(1, 2, 3, 1);
    store.removeTriples(2, 5, 6, 7); // Non-existent triple

    assertTrue(!store.existsTriples(1, 2, 3, 1));
    assertTrue(store.existsTriples(1, 2, 4, 2));
    assertTrue(store.existsTriples(2, 5, 6, 2));

    store.removeTriples(1, 3, 2, 4); // Non-existent triple

    store.removeTriples(2, 5, 6, 2);
    store.removeTriples(1, 2, 4, 2);

    assertTrue(!store.existsTriples(1, 2, 3, NodePool.NONE));
    assertTrue(!store.existsTriples(1, 2, 4, NodePool.NONE));
    assertTrue(!store.existsTriples(2, 5, 6, NodePool.NONE));
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode0() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars =
        new Variable[] {
        store.VARIABLES[1], store.VARIABLES[2], store.VARIABLES[3]};
    add(expected, vars, new long[] {
        2, 3, 1});
    add(expected, vars, new long[] {
        2, 4, 2});

    Tuples t = store.findTuples(1, NodePool.NONE, NodePool.NONE, NodePool.NONE);
    assertEquals(expected, t);
    t.close();
    expected.close();

    expected = new TestTuples();
    add(expected, vars, new long[] {
        5, 6, 2});

    t = store.findTuples(2, NodePool.NONE, NodePool.NONE, NodePool.NONE);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode1() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars =
        new Variable[] {
        store.VARIABLES[2], store.VARIABLES[0], store.VARIABLES[3]};
    add(expected, vars, new long[] {
        3, 1, 1});
    add(expected, vars, new long[] {
        4, 1, 2});

    Tuples t = store.findTuples(NodePool.NONE, 2, NodePool.NONE, NodePool.NONE);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode2() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars =
        new Variable[] {
        store.VARIABLES[0], store.VARIABLES[1], store.VARIABLES[3]};
    add(expected, vars, new long[] {
        1, 2, 1});

    Tuples t = store.findTuples(NodePool.NONE, NodePool.NONE, 3, NodePool.NONE);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode3() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars =
        new Variable[] {
        store.VARIABLES[0], store.VARIABLES[1], store.VARIABLES[2]};
    add(expected, vars, new long[] {
        1, 2, 4});
    add(expected, vars, new long[] {
        2, 5, 6});

    Tuples t = store.findTuples(NodePool.NONE, NodePool.NONE, NodePool.NONE, 2);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode01() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars = new Variable[] {
        store.VARIABLES[2], store.VARIABLES[3]};
    add(expected, vars, new long[] {
        3, 1});
    add(expected, vars, new long[] {
        4, 2});

    Tuples t = store.findTuples(1, 2, NodePool.NONE, NodePool.NONE);
    assertEquals(expected, t);
    t.close();

    t = store.findTuples(1, 3, NodePool.NONE, NodePool.NONE);
    assertTrue(!expected.equals(t));
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode02() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars = new Variable[] {
        store.VARIABLES[1], store.VARIABLES[3]};
    add(expected, vars, new long[] {
        2, 1});

    Tuples t = store.findTuples(1, NodePool.NONE, 3, NodePool.NONE);
    assertEquals(expected, t);
    t.close();

    t = store.findTuples(1, NodePool.NONE, 4, NodePool.NONE);
    assertTrue(!expected.equals(t));
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode03() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars = new Variable[] {
        store.VARIABLES[1], store.VARIABLES[2]};
    add(expected, vars, new long[] {
        2, 4});

    Tuples t = store.findTuples(1, NodePool.NONE, NodePool.NONE, 2);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode12() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars = new Variable[] {
        store.VARIABLES[0], store.VARIABLES[3]};
    add(expected, vars, new long[] {
        1, 1});

    Tuples t = store.findTuples(NodePool.NONE, 2, 3, NodePool.NONE);
    assertEquals(expected, t);
    t.close();

    t = store.findTuples(NodePool.NONE, 2, 4, NodePool.NONE);
    assertTrue(!expected.equals(t));
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode13() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars = new Variable[] {
        store.VARIABLES[2], store.VARIABLES[0]};
    add(expected, vars, new long[] {
        4, 1});

    Tuples t = store.findTuples(NodePool.NONE, 2, NodePool.NONE, 2);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode23() throws Exception {

    TestTuples expected = new TestTuples();
    Variable[] vars = new Variable[] {
        store.VARIABLES[0], store.VARIABLES[1]};
    add(expected, vars, new long[] {
        2, 5});

    Tuples t = store.findTuples(NodePool.NONE, NodePool.NONE, 6, 2);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode013() throws Exception {

    TestTuples expected = new TestTuples(store.VARIABLES[2]);
    Variable[] vars = new Variable[] {
        store.VARIABLES[2]};
    Tuples t = store.findTuples(2, 6, NodePool.NONE, 1);
    assertEquals(expected, t);
    t.close();

    t = store.findTuples(1, 2, NodePool.NONE, 4);
    assertEquals(expected, t);
    t.close();

    add(expected, vars, new long[] {
        4});
    t = store.findTuples(1, 2, NodePool.NONE, 2);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode023() throws Exception {

    TestTuples expected = new TestTuples(store.VARIABLES[1]);
    Variable[] vars = new Variable[] {
        store.VARIABLES[1]};
    Tuples t = store.findTuples(1, NodePool.NONE, 3, 4);
    assertEquals(expected, t);
    t.close();

    add(expected, vars, new long[] {
        5});
    t = store.findTuples(2, NodePool.NONE, 6, 2);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Test {@link StatementStore#findTuples}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void testFindTriplesByNode123() throws Exception {

    TestTuples expected = new TestTuples(store.VARIABLES[0]);
    Variable[] vars = new Variable[] {
        store.VARIABLES[0]};
    Tuples t = store.findTuples(NodePool.NONE, 2, 3, 4);
    assertEquals(expected, t);
    t.close();

    add(expected, vars, new long[] {
        1});
    t = store.findTuples(NodePool.NONE, 2, 3, 1);
    assertEquals(expected, t);
    t.close();
    expected.close();
  }

  /**
   * Populate the test store.
   *
   * @throws Exception EXCEPTION TO DO
   */
  protected void setUp() throws Exception {

    store.addTriple(1, 2, 3, 1);
    store.addTriple(1, 2, 4, 2);
    store.addTriple(2, 5, 6, 2);
  }

  /**
   * Close the test store.
   *
   * @throws Exception EXCEPTION TO DO
   */
  protected void tearDown() throws Exception {

    if (store != null) {

      try {

        store.close();
      }
      finally {

        store = null;
      }
    }
  }

  /**
   * METHOD TO DO
   *
   * @param tt PARAMETER TO DO
   * @param vars PARAMETER TO DO
   * @param nodes PARAMETER TO DO
   */
  protected void add(TestTuples tt, Variable[] vars, long[] nodes) {

    if (vars.length != nodes.length) {

      throw new AssertionError();
    }

    for (int i = 0; i < vars.length; ++i) {

      if (i == 0) {

        tt.or(vars[i], nodes[i]);
      }
      else {

        tt.and(vars[i], nodes[i]);
      }
    }
  }
}
