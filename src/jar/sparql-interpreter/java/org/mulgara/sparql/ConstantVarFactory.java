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

import org.mulgara.query.Variable;

/**
 * Creates variables to use for constants.
 *
 * @created Jun 30, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ConstantVarFactory {

  /** A label to use for all "constant" variables. */
  private static final String PREFIX = "c";

  /** The internal incrementing counter for identifying variables. */
  private int id;

  /** Creates a new factory, with a fresh counter. */
  ConstantVarFactory() {
    id = 0;
  }

  /**
   * Allocate a new variable.
   * @return The new variable.
   */
  public Variable newVar() {
    return new Variable(PREFIX + id++);
  }
}
