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
 * Indicates a UI request to record timing information for executing an operation.
 * @created Aug 17, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class SetTime extends BooleanSetCommand {
  
  private static final String SET_TIME = "Time keeping has been set: ";

  /**
   * Create a command to set timing on or off.
   * @param option The value to set the time recording to.
   */
  public SetTime(boolean option) {
    super(option);
  }

  /**
   * Does nothing at the client, except to indicate that time keeping records are required.
   */
  public Object execute(Connection conn) throws Exception {
    return setResultMessage(SET_TIME + (isOn() ? ON : OFF));
  }
}
