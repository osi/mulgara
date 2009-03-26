/**
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */
package org.mulgara.query.filter;

import org.mulgara.query.QueryException;
import org.mulgara.query.filter.value.Bool;


/**
 * An inversion of a test.
 *
 * @created Mar 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Not extends AbstractFilterValue implements Filter {

  /** Generated Serialization ID for RMI */
  private static final long serialVersionUID = 1225895946822519277L;

  /** The filter to invert. Local storage of operands[0]. */
  Filter operand;

  /**
   * Create an inversion of a filter
   * @param operand The filter to invert
   */
  public Not(Filter operand) {
    super(operand);
    this.operand = operand;
  }

  /**
   * @see org.mulgara.query.filter.Filter#test(Context)
   */
  public boolean test(Context context) throws QueryException {
    setCurrentContext(context);
    return !operand.test(context);
  }

  /** @see org.mulgara.query.filter.AbstractFilterValue#resolve() */
  protected RDFTerm resolve() throws QueryException {
    return operand.test(getCurrentContext()) ? Bool.FALSE : Bool.TRUE;
  }

}
