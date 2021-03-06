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
package org.mulgara.store.statement.xa;

import java.io.*;
import java.nio.*;

// Java 2 standard packages
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.store.nodepool.*;
import org.mulgara.store.statement.*;
import org.mulgara.store.tuples.StoreTuples;
import org.mulgara.store.tuples.TuplesOperations;
import org.mulgara.store.xa.AbstractBlockFile;
import org.mulgara.store.xa.Block;
import org.mulgara.store.xa.BlockFile;
import org.mulgara.store.xa.LockFile;
import org.mulgara.store.xa.PersistableMetaRoot;
import org.mulgara.store.xa.SimpleXAResourceException;
import org.mulgara.store.xa.XAStatementStore;
import org.mulgara.store.xa.XAUtils;
import org.mulgara.util.Constants;

/**
 * An implementation of {@link StatementStore}.
 *
 * @created 2001-10-12
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/02/22 08:16:34 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public final class XAStatementStoreImpl implements XAStatementStore {

  /**
   * Logger.
   */
  private final static Logger logger = Logger.getLogger(XAStatementStoreImpl.class);

  final static boolean RELEASE_NODE_LISTENERS_ENABLED = false;

  final static long NONE = NodePool.NONE;

  /**
   * Description of the Field
   */
  final static int TI_0123 = 0;

  /**
   * Description of the Field
   */
  final static int TI_1203 = 1;

  /**
   * Description of the Field
   */
  final static int TI_2013 = 2;

  /**
   * Description of the Field
   */
  final static int TI_3012 = 3;

  /**
   * Description of the Field
   */
  final static int TI_3120 = 4;

  /**
   * Description of the Field
   */
  final static int TI_3201 = 5;

  /**
   * Description of the Field
   */
  final static int NR_INDEXES = 6;

  /**
   * Description of the Field
   */
  private final static int[][] orders = {
      {0, 1, 2, 3},
      {1, 2, 0, 3},
      {2, 0, 1, 3},
      {3, 0, 1, 2},
      {3, 1, 2, 0},
      {3, 2, 0, 1}
  };

  private final static int[] selectIndex = {
    /* XXXX */ TI_0123,
    /* XXX0 */ TI_0123,
    /* XX1X */ TI_1203,
    /* XX10 */ TI_0123,
    /* X2XX */ TI_2013,
    /* X2X0 */ TI_2013,
    /* X21X */ TI_1203,
    /* X210 */ TI_0123,
    /* 3XXX */ TI_3012,
    /* 3XX0 */ TI_3012,
    /* 3X1X */ TI_3120,
    /* 3X10 */ TI_3012,
    /* 32XX */ TI_3201,
    /* 32X0 */ TI_3201,
    /* 321X */ TI_3120,
    /* 3210 */ TI_0123
  };

  /**
   * Description of the Field
   */
  private final static int FILE_MAGIC = 0xa5e7f2e1;

  /**
   * Description of the Field
   */
  private final static int FILE_VERSION = 8;

  /**
   * Index of the file magic number within each of the two on-disk metaroots.
   */
  private final static int IDX_MAGIC = 0;

  /**
   * Index of the file version number within each of the two on-disk metaroots.
   */
  private final static int IDX_VERSION = 1;

  /**
   * Index of the valid flag (in ints) within each of the two on-disk metaroots.
   */
  private final static int IDX_VALID = 2;

  /**
   * The index of the phase number in the on-disk phase.
   */
  private final static int IDX_PHASE_NUMBER = 3;

  /**
   * The size of the header of a metaroot in ints.
   */
  private final static int HEADER_SIZE_INTS = 4;

  /**
   * The size of the header of a metaroot in longs.
   */
  private final static int HEADER_SIZE_LONGS = (HEADER_SIZE_INTS + 1) / 2;

  /**
   * The size of a metaroot in longs.
   */
  private final static int METAROOT_SIZE = HEADER_SIZE_LONGS +
      Phase.RECORD_SIZE;

  /**
   * The number of metaroots in the metaroot file.
   */
  private final static int NR_METAROOTS = 2;

  /**
   * Description of the Field
   */
  private final static int MASK0 = 1;

  /**
   * Description of the Field
   */
  private final static int MASK1 = 2;

  /**
   * Description of the Field
   */
  private final static int MASK2 = 4;

  /**
   * Description of the Field
   */
  private final static int MASK3 = 8;

  /**
   * The name of the triple store which forms the base name for the graph files.
   */
  private String fileName;

  /**
   * The LockFile that protects the graph from being opened twice.
   */
  private LockFile lockFile;

  /**
   * The BlockFile for the node pool metaroot file.
   */
  private BlockFile metarootFile = null;

  /**
   * The metaroot blocks of the metaroot file.
   */
  private Block[] metarootBlocks = new Block[NR_METAROOTS];

  /**
   * Description of the Field
   */
  private boolean wrongFileVersion = false;

  /**
   * Description of the Field
   */
  private TripleAVLFile[] tripleAVLFiles = new TripleAVLFile[NR_INDEXES];

  /**
   * Description of the Field
   */
  private Phase currentPhase = null;

  /**
   * Determines if modifications can be performed without creating a new
   * (in-memory) phase. If dirty is false and the current phase is in use (by
   * unclosed Tupleses) then a new phase must be created to protect the existing
   * Tupleses before any further modifications are made.
   */
  private boolean dirty = true;

  /**
   * Description of the Field
   */
  private int phaseIndex = 0;

  /**
   * Description of the Field
   */
  private int phaseNumber = 0;

  /**
   * Description of the Field
   */
  private Phase.Token committedPhaseToken = null;

  private Object committedPhaseLock = new Object();

  /**
   * Description of the Field
   */
  private Phase.Token recordingPhaseToken = null;

  /**
   * Description of the Field
   */
  private boolean prepared = false;

  /**
   * Description of the Field
   */
  private List<ReleaseNodeListener> releaseNodeListeners = new ArrayList<ReleaseNodeListener>();

  //private XANodePoolImpl nodePool = null;


  /**
   * CONSTRUCTOR XAGraphImpl TO DO
   *
   * @param fileName PARAMETER TO DO
   * @throws IOException EXCEPTION TO DO
   */
  public XAStatementStoreImpl(String fileName) throws IOException {
    this.fileName = fileName;

    lockFile = LockFile.createLockFile(fileName + ".g.lock");

    try {
      // Check that the metaroot file was created with a compatible version
      // of the triplestore.
      RandomAccessFile metarootRAF = null;
      try {
        metarootRAF = new RandomAccessFile(fileName + ".g", "r");
        if (metarootRAF.length() >= 2 * Constants.SIZEOF_INT) {
          int fileMagic = metarootRAF.readInt();
          int fileVersion = metarootRAF.readInt();
          if (AbstractBlockFile.byteOrder != ByteOrder.BIG_ENDIAN) {
            fileMagic = XAUtils.bswap(fileMagic);
            fileVersion = XAUtils.bswap(fileVersion);
          }
          wrongFileVersion =
              fileMagic != FILE_MAGIC || fileVersion != FILE_VERSION;
        } else {
          wrongFileVersion = false;
        }
      } catch (FileNotFoundException ex) {
        wrongFileVersion = false;
      } finally {
        if (metarootRAF != null) {
          metarootRAF.close();
        }
      }

      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFiles[i] = new TripleAVLFile(
            fileName + ".g_" + orders[i][0] + orders[i][1] + orders[i][2] + orders[i][3],
            orders[i]
        );
      }
    } catch (IOException ex) {
      try {
        close();
      } catch (StatementStoreException ex2) {
        // NO-OP
      }
      throw ex;
    }
  }


  /**
   * Returns <code>true</code> if there are no triples in the graph
   *
   * @return <code>true</code> if there are no triples in the graph
   */
  public synchronized boolean isEmpty() {
    checkInitialized();
    return currentPhase.isEmpty();
  }


  /**
   * Returns a count of the number of triples in the graph
   *
   * @return a count of the number of triples in the graph
   */
  public synchronized long getNrTriples() {
    checkInitialized();
    return currentPhase.getNrTriples();
  }


  /**
   * Gets the PhaseNumber attribute of the XAGraphImpl object
   *
   * @return The PhaseNumber value
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized int getPhaseNumber() throws SimpleXAResourceException {
    checkInitialized();
    return phaseNumber;
  }


  /**
   * Adds a feature to the ReleaseNodeListener attribute of the XAGraphImpl
   * object
   *
   * @param l The feature to be added to the ReleaseNodeListener attribute
   */
  public synchronized void addReleaseNodeListener(ReleaseNodeListener l) {
    if (!releaseNodeListeners.contains(l)) {
      releaseNodeListeners.add(l);
    }
  }


  /**
   * METHOD TO DO
   *
   * @param l PARAMETER TO DO
   */
  public synchronized void removeReleaseNodeListener(ReleaseNodeListener l) {
    releaseNodeListeners.remove(l);
  }


  /**
   * Adds a new triple to the graph if it doesn't already exist.
   *
   * @param node0 the first element of the new triple
   * @param node1 the second element of the new triple
   * @param node2 the third element of the new triple
   * @param node3 the fourth element of the new triple
   * @throws StatementStoreException EXCEPTION TO DO
   */
  public synchronized void addTriple(
      long node0, long node1, long node2, long node3
  ) throws StatementStoreException {
    checkInitialized();
    if (
        node0 < NodePool.MIN_NODE ||
        node1 < NodePool.MIN_NODE ||
        node2 < NodePool.MIN_NODE ||
        node3 < NodePool.MIN_NODE
    ) {
      throw new StatementStoreException(
          "Attempt to add a triple with node number out of range: " +
          node0 + " " + node1 + " " + node2 + " " + node3
      );
    }

    if (!dirty && currentPhase.isInUse()) {
      try {
        new Phase();
      } catch (IOException ex) {
        throw new StatementStoreException("I/O error", ex);
      }
    }

    currentPhase.addTriple(node0, node1, node2, node3);
  }


  /**
   * Removes all triples matching the given specification.
   *
   * @param node0 the value for the first element of the triples
   * @param node1 the value for the second element of the triples
   * @param node2 the value for the third element of the triples
   * @param node3 the value for the fourth element of the triples
   * @throws StatementStoreException if something exceptional happens
   */
  public synchronized void removeTriples(
      long node0, long node1, long node2, long node3
  ) throws StatementStoreException {
    checkInitialized();
    if (node0 != NONE && node1 != NONE && node2 != NONE && node3 != NONE) {
      if (!dirty && currentPhase.isInUse()) {
        try {
          new Phase();
        } catch (IOException ex) {
          throw new StatementStoreException("I/O error", ex);
        }
      }

      // Remove the triple.
      currentPhase.removeTriple(node0, node1, node2, node3);
    } else {
      // Find all the tuples matching the specification and remove them.
      StoreTuples tuples = currentPhase.findTuples(node0, node1, node2, node3);
      try {
        try {
          if (!tuples.isEmpty()) {
            // There is at least one triple to remove so protect the
            // Tuples as we make changes to the triplestore.
            try {
              new Phase();
            } catch (IOException ex) {
              throw new StatementStoreException("I/O error", ex);
            }

            long[] triple = new long[] { node0, node1, node2, node3 };
            int[] columnMap = tuples.getColumnOrder();
            int nrColumns = columnMap.length;
            tuples.beforeFirst();
            while (tuples.next()) {
              // Copy the row data over to the triple.
              for (int col = 0; col < nrColumns; ++col) {
                triple[columnMap[col]] = tuples.getColumnValue(col);
              }

              currentPhase.removeTriple(
                  triple[0], triple[1], triple[2], triple[3]
              );
            }
          }
        } finally {
          tuples.close();
        }
      } catch (TuplesException ex) {
        throw new StatementStoreException(
            "Exception while iterating over temporary Tuples.", ex
        );
      }
    }
  }


  /**
   * Finds triples matching the given specification.
   *
   * @param node0 The 0 node of the triple to find.
   * @param node1 The 1 node of the triple to find.
   * @param node2 The 2 node of the triple to find.
   * @param node3 The 3 node of the triple to find.
   * @return A set of all the triples which match the search.
   * @throws StatementStoreException EXCEPTION TO DO
   */
  public synchronized StoreTuples findTuples(
      long node0, long node1, long node2, long node3
  ) throws StatementStoreException {
    checkInitialized();
    dirty = false;
    return currentPhase.findTuples(node0, node1, node2, node3);
  }

  
  /**
   * Finds triples matching the given specification and index mask.
   *
   * @param mask The mask of the index to use. This is only allowable for 3 variables
   *             and a given graph.
   * @param node0 The 0 node of the triple to find.
   * @param node1 The 1 node of the triple to find.
   * @param node2 The 2 node of the triple to find.
   * @param node3 The 3 node of the triple to find.
   * @return A set of all the triples which match the search.
   * @throws StatementStoreException EXCEPTION TO DO
   */
  public synchronized StoreTuples findTuples(
      int mask, long node0, long node1, long node2, long node3
  ) throws StatementStoreException {
    checkInitialized();
    dirty = false;
    if (!checkMask(mask, node0, node1, node2, node3)) throw new StatementStoreException("Bad explicit index selection for given node pattern.");
    return currentPhase.findTuples(mask, node0, node1, node2, node3);
  }

  /**
   * Tests a mask for consistency against the nodes it will be used to find.
   * @param mask The mask to test.
   * @param node0 The 0 node of the triple to find.
   * @param node1 The 1 node of the triple to find.
   * @param node2 The 2 node of the triple to find.
   * @param node3 The 3 node of the triple to find.
   * @return <code>true</code> if the mask is consistent with the given nodes.
   */
  private static boolean checkMask(int mask, long node0, long node1, long node2, long node3) {
    if (node0 != NONE && 0 == (mask & MASK0)) return false;
    if (node1 != NONE && 0 == (mask & MASK1)) return false;
    if (node2 != NONE && 0 == (mask & MASK2)) return false;
    if (node3 != NONE && 0 == (mask & MASK3)) return false;
    return true;
  }

  /**
   * Returns a StoreTuples which contains all triples in the store.  The
   * parameters provide a hint about how the StoreTuples will be used.  This
   * information is used to select the index from which the StoreTuples will be
   * obtained.
   *
   * @param node0Bound specifies that node0 will be bound
   * @param node1Bound specifies that node1 will be bound
   * @param node2Bound specifies that node2 will be bound
   * @param node3Bound specifies that node3 will be bound
   * @return the {@link StoreTuples}
   * @throws StatementStoreException if something exceptional happens
   */
  public synchronized StoreTuples findTuples(
      boolean node0Bound, boolean node1Bound, boolean node2Bound,
      boolean node3Bound
  ) throws StatementStoreException {
    checkInitialized();
    dirty = false;
    return currentPhase.findTuples(node0Bound, node1Bound, node2Bound, node3Bound);
  }


  /**
   * Returns <code>true</code> if any triples match the given specification.
   * Allows wild cards StatementStore.NONE for any of the node numbers.
   *
   * @param node0 The 0 node of the triple to find.
   * @param node1 The 1 node of the triple to find.
   * @param node2 The 2 node of the triple to find.
   * @param node3 The 3 node of the triple to find.
   * @return <code>true</code> if any matching triples exist in the graph.
   * @throws StatementStoreException EXCEPTION TO DO
   */
  public synchronized boolean existsTriples(
      long node0, long node1, long node2, long node3
  ) throws StatementStoreException {
    checkInitialized();
    return currentPhase.existsTriples(node0, node1, node2, node3);
  }


  public XAStatementStore newReadOnlyStatementStore() {
    return new ReadOnlyGraph();
  }


  public XAStatementStore newWritableStatementStore() {
    return this;
  }


  /**
   * Close all files, removing empty space from the ends as required.
   *
   * @throws StatementStoreException if an error occurs while truncating,
   * flushing or closing one of the three files.
   */
  public synchronized void close() throws StatementStoreException {
    try {
      unmap();
    } finally {
      try {
        IOException savedEx = null;

        for (int i = 0; i < NR_INDEXES; ++i) {
          try {
            if (tripleAVLFiles[i] != null) {
              tripleAVLFiles[i].close();
            }
          } catch (IOException ex) {
            savedEx = ex;
          }
        }

        if (metarootFile != null) {
          try {
            metarootFile.close();
          } catch (IOException ex) {
            savedEx = ex;
          }
        }

        if (savedEx != null) {
          throw new StatementStoreException("I/O error closing graph.", savedEx);
        }
      } finally {
        if (lockFile != null) {
          lockFile.release();
          lockFile = null;
        }
      }
    }
  }


  /**
   * Close this graph, if it is currently open, and remove all files associated
   * with it.
   *
   * @throws StatementStoreException EXCEPTION TO DO
   */
  public synchronized void delete() throws StatementStoreException {
    currentPhase = null;
    try {
      unmap();
    } finally {
      try {
        IOException savedEx = null;

        for (int i = 0; i < NR_INDEXES; ++i) {
          try {
            if (tripleAVLFiles[i] != null) tripleAVLFiles[i].delete();
          } catch (IOException ex) {
            savedEx = ex;
          }
        }

        if (metarootFile != null) {
          try {
            metarootFile.delete();
          } catch (IOException ex) {
            savedEx = ex;
          }
        }

        if (savedEx != null) {
          throw new StatementStoreException("I/O error deleting graph.", savedEx);
        }
      } finally {
        for (int i = 0; i < NR_INDEXES; ++i) {
          tripleAVLFiles[i] = null;
        }
        metarootFile = null;
        if (lockFile != null) {
          lockFile.release();
          lockFile = null;
        }
      }
    }
  }


  protected void finalize() {
    // close the statement store if it has not already been closed explicitly.
    try {
      close();
    } catch (Throwable t) {
      logger.warn(
          "Exception in finalize while trying to close the statement store.", t
      );
    }
  }


  /**
   * METHOD TO DO
   */
  public void release() {
    // NO-OP
    if (logger.isDebugEnabled()) {
      logger.debug("Release " + this.getClass() + ":" + System.identityHashCode(this));
    }
  }


  /**
   * METHOD TO DO
   */
  public void refresh() {
    if (logger.isDebugEnabled()) {
      logger.debug("Refresh " + this.getClass() + ":" + System.identityHashCode(this));
    }
    // NO-OP
  }


  //
  // Methods from SimpleXAResource.
  //

  /**
   * METHOD TO DO
   *
   * @param phaseNumber PARAMETER TO DO
   * @throws IOException EXCEPTION TO DO
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized void clear(int phaseNumber)
      throws IOException, SimpleXAResourceException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Clear(" + phaseNumber + ") " +
          this.getClass() + ":" + System.identityHashCode(this));
    }
    if (currentPhase != null) {
      throw new IllegalStateException("Graph already has a current phase.");
    }

    openMetarootFile(true);

    synchronized (committedPhaseLock) {
      committedPhaseToken = new Phase().use();
    }
    this.phaseNumber = phaseNumber;
    phaseIndex = 1;
    for (int i = 0; i < NR_INDEXES; ++i) {
      tripleAVLFiles[i].clear();
    }

    new Phase();
  }


  /**
   * METHOD TO DO
   *
   * @throws IOException EXCEPTION TO DO
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized void clear() throws IOException, SimpleXAResourceException {
    if (logger.isDebugEnabled()) {
      logger.debug("Clear " + this.getClass() + ":" + System.identityHashCode(this));
    }
    if (currentPhase == null) {
      clear(0);
    }

    // TODO - should throw an exception if clear() is called after any other
    // operations are performed.  Calling clear() multiple times should be
    // permitted.
  }


  /**
   * METHOD TO DO
   *
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized void prepare() throws SimpleXAResourceException {
    if (logger.isDebugEnabled()) {
      logger.debug("Prepare " + this.getClass() + ":" + System.identityHashCode(this));
    }
    checkInitialized();

    if (prepared) {
      // prepare already performed.
      throw new SimpleXAResourceException("prepare() called twice.");
    }

    try {
      // Perform a prepare.
      recordingPhaseToken = currentPhase.use();
      Phase recordingPhase = currentPhase;
      new Phase();

      // Ensure that all data associated with the phase is on disk.
      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFiles[i].force();
      }

      // Write the metaroot.
      int newPhaseIndex = 1 - phaseIndex;
      int newPhaseNumber = phaseNumber + 1;

      Block block = metarootBlocks[newPhaseIndex];
      block.putInt(IDX_VALID, 0); // should already be invalid.
      block.putInt(IDX_PHASE_NUMBER, newPhaseNumber);
      logger.debug("Writing graph metaroot for phase: " + newPhaseNumber);
      recordingPhase.writeToBlock(block, HEADER_SIZE_LONGS);
      block.write();
      metarootFile.force();
      block.putInt(IDX_VALID, 1);
      block.write();
      metarootFile.force();

      phaseIndex = newPhaseIndex;
      phaseNumber = newPhaseNumber;
      prepared = true;
    } catch (IOException ex) {
      logger.error("I/O error while performing prepare.", ex);
      throw new SimpleXAResourceException(
          "I/O error while performing prepare.", ex
      );
    } finally {
      if (!prepared) {
        // Something went wrong!
        logger.error("Prepare failed.");
        if (recordingPhaseToken != null) {
          recordingPhaseToken.release();
          recordingPhaseToken = null;
        }
      }
    }
  }


  /**
   * METHOD TO DO
   *
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized void commit() throws SimpleXAResourceException {
    if (logger.isDebugEnabled()) {
      logger.debug("Commit " + this.getClass() + ":" + System.identityHashCode(this));
    }
    if (!prepared) {
      // commit without prepare.
      throw new SimpleXAResourceException(
          "commit() called without previous prepare()."
      );
    }

    // Perform a commit.
    try {
      // Invalidate the metaroot of the old phase.
      Block block = metarootBlocks[1 - phaseIndex];
      block.putInt(IDX_VALID, 0);
      block.write();
      metarootFile.force();

      // Release the token for the previously committed phase.
      synchronized (committedPhaseLock) {
        if (committedPhaseToken != null) {
          committedPhaseToken.release();
        }
        committedPhaseToken = recordingPhaseToken;
      }
      recordingPhaseToken = null;
    } catch (IOException ex) {
      logger.fatal("I/O error while performing commit.", ex);
      throw new SimpleXAResourceException(
          "I/O error while performing commit.", ex
      );
    } finally {
      prepared = false;
      if (recordingPhaseToken != null) {
        // Something went wrong!
        recordingPhaseToken.release();
        recordingPhaseToken = null;

        logger.error("Commit failed.  Calling close().");
        try {
          close();
        } catch (Throwable t) {
          logger.error("Exception on forced close()", t);
        }
      }
    }
  }


  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized int[] recover() throws SimpleXAResourceException {
    if (logger.isDebugEnabled()) {
      logger.debug("Recover " + this.getClass() + ":" + System.identityHashCode(this));
    }
    if (currentPhase != null) {
      return new int[0];
    }
    if (wrongFileVersion) {
      throw new SimpleXAResourceException("Wrong metaroot file version.");
    }

    try {
      openMetarootFile(false);
    } catch (IOException ex) {
      throw new SimpleXAResourceException("I/O error", ex);
    }

    // Count the number of valid phases.
    int phaseCount = 0;
    if (metarootBlocks[0].getInt(IDX_VALID) != 0) {
      ++phaseCount;
    }
    if (metarootBlocks[1].getInt(IDX_VALID) != 0) {
      ++phaseCount;
    }

    // Read the phase numbers.
    int[] phaseNumbers = new int[phaseCount];
    int index = 0;
    if (metarootBlocks[0].getInt(IDX_VALID) != 0) {
      phaseNumbers[index++] = metarootBlocks[0].getInt(IDX_PHASE_NUMBER);
    }
    if (metarootBlocks[1].getInt(IDX_VALID) != 0) {
      phaseNumbers[index++] = metarootBlocks[1].getInt(IDX_PHASE_NUMBER);
    }
    return phaseNumbers;
  }


  /**
   * @param phaseNumber PARAMETER TO DO
   * @throws IOException EXCEPTION TO DO
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized void selectPhase(int phaseNumber)
      throws IOException, SimpleXAResourceException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("SelectPhase(" + phaseNumber + ") " +
          this.getClass() + ":" + System.identityHashCode(this));
    }
    if (currentPhase != null) {
      throw new SimpleXAResourceException(
          "selectPhase() called on initialized Graph."
      );
    }
    if (metarootFile == null) {
      throw new SimpleXAResourceException("Graph metaroot file is not open.");
    }

    // Locate the metaroot corresponding to the given phase number.
    if (
        metarootBlocks[0].getInt(IDX_VALID) != 0 &&
        metarootBlocks[0].getInt(IDX_PHASE_NUMBER) == phaseNumber
    ) {
      phaseIndex = 0;
      // A new phase will be saved in the other metaroot.
    } else if (
        metarootBlocks[1].getInt(IDX_VALID) != 0 &&
        metarootBlocks[1].getInt(IDX_PHASE_NUMBER) == phaseNumber
    ) {
      phaseIndex = 1;
      // A new phase will be saved in the other metaroot.
    } else {
      throw new SimpleXAResourceException(
          "Invalid phase number: " + phaseNumber
      );
    }

    // Load a duplicate of the selected phase.  The duplicate will have a
    // phase number which is one higher than the original phase.
    try {
      synchronized (committedPhaseLock) {
        committedPhaseToken = new Phase(
            metarootBlocks[phaseIndex], HEADER_SIZE_LONGS
        ).use();
      }
      this.phaseNumber = phaseNumber;
    } catch (IllegalStateException ex) {
      throw new SimpleXAResourceException(
          "Cannot construct initial phase.", ex
      );
    }
    new Phase();

    // Invalidate the on-disk metaroot that the new phase will be saved to.
    Block block = metarootBlocks[1 - phaseIndex];
    block.putInt(IDX_VALID, 0);
    block.write();
    metarootFile.force();
  }


  /**
   * METHOD TO DO
   *
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  public synchronized void rollback() throws SimpleXAResourceException {
    if (logger.isDebugEnabled()) {
      logger.debug("Rollback " + this.getClass() + ":" + System.identityHashCode(this));
    }
    checkInitialized();
    try {
      if (prepared) {
        // Restore phaseIndex and phaseNumber to their previous values.
        phaseIndex = 1 - phaseIndex;
        --phaseNumber;
        recordingPhaseToken = null;
        prepared = false;

        // Invalidate the metaroot of the other phase.
        Block block = metarootBlocks[1 - phaseIndex];
        block.putInt(IDX_VALID, 0);
        block.write();
        metarootFile.force();
      }
    } catch (IOException ex) {
      throw new SimpleXAResourceException(
          "I/O error while performing rollback (invalidating metaroot)", ex
      );
    } finally {
      try {
        new Phase(committedPhaseToken.getPhase());
      } catch (IOException ex) {
        throw new SimpleXAResourceException(
            "I/O error while performing rollback (new committed phase)", ex
        );
      }
    }
  }


  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public synchronized String toString() {
    if (currentPhase == null) {
      return "Uninitialized Graph.";
    }
    return currentPhase.toString();
  }


  /*
   * Sets the NodePool attribute of the XAStatementStoreImpl object
   *
   * @param nodePool The new NodePool value
   *
  public void setNodePool(XANodePoolImpl nodePool) {
    this.nodePool = nodePool;
  }
  */


  /**
   * METHOD TO DO
   */
  public synchronized void unmap() {
    if (committedPhaseToken != null) {
      recordingPhaseToken = null;
      prepared = false;

      try {
        new Phase(committedPhaseToken.getPhase());
      } catch (Throwable t) {
        logger.warn("Exception while rolling back in unmap()", t);
      }
      currentPhase = null;

      synchronized (committedPhaseLock) {
        committedPhaseToken.release();
        committedPhaseToken = null;
      }
    }

    if (tripleAVLFiles != null) {
      for (int i = 0; i < NR_INDEXES; ++i) {
        if (tripleAVLFiles[i] != null) {
          tripleAVLFiles[i].unmap();
        }
      }
    }

    if (metarootFile != null) {
      if (metarootBlocks[0] != null) metarootBlocks[0] = null;
      if (metarootBlocks[1] != null) metarootBlocks[1] = null;
      metarootFile.unmap();
    }
  }


  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  synchronized long checkIntegrity() {
    checkInitialized();
    return currentPhase.checkIntegrity();
  }


  /**
   * METHOD TO DO
   *
   * @param node PARAMETER TO DO
   * @throws Exception EXCEPTION TO DO
   */
  private void notifyReleaseNodeListeners(long node) throws Exception {
    for (ReleaseNodeListener l: releaseNodeListeners) l.releaseNode(node);
  }


  /**
   * METHOD TO DO
   *
   * @param clear PARAMETER TO DO
   * @throws IOException EXCEPTION TO DO
   * @throws SimpleXAResourceException EXCEPTION TO DO
   */
  private void openMetarootFile(boolean clear)
       throws IOException, SimpleXAResourceException {
    if (metarootFile == null) {
      metarootFile = AbstractBlockFile.openBlockFile(
          fileName + ".g", METAROOT_SIZE * Constants.SIZEOF_LONG,
          BlockFile.IOType.EXPLICIT
      );

      long nrBlocks = metarootFile.getNrBlocks();
      if (nrBlocks != NR_METAROOTS) {
        if (nrBlocks > 0) {
          logger.info(
              "Graph metaroot file for triple store \"" + fileName +
              "\" has invalid number of blocks: " + nrBlocks
          );
          if (nrBlocks < NR_METAROOTS) {
            clear = true;
            metarootFile.clear();
          }
        } else {
          // Perform initialization on empty file.
          clear = true;
        }
        metarootFile.setNrBlocks(NR_METAROOTS);
      }

      metarootBlocks[0] = metarootFile.readBlock(0);
      metarootBlocks[1] = metarootFile.readBlock(1);
    }

    if (clear) {
      // Invalidate the metaroots on disk.
      metarootBlocks[0].putInt(IDX_MAGIC, FILE_MAGIC);
      metarootBlocks[0].putInt(IDX_VERSION, FILE_VERSION);
      metarootBlocks[0].putInt(IDX_VALID, 0);
      metarootBlocks[0].write();
      metarootBlocks[1].putInt(IDX_MAGIC, 0);
      metarootBlocks[1].putInt(IDX_VERSION, 0);
      metarootBlocks[1].putInt(IDX_VALID, 0);
      metarootBlocks[1].write();
      metarootFile.force();
    }
  }


  /**
   * METHOD TO DO
   */
  private void checkInitialized() {
    if (currentPhase == null) {
      throw new IllegalStateException(
          "No current phase.  " +
          "Graph has not been initialized or has been closed."
      );
    }
  }


  final class ReadOnlyGraph implements XAStatementStore {

    private Phase phase = null;

    private Phase.Token token = null;


    /**
     * CONSTRUCTOR ReadOnlyGraph TO DO
     */
    ReadOnlyGraph() {
      synchronized (committedPhaseLock) {
        if (committedPhaseToken == null) {
          throw new IllegalStateException("Cannot create read only view of uninitialized Graph.");
        }
      }
    }


    public synchronized boolean isEmpty() {
      return phase.isEmpty();
    }


    /**
     * Returns a count of the number of triples in the graph
     *
     * @return a count of the number of triples in the graph
     */
    public synchronized long getNrTriples() {
      return phase.getNrTriples();
    }


    /**
     * Adds a triple to the graph.
     *
     * @param node0 The 0 node of the triple.
     * @param node1 The 1 node of the triple.
     * @param node2 The 2 node of the triple.
     * @param node3 The 3 node of the triple.
     * @throws StatementStoreException EXCEPTION TO DO
     */
    public void addTriple(
        long node0, long node1, long node2, long node3
    ) throws StatementStoreException {
      throw new UnsupportedOperationException(
          "Trying to modify a read-only graph."
      );
    }


    /**
     * Removes all triples matching the given specification.
     *
     * @param node0 the value for the first element of the triples
     * @param node1 the value for the second element of the triples
     * @param node2 the value for the third element of the triples
     * @param node3 the value for the fourth element of the triples
     * @throws StatementStoreException if something exceptional happens
     */
    public void removeTriples(
        long node0, long node1, long node2, long node3
    ) throws StatementStoreException {
      throw new UnsupportedOperationException(
          "Trying to modify a read-only graph."
      );
    }


    /**
     * Finds triples matching the given specification.
     *
     * @param node0 The 0 node of the triple to find.
     * @param node1 The 1 node of the triple to find.
     * @param node2 The 2 node of the triple to find.
     * @param node3 The 3 node of the triple to find.
     * @return A StoreTuples which contains the triples which match the search.
     * @throws StatementStoreException EXCEPTION TO DO
     */
    public synchronized StoreTuples findTuples(
        long node0, long node1, long node2, long node3
    ) throws StatementStoreException {
      return phase.findTuples(node0, node1, node2, node3);
    }

    /**
     * Finds triples matching the given specification.
     *
     * @param mask The mask of the index to use. This is only allowable for 3 variables
     *             and a given graph.
     * @param node0 The 0 node of the triple to find.
     * @param node1 The 1 node of the triple to find.
     * @param node2 The 2 node of the triple to find.
     * @param node3 The 3 node of the triple to find.
     * @return A StoreTuples which contains the triples which match the search.
     * @throws StatementStoreException EXCEPTION TO DO
     */
    public synchronized StoreTuples findTuples(
        int mask, long node0, long node1, long node2, long node3
    ) throws StatementStoreException {
      if (!checkMask(mask, node0, node1, node2, node3)) throw new StatementStoreException("Bad explicit index selection for given node pattern.");
      return phase.findTuples(mask, node0, node1, node2, node3);
    }


    /**
     * Returns a StoreTuples which contains all triples in the store.  The
     * parameters provide a hint about how the StoreTuples will be used.  This
     * information is used to select the index from which the StoreTuples will
     * be obtained.
     *
     * @param node0Bound specifies that node0 will be bound
     * @param node1Bound specifies that node1 will be bound
     * @param node2Bound specifies that node2 will be bound
     * @param node3Bound specifies that node3 will be bound
     * @return the {@link StoreTuples}
     * @throws StatementStoreException if something exceptional happens
     */
    public synchronized StoreTuples findTuples(
        boolean node0Bound, boolean node1Bound, boolean node2Bound,
        boolean node3Bound
    ) throws StatementStoreException {
      return phase.findTuples(node0Bound, node1Bound, node2Bound, node3Bound);
    }


    public synchronized boolean existsTriples(
        long node0, long node1, long node2, long node3
    ) throws StatementStoreException {
      return phase.existsTriples(node0, node1, node2, node3);
    }


    public XAStatementStore newReadOnlyStatementStore() {
      throw new UnsupportedOperationException();
    }


    public XAStatementStore newWritableStatementStore() {
      throw new UnsupportedOperationException();
    }


    public void close() {
      throw new UnsupportedOperationException("Trying to close a read-only graph.");
    }


    public void delete() {
      throw new UnsupportedOperationException(
          "Trying to delete a read-only graph."
      );
    }


    /**
     * Release the phase.
     */
    public synchronized void release() {
      if (logger.isDebugEnabled()) {
        logger.debug("Releasing " + this.getClass() + ":" + System.identityHashCode(this));
      }
      try {
        if (token != null) token.release();
      } finally {
        phase = null;
        token = null;
      }
    }


    public synchronized void refresh() {
      if (logger.isDebugEnabled()) {
        logger.debug("Refreshing " + this.getClass() + ":" + System.identityHashCode(this));
      }

      synchronized (committedPhaseLock) {
        Phase committedPhase = committedPhaseToken.getPhase();
        if (phase != committedPhase) {
          if (token != null) token.release();
          phase = committedPhase;
          token = phase.use();
        }
      }
    }

    public void addReleaseNodeListener(ReleaseNodeListener l) {
      throw new UnsupportedOperationException();
    }
    public void removeReleaseNodeListener(ReleaseNodeListener l) {
      throw new UnsupportedOperationException();
    }

    public void prepare() {
      if (logger.isDebugEnabled()) {
        logger.debug("Preparing " + this.getClass() + ":" + System.identityHashCode(this));
      }
    }

    public void commit() {
      if (logger.isDebugEnabled()) {
        logger.debug("Commit " + this.getClass() + ":" + System.identityHashCode(this));
      }
    }

    public void rollback() {
      if (logger.isDebugEnabled()) {
        logger.debug("Rollback " + this.getClass() + ":" + System.identityHashCode(this));
      }
    }

    public void clear() {
      if (logger.isDebugEnabled()) {
        logger.debug("Clearing " + this.getClass() + ":" + System.identityHashCode(this));
      }
    }

    public void clear(int phaseNumber) {
      if (logger.isDebugEnabled()) {
        logger.debug("Clearing (" + phaseNumber + ") " +
            this.getClass() + ":" + System.identityHashCode(this));
      }
    }

    public int[] recover() {
      if (logger.isDebugEnabled()) {
        logger.debug("Recovering " + this.getClass() + ":" + System.identityHashCode(this));
      }
      throw new UnsupportedOperationException("Attempting to recover ReadOnlyGraph");
    }

    public void selectPhase(int phaseNumber) {
      if (logger.isDebugEnabled()) {
        logger.debug("Selecting Phase " + this.getClass() + ":" + System.identityHashCode(this));
      }
      throw new UnsupportedOperationException("Attempting to selectPhase of ReadOnlyGraph");
    }

    public int getPhaseNumber() {
      return phaseNumber;
    }


    /**
     * Ignored for this implementation.
     */
    public void initializeSystemNodes(long systemGraphNode, long rdfTypeNode, long systemGraphTypeNode) {
      // do nothing
    }
  }


  final class Phase implements PersistableMetaRoot {

    final static int RECORD_SIZE = TripleAVLFile.Phase.RECORD_SIZE * NR_INDEXES;

    private TripleAVLFile.Phase[] tripleAVLFilePhases =
        new TripleAVLFile.Phase[NR_INDEXES];


    /**
     * CONSTRUCTOR Phase TO DO
     *
     * @throws IOException EXCEPTION TO DO
     */
    Phase() throws IOException {
      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFilePhases[i] = tripleAVLFiles[i].new Phase();
      }
      currentPhase = this;
      dirty = true;
    }


    /**
     * CONSTRUCTOR Phase TO DO
     *
     * @throws IOException EXCEPTION TO DO
     */
    Phase(Phase p) throws IOException {
      assert p != null;

      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFilePhases[i] = tripleAVLFiles[i].new Phase(
            p.tripleAVLFilePhases[i]
        );
      }
      currentPhase = this;
      dirty = true;
    }


    /**
     * CONSTRUCTOR Phase TO DO
     *
     * @param b PARAMETER TO DO
     * @param offset PARAMETER TO DO
     * @throws IOException EXCEPTION TO DO
     */
    Phase(Block b, int offset) throws IOException {
      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFilePhases[i] = tripleAVLFiles[i].new Phase(b, offset);
        offset += TripleAVLFile.Phase.RECORD_SIZE;
      }
      currentPhase = this;
      dirty = false;
    }


    /**
     * Writes this PersistableMetaRoot to the specified Block. The ints are
     * written at the specified offset.
     *
     * @param b the Block.
     * @param offset PARAMETER TO DO
     */
    public void writeToBlock(Block b, int offset) {
      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFilePhases[i].writeToBlock(b, offset);
        offset += TripleAVLFile.Phase.RECORD_SIZE;
      }
    }


    public String toString() {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < NR_INDEXES; ++i) {
        StoreTuples ts = tripleAVLFilePhases[i].allTuples();
        try {
          sb.append(ts).append('\n');
        } finally {
          try {
            ts.close();
          } catch (TuplesException ex) {
            logger.warn("TuplesException while closing Tuples", ex);
            return ex.toString();
          }
        }
      }
      return sb.toString();
    }


    boolean isInUse() {
      for (int i = 0; i < NR_INDEXES; ++i) {
        if (tripleAVLFilePhases[i].isInUse()) {
          return true;
        }
      }
      return false;
    }


    boolean isEmpty() {
      return tripleAVLFilePhases[TI_0123].isEmpty();
    }


    long getNrTriples() {
      return tripleAVLFilePhases[TI_0123].getNrTriples();
    }


    /**
     * Adds a new triple to the graph if it doesn't already exist.
     *
     * @param node0 the first element of the new triple
     * @param node1 the second element of the new triple
     * @param node2 the third element of the new triple
     * @param node3 the fourth element of the new triple
     * @throws StatementStoreException EXCEPTION TO DO
     */
    void addTriple(long node0, long node1, long node2, long node3)
         throws StatementStoreException {
      assert node0 >= NodePool.MIN_NODE;
      assert node1 >= NodePool.MIN_NODE;
      assert node2 >= NodePool.MIN_NODE;
      assert node3 >= NodePool.MIN_NODE;

      //if (
      //  DEBUG && nodePool != null &&
      //  !nodePool.isValid(node0) && !nodePool.isValid(node1) &&
      //  !nodePool.isValid(node2) && !nodePool.isValid(node3)
      //) throw new AssertionError(
      //  "Attempt to add a triple with an invalid node"
      //);

      long[] triple = new long[]{node0, node1, node2, node3};

      for (int i = 0; i < NR_INDEXES; ++i) {
        tripleAVLFilePhases[i].asyncAddTriple(triple);
      }
    }


    /**
     * Removes the specified triple.
     *
     * @param node0 the value for the first element of the triple
     * @param node1 the value for the second element of the triple
     * @param node2 the value for the third element of the triple
     * @param node3 the value for the fourth element of the triple
     * @throws StatementStoreException if something exceptional happens
     */
    void removeTriple(
        long node0, long node1, long node2, long node3
    ) throws StatementStoreException {
      if (
          node0 < NodePool.MIN_NODE ||
          node1 < NodePool.MIN_NODE ||
          node2 < NodePool.MIN_NODE ||
          node3 < NodePool.MIN_NODE
      ) {
        throw new StatementStoreException(
            "Attempt to remove a triple with node number out of range: " +
            node0 + " " + node1 + " " + node2 + " " + node3
        );
      }

      try {
        for (int i = 0; i < NR_INDEXES; ++i) {
          tripleAVLFilePhases[i].removeTriple(node0, node1, node2, node3);
        }

        if (RELEASE_NODE_LISTENERS_ENABLED) {
          // Check if any of the four nodes are no longer in use.

          // TODO batch up multiple nodes to check instead of checking them
          // on every removeTriple.

          if (
              !tripleAVLFilePhases[TI_0123].existsTriples(node0) &&
              !tripleAVLFilePhases[TI_1203].existsTriples(node0) &&
              !tripleAVLFilePhases[TI_2013].existsTriples(node0) &&
              !tripleAVLFilePhases[TI_3012].existsTriples(node0)
          ) {
            try {
              notifyReleaseNodeListeners(node0);
            } catch (Error e) {
              throw new StatementStoreException("ReleaseNodeListener threw error", e);
            } catch (Exception e) {
              throw new StatementStoreException("ReleaseNodeListener threw exception", e);
            }
          }

          if (node1 != node0) {
            if (
                !tripleAVLFilePhases[TI_0123].existsTriples(node1) &&
                !tripleAVLFilePhases[TI_1203].existsTriples(node1) &&
                !tripleAVLFilePhases[TI_2013].existsTriples(node1) &&
                !tripleAVLFilePhases[TI_3012].existsTriples(node1)
            ) {
              try {
                notifyReleaseNodeListeners(node1);
              } catch (Error e) {
                throw new StatementStoreException("ReleaseNodeListener threw error", e);
              } catch (Exception e) {
                throw new StatementStoreException("ReleaseNodeListener threw exception", e);
              }
            }
          }

          if (node2 != node0 && node2 != node1) {
            if (
                !tripleAVLFilePhases[TI_0123].existsTriples(node2) &&
                !tripleAVLFilePhases[TI_1203].existsTriples(node2) &&
                !tripleAVLFilePhases[TI_2013].existsTriples(node2) &&
                !tripleAVLFilePhases[TI_3012].existsTriples(node2)
            ) {
              try {
                notifyReleaseNodeListeners(node2);
              } catch (Error e) {
                throw new StatementStoreException("ReleaseNodeListener threw error", e);
              } catch (Exception e) {
                throw new StatementStoreException("ReleaseNodeListener threw exception", e);
              }
            }
          }

          if (node3 != node0 && node3 != node1 && node3 != node2) {
            if (
                !tripleAVLFilePhases[TI_0123].existsTriples(node3) &&
                !tripleAVLFilePhases[TI_1203].existsTriples(node3) &&
                !tripleAVLFilePhases[TI_2013].existsTriples(node3) &&
                !tripleAVLFilePhases[TI_3012].existsTriples(node3)
            ) {
              try {
                notifyReleaseNodeListeners(node3);
              } catch (Error e) {
                throw new StatementStoreException("ReleaseNodeListener threw error", e);
              } catch (Exception e) {
                throw new StatementStoreException("ReleaseNodeListener threw exception", e);
              }
            }
          }
        }
      } catch (IOException e) {
        throw new StatementStoreException("I/O error", e);
      }
    }

    /**
     * Finds triples matching the given specification.
     *
     * @param variableMask the mask used to indicate the desired index.
     * @param node0 The 0 node of the triple to find.
     * @param node1 The 1 node of the triple to find.
     * @param node2 The 2 node of the triple to find.
     * @param node3 The 3 node of the triple to find.
     * @return A StoreTuples containing all the triples which match the search.
     * @throws StatementStoreException EXCEPTION TO DO
     */
    StoreTuples findTuples(int variableMask, long node0, long node1, long node2, long node3) throws StatementStoreException {
      if (
          node0 < NodePool.NONE ||
          node1 < NodePool.NONE ||
          node2 < NodePool.NONE ||
          node3 < NodePool.NONE
      ) {
        // There is at least one query node.  Return an empty StoreTuples.
        return TuplesOperations.empty();
      }

      if (0 == (variableMask & MASK3)) throw new StatementStoreException("This version of find is for re-ordering graphs, base on a given mask.");
      try {
        switch (variableMask) {
          case MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3);
          case MASK0 | MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3);
          case MASK1 | MASK3:
            return tripleAVLFilePhases[TI_3120].findTuples(node3);
          case MASK0 | MASK1 | MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3);
          case MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3201].findTuples(node3);
          case MASK0 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3201].findTuples(node3);
          case MASK1 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3120].findTuples(node3);
          case MASK0 | MASK1 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3);
          default:
            throw new AssertionError();
        }
      } catch (IOException ex) {
        throw new StatementStoreException("I/O error", ex);
      }
    }

    /**
     * Finds triples matching the given specification.
     *
     * @param node0 The 0 node of the triple to find.
     * @param node1 The 1 node of the triple to find.
     * @param node2 The 2 node of the triple to find.
     * @param node3 The 3 node of the triple to find.
     * @return A StoreTuples containing all the triples which match the search.
     * @throws StatementStoreException EXCEPTION TO DO
     */
    StoreTuples findTuples(long node0, long node1, long node2, long node3) throws StatementStoreException {
      if (
          node0 < NodePool.NONE ||
          node1 < NodePool.NONE ||
          node2 < NodePool.NONE ||
          node3 < NodePool.NONE
      ) {
        // There is at least one query node.  Return an empty StoreTuples.
        return TuplesOperations.empty();
      }

      int variableMask =
        (node0 != NONE ? MASK0 : 0) |
        (node1 != NONE ? MASK1 : 0) |
        (node2 != NONE ? MASK2 : 0) |
        (node3 != NONE ? MASK3 : 0);

      try {
        switch (variableMask) {
          case 0:
            return tripleAVLFilePhases[TI_0123].allTuples();
          case MASK0:
            return tripleAVLFilePhases[TI_0123].findTuples(node0);
          case MASK1:
            return tripleAVLFilePhases[TI_1203].findTuples(node1);
          case MASK0 | MASK1:
            return tripleAVLFilePhases[TI_0123].findTuples(node0, node1);
          case MASK2:
            return tripleAVLFilePhases[TI_2013].findTuples(node2);
          case MASK0 | MASK2:
            return tripleAVLFilePhases[TI_2013].findTuples(node2, node0);
          case MASK1 | MASK2:
            return tripleAVLFilePhases[TI_1203].findTuples(node1, node2);
          case MASK0 | MASK1 | MASK2:
            return tripleAVLFilePhases[TI_0123].findTuples(node0, node1, node2);
          case MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3);
          case MASK0 | MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3, node0);
          case MASK1 | MASK3:
            return tripleAVLFilePhases[TI_3120].findTuples(node3, node1);
          case MASK0 | MASK1 | MASK3:
            return tripleAVLFilePhases[TI_3012].findTuples(node3, node0, node1);
          case MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3201].findTuples(node3, node2);
          case MASK0 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3201].findTuples(node3, node2, node0);
          case MASK1 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3120].findTuples(node3, node1, node2);
          case MASK0 | MASK1 | MASK2 | MASK3:
            if (tripleAVLFilePhases[TI_0123].existsTriple(node0, node1, node2, node3)) {
              return TuplesOperations.unconstrained();
            }
            return TuplesOperations.empty();
          default:
            throw new AssertionError();
        }
      } catch (IOException ex) {
        throw new StatementStoreException("I/O error", ex);
      }
    }


    StoreTuples findTuples(
        boolean node0Bound, boolean node1Bound,
        boolean node2Bound, boolean node3Bound
    ) throws StatementStoreException {
      int variableMask =
          (node0Bound ? MASK0 : 0) |
          (node1Bound ? MASK1 : 0) |
          (node2Bound ? MASK2 : 0) |
          (node3Bound ? MASK3 : 0);

      return tripleAVLFilePhases[selectIndex[variableMask]].allTuples();
    }


    boolean existsTriples(long node0, long node1, long node2, long node3) throws StatementStoreException {
      if (
          node0 < NodePool.NONE ||
          node1 < NodePool.NONE ||
          node2 < NodePool.NONE ||
          node3 < NodePool.NONE
      ) {
        // There is at least one query node.  Return an empty StoreTuples.
        return false;
      }

      int variableMask =
          (node0 != NONE ? MASK0 : 0) |
          (node1 != NONE ? MASK1 : 0) |
          (node2 != NONE ? MASK2 : 0) |
          (node3 != NONE ? MASK3 : 0);

      try {
        switch (variableMask) {
          case 0:
            return !tripleAVLFilePhases[TI_0123].isEmpty();
          case MASK0:
            return tripleAVLFilePhases[TI_0123].existsTriples(node0);
          case MASK1:
            return tripleAVLFilePhases[TI_1203].existsTriples(node1);
          case MASK0 | MASK1:
            return tripleAVLFilePhases[TI_0123].existsTriples(node0, node1);
          case MASK2:
            return tripleAVLFilePhases[TI_2013].existsTriples(node2);
          case MASK0 | MASK2:
            return tripleAVLFilePhases[TI_2013].existsTriples(node2, node0);
          case MASK1 | MASK2:
            return tripleAVLFilePhases[TI_1203].existsTriples(node1, node2);
          case MASK0 | MASK1 | MASK2:
            return tripleAVLFilePhases[TI_0123].existsTriples(node0, node1, node2);
          case MASK3:
            return tripleAVLFilePhases[TI_3012].existsTriples(node3);
          case MASK0 | MASK3:
            return tripleAVLFilePhases[TI_3012].existsTriples(node3, node0);
          case MASK1 | MASK3:
            return tripleAVLFilePhases[TI_3120].existsTriples(node3, node1);
          case MASK0 | MASK1 | MASK3:
            return tripleAVLFilePhases[TI_3012].existsTriples(node3, node0, node1);
          case MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3201].existsTriples(node3, node2);
          case MASK0 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3201].existsTriples(node3, node2, node0);
          case MASK1 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_3120].existsTriples(node3, node1, node2);
          case MASK0 | MASK1 | MASK2 | MASK3:
            return tripleAVLFilePhases[TI_0123].existsTriple(node0, node1, node2, node3);
          default:
            throw new AssertionError();
        }
      } catch (IOException ex) {
        throw new StatementStoreException("I/O error", ex);
      }
    }


    long checkIntegrity() {
      long nrTriples[] = new long[NR_INDEXES];

      for (int i = 0; i < NR_INDEXES; ++i) {
        nrTriples[i] = tripleAVLFilePhases[i].checkIntegrity();
      }

      for (int i = 1; i < NR_INDEXES; ++i) {
        if (nrTriples[0] != nrTriples[i]) {
          StringBuffer sb = new StringBuffer("tripleAVLFiles disagree on the number of triples:");
          for (int j = 0; j < NR_INDEXES; ++j) sb.append(' ').append(nrTriples[j]);
          throw new RuntimeException(sb.toString());
        }
      }

      return nrTriples[0];
    }


    Token use() {
      return new Token();
    }


    final class Token {

      private TripleAVLFile.Phase.Token[] tripleAVLFileTokens = new TripleAVLFile.Phase.Token[NR_INDEXES];

      private Phase phase = Phase.this;


      /**
       * CONSTRUCTOR Token TO DO
       */
      Token() {
        for (int i = 0; i < NR_INDEXES; ++i) tripleAVLFileTokens[i] = tripleAVLFilePhases[i].use();
      }


      public Phase getPhase() {
        assert tripleAVLFileTokens != null : "Invalid Token";
        return phase;
      }


      public void release() {
        assert tripleAVLFileTokens != null : "Invalid Token";
        for (int i = 0; i < NR_INDEXES; ++i) tripleAVLFileTokens[i].release();
        tripleAVLFileTokens = null;
        phase = null;
      }

    }

  }


  /**
   * Ignored for this implementation.
   */
  public void initializeSystemNodes(long systemGraphNode, long rdfTypeNode, long systemGraphTypeNode) {
    // do nothing
  }

}
