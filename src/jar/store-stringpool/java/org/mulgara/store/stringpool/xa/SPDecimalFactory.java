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

// Java 2 standard packages
import java.util.*;
import java.net.URI;
import java.nio.ByteBuffer;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.rdf.XSD;
import org.mulgara.query.rdf.XSDAbbrev;
import org.mulgara.store.stringpool.*;


/**
 * A factory for SPDecimalImpl objects.
 *
 * @created 2004-10-05
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/03/11 04:15:22 $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public final class SPDecimalFactory implements SPTypedLiteralFactory {

  @SuppressWarnings("unused")
  private final static Logger logger = Logger.getLogger(SPDecimalFactory.class);

  private final static URI[] TYPE_URIS = {
      URI.create(XSD.NAMESPACE + "decimal"),
      URI.create(XSD.NAMESPACE + "integer"),
      URI.create(XSD.NAMESPACE + "nonPositiveInteger"),
      URI.create(XSD.NAMESPACE + "negativeInteger"),
      URI.create(XSD.NAMESPACE + "long"),
      URI.create(XSD.NAMESPACE + "int"),
      URI.create(XSD.NAMESPACE + "short"),
      URI.create(XSD.NAMESPACE + "byte"),
      URI.create(XSD.NAMESPACE + "nonNegativeInteger"),
      URI.create(XSD.NAMESPACE + "unsignedLong"),
      URI.create(XSD.NAMESPACE + "unsignedInt"),
      URI.create(XSD.NAMESPACE + "unsignedShort"),
      URI.create(XSD.NAMESPACE + "unsignedByte"),
      URI.create(XSD.NAMESPACE + "positiveInteger"),
      // Hacks to pick up on missing namespaces
      URI.create(XSDAbbrev.NAMESPACE + "decimal"),
      URI.create(XSDAbbrev.NAMESPACE + "integer"),
      URI.create(XSDAbbrev.NAMESPACE + "nonPositiveInteger"),
      URI.create(XSDAbbrev.NAMESPACE + "negativeInteger"),
      URI.create(XSDAbbrev.NAMESPACE + "long"),
      URI.create(XSDAbbrev.NAMESPACE + "int"),
      URI.create(XSDAbbrev.NAMESPACE + "short"),
      URI.create(XSDAbbrev.NAMESPACE + "byte"),
      URI.create(XSDAbbrev.NAMESPACE + "nonNegativeInteger"),
      URI.create(XSDAbbrev.NAMESPACE + "unsignedLong"),
      URI.create(XSDAbbrev.NAMESPACE + "unsignedInt"),
      URI.create(XSDAbbrev.NAMESPACE + "unsignedShort"),
      URI.create(XSDAbbrev.NAMESPACE + "unsignedByte"),
      URI.create(XSDAbbrev.NAMESPACE + "positiveInteger"),
      // Always add new entries at the end of this array.
  };

  private final static Map<URI,Integer> uriToSubtypeIdMap;
  static {
    // Populate the uriToSubtypeIdMap.
    uriToSubtypeIdMap = new HashMap<URI,Integer>();
    for (int i = 0; i < TYPE_URIS.length; ++i) {
      uriToSubtypeIdMap.put(TYPE_URIS[i], new Integer(i));
    }
  }


  public int getTypeId() {
    return SPDecimalImpl.TYPE_ID;
  }


  /**
   * Returns the type URIs for the objects created by this factory.
   */
  public Set<URI> getTypeURIs() {
    return Collections.unmodifiableSet(uriToSubtypeIdMap.keySet());
  }


  public SPTypedLiteral newSPTypedLiteral(URI typeURI, String lexicalForm) {
    Integer subtypeIdI = (Integer)uriToSubtypeIdMap.get(typeURI);
    if (subtypeIdI == null) {
      throw new IllegalArgumentException("Invalid type URI: " + typeURI);
    }
    return new SPDecimalImpl(subtypeIdI.intValue(), typeURI, lexicalForm);
  }


  public SPTypedLiteral newSPTypedLiteral(int subtypeId, ByteBuffer data) {
    if (subtypeId < 0 || subtypeId >= TYPE_URIS.length) {
      throw new IllegalArgumentException("Invalid subtype ID: " + subtypeId);
    }
    return new SPDecimalImpl(subtypeId, TYPE_URIS[subtypeId], data);
  }

}
