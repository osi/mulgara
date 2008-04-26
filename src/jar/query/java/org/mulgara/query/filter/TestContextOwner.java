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
package org.mulgara.query.filter;


/**
 * A test class for emulating a context ownership.
 *
 * @created Mar 31, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class TestContextOwner implements ContextOwner {
  /** The owned context */
  private Context ctx;
  
  /**
   * Create the test ownership.
   * @param ctx The context to own.
   */
  public TestContextOwner(Context ctx) { this.ctx = ctx; }

  /**
   * Updates the owned context.
   * @param ctx The context to update to.
   */
  public void setCurrentContext(Context ctx) { this.ctx = ctx; }

  /** @return the current context. */
  public Context getCurrentContext() { return ctx; }

}
