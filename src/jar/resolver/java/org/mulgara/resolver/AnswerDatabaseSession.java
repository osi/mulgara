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

package org.mulgara.resolver;

// Local packages
import org.mulgara.query.*;

/**
 * Defines the extra requirements of {@link Answer}s upon
 * {@link DatabaseSession}.
 *
 * @created 2004-10-02
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:23 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface AnswerDatabaseSession {

  /**
   * Performs a query within an existing transaction.
   *
   * @param query the query to perform.
   * @return a non-<code>null</code> answer to the <var>query</var>
   * @throws QueryException if <var>query</var> can't be answered
   */
  public Answer innerQuery(Query query) throws QueryException;

  /**
   * Answers are registered so that the session can ensure that they are closed.
   *
   * @param answer the answer to register with this session.
   */
  public void registerAnswer(SubqueryAnswer answer);

  /**
   * Answers are deregistered to indicate that they have closed themselves.
   *
   * @param answer the answer to deregister.
   * @throws QueryException if the transactional block for this answer fails.
   */
  public void deregisterAnswer(SubqueryAnswer answer) throws QueryException;
}
