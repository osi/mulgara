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
 * Contributor(s):
 *   XAResource access copyright 2007 The Topaz Foundation.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.server.rmi;

// Java 2 Standard Packages
import java.rmi.*;
import java.io.*;
import java.util.*;
import java.net.*;

// JRDF
import org.jrdf.graph.*;

// emory util package
import edu.emory.mathcs.util.remote.io.*;

// Locally written packages
import org.mulgara.query.Answer;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.rules.InitializerException;
import org.mulgara.rules.Rules;  // Required only for Javadoc
import org.mulgara.rules.RulesException;
import org.mulgara.rules.RulesRef;


/**
 * Java RMI remote interface for drivers.
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @created 2001-09-11
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/06/26 12:48:15 $
 *
 * @maintenanceAuthor $Author: pgearon $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
interface RemoteSession extends Remote {

  /**
   * Insert RDFStatements into a model.
   *
   * @param modelURI The URI of the model to insert into.
   * @param statements The Set of RDFStatements to insert into the model.
   * @throws QueryException if the insert cannot be completed.
   * @throws RemoteException EXCEPTION TO DO
   */
  public void insert(URI modelURI, Set statements) throws QueryException,
      RemoteException;

  /**
   * Insert statements from one model into another model.
   *
   * @param modelURI URI The URI of the model to insert into.
   * @param query The query to perform on the server.
   * @throws QueryException if the insert cannot be completed.
   * @throws RemoteException EXCEPTION TO DO
   */
  public void insert(URI modelURI, Query query) throws QueryException,
      RemoteException;

  /**
   * Delete RDFStatements from a model.
   *
   * @param modelURI The URI of the model to delete from.
   * @param statements The Set of RDFStatements to delete from the model.
   * @throws QueryException if the deletion cannot be completed.
   * @throws RemoteException EXCEPTION TO DO
   */
  public void delete(URI modelURI, Set statements) throws QueryException,
      RemoteException;

  /**
   * Delete statements from a model.
   *
   * @param modelURI The URI of the model to delete from.
   * @param query The query to perform on the server.
   * @throws QueryException if the deletion cannot be completed.
   * @throws RemoteException EXCEPTION TO DO
   */
  public void delete(URI modelURI, Query query) throws QueryException,
      RemoteException;

  /**
   * Backup the specified server. The database is not changed by this method.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param destinationURI The URI of the backup file to dump into.
   * @throws QueryException if the backup cannot be completed.
   * @throws RemoteException EXCEPTION TO DO
   */
  public void backup(URI sourceURI, URI destinationURI) throws QueryException,
      RemoteException;

  /**
   * Backup all the data on the specified server to an output stream.
   * The database is not changed by this method.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param outputStream The stream to receive the contents
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, OutputStream outputStream)
    throws QueryException, RemoteException;

  /**
   * Restore the specified server.
   *
   * @param serverURI The URI of the model to dump from.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   * @throws RemoteException EXCEPTION TO DO
   */
  public void restore(URI serverURI, URI sourceURI) throws QueryException,
      RemoteException;


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
      throws QueryException, RemoteException;

  /**
   * Create a new model.
   *
   * @param modelURI the {@link URI} of the new model
   * @param modelTypeURI the {@link URI} of the type of the new model
   * @throws QueryException if the model can't be created
   * @throws RemoteException EXCEPTION TO DO
   */
  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException,
      RemoteException;

  /**
   * Tests for the existance of a model.
   *
   * @param uri the {@link URI} of the model.
   * @return true if the model exists or false if it doesn't.
   * @throws QueryException if the query against the system model fails.
   * @throws RemoteException if there was a communications error.
   */
  public boolean modelExists(URI uri) throws QueryException, RemoteException;

  /**
   * Remove an existing model.
   *
   * @param uri the {@link URI} of the doomed model
   * @throws QueryException if the model can't be removed
   * @throws RemoteException EXCEPTION TO DO
   */
  public void removeModel(URI uri) throws QueryException, RemoteException;

  /**
   * Define the contents of a model.
   *
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return The number of statements inserted into the model
   * @throws QueryException if the model can't be modified
   * @throws RemoteException EXCEPTION TO DO
   */
  public long setModel(URI uri,
      ModelExpression modelExpression) throws QueryException, RemoteException;

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
                      ModelExpression modelExpression) throws QueryException,
      RemoteException;

  /**
   * Sets the AutoCommit attribute of the RemoteSession object
   *
   * @param autoCommit The new AutoCommit value
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException,
      RemoteException;

  /**
   * METHOD TO DO
   *
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void commit() throws QueryException, RemoteException;

  /**
   * METHOD TO DO
   *
   * @throws QueryException EXCEPTION TO DO
   * @throws RemoteException EXCEPTION TO DO
   */
  public void rollback() throws QueryException, RemoteException;

  /**
   * Make a TQL query.
   *
   * @param query the query
   * @return a non-<code>null</code> answer to the <var>query</var>
   * @throws QueryException if <var>query</var> can't be answered
   * @throws RemoteException if the remote connection fails
   */
  public RemoteAnswer query(Query query) throws QueryException, RemoteException;

  /**
   * Make a list of TQL query.
   *
   * @param queries A list of queries
       * @return A List of non-<code>null</code> answers to the <var>queries</var> .
   *      The position of the answer corresponds to the position of the
   *      parameter query.
   * @throws QueryException if <var>query</var> can't be answered
   * @throws RemoteException if the remote connection fails
   */
  public List query(List queries) throws QueryException, RemoteException;

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
  public RulesRef buildRules(URI ruleModel, URI baseModel, URI destModel) throws QueryException, org.mulgara.rules.InitializerException, RemoteException;

  /**
   * Rules a set of {@link Rules} on its defined model.
   *
   * @param rules The rules to be run.
   * @throws RulesException An error was encountered executing the rules.
   */
  public void applyRules(RulesRef rules) throws RulesException, RemoteException;

  /**
   * Release resources associated with this session. The session won't be usable
   * after this method is invoked.
   *
   * @throws RemoteException EXCEPTION TO DO
   */
  public void close() throws QueryException, RemoteException;

  /**
   * Add authentication data to the session.
   *
   * @param securityDomain the URI uniquely identifying the security domain to
   *      which these credentials apply
   * @param username the identity to authenticate as
   * @param password the secret used to prove identity
   * @see org.mulgara.server.SessionFactory#getSecurityDomain
   * @throws RemoteException EXCEPTION TO DO
   */
  public void login(URI securityDomain, String username,
      char[] password) throws RemoteException;

  /**
   * Obtain an XAResource for this session.
   */
  public RemoteXAResource getXAResource() throws QueryException, RemoteException;
  public RemoteXAResource getReadOnlyXAResource() throws QueryException, RemoteException;
}
