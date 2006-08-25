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

package org.mulgara.store.stringpool.xa;

//Java 2 standard packages
import java.nio.ByteBuffer;
import java.io.*;
import java.net.URI;

//apache packages
import org.apache.log4j.*;

// jrdf
import org.jrdf.vocabulary.RDF;

//mulgara packages
import org.mulgara.store.stringpool.AbstractSPTypedLiteral;
import org.mulgara.store.stringpool.SPComparator;
import org.mulgara.store.stringpool.SPTypedLiteral;
import org.jrdf.graph.Graph;
import org.jrdf.graph.mem.GraphImpl;
import org.jrdf.parser.rdfxml.RdfXmlParser;

/**
 * A class that represents the inbuilt RDF datatype XML Literal.  Based on
 * {@link SPXSDStringImpl}.
 *
 * @created 2004-10-04
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.2 $
 *
 * @modified $Date: 2005/03/12 02:53:28 $ by $Author: newmana $
 *
 * @company <a href="http://www.tucanatech.com/">Tucana Technologies</a>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class SPXMLLiteralImpl extends AbstractSPTypedLiteral
    implements SPTypedLiteral {

  /**
   * Logger
   */
  private static final Category logger =
      Category.getInstance(SPXMLLiteralImpl.class.getName());

  /**
   * Type code that identifies this type
   */
  static final int TYPE_ID = 15;

  /**
   * the XML Literal URI
   */
  static final URI TYPE_URI = RDF.XML_LITERAL;

  /**
   * The lexical value of the XML Literal.
   */
  private String str;

  SPXMLLiteralImpl(String str) {
    super(TYPE_ID, TYPE_URI);
    if (str == null) {
      throw new IllegalArgumentException("Null \"str\" parameter");
    }
    validate(str);
    this.str = str;
  }

  /**
   * Attempts to ensure that the given XML Literal value is valid.
   *
   * @param str the XML Literal.
   * @throws IllegalArgumentException if the str does not validate.
   */
  private void validate(String str) {
    String document =
      "<rdf:RDF xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#'>\n" +
      "  <rdf:Description>\n" +
      "    <rdf:value rdf:parseType='Literal'>" + str + "</rdf:value>\n" +
      "  </rdf:Description>\n" +
      "</rdf:RDF>";

    try {
      final Graph jrdfMem = new GraphImpl();
      RdfXmlParser parser = new RdfXmlParser(jrdfMem.getElementFactory());
      parser.parse(new StringReader(document), URI.create("urn:foo:bar").
          toString());
    }
    catch (Exception e) {
      throw new IllegalArgumentException("Failed to validate: " + str);
    }
  }

  SPXMLLiteralImpl(ByteBuffer data) {
    this(CHARSET.decode(data).toString());
  }

  public ByteBuffer getData() {
    return CHARSET.encode(str);
  }

  public SPComparator getSPComparator() {
    return SPCaseInsensitiveStringComparator.getInstance();
  }

  public String getLexicalForm() {
    return str;
  }

  public int compareTo(Object o) {
    // Compare types.
    int c = super.compareTo(o);
    if (c != 0) return c;

    // Compare the Strings.
    return str.compareToIgnoreCase(((SPXMLLiteralImpl)o).str);
  }

  public int hashCode() {
    return str.hashCode();
  }

  public boolean equals(Object obj) {

    // Check for null.
    if (obj == null) {
      return false;
    }

    try {
      return str.equals(((SPXMLLiteralImpl) obj).str);
    }
    catch (ClassCastException ex) {
      // obj was not an SPXSDStringImpl.
      return false;
    }
  }
}
