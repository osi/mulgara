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

package org.mulgara.resolver.spi;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.store.tuples.Tuples;

/**
 * A collection of localized RDF statements satisfying a constraint.
 *
 * @created 2004-04-22
 * @author <a href="http://staff.tucanatech.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:50 $ 
 * @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@tucanatech.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */

public interface Resolution extends Tuples
{
  /**
   * The original {@link Constraint} resolved to this {@link Resolution}.
   * @return the original {@link Constraint} passed to the
   *   {@link Resolver#resolve} method to generate this {@link Resolution}
   */
  public Constraint getConstraint();

  /**
   * If a {@link Resolver} represents a closed world, it may assert that the
   * {@link Resolution} contains every possible solution.
   *
   * This is useful because it can short-circuit the need to consult any
   * further {@link Resolver}s when evaluating a query.
   *
   * @return whether this {@link Resolution} includes every solution to the
   *   constraint
   */
  public boolean isComplete();
}
