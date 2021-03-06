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

package org.mulgara.content;

// Local packages
import org.mulgara.content.Content;
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerException;
import org.mulgara.query.GraphResource;
import org.mulgara.server.Session;

/**
 * Interface for loading Content into a Graph (GraphExpression). Similar to a
 * {@link ContentHandler} but is reponsible for inserting global statements
 * directly into the destination.
 *
 * Note: This is a temporary measure until we can support reenterant transactions.
 *
 * @created 2004-10-29
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:50 $
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
public interface ContentLoader extends ContentHandler {
  /**
   * Processes and Loads the content and inserts the statements into the
   * GraphResource.
   *
   * @param content Content
   * @param destination GraphResource
   * @param session Session
   * @throws ContentHandlerException
   * @return long The number of statements loaded
   */
  public long load(Content content, GraphResource destination,
      Session session) throws ContentHandlerException;

}
