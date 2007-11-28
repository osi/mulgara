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

/**
 * This class indicates a command for the UI to set a local boolean property. 
 * @created Aug 17, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class BooleanSetCommand extends SetCommand {

  protected static final String ON = "on";
  protected static final String OFF = "off";

  /** Indicates that option has been set on or off. */
  private final boolean on;
  
  /**
   * Create a command to set and option on or off.
   * @param on <code>true</code> if the option is on.
   */
  public BooleanSetCommand(boolean on) {
    this.on = on;
  }
  
  /**
   * @return the set option
   */
  public boolean isOn() {
    return on;
  }
}
