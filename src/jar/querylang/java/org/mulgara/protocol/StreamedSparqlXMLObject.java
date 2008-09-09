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

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;

import org.mulgara.util.StringUtil;


/**
 * Represents a data object as XML.
 *
 * @created Jul 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class StreamedSparqlXMLObject implements StreamedXMLAnswer {

  /** A single indent for use when pretty printing. */
  static final private String INDENT_STR = "  ";

  /** Indicates that pretty printing should be used. */
  boolean prettyPrint = true;

  /** The encoded data. */
  final Object objectData;

  /** The writer used for creating the XML. */
  protected OutputStreamWriter s = null;

  /** The byte output stream used for creating the XML. */
  protected OutputStream output = null;

  /** The charset encoding to use when writing to the output stream. */
  Charset charset = Charset.defaultCharset();

  /**
   * Creates an XML object encoding.
   * @param objectData The data to encode.
   * @param output Where to send the output.
   */
  public StreamedSparqlXMLObject(Object objectData, OutputStream output) {
    this.objectData = objectData;
    this.output = output;
  }

  /** @see org.mulgara.protocol.StreamedXMLAnswer#setCharacterEncoding(java.lang.String) */
  public void setCharacterEncoding(String encoding) {
    charset = Charset.forName(encoding);
  }

  /** @see org.mulgara.protocol.StreamedXMLAnswer#setCharacterEncoding(java.nio.Charset) */
  public void setCharacterEncoding(Charset charset) {
    this.charset = charset;
  }

  /** {@inheritDoc} */
  protected void addDocHeader() throws IOException {
    s.append("<?xml version=\"1.0\"?>\n");
    s.append("<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"");
    s.append(">");
  }

  /** {@inheritDoc} */
  protected void addDocFooter() throws IOException {
    s.append("</sparql>");
  }

  /** {@inheritDoc} */
  protected void addResults() throws IOException {
    if (prettyPrint) s.append(INDENT_STR).append("<data>");
    if (objectData != null) s.append(StringUtil.quoteAV(objectData.toString()));
    s.append("</data>");
  }


  /**
   * Put the parts of the document together, and close the stream.
   * @see org.mulgara.protocol.StreamedXMLAnswer#emit()
   */
  public void emit() throws IOException {
    s = new OutputStreamWriter(output);
    addDocHeader();
    addResults();
    addDocFooter();
    s.flush();
  }


  /**
   * @see org.mulgara.protocol.XMLAnswer#addNamespace(java.lang.String, java.net.URI)
   * Ignored.
   */
  public void addNamespace(String name, URI nsValue) {
  }

  /**
   * @see org.mulgara.protocol.XMLAnswer#addNamespace(java.lang.String, java.net.URI)
   * Ignored.
   */
  public void clearNamespaces() {
  }

  public void setPrettyPrint(boolean prettyPrint) {
    this.prettyPrint = prettyPrint;
  }

}
