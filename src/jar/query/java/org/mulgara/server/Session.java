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
 *    XAResource addition copyright 2008 The Topaz Foundation
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.server;


// Java 2 Standard Packages
import java.net.*;
import java.util.*;
import java.io.*;
import javax.transaction.xa.XAResource;

// Locally written packages
import org.jrdf.graph.Triple;
import org.mulgara.query.Answer;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.rules.InitializerException;
import org.mulgara.rules.Rules;  // Required only for Javadoc
import org.mulgara.rules.RulesRef;

/**
 * Mulgara interaction session.
 *
 * @created 2001-11-11
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @author <a href="http://staff.pisoftware.com/kkucks">Kevin Kucks</a>
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/06/26 12:48:10 $ by $Author: pgearon $
 *
 * @maintenanceAuthor $Author: pgearon $
 *
 * @copyright &copy;2001-2003
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface Session {

  /**
   * This constant can be passed to {@link #createModel} to indicate that a
   * normal model backed by a triple store is required.
   */
  public final URI MULGARA_GRAPH_URI = URI.create(Mulgara.NAMESPACE + "Model");

  /**
   * Insert statements into a model.
   *
   * @param modelURI The URI of the model to insert into.
   * @param statements The Set of statements to insert into the model.
   * @throws QueryException if the insert cannot be completed.
   */
  public void insert(URI modelURI, Set<? extends Triple> statements) throws QueryException;

  /**
   * Insert statements from the results of a query into another model.
   *
   * @param modelURI URI The URI of the model to insert into.
   * @param query The query to perform on the server.
   * @throws QueryException if the insert cannot be completed.
   */
  public void insert(URI modelURI, Query query) throws QueryException;

  /**
   * Delete the set of statements from a model.
   *
   * @param modelURI The URI of the model to delete from.
   * @param statements The Set of statements to delete from the model.
   * @throws QueryException if the deletion cannot be completed.
   */
  public void delete(URI modelURI, Set<? extends Triple> statements) throws QueryException;

  /**
   * Delete statements from a model using the results of query.
   *
   * @param modelURI The URI of the model to delete from.
   * @param query The query to perform on the server.
   * @throws QueryException if the deletion cannot be completed.
   */
  public void delete(URI modelURI, Query query) throws QueryException;

  /**
   * Backup all the data on the specified server. The database is not changed by
   * this method.  Does not require an exclusive lock on the database and will
   * begin with the currently committed state.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param destinationURI The URI of the file to backup into.
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, URI destinationURI)
    throws QueryException;

  /**
   * Backup all the data on the specified server to an output stream.
   * The database is not changed by this method.  Does not require an exclusive
   * lock on the database and will begin with the currently committed state.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param outputStream The stream to receive the contents
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, OutputStream outputStream)
    throws QueryException;

  /**
   * Restore all the data on the specified server. If the database is not
   * currently empty then the database will contain the union of its current
   * content and the content of the backup file when this method returns.
   *
   * @param serverURI The URI of the server to restore.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   */
  public void restore(URI serverURI, URI sourceURI) throws QueryException;

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
      throws QueryException;

  /**
   * Perform a single query.
   *
   * @param query the query
   * @return a non-<code>null</code> answer to the <var>query</var>
   * @throws QueryException if <var>query</var> can't be answered
   */
  public Answer query(Query query) throws QueryException;

  /**
   * Performs multiple queries storing the results, answers, into the returned
   * list.
   *
   * @param queries the list of query objects.
   * @return a list of non-<code>null</code> answers to the <var>queries</var>
   * @throws QueryException if <var>query</var> can't be answered
   */
  public List<Answer> query(List<Query> queries) throws QueryException;

  /**
   * Creates a new model of a given type.  The standard model type is
   * {@link #MULGARA_GRAPH_URI}.
   *
   * @param modelURI the {@link URI} of the new model
   * @param modelTypeURI the {@link URI} identifying the type of model to use
   *   (e.g. Lucene); if <code>null</code>, use the same type as the system
   *   models
   * @throws QueryException if the model can't be created
   */
  public void createModel(URI modelURI, URI modelTypeURI)
    throws QueryException;

  /**
   * Remove an existing model.
   *
   * @param uri the {@link URI} of the doomed model.
   * @throws QueryException if the model can't be removed or doesn't exist.
   */
  public void removeModel(URI uri) throws QueryException;

  /**
   * Tests for the existance of a model.
   *
   * @param uri the {@link URI} of the model.
   * @throws QueryException if the query against the system model fails.
   * @return true if the model exists or false if it doesn't.
   */
  public boolean modelExists(URI uri) throws QueryException;

  /**
   * Define the contents of a model via a {@link ModelExpression}
   *
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return The number of statements inserted into the model
   * @throws QueryException if the model can't be modified
   */
  public long setModel(URI uri, ModelExpression modelExpression)
    throws QueryException;

  /**
   * Define the contents of a model via an {@link InputStream}.
   *
   * @param inputStream a remote inputstream
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return The number of statements inserted into the model
   * @throws QueryException if the model can't be modified
   */
  public long setModel(InputStream inputStream, URI uri,
      ModelExpression modelExpression) throws QueryException;

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
  public RulesRef buildRules(URI ruleModel, URI baseModel, URI destModel) throws QueryException, org.mulgara.rules.InitializerException;

  /**
   * Rules a set of {@link Rules} on its defined model.
   *
   * @param rules The rules to be run.
   * @throws QueryException An error was encountered executing the rules.
   * @throws QueryException An error was encountered accessing the rules accross a network.
   */
  public void applyRules(RulesRef rules) throws QueryException;

  /**
   * Sets whether permanent changes made to the database in this session
   * occur immediately (true) or until a commit has been made (false).  A
   * session may lose autocommit false status (the write phase) if it is idle.
   * By default a session is set to true.
   *
   * @param autoCommit true to make changes available to other sessions, false
   *   to allow rollback/commit.
   * @throws QueryException if it fails to suspend or resume the transaction.
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException;

  /**
   * Commits changes to the database so that other sessions can see the
   * modifications.  Requires that autocommit has been set to false.  It does
   * not change the autocommit state.
   *
   * @throws QueryException if there was an exception commiting the changes
   *   to the database.
   */
  public void commit() throws QueryException;

  /**
   * Undo the changes made to the database since autocommit has been set off.
   * It does not change the autocommit state.
   *
   * @throws QueryException if there was an exception rolling back the changes
   *   to the database.
   */
  public void rollback() throws QueryException;

  /**
   * Release resources associated with this session. The session won't be usable
   * after this method is invoked.
   */
  public void close() throws QueryException;

  /**
   * Returns true if the session is local (within the same JVM).
   *
   * @return if the session is local (within the same JVM)
   */
  public boolean isLocal();

  /**
   * Add authentication data to the session.
   *
   * @param securityDomain the URI uniquely identifying the security domain to
   *      which these credentials apply
   * @param username the identity to authenticate as
   * @param password the secret used to prove identity
   * @see SessionFactory#getSecurityDomain
   */
  public void login(URI securityDomain, String username, char[] password);

  /**
   * Obtain an XAResource for this Session.
   *
   * Use of this method is incompatible with any use of implicit or internally
   * mediated transactions with this Session.
   * Transactions initiated from the XAResource returned by the read-only
   * version of this method will be read-only.
   */
  public XAResource getXAResource() throws QueryException;
  public XAResource getReadOnlyXAResource() throws QueryException;

  /**
   * This class is just a devious way to get static initialization for the
   * {@link Session} interface.
   */
  abstract class ConstantFactory {

    static URI getMulgaraModelURI() {
      try {
        return new URI(Mulgara.NAMESPACE + "Model");
      }
       catch (URISyntaxException e) {
        throw new Error("Bad hardcoded URI");
      }
    }
  }
}
