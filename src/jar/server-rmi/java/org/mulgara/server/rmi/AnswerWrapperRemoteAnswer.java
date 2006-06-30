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

package org.mulgara.server.rmi;

// Java 2 standard packages
import java.rmi.*;
import java.util.*;
import java.rmi.server.UnicastRemoteObject;

// Third party packages
import org.apache.log4j.*;
import org.mulgara.query.Answer;
import org.mulgara.query.ArrayAnswer;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;
import org.mulgara.util.KowariResultSet;

/**
 * Remote ITQL answer. An answer is a set of solutions, where a solution is a
 * mapping of {@link Variable}s to {@link org.mulgara.query.Value}s.
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @created 2001-07-31
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/01/28 00:29:22 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class AnswerWrapperRemoteAnswer extends UnicastRemoteObject implements
    RemoteAnswer, Cloneable {
  /** logger */
  private static Logger logger =
      Logger.getLogger(AnswerWrapperRemoteAnswer.class.getName());

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  static final long serialVersionUID = -1887399686697126359L;

  /**
   * The wrapped instance.
   */
  protected final Answer answer;

  /**
   * Wraps a new answer for paging of results.
   *
   * @param answer the instance to wrap
   * @throws IllegalArgumentException if <var>answer</var> is <code>null</code>
   */
  public AnswerWrapperRemoteAnswer(Answer answer) throws RemoteException {
    // Validate "answer" parameter
    if (answer == null) {
      throw new IllegalArgumentException("Null \"answer\" parameter");
    }

    // Initialize wrapped field
    this.answer = answer;
  }

  /**
   * Reset the instance to iterate from the beginning.
   *
   * Needs to be followed by a call to {@link #next} before reading can start.
   * Synchronized to prevent a prefetch thread from asking for a page while this
   * is still in the process of executing.
   *
   * @throws TuplesException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public synchronized AnswerPage beforeFirstAndInitPage() throws
      TuplesException, RemoteException {
    try {
      answer.beforeFirst();
      AnswerPage result = nextPage();
      return result;
    }
    catch (TuplesException et) {
      logger.error("TuplesException thrown", et);
      throw et;
    }
    catch (RemoteException er) {
      logger.error("RemoteException thrown", er);
      throw er;
    }
    catch (Throwable t) {
      logger.error("Throwable thrown", t);
      throw new TuplesException("Throwable thrown", t);
    }
  }

  /**
   * Return the object at the given index.
   * TODO: This should not be called now due to paging.  Check.
   *
   * @param column PARAMETER TO DO
   * @return the value at the given index
   * @throws TuplesException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public Object getObject(int column) throws TuplesException, RemoteException {
    return optimiseObject(answer.getObject(column));
  }

  /**
   * Return the object at the given column name.
   * TODO: This should not be called now due to paging.  Check.
   *
   * @param columnName the index of the object to retrieve
   * @return the value at the given index
   * @throws SQLException on failure
   * @throws TuplesException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public Object getObject(String columnName) throws TuplesException,
      RemoteException {
    return optimiseObject(answer.getObject(columnName));
  }

  /**
   * Checks an object type, and if an Answer then converts it into a suitable form for the network.
   * TODO: This should not be called now due to paging.  Check.
   *
   * @param obj The object to check and possibly convert.
   * @return An object optimised for network transport.
   */
  private Object optimiseObject(Object obj) throws TuplesException,
      RemoteException {
    if (obj instanceof Answer) {
      // Standard answers are not serializable, so either instantiate it into a serializable object
      // or wrap in a remote reference
      Answer ans = (Answer) obj;
      if (ans.getRowUpperBound() <= MARSHALL_SIZE_LIMIT) {
        obj = new ArrayAnswer(ans); // return a serialised answer
        ans.close();
      }
      else {
        obj = new AnswerWrapperRemoteAnswer(ans); // return a remote reference to the answer
      }
    }
    return obj;
  }

  /**
   * @return the number of columns
   */
  public int getNumberOfVariables() throws RemoteException {
    return answer.getNumberOfVariables();
  }

  /**
   * The variables bound and their default collation order. The array returned
   * by this method should be treated as if its contents were immutable, even
   * though Java won't enforce this. If the elements of the array are modified,
   * there may be side effects on the past and future clones of the tuples it
   * was obtained from.
   *
   * @return the {@link Variable}s bound within this answer.
   * @throws RemoteException EXCEPTION TO DO
   */
  public Variable[] getVariables() throws RemoteException {
    return answer.getVariables();
  }

  /**
   * Tests whether this is a unit-valued answer. A unit answer appended to
   * something yields the unit answer. A unit answer joined to something yields
   * the same something. Notionally, the unit answer has zero columns and one
   * row.
   *
   * @return The Unconstrained value
   * @throws TuplesException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public boolean isUnconstrained() throws TuplesException, RemoteException {
    return answer.isUnconstrained();
  }

  /**
   * Advance to the next term in the solution.
   *
   * @return <code>false<code> if there was no further term to advance to.
   * @throws TuplesException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public boolean next() throws TuplesException, RemoteException {
    return answer.next();
  }

  /**
   * Advance to the next page in the solution.  This advances the answer by one page.
   * Synchronized to prevent a prefetch thread from asking for a page while this
   * is still in the process of executing.
   *
   * @return The new page, or <code>null</code> if there is no data left in the answer.
   * @throws TuplesException Iterating through the answer caused a problem.
   * @throws RemoteException Required for RMI interfaces.
   */
  public synchronized AnswerPage nextPage() throws TuplesException,
      RemoteException {
    try {
      AnswerPageImpl page = new AnswerPageImpl(answer);
      return (page.getPageSize() == 0) ? null : page;
    }
    catch (TuplesException et) {
      logger.warn("TuplesException thrown in nextPage", et);
      throw et;
    }
    catch (Throwable t) {
      logger.warn("Throwable thrown in nextPage", t);
      throw new TuplesException("Error in nextPage", t);
    }
  }

  /**
   * Accessor for the binding of a given variable within the current product
   * term (row).
   *
   * @param column PARAMETER TO DO
   * @return the bound value, or {@link org.mulgara.store.tuples.Tuples#UNBOUND}
   *      if there is no binding within the current product term (row)
   * @throws TuplesException if there is no current row (before first or after
   *      last) or if <var>variable</var> isn't an element of {@link
   *      #getVariables}
   * @throws RemoteException EXCEPTION TO DO
   */
  public int getColumnIndex(Variable column) throws TuplesException,
      RemoteException {
    return answer.getColumnIndex(column);
  }

  /**
   * This method returns the number of rows which this instance contains.
   *
   * @return the number of rows that this instance contains.
   * @throws TuplesException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public long getRowCount() throws TuplesException, RemoteException {
    return answer.getRowCount();
  }

  public long getRowUpperBound() throws TuplesException, RemoteException {
    return answer.getRowUpperBound();
  }

  public int getRowCardinality() throws TuplesException {
    return answer.getRowCardinality();
  }

  /**
   * Free resources associated with this instance.
   *
   * @throws RemoteException EXCEPTION TO DO
   */
  public void close() throws TuplesException, RemoteException {
    answer.close();
    try {
      unexportObject(this, false);
    }
    catch (NoSuchObjectException e) {
      // doesn't matter if this object was not exported, but that shouldn't happen
    }
  }

  /**
   * Returns a copy of this object.
   *
   * @return Object
   */
  public Object clone() {

    //copy the answer too
    Answer answerClone = (Answer)this.answer.clone();

    //reset the original Answer
    try {

      answerClone.beforeFirst();

      return new AnswerWrapperRemoteAnswer(answerClone);
    }
    catch (TuplesException tuplesException) {

      throw new RuntimeException(tuplesException);
    }
    catch (RemoteException remoteException) {

      throw new RuntimeException(remoteException);
    }
  }

  /**
   * Returns a copy of this object for a remote client
   *
   * @throws RemoteException is the server fails to clone
   * @return <var>RemoteAnswer</var>
   */
  public RemoteAnswer remoteClone() throws RemoteException {
    return (RemoteAnswer)this.clone();
  }
}
