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

package org.kowari.jena;

// Third party packages
import com.hp.hpl.jena.graph.impl.TransactionHandlerBase;
import com.hp.hpl.jena.shared.*;

// Local packages
import org.kowari.server.Session;

// Log4j
import org.apache.log4j.*;
import org.kowari.query.*;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.TransactionHandler} as a
 * wrapper around a {@link Session}.
 *
 * @created 2003-12-01
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KowariTransactionHandler extends TransactionHandlerBase {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(KowariTransactionHandler.class.getName());

  /**
   * The current session.
   */
  protected Session session;

  /**
   * Create a transaction handler around the given session.
   *
   * @param newSession the session generated from database.getNewSession().
   */
  public KowariTransactionHandler(Session newSession) {
    session = newSession;
  }

  public Object executeInTransaction(Command c) {

    if (logger.isInfoEnabled()) {
      logger.info("Executing in transaction");
    }

    begin();
    try {

      Object result = null;

      if (c != null) {

        result = c.execute();
        commit();
      }

      return result;
    }
    catch (Exception e) {
      abort();
      throw new JenaException(e);
    }
  }


  /**
   * This handler supports transactions. There is a single write lock but all
   * sessions have access to the write phase of the store - just one at a time.
   *
   * @return always true - all sessions support transactions.
   */
  public boolean transactionsSupported() {

    return true;
  }

  /**
   * If transactions are supported, begin a new transaction.
   *
   * Calls setAutoCommit(false) on the current database session.
   *
   * @throws JenaException if the session failed to acquire the transaction.
   */
  public void begin() throws JenaException {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Beginning transaction");
      }
      session.setAutoCommit(false);
    }
    catch (QueryException ex) {
      throw new JenaException("Failed to begin transaction", ex);
    }
  }

  /**
   * If transactions are supported and there is a transaction in progress,
   * abort it. If transactions are not supported, or there is no transaction
   * in progress, throw an UnsupportedOperationException.
   *
   * Calls rollback() on the current database sessions.
   *
   * @throws JenaException if the session failed to rollback the transaction.
   */
  public void abort() throws RuntimeException {
    try {
      session.rollback();
      session.setAutoCommit(true);
    }
    catch (QueryException ex) {
      throw new JenaException("Failed to rollback transaction", ex);
    }
  }

  /**
   * If transactions are supported and there is a tranaction in progress,
   * commit it. If transactions are not supported, or there is no transaction
   * in progress, throw an UnsupportedOperationException.
   *
   * Calls setAutoCommit(true) on the current database sessions.  Which will
   * commit if possible or rollback if there was an exception when trying to
   * commit.
   *
   * @throws JenaException if the session failed to rollback the transaction.
   */
  public void commit() throws JenaException {
    try {
      if (logger.isInfoEnabled()) {
        logger.info("Committing transaction");
      }
      session.commit();
      session.setAutoCommit(true);
    }
    catch (QueryException ex) {
      throw new JenaException("Failed to commit transaction", ex);
    }
  }
}
