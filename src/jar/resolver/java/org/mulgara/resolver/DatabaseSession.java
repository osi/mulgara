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
 *   SymbolicTransformation refactor contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver;

// Java 2 standard packages
import java.io.*;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

// Java 2 enterprise packages
import javax.transaction.RollbackException;
import javax.transaction.Status;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.*;

// Local packages
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerManager;
import org.mulgara.query.*;
import org.mulgara.query.rdf.*;
import org.mulgara.resolver.spi.*;
import org.mulgara.resolver.spi.ResolverFactoryException;
import org.mulgara.rules.*;
import org.mulgara.server.Session;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.tuples.TuplesOperations;

/**
 * A database session.
 *
 * @created 2004-04-26
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.17 $
 *
 * @modified $Date: 2005/07/03 13:00:26 $ by $Author: pgearon $
 *
 * @maintenanceAuthor $Author: pgearon $
 *
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class DatabaseSession implements Session {
  public static final boolean ASSERT_STATEMENTS = true;
  public static final boolean DENY_STATEMENTS = false;

  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(DatabaseSession.class.getName());

  /**
   * Resolver factories that should be have access to their models cached.
   *
   * This field is read-only.
   */
  private final Set cachedResolverFactorySet;

  /** The list of all registered {@link ResolverFactory} instances.  */
  private final List resolverFactoryList;

  /**
   * Map from URL protocol {@link String}s to the {@link ResolverFactory} which
   * handles external models using that protocol.
   */
  private final Map externalResolverFactoryMap;

  /**
   * Map from modelType {@link LocalNode}s to the {@link ResolverFactory} which
   * handles that model type.
   */
  private final Map internalResolverFactoryMap;

  private final DatabaseMetadata metadata;

  /** Security adapters this instance should enforce. */
  private final List securityAdapterList;

  /** Symbolic transformations this instance should apply. */
  private final List symbolicTransformationList;

  /** Persistent string pool. */
  private final ResolverSessionFactory resolverSessionFactory;

  /** Factory used to obtain the SystemResolver */
  private final SystemResolverFactory systemResolverFactory;

  /** Factory used to obtain the SystemResolver */
  private final ResolverFactory temporaryResolverFactory;

  /** Source of transactions.  */
  private final MulgaraTransactionManager transactionManager;

  /** The name of the rule loader to use */
  private String ruleLoaderClassName;

  /** A fallback rule loader */
  private static final String DUMMY_RULE_LOADER = "org.mulgara.rules.DummyRuleLoader";

  /** The registered {@link ContentHandler} instances.  */
  private ContentHandlerManager contentHandlers;

  /** The temporary model type-URI. */
  private final URI temporaryModelTypeURI;

  /**
   * Construct a database session.
   *
   * @param transactionManager  the source of transactions for this session,
   *   never <code>null</code>
   * @param securityAdapterList  {@link List} of {@link SecurityAdapter}s to be
   *   consulted before permitting operations, never <code>null</code>
   * @param symbolicTransformationList  {@link List} of
   *   {@link SymbolicTransformation}s, never <code>null</code>
   * @param resolverSessionFactory  source of {@link ResolverSessionFactory}s,
   *   never <code>null</code>
   * @param systemResolverFactory  Source of {@link SystemResolver}s to manage
   *   persistent models, for instance the system model (<code>#</code>); never
   *   <code>null</code>
   * @param temporaryResolverFactory  Source of {@link Resolver}s to manage
   *   models which only last the duration of a transaction, for instance the
   *   contents of external RDF/XML documents; never <code>null</code>
   * @param resolverFactoryList  the list of registered {@link ResolverFactory}
   *   instances to use for constraint resolution, never <code>null</code>
   * @param externalResolverFactoryMap  map from URL protocol {@link String}s
   *   to {@link ResolverFactory} instances for models accessed via that
   *   protocol, never <code>null</code>
   * @param internalResolverFactoryMap  map from model type {@link LocalNode}s
   *   to {@link ResolverFactory} instances for that model type, never
   *   <code>null</code>
   * @param metadata  even more parameters from the parent {@link Database},
   *   never <code>null</code>
   * @param contentHandlers contains the list of valid registered content handles
   *   never <code>null</code>
   * @param temporaryModelTypeURI  the URI of the model type to use to cache
   *   external models
   * @throws IllegalArgumentException if any argument is <code>null</code>
   */
  DatabaseSession(MulgaraTransactionManager transactionManager,
      List securityAdapterList,
      List symbolicTransformationList,
      ResolverSessionFactory resolverSessionFactory,
      SystemResolverFactory systemResolverFactory,
      ResolverFactory temporaryResolverFactory,
      List resolverFactoryList,
      Map externalResolverFactoryMap,
      Map internalResolverFactoryMap,
      DatabaseMetadata metadata,
      ContentHandlerManager contentHandlers,
      Set cachedResolverFactorySet,
      URI temporaryModelTypeURI,
      String ruleLoaderClassName) throws ResolverFactoryException {

    if (logger.isInfoEnabled()) {
      logger.info("Constructing DatabaseSession: externalResolverFactoryMap=" +
          externalResolverFactoryMap + " internalResolverFactoryMap=" +
          internalResolverFactoryMap + " metadata=" + metadata);
    }

    // Validate parameters
    if (transactionManager == null) {
      throw new IllegalArgumentException("Null 'transactionManager' parameter");
    } else if (securityAdapterList == null) {
      throw new IllegalArgumentException("Null 'securityAdapterList' parameter");
    } else if (symbolicTransformationList == null) {
      throw new IllegalArgumentException("Null 'symbolicTransformationList' parameter");
    } else if (resolverSessionFactory == null) {
      throw new IllegalArgumentException("Null 'resolverSessionFactory' parameter");
    } else if (systemResolverFactory == null) {
      throw new IllegalArgumentException("Null 'systemResolverFactory' parameter");
    } else if (temporaryResolverFactory == null) {
      throw new IllegalArgumentException("Null 'temporaryResolverFactory' parameter");
    } else if (resolverFactoryList == null) {
      throw new IllegalArgumentException("Null 'resolverFactoryList' parameter");
    } else if (externalResolverFactoryMap == null) {
      throw new IllegalArgumentException("Null 'externalResolverFactoryMap' parameter");
    } else if (internalResolverFactoryMap == null) {
      throw new IllegalArgumentException("Null 'internalResolverFactoryMap' parameter");
    } else if (contentHandlers == null) {
      throw new IllegalArgumentException("Null 'contentHandlers' parameter");
    } else if (metadata == null) {
      throw new IllegalArgumentException("Null 'metadata' parameter");
    } else if (cachedResolverFactorySet == null) {
      throw new IllegalArgumentException("Null 'cachedResolverFactorySet' parameter");
    } else if (temporaryModelTypeURI == null) {
      throw new IllegalArgumentException("Null 'temporaryModelTypeURI' parameter");
    } else if (ruleLoaderClassName == null) {
      ruleLoaderClassName = DUMMY_RULE_LOADER;
    }

    // Initialize fields
    this.transactionManager = transactionManager;
    this.securityAdapterList = securityAdapterList;
    this.symbolicTransformationList = symbolicTransformationList;
    this.resolverSessionFactory = resolverSessionFactory;
    this.systemResolverFactory = systemResolverFactory;
    this.temporaryResolverFactory = temporaryResolverFactory;
    this.resolverFactoryList = resolverFactoryList;
    this.externalResolverFactoryMap = externalResolverFactoryMap;
    this.internalResolverFactoryMap = internalResolverFactoryMap;
    this.metadata                   = metadata;
    this.contentHandlers            = contentHandlers;
    this.cachedResolverFactorySet   = cachedResolverFactorySet;
    this.temporaryModelTypeURI      = temporaryModelTypeURI;
    this.ruleLoaderClassName        = ruleLoaderClassName;

    if (logger.isDebugEnabled()) {
      logger.debug("Constructed DatabaseSession");
    }

    // Set the transaction timeout to an hour
    transactionManager.setTransactionTimeout(3600);
  }


  /**
   * Non-rule version of the constructor.  Accepts all parameters except ruleLoaderClassName.
   */
  DatabaseSession(MulgaraTransactionManager transactionManager,
      List securityAdapterList,
      List symbolicTransformationList,
      ResolverSessionFactory resolverSessionFactory,
      SystemResolverFactory systemResolverFactory,
      ResolverFactory temporaryResolverFactory,
      List resolverFactoryList,
      Map externalResolverFactoryMap,
      Map internalResolverFactoryMap,
      DatabaseMetadata metadata,
      ContentHandlerManager contentHandlers,
      Set cachedResolverFactorySet,
      URI temporaryModelTypeURI) throws ResolverFactoryException {
    this(transactionManager, securityAdapterList, symbolicTransformationList, resolverSessionFactory,
        systemResolverFactory, temporaryResolverFactory, resolverFactoryList, externalResolverFactoryMap,
        internalResolverFactoryMap, metadata, contentHandlers, cachedResolverFactorySet,
        temporaryModelTypeURI, "");
  }

  //
  // Internal methods required for database initialisation.
  //

  /**
   * Used by Database *only* to bootstrap the system model on DB startup.
   */
  long bootstrapSystemModel(DatabaseMetadataImpl metadata) throws QueryException {
    logger.info("Bootstrapping System Model");

    BootstrapOperation operation = new BootstrapOperation(metadata);
    execute(operation, "Failed to bootstrap system-model");

    systemResolverFactory.setDatabaseMetadata(metadata);

    return operation.getResult();
  }

  /**
   * Preallocate a local node number for an RDF {@link Node}.
   *
   * This method is used only by {@link DatabaseResolverFactoryInitializer}
   * and {@link DatabaseSecurityAdapterInitializer}.
   *
   * @param node  an RDF node
   * @return the preallocated local node number corresponding to the
   *   <var>node</var>, never {@link NodePool#NONE}
   * @throws QueryException if the local node number can't be obtained
   */
  long preallocate(Node node) throws QueryException {
    PreallocateOperation preOp = new PreallocateOperation(node);
    execute(preOp, "Failure to preallocated " + node);

    return preOp.getResult();
  }

  //
  // Methods implementing Session
  //

  public void insert(URI modelURI, Set statements) throws QueryException {
    modify(modelURI, statements, ASSERT_STATEMENTS);
  }

  public void insert(URI modelURI, Query query) throws QueryException {
    modify(modelURI, query, ASSERT_STATEMENTS);
  }

  public void delete(URI modelURI, Set statements) throws QueryException {
    modify(modelURI, statements, DENY_STATEMENTS);
  }

  public void delete(URI modelURI, Query query) throws QueryException {
    modify(modelURI, query, DENY_STATEMENTS);
  }

  /**
   * Backup all the data on the specified server. The database is not changed by
   * this method.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param destinationURI The URI of the file to backup into.
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, URI destinationURI) throws QueryException {
    this.backup(null, sourceURI, destinationURI);
  }

  /**
   * Backup all the data on the specified server to an output stream.
   * The database is not changed by this method.
   *
   * @param sourceURI The URI of the server or model to backup.
   * @param outputStream The stream to receive the contents
   * @throws QueryException if the backup cannot be completed.
   */
  public void backup(URI sourceURI, OutputStream outputStream) throws QueryException {
    this.backup(outputStream, sourceURI, null);
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
  public void restore(URI serverURI, URI sourceURI) throws QueryException {
    this.restore(null, serverURI, sourceURI);
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
  public void restore(InputStream inputStream, URI serverURI,
      URI sourceURI) throws QueryException {
    execute(new RestoreOperation(inputStream, serverURI, sourceURI),
            "Unable to restore from " + sourceURI);
  }

  public Answer query(Query query) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("QUERY: " + query);
    }

    QueryOperation queryOperation = new QueryOperation(query, this);
    execute(queryOperation, "Query failed");
    return queryOperation.getAnswer();
  }

  public List query(List queryList) throws QueryException {
    if (logger.isInfoEnabled()) {
      StringBuffer log = new StringBuffer("QUERYING LIST: ");
      for (int i = 0; i < queryList.size(); i++) {
        log.append(queryList.get(i));
      }
      logger.info(log.toString());
    }

    QueryOperation queryOperation = new QueryOperation(queryList, this);
    execute(queryOperation, "Failed list query");
    return queryOperation.getAnswerList();
  }


  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("Creating Model " + modelURI + " with type " + modelTypeURI);
    }

    execute(new CreateModelOperation(modelURI, modelTypeURI),
            "Could not commit creation of model " + modelURI + " of type " +
              modelTypeURI);
  }

  public void removeModel(URI modelURI) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("REMOVE MODEL: " + modelURI);
    }
    if (modelURI == null) {
      throw new IllegalArgumentException("Null 'modelURI' parameter");
    }

    execute(new RemoveModelOperation(modelURI), "Unable to remove " + modelURI);
  }

  public boolean modelExists(URI modelURI) throws QueryException {
    ModelExistsOperation operation = new ModelExistsOperation(modelURI);

    execute(operation, "Failed to determine model existence");

    return operation.getResult();
  }

  /**
   * Define the contents of a model.
   *
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return RETURNED VALUE TO DO
   * @throws QueryException if the model can't be modified
   */
  public synchronized long setModel(URI uri,
      ModelExpression modelExpression) throws QueryException {
    return this.setModel(null, uri, modelExpression);
  }

  /**
   * Define the contents of a model via an inputstream
   *
   * @param inputStream a remote inputstream
   * @param destinationModelURI the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return RETURNED VALUE TO DO
   * @throws QueryException if the model can't be modified
   */
  public synchronized long setModel(InputStream inputStream,
      URI destinationModelURI,
      ModelExpression modelExpression) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("SET-MODEL " + destinationModelURI + " to " + modelExpression +
          " from " + inputStream);
    }
    // Validate parameters
    if (destinationModelURI == null) {
      throw new IllegalArgumentException("Null 'destinationModelURI' parameter");
    } else if (modelExpression == null) {
      throw new IllegalArgumentException("Null 'modelExpression' parameter");
    }

    // Convert the model expression into the source model URI
    if (!(modelExpression instanceof ModelResource)) {
      throw new QueryException("Unsupported model expression " +
          modelExpression + " (" + modelExpression.getClass() + ")");
    }
    assert modelExpression instanceof ModelResource;

    URI sourceModelURI = ((ModelResource)modelExpression).getURI();
    assert sourceModelURI != null;

    // Perform the operation
    SetModelOperation op = new SetModelOperation(sourceModelURI, destinationModelURI,
                                  inputStream, contentHandlers, this);
    // preExcecute is a rather ugly hack, get rid of it once we support re-entrant transactions.
    if (op.preExecute()) {
      execute(op, "Unable to load " + sourceModelURI + " into " + destinationModelURI);
    }

    return op.getStatementCount();
  }

  /**
   * {@inheritDoc}
   */
  public RulesRef buildRules(URI ruleModel, URI baseModel, URI destModel) throws QueryException, org.mulgara.rules.InitializerException, java.rmi.RemoteException {
    if (logger.isInfoEnabled()) {
      logger.info("BUILD RULES: " + ruleModel);
    }

    // Set up the rule parser
    RuleLoader ruleLoader = RuleLoaderFactory.newRuleLoader(ruleLoaderClassName, ruleModel, baseModel, destModel);
    if (ruleLoader == null) {
      throw new org.mulgara.rules.InitializerException("No rule loader available");
    }

    // read in the rules
    Rules rules =  ruleLoader.readRules(this, metadata.getSystemModelURI());
    return new RulesRefImpl(rules);
  }

  /**
   * {@inheritDoc}
   */
  public void applyRules(RulesRef rulesRef) throws RulesException, java.rmi.RemoteException {
    Rules rules = rulesRef.getRules();
    rules.run(this);
  }


  public void setAutoCommit(boolean autoCommit) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("setAutoCommit(" + autoCommit + ") called.");
    }
    try {
      transactionManager.setAutoCommit(this, autoCommit);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error setting autocommit", em);
    }
  }

  public void commit() throws QueryException {
    logger.info("Committing transaction");
    try {
      transactionManager.commit(this);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error performing commit", em);
    }
  }

  public void rollback() throws QueryException {
    logger.info("Rollback transaction");
    try {
      transactionManager.rollback(this);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error performing rollback", em);
    }
  }

  public void close() throws QueryException {
    logger.info("Closing session");
    try {
      transactionManager.terminateCurrentTransactions(this);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error closing session. Forced close required", em);
    }
  }

  public boolean isLocal() {
    return true;
  }

  public void login(URI securityDomain, String user, char[] password) {
    if (logger.isDebugEnabled()) {
      logger.debug("Login of " + user + " to " + securityDomain);
    }

    if (securityDomain.equals(metadata.getSecurityDomainURI())) {
      // Propagate the login event to the security adapters
      for (Iterator i = securityAdapterList.iterator(); i.hasNext();) {
        ((SecurityAdapter) i.next()).login(user, password);
      }
    }
  }

  /**
   * Backup all the data on the specified server to a URI or an output stream.
   * The database is not changed by this method.
   *
   * If an outputstream is supplied then the destinationURI is ignored.
   *
   * @param outputStream Optional output stream to receive the contents
   * @param serverURI The URI of the server to backup.
   * @param destinationURI Option URI of the file to backup into.
   * @throws QueryException if the backup cannot be completed.
   */
  private synchronized void backup(OutputStream outputStream, URI serverURI, URI destinationURI)
      throws QueryException {
    execute(
        new BackupOperation(outputStream, serverURI, destinationURI),
        "Unable to backup to " + destinationURI);
  }

  //
  // Internal utility methods.
  //
  protected void modify(URI modelURI, Set statements, boolean insert) throws QueryException
  {
    if (logger.isInfoEnabled()) {
      logger.info("Inserting statements into " + modelURI);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Inserting statements: " + statements);
    }

    execute(new ModifyModelOperation(modelURI, statements, insert),
            "Could not commit insert");
  }

  private void modify(URI modelURI, Query query, boolean insert) throws QueryException
  {
    if (logger.isInfoEnabled()) {
      logger.info("INSERT QUERY: " + query + " into " + modelURI);
    }

    execute(new ModifyModelOperation(modelURI, query, insert, this),
            "Unable to modify " + modelURI);
  }

  /**
   * Execute an {@link Operation}.
   *
   * @param operation  the {@link Operation} to execute
   * @throws QueryException if the <var>operation</var> fails
   */
  private void execute(Operation operation, String errorString) throws QueryException
  {
    try {
      MulgaraTransaction transaction =
          transactionManager.getTransaction(this, operation.isWriteOperation());
      transaction.execute(operation, resolverSessionFactory, metadata);
    } catch (MulgaraTransactionException em) {
      logger.info("Error executing operation: " + errorString, em);
      throw new QueryException(errorString, em);
    }
  }

  public DatabaseOperationContext newOperationContext(boolean writing) throws QueryException {
    return new DatabaseOperationContext(
        cachedResolverFactorySet,
        externalResolverFactoryMap,
        internalResolverFactoryMap,
        metadata,
        securityAdapterList,
        temporaryModelTypeURI,
        temporaryResolverFactory,
        symbolicTransformationList,
        systemResolverFactory,
        writing);
  }
}
