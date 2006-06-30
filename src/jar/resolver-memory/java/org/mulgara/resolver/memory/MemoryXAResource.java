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

package org.mulgara.resolver.memory;

// Java 2 standard packages
import java.util.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

// Third party packages
import org.apache.log4j.Logger;


import org.mulgara.store.xa.SimpleXAResource;
import org.mulgara.store.xa.SimpleXAResourceException;
import org.mulgara.store.xa.XAResolverSession;

/**
 * A dummy implementation of the {@link XAResource} interface which logs the
 * calls made to it, but otherwise ignores them.
 *
 * @created 2004-05-12
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:48 $
 * @maintenanceAuthor $Author: newmana $
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technoogies, Inc.</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */

public class MemoryXAResource implements XAResource
{
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(MemoryXAResource.class.getName());

  /**
   * Map from keyed from the {@link Integer} value of the various flags
   * defined in {@link XAResource} and mapping to the formatted name for that
   * flag.
   */
  private final static Map flagMap = new HashMap();

  static {
    flagMap.put(new Integer(XAResource.TMENDRSCAN),   "TMENDRSCAN");
    flagMap.put(new Integer(XAResource.TMFAIL),       "TMFAIL");
    flagMap.put(new Integer(XAResource.TMJOIN),       "TMJOIN");
    flagMap.put(new Integer(XAResource.TMONEPHASE),   "TMONEPHASE");
    flagMap.put(new Integer(XAResource.TMRESUME),     "TMRESUME");
    flagMap.put(new Integer(XAResource.TMSTARTRSCAN), "TMSTARTRSCAN");
    flagMap.put(new Integer(XAResource.TMSUCCESS),    "TMSUCCESS");
    flagMap.put(new Integer(XAResource.TMSUCCESS),    "TMSUSPEND");
  }

  /** The transaction timeout value in seconds.  */
  private int transactionTimeout = 0;

  private XAResolverSession session;
  private boolean rollback;
  private Xid xid;

  //
  // Constructor
  //

  /**
   * Construct a {@link MemoryXAResource} with a specified transaction timeout.
   *
   * @param transactionTimeout  transaction timeout period, in seconds
   */
  public MemoryXAResource(int transactionTimeout,
                          XAResolverSession session)
  {
    logger.debug("<init> Creating MemoryXAResource: " + this);
    this.transactionTimeout = transactionTimeout * 100;
    this.session = session;
    this.rollback = false;
  }

  //
  // Methods implementing XAResource
  //

  public void start(Xid xid, int flags) throws XAException
  {
    logger.debug("Start " + System.identityHashCode(xid) + " flags=" + formatFlags(flags));
    switch (flags) {
      case XAResource.TMRESUME:
        logger.debug("Resuming transaction on " + System.identityHashCode(xid));
        break;
      case XAResource.TMNOFLAGS:
        try {
          session.refresh(new SimpleXAResource[] {});
          this.xid = xid;
        } catch (SimpleXAResourceException es) {
          logger.warn("Failed to refresh phases", es);
          throw new XAException(XAException.XAER_RMFAIL);
        }
        break;
      default:
        rollback = true;
        logger.error("Unrecognised flags in start: " + System.identityHashCode(xid) + " flags=" + formatFlags(flags));
        throw new XAException(XAException.XAER_INVAL);
    }
  }

  public int prepare(Xid xid) throws XAException
  {
    logger.debug("Prepare " + System.identityHashCode(xid));
    logger.debug("Prepare always returns XA_OK, never XA_RDONLY");

    if (rollback) {
      logger.error("Attempting to prepare in failed transaction");
      throw new XAException(XAException.XA_RBROLLBACK);
    }
    if (!xid.equals(this.xid)) {
      logger.error("Attempting to prepare unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }

    try {
      session.prepare();
    } catch (SimpleXAResourceException es) {
      logger.warn("Attempt to prepare store failed", es);
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    return XA_OK;
  }

  public void commit(Xid xid, boolean onePhase) throws XAException
  {
    logger.debug("Commit xid=" + System.identityHashCode(xid) + " onePhase=" + onePhase);
    if (rollback) {
      logger.error("Attempting to commit in failed transaction");
      throw new XAException(XAException.XA_RBROLLBACK);
    }
    if (!xid.equals(this.xid)) {
      logger.error("Attempting to commit unknown transaction.");
      throw new XAException(XAException.XAER_NOTA);
    }

    try {
      if (onePhase) {
        // Check return value is XA_OK.
        prepare(xid);
      }
    } catch (Throwable th) {
      this.rollback = true;
      logger.error("Attempt to prepare in onePhaseCommit failed.", th);
      throw new XAException(XAException.XA_RBROLLBACK);
    }

    try {
      session.commit();
    } catch (Throwable th) {
      // This is a serious problem since the database is now in an
      // inconsistent state.
      // Make sure the exception is logged.
      logger.fatal("Failed to commit resource in transaction " + xid, th);
      throw new XAException(XAException.XAER_RMERR);
    }
  }

  public void end(Xid xid, int flags) throws XAException
  {
    logger.debug("End xid=" + System.identityHashCode(xid) + " flags=" + formatFlags(flags));
  }

  public void forget(Xid xid) throws XAException
  {
    logger.debug("Forget xid=" + System.identityHashCode(xid));
  }

  public int getTransactionTimeout() throws XAException
  {
    logger.debug("Get transaction timeout: " + transactionTimeout);
    return transactionTimeout;
  }

  public boolean isSameRM(XAResource xaResource) throws XAException
  {
    logger.debug("Is same resource manager? " + (xaResource == this) + " :: " + xaResource + " on " + this);
    return xaResource == this;
  }

  public Xid[] recover(int flag) throws XAException
  {
    logger.debug("Recover flag=" + formatFlags(flag));
    throw new XAException(XAException.XAER_RMERR);
  }

  public void rollback(Xid xid) throws XAException
  {
    logger.debug("Rollback " + System.identityHashCode(xid));

    boolean fatalError = false;

    if (!xid.equals(this.xid)) {
      logger.error("Attempting to rollback unknown transaction.");
      fatalError = true;
    }

    try {
      session.rollback();
    } catch (Throwable th) {
      // This is a serious problem since the database is now in an
      // inconsistent state.
      // Make sure the exception is logged.
      logger.fatal("Failed to rollback resource in transaction " + xid, th);
      fatalError = true;
    }

    if (fatalError) {
      logger.fatal("Fatal error occured while rolling back transaction " + xid + " in manager for " + this.xid);
      throw new XAException(XAException.XAER_RMERR);
    }
  }

  public boolean setTransactionTimeout(int transactionTimeout)
    throws XAException
  {
    logger.debug("Set transaction timeout: " + transactionTimeout);
    this.transactionTimeout = transactionTimeout;
    return true;
  }

  //
  // Internal methods
  //

  /**
   * Format bitmasks defined by {@link XAResource}.
   *
   * @param flags  a bitmask composed from the constants defined in
   *   {@link XAResource}
   * @return a formatted representation of the <var>flags</var>
   */
  private static String formatFlags(int flags)
  {
    // Short-circuit evaluation if we've been explicitly passed no flags
    if (flags == XAResource.TMNOFLAGS) {
      return "TMNOFLAGS";
    }

    StringBuffer buffer = new StringBuffer();

    // Add any flags that are present
    for (Iterator i = flagMap.entrySet().iterator(); i.hasNext(); ) {
      Map.Entry entry = (Map.Entry)i.next();
      int entryFlag = ((Integer)entry.getKey()).intValue();

      // If this flag is present, add it to the formatted output and remove
      // from the bitmask
      if ((entryFlag & flags) == entryFlag) {
        if (buffer.length() > 0) {
          buffer.append(",");
        }
        buffer.append(entry.getValue());
        flags &= ~entryFlag;
      }
    }

    // We would expect to have removed all flags by this point
    // If there's some unknown flag we've missed, format it as hexadecimal
    if (flags != 0) {
      if (buffer.length() > 0) {
        buffer.append(",");
      }
      buffer.append("0x").append(Integer.toHexString(flags));
    }

    return buffer.toString();
  }
}
