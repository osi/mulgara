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
import java.io.*;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.graph.Node_Blank;
import com.hp.hpl.jena.shared.JenaException;

//JRDF packages
import org.jrdf.graph.*;

//Kowari packages

/**
 * Utilities for instantiating Jenaobjects.
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
public abstract class JenaUtil {


  /**
   * Converts a JRDF Triple to a Jena Triple.
   *
   * @param jrdfTriple Triple
   * @throws JenaException
   * @return Triple
   */
  public static com.hp.hpl.jena.graph.Triple convert(Triple jrdfTriple) throws
      JenaException {

    //validate
    if (jrdfTriple == null) {

      throw new IllegalArgumentException("JRDF Triple is null");
    }

    try {

      //get JRDF Nodes
      SubjectNode jenaSubject = jrdfTriple.getSubject();
      PredicateNode jenaPredicate = jrdfTriple.getPredicate();
      ObjectNode jenaObject = jrdfTriple.getObject();

      //get/create Jena Nodes
      com.hp.hpl.jena.graph.Node subjectNode = getNode(jenaSubject);
      com.hp.hpl.jena.graph.Node predicateNode = getNode(jenaPredicate);
      com.hp.hpl.jena.graph.Node objectNode = getNode(jenaObject);

      //return a new JRDF Triple
      return com.hp.hpl.jena.graph.Triple.create(subjectNode, predicateNode,
          objectNode);
    }
    catch (Exception exception) {

      //re-throw
      throw new JenaException("Could not convert jena triple to jrdf triple",
          exception);
    }
  }

  /**
   * Converts a JRDF Node to a Jena Node.
   *
   * @param node Node
   * @throws Exception
   * @return Node
   */
  private static com.hp.hpl.jena.graph.Node getNode(Node node) throws
      Exception {

    //validate
    if (node == null) {

      throw new Exception("JRDF Node is null");
    }

    //return value
    com.hp.hpl.jena.graph.Node jenaNode = null;

    if (node instanceof URIReference) {

      URI uri = ((URIReference) node).getURI();

      assert (uri != null) : "URIReference should have an URI";
      jenaNode = com.hp.hpl.jena.graph.Node.createURI(uri.toString());
    } else if (node instanceof Literal) {

      String lexical = ((Literal) node).getLexicalForm();
      String lang = ((Literal) node).getLanguage();
      URI type = ((Literal) node).getDatatypeURI();

      //convert type to a jena type (if the Node has a type)
      com.hp.hpl.jena.datatypes.RDFDatatype baseType = (type == null) ? null
          : new com.hp.hpl.jena.datatypes.BaseDatatype(type.toString());
      jenaNode = com.hp.hpl.jena.graph.Node.createLiteral(lexical, lang,
          baseType);
    }
    else {

      jenaNode = com.hp.hpl.jena.graph.Node.createAnon(new com.hp.hpl.jena.rdf.
          model.AnonId(node.toString()));
    }

    assert (jenaNode != null) : "Attempted to return null.";
    return jenaNode;
  }

}
