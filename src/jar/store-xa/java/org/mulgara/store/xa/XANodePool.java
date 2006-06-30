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

package org.mulgara.store.xa;

import org.mulgara.store.nodepool.*;

/**
 * Interface for transactional NodePools.
 *
 * @created 2001-09-20
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/02/22 08:17:02 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface XANodePool extends SimpleXAResource, ReleaseNodeListener, NodePool {

  /**
   * Obtain a new handle on a read-phase of the NodePool.
   */
  public XANodePool newReadOnlyNodePool();

  /**
   * Obtain a handle on the write-phase of the NodePool.
   */
  public XANodePool newWritableNodePool();

  /**
   * Add a new listener for notification of node allocations.
   */
  public void addNewNodeListener(NewNodeListener l);

  /**
   * Remove a listener from notification of node allocations.
   */
  public void removeNewNodeListener(NewNodeListener l);

  /**
   * Advise that this node pool is no longer needed.
   */
  public void close() throws NodePoolException;

  /**
   * Close this node pool, if it is currently open, and remove all files
   * associated with it.
   */
  public void delete() throws NodePoolException;

}
