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

package org.mulgara.content.rio;

// Java 2 standard packages
import java.beans.Beans;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;

// Third party packages
import junit.framework.*;        // JUnit unit testing framework
import org.apache.log4j.Logger;  // Apache Log4J
import org.jrdf.vocabulary.RDF;  // Java RDF API

// Locally written packages
import org.mulgara.content.Content;
import org.mulgara.query.TuplesException;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.ResolverSession;

/**
 * Test suite for {@link RDFXMLStatements}.
 *
 * @created 2004-09-17
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:04 $ @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy; 2004 <a href="http://www.tucanatech.com/">Tucan
 *   Technology Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class RDFXMLStatementsUnitTest extends TestCase
{
  /**
   * Logger.
   */
  private static final Logger logger =
    Logger.getLogger(RDFXMLStatementsUnitTest.class.getName());

  //
  // Constructors
  //

  /**
   * Construct a test.
   *
   * @param name  the name of the test to construct
   */
  public RDFXMLStatementsUnitTest(String name)
  {
    super(name);
  }

  //
  // Methods implementing TestCase
  //

  public void setup()
  {
  }

  /**
   * Hook from which the test runner can obtain a test suite.
   *
   * @return the test suite
   */
  public static Test suite()
  {
    return new TestSuite(RDFXMLStatementsUnitTest.class);
  }

  //
  // Tests
  //

  /**
   * Test {@link RDFXMLStatements} parsing the file <tt>test0014.rdf</tt>..
   *
   * @throws Exception if there's an error running the test (note that if the
   *   test merely fails, this should <em>not</em> throw any exception
   */
  public void test1() throws Exception
  {
    File file = new File(
                  new File(new File(System.getProperty("cvs.root")), "data"),
                  "test0014.rdf"
                );

    // Obtain a content handler for the test file
    Content content = (Content)
      Class.forName("org.mulgara.resolver.file.FileContent")
           .getConstructor(new Class[] { File.class })
           .newInstance(new Object[] { file });
    assert content != null;

    // Obtain a resolver session
    ResolverSession resolverSession = new TestResolverSession();

    // Seed the resolver session with the nodes occurring in the test document
    long s1 = resolverSession.localize(new URIReferenceImpl(
                                         file.toURI().resolve("#container")));
    long p1 = resolverSession.localize(new URIReferenceImpl(RDF.TYPE));
    long o1 = resolverSession.localize(new URIReferenceImpl(RDF.SEQ));

    long p2 = resolverSession.localize(new URIReferenceImpl(new URI(
                                         RDF.baseURI + "_1")));
    long o2 = resolverSession.localize(new LiteralImpl("bar"));

    try {
      RDFXMLStatements rdfXmlStatements =
        new RDFXMLStatements(content, resolverSession);

      // Validate first statement
      rdfXmlStatements.beforeFirst();
      assertTrue(rdfXmlStatements.next());
      assertEquals(s1, rdfXmlStatements.getSubject());
      assertEquals(p1, rdfXmlStatements.getPredicate());
      assertEquals(o1, rdfXmlStatements.getObject());

      // Validate second statement
      assertTrue(rdfXmlStatements.next());
      assertEquals(s1, rdfXmlStatements.getSubject());
      assertEquals(p2, rdfXmlStatements.getPredicate());
      assertEquals(o2, rdfXmlStatements.getObject());

      // No more statements
      assertFalse(rdfXmlStatements.next());
    }
    catch (Exception e) {
      fail(e);
    }
  }

  /**
   * Test {@link RDFXMLStatements} parsing the file <tt>test001.rdf</tt>..
   *
   * This is intended to exercise the requirement that bnodes within the
   * document be recognized as equal to themselves on subsequent parses.
   *
   * @throws Exception if there's an error running the test (note that if the
   *   test merely fails, this should <em>not</em> throw any exception
   */
  /*
  public void test2() throws Exception
  {
    File file = new File(
                  new File(new File(System.getProperty("cvs.root")), "data"),
                  "test001.rdf"
                );

    // Obtain a content handler for the test file
    Content content = (Content)
      Class.forName("org.mulgara.resolver.file.FileContent")
           .getConstructor(new Class[] { File.class })
           .newInstance(new Object[] { file });
    assert content != null;

    // Obtain a resolver session
    ResolverSession resolverSession = new TestResolverSession();

    // Seed the resolver session with the nodes occurring in the test document
    long p1 = resolverSession.localize(new URIReferenceImpl(new URI(
                                         "http://example.org/property")));
    long o1 = resolverSession.localize(new LiteralImpl("property value"));

    try {
      RDFXMLStatements rdfXmlStatements =
        new RDFXMLStatements(content, resolverSession);

      // Validate first statement
      rdfXmlStatements.beforeFirst();
      assertTrue(rdfXmlStatements.next());
      long s1 = rdfXmlStatements.getSubject();  // read the bnode
      assertEquals(p1, rdfXmlStatements.getPredicate());
      assertEquals(o1, rdfXmlStatements.getObject());

      // No more statements
      assertFalse(rdfXmlStatements.next());

      // Validate second statement
      rdfXmlStatements.beforeFirst();
      assertTrue(rdfXmlStatements.next());
      assertEquals(s1, rdfXmlStatements.getSubject());  // must match the bnode
      assertEquals(p1, rdfXmlStatements.getPredicate());
      assertEquals(o1, rdfXmlStatements.getObject());

      // No more statements
      assertFalse(rdfXmlStatements.next());
    }
    catch (Exception e) {
      fail(e);
    }
  }
  */

  //
  // Internal methods
  //

  /**
   * Fail with an unexpected exception
   */
  private void fail(Throwable throwable)
  {
    fail(throwable.getMessage(), throwable);
  }

  private void fail(String test, Throwable throwable)
  {
    StringWriter stringWriter = new StringWriter();

    if (test != null) {
      stringWriter.write(test + ": ");
    }

    throwable.printStackTrace(new PrintWriter(stringWriter));
    fail(stringWriter.toString());
  }
}
