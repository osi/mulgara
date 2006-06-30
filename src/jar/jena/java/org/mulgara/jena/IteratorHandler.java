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
 * The Initial Developer of the Original Code is Andrew Newman Copyright (C)
 * 2005. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.jena;

// Jena
import com.hp.hpl.jena.util.iterator.ClosableIterator;

/**
 * Handles the removal of iterators created through Jena.
 *
 * @created 2005-01-18
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/01/27 11:19:08 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2005 Andrew Newman
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface IteratorHandler {

  /**
   * Register a iterator.
   *
   * @param iterator to register.
   */
  public void registerIterator(ClosableIterator iterator);


  /**
   * Callback from a iterator to inform the graph that it was successfully
   * closed.  All iterators must be closed as soon as possible, at in the least
   * when the graph is closed.
   *
   * @param iterator the iterator to find in the list and remove.
   */
  public void deregisterIterator(ClosableIterator iterator);

  /**
   * Close all registered iterators.
   */
  public void close();
}
