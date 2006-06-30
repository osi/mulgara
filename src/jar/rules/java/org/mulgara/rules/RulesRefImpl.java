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

package org.mulgara.rules;

// Java 2 standard packages
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Contains a reference to a local Rules object, while this object can be
 * shipped over RMI.
 *
 * @created 2005-6-23
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.1 $
 * @modified $Date: 2005/06/26 12:42:43 $
 * @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class RulesRefImpl extends UnicastRemoteObject implements RulesRef {

  /** The internal reference to the local object. */
  private Rules rules;

  /**
   * Principle constructor.
   */
  public RulesRefImpl(Rules rules) throws RemoteException {
    this.rules = rules;
  }

  /**
   * Retrieves the local rules reference.
   *
   * @return The local Rules object.
   * @throws RemoteException This should never happen, as this method is only
   *         for local access.
   */
  public Rules getRules() throws RemoteException {
    return rules;
  }

  private void writeObject(java.io.ObjectOutputStream out)
       throws java.io.IOException {
    java.io.IOException ioe = new java.io.IOException("This class should not be serialized");
    ioe.printStackTrace();
    throw ioe;
  }

}
