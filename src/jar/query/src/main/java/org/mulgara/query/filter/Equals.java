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


/**
 * The equality comparison for Values.
 *
 * @created Mar 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Equals extends BinaryTestFilter {

  /** Generated Serialization ID for RMI */
  private static final long serialVersionUID = -7289754671291518073L;

  /**
   * Creates an equality test operation
   * @param lhs The first term to compare
   * @param rhs The second term to compare
   */
  public Equals(RDFTerm lhs, RDFTerm rhs) {
    super(lhs, rhs);
  }

  /** @see org.mulgara.query.filter.BinaryTestFilter#testCmp() */
  boolean testCmp() throws QueryException {
    return lhs.equals(rhs);
  }

}
