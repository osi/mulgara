/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.rosenlaw.com/OSL3.0.htm
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * This file is an original work developed by Netymon Pty Ltd
 * (http://www.netymon.com, mailto:mail@netymon.com) under contract to 
 * Topaz Foundation. Portions created under this contract are
 * Copyright (c) 2007 Topaz Foundation
 * All Rights Reserved.
 *
 * scaffolding based on AdvDatabaseSessionUnitTest.java
 */

package org.mulgara.resolver;

// Java 2 standard packages
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

// Third party packages
import junit.framework.*;        // JUnit
import org.apache.log4j.Logger;  // Log4J
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.ObjectNode;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.query.rdf.TripleImpl;
import org.mulgara.server.Session;
import org.mulgara.util.FileUtil;

/**
 * Testing Externally Mediated Transactions. 
 *
 * @created 2007-11-27
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 * @company <a href="http://www.netymon.com/">Netymon Pty Ltd</a>
 *      Software Pty Ltd</a>
 * @copyright &copy;2006 <a href="http://www.topazproject.org/">Topaz Project
 * Foundation</a>
 *
 * @licence Open Software License v3.0</a>
 */

public class ExternalTransactionUnitTest extends TestCase
{
  /** Logger.  */
  private static Logger logger =
    Logger.getLogger(ExternalTransactionUnitTest.class.getName());

  private static final URI databaseURI;

  private static final URI systemModelURI;

  private static final URI modelURI;
  private static final URI model2URI;
  private static final URI model3URI;
  private static final URI model4URI;
  private static final URI model5URI;

  static {
    try {
      databaseURI    = new URI("local://database");
      systemModelURI = new URI("local://database#");
      modelURI       = new URI("local://database#model");
      model2URI      = new URI("local://database#model2");
      model3URI      = new URI("local://database#model3");
      model4URI      = new URI("local://database#model4");
      model5URI      = new URI("local://database#model5");
    } catch (URISyntaxException e) {
      throw new Error("Bad hardcoded URI", e);
    }
  }

  private static Database database = null;

  public ExternalTransactionUnitTest(String name)
  {
    super(name);
  }

  public static Test suite()
  {
    TestSuite suite = new TestSuite();
    suite.addTest(new ExternalTransactionUnitTest("testSimpleOnePhaseCommit"));
    suite.addTest(new ExternalTransactionUnitTest("testSimpleTwoPhaseCommit"));
    suite.addTest(new ExternalTransactionUnitTest("testBasicQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testMultipleQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testBasicReadOnlyQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testConcurrentQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testConcurrentReadWrite"));
    suite.addTest(new ExternalTransactionUnitTest("testSubqueryQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testConcurrentSubqueryQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testExplicitIsolationQuerySingleSession"));
    suite.addTest(new ExternalTransactionUnitTest("testConcurrentExplicitTxn"));
    suite.addTest(new ExternalTransactionUnitTest("testExplicitRollbackIsolationQuery"));
    suite.addTest(new ExternalTransactionUnitTest("testExternalInternalIsolation"));
    suite.addTest(new ExternalTransactionUnitTest("testInternalExternalIsolation"));
    suite.addTest(new ExternalTransactionUnitTest("testInternalExternalConcurrentTxn"));
    suite.addTest(new ExternalTransactionUnitTest("testExternalInternalConcurrentTxn"));
    suite.addTest(new ExternalTransactionUnitTest("testInternalExternalConcurrentTxnRollback"));
    suite.addTest(new ExternalTransactionUnitTest("testExternalInternalConcurrentTxnRollback"));
    suite.addTest(new ExternalTransactionUnitTest("testInternalSerialMultipleSessions"));

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

  private static class TestXid implements Xid {
    private int xid;
    public TestXid(int xid) {
      this.xid = xid;
    }
    
    public int getFormatId() {
      return 'X';
    }

    public byte[] getBranchQualifier() {
      return new byte[] {
        (byte)(xid >> 0x00),
        (byte)(xid >> 0x08)
      };
    }

    public byte[] getGlobalTransactionId() {
      return new byte[] {
        (byte)(xid >> 0x10),
        (byte)(xid >> 0x18)
      };
    }
  }

  /**
   * Test the {@link DatabaseSession#create} method.
   * As a side-effect, creates the model required by the next tests.
   */
  public void testSimpleOnePhaseCommit() throws URISyntaxException
  {
    logger.info("testSimpleOnePhaseCommit");

    try {
      DatabaseSession session = (DatabaseSession)database.newSession();
      XAResource resource = session.getXAResource();
      Xid xid = new TestXid(1);
      resource.start(xid, XAResource.TMNOFLAGS);
      try {
        session.createModel(modelURI, null);
        resource.end(xid, XAResource.TMSUCCESS);
        resource.commit(xid, true);
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  /**
   * Test two phase commit.
   * As a side-effect, loads the model required by the next tests.
   */
  public void testSimpleTwoPhaseCommit() throws URISyntaxException
  {
    logger.info("testSimpleTwoPhaseCommit");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      DatabaseSession session = (DatabaseSession)database.newSession();
      XAResource resource = session.getXAResource();
      Xid xid = new TestXid(1);
      resource.start(xid, XAResource.TMNOFLAGS);
      try {
        session.setModel(modelURI, new ModelResource(fileURI));
        resource.end(xid, XAResource.TMSUCCESS);
        resource.prepare(xid);
        resource.commit(xid, false);
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
      DatabaseSession session = (DatabaseSession)database.newSession();
      try {
        XAResource resource = session.getXAResource();
        Xid xid = new TestXid(1);
        resource.start(xid, XAResource.TMNOFLAGS);

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

        resource.end(xid, XAResource.TMSUCCESS);
        resource.commit(xid, true);
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  public void testMultipleQuery() throws URISyntaxException {
    logger.info("Testing MultipleQuery");

    try {
      // Load some test data
      Session session = database.newSession();
      XAResource resource = session.getXAResource();
      Xid xid = new TestXid(1);
      resource.start(xid, XAResource.TMNOFLAGS);
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

        resource.end(xid, XAResource.TMSUCCESS);
        resource.commit(xid, true);
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  public void testBasicReadOnlyQuery() throws URISyntaxException {
    logger.info("Testing basicReadOnlyQuery");

    try {
      // Load some test data
      DatabaseSession session = (DatabaseSession)database.newSession();
      try {
        XAResource resource = session.getReadOnlyXAResource();
        Xid xid = new TestXid(1);
        resource.start(xid, XAResource.TMNOFLAGS);

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

        resource.end(xid, XAResource.TMSUCCESS);
        resource.commit(xid, true);
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
      XAResource resource = session.getReadOnlyXAResource();
      Xid xid1 = new TestXid(1);
      Xid xid2 = new TestXid(2);
      resource.start(xid1, XAResource.TMNOFLAGS);
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
        resource.end(xid1, XAResource.TMSUSPEND);
        resource.start(xid2, XAResource.TMNOFLAGS);

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
        resource.end(xid2, XAResource.TMSUSPEND);

        compareResults(answer1, answer2);

        answer1.close();
        answer2.close();

        resource.start(xid1, XAResource.TMRESUME);
        resource.end(xid1, XAResource.TMSUCCESS);
        resource.end(xid2, XAResource.TMSUCCESS);
        resource.commit(xid1, true);
        resource.commit(xid2, true);
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
      XAResource roResource = session.getReadOnlyXAResource();
      XAResource rwResource = session.getXAResource();
      Xid xid1 = new TestXid(1);

      rwResource.start(xid1, XAResource.TMNOFLAGS);
      session.createModel(model2URI, null);
      rwResource.end(xid1, XAResource.TMSUSPEND);

      try {
        Variable subjectVariable   = new Variable("subject");
        Variable predicateVariable = new Variable("predicate");
        Variable objectVariable    = new Variable("object");

        List selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(predicateVariable);
        selectList.add(objectVariable);

        Xid xid2 = new TestXid(2);
        roResource.start(xid2, XAResource.TMNOFLAGS);

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

        roResource.end(xid2, XAResource.TMSUSPEND);
        answer.beforeFirst();
        while (answer.next()) {
          rwResource.start(xid1, XAResource.TMRESUME);
          session.insert(model2URI, Collections.singleton(new TripleImpl(
              (SubjectNode)answer.getObject(0),
              (PredicateNode)answer.getObject(1),
              (ObjectNode)answer.getObject(2))));
          rwResource.end(xid1, XAResource.TMSUSPEND);
        }
        answer.close();

        rwResource.end(xid1, XAResource.TMSUCCESS);
        rwResource.commit(xid1, true);

        Xid xid3 = new TestXid(3);
        roResource.start(xid3, XAResource.TMNOFLAGS);

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

        roResource.end(xid3, XAResource.TMSUSPEND);
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

        Xid xid4 = new TestXid(4);
        rwResource.start(xid4, XAResource.TMNOFLAGS);
        session.removeModel(model2URI);
        rwResource.end(xid4, XAResource.TMSUCCESS);
        rwResource.commit(xid4, true);

        roResource.end(xid2, XAResource.TMSUCCESS);
        roResource.commit(xid2, true);
        roResource.end(xid3, XAResource.TMSUCCESS);
        roResource.commit(xid3, true);
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
      XAResource roResource = session.getReadOnlyXAResource();
      Xid xid1 = new TestXid(1);
      roResource.start(xid1, XAResource.TMNOFLAGS);

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

        roResource.end(xid1, XAResource.TMSUSPEND);

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

        // Leave transaction to be closed on session close.
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
      Session session = database.newSession();
      XAResource rwResource = session.getXAResource();
      Xid xid1 = new TestXid(1);
      rwResource.start(xid1, XAResource.TMNOFLAGS);

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

        rwResource.end(xid1, XAResource.TMSUSPEND);

        assertTrue(sub1.next());
        assertTrue(sub2.next());
        assertEquals(new URIReferenceImpl(new URI("test:o02")), sub1.getObject(0));
        assertEquals(new URIReferenceImpl(new URI("test:o03")), sub2.getObject(0));
        assertFalse(sub1.next());
        assertFalse(sub2.next());

        answer.close();

        rwResource.end(xid1, XAResource.TMSUCCESS);
        rwResource.commit(xid1, true);
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  public void testExplicitIsolationQuerySingleSession() throws URISyntaxException
  {
    logger.info("testExplicitIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session = database.newSession();
      try {
        XAResource roResource = session.getReadOnlyXAResource();
        XAResource rwResource = session.getXAResource();
        Xid xid1 = new TestXid(1); // Initial create model.
        Xid xid2 = new TestXid(2); // Started before setModel.
        Xid xid3 = new TestXid(3); // setModel.
        Xid xid4 = new TestXid(4); // Started before setModel prepares
        Xid xid5 = new TestXid(5); // Started before setModel commits
        Xid xid6 = new TestXid(6); // Started after setModel commits
        Xid xid7 = new TestXid(7); // Final remove model.

        rwResource.start(xid1, XAResource.TMNOFLAGS);
        session.createModel(model3URI, null);
        rwResource.end(xid1, XAResource.TMSUCCESS);
        rwResource.commit(xid1, true);

        // Nothing visible.
        roResource.start(xid2, XAResource.TMNOFLAGS);
        assertChangeNotVisible(session);
        roResource.end(xid2, XAResource.TMSUSPEND);

        // Perform update
        rwResource.start(xid3, XAResource.TMNOFLAGS);
        session.setModel(model3URI, new ModelResource(fileURI));
        rwResource.end(xid3, XAResource.TMSUSPEND);

        // Check uncommitted change not visible
        roResource.start(xid4, XAResource.TMNOFLAGS);
        assertChangeNotVisible(session);
        roResource.end(xid4, XAResource.TMSUSPEND);

        // Check original phase unaffected.
        roResource.start(xid2, XAResource.TMRESUME);
        assertChangeNotVisible(session);
        roResource.end(xid2, XAResource.TMSUSPEND);

        // Check micro-commit visible to current-phase
        rwResource.start(xid3, XAResource.TMRESUME);
        assertChangeVisible(session);
        // Perform prepare
        rwResource.end(xid3, XAResource.TMSUCCESS);
        rwResource.prepare(xid3);

        // Check original phase unaffected
        roResource.start(xid2, XAResource.TMRESUME);
        assertChangeNotVisible(session);
        roResource.end(xid2, XAResource.TMSUSPEND);

        // Check pre-prepare phase unaffected
        roResource.start(xid4, XAResource.TMRESUME);
        assertChangeNotVisible(session);
        roResource.end(xid4, XAResource.TMSUSPEND);

        // Check committed phase unaffected.
        roResource.start(xid5, XAResource.TMNOFLAGS);
        assertChangeNotVisible(session);
        roResource.end(xid5, XAResource.TMSUSPEND);

        // Do commit
        rwResource.commit(xid3, false);

        // Check original phase
        roResource.start(xid2, XAResource.TMRESUME);
        assertChangeNotVisible(session);
        roResource.end(xid2, XAResource.TMSUSPEND);

        // Check pre-prepare
        roResource.start(xid4, XAResource.TMRESUME);
        assertChangeNotVisible(session);
        roResource.end(xid4, XAResource.TMSUSPEND);

        // Check pre-commit
        roResource.start(xid5, XAResource.TMRESUME);
        assertChangeNotVisible(session);
        roResource.end(xid5, XAResource.TMSUSPEND);

        // Check committed phase is now updated
        roResource.start(xid6, XAResource.TMNOFLAGS);
        assertChangeVisible(session);

        // Cleanup transactions.
        roResource.end(xid6, XAResource.TMSUCCESS);
        roResource.end(xid2, XAResource.TMSUCCESS);
        roResource.end(xid4, XAResource.TMSUCCESS);
        roResource.end(xid5, XAResource.TMSUCCESS);
        roResource.commit(xid2, true);
        roResource.commit(xid4, true);
        roResource.commit(xid5, true);
        roResource.commit(xid6, true);

        // Cleanup database
        rwResource.start(xid7, XAResource.TMNOFLAGS);
        session.removeModel(model3URI);
        rwResource.end(xid7, XAResource.TMSUCCESS);
        rwResource.commit(xid7, true);
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  public void testExternalInternalIsolation() throws URISyntaxException
  {
    logger.info("testExplicitIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        Session session2 = database.newSession();
        try {
          XAResource roResource = session1.getReadOnlyXAResource();
          XAResource rwResource = session1.getXAResource();
          Xid xid1 = new TestXid(1); // Initial create model.
          Xid xid2 = new TestXid(2); // Main Test.
          Xid xid3 = new TestXid(3); // Cleanup test.

          rwResource.start(xid1, XAResource.TMNOFLAGS);
          session1.createModel(model3URI, null);
          rwResource.end(xid1, XAResource.TMSUCCESS);
          rwResource.commit(xid1, true);

          // Nothing visible.
          assertChangeNotVisible(session2);

          // Perform update
          rwResource.start(xid2, XAResource.TMNOFLAGS);
          session1.setModel(model3URI, new ModelResource(fileURI));
          rwResource.end(xid2, XAResource.TMSUSPEND);

          // Check uncommitted change not visible
          assertChangeNotVisible(session2);

          // Check micro-commit visible to current-phase
          rwResource.start(xid2, XAResource.TMRESUME);
          assertChangeVisible(session1);
          // Perform prepare
          rwResource.end(xid2, XAResource.TMSUCCESS);
          rwResource.prepare(xid2);

          // Check original phase unaffected
          assertChangeNotVisible(session2);

          // Do commit
          rwResource.commit(xid2, false);

          // Check committed phase is now updated
          assertChangeVisible(session2);

          // Cleanup database
          session2.removeModel(model3URI);
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

  public void testInternalExternalIsolation() throws URISyntaxException
  {
    logger.info("testExplicitIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        Session session2 = database.newSession();
        try {
          XAResource roResource = session2.getReadOnlyXAResource();
          XAResource rwResource = session2.getXAResource();
          Xid xid1 = new TestXid(1); // Pre-update
          Xid xid2 = new TestXid(2); // Post-update/Pre-commit
          Xid xid3 = new TestXid(3); // Post-commit

          session1.createModel(model3URI, null);

          // Nothing visible.
          roResource.start(xid1, XAResource.TMNOFLAGS);
          assertChangeNotVisible(session2);
          roResource.end(xid1, XAResource.TMSUSPEND);

          // Perform update with autocommit off
          session1.setAutoCommit(false);
          session1.setModel(model3URI, new ModelResource(fileURI));

          // Check uncommitted change not visible
          roResource.start(xid2, XAResource.TMNOFLAGS);
          assertChangeNotVisible(session2);
          roResource.end(xid2, XAResource.TMSUSPEND);

          // Check original phase unaffected.
          roResource.start(xid1, XAResource.TMRESUME);
          assertChangeNotVisible(session2);
          roResource.end(xid1, XAResource.TMSUSPEND);

          // Check micro-commit visible to current-phase
          assertChangeVisible(session1);
          session1.setAutoCommit(true);

          // Check original phase unaffected
          roResource.start(xid1, XAResource.TMRESUME);
          assertChangeNotVisible(session2);
          roResource.end(xid1, XAResource.TMSUSPEND);

          // Check pre-commit phase unaffected
          roResource.start(xid2, XAResource.TMRESUME);
          assertChangeNotVisible(session2);
          roResource.end(xid2, XAResource.TMSUSPEND);

          // Check committed phase is now updated and write-lock available
          rwResource.start(xid3, XAResource.TMNOFLAGS);
          assertChangeVisible(session2);
          
          // Check internal transaction read-only
          assertChangeVisible(session1);

          // Cleanup transactions.
          rwResource.end(xid3, XAResource.TMSUCCESS);
          roResource.end(xid2, XAResource.TMSUCCESS);
          roResource.end(xid1, XAResource.TMSUCCESS);
          roResource.commit(xid1, true);
          roResource.commit(xid2, true);
          rwResource.commit(xid3, true);

          // Cleanup database (check write-lock available again)
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

  private void assertChangeVisible(Session session) throws Exception {
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
  }

  private void assertChangeNotVisible(Session session) throws Exception {
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
  }

  /**
   * Test two simultaneous, explicit transactions, in two threads. The second one should block
   * until the first one sets auto-commit back to true.
   */
  public void testConcurrentExplicitTxn() throws URISyntaxException
  {
    logger.info("testConcurrentExplicitTxn");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        XAResource resource1 = session1.getXAResource();
        resource1.start(new TestXid(1), XAResource.TMNOFLAGS);
        session1.createModel(model3URI, null);
        resource1.end(new TestXid(1), XAResource.TMSUCCESS);
        resource1.commit(new TestXid(1), true);

        resource1.start(new TestXid(2), XAResource.TMNOFLAGS);
        session1.setModel(model3URI, new ModelResource(fileURI));

        final boolean[] tx2Started = new boolean[] { false };

        Thread t2 = new Thread("tx2Test") {
          public void run() {
            try {
              Session session2 = database.newSession();
              XAResource resource2 = session2.getXAResource();
              try {
                resource2.start(new TestXid(3), XAResource.TMNOFLAGS);

                synchronized (tx2Started) {
                  tx2Started[0] = true;
                  tx2Started.notify();
                }

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

                resource2.end(new TestXid(3), XAResource.TMSUCCESS);
                resource2.commit(new TestXid(3), true);
              } finally {
                session2.close();
              }
            } catch (Exception e) {
              fail(e);
            }
          }
        };
        t2.start();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        resource1.commit(new TestXid(2), true);

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
            assertTrue("second transaction should've started", tx2Started[0]);
          }
        }

        try {
          t2.join(2000L);
        } catch (InterruptedException ie) {
          logger.error("wait for tx2-terminated interrupted", ie);
          fail(ie);
        }
        assertFalse("second transaction should've terminated", t2.isAlive());

        resource1.start(new TestXid(4), XAResource.TMNOFLAGS);
        session1.removeModel(model3URI);
        resource1.end(new TestXid(4), XAResource.TMSUCCESS);
        resource1.commit(new TestXid(4), true);

      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  /**
   * Test two simultaneous transactions, in two threads. The second one should block
   * until the first one sets auto-commit back to true.
   */
  public void testExternalInternalConcurrentTxn() throws URISyntaxException
  {
    logger.info("testConcurrentExplicitTxn");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        XAResource resource1 = session1.getXAResource();
        resource1.start(new TestXid(1), XAResource.TMNOFLAGS);
        session1.createModel(model3URI, null);
        resource1.end(new TestXid(1), XAResource.TMSUCCESS);
        resource1.commit(new TestXid(1), true);

        resource1.start(new TestXid(2), XAResource.TMNOFLAGS);
        session1.setModel(model3URI, new ModelResource(fileURI));

        final boolean[] tx2Started = new boolean[] { false };

        Thread t2 = new Thread("tx2Test") {
          public void run() {
            try {
              Session session2 = database.newSession();
              try {
                session2.setAutoCommit(false);

                synchronized (tx2Started) {
                  tx2Started[0] = true;
                  tx2Started.notify();
                }

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

                session2.setAutoCommit(true);
              } finally {
                session2.close();
              }
            } catch (Exception e) {
              fail(e);
            }
          }
        };
        t2.start();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        resource1.commit(new TestXid(2), true);

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
            assertTrue("second transaction should've started", tx2Started[0]);
          }
        }

        try {
          t2.join(2000L);
        } catch (InterruptedException ie) {
          logger.error("wait for tx2-terminated interrupted", ie);
          fail(ie);
        }
        assertFalse("second transaction should've terminated", t2.isAlive());

        resource1.start(new TestXid(4), XAResource.TMNOFLAGS);
        session1.removeModel(model3URI);
        resource1.end(new TestXid(4), XAResource.TMSUCCESS);
        resource1.commit(new TestXid(4), true);

      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  /**
   * Test two simultaneous transactions, in two threads. The second one should block
   * until the first one sets auto-commit back to true.
   */
  public void testInternalExternalConcurrentTxn() throws URISyntaxException
  {
    logger.info("testConcurrentExplicitTxn");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        session1.createModel(model3URI, null);

        session1.setAutoCommit(false);
        session1.setModel(model3URI, new ModelResource(fileURI));

        final boolean[] tx2Started = new boolean[] { false };

        Thread t2 = new Thread("tx2Test") {
          public void run() {
            try {
              Session session2 = database.newSession();
              try {
                XAResource resource = session2.getXAResource();
                resource.start(new TestXid(1), XAResource.TMNOFLAGS);

                synchronized (tx2Started) {
                  tx2Started[0] = true;
                  tx2Started.notify();
                }

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

                resource.end(new TestXid(1), XAResource.TMSUCCESS);
                resource.rollback(new TestXid(1));
              } finally {
                session2.close();
              }
            } catch (Exception e) {
              fail(e);
            }
          }
        };
        t2.start();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        session1.commit();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        session1.setAutoCommit(true);

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
            assertTrue("second transaction should've started", tx2Started[0]);
          }
        }

        try {
          t2.join(2000L);
        } catch (InterruptedException ie) {
          logger.error("wait for tx2-terminated interrupted", ie);
          fail(ie);
        }
        assertFalse("second transaction should've terminated", t2.isAlive());

        session1.removeModel(model3URI);
      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  /**
   * Test two simultaneous transactions, in two threads. The second one should block
   * until the first one sets auto-commit back to true.
   */
  public void testExternalInternalConcurrentTxnRollback() throws URISyntaxException
  {
    logger.info("testConcurrentExplicitTxn");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        XAResource resource1 = session1.getXAResource();
        resource1.start(new TestXid(1), XAResource.TMNOFLAGS);
        session1.createModel(model3URI, null);
        resource1.end(new TestXid(1), XAResource.TMSUCCESS);
        resource1.commit(new TestXid(1), true);

        resource1.start(new TestXid(2), XAResource.TMNOFLAGS);
        session1.setModel(model3URI, new ModelResource(fileURI));

        final boolean[] tx2Started = new boolean[] { false };

        Thread t2 = new Thread("tx2Test") {
          public void run() {
            try {
              Session session2 = database.newSession();
              try {
                session2.setAutoCommit(false);

                synchronized (tx2Started) {
                  tx2Started[0] = true;
                  tx2Started.notify();
                }

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

                session2.setAutoCommit(true);
              } finally {
                session2.close();
              }
            } catch (Exception e) {
              fail(e);
            }
          }
        };
        t2.start();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        resource1.rollback(new TestXid(2));

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
            assertTrue("second transaction should've started", tx2Started[0]);
          }
        }

        try {
          t2.join(2000L);
        } catch (InterruptedException ie) {
          logger.error("wait for tx2-terminated interrupted", ie);
          fail(ie);
        }
        assertFalse("second transaction should've terminated", t2.isAlive());

        resource1.start(new TestXid(4), XAResource.TMNOFLAGS);
        session1.removeModel(model3URI);
        resource1.end(new TestXid(4), XAResource.TMSUCCESS);
        resource1.commit(new TestXid(4), true);

      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  /**
   * Test two simultaneous transactions, in two threads. The second one should block
   * until the first one sets auto-commit back to true.
   */
  public void testInternalExternalConcurrentTxnRollback() throws URISyntaxException
  {
    logger.info("testConcurrentExplicitTxn");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      try {
        session1.createModel(model3URI, null);

        session1.setAutoCommit(false);
        session1.setModel(model3URI, new ModelResource(fileURI));

        final boolean[] tx2Started = new boolean[] { false };

        Thread t2 = new Thread("tx2Test") {
          public void run() {
            try {
              Session session2 = database.newSession();
              try {
                XAResource resource = session2.getXAResource();
                resource.start(new TestXid(1), XAResource.TMNOFLAGS);

                synchronized (tx2Started) {
                  tx2Started[0] = true;
                  tx2Started.notify();
                }

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

                resource.end(new TestXid(1), XAResource.TMFAIL);
                resource.rollback(new TestXid(1));
              } finally {
                session2.close();
              }
            } catch (Exception e) {
              fail(e);
            }
          }
        };
        t2.start();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        session1.rollback();

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
          }
          assertFalse("second transaction should still be waiting for write lock", tx2Started[0]);
        }

        session1.setAutoCommit(true);

        synchronized (tx2Started) {
          if (!tx2Started[0]) {
            try {
              tx2Started.wait(2000L);
            } catch (InterruptedException ie) {
              logger.error("wait for tx2-started interrupted", ie);
              fail(ie);
            }
            assertTrue("second transaction should've started", tx2Started[0]);
          }
        }

        try {
          t2.join(2000L);
        } catch (InterruptedException ie) {
          logger.error("wait for tx2-terminated interrupted", ie);
          fail(ie);
        }
        assertFalse("second transaction should've terminated", t2.isAlive());

        session1.removeModel(model3URI);
      } finally {
        session1.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  public void testExplicitRollbackIsolationQuery() throws URISyntaxException {
    logger.info("testExplicitRollbackIsolationQuery");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session = database.newSession();
      XAResource roResource = session.getReadOnlyXAResource();
      XAResource rwResource = session.getXAResource();
      try {
        rwResource.start(new TestXid(1), XAResource.TMNOFLAGS);
        session.createModel(model3URI, null);
        rwResource.end(new TestXid(1), XAResource.TMSUCCESS);
        rwResource.commit(new TestXid(1), true);

        rwResource.start(new TestXid(2), XAResource.TMNOFLAGS);
        session.setModel(model3URI, new ModelResource(fileURI));
        rwResource.end(new TestXid(2), XAResource.TMSUSPEND);

        roResource.start(new TestXid(3), XAResource.TMNOFLAGS);

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

        roResource.end(new TestXid(3), XAResource.TMSUCCESS);
        roResource.commit(new TestXid(3), true);

        rwResource.end(new TestXid(2), XAResource.TMFAIL);
        rwResource.rollback(new TestXid(2));

        roResource.start(new TestXid(4), XAResource.TMNOFLAGS);
        selectList = new ArrayList(3);
        selectList.add(subjectVariable);
        selectList.add(predicateVariable);
        selectList.add(objectVariable);

        // Evaluate the query
        answer = session.query(new Query(
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

        roResource.end(new TestXid(4), XAResource.TMFAIL);
        roResource.rollback(new TestXid(4));
      } finally {
        session.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }


  /**
   * Tests cleaning up a transaction on close.  This test added in the process
   * of fixing a bug reported by Ronald on the JTA-beta.
   */
  public void testInternalSerialMultipleSessions() throws URISyntaxException
  {
    logger.info("testInternalSerialMultipleSessions");
    URI fileURI  = new File("data/xatest-model1.rdf").toURI();

    try {
      Session session1 = database.newSession();
      Session session2 = database.newSession();
      try {
        session1.createModel(model4URI, null);

        session1.setAutoCommit(false);
        session1.setModel(model4URI, new ModelResource(fileURI));

        session1.commit();
        session1.close();

        session2.setAutoCommit(false);
      } finally {
        session2.close();
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  //
  // Internal methods
  //

  private void compareResults(String[][] expected, Answer answer) throws Exception {
    try {
      answer.beforeFirst();
      for (int i = 0; i < expected.length; i++) {
        assertTrue("Answer short at row " + i, answer.next());
        assertEquals(expected[i].length, answer.getNumberOfVariables());
        for (int j = 0; j < expected[i].length; j++) {
          URIReferenceImpl uri = new URIReferenceImpl(new URI(expected[i][j]));
          assertEquals(uri, answer.getObject(j));
        }
      }
      assertFalse(answer.next());
    } catch (Exception e) {
      logger.error("Failed test - " + answer);
      answer.close();
      throw e;
    }
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