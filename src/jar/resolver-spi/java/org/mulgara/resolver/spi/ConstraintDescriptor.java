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
 * The copyright to this file is held by:
 *   The Australian Commonwealth Government
 *   Department of Defense
 * Developed by Netymon Pty Ltd
 * under contract 4500430665
 * contributed to the Kowari Project under the
 *   Mozilla Public License version 1.1
 * per clause 4.1.3 of the above contract.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver.spi;

// Local packages
import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ModelExpression;
import org.mulgara.store.tuples.Tuples;

/**
 * Describes a Constraint object.
 *
 * This interface is used by ResolverFactory's to introduce custom private
 * constraint types to Kowari.  One alternative design would be to have
 * methods return the various Handlers required (initially two).  For the
 * moment we will provide an AbstractConstraintDescriptor that can provide
 * that functionality.
 *
 * @created 2005-05-02
 * @author <a href="http://netymon.com/people/staff/andrae">Andrae Muys</a>
 * @version $Revision: 1.4 $
 * @modified $Date: 2005/05/29 08:34:20 $ 
 * @maintenanceAuthor $Author: raboczi $
 * @copyright &copy;2005 <a href="http://www.defence.gov.au/">
 *      Australian Commonwealth Government, Department of Defence</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */

public interface ConstraintDescriptor extends ConstraintResolutionHandler,
                                              ConstraintModelRewrite
{
  public Class getConstraintClass();

  // NOTE: For convenient reference these are the signatures of the two
  // methods imported from the super-interfaces.
  /*
  public Constraint rewrite(ConstraintElement newModel, Constraint constraint) throws Exception;
  public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception;
  */
}
