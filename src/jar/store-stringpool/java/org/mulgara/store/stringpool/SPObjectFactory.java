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

package org.mulgara.store.stringpool;

// Java 2 standard packages
import java.util.Date;
import java.net.URI;
import java.nio.ByteBuffer;


/**
 * @created 2004-07-04
 *
 * @author <a href="http://staff.pisoftware.com/david">David Makepeace</a>
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/02/20 10:26:19 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2003
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface SPObjectFactory {

  public static final int INVALID_TYPE_ID = 0;

  public SPObject createSPObjectFromEncodedString(String encodedString);
  public SPObject createSPObjectFromBackupEncodedString(String encodedString);

  public SPString newSPString(String str);
  public SPURI newSPURI(URI uri);
  public SPTypedLiteral newSPTypedLiteral(String lexicalForm, URI typeURI);
  public SPDouble newSPDouble(double d);

  public SPObject newSPObject(org.jrdf.graph.Node rdfNode);

  public SPTypedLiteralFactory getSPTypedLiteralFactory(URI typeURI);
  public SPTypedLiteralFactory getSPTypedLiteralFactory(int typeId);
  public int getTypeId(URI typeURI);
  public SPObject newSPObject(
      SPObject.TypeCategory typeCategory, int typeId, int subtypeId,
      ByteBuffer data
  );

}
