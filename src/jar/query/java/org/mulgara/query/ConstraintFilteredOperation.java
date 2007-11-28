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
package org.mulgara.query;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A constraint operation where the result uses an intersection of variables from the parameters.
 *
 * @created Aug 20, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class ConstraintFilteredOperation extends ConstraintOperation {

  /**
   * Create a binary operation for requiring intersecting variables.
   * @param lhs The first constraint operations.
   * @param rhs The second constraint operations.
   */
  public ConstraintFilteredOperation(ConstraintExpression lhs, ConstraintExpression rhs) {
    super(lhs, rhs);
  }


  /**
   * Create an operation for requiring intersecting variables.
   * @param elements The list of expressions to use as parameters.
   */
  public ConstraintFilteredOperation(List<ConstraintExpression> elements) {
    super(elements);
  }

  /**
   * Remove the constraint expressions from the product that have non-intersecting variables.
   *
   * @param product The list of constraints to test and modify.
   */
  protected void filter(List<ConstraintExpression> product) {
  
    Set<Variable> o1 = new HashSet<Variable>();
  
    // Variables which occur at least once.
    Set<Variable> o2 = new HashSet<Variable>();
  
    // Variables which occur two or more times.
    // Get a set of variables which occur two or more times.
    for (ConstraintExpression oc: product) {
  
      Set<Variable> ocVars = oc.getVariables();
      Set<Variable> vars = new HashSet<Variable>(ocVars);
      vars.retainAll(o1);
      o2.addAll(vars);
      o1.addAll(ocVars);
    }
  
    // remove the expressions which have non-intersecting variables
    for (Iterator<ConstraintExpression> pIt = product.iterator(); pIt.hasNext(); ) {
  
      ConstraintExpression oc = pIt.next();
      Set<Variable> vars = new HashSet<Variable>(oc.getVariables());
      vars.retainAll(o2);
  
      if (vars.isEmpty()) pIt.remove();
    }
  }

}