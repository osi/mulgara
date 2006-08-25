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

package org.mulgara.demo.mp3;

// Java 2 standard packages

// Logging
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.URIReference;
import org.jrdf.util.ClosableIterator;
import org.jrdf.vocabulary.RDFS;

// Local packages
import org.mulgara.query.QueryException;

/**
 * Represents a Model containg RDF schema statements.
 *
 * @created 2004-12-03
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.3 $
 *
 * @modified $Date: 2005/01/05 04:58:06 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class SchemaModelImpl extends AbstractModel implements SchemaModel {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger log = Logger.getLogger(SchemaModelImpl.class.
      getName());

  /**
   * Loads the Schema file/content into the SchemaModel
   *
   * @param schema URIReference
   * @throws QueryException
   */
  public void loadSchema(URIReference schema) throws QueryException {
    checkInitialized();
    if (schema == null) {
      throw new IllegalArgumentException("'schema' is null");
    }

    try {
      String query = "load <" + schema.getURI() + "> " + NEWLINE +
          "into <" + getResource().getURI() + "> ;";
      getBean().executeUpdate(query);
    } catch (Exception exception) {
      throw new QueryException("Failed to load schema: " + schema, exception);
    }
  }

  /**
   * Returns an Iterator (of Triples) of Properties for the specified domain.
   *
   * @param domain URIReference
   * @return ClosableIterator
   * @throws QueryException
   */
  public ClosableIterator getDomainProperties(URIReference domain) throws
      QueryException {

    if (domain == null) {
      throw new IllegalArgumentException("'domain' is null");
    }

    try {
      String query = getDomainPropertiesQuery(domain);
      return query(query);
    }
    catch (Exception exception) {
      throw new QueryException("Failed to determine domain properties.",
          exception);
    }
  }

  /**
   * Returns a query in the following format:
   * <p><pre>
   *   select $s $p $o
   *   from <getResource()>
   *   where $s $p $o
   *   and $p <mulgara:is> <rdf:domain>
   *   and $o <mulgara:is> <domain> ;
   * </pre>
   *
   * @param domain URIReference
   * @return String
   */
  private String getDomainPropertiesQuery(URIReference domain) {
    StringBuffer query = new StringBuffer();
    query.append("select $s $p $o " + NEWLINE);
    query.append("from <" + getResource().getURI() + "> " + NEWLINE);
    query.append("where $s $p $o " + NEWLINE);
    query.append("and $p " + MULGARA_IS + " <" + RDFS.DOMAIN + ">" + NEWLINE);
    query.append("and $o " + MULGARA_IS + " <" + domain.getURI() + "> ;");
    return query.toString();
  }

}
