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

// Standard Java
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

// logging and unit testing packages
import junit.framework.TestSuite;
import org.apache.log4j.Logger;

// Jena packages
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.TripleMatch;
import com.hp.hpl.jena.graph.impl.GraphBase;
import com.hp.hpl.jena.graph.query.BindingQueryPlan;
import com.hp.hpl.jena.graph.query.Domain;
import com.hp.hpl.jena.graph.query.ExpressionSet;
import com.hp.hpl.jena.graph.query.Mapping;
import com.hp.hpl.jena.graph.query.NamedGraphMap;
import com.hp.hpl.jena.graph.query.Query;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.graph.query.Stage;
import com.hp.hpl.jena.graph.query.test.AbstractTestQuery;
import com.hp.hpl.jena.mem.GraphMem;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.iterator.ClosableIterator;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.util.iterator.NiceIterator;

// local packages
import org.kowari.server.SessionFactory;
import org.kowari.server.driver.SessionFactoryFinder;


/**
 * Test case for {@link AbstractTestQuery}.
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
public class KowariQueryHandlerUnitTest extends AbstractTestQuery {

  /**
   * init the logging class
   */
  private static Logger logger =
      Logger.getLogger(GraphKowariUnitTest.class.getName());

  /**
   * The URI of the server.
   */
  protected URI serverURI;

  /**
   * The URI of the graph.
   */
  protected URI graphURITest;

  /**
   * The name of the test model.
   */
  protected final static String SERVER_NAME = "server1";

  /**
   * The session.
   */
  protected LocalJenaSession sessionTest = null;

  /**
   * Creates new graphs.
   */
  protected GraphKowariMaker graphMaker;

  public KowariQueryHandlerUnitTest(String name) {
    super(name);
    //Logger.getRootLogger().setLevel(Level.ERROR);
  }

  public static TestSuite suite() {
//    return new TestSuite(KowariQueryHandlerUnitTest.class);
    TestSuite result = new TestSuite();
    result.addTest(new KowariQueryHandlerUnitTest("testAtomicTreeQuery"));
    result.addTest(new KowariQueryHandlerUnitTest("testBinding1"));
    result.addTest(new KowariQueryHandlerUnitTest("testBinding2"));
    result.addTest(new KowariQueryHandlerUnitTest("testBindingQuery"));
    result.addTest(new KowariQueryHandlerUnitTest("testChainedTreeQuery"));
    result.addTest(new KowariQueryHandlerUnitTest("testCloseQuery"));
    result.addTest(new KowariQueryHandlerUnitTest("testCompositeTreeQuery"));
    result.addTest(new KowariQueryHandlerUnitTest("testChainedTreeQuery"));

    // Does not handle constraints.
////    result.addTest(new KowariQueryHandlerUnitTest("testConstraint"));
////    result.addTest(new KowariQueryHandlerUnitTest("testConstraintFour"));
////    result.addTest(new KowariQueryHandlerUnitTest("testConstraintThree"));

    result.addTest(new KowariQueryHandlerUnitTest("testDisconnected"));
    result.addTest(new KowariQueryHandlerUnitTest("testEmpty"));
    result.addTest(new KowariQueryHandlerUnitTest("testEmptyIterator"));
    result.addTest(new KowariQueryHandlerUnitTest("testFor"));

    // Does not handle constraints.
////    result.addTest(new KowariQueryHandlerUnitTest("testExtractConstraints"));
////    result.addTest(new KowariQueryHandlerUnitTest("testGraphConstraints"));

    result.addTest(new KowariQueryHandlerUnitTest("testGraphQuery"));
    result.addTest(new KowariQueryHandlerUnitTest("testManyThings"));

    // Does not handle constraints.
////    result.addTest(new KowariQueryHandlerUnitTest("testMatchConstraints"));
////    result.addTest(new KowariQueryHandlerUnitTest("testExtractConstraint"));

    result.addTest(new KowariQueryHandlerUnitTest("testMissingVariable"));
    result.addTest(new KowariQueryHandlerUnitTest("testMultiplePatterns"));
    result.addTest(new KowariQueryHandlerUnitTest("testNodeVariablesA"));
    result.addTest(new KowariQueryHandlerUnitTest("testNodeVariablesB"));
    result.addTest(new KowariQueryHandlerUnitTest("testOneMatch"));

    // Does not handle constraints.
////    result.addTest(new KowariQueryHandlerUnitTest("testQueryConstraintUnbound"));

    result.addTest(new KowariQueryHandlerUnitTest("testQueryOptimisation"));
    result.addTest(new KowariQueryHandlerUnitTest("testQueryTripleOrder"));
    result.addTest(new KowariQueryHandlerUnitTest("testStringResults"));

    // Does not handle constraints.
////    result.addTest(new KowariQueryHandlerUnitTest("testTripleSorting"));

    result.addTest(new KowariQueryHandlerUnitTest("testTwoGraphs"));
    result.addTest(new KowariQueryHandlerUnitTest("testTwoPatterns"));

    // These should work.
//    result.addTest(new KowariQueryHandlerUnitTest("testVariableCount"));
    result.addTest(new KowariQueryHandlerUnitTest("testXXXMatch1"));
    result.addTest(new KowariQueryHandlerUnitTest("testXXXMatch3"));

    return result;
  }

  public void testBinding1() {
    Graph single = getGraphWith("rice grows quickly");
    KowariQuery q = new KowariQuery();
    Node V1 = node("?v1"), V3 = node("?v3");
    BindingQueryPlan qp = single.queryHandler().prepareBindings(q.addMatch(V1,
        node("grows"), V3), new Node[] {
        V1, V3});
    Domain binding = (Domain) qp.executeBindings().next();
    assertEquals("binding subject to rice", binding.get(0), node("rice"));
    assertEquals("binding object to quickly", binding.get(1), node("quickly"));
  }

  public void testBinding2() {
    Graph several = getGraphWith("rice grows quickly; time isan illusion");
    String[][] answers = {
        {
        "time", "isan", "illusion"}
        , {
        "rice", "grows", "quickly"}
    };
    boolean[] found = {
        false, false};
    Query q = new KowariQuery();
    Node V1 = node("?v1"), V2 = node("?v2"), V3 = node("?v3");
    BindingQueryPlan qp =
        several.queryHandler().prepareBindings(
        q.addMatch(V1, V2, V3),
        new Node[] {
        V1, V2, V3});
    Iterator bindings = qp.executeBindings();
    for (int i = 0; i < answers.length; i += 1) {
      if (bindings.hasNext() == false)
        fail("wanted some more results");
      Domain bound = (Domain) bindings.next();
      for (int k = 0; k < answers.length; k++) {
        if (found[k])
          continue;
        boolean match = true;
        for (int j = 0; j < 3; j += 1) {
          if (!bound.get(j).equals(node(answers[k][j]))) {
            match = false;
            break;
          }
        }
        if (match) {
          found[k] = true;
          break;
        }
      }
    }
    for (int k = 0; k < answers.length; k++) {
      if (!found[k])
        assertTrue("binding failure", false);
    }
    assertFalse("iterator should be empty", bindings.hasNext());
  }

  public void testBindingQuery() {
    Graph empty = getGraphWith("");
    Graph base = getGraphWith(
        "pigs might fly; cats chase mice; dogs chase cars; cats might purr");
    /* */
    Query any = new KowariQuery().addMatch(Query.ANY, Query.ANY, Query.ANY);
    assertFalse("empty graph, no bindings", eb(empty, any, none).hasNext());
    assertTrue("full graph, > 0 bindings", eb(base, new KowariQuery(), none).hasNext());
  }

  public void testChainedTreeQuery() {
    testTreeQuery("a pings b; b pings c; c pings d", "a pings b; b pings c",
        "a pings b; b pings c");
  }

  private void testTreeQuery(String content, String pattern, String answer) {
    testTreeQuery("checking", content, pattern, answer);
  }

  public void testCloseQuery() {

    // Insert data
    Graph g = getGraphWith("x R y; a P b; i L j; d X f; h S g; no more heroes");

    // Add with transactions to speed up test.
    g.getTransactionHandler().begin();
    for (int n = 0; n < 1000; n += 1) graphAdd(g, "ping pong X" + n);
    g.getTransactionHandler().commit();

    Query q = new KowariQuery().addMatch(Query.S, Query.P, Query.O);
    List stages = new ArrayList();
    ExtendedIterator it = eb(g, q, nodes("?P"));
    /* eat one answer to poke pipe */

    it.next();
    for (int i = 0; i < stages.size(); i += 1) assertFalse(((Stage) stages.get(
        i)).isClosed());
    it.close();
    for (int i = 0; i < stages.size(); i += 1) assertTrue(((Stage) stages.get(i)).
        isClosed());
  }

  public void testDisconnected() {
    Graph g = getGraphWith("x pred1 foo; y pred2 bar");
    Query q = new KowariQuery(getGraphWithMem("?X ?? foo; ?Y ?? bar"));
    List bindings = ebList(g, q, nodes("?X ?Y"));
    assertEquals(1, bindings.size());
    assertEquals(node("x"), ((List) bindings.get(0)).get(0));
    assertEquals(node("y"), ((List) bindings.get(0)).get(1));
  }

  public void testEmpty() {
    List bindings = ebList(empty, Q, none);
    assertEquals("testEmpty: select [] from {} => 1 empty binding [size]",
        bindings.size(), 1);
    Domain d = (Domain) bindings.get(0);
    assertEquals("testEmpty: select [] from {} => 1 empty binding [width]",
        d.size(), 0);
  }

  public void testEmptyIterator() {
    Graph empty = getGraph();
    Query q = new KowariQuery().addMatch(X, Y, Z);
    BindingQueryPlan bqp = empty.queryHandler().prepareBindings(q, justX);
    try {
      bqp.executeBindings().next();
      fail("there are no bindings; next() should fail");
    }
    catch (NoSuchElementException e) {
      pass();
    }
  }

  /**
   * Tests query handler subjectsFor and objectFor.
   */
  public void testFor() {
    Graph bookish = getGraphWith("urn:ben urn:wrote urn:Clayface; " +
        "urn:Starfish urn:ingenre urn:SF; " +
        "urn:Clayface urn:ingenre urn:Geology; " +
        "urn:bill urn:wrote urn:Starfish");
    ModelKowari model = new ModelKowari((GraphKowari) bookish);
    QueryHandler handler = model.queryHandler();

    // Expected result.
    Resource subject1 = model.getResource("urn:ben");
    Resource subject2 = model.getResource("urn:bill");
    Property predicate1 = model.getProperty("urn:wrote");
    Property predicate2 = model.getProperty("urn:ingenre");
    Resource object1 = model.getResource("urn:Clayface");
    Resource object2 = model.getResource("urn:Geology");
    Resource object3 = model.getResource("urn:SF");

    // Do a search for subjects with a fixed predicate and object.
    ExtendedIterator iter = handler.subjectsFor(predicate1.getNode(),
        object1.getNode());
    assertTrue("Should have a result", iter.hasNext());
    assertEquals("Should correct subject", subject1.getNode(),
        iter.next());

    // Do a search for subjects with only a fixed predicate.
    iter = handler.subjectsFor(predicate1.getNode(), null);
    assertTrue("Should have a result", iter.hasNext());
    boolean correct;
    Node resultNode = (Node) iter.next();
    correct = ((subject1.getNode().equals(resultNode)) ||
        subject2.getNode().equals(resultNode));
    assertTrue("Should have either one of two nodes result", correct);
    resultNode = (Node) iter.next();
    correct = ((subject1.getNode().equals(resultNode)) ||
        subject2.getNode().equals(resultNode));
    assertTrue("Should have either one of two nodes result", correct);

    // Do a search for objects with a fixed subject and predicate.
    iter = handler.objectsFor(object1.getNode(), predicate2.getNode());
    assertTrue("Should have a result", iter.hasNext());
    assertEquals("Should correct subject", object2.getNode(),
        iter.next());

    // Do a search for objects with a fixed predicate.
    iter = handler.objectsFor(null, predicate2.getNode());
    resultNode = (Node) iter.next();
    correct = ((object2.getNode().equals(resultNode)) ||
        object3.getNode().equals(resultNode));
    assertTrue("Should have either one of two nodes result", correct);
    resultNode = (Node) iter.next();
    correct = ((object2.getNode().equals(resultNode)) ||
        object3.getNode().equals(resultNode));
    assertTrue("Should have either one of two nodes result", correct);
  }

  public void testGraphQuery() {
    Graph pattern = getGraphWithMem("?X reads ?Y; ?Y inGenre ?Z");
    Graph target = getGraphWith("chris reads blish; blish inGenre SF");
    // System.err.println( "| pattern: " + pattern );
    Query q = new KowariQuery(pattern);
    List bindings = ebList(target, q, new Node[] {
        node("?X"), node("?Z")});
    assertEquals("testTwoPatterns: one binding", 1, bindings.size());
    Domain d = (Domain) bindings.get(0);
    // System.out.println( "* width = " + d.width() );
    assertTrue("testTwoPatterns: width 2", d.size() >= 2);
    assertEquals("testTwoPatterns: X = chris", d.get(0), node("chris"));
    assertEquals("testTwoPatterns: Y = SF", d.get(1), node("SF"));
  }

  private static final String[][] tests = {
      { "", "pigs might fly", "", ""},
      { "", "", "pigs might fly", ""},
      { "", "a pings b; b pings c", "a pings _x; _x pings c", "a pings b; b pings c"},
      { "", "a pings b; b pings c; a pings x; x pings c", "a pings _x; _x pings c", "a pings b; b pings c; a pings x; x pings c"}
  };

  public void testManyThings() {
    for (int i = 0; i < tests.length; i += 1)
      testTreeQuery(tests[i][0], tests[i][1], tests[i][2], tests[i][3]);
  }

  private void testTreeQuery(String title, String content, String pattern,
      String correct) {
    Graph gc = getGraphWith(content), gp = getGraphWith(pattern);
    Graph answer = gc.queryHandler().prepareTree(gp).executeTree();
    if (title.equals("")) title = "checking {" + content + "} against {" +
        pattern + "} should give {" + correct + "}" + " not " + answer;
    assertIsomorphic(title, getGraphWith(correct), answer);
  }

  public void testMissingVariable() {
    Graph g = getGraphWith("x y z");
    List bindings = ebList(g, Q, new Node[] {
        X, Y});
    List L = (List) bindings.get(0);
    assertEquals("undefined variables get null", null, L.get(0));
  }

  public void testMultiplePatterns() {
    Graph bookish = getGraphWith("ben wrote Clayface; Starfish ingenre SF; Clayface ingenre Geology; bill wrote Starfish");
    Query q = new KowariQuery();
    Node A = node("?A");
    q.addMatch(X, node("wrote"), A).addMatch(A, node("ingenre"), node("SF"));
    BindingQueryPlan qp = bookish.queryHandler().prepareBindings(q, justX);
    Iterator bindings = qp.executeBindings();
    if (bindings.hasNext()) {
      Domain it = (Domain) bindings.next();
      if (it.size() > 0) {
        if (it.get(0).equals(node("bill"))) {
          if (bindings.hasNext())
            System.out.println(
                "! failed: more than one multiple pattern answer: " +
                bindings.next());
        }
        else
          System.out.println(
              "! failed: multiple pattern answer should be 'bill'");
      }
      else
        System.out.println(
            "! failed: multiple pattern answer should have one element");
    }
    else
      System.out.println(
          "! failed: multiple pattern query should have an answer");
  }

  public void testNodeVariablesA() {
    Graph mine = getGraphWith("storms hit England");
    Node spoo = node("?spoo");
    Query q = new KowariQuery().addMatch(spoo, node("hit"), node("England"));
    ClosableIterator it = eb(mine, q, new Node[] {
        spoo});
    assertTrue("tnv: it has a solution", it.hasNext());
    assertEquals("", node("storms"), ((List) it.next()).get(0));
    assertFalse("tnv: just the one solution", it.hasNext());
  }

  public void testNodeVariablesB() {
    Graph mine = getGraphWith("storms hit England");
    Node spoo = node("?spoo"), flarn = node("?flarn");
    Query q = new KowariQuery().addMatch(spoo, node("hit"), flarn);
    ClosableIterator it = eb(mine, q, new Node[] {
        flarn, spoo});
    assertTrue("tnv: it has a solution", it.hasNext());
    List answer = (List) it.next();
    assertEquals("tnvB", node("storms"), answer.get(1));
    assertEquals("tnvB", node("England"), answer.get(0));
    assertFalse("tnv: just the one solution", it.hasNext());
  }

  public void testOneMatch() {
    Query q = new KowariQuery().addMatch(X, Query.ANY, Query.ANY);
    List bindings = ebList(single, q, justX);
    assertEquals("select X from {spindizzies drive cities} => 1 binding [size]",
        bindings.size(), 1);
    Domain d = (Domain) bindings.get(0);
    assertEquals(
        "select X from {spindizzies drive cities} => 1 binding [width]", d.size(),
        1);
    assertTrue("select X from {spindizzies drive cities} => 1 binding [value]",
        d.get(0).equals(node("spindizzies")));
  }

  public void testQueryTripleOrder() {
    Triple t1 = Triple.create("A B C"), t2 = Triple.create("D E F");
    List desired = Arrays.asList(new Triple[] {
        t1, t2});
    List obtained = getTriplesFromQuery(desired);
    assertEquals(desired, obtained);
  }

  public void testStringResults() {
    Graph g = getGraphWith("ding dong dilly");
    Query q = new KowariQuery().addMatch(X, Y, Query.ANY);
    List bindings = ebList(g, q, new Node[] {
        X, Y});
    assertEquals("one result back by name", bindings.size(), 1);
    assertEquals("x = ding", ((Domain) bindings.get(0)).get(0), node("ding"));
  }

  public void testTwoGraphs() {
    Graph a = getGraphWith(
        "chris reads blish; chris reads norton; chris eats curry");
    Graph b = getGraphWith("blish inGenre SF; curry inGenre food");
    Node reads = node("reads"), inGenre = node("inGenre");
    Query q = new KowariQuery().addMatch("a", X, reads, Y).addMatch("b", Y, inGenre, Z);
    NamedGraphMap args = q.args().put("a", a).put("b", b);
    List bindings = iteratorToList(q.executeBindings(args, new Node[] {
        X, Z})); // TODO
    assertEquals("testTwoGraphs: one binding", bindings.size(), 1);
    Domain d = (Domain) bindings.get(0);
    assertTrue("testTwoGraphs: width 2", d.size() >= 2);
    assertEquals("testTwoGraphs: X = chris", node("chris"), d.get(0));
    assertEquals("testTwoGraphs: Z = SF", node("SF"), d.get(1));
  }

  public void testTwoPatterns() {
    Node reads = node("reads"), inGenre = node("inGenre");
    Graph g = getGraphWith("chris reads blish; blish inGenre SF");
    // System.err.println( "| X = " + X + ", Y = " + Y + ", Z = " + Z );
    Q.addMatch(X, reads, Y);
    Q.addMatch(Y, inGenre, Z);
    List bindings = ebList(g, Q, new Node[] {
        X, Z});
    assertTrue("testTwoPatterns: one binding", bindings.size() == 1);
    Domain d = (Domain) bindings.get(0);
    // System.out.println( "* width = " + d.width() );
    assertTrue("testTwoPatterns: width 2", d.size() >= 2);
    assertEquals("testTwoPatterns: X = chris", d.get(0), node("chris"));
    assertEquals("testTwoPatterns: Y = SF", d.get(1), node("SF"));
  }

  public void testVariableCount() {
    super.testVariableCount();
  }

  // For testVariableCount
  public void assertCount(int expected, String query, String vars) {
    Graph g = getGraphWith("");
    Query q = new KowariQuery();
    Triple[] triples = tripleArray(query);
    for (int i = 0; i < triples.length; i += 1) q.addMatch(triples[i]);
    // eb( g, q, nodes( vars ) );
    q.executeBindings(g, nodes(vars));
    assertEquals(expected, q.getVariableCount());
  }

  public void testXXXMatch1() {
    Q.addMatch(X, X, X);
    Graph xxx = getGraphWith("ring ring ring");
    List bindings = ebList(xxx, Q, justX);
    assertEquals("bindings match (X X X)", bindings.size(), 1);
  }

  public void testXXXMatch3() {
    Q.addMatch(X, X, X);
    Graph xxx = getGraphWith("ring ring ring; ding ding ding; ping ping ping;");
    List bindings = ebList(xxx, Q, justX);
    assertEquals("bindings match (X X X)", 3, bindings.size());
    /* */
    HashSet found = new HashSet();
    for (int i = 0; i < bindings.size(); i += 1) {
      Domain d = (Domain) bindings.get(i);
      assertEquals("one bound variable", 1, d.size());
      found.add(d.get(0));
    }
    Set wanted = nodeSet("ring ding ping");
    assertEquals("testMatch getting {ring ding ping}", found, wanted);
  }

  private List getTriplesFromQuery(List desired) {
    Query q = new KowariQuery();
    final Triple[][] tripleses = new Triple[1][];

    final Graph g = new GraphBase() {
      public ExtendedIterator find(TripleMatch tm) {
        return new NiceIterator();
      }

      public QueryHandler queryHandler() {
        return new SimpleQueryHandler(this) {
          public Stage patternStage(Mapping map, ExpressionSet constraints,
              Triple[] t) {
            if (t.length > 1) tripleses[0] = t;
            return super.patternStage(map, constraints, t);
          }
        };
      }
    };
    for (int i = 0; i < desired.size(); i += 1) q.addMatch((Triple) desired.get(
        i));
    eb(g, q, none);
    return Arrays.asList(tripleses[0]);
  }

  public Graph getGraphWithMem(String facts) {
    return graphAdd(getGraphMem(), facts);
  }

  public Graph getGraphMem() {
    return new GraphMem();
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) throws Exception {

    junit.textui.TestRunner.run(suite());
  }

  public void setUp() {

    boolean exceptionOccurred = true;

    try {
      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);

      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverURI, false);
      sessionTest = (LocalJenaSession) sessionFactory.newJenaSession();

      graphMaker = new GraphKowariMaker((LocalJenaSession) sessionTest, serverURI,
          ReificationStyle.Minimal);

      Q = new KowariQuery();
      empty = getGraphWith("");
      single = getGraphWith("spindizzies drive cities");

      exceptionOccurred = false;
    }
    catch (Exception e) {
      logger.error("Error in database setup", e);
    }
    finally {
      if (exceptionOccurred)
        tearDown();
    }
  }

  public void tearDown() {

    try {
      graphMaker.removeAll();
      graphMaker.close();
    }
    catch (Exception e) {
      graphMaker = null;
    }
    finally {

      try {
        sessionTest.close();
      }
      catch (Exception e) {}
    }
  }

  public Graph getGraph() {
    try {
      return graphMaker.createGraph();
//      return new GraphMem();
    }
    catch (Exception e) {
      logger.error("Error obtaining graph", e);
      return null;
    }
  }
}
