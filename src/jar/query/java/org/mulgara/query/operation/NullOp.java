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
package org.mulgara.query.operation;

import org.mulgara.connection.Connection;

/**
 * An AST element for commands that do not require processing.
 *
 * @created 2007-08-21
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class NullOp extends LocalCommand {

  /**
   * @see org.mulgara.operation.Command#isUICommand()
   */
  public boolean isUICommand() {
    return false;
  }

  /**
   * Do nothing.
   */
  public Object execute(Connection conn) throws Exception {
    return null;
  }

}
