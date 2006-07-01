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
import java.net.URI;

// Third party packages
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.*;

// Locally written packages
import org.mulgara.query.rdf.*;

/**
 * An instance of this GraphElementFactoryImpl is passed to
 * org.jrdf.parser.rdfxml.RdfXmlParser for it to allocate the Mulgara variations
 * of Node objects.
 *
 * @created 2004-12-23
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/01/05 04:58:04 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class GraphElementFactoryImpl implements GraphElementFactory {

  /** Logger. */
  private static final Logger logger =
      Logger.getLogger(GraphElementFactoryImpl.class.getName());

  /**
   * Throws a GraphElementFactoryException.
   *
   * @return BlankNode
   * @throws GraphElementFactoryException
   */
  public BlankNode createResource() throws GraphElementFactoryException {
    throw new GraphElementFactoryException("createResource() not supported");
  }

  /**
   * Returns a new URIResource. The uri is validated.
   *
   * @param uri URI
   * @return URIReference
   */
  public URIReference createResource(URI uri) {
    return createResource(uri, true);
  }

  /**
   * Returns a new URIResource.
   *
   * @param uri URI
   * @param validate boolean
   * @return URIReference
   */
  public URIReference createResource(URI uri, boolean validate) {
    return new URIReferenceImpl(uri, validate);
  }

  /**
   * Returns a new Literal with no language or datatype.
   *
   * @param lexicalValue String
   * @return Literal
   */
  public Literal createLiteral(String lexicalValue) {
    return new LiteralImpl(lexicalValue);
  }

  /**
   * Returns a new Literal with the specified language.
   *
   * @param lexicalValue String
   * @param languageType String
   * @return Literal
   */
  public Literal createLiteral(String lexicalValue, String languageType) {
    return new LiteralImpl(lexicalValue, languageType);
  }

  /**
   * Returns a new Literal with the specified datatype.
   *
   * @param lexicalValue String
   * @param datatypeURI URI
   * @return Literal
   */
  public Literal createLiteral(String lexicalValue, URI datatypeURI) {
    return new LiteralImpl(lexicalValue, datatypeURI);
  }

  /**
   * Throws a GraphElementFactoryException.
   *
   * @param subject SubjectNode
   * @param predicate PredicateNode
   * @param object ObjectNode
   * @return Triple
   */
  public Triple createTriple(SubjectNode subject, PredicateNode predicate,
      ObjectNode object) throws GraphElementFactoryException {
    throw new GraphElementFactoryException("createTriple() not supported");
  }
}
