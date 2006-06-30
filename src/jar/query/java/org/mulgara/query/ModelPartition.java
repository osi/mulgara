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


// Third party packages
import org.apache.log4j.Category;

/**
 * A model expression composed of the union of two subexpressions.
 *
 *
 * @created 2002-05-20
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2002-2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ModelPartition extends ModelOperation {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = 8660358035003409731L;

  /**
   * Logger.
   */
  private final static Category logger =
    Category.getInstance(ModelPartition.class.getName());

  //
  // Constructor
  //

  /**
   * Construct a model union.
   *
   * @param lhs a non-<code>null</code> model expression
   * @param rhs another non-<code>null</code> model expression
   */
  public ModelPartition(ModelExpression lhs, ModelExpression rhs) {
    super(lhs, rhs);
  }

  /**
   * METHOD TO DO
   *
   * @param constraint PARAMETER TO DO
   * @param transformation PARAMETER TO DO
   * @param modelProperty PARAMETER TO DO
   * @param systemModel PARAMETER TO DO
   * @param variableFactory PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws TransformationException EXCEPTION TO DO
   */
  public ConstraintExpression toConstraintExpression(Constraint constraint,

  // (s p o m)
  Transformation transformation, Value modelProperty,
  // tucana:model
  Value systemModel,
  // #SYSTEM
  VariableFactory variableFactory) throws TransformationException {

    logger.warn("Replacing partition operation with disjunction");

    return new ConstraintDisjunction(getLHS().toConstraintExpression(constraint,
        transformation, modelProperty, systemModel, variableFactory),
      getRHS().toConstraintExpression(constraint, transformation,
        modelProperty, systemModel, variableFactory));
  }

  /**
   * Legible representation
   *
   * @return RETURNED VALUE TO DO
   */
  public String toString() {

    return "(" + getLHS() + " par " + getRHS() + ")";
  }
}
