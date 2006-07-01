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
 * The Initial Developer of the Original Code is Andrew Newman (C) 2005
 * All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.client.jena.model;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.shared.*;

//Mulgara packages
import org.mulgara.query.*;
import org.mulgara.server.*;

/**
 * A Jena TransactionHandler for a Mulgara Session.
 *
 * <p>An instance of this class is usually obtained via a ClientGraph.</p>
 *
 * @created 2005-01-18
 *
 * @author Chris Wilper
 * @author Andrew Newman
 *
 * @version $Revision: 1.1 $
 *
 * @modified $Date: 2005/01/28 20:07:31 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2005 Andrew Newman
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ClientTransactionHandler extends TransactionHandlerBase
    implements TransactionHandler {

  /**
   * Session to handle transactions for
   */
  private Session session;

  /**
   * Construct a ClientTransactionHandler for the given session.
   *
   * @param session Session
   */
  public ClientTransactionHandler(Session session) {

    this.session = session;
  }

  /**
   * true.
   *
   * @return boolean
   */
  public boolean transactionsSupported() {

    return true;
  }

  /**
   * Start a transaction.
   *
   * @throws JenaException
   */
  public void begin() throws JenaException {

    try {

      session.setAutoCommit(false);
    }
    catch (QueryException queryException) {

      throw new JenaException("Failed to begin transaction", queryException);
    }
  }

  /**
   * Abort a transaction.
   *
   * @throws RuntimeException
   */
  public void abort() throws RuntimeException {

    try {

      session.rollback();
    }
    catch (QueryException queryException) {

      throw new JenaException("Failed to rollback transaction", queryException);
    }
  }

  /**
   * Commit transaction and go back to auto-commit mode.
   *
   * @throws JenaException
   */
  public void commit() throws JenaException {

    try {

      session.setAutoCommit(true);
    }
    catch (QueryException queryException) {

      throw new JenaException("Failed to commit transaction", queryException);
    }
  }
}
