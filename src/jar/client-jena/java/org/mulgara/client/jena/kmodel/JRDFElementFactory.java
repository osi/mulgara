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

package org.mulgara.client.jena.kmodel;

// Java 2 standard packages
import java.net.URI;

// Log4J
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.*;
import org.kowari.query.rdf.*;

/**
 * A JRDF GraphElementFactory implementation for creating server nodes.
 * BlankNodes cannot be reused as the Factory exists outside of a Graph.
 *
 * @created 2004-10-27
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:34 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class JRDFElementFactory implements GraphElementFactory {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger log = Logger.getLogger(JRDFElementFactory.class.
      getName());

  /**
   * Default Constructor.
   */
  public JRDFElementFactory() {

    super();
  }

  /**
   * Creates a new BlankNode that does not exist in a specific Graph. Cannot be
   * reused, simply an object reference.
   *
   * @return BlankNode
   * @throws GraphElementFactoryException
   */
  public BlankNode createResource() throws GraphElementFactoryException {

    return new AbstractBlankNode() {
      //simply an Object reference
    };
  }

  /**
   * {@inheritDoc}
   */
  public URIReference createResource(URI uri) throws
      GraphElementFactoryException {
    return new URIReferenceImpl(uri);
  }

  /**
   * {@inheritDoc}
   */
  public URIReference createResource(URI uri,
      boolean validate) throws GraphElementFactoryException {
    return new URIReferenceImpl(uri, validate);
  }

  /**
   * {@inheritDoc}
   */
  public Literal createLiteral(String lexicalValue) throws
      GraphElementFactoryException {
    return new LiteralImpl(lexicalValue);
  }

  /**
   * {@inheritDoc}
   */
  public Literal createLiteral(String lexicalValue,
      String languageType) throws GraphElementFactoryException {
    return new LiteralImpl(lexicalValue, languageType);
  }

  /**
   * {@inheritDoc}
   */
  public Literal createLiteral(String lexicalValue,
      URI datatypeURI) throws GraphElementFactoryException {
    return new LiteralImpl(lexicalValue, datatypeURI);
  }

  /**
   * {@inheritDoc}
   */
  public Triple createTriple(SubjectNode subject, PredicateNode predicate,
      ObjectNode object) throws GraphElementFactoryException {
    return new TripleImpl(subject, predicate, object);
  }

}
