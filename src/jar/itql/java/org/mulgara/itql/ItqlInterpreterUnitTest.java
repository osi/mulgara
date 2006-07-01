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

package org.mulgara.itql;

// third party packages
import junit.framework.*;

// Java 2 standard packages
import java.io.*;
import java.net.*;
import java.sql.ResultSet;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;

import javax.xml.parsers.*;

import org.apache.log4j.Category;
import org.apache.log4j.xml.DOMConfigurator;

// automagically generated classes
import org.mulgara.itql.parser.ParserException;
import org.mulgara.query.Answer;
import org.mulgara.query.AnswerImpl;
import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ConstraintHaving;
import org.mulgara.query.ConstraintImpl;
import org.mulgara.query.ConstraintOccurs;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.ModelResource;
import org.mulgara.query.Query;
import org.mulgara.query.UnconstrainedAnswer;
import org.mulgara.query.Variable;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.util.ResultSetRow;
import org.mulgara.util.TempDir;
import org.mulgara.util.TestResultSet;

// emory util package
import edu.emory.mathcs.util.remote.io.*;
import edu.emory.mathcs.util.remote.io.server.impl.*;

/**
 * Unit test for {@link ItqlInterpreterUnitTest}. <p>
 *
 * Note. Constraint disjunctions have not yet been implemented, so for now will
 * give the same answers as conjunctions. Hence test results will be <strong>
 * WRONG</strong> once disjunctions are implemented. </p>
 *
 * @created 2001-08-27
 *
 * @author Tom Adams
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/06/26 12:48:09 $ by $Author: pgearon $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ItqlInterpreterUnitTest extends TestCase {

  //
  // Members
  //

  /**
   * the category to send logging info to
   */
  private static Category log =
      Category.getInstance(ItqlInterpreterUnitTest.class.getName());

  /**
   * Get line separator.
   */
  private static final String eol = System.getProperty("line.separator");

  /**
   * Schema namespace.
   */
  private static final String rdfSchemaNamespace =
      "http://www.w3.org/2000/01/rdf-schema#";

  /**
   * the ITQL command interpreter
   */
  private static ItqlInterpreter interpreter = null;

  /**
   * the URI of the dublin core schema
   */
  private static URI dcSchemaURI = null;

  /**
   * an example model
   */
  private static String testModel = null;

  /**
   * a temp directory location
   */
  private static final File tmpDirectory = TempDir.getTempDir();

  /**
    * host name of server
    */
   private static String hostName = System.getProperty("host.name");

  /**
   * Server1
   */
  private static String server = "rmi://"+hostName+"/server1";

  //
  // Public API
  //

  /**
   * Constructs a new ItqlInterpreter unit test.
   *
   * @param name the name of the test
   */
  public ItqlInterpreterUnitTest(String name) {

    // delegate to super class constructor
    super(name);

    // load the logging configuration
    try {

      DOMConfigurator.configure(System.getProperty("cvs.root") +
                                "/log4j-conf.xml");
    }
    catch (FactoryConfigurationError fce) {

      log.error("Unable to configure logging service from XML configuration " +
                "file");
    }

    // try-catch
  }

  // setUp()

  /**
   * Returns a test suite containing the tests to be run.
   *
   * @return the test suite
   */
  public static TestSuite suite() {

    TestSuite suite = new TestSuite();

    suite.addTest(new ItqlInterpreterUnitTest("testHelp"));
    suite.addTest(new ItqlInterpreterUnitTest("testQuit"));
    suite.addTest(new ItqlInterpreterUnitTest("testSu1"));
    suite.addTest(new ItqlInterpreterUnitTest("testAlias1"));

    suite.addTest(new ItqlInterpreterUnitTest("testSelect1"));
    suite.addTest(new ItqlInterpreterUnitTest("testSelect2"));
    suite.addTest(new ItqlInterpreterUnitTest("testSelect3"));
    suite.addTest(new ItqlInterpreterUnitTest("testSelect4"));

    suite.addTest(new ItqlInterpreterUnitTest("testSelect5"));
    suite.addTest(new ItqlInterpreterUnitTest("testSelect6"));

    suite.addTest(new ItqlInterpreterUnitTest("testSelect7"));
    suite.addTest(new ItqlInterpreterUnitTest("testSelect8"));
    suite.addTest(new ItqlInterpreterUnitTest("testDirectory1"));
    suite.addTest(new ItqlInterpreterUnitTest("testCreate1"));
    suite.addTest(new ItqlInterpreterUnitTest("testDrop1"));
    suite.addTest(new ItqlInterpreterUnitTest("testDelete1"));
    suite.addTest(new ItqlInterpreterUnitTest("testDelete2"));
    suite.addTest(new ItqlInterpreterUnitTest("testInsert1"));
    suite.addTest(new ItqlInterpreterUnitTest("testInsert2"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad1"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad2"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad3"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad4"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad5"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad6"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad7"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad8"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoad9"));
    suite.addTest(new ItqlInterpreterUnitTest("testSet1"));
    suite.addTest(new ItqlInterpreterUnitTest("testSet2"));
    suite.addTest(new ItqlInterpreterUnitTest("test1ParseQuery"));
    suite.addTest(new ItqlInterpreterUnitTest("test2ParseQuery"));
    suite.addTest(new ItqlInterpreterUnitTest("test3ParseQuery"));
    suite.addTest(new ItqlInterpreterUnitTest("test4ParseQuery"));

    suite.addTest(new ItqlInterpreterUnitTest("testBackup1"));
    suite.addTest(new ItqlInterpreterUnitTest("testRestore1"));

    suite.addTest(new ItqlInterpreterUnitTest("testCreate3"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoadApi1"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoadApi2"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoadApi3"));
    suite.addTest(new ItqlInterpreterUnitTest("testLoadApi4"));

    suite.addTest(new ItqlInterpreterUnitTest("testBackupApi1"));
    suite.addTest(new ItqlInterpreterUnitTest("testBackupApi2"));
    suite.addTest(new ItqlInterpreterUnitTest("testBackupApi3"));
    suite.addTest(new ItqlInterpreterUnitTest("testBackupApi4"));

    suite.addTest(new ItqlInterpreterUnitTest("testLoadBackupApi1"));

    suite.addTest(new ItqlInterpreterUnitTest("testRestoreApi1"));
    suite.addTest(new ItqlInterpreterUnitTest("testRestoreApi2"));
    suite.addTest(new ItqlInterpreterUnitTest("testRestoreApi3"));

    suite.addTest(new ItqlInterpreterUnitTest("testBackupRestore1"));
    suite.addTest(new ItqlInterpreterUnitTest("testBackupRestore2"));
    suite.addTest(new ItqlInterpreterUnitTest("testBackupRestore3"));

    return suite;
  }

  /**
   * Default text runner.
   *
   * @param args the command line arguments
   */
  public static void main(String[] args) {

    junit.textui.TestRunner.run(suite());
  }

  // suite()
  //
  // Test cases
  //

  /**
   * Test the interpreter using a help statement. Executes the following query:
   * <pre>
   * help ;
   * </pre> Expects results: <pre>
   *
   *   Valid commands are:
   *
   *     su       authenticate a user
   *     set      set a property
   *     execute  execute an iTQL script
   *     alias    define an alias
   *     create   create a model
   *     commit   commits a transaction
   *     drop     drop an entire resource
   *     insert   insert a set of triples
   *     delete   delete a set of triples
   *     load     load contents of a file
   *     backup   backup the contents of a server to a file
   *     restore  restore a server from a backup file
   *     rollback rolls back a transaction
   *     select   perform a query
   *     set      sets various options
   *     quit     end the ITQL session
   *     help     display this help screen
   *
   *   You can also get detailed help on each command:
   *
   *     $ help <command> ;
   *
   *   For example, to display help on the select command:
   *
   *     $ help select ;
   *
   *   Note. All commands must be terminated with ";".
   *
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testHelp() throws Exception {

    // log that we're executing the test
    log.debug("Starting help test");

    // create the statement
    String statement = "help ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);

    String results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    StringBuffer expected = new StringBuffer();
    expected.append(eol + "Valid commands are:" + eol + eol);
    expected.append("  su       authenticate a user" + eol);
    expected.append("  set      set a property" + eol);
    expected.append("  execute  execute an iTQL script" + eol);
    expected.append("  alias    define an alias" + eol);
    expected.append("  create   create a model" + eol);
    expected.append("  commit   commits a transaction" + eol);
    expected.append("  drop     drop an entire resource" + eol);
    expected.append("  insert   insert a set of triples" + eol);
    expected.append("  delete   delete a set of triples" + eol);
    expected.append("  load     load contents of a file          " + eol);
    expected.append("  backup   backup the contents of a server to a file" + eol);
    expected.append("  restore  restore a server from a backup file" + eol);
    expected.append("  rollback rolls back a transaction" + eol);
    expected.append("  select   perform a query" + eol);
    expected.append("  set      sets various options" + eol);
    expected.append("  apply    applies a set of rules" + eol);
    expected.append("  quit     end the ITQL session" + eol);
    expected.append("  help     display this help screen" + eol + eol);
    expected.append("You can also get detailed help on each command:" + eol + eol);
    expected.append("  $ help <command> ;" + eol + eol);
    expected.append("For example, to display help on the select command:" + eol + eol);
    expected.append("  $ help select ;" + eol + eol);
    expected.append("Note. All commands must be terminated with \";\"." + eol);

    // check that the result are what we expected
    assertEquals(expected.toString(), results);

    // log that we've completed the test
    log.debug("Completed help test");
  }

  /**
   * Test the interpreter using an insert statement. Executes the following query:
   * <pre>
   *   insert $s $p $o into &lt;rmi://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testInsert1() throws Exception {

    // log that we're executing the test
    log.debug("Starting insert test 1");

    String statement;

    // create the statement
    statement = "insert $s $p $o into <file://foo/bar.txt> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    String expected = "Could not insert statements into file://foo/bar.txt" +
        eol + "Predicate must be a valid URI";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed insert test 1");
  }

  /**
   * Test the interpreter using an insert statement. Executes the following query:
   * <pre>
   *   insert $s <urn:foo:bar> $o into &lt;rmi://localhost/database#model&gt; ;
   * </pre> Expects results: 1 inserted statements with the subject and object
   *   as a new blank node each and a fixed predicate.
   *
   * @throws Exception if the test fails
   */
  public void testInsert2() throws Exception {

    // log that we're executing the test
    log.debug("Starting insert test 1");

    String statement;

    // create the statement
    statement = "create <rmi://localhost/server1#database> ; " + eol +
        "insert $s <urn:foo:bar> $o into <rmi://localhost/server1#database> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    String expected = "Successfully inserted statements into rmi://localhost/server1#database";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log the results
    log.debug("Received results : " + results);

    statement = "drop <rmi://localhost/server1#database> ;";
    interpreter.executeCommand(statement);

    // log that we've completed the test
    log.debug("Completed insert test 2");
  }

  // testHelp()

  /**
   * Test the interpreter using a quit statement. Executes the following query:
   * <pre>
   *   quit ;
   * </pre> Expects results: <pre>
   *  Quitting ITQL session
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testQuit() throws Exception {

    // log that we're executing the test
    log.debug("Starting quit test");

    // create the statement
    String statement = "quit ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);

    String results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    String expected = "Quitting ITQL session";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log that we've completed the test
    log.debug("Completed quit test");
  }

  // testQuit()

  /**
   * Test the interpreter using an su statement. Executes the following query:
   * <pre>
   *   su fred Fo0B@r ;
   * </pre> Expects results: <pre>
   *   su is not currently implemented
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSu1() throws Exception {

    // log that we're executing the test
    log.debug("Starting su test 1");

    // create the statement
    String statement = "su <ldap://bar.org> fred Fo0Bar ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);

    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    String expected = "Credential presented";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log that we've completed the test
    log.debug("Completed su test 1");
  }

  // testSu1()

  /**
   * Test the interpreter using an alias statement. Executes the following
   * query: <pre>
   *   alias &lt;http://purl.org/dc/elements/1.1&gt; as dc;
   * </pre> Expects results: <pre>
   *  Successfully aliased http://purl.org/dc/elements/1.1 as dc
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testAlias1() throws Exception {

    // log that we're executing the test
    log.debug("Starting alias test 1");

    // create the statement
    String statement = "alias <http://purl.org/dc/elements/1.1> as dc ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    String results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    String expected =
        "Successfully aliased http://purl.org/dc/elements/1.1 " + "as dc";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log that we've completed the test
    log.debug("Completed alias test 1");
  }

  // testAlias1()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
   * select $x from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; where $x
   * &lt;http://www.w3.org/2000/01/rdf-schema#label&gt; 'Title' ; </pre> Expects
   * results: <pre>
   *   x=http://purl.org/dc/elements/1.1/title
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect1() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 1");

    // create the statement
    String statement =
        "select $x from <" + dcSchemaURI + "> where $x <" + rdfSchemaNamespace +
        "label> 'Title' ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = new AnswerImpl(interpreter.getLastAnswer());

    // log the results
    if (log.isDebugEnabled()) {
      log.debug("Received results : " + results);
    }

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x", new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/title")));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 1");
  }

  // testSelect1()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
   * select $x from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; where ($x
   * &lt;http://www.w3.org/2000/01/rdf-schema#label&gt; 'Title') ; </pre>
   * Expects results: <pre>
   *   x=http://purl.org/dc/elements/1.1/title
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect2() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 2");

    // create the statement
    String statement =
        "select $x from <" + dcSchemaURI + "> where ($x <" +
        rdfSchemaNamespace + "label> 'Title') ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = interpreter.getLastAnswer();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x", new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/title")));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 2");
  }

  // testSelect2()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
   * select $x $y from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; where ($x
   * $y 'Subject'); </pre> Expects results: <pre>
   *   x=http://purl.org/dc/elements/1.1/subject
   *   y=http://www.w3.org/2000/01/rdf-schema#label
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect3() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 3");

    // create the statement
    String statement =
        "select $x $y from <" + dcSchemaURI + "> where ($x $y 'Subject') ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = interpreter.getLastAnswer();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x", "y"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x",
                  new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y",
                  new URIReferenceImpl(new URI(
        "http://www.w3.org/2000/01/rdf-schema#label")));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 3");
  }

  // testSelect3()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
       * select $x $y from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; where (($x
   * $y 'Subject') and ($x &lt;http://www.w3.org/2000/01/rdf-schema#label&gt;
   * 'Subject')) ; </pre> Expects results: <pre>
   *   x=http://purl.org/dc/elements/1.1/subject
   *   y=http://www.w3.org/2000/01/rdf-schema#label
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect4() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 4");

    // create the statement
    String statement =
        "select $x $y from <" + dcSchemaURI +
        "> where (($x $y 'Subject') and ($x <" + rdfSchemaNamespace +
        "label> 'Subject')) ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = interpreter.getLastAnswer();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x", "y"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x", new URIReferenceImpl(
        new URI("http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y", new URIReferenceImpl(new URI(
        "http://www.w3.org/2000/01/rdf-schema#label")));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 4");
  }

  // testSelect4()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
   * select $x $y from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; where (($x
   * $y 'Subject') or ($x &lt;http://www.w3.org/2000/01/rdf-schema#label&gt;
   * 'Subject')) ; </pre> Expects results: <pre>
   *   x=http://purl.org/dc/elements/1.1/subject
   *   y=http://www.w3.org/2000/01/rdf-schema#label
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect5() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 5");

    // create the statement
    String statement =
        "select $x $y from <" + dcSchemaURI +
        "> where (($x $y 'Subject') or ($x <" + rdfSchemaNamespace +
        "label> 'Subject')) ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = new AnswerImpl(interpreter.getLastAnswer());

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x", "y"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x", new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y", null);
    row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x", new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y", new URIReferenceImpl(new URI(
        "http://www.w3.org/2000/01/rdf-schema#label")));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 5");
  }

  // testSelect5()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
   * select $x $y from <file:<it>mulgarahome</it> /data/dc.rdfs&gt; where (($x $y
   * 'Subject') and ($x &lt;http://www.w3.org/2000/01/rdf-schema#label&gt;
   * 'Subject')) and &lt;http://purl.org/dc/elements/1.1/subject&gt; $y
   * 'Subject' ; </pre> Expects results: <pre>
   *   x=http://purl.org/dc/elements/1.1/subject
   *   y=http://www.w3.org/2000/01/rdf-schema#label
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect6() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 6");

    // create the statement
    String statement =
        "select $x $y from <" + dcSchemaURI +
        "> where (($x $y 'Subject') and ($x <" + rdfSchemaNamespace +
        "label> 'Subject'))" +
        "and <http://purl.org/dc/elements/1.1/subject> $y 'Subject' ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = interpreter.getLastAnswer();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x", "y"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x",
                  new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y",
                  new URIReferenceImpl(new URI(
        "http://www.w3.org/2000/01/rdf-schema#label")));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 6");
  }

  // testSelect6()

  /**
   * Test the interpreter using a select statement. Executes the following
   * query: <pre>
   * select $x $y from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; where (($x
   * $y 'Subject') and ($x &lt;http://www.w3.org/2000/01/rdf-schema#label&gt;
   * 'Subject')) or &lt;http://purl.org/dc/elements/1.1/subject&gt; $y 'Subject'
   * ; </pre> Expects results: <pre>
   *   x=http://purl.org/dc/elements/1.1/subject
   *   y=http://www.w3.org/2000/01/rdf-schema#label
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect7() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 7");

    // create the statement
    String statement =
        "select $x $y from <" + dcSchemaURI +
        "> where (($x $y 'Subject') and ($x <" + rdfSchemaNamespace +
        "label> 'Subject'))" +
        "or <http://purl.org/dc/elements/1.1/subject> $y 'Subject' ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = interpreter.getLastAnswer();

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x", "y"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x",
                  new URIReferenceImpl(new URI(
        "http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y",
                  new URIReferenceImpl(new URI(
        "http://www.w3.org/2000/01/rdf-schema#label")));

    Answer expected = new AnswerImpl(trs);
/*
    expected.beforeFirst();
    results.beforeFirst();
    while (true) {
      boolean expN = expected.next();
      boolean resN = results.next();
      if (expN && resN) {
        log.warn("exp x = " + expected.getObject("x") + " exp y = " + expected.getObject("y") +
            " res x = " + results.getObject("x") + " res y = " + results.getObject("y"));
      } else if (!expN && !resN) {
        log.warn("Finished dumping results");
        break;
      } else if (resN) {
        do {
          log.warn("extra result: res x = " + results.getObject("x") + " res y = " + results.getObject("y"));
        } while (results.next());
      } else {
        log.warn("expN = " + expN + " resN = " + resN);
        break;
      }
    }
*/

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 7");
  }

  // testSelect7()

  /**
   * Test the interpreter using a select statement with a count and a having.
   * Executes the following query: <pre>
   * select count (
   *   select $x $y
   *   from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt;
   *   where (($x $y 'Subject') and ($x &lt;http://www.w3.org/2000/01/rdf-schema#label&gt; 'Subject'))
   * from &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt;
   * where (($x $y 'Subject') and ($x &lt;http://www.w3.org/2000/01/rdf-schema#label&gt; 'Subject')) ;
   * </pre> Expects results: <pre>
   *   k0=http://purl.org/dc/elements/1.1/subject
   *   y=http://www.w3.org/2000/01/rdf-schema#label
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSelect8() throws Exception {

    // log that we're executing the test
    log.debug("Starting select test 8");

    // create the statement
    String statement =
        "select $x $y count ( " +
        "  select $x $y " +
        "  from <" + dcSchemaURI + "> " +
        "  where (($x $y 'Subject') " +
        "  and ($x <" + rdfSchemaNamespace + "label> 'Subject'))) " +
        "from <" + dcSchemaURI + "> " +
        "where (($x $y 'Subject') " +
        "and ($x <" + rdfSchemaNamespace + "label> 'Subject')) " +
        "having $k0 <http://tucana.org/tucana#occurs> '1.0'^^<http://www.w3.org/2001/XMLSchema#double> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    interpreter.executeCommand(statement);
    Answer results = new AnswerImpl(interpreter.getLastAnswer());

    // log the results
    log.debug("Received results : " + results);

    // compose the expected result
    TestResultSet trs = new TestResultSet(new String[] {"x", "y", "k0"});
    ResultSetRow row = new ResultSetRow(trs);
    trs.addRow(row);
    row.setObject("x", new URIReferenceImpl(
        new URI("http://purl.org/dc/elements/1.1/subject")));
    row.setObject("y", new URIReferenceImpl(
        new URI("http://www.w3.org/2000/01/rdf-schema#label")));
    row.setObject("k0", new LiteralImpl(1.0d));

    Answer expected = new AnswerImpl(trs);

    // check that the result are what we expected
    assertEquals(expected, results);

    results.close();
    expected.close();

    // log that we've completed the test
    log.debug("Completed select test 7");
  }

  // testSelect8()

  /**
   * Test the interpreter using a delete statement. Executes the following query:
   * <pre>
   *   delete $s $p $o from &lt;rmi://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testDelete1() throws Exception {

    // log that we're executing the test
    log.debug("Starting delete test 1");

    String statement;

    // create the statement
    statement = "delete $s $p $o from <file://foo/bar.txt> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    String expected = "Could not delete statements from file://foo/bar.txt" +
        eol + "Predicate must be a valid URI";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed drop test 1");
  }

  /**
   * Test the interpreter using a delete statement. Executes the following query:
   * <pre>
   *   delete $s <urn:foo:bar> $o from &lt;rmi://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testDelete2() throws Exception {

    // log that we're executing the test
    log.debug("Starting delete test 2");

    String statement;

    // create the statement
    statement = "delete $s <urn:foo:bar> $o from <file://foo/bar.txt> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    String expected = "Could not delete statements from file://foo/bar.txt" +
       eol + "Cannot use variables when deleting statements";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed drop test 2");
  }

  /**
   * Test the interpreter using a directory statement. Executes the following
   * query: <pre>
   *   directory &lt;beep://rns.site1.net:7000/models&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testDirectory1() throws Exception {

    // log that we're executing the test
    log.debug("Starting directory DB test 1");

    // create the statement
    String statement = "directory <beep://rns.site1.net:7000/models> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed directory test 1");
  }

  // testDirectory1()

  /**
   * Test the interpreter using a create statement. Executes the following
   * query: <pre>
   *   create &lt;tucana://localhost/database&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testCreate1() throws Exception {

    // log that we're executing the test
    log.debug("Starting create test 1");

    // create the statement
    String statement = "create <tucana://localhost/database> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed create test 1");
  }

  // testCreate1()

  /**
   * Test the interpreter using a create statement. Executes the following
   * query: <pre>
   *   create &lt;tucana://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testCreate2() throws Exception {

    // log that we're executing the test
    log.debug("Starting create test 2");

    // create the statement
    String statement = "create <tucana://localhost/database#model> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed create test 2");
  }

  // testCreate2()

  /**
   * Test the interpreter using a drop statement. Executes the following query:
   * <pre>
   *   drop &lt;tucana://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testDrop1() throws Exception {

    // log that we're executing the test
    log.debug("Starting drop test 1");

    // create the statement
    String statement = "drop <tucana://localhost/database> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed drop test 1");
  }

  // testDrop1()

  /**
   * Test the interpreter using a drop statement. Executes the following query:
   * <pre>
   *   drop &lt;tucana://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testDrop2() throws Exception {

    // log that we're executing the test
    log.debug("Starting drop test 2");

    // create the statement
    String statement = "drop <tucana://localhost/database#model> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed drop test 2");
  }

  // testDrop2()
  // Proposed tests
  //
  //insert unbraced triple into database
  //insert unbraced triple into model
  //insert braced triple into database
  //insert braced triple into model
  //insert unbraced database into database
  //insert unbraced database into model
  //insert unbraced model into database
  //insert unbraced model into model
  //insert braced database into database
  //insert braced database into model
  //insert braced model into database
  //insert braced model into model
  //insert unbraced select into database
  //insert unbraced select into model
  //insert braced select into database
  //insert braced select into model
  // insert 'title' 'title' 'title' into rmi://localhost/server1#insert ;
  // -> this is a bad triple as it contains a literal as it's subject and predicate
  //
  // alias http://www.w3.org/2000/01/rdf-schema# as rdfs ;
  // insert http://purl.org/dc/elements/1.1/language rdfs:label 'Language' into rmi://localhost/server1#insert ;
  //
  // or equivalently
  //
  // insert http://purl.org/dc/elements/1.1/language http://www.w3.org/2000/01/rdf-schema#label 'Language' into rmi://localhost/server1#insert ;
  //
  // iTQL> create rmi://localhost/server1#foo ;
  // Successfully created model rmi://localhost/server1#foo
  // iTQL> select $x $y $z from rmi://localhost/server1#foo where $x $y $x ;
  // 2 columns: x y (0 rows)
  // iTQL> insert http://purl.org/dc/elements/1.1/title http://www.w3.org/2000/01/rdf-schema#label 'Title' into rmi://localhost/server1#foo ;
  // Successfully inserted statements into rmi://localhost/server1#foo
  // iTQL> select $x $y $z from rmi://localhost/server1#foo where $x $y $x ;
  // 2 columns: x y (1 rows)
  //         x="Title"       y=http://www.w3.org/2000/01/rdf-schema#label
  // iTQL> drop rmi://localhost/server1#foo ;
  // Successfully dropped model rmi://localhost/server1#foo
  // Proposed tests
  //
  //delete unbraced triple into database
  //delete unbraced triple into model
  //delete braced triple into database
  //delete braced triple into model
  //delete unbraced database into database
  //delete unbraced database into model
  //delete unbraced model into database
  //delete unbraced model into model
  //delete braced database into database
  //delete braced database into model
  //delete braced model into database
  //delete braced model into model
  //delete unbraced select into database
  //delete unbraced select into model
  //delete braced select into database
  //delete braced select into model

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load &lt;http://purl.org/dc/elements/1.1&gt; into
   *       &lt;tucana://localhost/database#model&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad1() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 1");

    // create the statement
    String statement =
        "load <http://purl.org/dc/elements/1.1> into <" + testModel + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 1");
  }

  // testLoad1()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; into
   * &lt;tucana://localhost/database&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad2() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 2");

    // create the statement
    String statement =
        "load <" + dcSchemaURI + "> into <" + server + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 2");
  }

  // testLoad2()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; into
   * &lt;tucana://localhost/database#model&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad3() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 3");

    // create the statement
    String statement = "load <" + dcSchemaURI + "> into <" + testModel + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 3");
  }

  // testLoad3()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load as rdf &lt;http://<it>dchost</it> /<it>path</it> /dc.rdfs&gt; into
   * &lt;tucana://localhost/database#model&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad4() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 4");

    // create the statement
    String statement =
        "load <http://purl.org/dc/elements/1.1> into <" + testModel + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 4");
  }

  // testLoad4()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load as rdf &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; into
   * &lt;tucana://localhost/database&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad5() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 5");

    // create the statement
    String statement =
        "load <" + dcSchemaURI + "> into <" + server + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 5");
  }

  // testLoad5()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load as rdf &lt;file:<it>mulgarahome</it> /data/dc.rdfs&gt; into
   * &lt;tucana://localhost/database#model&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad6() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 6");

    // create the statement
    String statement =
        "load <" + dcSchemaURI + "> into <" + testModel + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 6");
  }

  // testLoad6()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load as serial &lt;http://<it>dchost</it> /<it>path</it> /<it>database
   * </it>&gt; into &lt;tucana://localhost/database#model&gt; ; </pre> Expects
   * results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad7() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 7");

    // create the statement
    String statement =
        "load <http://purl.org/dc/elements/1.1> into <" + testModel +
        "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 7");
  }

  // testLoad7()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load as serial &lt;file:<it>mulgarahome</it> /data/<it>database</it> &gt;
   * into &lt;tucana://localhost/database&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad8() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 8");

    // create the statement
    String statement =
        "load <" + dcSchemaURI + "> into <" + server + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 8");
  }

  // testLoad8()

  /**
   * Test the interpreter using a load statement. Executes the following query:
   * <pre>
   *   load as serial &lt;file:<it>mulgarahome</it> /<it>database</it> &gt; into
   * &lt;tucana://localhost/database#model&gt; ; </pre> Expects results:
   * ParserException
   *
   * @throws Exception if the test fails
   */
  public void testLoad9() throws Exception {

    // log that we're executing the test
    log.debug("Starting load test 9");

    // create the statement
    String statement =
        "load <" + dcSchemaURI + "> into <" + testModel + "> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed load test 9");
  }

  // testLoad9()
  // Proposed tests
  //
  //dump unbraced triple into file:<filename>
  //dump braced triple into file:<filename>
  //dump unbraced database into file:<filename>
  //dump unbraced model into file:<filename>
  //dump braced database into file:<filename>
  //dump braced model into file:<filename>
  //dump unbraced select into file:<filename>
  //dump braced select into file:<filename>
  //dump unbraced triple into file:<filename> as rdf
  //dump braced triple into file:<filename> as rdf
  //dump unbraced database into file:<filename> as rdf
  //dump unbraced model into file:<filename> as rdf
  //dump braced database into file:<filename> as rdf
  //dump braced model into file:<filename> as rdf
  //dump unbraced select into file:<filename> as rdf
  //dump braced select into file:<filename> as rdf
  //dump unbraced triple into file:<filename> as serial
  //dump braced triple into file:<filename> as serial
  //dump unbraced database into file:<filename> as serial
  //dump unbraced model into file:<filename> as serial
  //dump braced database into file:<filename> as serial
  //dump braced model into file:<filename> as serial
  //dump unbraced select into file:<filename> as serial
  //dump braced select into file:<filename> as serial

  /**
   * Test the interpreter using a set statement. Executes the following query:
   * <pre>
   *   set time on ;
   * </pre> Expects results: <pre>
   *   Command timing on
   *   Command execution time - XXX.XXX seconds
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSet1() throws Exception {

    // log that we're executing the test
    log.debug("Starting set test 1");

    // create the statement
    String statement = "set time on ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // compose expected result
    String expected = "Command timing is on" + eol + "Command execution time - ";

    // check that the result are what we expected (we cannot know the
    // execution time)
    assertEquals(true, results.startsWith(expected));

    // log that we've completed the test
    log.debug("Completed set test 1");
  }

  // testSet1()

  /**
   * Test the interpreter using a set statement. Executes the following query:
   * <pre>
   *   set time off ;
   * </pre> Expects results: <pre>
   *   Command timing off
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testSet2() throws Exception {

    // log that we're executing the test
    log.debug("Starting set test 2");

    // create the statement
    String statement = "set time off ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // compose expected result
    String expected = "Command timing is off";

    // check that the result are what we expected
    assertEquals(expected, results);

    // log that we've completed the test
    log.debug("Completed set test 2");
  }

  // testSet2()

  /**
   * Test the interpreter using a backup statement. Executes the following
   * query: <pre>
   *    backup <rmi://localhost/server1> to <file:/tmp/foobackup1.gz> ;
   * </pre> Expects results: <pre>
   *    TODO
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testBackup1() throws Exception {

    // log that we're executing the test
    log.debug("Starting backup test 1");

    // create the statement
    File backupFile = new File(tmpDirectory, "server1backup.gz");
    String statement =
        "backup <"+server+"> to <" + backupFile.toURI() + ">;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log that we've completed the test
    log.debug("Completed backup test 1");
  }

  // testBackup1()

  /**
   * Test the interpreter using a restore statement. Executes the following
   * query: <pre>
   *    restore <rmi://localhost/server1> from <file:/tmp/foobackup1.gz> ;
   * </pre> Expects results: <pre>
   *    TODO
   * </pre>
   *
   * @throws Exception if the test fails
   */
  public void testRestore1() throws Exception {

    // log that we're executing the test
    log.debug("Starting restore test 1");

    // create the statement
    File backupFile = new File(tmpDirectory, "server1backup.gz");
    String statement =
        "restore <"+server+"> from <" + backupFile.toURI() + ">;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log that we've completed the test
    log.debug("Completed restore test 1");
  }

  // testRestore1()

  /**
   * Tests the backup and restore functionality of Mulgara by running a query,
   * backing up the DB, deleting the server.
   *
   * @throws Exception if the test fails
   */
  public void testBackupRestore1() throws Exception {

    // log that we're executing the test
    log.debug("Starting combined backup-restore test 1");

    // TODO - perform the backup
    // create the statement
    File backupFile = new File(tmpDirectory, "server1backup.gz");
    String statement =
        "restore <"+server+"> from <" + backupFile.toURI() + ">;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log that we've completed the test
    log.debug("Completed combined backup-restore test 1");
  }

  // testBackupRestore1()

  /**
   * Tests the backup and restore functionality of Mulgara by running a query,
   * backing up the DB, deleting the server.
   *
   * @throws Exception if the test fails
   */
  public void testBackupRestore2() throws Exception {

    // log that we're executing the test
    log.debug("Starting combined backup-restore test 2");

    // TODO - perform the backup
    // create the statement
    File backupFile = new File(tmpDirectory, "server1backup.gz");
    String statement =
        "restore <"+server+"> from local <" + backupFile.toURI() + ">;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log that we've completed the test
    log.debug("Completed combined backup-restore test 2");
  }

  /**
   * Tests the backup and restore functionality of Mulgara by running a query,
   * backing up the DB, deleting the server.
   *
   * @throws Exception if the test fails
   */
  public void testBackupRestore3() throws Exception {

    // log that we're executing the test
    log.debug("Starting combined backup-restore test 3");

    // TODO - perform the backup
    // create the statement
    File backupFile = new File(tmpDirectory, "server1backup.gz");
    String statement =
        "restore <"+server+"> from remote <" + backupFile.toURI() + ">;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    // execute the query
    String results;
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log that we've completed the test
    log.debug("Completed combined backup-restore test 3");
  }



  /**
   * Test #1 for {@link ItqlInterpreter#parseQuery}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void test1ParseQuery() throws Exception {

    // Compose the expected result
    Query expected = new Query(
        Arrays.asList(new Variable[] {
        new Variable("x")}),                   // variable list
        new ModelResource(new URI("x:m")),     // model expression
        new ConstraintImpl(new Variable("x"),  // constraint expression
        new URIReferenceImpl(new URI("x:p")),
        new LiteralImpl("o")),
        null,                                  // no having
        Collections.EMPTY_LIST,                // no ordering
        null,                                  // no limit
        0,                                     // zero offset
        new UnconstrainedAnswer());

    // Check that the result is as expected
    assertEquals(expected,
                 interpreter.parseQuery(
        "select $x from <x:m> where $x <x:p> 'o';"));
  }

  /**
   * Test #2 for {@link ItqlInterpreter#parseQuery}. Non-queries should return a
   * {@link ParserException}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void test2ParseQuery() throws Exception {

    try {

      String notAQuery = "quit";
      Query query = interpreter.parseQuery(notAQuery);
      fail("\"" + notAQuery + "\" erroneously parsed as \"" + query + "\"");
    }
    catch (ParserException e) {

      // this is the correct response
    }
  }

  /**
   * Test #3 for {@link ItqlInterpreter#parseQuery}. This tests that the limit
   * and offset can be assigned.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void test3ParseQuery() throws Exception {

    // Compose the expected result
    Query expected = new Query(
        Arrays.asList(new Variable[] {
        new Variable("x")}),                   // variable list
        new ModelResource(new URI("x:m")),     // model expression
        new ConstraintImpl(new Variable("x"),  // constraint expression
        new URIReferenceImpl(new URI("x:p")),
        new LiteralImpl("o")),
        null,                                  // no having
        Collections.EMPTY_LIST,                // no ordering
        new Integer(10),                       // limit
        2,                                     // offset
        new UnconstrainedAnswer());

    // Check that the result is as expected
    assertEquals(expected,
                 interpreter.parseQuery(
        "select $x from <x:m> where $x <x:p> 'o' limit 10 offset 2;"));
  }

  /**
   * Test #3 for {@link ItqlInterpreter#parseQuery}. This tests the having
   * expression.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void test4ParseQuery() throws Exception {

    // Compose the expected result

    List varList = Arrays.asList(new Variable[] { new Variable("x") });
    ModelExpression model = new ModelResource(new URI("x:m"));
    ConstraintExpression where =  new ConstraintImpl(
        new Variable("x"),
        new URIReferenceImpl(new URI("x:p")),
        new LiteralImpl("o"));
    ConstraintHaving having = new ConstraintOccurs(new Variable("x"),
        new LiteralImpl(2d));

    Query expected = new Query(
        varList,                               // variable list
        model,                                 // model expression
        where,                                 // constraint expression
        having,                                // a simple having
        Collections.EMPTY_LIST,                // no ordering
        new Integer(10),                       // limit
        2,                                     // offset
        new UnconstrainedAnswer());

    // Check that the result is as expected
    assertEquals(expected, interpreter.parseQuery("select $x from <x:m> " +
        "where $x <x:p> 'o' " +
        "having $x <http://tucana.org/tucana#occurs> " +
        "'2.0'^^<http://www.w3.org/2001/XMLSchema#double> limit 10 offset 2;"));
  }

  // ItqlInterpreterUnitTest()
  //
  // Test configuration
  //

  /**
   * Initialise members.
   *
   * @throws Exception if something goes wrong
   */
  protected void setUp() throws Exception {

    // initialise the interpreter if needed
    if (this.interpreter == null) {

      /*
      SessionFactory sessionFactory =
        SessionFactoryFinder.newSessionFactory(null);
      */

      this.interpreter = new ItqlInterpreter(
        /*
        sessionFactory.newSession(),
        sessionFactory.getSecurityDomain(),
        */
        new HashMap()
      );
    }

    // end if
    // initialise the dublin core schema test data if needed
    if (dcSchemaURI == null) {

      dcSchemaURI =
          new URI(new File(System.getProperty("cvs.root") + "/data/dc.rdfs").
                  toURL()
                  .toString());
    }

    // end if
    // initialise the test model if needed
    if (testModel == null) {

      testModel = server+"#itqlmodel2";
    }

    // end if
  }

  /**
   * Test the interpreter using a create statement. Executes the following
   * query: <pre>
   *   create &lt;tucana://localhost/database&gt; ;
   * </pre> Expects results: ParserException
   *
   * @throws Exception if the test fails
   */
  public void testCreate3() throws Exception {

    // log that we're executing the test
    log.debug("Starting create test 3");

    // create the statement
    String statement = "create <"+ testModel +"> ;";

    // log the query we'll be sending
    log.debug("Executing statement : " + statement);

    String results = "";

    // execute the query
    interpreter.executeCommand(statement);
    results = interpreter.getLastMessage();

    // log the results
    log.debug("Received results : " + results);

    // log that we've completed the test
    log.debug("Completed create test 3");
  }

  // testCreate1()


  /**
   * Test the interpreter using a load API remotely .
   *
   * @throws Exception if the test fails
   */
  public void testLoadApi1() throws Exception {

    // log that we're executing the test
    log.debug("Starting load API test 1");

    URI sourceURI = new URI("http://purl.org/dc/elements/1.1");
    URI modelURI = new URI(testModel);

    // execute the load remotely
    long statements = interpreter.load(null, sourceURI, modelURI);

    this.assertEquals("Incorrect number of statements inserted", 146, statements);
  }

  // testLoadApi1()

  /**
   * Test the interpreter using a load API locally
   *
   * @throws Exception if the test fails
   */
  public void testLoadApi2() throws Exception {

    // log that we're executing the test
    log.debug("Starting load API test 2");

    URI sourceURI = new URI("http://purl.org/dc/elements/1.1");
    URI modelURI = new URI(testModel);

    // open and wrap the inputstream
    RemoteInputStreamSrvImpl srv =
        new RemoteInputStreamSrvImpl(sourceURI.toURL().openStream());

    // prepare it for exporting
    UnicastRemoteObject.exportObject(srv);

    RemoteInputStream inputStream = new RemoteInputStream(srv);

    // execute the load locally
    long statements = interpreter.load(inputStream,
                                       sourceURI, modelURI);

    this.assertEquals("Incorrect number of statements inserted", 146, statements);

    inputStream.close();

  }

  // testLoadApi2()

  /**
   * Test the interpreter using a load API locally
   *
   * @throws Exception if the test fails
   */
  public void testLoadApi3() throws Exception {

    // log that we're executing the test
    log.debug("Starting load API test 3");

    URI sourceURI = new URI("http://purl.org/dc/elements/1.1");
    URI dummyURI = new URI("http://mydummysite.com/rssfeed.rdf");
    URI modelURI = new URI(testModel);

    // execute the load locally - pass an invalid URI for the server.
    // This should succeed as the server will ignore the dummyURI
    long statements = interpreter.load(sourceURI.toURL().openStream(),
                                       dummyURI, modelURI);

    this.assertEquals("Incorrect number of statements inserted", 146, statements);

  }

  /**
   * Test the interpreter using a load API locally
   *
   * @throws Exception if the test fails
   */
  public void testLoadApi4() throws Exception {

    // log that we're executing the test
    log.debug("Starting load API test 4");

    URI sourceURI = new URI("http://purl.org/dc/elements/1.1");
    URI modelURI = new URI(testModel);

    // execute the load locally
    long statements = interpreter.load(sourceURI.toURL().openStream(),
                                       modelURI);

    this.assertEquals("Incorrect number of statements inserted", 146, statements);

  }

  /**
    * Test the interpreter using a backup API remotely .
    *
    * @throws Exception if the test fails
    */
   public void testBackupApi1() throws Exception {

     // log that we're executing the test
     log.debug("Starting backup API test 1");

     File file = new File(tmpDirectory, "backup1.rdf");
     file.delete();

     URI modelURI = new URI(testModel);

     // execute the backup remotely
     interpreter.backup(modelURI, file);

     this.assertTrue("Expected a backup file", file.exists());
   }

   // testbackupApi1()

   /**
    * Test the interpreter using a backup API locally
    *
    * @throws Exception if the test fails
    */
   public void testBackupApi2() throws Exception {

     // log that we're executing the test
     log.debug("Starting backup API test 2");

     File file = new File(tmpDirectory, "backup2.rdf");
     file.delete();

     URI modelURI = new URI(testModel);

     // execute the backup remotely
     interpreter.backup(modelURI, new FileOutputStream(file));

     this.assertTrue("Expected a backup file", file.exists());

   }

   // testbackupApi2()

   /**
    * Test the interpreter using a backup API locally
    *
    * @throws Exception if the test fails
    */
   public void testBackupApi3() throws Exception {

     // log that we're executing the test
     log.debug("Starting backup API test 3");

     File file = new File(tmpDirectory, "backup1.gz");
     file.delete();

     URI serverURI = new URI(server);

     // execute the backup locally
     interpreter.backup(serverURI, file);

     this.assertTrue("Expected a backup file", file.exists());

   }

   /**
    * Test the interpreter using a backup API locally
    *
    * @throws Exception if the test fails
    */
   public void testBackupApi4() throws Exception {

     // log that we're executing the test
     log.debug("Starting backup API test 3");

     File file = new File(tmpDirectory, "backup2.gz");
     file.delete();

     URI serverURI = new URI(server);

     // execute the backup remotely
     interpreter.backup(serverURI, new FileOutputStream(file));

     this.assertTrue("Expected a backup file", file.exists());

   }

   /**
    * Test the interpreter using a Load API locally
    *
    * @throws Exception if the test fails
    */
   public void testLoadBackupApi1() throws Exception {

     // log that we're executing the test
     log.debug("Starting load a backup API test 1");

     File file = new File(tmpDirectory, "backup1.rdf");

     URI modelURI = new URI(testModel);

     // execute the backup remotely
     long statements = interpreter.load(file.toURL().openStream(), modelURI);

     this.assertEquals("Incorrect number of statements inserted", 146, statements);

   }


   /**
    * Test the interpreter using a restore API locally
    *
    * @throws Exception if the test fails
    */
   public void testRestoreApi1() throws Exception {

     // log that we're executing the test
     log.debug("Starting Restore API test 1");

     File file = new File(tmpDirectory, "backup1.gz");

     URI serverURI = new URI(server);

     // execute the backup remotely
     interpreter.restore(new FileInputStream(file), serverURI);

   }

   /**
    * Test the interpreter using a restore API remotely
    *
    * @throws Exception if the test fails
    */
   public void testRestoreApi2() throws Exception {

     // log that we're executing the test
     log.debug("Starting Restore API test 2");

     File file = new File(tmpDirectory, "backup1.gz");

     URI serverURI = new URI(server);

     // execute the backup remotely
     interpreter.restore(null, serverURI, file.toURI());

   }

   /**
    * Test the interpreter using a restore API locally
    *
    * @throws Exception if the test fails
    */
   public void testRestoreApi3() throws Exception {

     // log that we're executing the test
     log.debug("Starting Restore API test 3");

     File file = new File(tmpDirectory, "backup2.gz");

     // provide a not existence file.
     // this should not fail as the input stream is used
     File dummy = new File(tmpDirectory, "dummyfile.gz");

     URI serverURI = new URI(server);

     // execute the backup remotely
     interpreter.restore(file.toURL().openStream(), serverURI, dummy.toURI());

   }


  // main()
}
