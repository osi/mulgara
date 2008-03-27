/*
 * Copyright 2008 The Topaz Foundation 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributions:
 */

package org.mulgara.resolver.spi;

// Java 2 standard packages
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

// Third party packages
import org.apache.log4j.Logger;

/**
 * A skeleton XAResource implementation. This handles the basic
 * resource-manager and transaction management and ensures correct {@link
 * #isSameRM} implementation. Subclasses must implement the actual
 * functionality in the {@link #doStart}, {@link #doPrepare}, {@link
 * #doCommit}, and {@link #doRollback} methods.
 *
 * @created 2008-02-16
 * @author Ronald Tschalär
 * @licence Apache License v2.0
 */
public abstract class AbstractXAResource<R extends AbstractXAResource.RMInfo<T>,T extends AbstractXAResource.TxInfo>
    extends DummyXAResource {
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(AbstractXAResource.class.getName());

  protected static final Map<ResolverFactory,RMInfo<? extends TxInfo>> resourceManagers =
    new WeakHashMap<ResolverFactory,RMInfo<? extends TxInfo>>();

  protected final R resourceManager;
  protected final T tmpTxInfo;


  //
  // Constructor
  //

  /**
   * Construct an XAResource.
   *
   * @param transactionTimeout  transaction timeout period, in seconds
   * @param resolverFactory     the resolver-factory we belong to
   * @param txInfo              the initial transaction-info
   */
  public AbstractXAResource(int transactionTimeout,
                            ResolverFactory resolverFactory,
                            T txInfo) {
    super(transactionTimeout);

    synchronized (resourceManagers) {
      @SuppressWarnings("unchecked")
      R rmgr = (R) resourceManagers.get(resolverFactory);
      if (rmgr == null)
        resourceManagers.put(resolverFactory, rmgr = newResourceManager());
      this.resourceManager = rmgr;
    }

    this.tmpTxInfo = txInfo;
  }

  /**
   * Create a new resource-manager instance - invoked only from the
   * constructor and only when no resource-manager instance exists for the
   * given resolver-factory.
   */
  protected abstract R newResourceManager();

  //
  // Methods implementing XAResource
  //

  public void start(Xid xid, int flags) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Start xid=" + System.identityHashCode(xid) + " flags=" + formatFlags(flags));
      logger.debug("xid.format=" + xid.getFormatId() + " xid.gblTxId=" + Arrays.toString(xid.getGlobalTransactionId()) + " xid.brnchQual=" + Arrays.toString(xid.getBranchQualifier()));
    }

    T tx = resourceManager.transactions.get(new XidWrapper(xid));
    boolean isNew = false;

    switch (flags) {
      case XAResource.TMRESUME:
        if (tx == null) {
          logger.error("Attempting to resume unknown transaction.");
          throw new XAException(XAException.XAER_NOTA);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Resuming transaction on xid=" + System.identityHashCode(xid));
        }
        break;

      case XAResource.TMNOFLAGS:
        if (tx != null) {
          logger.warn("Received plain start for existing tx");
          logger.warn("xid.format=" + xid.getFormatId() + " xid.gblTxId=" + Arrays.toString(xid.getGlobalTransactionId()) + " xid.brnchQual=" + Arrays.toString(xid.getBranchQualifier()));
          throw new XAException(XAException.XAER_DUPID);
        }
        // fallthrough

      case XAResource.TMJOIN:
        if (tx == null) {
          resourceManager.transactions.put(new XidWrapper(xid), tx = tmpTxInfo);
          tx.xid = xid;
          isNew = true;
        }
        break;

      default:
        // XXX: is this correct? Or should we actually roll back here?
        if (tx != null)
          tx.rollback = true;
        logger.error("Unrecognised flags in start: xid=" + System.identityHashCode(xid) + " flags=" + formatFlags(flags));
        throw new XAException(XAException.XAER_INVAL);
    }

    try {
      doStart(tx, flags, isNew);
    } catch (Throwable t) {
      logger.warn("Failed to do start", t);
      resourceManager.transactions.remove(new XidWrapper(xid));
      throw new XAException(XAException.XAER_RMFAIL);
    }
  }

  public void end(Xid xid, int flags) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("End xid=" + System.identityHashCode(xid) + " flags=" + formatFlags(flags));
    }

    T tx = resourceManager.transactions.get(new XidWrapper(xid));
    if (tx == null) {
      logger.error("Attempting to end unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }

    try {
      doEnd(tx, flags);
    } catch (Throwable t) {
      logger.warn("Failed to do end", t);
      resourceManager.transactions.remove(new XidWrapper(xid));
      throw new XAException(XAException.XAER_RMFAIL);
    }
  }

  public int prepare(Xid xid) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Prepare xid=" + System.identityHashCode(xid));
    }

    T tx = resourceManager.transactions.get(new XidWrapper(xid));
    if (tx == null) {
      logger.error("Attempting to prepare unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }

    if (tx.rollback) {
      logger.info("Attempting to prepare in failed transaction");
      rollback(xid);
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    try {
      int sts = doPrepare(tx);
      if (sts == XA_RDONLY)
        resourceManager.transactions.remove(new XidWrapper(xid));
      return sts;
    } catch (Throwable t) {
      logger.warn("Attempt to prepare failed", t);
      rollback(xid);
      throw new XAException(XAException.XA_RBROLLBACK);
    }
  }

  public void commit(Xid xid, boolean onePhase) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Commit xid=" + System.identityHashCode(xid) + " onePhase=" + onePhase);
    }

    T tx = resourceManager.transactions.get(new XidWrapper(xid));
    if (tx == null) {
      logger.error("Attempting to commit unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }
    if (tx.rollback) {
      logger.error("Attempting to commit in failed transaction");
      rollback(xid);
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    try {
      if (onePhase) {
        int sts = doPrepare(tx);
        if (sts == XA_RDONLY) {
          resourceManager.transactions.remove(new XidWrapper(xid));
          return;
        }
      }
    } catch (Throwable th) {
      logger.error("Attempt to prepare in onePhaseCommit failed.", th);
      rollback(xid);
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    boolean clean = true;
    try {
      doCommit(tx);
    } catch (XAException xae) {
      if (isHeuristic(xae)) {
        clean = false;
      }
      throw xae;
    } catch (Throwable th) {
      // This is a serious problem since the database is now in an
      // inconsistent state.
      // Make sure the exception is logged.
      logger.fatal("Failed to commit resource in transaction " + xid, th);
      throw new XAException(XAException.XAER_RMERR);
    } finally {
      if (clean) {
        resourceManager.transactions.remove(new XidWrapper(xid));
      }
    }
  }

  public void rollback(Xid xid) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Rollback xid=" + System.identityHashCode(xid));
    }

    T tx = resourceManager.transactions.get(new XidWrapper(xid));
    if (tx == null) {
      logger.error("Attempting to rollback unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }

    boolean clean = true;
    try {
      doRollback(tx);
    } catch (XAException xae) {
      if (isHeuristic(xae)) {
        clean = false;
      }
      throw xae;
    } catch (Throwable th) {
      // This is a serious problem since the database is now in an
      // inconsistent state.
      // Make sure the exception is logged.
      logger.fatal("Failed to rollback resource in transaction " + xid, th);
      throw new XAException(XAException.XAER_RMERR);
    } finally {
      if (clean) {
        resourceManager.transactions.remove(new XidWrapper(xid));
      }
    }
  }

  public void forget(Xid xid) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Forget xid=" + System.identityHashCode(xid));
    }

    T tx = resourceManager.transactions.get(new XidWrapper(xid));
    if (tx == null) {
      logger.error("Attempting to forget unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }

    boolean clean = true;
    try {
      doForget(tx);
    } catch (XAException xae) {
      if (xae.errorCode == XAException.XAER_RMERR) {
        clean = false;
      }
      throw xae;
    } catch (Throwable th) {
      logger.error("Failed to forget transaction " + xid, th);
      clean = false;
      throw new XAException(XAException.XAER_RMERR);
    } finally {
      if (clean) {
        resourceManager.transactions.remove(new XidWrapper(xid));
      }
    }
  }

  public Xid[] recover(int flag) throws XAException {
    if (logger.isDebugEnabled()) {
      logger.debug("Recover flag=" + formatFlags(flag));
    }

    throw new XAException(XAException.XAER_RMERR);
  }

  public boolean isSameRM(XAResource xaResource) throws XAException {
    boolean same = (xaResource instanceof AbstractXAResource) &&
      ((AbstractXAResource)xaResource).resourceManager == resourceManager;

    if (logger.isDebugEnabled()) {
      logger.debug("Is same resource manager? " + same + " :: " + xaResource + " on " + this);
    }

    return same;
  }

  /** 
   * Invoked on start with valid flags and tx state.
   * 
   * @param tx     the transaction being started; always non-null
   * @param flags  one of TMNOFLAGS, TMRESUME, or TMJOIN
   * @param isNew  true if <var>tx</var> was created as part of this start()
   * @throws Exception 
   */
  protected abstract void doStart(T tx, int flags, boolean isNew) throws Exception;

  /** 
   * Invoked on end().
   * 
   * @param tx     the transaction being ended; always non-null
   * @param flags  one of TMSUCCESS, TMFAIL, or TMSUSPEND
   * @throws Exception 
   */
  protected abstract void doEnd(T tx, int flags) throws Exception;

  /** 
   * Invoked on prepare() or commit(onePhase=true).
   * 
   * @param tx  the transaction being prepared; always non-null
   * @return XA_OK or XA_RDONLY
   * @throws Exception 
   */
  protected abstract int doPrepare(T tx) throws Exception;

  /** 
   * Invoked on commit().
   * 
   * @param tx  the transaction being committed; always non-null
   * @throws Exception 
   */
  protected abstract void doCommit(T tx) throws Exception;

  /** 
   * Invoked on (explicit or implicit) rollback().
   * 
   * @param tx  the transaction being rolled back; always non-null
   * @throws Exception 
   */
  protected abstract void doRollback(T tx) throws Exception;

  /** 
   * Invoked on forget().
   * 
   * @param tx  the transaction to forget; always non-null
   * @throws Exception 
   */
  protected abstract void doForget(T tx) throws Exception;

  private static boolean isHeuristic(XAException xae) {
    return (xae.errorCode == XAException.XA_HEURHAZ || xae.errorCode == XAException.XA_HEURCOM ||
            xae.errorCode == XAException.XA_HEURRB  || xae.errorCode == XAException.XA_HEURMIX);
  }


  /** The resource-manager info */
  public static class RMInfo<T extends TxInfo> {
    /** the list of active transactions */
    public final Map<XidWrapper,T> transactions =
      Collections.synchronizedMap(new HashMap<XidWrapper,T>());
  }

  /** The info pertaining to a single transaction */
  public static class TxInfo {
    /** the underlying Xid of this transaction; not valid till the first start() */
    public Xid xid;
    /** true if this transaction has been marked for rollback */
    public boolean rollback;
  }

  /**
   * Xid-wrapper that implements hashCode() and equals(). JTA does not require
   * Xid's to implement hashCode() and equals(), so in order to be able to use
   * them as keys in a map we need to wrap them with something that implements
   * them based on the individual fields of the Xid.
   */
  public static class XidWrapper {
    private final Xid xid;
    private final int hash;

    public XidWrapper(Xid xid) {
      this.xid = xid;
      this.hash = Arrays.hashCode(xid.getBranchQualifier());
    }

    public int hashCode() {
      return hash;
    }

    public boolean equals(Object other) {
      Xid o;

      if (other instanceof XidWrapper) {
        o = ((XidWrapper)other).xid;
      } else if (other instanceof Xid) {
        o = (Xid)other;
      } else {
        return false;
      }

      if (o == xid)
        return true;
      return o.getFormatId() == xid.getFormatId() &&
             Arrays.equals(o.getGlobalTransactionId(), xid.getGlobalTransactionId()) &&
             Arrays.equals(o. getBranchQualifier(), xid. getBranchQualifier());
    }
  }
}