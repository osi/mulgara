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

// Java 2 enterprise packages
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;

// Third party packages
import org.apache.log4j.Logger;

// Local packages
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.EnlistableResource;
import org.mulgara.resolver.spi.ResolverSessionFactory;

import org.mulgara.query.MulgaraTransactionException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.QueryException;

/**
 * @created 2007-11-06
 *
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 *
 * @company <a href="mailto:mail@netymon.com">Netymon Pty Ltd</a>
 *
 * @copyright &copy;2007 <a href="http://www.topazproject.org/">Topaz Foundation</a>
 *
 * @licence Open Software License v3.0
 */
public class MulgaraExternalTransaction implements MulgaraTransaction {
  private static final Logger logger =
    Logger.getLogger(MulgaraExternalTransaction.class.getName());

  private Xid xid;

  private Set<EnlistableResource> enlisted;
  private Set<EnlistableResource> prepared;
  private Set<EnlistableResource> committed;
  private Set<EnlistableResource> rollbacked;

  private Map<EnlistableResource, XAResource> xaResources;

  private MulgaraExternalTransactionFactory factory;
  private DatabaseOperationContext context;

  private boolean hRollback;
  private int heurCode;
  private boolean rollback;

  MulgaraExternalTransaction(MulgaraExternalTransactionFactory factory, Xid xid, DatabaseOperationContext context)
      throws QueryException {
    this.factory = factory;
    this.context = context;
    this.xid = xid;

    this.enlisted = new HashSet<EnlistableResource>();
    this.prepared = new HashSet<EnlistableResource>();
    this.committed = new HashSet<EnlistableResource>();
    this.rollbacked = new HashSet<EnlistableResource>();

    this.xaResources = new HashMap<EnlistableResource, XAResource>();

    this.hRollback = false;
    this.heurCode = 0;
    this.rollback = false;

    this.context.initiate(this);
  }

  // We ignore reference counting in external transactions
  public void reference() throws MulgaraTransactionException {}  
  public void dereference() throws MulgaraTransactionException {}

  public MulgaraTransactionException abortTransaction(String errorMessage, Throwable cause)
      throws MulgaraTransactionException {
    report("abortTransaction");
    try {
      for (EnlistableResource resource : enlisted) {
        try {
          resource.abort();
        } catch (Throwable throw_away) {}
      }
      for (EnlistableResource resource : prepared) {
        try {
          resource.abort();
        } catch (Throwable throw_away) {}
      }

      return new MulgaraTransactionException(errorMessage, cause);
    } finally {
      factory.transactionComplete(this);
    }
  }

  public void heuristicRollback(String cause) throws MulgaraTransactionException {
    report("heuristicRollback");
    hRollback = true;
    try {
      rollback(xid);
    } catch (XAException xa) {
      throw new MulgaraTransactionException("Failed heuristic rollback", xa);
    } finally {
      heurCode = heurCode == 0 ? XAException.XA_HEURRB : heurCode;
    }
  }

  public void execute(Operation operation,
               ResolverSessionFactory resolverSessionFactory,
               DatabaseMetadata metadata) throws MulgaraTransactionException {
    // FIXME: Do I need to check that this transaction is 'active' ?
    try {
      operation.execute(context,
                        context.getSystemResolver(),
                        resolverSessionFactory,
                        metadata);
    } catch (Throwable th) {
      try {
        rollback(xid);
      } catch (XAException ex) {
        logger.error("Error in rollback after operation failure", ex);
      }
      throw new MulgaraTransactionException("Operation failed", th);
    }
  }

  public AnswerOperationResult execute(AnswerOperation ao) throws TuplesException {
    try {
      ao.execute();
      return ao.getResult();
    } catch (Throwable th) {
      try {
        logger.warn("Error in answer operation triggered rollback", th);
        rollback(xid);
      } catch (XAException ex) {
        logger.error("Error in rollback after answer-operation failure", ex);
      }
      throw new TuplesException("Request failed", th);
    }
  }

  // FIXME: See if we can't rearrange things to allow this to be deleted.
  public void execute(TransactionOperation to) throws MulgaraTransactionException {
    to.execute();
  }

  public void enlist(EnlistableResource enlistable) throws MulgaraTransactionException {
    try {
      XAResource res = enlistable.getXAResource();
      for (EnlistableResource eres : enlisted) {
        if (res.isSameRM(xaResources.get(eres))) {
          return;
        }
      }
      enlisted.add(enlistable);
      xaResources.put(enlistable, res);
      // FIXME: We need to handle this uptodate operation properly - handle
      // suspension or mid-prepare/commit.
      // bringUptodate(res);
      res.start(xid, XAResource.TMNOFLAGS);
    } catch (XAException ex) {
      throw new MulgaraTransactionException("Failed to enlist resource", ex);
    }
  }

  //
  // Methods used to manage transaction from XAResource.
  //

  void commit(Xid xid) throws XAException {
    report("commit");
    // FIXME: Consider the possiblity prepare failed, or was incomplete.
    for (EnlistableResource er : prepared) {
      xaResources.get(er).commit(xid, false);
      committed.add(er);
    }
    cleanupTransaction();
  }

  boolean isHeuristicallyRollbacked() {
    return hRollback;
  }

  boolean isHeuristicallyCommitted() {
    return false;
  }

  int getHeuristicCode() {
    return heurCode;
  }

  boolean isRollbacked() {
    return rollback;
  }

  void prepare(Xid xid) throws XAException {
    report("prepare");
    for (EnlistableResource er : enlisted) {
      xaResources.get(er).prepare(xid);
      prepared.add(er);
    }
    // status = PREPARED; ?
  }

  /**
   * Perform rollback.  Only throws exception if transaction is subject to
   * Heuristic Completion.
   */
  void rollback(Xid xid) throws XAException {
    report("rollback");
    try {
      rollback = true;
      Map<EnlistableResource, XAException> rollbackFailed = new HashMap<EnlistableResource, XAException>();

      for (EnlistableResource er : enlisted) {
        try {
          if (!committed.contains(er)) {
            xaResources.get(er).rollback(xid);
            rollbacked.add(er);
          }
        } catch (XAException ex) {
          logger.error("Attempt to rollback resource failed", ex);
          rollbackFailed.put(er, ex);
        }
      }

      if (rollbackFailed.isEmpty()) {
        if (committed.isEmpty()) {        // Clean failure and rollback
          return; // SUCCESSFUL ROLLBACK - RETURN
        } else {                          // No rollback-failure, but partial commit
          heurCode = XAException.XA_HEURMIX;
          throw new XAException(heurCode);
        }
      } else {
        // Something went wrong - start by assuming if one committed all committed
        heurCode = (committed.isEmpty()) ? 0 : XAException.XA_HEURCOM;
        // Then check every rollback failure code for a contradiction to all committed.
        for (XAException xaex : rollbackFailed.values()) {
          switch (xaex.errorCode) {
            case XAException.XA_HEURHAZ:  
            case XAException.XAER_NOTA:
            case XAException.XAER_RMERR:
            case XAException.XAER_RMFAIL:
            case XAException.XAER_INVAL:
            case XAException.XAER_PROTO:
              // All these amount to not knowing the result - so we have a hazard
              // unless we already know we have a mixed result.
              if (heurCode != XAException.XA_HEURMIX) {
                heurCode = XAException.XA_HEURHAZ;
              }
              break;
            case XAException.XA_HEURCOM:
              if (!rollbacked.isEmpty() || heurCode == XAException.XA_HEURRB) {
                // We know something else was rollbacked, so we know we have a mixed result.
                heurCode = XAException.XA_HEURMIX;
              } else if (heurCode == 0) {
                heurCode = XAException.XA_HEURCOM;
              } // else it's a HEURHAZ or a HEURCOM and stays that way.
              break;
            case XAException.XA_HEURRB:
              if (!committed.isEmpty() || heurCode == XAException.XA_HEURCOM) {
                heurCode = XAException.XA_HEURMIX;
              } else if (heurCode == 0) {
                heurCode = XAException.XA_HEURRB;
              } // else it's a HEURHAZ or a HEURRB and stays that way.
              break;
            case XAException.XA_HEURMIX:
              // It can't get worse than, we know we have a mixed result.
              heurCode = XAException.XA_HEURMIX;
              break;
            default:
              // The codes above are the only codes permitted from a rollback() so
              // anything else indicates a serious error in the resource-manager.
              throw new XAException(XAException.XAER_RMERR);
          }
        }

        throw new XAException(heurCode);
      }
    } finally {
      cleanupTransaction();
    }
  }


  Xid getXid() {
    return xid;
  }

  private void cleanupTransaction() throws XAException {
    report("cleanupTransaction");
    try {
      factory.transactionComplete(this);
    } catch (MulgaraTransactionException em) {
      try {
        logger.error("Failed to cleanup transaction", em);
        abortTransaction("Failure in cleanup", em);
        throw new XAException(XAException.XAER_RMERR);
      } catch (MulgaraTransactionException em2) {
        logger.error("Failed to abort transaction on cleanup failure", em2);
        throw new XAException(XAException.XAER_RMFAIL);
      }
    }
  }

  private void report(String desc) {
    if (logger.isInfoEnabled()) {
      logger.info(desc + ": " + System.identityHashCode(this));
    }
  }
}
