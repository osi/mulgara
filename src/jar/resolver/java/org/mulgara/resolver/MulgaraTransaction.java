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

// Java 2 enterprise packages
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverSessionFactory;

import org.mulgara.query.TuplesException;

/**
 * Responsible for the javax.transaction.Transaction object.
 * Responsibilities
 * Ensuring every begin or resume is followed by either a suspend or an end.
 * Ensuring every suspend or end is preceeded by either a begin or a resume.
 * In conjunction with TransactionalAnswer ensuring that
 * all calls to operations on SubqueryAnswer are preceeded by a successful resume.
 * all calls to operations on SubqueryAnswer conclude with a suspend as the last call prior to returning to the user.
 * Collaborates with DatabaseTransactionManager to determine when to end the transaction.
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
public class MulgaraTransaction {
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(MulgaraTransaction.class.getName());

  private MulgaraTransactionManager manager;
  private OperationContext context;

  private Transaction transaction;
  private Thread currentThread;

  private int inuse;
  private int using;

  private final int NO_ROLLBACK = 0;
  private final int IMPLICIT_ROLLBACK = 1;
  private final int EXPLICIT_ROLLBACK = 2;

  private int rollback;
  private Throwable rollbackCause;

  public MulgaraTransaction(MulgaraTransactionManager manager, OperationContext context) {
    report("Creating Transaction");
    this.manager = manager;
    this.context = context;

//    FIXME: MTMgr will be null until operational.
//    this.transaction = manager.transactionStart(this);
    inuse = 1; // Note: This implies implict activation as a part of construction.
    using = 0;

    rollback = NO_ROLLBACK;
    rollbackCause = null;

//    FIXME: need this added to context. Sets up and enlists the system-resolver.
//    context.initiate();

//    FIXME: need this added to context. Allows context to cleanup caches at end of transaction.
//    this.transaction.enlistResource(context.getXAResource());
    report("Created Transaction");
  }

  // FIXME: Not yet certain I have the error handling right here.
  // Need to clarify semantics and ensure the error conditions are 
  // properly handled.
  private synchronized void activate() throws MulgaraTransactionException {
    report("Activating Transaction");
    if (currentThread == null) {
      currentThread = Thread.currentThread();
    } else if (!currentThread.equals(Thread.currentThread())) {
      throw new MulgaraTransactionException("Concurrent access attempted to transaction: Transaction has NOT been rolledback.");
    }

    if (inuse == 0) {
      report("Resuming transaction");
//      try {
//        manager.transactionResumed(this);
//      } catch (Throwable th) {
//        logger.warn("Error resuming transaction: ", th);
//        failTransaction();
//        throw new MulgaraTransactionException("Error resuming transaction", th);
//      }
    }

    inuse++;

    report("Activated transaction");
  }


  public synchronized void tempDeactivate() throws MulgaraTransactionException {
    deactivate();
  }

  // FIXME: Not yet certain I have the error handling right here.
  // Need to clarify semantics and ensure the error conditions are 
  // properly handled.
  private synchronized void deactivate() throws MulgaraTransactionException {
    report("Deactivating transaction");

    inuse--;

    if (inuse < 0) {
        throw new MulgaraTransactionException("Mismatched activate/deactivate.  inuse < 0: " + inuse);
//      throw implicitRollback(
//          new MulgaraTransactionException("Mismatched activate/deactivate.  inuse < 0: " + inuse));
    } else if (using < 0) {
        throw new MulgaraTransactionException("Reference Failure.  using < 0: " + using);
    }

    if (inuse == 0) {
      if (using == 0) {
        report("Completing Transaction");
        // END TRANSACTION HERE.  But commit might fail.
//        manager.transactionComplete(this);
          manager = null;
          transaction = null;
      } else {
        report("Suspending Transaction");
        // What happens if suspend fails?
        // Rollback and terminate transaction.
        // JTA isn't entirely unambiguous as to the long-term stability of the original
        // transaction object - can suspend return a new object?
//        this.transaction = manager.transactionSuspended(this);
      }
      currentThread = null;
    }
    report("Deactivated Transaction");
  }

  // Do I want to check for currentThread here?  Do I want a seperate check() method to 
  // cover precondition checks against currentThread?
  void reference() throws MulgaraTransactionException {
    report("Referencing Transaction");
    if (inuse < 1) {
        throw new MulgaraTransactionException("Mismatched activate/deactivate.  inuse < 1: " + inuse);
    } else if (using < 0) {
        throw new MulgaraTransactionException("Reference Failure.  using < 0: " + using);
    }
    using++;
    report("Referenced Transaction");
  }

  void dereference() throws MulgaraTransactionException {
    report("Dereferencing Transaction");
    if (inuse < 1) {
        throw new MulgaraTransactionException("Mismatched activate/deactivate.  inuse < 1: " + inuse);
    } else if (using < 1) {
        throw new MulgaraTransactionException("Reference Failure.  using < 1: " + using);
    }
    using--;
    report("Dereferenced Transaction");
  }

  void execute(Operation operation,
               ResolverSessionFactory resolverSessionFactory, // FIXME: We shouldn't need this. - only used for backup and restore operations.
               DatabaseMetadata metadata) throws MulgaraTransactionException {
    activate();
    try {
//      FIXME: Need to migrate systemResolver to context for this to work.
//      operation.execute(context,
//                        context.getSystemResolver(),
//                        resolverSessionFactory,
//                        metadata);
    } catch (Throwable th) {
      throw implicitRollback(th);
    } finally {
      deactivate();
    }
  }

  /** Should rename this 'wrap' */
  AnswerOperationResult execute(AnswerOperation ao) throws TuplesException {
//    FIXME: activate/deactivate won't work until we have MTMgr operational.
    report("Executing Operation");
    try {
      activate();
      try {
        ao.execute();
        return ao.getResult();
      } catch (Throwable th) {
        throw new TuplesException("Error accessing Answer", th);
  //      throw implicitRollback(th);
      } finally {
        deactivate();
      }
    } catch (MulgaraTransactionException em) {
      throw new TuplesException("Transaction error", em);
    } finally {
      report("Executed Operation");
    }
  }


  private MulgaraTransactionException implicitRollback(Throwable cause) throws MulgaraTransactionException {
    rollback = IMPLICIT_ROLLBACK;
    rollbackCause = cause;
    failTransaction();
    return new MulgaraTransactionException("Transaction Rolledback", cause);
  }

  /**
   * Note: I think this is the only one that matters. 
   */
  protected void explicitRollback() throws MulgaraTransactionException {
    rollback = EXPLICIT_ROLLBACK;
    // We don't throw an exception here when transaction fails - this is expected,
    // after all we requested it.
  }

  private void terminateTransaction() throws MulgaraTransactionException {
  }

  private void failTransaction() throws MulgaraTransactionException {
    // We need to handle the whole fact this is an error, but the core operation is rollback.
    try {
      transaction.rollback();
    } catch (SystemException es) {
      throw new MulgaraTransactionException("Failed to Rollback", es);
    }
  }

  private void finalizeTransaction() throws MulgaraTransactionException {
    // We need a whole load of error handling here, but the core operation is commit.
    try {
      transaction.commit();
    } catch (Exception e) {
      throw new MulgaraTransactionException("Error while trying to commit", e);
    } finally {
//      manager.transactionComplete(this);
    }
  }

  //
  // Note that OperationContext needs to be decoupled from DatabaseSession, so that it is
  // recreated for each operation - it also needs to provide an XAResource of it's own for
  // enlisting in the transaction to clean-up caches and the like.
  //
  public void enlistResolver(Resolver resolver) throws MulgaraTransactionException {
    try {
      XAResource resource = resolver.getXAResource();
      transaction.enlistResource(resource);
    } catch (Exception e) {
      throw new MulgaraTransactionException("Error enlisting resolver", e);
    }
  }

  /**
   * Should only be visible to MulgaraTransactionManager.
   */
  protected Transaction getTransaction() {
    return transaction;
  }

  protected void finalize() {
    report("GC-finalize");
    if (inuse != 0 || using != 0) {
      logger.error("Referernce counting error in transaction, inuse=" + inuse + ", using=" + using);
    }
    if (manager != null || transaction != null) {
      logger.error("Transaction not terminated properly");
    }
  }

  private void report(String desc) {
    if (logger.isInfoEnabled()) {
      logger.info(desc + ": " + System.identityHashCode(this) +
          ", inuse=" + inuse + ", using=" + using);
    }
  }
}
