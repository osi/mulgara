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
import java.io.*;
import java.net.URI;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

// Third party packages
import org.jrdf.graph.*;
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.Answer;
import org.mulgara.query.ArrayAnswer;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.rules.InitializerException;
import org.mulgara.rules.Rules;  // Required only for Javadoc
import org.mulgara.rules.RulesException;
import org.mulgara.rules.RulesRef;
import org.mulgara.server.Session;


/**
 * Wrapper around a {@link Session} to make it look like a
 * {@link RemoteSession}.
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @created 2002-01-03
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/06/26 12:48:16 $ by $Author: pgearon $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2002-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class SessionWrapperRemoteSession implements RemoteSession {

  /** Logger.  */
  private static final Logger logger = Logger.getLogger(SessionWrapperRemoteSession.class.getName());

  /**
   * The wrapped {@link Session}
   */
  private final Session session;

  //
  // Constructor
  //

  /**
   * @param session the wrapped session
   * @throws IllegalArgumentException if <var>session</var> is <code>null</code>
   */
  protected SessionWrapperRemoteSession(Session session) {

    // Validate "session" parameter
    if (session == null) {

      throw new IllegalArgumentException("Null \"session\" parameter");
    }

    // Initialize fields
    this.session = session;
  }

  /**
   * Sets the Model attribute of the SessionWrapperRemoteSession object
   *
   * @param uri The new Model value
   * @param modelExpression The new Model value
   * @return RETURNED VALUE TO DO
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public long setModel(URI uri,
      ModelExpression modelExpression) throws QueryException, RemoteException {
    try {
      return session.setModel(uri, modelExpression);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Define the contents of a model via an inputstream.
   *
   * @param inputStream a remote inputstream
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return The number of statements inserted into the model
   * @throws QueryException if the model can't be modified
   */
  public long setModel(InputStream inputStream, URI uri,
                       ModelExpression modelExpression)
    throws QueryException {

    try {
      return session.setModel(inputStream, uri, modelExpression);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }


  /**
   * Sets the AutoCommit attribute of the SessionWrapperRemoteSession object
   *
   * @param autoCommit The new AutoCommit value
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException,
      RemoteException {

    try {
      session.setAutoCommit(autoCommit);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  //
  // Methods implementing the RemoteSession interface
  //

  public void insert(URI modelURI, Set statements) throws QueryException,
      RemoteException {

    try {
      session.insert(modelURI, statements);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  public void insert(URI modelURI, Query query) throws QueryException,
      RemoteException {

    try {
      session.insert(modelURI, query);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  public void delete(URI modelURI, Set statements) throws QueryException,
      RemoteException {

    try {
      session.delete(modelURI, statements);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  public void delete(URI modelURI, Query query) throws QueryException,
      RemoteException {

    try {
      session.delete(modelURI, query);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Backup all the data on the specified server. The database is not changed by
   * this method.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param destinationURI The URI of the file to backup into.
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, URI destinationURI) throws QueryException,
      RemoteException {

    try {
      session.backup(sourceURI, destinationURI);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Backup all the data on the specified server to an output stream.
   * The database is not changed by this method.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param outputStream The stream to receive the contents
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, OutputStream outputStream)
    throws QueryException, RemoteException {

    try {
      session.backup(sourceURI, outputStream);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Restore all the data on the specified server. If the database is not
   * currently empty then the database will contain the union of its current
   * content and the content of the backup file when this method returns.
   *
   * @param serverURI The URI of the server to restore.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   */
  public void restore(URI serverURI, URI sourceURI) throws QueryException,
      RemoteException {

    try {
      session.restore(serverURI, sourceURI);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Restore all the data on the specified server. If the database is not
   * currently empty then the database will contain the union of its current
   * content and the content of the backup file when this method returns.
   *
   * @param inputStream a client supplied inputStream to obtain the restore
   *        content from. If null assume the sourceURI has been supplied.
   * @param serverURI The URI of the server to restore.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   */
  public void restore(InputStream inputStream, URI serverURI, URI sourceURI)
      throws QueryException, RemoteException {

    try {
      session.restore(inputStream, serverURI, sourceURI);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }

  }


  /**
   * METHOD TO DO
   *
   * @param modelURI PARAMETER TO DO
   * @param modelTypeURI PARAMETER TO DO
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException,
      RemoteException {

    try {
      session.createModel(modelURI, modelTypeURI);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * METHOD TO DO
   *
   * @param uri PARAMETER TO DO
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void removeModel(URI uri) throws QueryException, RemoteException {

    try {
      session.removeModel(uri);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  public boolean modelExists(URI uri) throws QueryException, RemoteException {
    try {
      return session.modelExists(uri);
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * METHOD TO DO
   *
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void commit() throws QueryException, RemoteException {

    try {
      session.commit();
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * METHOD TO DO
   *
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void rollback() throws QueryException, RemoteException {

    try {
      session.rollback();
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Queries the local server and returns a remote reference to an Answer.
   *
   * @param query The query to perform.
   * @return A remote reference to an Answer.
   * @throws QueryException The query caused an exception.
   * @throws RemoteException Thrown when there is a network error.
   */
  public RemoteAnswer query(Query query) throws QueryException, RemoteException {

    try {
      Answer ans = session.query(query);
      try {
        if (ans.getRowUpperBound() <= RemoteAnswer.MARSHALL_SIZE_LIMIT) {
          RemoteAnswer serialAnswer = new AnswerWrapperRemoteAnswerSerialised(new
              ArrayAnswer(ans));
          ans.close();
          return serialAnswer;
        }
        else {
          return new AnswerWrapperRemoteAnswer(ans);
        }
      }
      catch (TuplesException e) {
        throw new QueryException("Error getting information for answer", e);
      }
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Queries a local server for a list of queries.  Wraps the resulting answers in remote
   * objects before returning them.
   *
   * @param queries A List of Query objects to be executed in order.
   * @return A list of remote references to Answer objects.  This list gets fully marshalled for returning.
   * @throws QueryException There was an exception on one of the queries, or a query returned a non-Answer.
   * @throws RemoteException Thrown when there is a network error.
   */
  public List query(List queries) throws QueryException, RemoteException {

    try {
      List localAnswers = session.query(queries);
      List servedAnswers = new ArrayList(localAnswers.size());

      Iterator i = localAnswers.iterator();
      while (i.hasNext()) {
        Object servedAnswer = null;
        Object ans = i.next();
        if (!(ans instanceof Answer))
          throw new QueryException("Non-answer returned from query.");
        try {
          if (((Answer) ans).getRowUpperBound() <= RemoteAnswer.MARSHALL_SIZE_LIMIT) {
            // don't need to wrap this in an
            // AnswerWrapperRemoteAnswerSerialised as the other end can handle
            // any kind of object as it comes out of the list
            servedAnswer = new ArrayAnswer((Answer) ans);
          }
          else {
            servedAnswer = new AnswerWrapperRemoteAnswer((Answer) ans);
          }
          ((Answer) ans).close();
        }
        catch (TuplesException e) {
          throw new QueryException("Error getting information for answer", e);
        }
        servedAnswers.add(servedAnswer);
      }

      return servedAnswers;
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }
  }

  /**
   * Extract {@link Rules} from the data found in a model.
   *
   * @param ruleModel The URI of the model with the rule structure.
   * @param baseModel The URI of the model with the base data to read.
   * @param destModel The URI of the model to receive the entailed data.
   * @return The extracted rule structure.
   * @throws InitializerException If there was a problem accessing the rule loading module.
   * @throws QueryException If there was a problem loading the rule structure.
   */
  public RulesRef buildRules(URI ruleModel, URI baseModel, URI destModel) throws QueryException, org.mulgara.rules.InitializerException, RemoteException {
    RulesRef r = null;
    try {
      r = session.buildRules(ruleModel, baseModel, destModel);
    } catch (QueryException qe) {
      throw qe;
    } catch (org.mulgara.rules.InitializerException ie) {
      throw ie;
    } catch (Throwable t) {
      throw convertToQueryException(t);
    }
    return r;
  }

  /**
   * Rules a set of {@link Rules} on its defined model.
   *
   * @param rules The rules to be run.
   * @throws RulesException An error was encountered executing the rules.
   */ 
  public void applyRules(RulesRef rules) throws RulesException, RemoteException {
    try {
      session.applyRules(rules);
    } catch (RulesException re) {
      throw re;
    } catch (Throwable t) {
      throw new RulesException(t.toString(), t);
    }
  }

  /**
   * METHOD TO DO
   *
   * @throws RemoteException EXCEPTION TO DO
   */
  public void close() throws QueryException, RemoteException {

    try {
      session.close();
    }
    catch (Throwable t) {
      throw convertToQueryException(t);
    }

  }

  /**
   * METHOD TO DO
   *
   * @param securityDomain PARAMETER TO DO
   * @param username PARAMETER TO DO
   * @param password PARAMETER TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void login(URI securityDomain, String username,
      char[] password) throws RemoteException {

    session.login(securityDomain, username, password);
  }

  // Construct an exception chain that will pass over RMI.
  protected Throwable mapThrowable(Throwable t) {
    Throwable cause = t.getCause();
    Throwable mappedCause = cause != null ? mapThrowable(cause) : null;
    Class tClass = t.getClass();
    String tClassName = tClass.getName();

    if (
        t instanceof QueryException || (
        t instanceof Error ||
        t instanceof RuntimeException
        ) && tClassName.startsWith("java.")
        ) {
      // This exception can pass over RMI - but maybe not the cause.

      // Check if the cause has been reinstantiated.
      if (cause == mappedCause) {
        // There has been no change to the cause chain so just return this
        // Throwable unchanged.
        return t;
      }

      // TODO use reflection to instantiate a Throwable of the same class.
      // for now we just fall through and construct a QueryException.
    }

    String message = t.getMessage();
    if (!(t instanceof QueryException)) {
      // Prepend the class name to the message
      message = tClassName + ": " + message;
    }

    QueryException qe = new QueryException(message, mappedCause);
    qe.setStackTrace(t.getStackTrace());
    return qe;
  }

  /**
   * Return t if it is already a QueryException or wrap it as one.
   *
   * @return t if it is already a QueryException or wrap it as one.
   */
  private QueryException convertToQueryException(Throwable t) {
    t = mapThrowable(t);
    if (t instanceof QueryException)return (QueryException) t;
    return new QueryException(t.toString(), t);
  }
}
