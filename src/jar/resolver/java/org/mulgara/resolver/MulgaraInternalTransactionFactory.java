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
 *
 * Work deriving from MulgaraTransactionManager Copyright (c) 2007 Topaz
 * Foundation under contract by Andrae Muys (mailto:andrae@netymon.com).
 */

package org.mulgara.resolver;

// Java2 packages
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.query.MulgaraTransactionException;
import org.mulgara.transaction.TransactionManagerFactory;
import org.mulgara.util.Assoc1toNMap;
import org.mulgara.util.StackTrace;

/**
 * Implements the internal transaction controls offered by Session.
 *
 * @created 2006-10-06
 *
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 *
 * @company <A href="mailto:mail@netymon.com">Netymon Pty Ltd</A>
 *
 * @copyright &copy;2006 <a href="http://www.netymon.com/">Netymon Pty Ltd</a>
 *
 * @licence Open Software License v3.0</a>
 */

public class MulgaraInternalTransactionFactory extends MulgaraTransactionFactory {
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(MulgaraInternalTransactionFactory.class.getName());

  private boolean autoCommit;

  /** Set of sessions whose transactions have been rolledback.*/
  private Set<DatabaseSession> failedSessions;

  /** Map of threads to active transactions. */
  private Map<Thread, MulgaraTransaction> activeTransactions;

  private Assoc1toNMap<DatabaseSession, MulgaraTransaction> sessionXAMap;

  private final TransactionManager transactionManager;

  public MulgaraInternalTransactionFactory(MulgaraTransactionManager manager, TransactionManagerFactory transactionManagerFactory) {
    super(manager);
    this.autoCommit = true;

    this.failedSessions = new HashSet<DatabaseSession>();
    this.activeTransactions = new HashMap<Thread, MulgaraTransaction>();
    this.sessionXAMap = new Assoc1toNMap<DatabaseSession, MulgaraTransaction>();

    this.transactionManager = transactionManagerFactory.newTransactionManager();
  }

  public MulgaraTransaction getTransaction(final DatabaseSession session, boolean write)
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (manager.isHoldingWriteLock(session)) {
        return writeTransaction;
      }

      try {
        MulgaraInternalTransaction transaction;
        if (write) {
          runWithoutMutex(new TransactionOperation() {
            public void execute() throws MulgaraTransactionException {
              manager.obtainWriteLock(session);
            }
          });
          try {
            assert writeTransaction == null;
            writeTransaction = transaction = 
                new MulgaraInternalTransaction(this, session.newOperationContext(true));
          } catch (Throwable th) {
            manager.releaseWriteLock(session);
            throw new MulgaraTransactionException("Error creating write transaction", th);
          }
        } else {
          transaction = new MulgaraInternalTransaction(this, session.newOperationContext(false));
        }

        sessionXAMap.put(session, transaction);

        return transaction;
      } catch (MulgaraTransactionException em) {
        throw em;
      } catch (Exception e) {
        throw new MulgaraTransactionException("Error creating transaction", e);
      }
    } finally {
      releaseMutex();
    }
  }

  public Set<MulgaraTransaction> getTransactionsForSession(DatabaseSession session) {
    acquireMutex();
    try {
      Set <MulgaraTransaction> xas = sessionXAMap.getN(session);
      return xas == null ? Collections.<MulgaraTransaction>emptySet() : xas;
    } finally {
      releaseMutex();
    }
  }

  public MulgaraTransaction newMulgaraTransaction(DatabaseOperationContext context)
      throws MulgaraTransactionException {
    return new MulgaraInternalTransaction(this, context);
  }


  public void commit(DatabaseSession session) throws MulgaraTransactionException {
    acquireMutex();
    try {
      manager.reserveWriteLock(session);
      try {
        if (failedSessions.contains(session)) {
          throw new MulgaraTransactionException("Attempting to commit failed exception");
        } else if (!manager.isHoldingWriteLock(session)) {
          throw new MulgaraTransactionException(
              "Attempting to commit while not the current writing transaction");
        }

        setAutoCommit(session, true);
        setAutoCommit(session, false);
      } finally {
        manager.releaseReserve(session);
      }
    } finally {
      releaseMutex();
    }
  }


  /**
   * This is an explicit, user-specified rollback.
   * 
   * This needs to be distinguished from an implicit rollback triggered by failure.
   */
  public void rollback(DatabaseSession session) throws MulgaraTransactionException {
    acquireMutex();
    try {
      manager.reserveWriteLock(session);
      try {
        if (manager.isHoldingWriteLock(session)) {
          try {
            writeTransaction.execute(new TransactionOperation() {
                public void execute() throws MulgaraTransactionException {
                  writeTransaction.heuristicRollback("Explicit Rollback");
                }
            });
            // FIXME: Should be checking status here, not writelock.
            if (manager.isHoldingWriteLock(session)) {
              // transaction referenced by something - need to explicitly end it.
              writeTransaction.abortTransaction("Rollback failed",
                  new MulgaraTransactionException("Rollback failed to terminate write transaction"));
            }
          } finally {
            failedSessions.add(session);
            setAutoCommit(session, false);
          }
        } else if (failedSessions.contains(session)) {
          failedSessions.remove(session);
          setAutoCommit(session, false);
        } else {
          throw new MulgaraTransactionException(
              "Attempt to rollback while not in the current writing transaction");
        }
      } finally {
        manager.releaseReserve(session);
      }
    } finally {
      releaseMutex();
    }
  }

  public void setAutoCommit(DatabaseSession session, boolean autoCommit)
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (manager.isHoldingWriteLock(session) && failedSessions.contains(session)) {
        writeTransaction.abortTransaction("Session failed and still holding writeLock",
            new MulgaraTransactionException("Failed Session in setAutoCommit"));
      }

      if (manager.isHoldingWriteLock(session) || failedSessions.contains(session)) {
        if (autoCommit) {
          // AutoCommit off -> on === branch on current state of transaction.
          if (manager.isHoldingWriteLock(session)) {
            // Within active transaction - commit and finalise.
            try {
              runWithoutMutex(new TransactionOperation() {
                public void execute() throws MulgaraTransactionException {
                  writeTransaction.execute(new TransactionOperation() {
                    public void execute() throws MulgaraTransactionException {
                      writeTransaction.dereference();
                      ((MulgaraInternalTransaction)writeTransaction).commitTransaction();
                    }
                  });
                }
              });
            } finally {
              // This should have been cleaned up by the commit above, but if it
              // hasn't then if we don't release here we could deadlock the
              // transaction manager
              if (manager.isHoldingWriteLock(session)) {
                manager.releaseWriteLock(session);
              }
              this.autoCommit = true;
            }
          } else if (failedSessions.contains(session)) {
            // Within failed transaction - cleanup.
            failedSessions.remove(session);
          }
        } else {
          if (!manager.isHoldingWriteLock(session)) {
            if (failedSessions.contains(session)) {
              failedSessions.remove(session);
              setAutoCommit(session, false);
            } else {
              throw new IllegalStateException("Can't reach here");
            }
          } else {
            // AutoCommit off -> off === no-op. Log info.
            if (logger.isInfoEnabled()) {
              logger.info("Attempt to set autocommit false twice\n" + new StackTrace());
            }
          }
        }
      } else {
        if (autoCommit) {
          // AutoCommit on -> on === no-op.  Log info.
          logger.info("Attempting to set autocommit true without setting it false");
        } else {
          // AutoCommit on -> off == Start new transaction.
          getTransaction(session, true); // Set's writeTransaction.
          writeTransaction.reference();
          this.autoCommit = false;
        }
      }
    } finally {
      releaseMutex();
    }
  }

  //
  // Transaction livecycle callbacks.
  //

  public Transaction transactionStart(MulgaraTransaction transaction) throws MulgaraTransactionException {
    acquireMutex();
    try {
      try {
        logger.info("Beginning Transaction");
        if (activeTransactions.get(Thread.currentThread()) != null) {
          throw new MulgaraTransactionException(
              "Attempt to start transaction in thread with exiting active transaction.");
        } else if (activeTransactions.containsValue(transaction)) {
          throw new MulgaraTransactionException("Attempt to start transaction twice");
        }

        transactionManager.begin();
        Transaction jtaTrans = transactionManager.getTransaction();

        activeTransactions.put(Thread.currentThread(), transaction);

        return jtaTrans;
      } catch (Exception e) {
        throw new MulgaraTransactionException("Transaction Begin Failed", e);
      }
    } finally {
      releaseMutex();
    }
  }

  public void transactionResumed(MulgaraTransaction transaction, Transaction jtaXA) 
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (activeTransactions.get(Thread.currentThread()) != null) {
        throw new MulgaraTransactionException(
            "Attempt to resume transaction in already activated thread");
      } else if (activeTransactions.containsValue(transaction)) {
        throw new MulgaraTransactionException("Attempt to resume active transaction");
      }
      
      try {
        transactionManager.resume(jtaXA);
        activeTransactions.put(Thread.currentThread(), transaction);
      } catch (Exception e) {
        throw new MulgaraTransactionException("Resume Failed", e);
      }
    } finally {
      releaseMutex();
    }
  }

  public Transaction transactionSuspended(MulgaraTransaction transaction)
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      try {
        if (transaction != activeTransactions.get(Thread.currentThread())) {
          throw new MulgaraTransactionException(
              "Attempt to suspend transaction from outside thread");
        }

        if (autoCommit && transaction == writeTransaction) {
          logger.error("Attempt to suspend write transaction without setting AutoCommit Off");
          throw new MulgaraTransactionException(
              "Attempt to suspend write transaction without setting AutoCommit Off");
        }

        Transaction xa = transactionManager.suspend();
        activeTransactions.remove(Thread.currentThread());

        return xa;
      } catch (Throwable th) {
        logger.error("Attempt to suspend failed", th);
        try {
          transactionManager.setRollbackOnly();
        } catch (Throwable t) {
          logger.error("Attempt to setRollbackOnly() failed", t);
        }
        throw new MulgaraTransactionException("Suspend failed", th);
      }
    } finally {
      releaseMutex();
    }
  }

  public void transactionComplete(MulgaraTransaction transaction) throws MulgaraTransactionException {
    acquireMutex();
    try {
      logger.debug("Transaction Complete");
      if (transaction == writeTransaction) {
        DatabaseSession session = sessionXAMap.get1(transaction);
        if (session == null) {
          throw new MulgaraTransactionException("No associated session found for write transaction");
        }
        if (manager.isHoldingWriteLock(session)) {
          manager.releaseWriteLock(session);
          writeTransaction = null;
        }
      }

      sessionXAMap.removeN(transaction);
      activeTransactions.remove(Thread.currentThread());
    } finally {
      releaseMutex();
    }
  }

  public void transactionAborted(MulgaraTransaction transaction) {
    acquireMutex();
    try {
      try {
        // Make sure this cleans up the transaction metadata - this transaction is DEAD!
        if (transaction == writeTransaction) {
          failedSessions.add(sessionXAMap.get1(transaction));
        }
        transactionComplete(transaction);
      } catch (Throwable th) {
        // FIXME: This should probably abort the entire server after logging the error!
        logger.error("Error managing transaction abort", th);
      }
    } finally {
      releaseMutex();
    }
  }

  public void setTransactionTimeout(int transactionTimeout) {
    try {
      transactionManager.setTransactionTimeout(transactionTimeout);
    } catch (SystemException es) {
      logger.warn("Unable to set transaction timeout: " + transactionTimeout, es);
    }
  }

  void abortWriteTransaction() throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (writeTransaction != null) {
        writeTransaction.abortTransaction(new MulgaraTransactionException("Explicit abort requested by write-lock manager"));
        writeTransaction = null;
      }
    } finally {
      releaseMutex();
    }
  }
}
