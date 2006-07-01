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

package org.mulgara.client.jena.model;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*; // Capabilities


/**
 * A Jena Capabilities object representing a Mulgara session.
 *
 * @created 2001-08-16
 *
 * @author Chris Wilper
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/01/28 20:07:31 $
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
public class ClientCapabilities implements Capabilities {

  /**
   * true.
   */
  public boolean sizeAccurate() {

    return true;
  }

  /**
   * true.
   */
  public boolean addAllowed() {

    return true;
  }

  /**
   * true.
   */
  public boolean addAllowed(boolean everyTriple) {

    return true;
  }

  /**
   * true.
   */
  public boolean deleteAllowed() {

    return true;
  }

  /**
   * true.
   */
  public boolean deleteAllowed(boolean everyTriple) {

    return true;
  }

  /**
   * false.
   */
  public boolean iteratorRemoveAllowed() {

    return false;
  }

  /**
   * true.
   */
  public boolean canBeEmpty() {

    return true;
  }

}
