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

import org.jrdf.graph.*;

import java.io.Serializable;

/**
 * An object which contains the various parameters in a related query.
 *
 * @created 2004-10-20
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class RelatedExpression implements Cloneable, Serializable {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = 3476137906321174349L;

  /**
   * The optionally empty base node.
   */
  private URIReference baseNode;

  /**
   * The optionally 0 limit clause.  0 indicating no limit.
   */
  private int limit = 0;

  /**
   * The optionally 0 threshold clause.  0 indicating no threshold.
   */
  private double threshold = 0.0d;

  /**
   * Create a new related expression.
   *
   * @param newBaseNode the base node to start from.
   * @param newLimit how many results to limit returning.
   * @param newThreshold the threshold to remove results with.
   */
  public RelatedExpression(URIReference newBaseNode, int newLimit,
      double newThreshold) {
    baseNode = newBaseNode;
    limit = newLimit;
    threshold = newThreshold;
  }

  /**
   * Returns the base node value.
   *
   * @return the base node value.
   */
  public URIReference getBaseNode() {
    return baseNode;
  }

  /**
   * Returns the limit value.
   *
   * @return the limit value.
   */
  public int getLimit() {
    return limit;
  }

  /**
   * Returns the threshold value.
   *
   * @return the threshold value.
   */
  public double getThresholdValue() {
    return threshold;
  }
}
