/*
 * Copyright 2008 The Topaz Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributions:
 */

package org.mulgara.content.n3;

// Java 2 standard packages
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.Map;

// Java 2 enterprise packages
import javax.activation.MimeType;

// Third party packages
import junit.framework.TestCase;
import org.jrdf.graph.BlankNode;

// Locally written packages
import org.mulgara.content.Content;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;
import org.mulgara.resolver.spi.TestResolverSession;

/**
 * Test suite for {@link N3Writer}.
 *
 * @created 2009-02-15
 * @author Ronald Tschalär
 * @licence Apache License v2.0
 */
public class N3WriterUnitTest extends TestCase {
  /**
   * Test {@link N3Wrtier} writing.
   *
   * @throws Exception if there's an error running the test (note that if the test
   *                   merely fails, this should <em>not</em> throw any exception
   */
  public void test1() throws Exception {
    // basic statement with plain literal
    runTest("<foo:bar> <foo:baz> \"42\" .\n", null);
    // basic statement with blank-node and datatyped literal
    runTest("_:node1000001 <foo:baz> \"42\"^^<xsd:int> .\n", null);
    // literal and uri with non-ascii characters
    runTest("<foo:i18n:øé> <foo:baz> \"Some text with \\\" in it, and 日本 chars, and \\u00E0 \" .",
            "<foo:i18n:%C3%B8%C3%A9> <foo:baz> \"Some text with \\\" in it, and \\u65E5\\u672C chars, and \\u00E0 \" .\n");
    // literal with newlines, ff, etc
    runTest("<foo:bar> <foo:baz> \"Some text with \n, \r, \f, \u0000 in it\" .",
            "<foo:bar> <foo:baz> \"Some text with \\n, \\r, \\u000C, \\u0000 in it\" .\n");
    runTest("<foo:bar> <foo:baz> \"Some text with \\n, \\r, \\f, \\u0000 in it\" .",
            "<foo:bar> <foo:baz> \"Some text with \\n, \\r, \\u000C, \\u0000 in it\" .\n");
    // multiple blank-nodes, and language-tag
    runTest("_:bn1 <baz:baz> _:bn2 .\n" +
            "_:bn2 <bar:bar> <foo:foo> .\n" +
            "<foo:foo> <dc:title> \"hello\"@en .",
            "_:node1000002 <baz:baz> _:node1000001 .\n" +
            "_:node1000001 <bar:bar> <foo:foo> .\n" +
            "<foo:foo> <dc:title> \"hello\"@en .\n");
    // multiple blank-nodes using internal-ids (numbers)
    runTest("_:42 <baz:baz> _:987 .\n" +
            "_:987 <bar:bar> <foo:foo> .\n",
            "_:node1000002 <baz:baz> _:node1000001 .\n" +
            "_:node1000001 <bar:bar> <foo:foo> .\n");
  }

  private void runTest(String n3, String exp) throws Exception {
    // Obtain a resolver session
    ResolverSession resolverSession = new TestResolverSession();

    // create the statements
    Statements statements = new N3Statements(new StringContent(n3), resolverSession);

    // test
    StringWriter out = new StringWriter();
    new N3Writer().write(statements, resolverSession, out);

    assertEquals(exp != null ? exp : n3, out.toString());
  }

  private static class StringContent implements Content {
    private final String content;

    public StringContent(String content) {
      this.content = content;
    }

    public Map<Object,BlankNode> getBlankNodeMap()         { return null; }
    public MimeType getContentType()                       { return null; }
    public URI getURI()                                    { return null; }
    public String getURIString()                           { return null; }
    public OutputStream newOutputStream()                  { return null; }

    public InputStream newInputStream() throws IOException {
      return new ByteArrayInputStream(content.getBytes("UTF-8"));
    }
  }
}
