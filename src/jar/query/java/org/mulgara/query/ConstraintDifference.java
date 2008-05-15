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

// Java 2 standard packages
import java.util.*;

/**
 * A constraint expression composed of the difference between two
 * subexpressions.
 *
 * @created 2005-03-08
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @copyright &copy; 2005 <A href="mailto:pgearon@users.sourceforge.net">Paul Gearon</A>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ConstraintDifference extends ConstraintBinaryOperation {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = 7601600010765365077L;

  //
  // Constructor
  //

  /**
   * Construct a constraint difference.
   *
   * @param minuend a non-<code>null</code> constraint expression
   * @param subtrahend another non-<code>null</code> constraint expression
   */
  public ConstraintDifference(ConstraintExpression minuend,
      ConstraintExpression subtrahend) {
    super(minuend, subtrahend);
  }

  /**
   * Gets the Filtered attribute of the ConstraintDifference object
   *
   * @return The Filtered value
   */
  public ConstraintDifference getFiltered() {

    List<ConstraintExpression> elements = new ArrayList<ConstraintExpression>(this.getElements());
    filter(elements);

    assert elements.size() == 2;
    return new ConstraintDifference(elements.get(0), elements.get(1));
  }


  /**
   * Gets the Name attribute of the ConstraintDifference object
   *
   * @return The Name value
   */
  String getName() {
    return " minus ";
  }

}
