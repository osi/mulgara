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

package org.mulgara.jena;

// Third party packages
import com.hp.hpl.jena.graph.Capabilities;

// Log4j
import org.apache.log4j.*;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.Capabilities} that
 * describes Kowari graphs.
 *
 * @created 2003-02-12
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KowariCapabilities implements Capabilities {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(KowariCapabilities.class.getName());

  /**
   * Create a capabilities object.
   */
  public KowariCapabilities() {

  }

  /**
   * Returns true - the size of the graph is accurate.
   *
   * @return true - the size of the graph is accurate.
   */
  public boolean sizeAccurate() {

    return true;
  }

  /**
   * Returns true - triples can be added to the graph.
   *
   * @return true - triples can be added to the graph.
   */
  public boolean addAllowed() {

    return true;
  }

  /**
   * Returns true - triples can be added to the graph.
   *
   * @return true - triples can be added to the graph.
   */
  public boolean addAllowed(boolean everyTriple) {

    return true;
  }

  /**
   * Returns true - triples can be removed from the graph.
   *
   * @return true - triples can be removed from the graph.
   */
  public boolean deleteAllowed() {

    return true;
  }

  /**
   * Returns true - the graph can have any triples removed from the graph.
   *
   * @return true - the graph can have any triples removed from the graph.
   */
  public boolean deleteAllowed(boolean everyTriple) {

    return true;
  }

  /**
   * Returns false - iterator does not support removals.
   *
   * @return false - the iterator does not support removals.
   */
  public boolean iteratorRemoveAllowed() {

    return false;
  }

  /**
   * Returns true the graph can be completely empty.
   *
   * @return true the graph can be completely empty.
   */
  public boolean canBeEmpty() {

    return true;
  }
}
