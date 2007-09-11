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

// Java 2 standard packages
import java.net.*;
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.query.rdf.URIReferenceImpl;

/**
 * A leaf expression containing the {@link URL} of an RDF model.
 *
 * @created 2001-07-12
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2003
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ModelResource implements Model {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = -5673467065206144337L;

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(ModelResource.class.getName());

  /**
   * The {@link URI} of the RDF model.
   */
  private URI uri;

  //
  // Constructors
  //

  /**
   * Construct a model from a {@link URL}.
   *
   * @param uri the {@link URI} of the model to query
   * @throws IllegalArgumentException if <var>url</var> is <code>null</code>
   */
  public ModelResource(URI uri) {

    // Validate "uri" parameter
    if (uri == null) {

      throw new IllegalArgumentException("Null \"uri\" parameter");
    }

    // Initialize fields
    this.uri = uri;
  }

  //
  // Methods implementing ModelExpression
  //

  /**
   * @return an immutable singleton {@link Set} containing the {@link URL} of
   *   the server if this is a Java RMI or BEEP model, or the empty {@link Set}
   *   otherwise
   */
  public Set getDatabaseURIs() {

    try {

      if ("beep".equals(uri.getScheme()) || "rmi".equals(uri.getScheme())) {

        // In Java RMI models, the database is the URI without
        // the fragment identifier
        // TODO: Call RmiQueryHandler.serverURI instead
        return Collections.singleton(new URI(uri.getScheme(),
                                             uri.getAuthority(),
                                             uri.getPath(),
                                             null,
                                             null));
      }
      else {

        return Collections.EMPTY_SET;
      }
    }
    catch (URISyntaxException e) {

      throw new RuntimeException("Couldn't truncate model URI " + uri +
          " to obtain a database URI");
    }

  }

  //
  // API methods
  //

  /**
   * Accessor for the <var>uri</var> property.
   *
   * @return a {@link URI} instance (never <code>null</code>)
   */
  public URI getURI() {

    return uri;
  }

  //
  // Methods extending Object
  //

  /**
   * The text representation of the URI.
   *
   * @return the text representation of the URI.
   */
  public String toString() {

    return uri.toString();
  }

  //
  // Methods overriding Object
  //

  /**
   * Return true if the URIs of a ModelResource are equal.
   *
   * @param object ModelResource to test equality.
   * @return true if the URIs of a ModelResource are equal.
   */
  public boolean equals(Object object) {

    if (object == null) {

      return false;
    }

    try {

      ModelResource tmpModelResource = (ModelResource) object;
      return getURI().equals(tmpModelResource.getURI());
    }
    catch (ClassCastException cce) {

      return false;
    }
  }

  /**
   * Returns the hashCode of a URI.
   *
   * @return the hashCode of a URI.
   */
  public int hashCode() {

    return uri.hashCode();
  }

  /**
   * Returns just the default Object clone.
   *
   * @return just the default Object clone.
   */
  public Object clone() {

    try {

      ModelResource cloned = (ModelResource) super.clone();
      cloned.uri = getURI();
      return cloned;
    }
    catch (CloneNotSupportedException e) {

      throw new RuntimeException("ModelResource not cloneable");
    }
  }
}
