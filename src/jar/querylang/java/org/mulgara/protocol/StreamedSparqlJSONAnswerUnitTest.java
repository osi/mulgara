/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */

package org.mulgara.protocol;


// JUnit
import junit.framework.*;

// Java 2 standard packages
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Locally written packages
import org.mulgara.query.AnswerImpl;
import org.mulgara.query.BooleanAnswer;
import org.mulgara.query.Variable;
import org.mulgara.query.rdf.BlankNodeImpl;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.util.ResultSetRow;
import org.mulgara.util.TestResultSet;

/**
 * Test case for {@link StreamedSparqlJSONAnswer}.
 *
 * @created Sep 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class StreamedSparqlJSONAnswerUnitTest extends TestCase {

  /**
   * Test instance.
   * <table>
   *   <thead>
   *     <tr><th>x</th> <th>y</th></tr>
   *   </thead>
   *   <tbody>
   *     <tr><td>X1</td><td>Y1</td></tr>
   *     <tr><td>X2</td><td>Y2</td></tr>
   *   </tbody>
   * </table>
   */
  private AnswerImpl answer;

  private AnswerImpl empty;

  private ByteArrayOutputStream output;

  private ByteArrayOutputStream outputb;

  private ByteArrayOutputStream outputa;

  private static final URI REL_URI = URI.create("rel/uri");
  
  /**
   * Constructs a new answer test with the given name.
   * @param name the name of the test
   */
  public StreamedSparqlJSONAnswerUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   * @return The test suite
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();
    // TODO: tests not yet written
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testEmptyConstructor"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testEmptyConstructorPretty"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testBooleanAnswer"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testBooleanAnswerPretty"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testPrettyPrint"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testCompactPrint"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testPrettyPrintVariations"));
//    suite.addTest(new StreamedSparqlJSONAnswerUnitTest("testCompactPrintVariations"));
    return suite;
  }

  /**
   * Default text runner.
   * @param args The command line arguments
   */
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }


  /**
   * Tests an empty answer.
   * @throws Exception On error
   */
  public void testEmptyConstructor() throws Exception {
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(empty, output);
    a.emit();
    assertEquals(getEmpty(), output.toString());
  }


  /**
   * Tests an empty answer.
   * @throws Exception On error
   */
  public void testEmptyConstructorPretty() throws Exception {
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(empty, output);
    a.emit();
    assertEquals(getEmptyP(), output.toString());

    output.reset();
    a.emit();
    assertEquals(getEmpty(), output.toString());
  }


  /**
   * Tests a boolean answer.
   * @throws Exception On error
   */
  public void testBooleanAnswer() throws Exception {
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(true, output);
    a.emit();
    assertEquals(getTrue(), output.toString());

    a = new StreamedSparqlJSONAnswer(new BooleanAnswer(true), output);
    output.reset();
    a.emit();
    assertEquals(getTrue(), output.toString());
  }


  /**
   * Tests a boolean answer.
   * @throws Exception On error
   */
  public void testBooleanAnswerPretty() throws Exception {
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(true, output);
    a.emit();
    assertEquals(getTrueP(), output.toString());
    output.reset();
    a.emit();
    assertEquals(getTrue(), output.toString());
  }


  /**
   * Test main structure.
   */
  public void testPrettyPrint() throws Exception {
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(answer, output);
    a.emit();
    assertEquals(getAnswerP(), output.toString());
  }


  /**
   * Test main structure.
   */
  public void testCompactPrint() throws Exception {
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(answer, output);
    a.emit();
    assertEquals(getAnswer(), output.toString());
  }


  /**
   * Tests the variations of answers.
   * @throws Exception On error
   */
  public void testCompactPrintVariations() throws Exception {
    StreamedSparqlJSONAnswer e = new StreamedSparqlJSONAnswer(empty, REL_URI, output);
    StreamedSparqlJSONAnswer b = new StreamedSparqlJSONAnswer(true, REL_URI, outputb);
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(answer, REL_URI, outputa);

    // No namespaces, no schema, meta set
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(false, true), output.toString());
    assertEquals(getTrue(false, true), outputb.toString());
    assertEquals(getAnswer(false, true), outputa.toString());

    // No namespaces, schema set, meta set
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(true, true), output.toString());
    assertEquals(getTrue(true, true), outputb.toString());
    assertEquals(getAnswer(true, true), outputa.toString());

    // Namespaces set, schema set, meta set
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(true, true), output.toString());
    assertEquals(getTrue(true, true), outputb.toString());
    assertEquals(getAnswer(true, true), outputa.toString());

    // Namespaces set, no schema, meta set
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(false, true), output.toString());
    assertEquals(getTrue(false, true), outputb.toString());
    assertEquals(getAnswer(false, true), outputa.toString());
    
    e = new StreamedSparqlJSONAnswer(empty, output);
    b = new StreamedSparqlJSONAnswer(true, outputb);
    a = new StreamedSparqlJSONAnswer(answer, outputa);

    // No namespaces, schema set, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(true, false), output.toString());
    assertEquals(getTrue(true, false), outputb.toString());
    assertEquals(getAnswer(true, false), outputa.toString());

    // Namespaces set, schema set, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(true, false), output.toString());
    assertEquals(getTrue(true, false), outputb.toString());
    assertEquals(getAnswer(true, false), outputa.toString());

    // Namespaces set, no schema, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(false, false), output.toString());
    assertEquals(getTrue(false, false), outputb.toString());
    assertEquals(getAnswer(false, false), outputa.toString());

    // No Namespaces, no schema, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(false, false), output.toString());
    assertEquals(getTrue(false, false), outputb.toString());
    assertEquals(getAnswer(false, false), outputa.toString());

    // No namespaces, schema set, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmpty(true, false), output.toString());
    assertEquals(getTrue(true, false), outputb.toString());
    assertEquals(getAnswer(true, false), outputa.toString());
  }


  /**
   * Tests the variations of answers.
   * @throws Exception On error
   */
  public void testPrettyPrintVariations() throws Exception {
    StreamedSparqlJSONAnswer e = new StreamedSparqlJSONAnswer(empty, REL_URI, output);
    StreamedSparqlJSONAnswer b = new StreamedSparqlJSONAnswer(true, REL_URI, outputb);
    StreamedSparqlJSONAnswer a = new StreamedSparqlJSONAnswer(answer, REL_URI, outputa);

    // No namespaces, no schema, meta set
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(false, true), output.toString());
    assertEquals(getTrueP(false, true), outputb.toString());
    assertEquals(getAnswerP(false, true), outputa.toString());

    // No namespaces, schema set, meta set
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(true, true), output.toString());
    assertEquals(getTrueP(true, true), outputb.toString());
    assertEquals(getAnswerP(true, true), outputa.toString());

    // Namespaces set, schema set, meta set
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(true, true), output.toString());
    assertEquals(getTrueP(true, true), outputb.toString());
    assertEquals(getAnswerP(true, true), outputa.toString());

    // Namespaces set, no schema, meta set
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(false, true), output.toString());
    assertEquals(getTrueP(false, true), outputb.toString());
    assertEquals(getAnswerP(false, true), outputa.toString());
    
    e = new StreamedSparqlJSONAnswer(empty, output);
    b = new StreamedSparqlJSONAnswer(true, outputb);
    a = new StreamedSparqlJSONAnswer(answer, outputa);

    // No namespaces, schema set, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(true, false), output.toString());
    assertEquals(getTrueP(true, false), outputb.toString());
    assertEquals(getAnswerP(true, false), outputa.toString());

    // Namespaces set, schema set, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(true, false), output.toString());
    assertEquals(getTrueP(true, false), outputb.toString());
    assertEquals(getAnswerP(true, false), outputa.toString());

    // Namespaces set, no schema, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(false, false), output.toString());
    assertEquals(getTrueP(false, false), outputb.toString());
    assertEquals(getAnswerP(false, false), outputa.toString());

    // No Namespaces, no schema, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(false, false), output.toString());
    assertEquals(getTrueP(false, false), outputb.toString());
    assertEquals(getAnswerP(false, false), outputa.toString());

    // No namespaces, schema set, no meta
    output.reset();
    outputb.reset();
    outputa.reset();
    e.emit();
    b.emit();
    a.emit();
    assertEquals(getEmptyP(true, false), output.toString());
    assertEquals(getTrueP(true, false), outputb.toString());
    assertEquals(getAnswerP(true, false), outputa.toString());
  }


  /**
   * Populate the test answer.
   * @throws Exception Error setting up the ResultSet
   */
  protected void setUp() throws Exception {
    TestResultSet trs1 = new TestResultSet(new String[] { "x", "y" });
    ResultSetRow row;
    row = new ResultSetRow(trs1);
    row.setObject("x", new LiteralImpl("X1"));
    row.setObject("y", new URIReferenceImpl(URI.create("urn:y1")));
    trs1.addRow(row);
    row = new ResultSetRow(trs1);
    row.setObject("x", new LiteralImpl("X2", "en"));
    row.setObject("y", new BlankNodeImpl(42));
    trs1.addRow(row);
    answer = new AnswerImpl(trs1);

    List<Variable> variables = Arrays.asList(new Variable[] { new Variable("x") });
    empty = new AnswerImpl(variables);
    
    output = new ByteArrayOutputStream();
    outputb = new ByteArrayOutputStream();
    outputa = new ByteArrayOutputStream();
  }

  /**
   * Clean up the test answer.
   */
  public void tearDown() {
    answer.close();
    empty.close();
  }

  /////////////////////////////
  // start of getEmpty variants
  /////////////////////////////

  private static String getEmpty() {
    return getEmpty(false, false);
  }

  private static String getEmptyP() {
    return getEmptyP(false, false);
  }

  private static String getEmpty(boolean schema, boolean meta) {
    return getCommonStart(schema, meta) + EMPTY_BODY;
  }

  private static String getEmptyP(boolean schema, boolean meta) {
    return getCommonStartP(schema, meta) + EMPTY_BODY_P;
  }

  ////////////////////////////
  // start of getTrue variants
  ////////////////////////////

  private static String getTrue() {
    return getTrue(false, false);
  }

  private static String getTrueP() {
    return getTrueP(false, false);
  }

  private static String getTrue(boolean schema, boolean meta) {
    return getCommonStart(schema, meta) + TRUE_BODY;
  }

  private static String getTrueP(boolean schema, boolean meta) {
    return getCommonStartP(schema, meta) + TRUE_BODY_P;
  }

  //////////////////////////////
  // start of getAnswer variants
  //////////////////////////////

  private static String getAnswer() {
    return getAnswer(false, false);
  }

  private static String getAnswerP() {
    return getAnswerP(false, false);
  }

  private static String getAnswer(boolean schema, boolean meta) {
    return getShortCommonStart(schema) + ANSWER_VARS + getMeta(meta) + ANSWER_BODY;
  }

  private static String getAnswerP(boolean schema, boolean meta) {
    return getShortCommonStartP(schema) + ANSWER_VARS_P + getMetaP(meta) + ANSWER_BODY_P;
  }

  ////////////////
  // common header
  ////////////////

  private static String getCommonStart(boolean schema, boolean meta) {
    return getShortCommonStart(schema) + getMeta(meta);
  }

  private static String getCommonStartP(boolean schema, boolean meta) {
    return getShortCommonStartP(schema) + getMetaP(meta);
  }

  private static String getShortCommonStart(boolean schema) {
    String result = DOC_HEAD + SPARQL_HEAD;
    if (schema) result += " " + SPARQL_HEAD_ATTR;
    result += ">" + EMPTY_HEAD;
    return result;
  }

  private static String getShortCommonStartP(boolean schema) {
    String result = DOC_HEAD + SPARQL_HEAD;
    if (schema) result += "\n" + SPARQL_HEAD_INDENT + SPARQL_HEAD_ATTR;
    result += ">\n  " + EMPTY_HEAD + "\n";
    return result;
  }

  private static String getMeta(boolean meta) {
    return meta ? HEAD_META : "";
  }

  private static String getMetaP(boolean meta) {
    return meta ? HEAD_META_INDENT + HEAD_META + "\n" : "";
  }

  static final String DOC_HEAD = "<?xml version=\"1.0\"?>\n";

  static final String SPARQL_HEAD = "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"";

  static final String SPARQL_HEAD_ATTR = "xsi:schemaLocation=\"http://www.w3.org/2007/SPARQL/result.xsd\"";

  static final String SPARQL_HEAD_INDENT = "        ";
  
  static final String HEAD_META = "<link href=\"" + REL_URI + "\"/>";

  static final String HEAD_META_INDENT = "    ";

  static final String EMPTY_HEAD = "<head>";

  static final String EMPTY_BODY = "</head><results></results></sparql>";

  static final String EMPTY_BODY_P = "  </head>\n" +
      "  <results>\n" +
      "  </results>\n" +
      "</sparql>";

  static final String TRUE_BODY = "</head><boolean>true</boolean></sparql>";

  static final String TRUE_BODY_P = "  </head>\n" +
      "  <boolean>true</boolean>\n" +
      "</sparql>";

  static final String ANSWER_VARS = "<variable name=\"x\"/><variable name=\"y\"/>";

  static final String ANSWER_BODY = "</head>" +
      "<results>" +
      "<result><binding name=\"x\"><literal>X1</literal></binding>" +
      "<binding name=\"y\"><uri>urn:y1</uri></binding></result>" +
      "<result><binding name=\"x\"><literal xml:lang=\"en\">X2</literal></binding>" +
      "<binding name=\"y\"><bnode>_node42</bnode></binding></result>" +
      "</results>" +
      "</sparql>";

  static final String ANSWER_VARS_P = "    <variable name=\"x\"/>\n" +
      "    <variable name=\"y\"/>\n";

  static final String ANSWER_BODY_P = "  </head>\n" +
      "  <results>\n" +
      "    <result>\n" +
      "      <binding name=\"x\">\n" +
      "        <literal>X1</literal>\n" +
      "      </binding>\n" +
      "      <binding name=\"y\">\n" +
      "        <uri>urn:y1</uri>\n" +
      "      </binding>\n" +
      "    </result>\n" +
      "    <result>\n" +
      "      <binding name=\"x\">\n" +
      "        <literal xml:lang=\"en\">X2</literal>\n" +
      "      </binding>\n" +
      "      <binding name=\"y\">\n" +
      "        <bnode>_node42</bnode>\n" +
      "      </binding>\n" +
      "    </result>\n" +
      "  </results>\n" +
      "</sparql>";
}
