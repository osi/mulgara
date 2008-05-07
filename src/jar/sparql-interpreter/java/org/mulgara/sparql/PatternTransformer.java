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
package org.mulgara.sparql;

import java.util.HashMap;
import java.util.Map;

import org.mulgara.parser.MulgaraParserException;
import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ConstraintFilter;
import org.mulgara.query.ConstraintOptionalJoin;
import org.mulgara.query.filter.And;
import org.mulgara.query.filter.Filter;
import org.mulgara.query.filter.value.Bool;

/**
 * This object transforms a {@link ConstraintExpression} into a minimized {@link ConstraintExpresion}.
 *
 * @created May 06, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class PatternTransformer {

  /**
   * Perform the mapping of the graph pattern and return the results as a {@link ConstraintExpression}.
   * @return The mapped constraint expression.
   */
  static public ConstraintExpression transform(ConstraintExpression constraints) throws MulgaraParserException {
    Transformer<? extends ConstraintExpression> tx = txMap.get(constraints);
    if (tx == null) return constraints;
    return tx.internalTx(constraints);
  }

  /** A mapping of constraint expressions to Transformers. */
  private static Map<Class<? extends ConstraintExpression>,Transformer<? extends ConstraintExpression>> txMap =
      new HashMap<Class<? extends ConstraintExpression>,Transformer<? extends ConstraintExpression>>();

  /**
   * The class for the mapping of {@link ConstraintExpression}s to {@link ConstraintExpression}s.
   */
  private static abstract class Transformer<T extends ConstraintExpression>  {
    /** An entry point for the tx operation. This method handles casting to be compatible with the generic template. */
    @SuppressWarnings("unchecked")
    public ConstraintExpression internalTx(ConstraintExpression constraints) throws MulgaraParserException {
      return tx((T)constraints);
    }
    public abstract ConstraintExpression tx(T constraints) throws MulgaraParserException;
    /** Identify the class to be mapped by the extension. */
    public abstract Class<T> getTxType();
  }

  /**
   * Utility method to add a transformer to the map, keyed on the class it transforms.
   * @param mapper The mapper to add to the map.
   */
  static void addToMap(Transformer<?> tx) {
    txMap.put(tx.getTxType(), tx);
  }

  /**
   * Initialize the mapping of patterns to the constraint builders.
   */
  static {
    addToMap(new FilterTx());
    addToMap(new LeftJoinTx());
  }

  /**
   * Creates a conjunction of filters, skipping any TRUE values on the way.
   * @param lhs The first filter to join
   * @param rhs The second filter to join
   * @return A new filter that represents the conjunction of the lhs and the rhs
   */
  private static Filter and(Filter lhs, Filter rhs) {
    if (lhs == Bool.TRUE) return rhs;
    if (rhs == Bool.TRUE) return lhs;
    return new And(lhs, rhs);
  }


  /**
   * Map filtered constraints to the flattening operation.
   *   Filter(X1,Filter(X2,A)) => Filter(X2 && X1, A)
   */
  private static class FilterTx extends Transformer<ConstraintFilter> {
    public Class<ConstraintFilter> getTxType() { return ConstraintFilter.class; }
    public ConstraintExpression tx(ConstraintFilter constraint) throws MulgaraParserException {
      ConstraintExpression subConstraint = constraint.getUnfilteredConstraint();
      if (subConstraint instanceof ConstraintFilter) {
        // found Filter(X1,Filter(X2,A))
        ConstraintFilter subFilter = (ConstraintFilter)transform(subConstraint); // Filter(X2,A)
        return new ConstraintFilter(subFilter.getUnfilteredConstraint(), and(subFilter.getFilter(), constraint.getFilter()));
      }
      return constraint;
    }
  }

  /**
   * Based on the syntactic (not algebraic) transformations:
   *   LeftJoin(A, Filter(X1, B), X2) => LeftJoin(A, B, X1 && X2)
   *   LeftJoin(A, LeftJoin(B, C, X1), X2) => LeftJoin(A, LeftJoin(B, C, true), X1 && X2)
   */
  private static class LeftJoinTx extends Transformer<ConstraintOptionalJoin> {
    public Class<ConstraintOptionalJoin> getTxType() { return ConstraintOptionalJoin.class; }
    public ConstraintExpression tx(ConstraintOptionalJoin leftJoin) throws MulgaraParserException {
      ConstraintExpression op = leftJoin.getOptional();
      if (op instanceof ConstraintFilter) {
        // found LeftJoin(A, Filter(X1, B), X2)
        ConstraintFilter filter = (ConstraintFilter)transform(op);  // Filter(X1, B)
        Filter f = and(filter.getFilter(), leftJoin.getFilter());  // X1 && X2
        return new ConstraintOptionalJoin(leftJoin.getMain(), filter.getUnfilteredConstraint(), f);
      }
      if (op instanceof ConstraintOptionalJoin) {
        // found LeftJoin(A, LeftJoin(B, C, X1), X2)
        ConstraintOptionalJoin subJoin = (ConstraintOptionalJoin)transform(op);  // LeftJoin(B, C, X1)
        ConstraintOptionalJoin newSubJoin = new ConstraintOptionalJoin(subJoin.getMain(), subJoin.getOptional(), Bool.TRUE);
        Filter newFilter = and(subJoin.getFilter(), leftJoin.getFilter());  // X1 && X2
        return new ConstraintOptionalJoin(leftJoin.getMain(), newSubJoin, newFilter);
      }
      return leftJoin;
    }
  }

}
