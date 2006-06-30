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

//Java 2 standard packages
import java.net.*;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.Node_Blank;
import com.hp.hpl.jena.shared.JenaException;

//JRDF packages
import org.jrdf.graph.*;

//Kowari packages

/**
 * Utilities for instantiating JRDF objects.
 *
 * @created 2001-08-16
 *
 * @author Chris Wilper
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:34 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2003 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class JRDFUtil {

  /** Factory used to create client-side JRDF Nodes (org.jrdf.graph.mem) */
  private static GraphElementFactory elementFactory = null;

  /**
   * Convert a Jena Triple to a JRDF Triple.
   *
   * @param jenaTriple Triple
   * @throws JenaException
   * @return Triple
   */
  public static Triple convert(com.hp.hpl.jena.graph.Triple jenaTriple) throws
      JenaException {

    //validate
    if (jenaTriple == null) {

      throw new IllegalArgumentException("Jena Triple is null");
    }

    try {

      //get Jena Nodes
      com.hp.hpl.jena.graph.Node jenaSubject = jenaTriple.getSubject();
      com.hp.hpl.jena.graph.Node jenaPredicate = jenaTriple.getPredicate();
      com.hp.hpl.jena.graph.Node jenaObject = jenaTriple.getObject();

      //get/create JRDF Nodes
      SubjectNode subjectNode = getSubjectNode(jenaSubject);
      PredicateNode predicateNode = getPredicateNode(jenaPredicate);
      ObjectNode objectNode = getObjectNode(jenaObject);

      //return a new JRDF Triple
      return getElementFactory().createTriple(subjectNode, predicateNode,
          objectNode);
    }
    catch (Exception exception) {

      //re-throw
      throw new JenaException("Could not convert jena triple to jrdf triple",
          exception);
    }
  }

  /**
   * Returns a SubjectNode representing the Jena Node.
   *
   * @param node Node
   * @throws GraphException
   * @return SubjectNode
   */
  private static SubjectNode getSubjectNode(com.hp.hpl.jena.graph.Node node) throws
      GraphException {

    try {

      //return value
      SubjectNode subjectNode = null;

      //check for null
      if ((node == null)
          || (node.equals(node.ANY))
          || (node.equals(node.NULL))) {

        return null;
      }
      else if (node instanceof Node_Blank) {

        //create a new one
        URI anonURI = new URI("anon:" + node);
        subjectNode = getElementFactory().createResource(anonURI);
      }
      else {

        //get uri and create resource
        URI uri = new URI(node.toString());
        subjectNode = (SubjectNode) getElementFactory().createResource(uri);
      }

      return subjectNode;
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create SubjectNode.",
          factoryException);
    }
    catch (URISyntaxException uriException) {

      throw new GraphException("Could not create URI for Jena node.",
          uriException);
    }
  }

  /**
   * Returns a PredicateNode representing the Jena Node.
   *
   * @param node Node
   * @throws GraphException
   * @return SubjectNode
   */
  private static PredicateNode getPredicateNode(com.hp.hpl.jena.graph.Node node) throws
      GraphException {

    try {

      //cannot have a BlankNode predicate
      if (node instanceof Node_Blank) {

        throw new IllegalStateException("Predicate cannot be a BlankNode: " +
            node);
      }

      //check for null
      if ((node == null)
          || (node.equals(node.ANY))
          || (node.equals(node.NULL))) {

        return null;
      }
      else {

        //return a new resource
        URI uri = new URI(node.toString());
        return (PredicateNode) getElementFactory().createResource(uri);
      }
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create PredicateNode.",
          factoryException);
    }
    catch (URISyntaxException uriException) {

      throw new GraphException("Could not create URI for Jena node.",
          uriException);
    }
  }

  /**
   * Returns an ObjectNode representing the Jena Node.
   *
   * @param node Node
   * @throws GraphException
   * @return ObjectNode
   */
  private static ObjectNode getObjectNode(com.hp.hpl.jena.graph.Node node) throws
      GraphException {

    try {

      //return value
      ObjectNode objectNode = null;

      //check for null
      if ((node == null)
          || (node.equals(node.ANY))
          || (node.equals(node.NULL))) {

        return null;
      }
      else if (node.isLiteral()) {

        LiteralLabel literal = node.getLiteral();
        String litString = literal.getLexicalForm();
        String lang = literal.language();
        String typeURI = literal.getDatatypeURI();

        if (lang != null) {

          //create with language
          objectNode = getElementFactory().createLiteral(litString, lang);
        }
        else if (typeURI != null) {

          //create with datatype
          URI datatype = new URI(typeURI);
          objectNode = getElementFactory().createLiteral(litString, datatype);
        }
        else {

          //just create normal Literal
          objectNode = getElementFactory().createLiteral(litString);
        }
      }
      else if (node instanceof Node_Blank) {

        //create a new one
        URI anonURI = new URI("anon:" + node);
        objectNode = getElementFactory().createResource(anonURI);
      }
      else {

        //get uri and create resource
        URI uri = new URI(node.toString());
        objectNode = (ObjectNode) getElementFactory().createResource(uri);
      }

      return objectNode;
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create ObjectNode.", factoryException);
    }
    catch (URISyntaxException uriException) {

      throw new GraphException("Could not create URI for Jena node.",
          uriException);
    }
  }

  /**
   * Get a JRDF Triple for a regular statement.
   *
   * @param subj URI
   * @param pred URI
   * @param obj URI
   * @throws GraphException
   * @return Triple
   */
  public static Triple create(URI subj, URI pred, URI obj) throws
      GraphException {

    try {

      SubjectNode subject = getElementFactory().createResource(subj);
      PredicateNode predicate = getElementFactory().createResource(pred);
      ObjectNode object = getElementFactory().createResource(obj);

      return getElementFactory().createTriple(subject, predicate, object);
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create Triple: " + subj + ", " + pred +
          ", " + obj, factoryException);
    }
  }

  /**
   * Get a JRDF Triple for a statement with a plain literal as the object.
   *
   * @param subj URI
   * @param pred URI
   * @param literal String
   * @throws GraphException
   * @return Triple
   */
  public static Triple createLiteral(URI subj, URI pred, String literal) throws
      GraphException {

    try {

      SubjectNode subject = getElementFactory().createResource(subj);
      PredicateNode predicate = getElementFactory().createResource(pred);
      ObjectNode object = getElementFactory().createLiteral(literal);

      return getElementFactory().createTriple(subject, predicate, object);
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create Triple: " + subj + ", " + pred +
          ", " + literal, factoryException);
    }
  }

  /**
   * Get a JRDF Triple for a statement with a typed literal as the object.
   *
   * @param subj URI
   * @param pred URI
   * @param literal String
   * @param dataType URI
   * @throws GraphException
   * @return Triple
   */
  public static Triple createTypedLiteral(URI subj, URI pred, String literal,
      URI dataType) throws GraphException {

    try {

      SubjectNode subject = getElementFactory().createResource(subj);
      PredicateNode predicate = getElementFactory().createResource(pred);
      ObjectNode object = getElementFactory().createLiteral(literal, dataType);

      return getElementFactory().createTriple(subject, predicate, object);
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create Triple: " + subj + ", " + pred +
          ", " + literal + ", " + dataType,
          factoryException);
    }
  }

  /**
   * Get a JRDF Triple for a statement with a locale-specific literal as the
   * object.
   *
   * @param subj URI
   * @param pred URI
   * @param literal String
   * @param langCode String
   * @throws GraphException
   * @return Triple
   */
  public static Triple createLocalLiteral(URI subj, URI pred, String literal,
      String langCode) throws GraphException {

    try {

      SubjectNode subject = getElementFactory().createResource(subj);
      PredicateNode predicate = getElementFactory().createResource(pred);
      ObjectNode object = getElementFactory().createLiteral(literal, langCode);

      return getElementFactory().createTriple(subject, predicate, object);
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Could not create Triple: " + subj + ", " + pred +
          ", " + literal + ", " + langCode,
          factoryException);
    }
  }

  /**
   * Creates a JRDF Triple from the subject, predicate and object nodes.
   *
   * @param subject SubjectNode
   * @param predicate PredicateNode
   * @param object ObjectNode
   * @throws GraphException
   * @return Triple
   */
  public static Triple createTriple(SubjectNode subject,
      PredicateNode predicate, ObjectNode object) throws GraphException {

    try {

      return getElementFactory().createTriple(subject, predicate, object);
    }
    catch (GraphElementFactoryException factoryException) {

      throw new GraphException("Failed to create Triple.", factoryException);
    }
  }

  /**
   * Returns a GraphElementFactory that is used to create JRDF Nodes and Triples.
   *
   * @return GraphElementFactory
   * @throws GraphException
   */
  private synchronized static GraphElementFactory getElementFactory() throws
      GraphException {

    //lazy instantiation of factory
    if (elementFactory == null) {

      elementFactory = new JRDFElementFactory();
    }

    return elementFactory;
  }

}
