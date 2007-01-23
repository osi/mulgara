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
 * Contributor(s):
 *   All tests in this file (c) Netymon Pty Ltd 2006 All rights reserved.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver;

// Java 2 standard packages
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

// Third party packages
import junit.framework.*;        // JUnit
import org.apache.log4j.Logger;  // Log4J
import org.jrdf.vocabulary.RDF;  // JRDF
import org.jrdf.graph.SubjectNode;  // JRDF
import org.jrdf.graph.PredicateNode;  // JRDF
import org.jrdf.graph.ObjectNode;  // JRDF

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.query.rdf.TripleImpl;
import org.mulgara.server.Session;
import org.mulgara.store.StoreException;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.stringpool.StringPool;
import org.mulgara.util.FileUtil;

/**
* Test case for {@link DatabaseSession}.
*
* @created 2006-11-20
* @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
* @company <a href="mailto:mail@netymon.com">Netymon Pty Ltd</a>
* @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
*      Software Pty Ltd</a>
* @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
*/
public class AdvDatabaseSessionUnitTest extends TestCase
{
  /** Logger.  */
  private static Logger logger =
    Logger.getLogger(AdvDatabaseSessionUnitTest.class.getName());

  private static final URI databaseURI;

  private static final URI systemModelURI;

  private static final URI modelURI;
  private static final URI model2URI;
  private static final URI model3URI;

  static {
    try {
      databaseURI    = new URI("local:database");
      systemModelURI = new URI("local:database#");
      modelURI       = new URI("local:database#model");
      model2URI      = new URI("local:database#model2");
      model3URI      = new URI("local:database#model3");
    } catch (URISyntaxException e) {
      throw new Error("Bad hardcoded URI", e);
    }
  }

  private static Database database = null;

  public AdvDatabaseSessionUnitTest(String name)
  {
    super(name);
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new AdvDatabaseSessionUnitTest("testSetModel"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testBasicQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testConcurrentQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testSubqueryQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testConcurrentSubqueryQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testConcurrentReadWrite"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testExplicitBasicQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testExplicitIsolationQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testExplicitRollbackIsolationQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testExplicitCommitIsolationQuery"));
    suite.addTest(new AdvDatabaseSessionUnitTest("testDatabaseDelete"));

    return suite;
  }

  /**
   * Create test objects.
   */
  public void setUp() throws Exception {
    if (database == null) {
      // Create the persistence directory
      File persistenceDirectory =
        new File(new File(System.getProperty("cvs.root")), "testDatabase");
      if (persistenceDirectory.isDirectory()) {
        if (!FileUtil.deleteDirectory(persistenceDirectory)) {
          throw new RuntimeException(
            "Unable to remove old directory " + persistenceDirectory
          );
        }
      }
      if (!persistenceDirectory.mkdirs()) {
        throw new Exception("Unable to create directory "+persistenceDirectory);
      }

      // Define the the node pool factory
      String nodePoolFactoryClassName =
        "org.mulgara.store.nodepool.xa.XANodePoolFactory";

      // Define the string pool factory
      String stringPoolFactoryClassName =
        "org.mulgara.store.stringpool.xa.XAStringPoolFactory";

      String tempNodePoolFactoryClassName =
        "org.mulgara.store.nodepool.memory.MemoryNodePoolFactory";

      // Define the string pool factory
      String tempStringPoolFactoryClassName =
        "org.mulgara.store.stringpool.memory.MemoryStringPoolFactory";

      // Define the resolver factory used to manage system models
      String systemResolverFactoryClassName =
        "org.mulgara.resolver.store.StatementStoreResolverFactory";

      // Define the resolver factory used to manage system models
      String tempResolverFactoryClassName =
        "org.mulgara.resolver.memory.MemoryResolverFactory";

      String ruleLoaderFactoryClassName =
        "org.mulgara.rules.RuleLoaderFactory";

      // Create a database which keeps its system models on the Java heap
      database = new Database(
                   databaseURI,
                   persistenceDirectory,
                   null,                            // no security domain
                   new JotmTransactionManagerFactory(),
                   0,                               // default transaction timeout
                   nodePoolFactoryClassName,        // persistent
                   new File(persistenceDirectory, "xaNodePool"),
                   stringPoolFactoryClassName,      // persistent
                   new File(persistenceDirectory, "xaStringPool"),
                   systemResolverFactoryClassName,  // persistent
                   new File(persistenceDirectory, "xaStatementStore"),
                   tempNodePoolFactoryClassName,    // temporary nodes
                   null,                            // no dir for temp nodes
                   tempStringPoolFactoryClassName,  // temporary strings
                   null,                            // no dir for temp strings
                   tempResolverFactoryClassName,    // temporary models
                   null,                            // no dir for temp models
                   "",                              // no rule loader
                   "org.mulgara.content.rdfxml.RDFXMLContentHandler");

      database.addResolverFactory("org.mulgara.resolver.url.URLResolverFactory", null);
    }
  }


  /**
  * The teardown method for JUnit
  */
  public void tearDown()
  {
  }

  //
  // Test cases
  //

  /**
  * Test the {@link DatabaseSession#setModel} method.
  */
  public void testSetModel() throws URISyntaxException
  {
    logger.info("testSetModel");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      // Load some test data
      Session session = database.newSession();
      try {
        session.createModel(modelURI, null);
        session.setModel(modelURI, new ModelResource(fileURI));
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  public void testBasicQuery() throws URISyntaxException {
    logger.info("Testing basicQuery");

    try {
      // Load some test data
      Session session = database.newSession();
      try {
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(predicateVariable);
        selectList.add(objectVariable);

        // Evaluate the query
        Answer answer = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Arrays.asList(new Order[] {                       // ORDER BY
            new Order(subjectVariable, true),
            new Order(predicateVariable, true),
            new Order(objectVariable, true)
          }),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));
        String[][] results = {
          { "test:s01", "test:p01", "test:o01" },
          { "test:s01", "test:p02", "test:o01" },
          { "test:s01", "test:p02", "test:o02" },
          { "test:s01", "test:p03", "test:o02" },
          { "test:s02", "test:p03", "test:o02" },
          { "test:s02", "test:p04", "test:o02" },
          { "test:s02", "test:p04", "test:o03" },
          { "test:s02", "test:p05", "test:o03" },
          { "test:s03", "test:p01", "test:o01" },
          { "test:s03", "test:p05", "test:o03" },
          { "test:s03", "test:p06", "test:o01" },
          { "test:s03", "test:p06", "test:o03" },
        };
        compareResults(results, answer);
        answer.close();
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  
  public void testConcurrentQuery() throws URISyntaxException {
    logger.info("Testing concurrentQuery");

    try {
      // Load some test data
      Session session = database.newSession();
      try {
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(predicateVariable);
        selectList.add(objectVariable);

        // Evaluate the query
        Answer answer1 = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Collections.singletonList(                        // ORDER BY
            new Order(subjectVariable, true)
          ),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));

        Answer answer2 = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Collections.singletonList(                        // ORDER BY
            new Order(subjectVariable, true)
          ),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));

        compareResults(answer1, answer2);

        answer1.close();
        answer2.close();
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  
  public void testSubqueryQuery() throws URISyntaxException {
    logger.info("Testing subqueryQuery");

    try {
      // Load some test data
      Session session = database.newSession();
      try {
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(new Subquery(new Variable("k0"), new Query(
          Collections.singletonList(objectVariable),
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Collections.singletonList(                        // ORDER BY
            new Order(objectVariable, true)
          ),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        )));


        // Evaluate the query
        Answer answer = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
              new URIReferenceImpl(new URI("test:p03")),
              objectVariable),
          null,                                             // HAVING
          Collections.singletonList(                        // ORDER BY
            new Order(subjectVariable, true)
          ),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));

        answer.beforeFirst();

        assertTrue(answer.next());
        assertEquals(new URIReferenceImpl(new URI("test:s01")),
            answer.getObject(0));
        Answer sub1 = (Answer)answer.getObject(1);
        compareResults(new String[][] { new String[] { "test:o01" },
                                        new String[] { "test:o02" } }, sub1);
        sub1.close();

        assertTrue(answer.next());
        assertEquals(new URIReferenceImpl(new URI("test:s02")),
            answer.getObject(0));
        Answer sub2 = (Answer)answer.getObject(1);
        compareResults(new String[][] { new String[] { "test:o02" },
                                        new String[] { "test:o03" } }, sub2);
        // Leave sub2 open.

        assertFalse(answer.next());
        answer.close();
        sub2.close();
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  
  public void testConcurrentSubqueryQuery() throws URISyntaxException {
    logger.info("Testing concurrentSubqueryQuery");

    try {
      // Load some test data
      Session session = database.newSession();
      try {
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(new Subquery(new Variable("k0"), new Query(
          Collections.singletonList(objectVariable),
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Collections.singletonList(                        // ORDER BY
            new Order(objectVariable, true)
          ),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        )));


        // Evaluate the query
        Answer answer = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
              new URIReferenceImpl(new URI("test:p03")),
              objectVariable),
          null,                                             // HAVING
          Collections.singletonList(                        // ORDER BY
            new Order(subjectVariable, true)
          ),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));

        answer.beforeFirst();

        assertTrue(answer.next());
        assertEquals(new URIReferenceImpl(new URI("test:s01")),
            answer.getObject(0));
        Answer sub1 = (Answer)answer.getObject(1);
        assertTrue(answer.next());
        assertEquals(new URIReferenceImpl(new URI("test:s02")),
            answer.getObject(0));
        Answer sub2 = (Answer)answer.getObject(1);
        assertFalse(answer.next());

        assertEquals(1, sub1.getNumberOfVariables());
        assertEquals(1, sub2.getNumberOfVariables());
        sub1.beforeFirst();
        sub2.beforeFirst();
        assertTrue(sub1.next());
        assertTrue(sub2.next());
        assertEquals(new URIReferenceImpl(new URI("test:o01")), sub1.getObject(0));
        assertEquals(new URIReferenceImpl(new URI("test:o02")), sub2.getObject(0));
        assertTrue(sub1.next());
        assertTrue(sub2.next());
        assertEquals(new URIReferenceImpl(new URI("test:o02")), sub1.getObject(0));
        assertEquals(new URIReferenceImpl(new URI("test:o03")), sub2.getObject(0));
        assertFalse(sub1.next());
        assertFalse(sub2.next());

        answer.close();
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  
  /**
   * Note: What this test does is a really bad idea - there is no
   *       isolation provided as each operation is within its own
   *       transaction.  It does however provide a good test.
   */
  public void testConcurrentReadWrite() throws URISyntaxException {
    logger.info("Testing concurrentReadWrite");

    try {
      Session session = database.newSession();

      session.createModel(model2URI, null);

      try {
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(predicateVariable);
        selectList.add(objectVariable);

        // Evaluate the query
        Answer answer = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Arrays.asList(new Order[] {                       // ORDER BY
            new Order(subjectVariable, true),
            new Order(predicateVariable, true),
            new Order(objectVariable, true)
          }),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));

        answer.beforeFirst();
        while (answer.next()) {
          session.insert(model2URI, Collections.singleton(new TripleImpl(
              (SubjectNode)answer.getObject(0),
              (PredicateNode)answer.getObject(1),
              (ObjectNode)answer.getObject(2))));
        }
        answer.close();

        Answer answer2 = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(model2URI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Arrays.asList(new Order[] {                       // ORDER BY
            new Order(subjectVariable, true),
            new Order(predicateVariable, true),
            new Order(objectVariable, true)
          }),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));
        String[][] results = {
          { "test:s01", "test:p01", "test:o01" },
          { "test:s01", "test:p02", "test:o01" },
          { "test:s01", "test:p02", "test:o02" },
          { "test:s01", "test:p03", "test:o02" },
          { "test:s02", "test:p03", "test:o02" },
          { "test:s02", "test:p04", "test:o02" },
          { "test:s02", "test:p04", "test:o03" },
          { "test:s02", "test:p05", "test:o03" },
          { "test:s03", "test:p01", "test:o01" },
          { "test:s03", "test:p05", "test:o03" },
          { "test:s03", "test:p06", "test:o01" },
          { "test:s03", "test:p06", "test:o03" },
        };
        compareResults(results, answer2);
        answer2.close();

        session.removeModel(model2URI);
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  
  public void testExplicitBasicQuery() throws URISyntaxException {
    logger.info("Testing basicQuery");

    try {
      // Load some test data
      Session session = database.newSession();
      try {
        session.setAutoCommit(false);
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(predicateVariable);
        selectList.add(objectVariable);

        // Evaluate the query
        Answer answer = session.query(new Query(
          selectList,                                       // SELECT
          new ModelResource(modelURI),                      // FROM
          new ConstraintImpl(subjectVariable,               // WHERE
                         predicateVariable,
                         objectVariable),
          null,                                             // HAVING
          Arrays.asList(new Order[] {                       // ORDER BY
            new Order(subjectVariable, true),
            new Order(predicateVariable, true),
            new Order(objectVariable, true)
          }),
          null,                                             // LIMIT
          0,                                                // OFFSET
          new UnconstrainedAnswer()                         // GIVEN
        ));
        String[][] results = {
          { "test:s01", "test:p01", "test:o01" },
          { "test:s01", "test:p02", "test:o01" },
          { "test:s01", "test:p02", "test:o02" },
          { "test:s01", "test:p03", "test:o02" },
          { "test:s02", "test:p03", "test:o02" },
          { "test:s02", "test:p04", "test:o02" },
          { "test:s02", "test:p04", "test:o03" },
          { "test:s02", "test:p05", "test:o03" },
          { "test:s03", "test:p01", "test:o01" },
          { "test:s03", "test:p05", "test:o03" },
          { "test:s03", "test:p06", "test:o01" },
          { "test:s03", "test:p06", "test:o03" },
        };
        compareResults(results, answer);

        session.setAutoCommit(true);

        // Should throw an Exception here as commit should close answer
        boolean thrown = false;;
        try {
          answer.beforeFirst();
        } catch (TuplesException et) {
          thrown = true;
        } finally {
          assertTrue("Answer failed to throw exception, should be closed by commit",
              thrown);
        }
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  
  public void testExplicitIsolationQuery() throws URISyntaxException
  {
    logger.info("testExplicitIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        Session session2 = database.newSession();
        try {
          session1.createModel(model3URI, null);
          session1.setAutoCommit(false);
          session1.setModel(model3URI, new ModelResource(fileURI));

          Variable subjectVariable   = new Variable("subject");
          Variable predicateVariable = new Variable("predicate");
          Variable objectVariable    = new Variable("object");

          List selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          Answer answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));
          answer.beforeFirst();
          assertFalse(answer.next());
          answer.close();

          session1.setAutoCommit(true);

          selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));

          String[][] results = {
            { "test:s01", "test:p01", "test:o01" },
            { "test:s01", "test:p02", "test:o01" },
            { "test:s01", "test:p02", "test:o02" },
            { "test:s01", "test:p03", "test:o02" },
            { "test:s02", "test:p03", "test:o02" },
            { "test:s02", "test:p04", "test:o02" },
            { "test:s02", "test:p04", "test:o03" },
            { "test:s02", "test:p05", "test:o03" },
            { "test:s03", "test:p01", "test:o01" },
            { "test:s03", "test:p05", "test:o03" },
            { "test:s03", "test:p06", "test:o01" },
            { "test:s03", "test:p06", "test:o03" },
          };
          compareResults(results, answer);
          answer.close();

          session1.removeModel(model3URI);
        } finally {
          session2.close();
        }
      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  public void testExplicitRollbackIsolationQuery() throws URISyntaxException
  {
    logger.info("testExplicitRollbackIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        Session session2 = database.newSession();
        try {
          session1.createModel(model3URI, null);
          session1.setAutoCommit(false);
          session1.setModel(model3URI, new ModelResource(fileURI));

          Variable subjectVariable   = new Variable("subject");
          Variable predicateVariable = new Variable("predicate");
          Variable objectVariable    = new Variable("object");

          List selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          Answer answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));
          answer.beforeFirst();
          assertFalse(answer.next());
          answer.close();

          session1.rollback();
          session1.setAutoCommit(true);

          selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));

          answer.beforeFirst();
          assertFalse(answer.next());
          answer.close();
        } finally {
          session2.close();
        }
      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  public void testExplicitCommitIsolationQuery() throws URISyntaxException
  {
    logger.info("testExplicitCommitIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        Session session2 = database.newSession();
        try {
          session1.createModel(model3URI, null);
          session1.setAutoCommit(false);
          session1.setModel(model3URI, new ModelResource(fileURI));

          Variable subjectVariable   = new Variable("subject");
          Variable predicateVariable = new Variable("predicate");
          Variable objectVariable    = new Variable("object");

          List selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          Answer answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));
          answer.beforeFirst();
          assertFalse(answer.next());
          answer.close();

          session1.commit();

          selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));

          String[][] results = {
            { "test:s01", "test:p01", "test:o01" },
            { "test:s01", "test:p02", "test:o01" },
            { "test:s01", "test:p02", "test:o02" },
            { "test:s01", "test:p03", "test:o02" },
            { "test:s02", "test:p03", "test:o02" },
            { "test:s02", "test:p04", "test:o02" },
            { "test:s02", "test:p04", "test:o03" },
            { "test:s02", "test:p05", "test:o03" },
            { "test:s03", "test:p01", "test:o01" },
            { "test:s03", "test:p05", "test:o03" },
            { "test:s03", "test:p06", "test:o01" },
            { "test:s03", "test:p06", "test:o03" },
          };
          compareResults(results, answer);
          answer.close();

          session1.removeModel(model3URI);
          session1.createModel(model3URI, null);

          selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));

          results = new String[][] {
            { "test:s01", "test:p01", "test:o01" },
            { "test:s01", "test:p02", "test:o01" },
            { "test:s01", "test:p02", "test:o02" },
            { "test:s01", "test:p03", "test:o02" },
            { "test:s02", "test:p03", "test:o02" },
            { "test:s02", "test:p04", "test:o02" },
            { "test:s02", "test:p04", "test:o03" },
            { "test:s02", "test:p05", "test:o03" },
            { "test:s03", "test:p01", "test:o01" },
            { "test:s03", "test:p05", "test:o03" },
            { "test:s03", "test:p06", "test:o01" },
            { "test:s03", "test:p06", "test:o03" },
          };
          compareResults(results, answer);
          answer.close();

          session1.setAutoCommit(true);

          selectList = new ArrayList(3);
          selectList.add(subjectVariable);
          selectList.add(predicateVariable);
          selectList.add(objectVariable);

          // Evaluate the query
          answer = session2.query(new Query(
            selectList,                                       // SELECT
            new ModelResource(model3URI),                      // FROM
            new ConstraintImpl(subjectVariable,               // WHERE
                           predicateVariable,
                           objectVariable),
            null,                                             // HAVING
            Arrays.asList(new Order[] {                       // ORDER BY
              new Order(subjectVariable, true),
              new Order(predicateVariable, true),
              new Order(objectVariable, true)
            }),
            null,                                             // LIMIT
            0,                                                // OFFSET
            new UnconstrainedAnswer()                         // GIVEN
          ));
          answer.beforeFirst();
          assertFalse(answer.next());
          answer.close();
        } finally {
          session2.close();
        }
      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  public void testDatabaseDelete() {
    database.delete();
    database = null;
  }

  //
  // Internal methods
  //

  private void compareResults(String[][] expected, Answer answer) throws Exception {
    answer.beforeFirst();
    for (int i = 0; i < expected.length; i++) {
      assertTrue("Answer short at row " + i, answer.next());
      assertEquals(expected[i].length, answer.getNumberOfVariables());
      for (int j = 0; j < expected[i].length; j++) {
        URIReferenceImpl uri = new URIReferenceImpl(
            new URI(expected[i][j]));
        assertEquals(uri, answer.getObject(j));
      }
    }
    assertFalse(answer.next());
  }

  private void compareResults(Answer answer1, Answer answer2) throws Exception {
    answer1.beforeFirst();
    answer2.beforeFirst();
    assertEquals(answer1.getNumberOfVariables(), answer2.getNumberOfVariables());
    while (answer1.next()) {
      assertTrue(answer2.next());
      for (int i = 0; i < answer1.getNumberOfVariables(); i++) {
        assertEquals(answer1.getObject(i), answer2.getObject(i));
      }
    }
    assertFalse(answer2.next());
  }

  /**
   * Fail with an unexpected exception
   */
  private void fail(Throwable throwable)
  {
    StringWriter stringWriter = new StringWriter();
    throwable.printStackTrace(new PrintWriter(stringWriter));
    fail(stringWriter.toString());
  }
}