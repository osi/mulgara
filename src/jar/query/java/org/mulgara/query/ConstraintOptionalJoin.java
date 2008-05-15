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


import org.mulgara.query.filter.Filter;
import org.mulgara.query.filter.value.Bool;

/**
 * A constraint expression composed of a left-out-join conjunction of two subexpressions
 *
 * @created Apr 2, 2008
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */

public class ConstraintOptionalJoin extends ConstraintBinaryOperation {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  private static final long serialVersionUID = -6776376724963178229L;

  private Filter filter = Bool.TRUE;

  /**
   * Construct a constraint left-outer-join.
   * @param lhs a non-<code>null</code> constraint expression
   * @param rhs another non-<code>null</code> constraint expression
   */
  public ConstraintOptionalJoin(ConstraintExpression lhs, ConstraintExpression rhs) {
    super(lhs, rhs);
  }

  /**
   * Construct a constraint left-outer-join.
   * @param lhs a non-<code>null</code> constraint expression
   * @param rhs another non-<code>null</code> constraint expression
   * @param filter Filters the join.
   */
  public ConstraintOptionalJoin(ConstraintExpression lhs, ConstraintExpression rhs, Filter filter) {
    super(lhs, rhs);
    if (filter == null) throw new IllegalArgumentException("Null \"filter\" parameter");
    this.filter = filter;
  }

  /**
   * @return Get the LHS "main" parameter
   */
  public ConstraintExpression getMain() {
    return getLhs();
  }

  /**
   * @return Get the RHS "optional" parameter
   */
  public ConstraintExpression getOptional() {
    return getRhs();
  }

  /**
   * @return Get the filter parameter
   */
  public Filter getFilter() {
    return filter;
  }

  /**
   * Gets the Name attribute of the ConstraintOptionalJoin object
   * @return The Name value
   */
  String getName() {
    return " optional ";
  }
}
