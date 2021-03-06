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
 * Contributor(s):
 *   getModel() contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.query;

// Java 2 standard packages
import java.util.*;

// Third party packages
// import org.apache.log4j.Logger;

/**
 * A transitive constraint.  The constraint must take the form of an anchored
 * constraint (either $x <pred> <obj> or <subj> <pred> $z) followed by
 * unanchored constraint ($x <pred> $z).
 *
 * @created 2004-06-22
 *
 * @author Paul Gearon
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/05/29 08:32:40 $
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
public class TransitiveConstraint implements Constraint {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = -3828723182891026790L;

  // /** Logger. */
  // private final static Logger logger = Logger.getLogger(Constraint.class);

  /**
   * The constraint containing the anchored transitive predicate.
   */
  private final Constraint anchoredConstraint;

  /**
   * The constraint containing the unanchored transitive predicate.
   */
  private final Constraint unanchoredConstraint;

  /**
   * Constructor.
   * The parameters are expected to appear in the form {$var &lt;uri&gt; $var}
   * @param trans1 The anchored constraint defining the transitive predicate
   *   and beginning or ending point.
   * @param trans2 The constraint defining the transitive predicate.
   * @throws IllegalArgumentException If either trans is null.
   */
  public TransitiveConstraint(Constraint trans1, Constraint trans2)
      throws IllegalArgumentException {

    // Validate parameters
    if (trans1 == null || trans2 == null) {
      throw new IllegalArgumentException("Null constraints parameters");
    }

    // Initialize fields
    anchoredConstraint = trans1;
    unanchoredConstraint = trans2;
  }

  /**
   * Get the anchored constraint.
   *
   * @return The constraint element referred to by index.
   */
  public Constraint getAnchoredConstraint() {
    return anchoredConstraint;
  }

  public boolean isRepeating() {
    return false;
  }

  /**
   * Get the unanchored constraint.
   *
   * @return The constraint element referred to by index.
   */
  public Constraint getUnanchoredConstraint() {
    return unanchoredConstraint;
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
    if (object.getClass().equals(TransitiveConstraint.class)) {

      TransitiveConstraint tmpConstraint = (TransitiveConstraint) object;

      returnValue = this.anchoredConstraint.equals(tmpConstraint.anchoredConstraint) &&
          this.unanchoredConstraint.equals(tmpConstraint.unanchoredConstraint);
    }

    return returnValue;
  }

  /**
   * Creates a relatively unique value representing this constraint.
   *
   * @return A numerical combination of the elements and the anchor
   */
  public int hashCode() {
    return anchoredConstraint.hashCode() ^ unanchoredConstraint.hashCode();
  }


  /**
   * Get all constraints which are variables. For back-compatibility, this
   * method currently ignores the fourth element of the triple.
   *
   * @return A set containing all variable constraints.
   */
  public Set<Variable> getVariables() {
    Set<Variable> vars = new HashSet<Variable>();
    vars.addAll(unanchoredConstraint.getVariables());
    vars.addAll(anchoredConstraint.getVariables());
    return vars;
  }


  /**
   * Get a constraint element by index.
   *
   * @param index The constraint element to retrieve, from 0 to 3.
   * @return The constraint element referred to by index.
   */
  public ConstraintElement getElement(int index) {
    return unanchoredConstraint.getElement(index);
  }

  public ConstraintElement getModel() {
    return unanchoredConstraint.getModel();
  }

  /**
   * Creates a string representation of these constraints. A typical result
   * might be <code>[$x &lt;urn:foo&gt; 'bar' $0 ] :
   * {&lt;start&gt; &lt;urn:foo&gt; &lt;end&gt;}</code>.
   *
   * @return String containing all data pertinent to the constraint.
   */
  public String toString() {
    StringBuffer buffer = new StringBuffer("trans[");
    buffer.append(this.anchoredConstraint);
    buffer.append(" and ");
    buffer.append(this.unanchoredConstraint);
    buffer.append("]");
    return buffer.toString();
  }

  /**
   * Not sure what associativity would mean here, but it shouldn't be possible.
   * @return <code>false</code> to indicate that this operation is not associative.
   */
  public boolean isAssociative() {
    return false;
  }

}
