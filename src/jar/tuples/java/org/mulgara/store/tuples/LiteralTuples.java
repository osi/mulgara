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

package org.mulgara.store.tuples;

// Java 2 standard packages
import java.util.*;

// Third party packages
import org.apache.log4j.*;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.query.Cursor;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;

/**
 *
 * @created 2003-01-09
 *
 * @author <a href="http://staff.pisoftware.com/andrae">Andrae Muys</a>
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/03/07 17:28:28 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class LiteralTuples extends AbstractTuples {

  /**
   * Logger.
   */
  private static Logger logger =
      Logger.getLogger(LiteralTuples.class.getName());

  private int numberOfVariables;

  private List tuples;
  private Iterator tupleIterator;

  private int currentTuple;
  private boolean[] columnContainsUnbound;
  private long[] prefix;

  /**
   * Creates a literal tuples with specified variables.
   */
  public LiteralTuples(Variable[] variables) {
    init(variables);
  }

  /**
   * Creates a literal tuples with specified variables.
   * Variables created to match variableNames[].
   */
  public LiteralTuples(String[] variableNames) {
    List vars = new ArrayList();
    for (int i = 0; i < variableNames.length; i++) {
      Variable v = new Variable(variableNames[i]);
      assert!vars.contains(v);
      vars.add(v);
    }
    init((Variable[]) vars.toArray(new Variable[0]));
  }

  private void init(Variable[] variables) {
    tuples = new ArrayList();
    currentTuple = 0;
    tupleIterator = null;
    setVariables(Arrays.asList(variables));
    columnContainsUnbound = new boolean[variables.length];
    Arrays.fill(columnContainsUnbound, false);
  }

  /**
   * Create a new set of tuples.
   *
   * @param vars String[] the variable columns.
   * @param tuplesValues long[][] the values.
   * @return LiteralTuples the newly created tuples.
   * @throws TuplesException if there was an error creating them.
   */
  public static LiteralTuples create(String[] vars, long[][] tuplesValues)
      throws TuplesException {
    LiteralTuples tuples = new LiteralTuples(vars);
    for (int i = 0; i < tuplesValues.length; i++) {
      tuples.appendTuple(tuplesValues[i]);
    }
    return tuples;
  }


  public void appendTuple(long[] tuple) throws TuplesException {
    if (tupleIterator != null) {
      throw new TuplesException("Can't append row after beforeFirst is called");
    }
    if (tuple.length != getNumberOfVariables()) {
      throw new TuplesException("Arity of rows dosn't match arity of tuples");
    }

    for (int i = 0; i < tuple.length; i++) {
      if (tuple[i] == Tuples.UNBOUND) {
        columnContainsUnbound[i] = true;
      }
    }

    tuples.add(tuple.clone());
  }

  public long getColumnValue(int column) throws TuplesException {
    assert column >= 0;
    if (tuples == null) {
      throw new TuplesException("getColumnValue called before beforeFirst()");
    }

    return ((long[]) tuples.get(currentTuple))[column];
  }

  public long getRowCount() throws TuplesException {
    return tuples.size();
  }

  public long getRowUpperBound() throws TuplesException {
    return getRowCount();
  }

  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return columnContainsUnbound[column];
  }

  public boolean isMaterialized() {
    return true;
  }

  public List getOperands() {
    return new ArrayList(0);
  }

  public RowComparator getComparator() {
    return null;
  }

  //
  // Methods implementing Tuples interface
  //

  public void beforeFirst() throws TuplesException {
    beforeFirst(Tuples.NO_PREFIX, 0);
  }

  public void beforeFirst(long[] prefix,
      int suffixTruncation) throws TuplesException {
//    Throwable th = new Throwable();
//    th.fillInStackTrace();
//    logger.debug("LiteralTuples[" + Arrays.asList(getVariables()) + "].beforeFirst called with prefix " + toString(prefix), th);
//    logger.debug("LiteralTuples[" + Arrays.asList(getVariables()) + "].beforeFirst called with prefix " + toString(prefix));

    assert suffixTruncation == 0;

    this.prefix = (long[]) prefix.clone();

    search:for (currentTuple = 0; currentTuple < tuples.size(); currentTuple++) {
      for (int j = 0; j < prefix.length; j++) {
        if (((long[]) tuples.get(currentTuple))[j] != prefix[j]) {
          continue search;
        }
      }
      // Found prefix;
      currentTuple -= 1;
      break;
    }
  }

  public boolean next() throws TuplesException {
    if (++currentTuple < tuples.size()) {
      for (int j = 0; j < prefix.length; j++) {
        if (((long[]) tuples.get(currentTuple))[j] != prefix[j]) {
          return false;
        }
      }

      return true;
    }

    return false;
  }

  public void close() throws TuplesException {
    // Do nothing.
  }

  public boolean hasNoDuplicates() throws TuplesException {
    return isUnconstrained();
  }

  public Object clone() {
    return super.clone();
  }
}
