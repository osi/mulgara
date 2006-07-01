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

package org.mulgara.query;

// java packages
import java.net.URI;
import java.net.URISyntaxException;

// external packages
import org.jrdf.graph.URIReference;
import org.jrdf.vocabulary.Vocabulary;

// local packages
import org.mulgara.query.rdf.Mulgara;

/**
 * Provides a central point for defining all magic predicates used by Mulgara.
 * Each predicate is stored as string and URI.
 *
 * @created 2004-08-10
 *
 * @author <a href="mailto:pag@tucanatech.com">Paul Gearon</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2003
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class SpecialPredicates extends Vocabulary {

  /** The string for the equality predicate */
  public static final String IS = Mulgara.NAMESPACE + "is";

  /** The string for the cardinality equality predicate */
  public static final String OCCURS = Mulgara.NAMESPACE + "occurs";

  /** The string for the cardinality equality predicate */
  public static final String NOT_OCCURS = Mulgara.NAMESPACE + "notOccurs";

  /** The string for the cardinality less than predicate */
  public static final String OCCURS_LESS_THAN = Mulgara.NAMESPACE + "occursLessThan";

  /** The string for the cardinality greater than predicate */
  public static final String OCCURS_MORE_THAN = Mulgara.NAMESPACE + "occursMoreThan";

  /** The URI for the equality predicate */
  public static final URI MULGARA_IS;

  /** The URI for the cardinality equality predicate */
  public static final URI MULGARA_OCCURS;

  /** The URI for the cardinality equality predicate */
  public static final URI MULGARA_NOT_OCCURS;

  /** The URI for the cardinality less than predicate */
  public static final URI MULGARA_OCCURS_LESS_THAN;

  /** The URI for the cardinality greater than predicate */
  public static final URI MULGARA_OCCURS_MORE_THAN;

  // intialize all predicate URIs
  static {
    try {
      MULGARA_IS = new URI(IS);
      MULGARA_OCCURS = new URI(OCCURS);
      MULGARA_NOT_OCCURS = new URI(NOT_OCCURS);
      MULGARA_OCCURS_LESS_THAN = new URI(OCCURS_LESS_THAN);
      MULGARA_OCCURS_MORE_THAN = new URI(OCCURS_MORE_THAN);

      resources.add(MULGARA_IS);
      resources.add(MULGARA_OCCURS);
      resources.add(MULGARA_NOT_OCCURS);
      resources.add(MULGARA_OCCURS_LESS_THAN);
      resources.add(MULGARA_OCCURS_MORE_THAN);
    } catch (URISyntaxException e) {
      // Houston, we have a problem.  Not much we can do with it, so rethrow as an error.
      throw new ExceptionInInitializerError(e);
    }
  }


  /**
   * This method is used to see if a predicate is "Magic" according the list defined in this class.
   *
   * @param uri The uri of the predicate to test.
   * @return <code>true</code> if the predicate is magic.
   */
  public static final boolean isSpecialPredicate(URI uri) {
    return uri.equals(MULGARA_IS) || uri.equals(MULGARA_OCCURS) ||
        uri.equals(MULGARA_NOT_OCCURS) || uri.equals(MULGARA_OCCURS_LESS_THAN) ||
        uri.equals(MULGARA_OCCURS_MORE_THAN);
  }


  /**
    * This method is used to see if a predicate is "Magic" according the list defined in this class.
   *
   * @param uri The string of the uri of the predicate to test.
   * @return <code>true</code> if the predicate is magic.
   */
  public static final boolean isSpecialPredicate(String uri) {
    return uri.equals(IS) || uri.equals(OCCURS) || uri.equals(NOT_OCCURS) ||
        uri.equals(OCCURS_LESS_THAN) || uri.equals(OCCURS_MORE_THAN);
  }

}
