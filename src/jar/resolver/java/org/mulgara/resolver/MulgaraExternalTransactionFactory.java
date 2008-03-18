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
 * (http://www.netymon.com, mailto:mail@netymon.com) under contract to 
 * Topaz Foundation. Portions created under this contract are
 * Copyright (c) 2007 Topaz Foundation
 * All Rights Reserved.
 */

package org.mulgara.resolver;

// Java2 packages
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.query.MulgaraTransactionException;
import org.mulgara.query.QueryException;
import org.mulgara.transaction.TransactionManagerFactory;
import org.mulgara.util.Assoc1toNMap;

/**
 * Manages external transactions.
 *
 * @created 2007-11-06
 *
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 *
 * @company <A href="mailto:mail@netymon.com">Netymon Pty Ltd</A>
 *
 * @copyright &copy;2007 <a href="http://www.topazproject.org/">Topaz Foundation</a>
 *
 * @licence Open Software License v3.0</a>
 */

public class MulgaraExternalTransactionFactory extends MulgaraTransactionFactory {
  private Map<DatabaseSession, MulgaraExternalTransaction> associatedTransaction;
  private Assoc1toNMap<DatabaseSession, MulgaraExternalTransaction> sessionXAMap;

  private Map<DatabaseSession, MulgaraXAResourceContext> xaResources;

  public MulgaraExternalTransactionFactory(MulgaraTransactionManager manager) {
    super(manager);

    this.associatedTransaction = new HashMap<DatabaseSession, MulgaraExternalTransaction>();
    this.sessionXAMap = new Assoc1toNMap<DatabaseSession, MulgaraExternalTransaction>();
    this.xaResources = new HashMap<DatabaseSession, MulgaraXAResourceContext>();
  }

  public MulgaraTransaction getTransaction(final DatabaseSession session, boolean write)
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      MulgaraExternalTransaction xa = associatedTransaction.get(session);
      if (xa == null) {
        throw new MulgaraTransactionException("No externally mediated transaction associated with session");
      } else if (write && xa != writeTransaction) {
        throw new MulgaraTransactionException("RO-transaction associated with session when requesting write operation");
      }

      return xa;
    } finally {
      releaseMutex();
    }
  }

  protected MulgaraExternalTransaction createTransaction(final DatabaseSession session, Xid xid, boolean write)
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (associatedTransaction.get(session) != null) {
        throw new MulgaraTransactionException(
            "Attempt to initiate transaction with existing transaction active with session");
      }
      if (write && manager.isHoldingWriteLock(session)) {
        throw new MulgaraTransactionException("Attempt to initiate two write transactions from the same session");
      }

      if (write) {
          runWithoutMutex(new TransactionOperation() {
            public void execute() throws MulgaraTransactionException {
              manager.obtainWriteLock(session);
            }
          });
        try {
          MulgaraExternalTransaction xa = new MulgaraExternalTransaction(this, xid, session.newOperationContext(true));
          writeTransaction = xa;
          associatedTransaction.put(session, xa);
          sessionXAMap.put(session, xa);

          return xa;
        } catch (Throwable th) {
          manager.releaseWriteLock(session);
          throw new MulgaraTransactionException("Error initiating write transaction", th);
        }
      } else {
        try {
          MulgaraExternalTransaction xa = new MulgaraExternalTransaction(this, xid, session.newOperationContext(false));
          associatedTransaction.put(session, xa);
          sessionXAMap.put(session, xa);

          return xa;
        } catch (QueryException eq) {
          throw new MulgaraTransactionException("Error obtaining new read-only operation-context", eq);
        }
      }
    } finally {
      releaseMutex();
    }
  }

  public Set<MulgaraExternalTransaction> getTransactionsForSession(DatabaseSession session) {
    acquireMutex();
    try {
      Set<MulgaraExternalTransaction> xas = sessionXAMap.getN(session);
      return xas != null ? xas : Collections.<MulgaraExternalTransaction>emptySet();
    } finally {
      releaseMutex();
    }
  }

  public XAResource getXAResource(DatabaseSession session, boolean writing) {
    acquireMutex();
    try {
      MulgaraXAResourceContext xarc = xaResources.get(session);
      if (xarc == null) {
        xarc = new MulgaraXAResourceContext(this, session);
        xaResources.put(session, xarc);
      }

      return xarc.getResource(writing);
    } finally {
      releaseMutex();
    }
  }

  public void closingSession(DatabaseSession session) throws MulgaraTransactionException {
    acquireMutex();
    try {
      try {
        super.closingSession(session);
      } finally {
        xaResources.remove(session);
      }
    } finally {
      releaseMutex();
    }
  }

  public void transactionComplete(MulgaraExternalTransaction xa)
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (xa == null) {
        throw new IllegalArgumentException("Null transaction indicated completion");
      }
      DatabaseSession session = sessionXAMap.get1(xa);
      if (xa == writeTransaction) {
        manager.releaseWriteLock(session);
        writeTransaction = null;
      }
      sessionXAMap.removeN(xa);
      if (associatedTransaction.get(session) == xa) {
        associatedTransaction.remove(session);
      }
    } finally {
      releaseMutex();
    }
  }

  public boolean hasAssociatedTransaction(DatabaseSession session) {
    acquireMutex();
    try {
      return associatedTransaction.get(session) != null;
    } finally {
      releaseMutex();
    }
  }

  public boolean associateTransaction(DatabaseSession session, MulgaraExternalTransaction xa) {
    acquireMutex();
    try {
      if (associatedTransaction.get(session) != null) {
        return false;
      } else {
        associatedTransaction.put(session, xa);
        return true;
      }
    } finally {
      releaseMutex();
    }
  }

  public MulgaraExternalTransaction getAssociatedTransaction(DatabaseSession session) {
    acquireMutex();
    try {
      return associatedTransaction.get(session);
    } finally {
      releaseMutex();
    }
  }

  public void disassociateTransaction(DatabaseSession session, MulgaraExternalTransaction xa) 
      throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (associatedTransaction.get(session) == xa) {
        associatedTransaction.remove(session);
      }
    } finally {
      releaseMutex();
    }
  }

  void abortWriteTransaction() throws MulgaraTransactionException {
    acquireMutex();
    try {
      if (writeTransaction != null) {
        writeTransaction.abortTransaction("Explicit abort requested by write-lock manager", new Throwable());
        writeTransaction = null;
      }
    } finally {
      releaseMutex();
    }
  }
}
