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

package org.mulgara.query.filter.value;

import org.mulgara.query.QueryException;
import org.mulgara.query.filter.AbstractContextOwner;

/**
 * Represents literal values that can be compared.
 *
 * @created Mar 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class AbstractComparable extends AbstractContextOwner implements ComparableExpression {

  /** {@inheritDoc} */
  public boolean lessThan(ComparableExpression v) throws QueryException {
    compatibilityTest(v);
    return compare(getValue(), v.getValue()) < 0;
  }

  /** {@inheritDoc} */
  public boolean greaterThan(ComparableExpression v) throws QueryException {
    compatibilityTest(v);
    return compare(getValue(), v.getValue()) > 0;
  }

  /** {@inheritDoc} */
  public boolean lessThanEqualTo(ComparableExpression v) throws QueryException {
    return !greaterThan(v);
  }

  /** {@inheritDoc} */
  public boolean greaterThanEqualTo(ComparableExpression v) throws QueryException {
    return !lessThan(v);
  }

  /** {@inheritDoc} */
  public boolean equals(ComparableExpression v) throws QueryException {
    return compare(getValue(), v.getValue()) == 0;
  }

  /** {@inheritDoc} */
  public boolean notEquals(ComparableExpression v) throws QueryException {
    return !equals(v);
  }

  /**
   * Tests a value to see if it is a simple literal, and throws an exception if it is.
   * Simple literals do a similar test when compared with a ComparableExpression.
   * @param v The comparable expression to test.
   * @throws QueryException If the comparable expression resolves to a {@link SimpleLiteral}.
   */
  private void compatibilityTest(ComparableExpression v) throws QueryException {
    if (v.isLiteral() && ((ValueLiteral)v).isSimple()) throw new QueryException("Type Error: cannot compare a simple literal with a: " + getClass().getSimpleName());
  }

  /**
   * Compares elements of the type handled by the implementing class.
   * @param left The LHS of the comparison
   * @param right The RHS of the comparison
   * @return -1 if left<right, +1 if left>right, 0 if left==right
   * @throws QueryException If getting the values for the comparison is invalid.
   */
  protected abstract int compare(Object left, Object right) throws QueryException;

}