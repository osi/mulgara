/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.rosenlaw.com/OSL3.0.htm
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * This file is an original work developed by Netymon Pty Ltd
 * (http://www.netymon.com, mailto:mail@netymon.com). Portions created
 * by Netymon Pty Ltd are Copyright (c) 2006 Netymon Pty Ltd.
 * All Rights Reserved.
 */

package org.mulgara.resolver;

// Java2 packages
import java.util.HashMap;
import java.util.Map;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.server.Session;

/**
 * Manages transactions within Mulgara.
 *
 * see http://mulgara.org/confluence/display/dev/Transaction+Architecture
 *
 * Maintains association between Answer's and TransactionContext's.
 * Manages tracking the ownership of the write-lock.
 * Maintains the write-queue and any timeout algorithm desired.
 * Provides new/existing TransactionContext's to DatabaseSession on request.
 *    Note: Returns new context unless Session is currently in a User Demarcated Transaction.
 * 
 *
 * @created 2006-10-06
 *
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 *
 * @version $Revision: $
 *
 * @modified $Date: $
 *
 * @maintenanceAuthor $Author: $
 *
 * @company <A href="mailto:mail@netymon.com">Netymon Pty Ltd</A>
 *
 * @copyright &copy;2006 <a href="http://www.netymon.com/">Netymon Pty Ltd</a>
 *
 * @licence Open Software License v3.0</a>
 */

public class MulgaraTransactionManager {
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(MulgaraTransactionManager.class.getName());

  // Write lock is associated with a session.
  private Session currentWritingSession;
  private MulgaraTransaction userTransaction;

  /** Map from session to transaction for all 'write' transactions that have been rolledback. */
  private Map failedSessions;

  /** Map from thread to associated transaction. */
  private Map activeTransactions;

  private TransactionManager transactionManager;

  private Object writeLockMutex;

  public MulgaraTransactionManager(TransactionManager transactionManager) {
    this.currentWritingSession = null;
    this.userTransaction = null;

    this.failedSessions = new HashMap();

    this.transactionManager = transactionManager;
    this.writeLockMutex = new Object();
  }

  /**
   * Allows DatabaseSession to initiate/obtain a transaction.
   * <ul>
   * <li>If the Session holds the write lock, return the current Write-Transaction.</li>
   * <li>If the Session does not hold the write lock and requests a read-only transaction,
   *     create a new ro-transaction object and return it.</li>
   * <li>If the Session does not hold the write lock and requests a read-write transaction,
   *     obtain the write-lock, create a new transaction object and return it.</li>
   * </ul>
   */
  public synchronized MulgaraTransaction getTransaction(DatabaseSession session, boolean write) throws MulgaraTransactionException {
    if (session == currentWritingSession) {
      return userTransaction;
    } 

    if (write) {
      obtainWriteLock(session);
    }

//    FIXME: Need to finish 1-N DS-OC and provide this method - should really be newOperationContext.
    return new MulgaraTransaction(this, session.getOperationContext());
  }


  public synchronized MulgaraTransaction getTransaction()
      throws MulgaraTransactionException {
    MulgaraTransaction transaction = (MulgaraTransaction)activeTransactions.get(Thread.currentThread());
    if (transaction != null) {
      return transaction;
    } else {
      throw new MulgaraTransactionException("No transaction assoicated with current thread");
    }
  }

  private synchronized void obtainWriteLock(Session session)
      throws MulgaraTransactionException {
    while (currentWritingSession != null) {
      try {
        writeLockMutex.wait();
      } catch (InterruptedException ei) {
        throw new MulgaraTransactionException("Interrupted while waiting for write lock", ei);
      }
    }
    currentWritingSession = session;
  }

  private synchronized void releaseWriteLock() {
    currentWritingSession = null;
    userTransaction = null;
    writeLockMutex.notify();
  }


  public synchronized void commit(Session session) throws MulgaraTransactionException {
    setAutoCommit(session, true);
    setAutoCommit(session, false);
  }


  /**
   * This is an explicit, user-specified rollback.
   * This 
   * This needs to be distinguished from an implicit rollback triggered by failure.
   */
  public synchronized void rollback(Session session) throws MulgaraTransactionException {
    if (session == currentWritingSession) {
      try {
        userTransaction.explicitRollback();
        finalizeTransaction();
      } finally {
        failedSessions.put(currentWritingSession, userTransaction);
        userTransaction = null;
        currentWritingSession = null;
        releaseWriteLock();
      }
    } else {
      // We have a problem - rollback called on session that doesn't have a transaction active.
    }
  }

  public synchronized void setAutoCommit(Session session, boolean autoCommit)
      throws MulgaraTransactionException {
    if (session == currentWritingSession && failedSessions.containsKey(session)) {
      // CRITICAL ERROR - transaction failed, but we did not finalise it.
    }

    if (session == currentWritingSession || failedSessions.containsKey(session)) {
      if (autoCommit) {
        // AutoCommit off -> on === branch on current state of transaction.
        if (session == currentWritingSession) {
          // Within active transaction - commit and finalise.
          try {
          } finally {
            releaseWriteLock();
          }
        } else {
          // Within failed transaction - cleanup and finalise.
          failedSessions.remove(session);
        }
      } else {
        // AutoCommit off -> off === no-op. Log info.
      }
    } else {
      if (autoCommit) {
        // AutoCommit on -> on === no-op.  Log info.
      } else {
        // AutoCommit on -> off == Start new transaction.
        obtainWriteLock(session);
//        FIXME: finish DS-OC first.
//        userTransaction = new MulgaraTransaction(this, session.newOperationContext(true));
        currentWritingSession = session;
      }
    }
  }

  public synchronized void closingSession(Session session) throws MulgaraTransactionException {
    // Check if we hold the write lock, if we do then rollback and throw exception.
    // Regardless we need to close all associated Answer objects
  }

  public void finalizeTransaction() {
    throw new IllegalStateException("mmmm I was doing something here. I need to remember what.");
  }

  //
  // Transaction livecycle callbacks.
  //

  public synchronized Transaction transactionStart(MulgaraTransaction transaction)
      throws MulgaraTransactionException {
    try {
      transactionManager.begin();
      Transaction jtaTrans = transactionManager.getTransaction();

      activeTransactions.put(Thread.currentThread(), transaction);

      return jtaTrans;
    } catch (Exception e) {
      throw new MulgaraTransactionException("Transaction Begin Failed", e);
    }
  }

  public synchronized void transactionResumed(MulgaraTransaction transaction) 
      throws MulgaraTransactionException {
    if (activeTransactions.get(Thread.currentThread()) != null) {
      throw new MulgaraTransactionException(
          "Attempt to resume transaction in already activated thread");
    } else if (activeTransactions.containsValue(transaction)) {
      throw new MulgaraTransactionException(
          "Attempt to resume resumed transaction");
    }
    
    try {
      transactionManager.resume(transaction.getTransaction());
      activeTransactions.put(Thread.currentThread(), transaction);
    } catch (Exception e) {
      throw new MulgaraTransactionException("Resume Failed", e);
    }
  }

  public synchronized Transaction transactionSuspended(MulgaraTransaction transaction)
      throws MulgaraTransactionException {
    try {
      if (transaction != activeTransactions.get(Thread.currentThread())) {
        throw new MulgaraTransactionException(
            "Attempt to commit transaction from outside thread");
      }

      return transactionManager.suspend();
    } catch (Exception e) {
      throw new MulgaraTransactionException("Suspend failed", e);
    } finally {
      activeTransactions.remove(Thread.currentThread());
    }

  }

  public synchronized void transactionComplete(MulgaraTransaction transaction) 
      throws MulgaraTransactionException {
    try {
      transactionManager.commit();
      if (transaction == userTransaction) {
        releaseWriteLock();
      }
    } catch (Exception e) {
      throw new MulgaraTransactionException("Commit Failed", e);
    } finally {
      activeTransactions.remove(Thread.currentThread());
//    Remove transaction from Session's list.
    }
  }
}
