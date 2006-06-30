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

import java.util.*;

// Third party packages
import org.apache.log4j.Logger;

/**
 * A constraint expression composed of the negation (logical NOT) of one
 * subexpressions.
 *
 * @created 2001-10-29
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/05/29 08:32:39 $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ConstraintNegation implements Constraint {

  /**
   * The logger
   */
  private final static Logger logger =
      Logger.getLogger(ConstraintNegation.class.getName());

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  private static final long serialVersionUID = 9147228997817064907L;

  /**
   * The negated expression.
   */
  private Constraint constraint;

  //
  // Constructor
  //

  /**
   * Construct a constraint negation.
   *
   * @param constraint  a non-<code>null</code> constraint
   * @throws IllegalArgumentException if <var>constraint</var> is
   *   <code>null</code>
   */
  public ConstraintNegation(Constraint constraint) {

    // Validate "expression" parameter
    if (constraint == null) {

      throw new IllegalArgumentException("Null \"constraint\" parameter");
    }

    // Initialize fields
    this.constraint = constraint;
  }

  public boolean isRepeating() {
    return false;
  }


  /**
   * Get a constraint element by index.
   *
   * @param index The constraint element to retrieve, from 0 to 3.
   * @return The constraint element referred to by index.
   */
  public ConstraintElement getElement(int index) {
    return constraint.getElement(index);
  }

  /**
   * Get all constraints which are variables. For back-compatibility, this
   * method currently ignores the fourth element of the triple.
   *
   * @return A set containing all variable constraints.
   */
  public Set getVariables() {
    return constraint.getVariables();
  }


  /**
   * tests if the inner constraint is a ConstraintIs.
   *
   * @return <code>true</code> if the inner constraint is a ConstraintIs class.
   */
  public boolean isInnerConstraintIs() {
    return constraint instanceof ConstraintIs;
  }

  /**
   * Convert this object to a string.
   *
   * @return A string representation of this object.
   */
  public String toString() {
    return "not " + constraint;
  }

  /**
   * Creates a relatively unique value representing this constraint.
   *
   * @return A numerical combination of the elements and the anchor
   */
  public int hashCode() {
    return constraint.hashCode() * -1;
  }

  /**
   * Equality is by value.
   *
   * @param object PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public boolean equals(Object object) {

    if (object == null) {
      return false;
    }

    if (object == this) {
      return true;
    }

    boolean returnValue = false;

    // Check that the given object is the correct class if so check each
    // element.
    if (object.getClass().equals(ConstraintNegation.class)) {

      ConstraintNegation tmpConstraint = (ConstraintNegation) object;
      returnValue = constraint.equals(tmpConstraint.constraint);
    }

    return returnValue;
  }
}
