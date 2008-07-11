/*
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


import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.ConstraintImpl;
import org.mulgara.query.ConstraintIs;
import org.mulgara.query.ConstraintNotOccurs;
import org.mulgara.query.ConstraintOccurs;
import org.mulgara.query.ConstraintOccursLessThan;
import org.mulgara.query.ConstraintOccursMoreThan;
import org.mulgara.query.Variable;

/**
 * Transforms constraint expressions to rename variables.
 *
 * @created May 19, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class VariableRenameTransformer extends IdentityTransformer {

  /** The variable to find and change. */
  private final Variable from;

  /** The new variable to use in place of <var>from</var>. */
  private final Variable to;

  /**
   * Creates a new transformer for mapping constraints with a replaced variable.
   * @param from The Variable to replace.
   * @param to The Variable to change to.
   */
  public VariableRenameTransformer(Variable from, Variable to) {
    this.from = from;
    this.to = to;
    initialize(new CNO(), new CO(), new COLT(), new COMT(), new CI(), new CIs());
  }

  // reimplement the individual construction code to replace variables

  /**
   * Updates ops when a constraint contains 4 of them.
   * @return An array of 4 update operands.
   */
  private ConstraintElement[] morphOps(Constraint c) {
    ConstraintElement[] ops = new ConstraintElement[4];
    for (int i = 0; i < ops.length; i++) {
      ConstraintElement e = c.getElement(i);
      if (e instanceof Variable && ((Variable)e).equals(from)) e = to;
      ops[i] = e;
    }
    return ops;
  }

  protected class CNO extends ConsNotOccurs {
    public ConstraintNotOccurs newConstraint(Constraint c) {
      return newHaving(morphOps(c));
    }
  }

  protected class CO extends ConsOccurs {
    public ConstraintOccurs newConstraint(Constraint c) {
      return newHaving(morphOps(c));
    }
  }

  protected class COLT extends ConsOccursLessThan {
    public ConstraintOccursLessThan newConstraint(Constraint c) {
      return newHaving(morphOps(c));
    }
  }

  protected class COMT extends ConsOccursMoreThan {
    public ConstraintOccursMoreThan newConstraint(Constraint c) {
      return newHaving(morphOps(c));
    }
  }

  protected class CI extends ConsImpl {
    public ConstraintImpl newConstraint(Constraint c) {
      ConstraintElement[] o = morphOps(c);
      return new ConstraintImpl(o[0], o[1], o[2], o[3]);
    }
  }

  protected class CIs extends ConsIs {
    public ConstraintIs newConstraint(Constraint c) {
      ConstraintElement[] o = morphOps(c);
      return new ConstraintIs(o[0], o[2], o[3]);
    }
  }

}
