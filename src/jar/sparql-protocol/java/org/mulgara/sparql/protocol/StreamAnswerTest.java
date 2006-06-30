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

package org.mulgara.sparql.protocol;

// Java 2 standard packages
import java.io.*;
import java.net.URI;
import java.util.*;

// Third party packages
import junit.framework.*;          // JUnit
import junit.textui.TestRunner;
import org.apache.log4j.Category;  // Log4J
import org.jrdf.graph.Literal;     // JRDF
import org.jrdf.graph.URIReference;

// Locally written packages
import org.mulgara.query.Answer;
import org.mulgara.query.AnswerImpl;
import org.mulgara.query.AnswerOperations;
import org.mulgara.query.UnconstrainedAnswer;
import org.mulgara.query.Variable;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;

/**
* Test suite for {@link StreamAnswer}.
*
* @created 2004-03-21
* @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
* @version $Revision: 1.9 $
* @modified $Date: 2005/01/05 04:59:05 $ by $Author: newmana $
* @copyright &copy;2004
*   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
* @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
*/
public class StreamAnswerTest extends TestCase
{
  /**
  * Logger.
  */
  private Category logger = Category.getInstance(getClass().getName());

  /**
  * Constructs a new answer test with the given name.
  *
  * @param name  the name of the test
  */
  public StreamAnswerTest(String name)
  {
    super(name);
  }

  /**
  * Obtain a suite of all tests.
  *
  * @return the test suite
  */
  public static Test suite()
  {
    return new TestSuite(StreamAnswerTest.class);
  }

  /**
  * Entry point for running the test standalone.
  *
  * This uses the default text user interface {@link TestRunner}.
  *
  * @param args  command line arguments
  */
  public static void main(String[] args)
  {
    TestRunner.run(suite());
  }

  //
  // Test cases
  //

  /**
  * Parse a string into a {@link StreamAnswer} and access it via the
  * {@link Answer} interface.
  */
  public void test1Parse() throws Exception
  {
    String string =
      "<?itql-variables x y ?>"+
      "<?itql-row-count 2 ?>"+
      "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"+
      "         xmlns='"+StreamAnswer.NAMESPACE+"'>"+
      "<Solution>"+
      "  <x>X1</x>"+
      "  <y>Y1</y>"+
      "</Solution>"+
      "<Solution>"+
      "  <x>X2</x>"+
      "  <y>Y2</y>"+
      "</Solution>"+
      "</rdf:RDF>";

    Answer answer =
      new StreamAnswer(new ByteArrayInputStream(string.getBytes("utf-8")));

    Variable x = new Variable("x");
    Variable y = new Variable("y");

    Literal X1 = new LiteralImpl("X1");
    Literal Y1 = new LiteralImpl("Y1");
    Literal X2 = new LiteralImpl("X2");
    Literal Y2 = new LiteralImpl("Y2");

    assertTrue(answer.getVariables() != null);
    assertEquals(2, answer.getVariables().length);
    assertEquals(x, answer.getVariables()[0]);
    assertEquals(y, answer.getVariables()[1]);

    assertEquals(0, answer.getColumnIndex(x));
    assertEquals(1, answer.getColumnIndex(y));

    assertTrue(answer.next());
    assertEquals(X1, answer.getObject(0));
    assertEquals(Y1, answer.getObject(1));

    assertTrue(answer.next());
    assertEquals(X2, answer.getObject(0));
    assertEquals(Y2, answer.getObject(1));

    assertTrue(!answer.next());
  }

  /**
  * Parse a string into a {@link StreamAnswer} and access it via the
  * {@link Answer} interface.
  */
  public void test2Parse() throws Exception
  {
    String string =
      "<?itql-variables x y ?>"+
      "<?itql-row-count 2 ?>"+
      "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>"+
      "         xmlns='"+StreamAnswer.NAMESPACE+"'>"+
      "<Solution>"+
      "  <x rdf:resource=\"foo:X1\"/>"+
      "  <y rdf:resource=\"foo:Y1\"/>"+
      "</Solution>"+
      "<Solution>"+
      "  <x rdf:resource=\"foo:X2\"/>"+
      "  <y rdf:resource=\"foo:Y2\"/>"+
      "</Solution>"+
      "</rdf:RDF>";

    Answer answer =
      new StreamAnswer(new ByteArrayInputStream(string.getBytes("utf-8")));

    Variable x = new Variable("x");
    Variable y = new Variable("y");

    URIReference X1 = new URIReferenceImpl(new URI("foo:X1"));
    URIReference Y1 = new URIReferenceImpl(new URI("foo:Y1"));
    URIReference X2 = new URIReferenceImpl(new URI("foo:X2"));
    URIReference Y2 = new URIReferenceImpl(new URI("foo:Y2"));

    assertTrue(answer.getVariables() != null);
    assertEquals(2, answer.getVariables().length);
    assertEquals(x, answer.getVariables()[0]);
    assertEquals(y, answer.getVariables()[1]);

    assertEquals(0, answer.getColumnIndex(x));
    assertEquals(1, answer.getColumnIndex(y));

    assertTrue(answer.next());
    assertEquals(X1, answer.getObject(0));
    assertEquals(Y1, answer.getObject(1));

    assertTrue(answer.next());
    assertEquals(X2, answer.getObject(0));
    assertEquals(Y2, answer.getObject(1));

    assertTrue(!answer.next());
  }

  /**
  * Parse a string into a {@link StreamAnswer} and back.
  *
  * Round-tripping won't generally work here, because whitespace is discarded
  * as meaningless during parsing and won't be recreated during serialization.
  * The test string in this case has no whitespace, so nothing is lost.
  */
  public void test1RoundTrip() throws Exception
  {
    String string = "<?itql-row-count 3?><?itql-variables s p o?><RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><Solution><s>EMPTY_GROUP</s><p rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\"></p><o rdf:resource=\"http://tucana.org/tucana-int#Group\"></o></Solution><Solution><s rdf:resource=\"beep://10.0.1.2#\"></s><p rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\"></p><o rdf:resource=\"http://tucana.org/tucana#Model\"></o></Solution><Solution><s rdf:resource=\"beep://10.0.1.2#_\"></s><p rdf:resource=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#type\"></p><o rdf:resource=\"http://tucana.org/tucana#Model\"></o></Solution></RDF>";

    // Parse the string into an Answer, and then serialize it back
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamAnswer.serialize(
      new StreamAnswer(new ByteArrayInputStream(string.getBytes("utf-8"))),
      baos
    );

    // Check whether the round trip succeeded
    assertEquals(string, new String(baos.toByteArray(), "utf8"));
  }

  /**
  * Serialize an {@link Answer} and parse it back.
  */
  public void test2RoundTrip() throws Exception
  {
    // Serialize the Answer into a buffer
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamAnswer.serialize(new UnconstrainedAnswer(), baos);

    System.out.print(new String(baos.toByteArray()));

    // Parse the buffer back into an Answer and verify equality
    assertTrue(AnswerOperations.equal(
      new UnconstrainedAnswer(),
      new StreamAnswer(new ByteArrayInputStream(baos.toByteArray()))
    ));
  }

  /**
  * Test the {@link StreamAnswer#serialize} method.
  */
  public void testSerialize() throws Exception
  {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    StreamAnswer.serialize(new UnconstrainedAnswer(), baos);
    assertEquals(new String(baos.toByteArray(), "utf8"),
      "<?itql-row-count 1?>"+
      "<RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">"+
      "<Solution>"+
      "</Solution>"+
      "</RDF>"
    );
  }
}
