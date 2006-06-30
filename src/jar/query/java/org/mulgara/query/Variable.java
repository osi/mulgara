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

/**
 * Variable nodes.
 *
 * @created 2001-07-31
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2004
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Variable implements ConstraintElement {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = -3242803845875986693L;

  /**
   * Description of the Field
   */
  public final static Variable FROM = new Variable("_from");

  /**
   * The <var>name</var> property.
   */
  private String name;

  /**
   * Create a new variable.
   *
   * @param name the variable name (no leading <code>$</code> character)
   * @throws IllegalArgumentException if <var>name</var> is <code>null</code>
   */
  public Variable(String name) {

    // Validate "name" parameter
    if (name == null) {

      throw new IllegalArgumentException("Null \"name\" parameter");
    }

    if (name.indexOf(" ") != -1) {

      throw new IllegalArgumentException("\"" + name +
          "\" is a not a variable name");
    }

    // Initialize fields
    this.name = name;
  }

  /**
   * Accessor for the <var>name</var> property.
   *
   * @return The Name value
   */
  public String getName() {

    return name;
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public Object clone() {

    if (name.equals("_from")) {

      return FROM;
    }
    else {

      try {
        return super.clone();
      }
      catch (CloneNotSupportedException e) {
        // Should never happen
        throw new InternalError(e.toString());
      }
    }
  }

  /**
   * Variables are equal by <var>name</var> .
   *
   * @param object PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public boolean equals(Object object) {

    if ( (object == null) || !Variable.class.equals(object.getClass())) {

      return false;
    }

    return (object == this) || name.equals( ( (Variable) object).name);
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public int hashCode() {

    return name.hashCode();
  }

  /**
   * Legible representation of the variable.
   *
   * @return a <q>$</q> prefixed to the <var>name</var>
   */
  public String toString() {

    return "$" + name;
  }
}
