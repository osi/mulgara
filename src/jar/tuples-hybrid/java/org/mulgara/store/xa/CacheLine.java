package org.mulgara.store.xa;

import org.apache.log4j.Logger;

import org.mulgara.query.TuplesException;
import org.mulgara.util.StackTrace;

/**
 *
 *
 * @created 2004-03-22
 *
 * @author Andrae Muys
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:12 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class CacheLine implements Cloneable {
  protected static final int SIZEOF_LONG = 8;

  protected final int segmentSize;

  protected StackTrace closed;
  protected int firstCloser;


  public CacheLine(int size) {
    this.segmentSize = size;
    this.closed = null;
  }


  /**
   * Return the currentTuple.  If tuple is null, will allocate if necessary
   * or return an internal tuple.
   *
   * @return The current tuple.  This is immutable (even if it is the same one 
   *         you passed in).  Reference may be retained.  Do not modify.
   */
  public abstract long[] getCurrentTuple(long[] tuple);
  public abstract long[] getPreviousTuple(long[] tuple);
  public abstract boolean isEmpty();
  public abstract void advance() throws TuplesException;


  private final static Logger logger = Logger.getLogger(CacheLine.class);


  public int getSegmentSize() {
    return segmentSize;
  }


  public void reset(long[] prefix) throws TuplesException {
    if (closed != null) {
      throw new TuplesException("Attempt to reset closed MemoryCacheLine");
    }
  }


  public void close(int closer) throws TuplesException {
    if (closed != null) {
      logger.error("Attempt to close CacheLine twice by " + closer + new StackTrace());
      logger.error("    First closed at " + closed);
      logger.error("    First closed by " + firstCloser);
      throw new TuplesException("Attempt to close CacheLine twice");
    }
    closed = new StackTrace();
    firstCloser = closer;
  }


  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException ce) {
      throw new RuntimeException("CacheLine.clone() threw CloneNotSupported", ce);
    }
  }


  protected int matchPrefix(long[] value, long[] prefix) {
    for (int i = 0; i < prefix.length; i++) {
      if (value[i] == prefix[i]) {
        continue;
      } else {
        return value[i] - prefix[i] > 0 ? +1 : -1;
      }
    }

    return 0;
  }
}
