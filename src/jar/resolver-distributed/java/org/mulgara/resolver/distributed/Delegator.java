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

package org.mulgara.resolver.distributed;

import org.mulgara.query.Constraint;
import org.mulgara.query.LocalNode;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.query.QueryException;


/**
 * Inteface for an object capable of resolving a constraint
 *
 * @created 2007-03-20
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: $
 * @modified $Date: $
 * @maintenanceAuthor $Author: $
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface Delegator {

  /**
   * Resolve a given constraint down to the appropriate resolution.
   * @param constraint The constraint to resolve.
   * @param model The LocalNode containing the model
   * @throws QueryException A delegator specific problem occurred resolving the constraint.
   */
  public Resolution resolve(Constraint constraint, LocalNode model) throws QueryException;
}
