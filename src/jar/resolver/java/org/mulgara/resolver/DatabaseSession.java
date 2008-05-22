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
 *   SymbolicTransformation refactor contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *   External XAResource contributed by Netymon Pty Ltd on behalf of Topaz
 *   Foundation under contract.
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
import java.net.URI;
import java.util.*;

// Java 2 enterprise packages
import javax.transaction.xa.XAResource;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.*;

// Local packages
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerManager;
import org.mulgara.query.*;
import org.mulgara.resolver.spi.*;
import org.mulgara.rules.*;
import org.mulgara.server.Session;
import org.mulgara.store.nodepool.NodePool;

/**
 * A database session.
 *
 * @created 2004-04-26
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class DatabaseSession implements Session {
  public static final boolean ASSERT_STATEMENTS = true;
  public static final boolean DENY_STATEMENTS = false;

  /** Logger.  */
  private static final Logger logger = Logger.getLogger(DatabaseSession.class.getName());

  /**
   * Resolver factories that should be have access to their models cached.
   * This field is read-only.
   */
  private final Set<ResolverFactory> cachedResolverFactorySet;

  /**
   * The list of all registered {@link ResolverFactory} instances.
   * Not used in this implementation.
   */
  @SuppressWarnings("unused")
  private final List<ResolverFactory> resolverFactoryList;

  /**
   * Map from URL protocol {@link String}s to the {@link ResolverFactory} which
   * handles external models using that protocol.
   */
  private final Map<String,ResolverFactory> externalResolverFactoryMap;

  /**
   * Map from modelType {@link LocalNode}s to the {@link ResolverFactory} which
   * handles that model type.
   */
  private final Map<URI,InternalResolverFactory> internalResolverFactoryMap;

  private final DatabaseMetadata metadata;

  /** Security adapters this instance should enforce. */
  private final List<SecurityAdapter> securityAdapterList;

  /** Symbolic transformations this instance should apply. */
  private final List<SymbolicTransformation> symbolicTransformationList;

  /** Persistent string pool. Not used, but passed in as a parameter. */
  @SuppressWarnings("unused")
  private final ResolverSessionFactory resolverSessionFactory;

  /** Factory used to obtain the SystemResolver */
  private final SystemResolverFactory systemResolverFactory;

  /** Factory used to obtain the SystemResolver */
  private final ResolverFactory temporaryResolverFactory;

  /** Source of transactions.  */
  private final MulgaraTransactionManager transactionManager;

  private MulgaraTransactionFactory transactionFactory;
  private MulgaraInternalTransactionFactory internalFactory;
  private MulgaraExternalTransactionFactory externalFactory;

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
      List<SecurityAdapter> securityAdapterList,
      List<SymbolicTransformation> symbolicTransformationList,
      ResolverSessionFactory resolverSessionFactory,
      SystemResolverFactory systemResolverFactory,
      ResolverFactory temporaryResolverFactory,
      List<ResolverFactory> resolverFactoryList,
      Map<String,ResolverFactory> externalResolverFactoryMap,
      Map<URI,InternalResolverFactory> internalResolverFactoryMap,
      DatabaseMetadata metadata,
      ContentHandlerManager contentHandlers,
      Set<ResolverFactory> cachedResolverFactorySet,
      URI temporaryModelTypeURI,
      String ruleLoaderClassName) throws ResolverFactoryException {

    if (logger.isDebugEnabled()) {
      logger.debug("Constructing DatabaseSession: externalResolverFactoryMap=" +
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

    this.transactionFactory = null;
    this.internalFactory = null;

    if (logger.isTraceEnabled()) logger.trace("Constructed DatabaseSession");

    // Set the transaction timeout to an hour
    transactionManager.setTransactionTimeout(3600);
  }


  /**
   * Non-rule version of the constructor.  Accepts all parameters except ruleLoaderClassName.
   */
  DatabaseSession(MulgaraTransactionManager transactionManager,
      List<SecurityAdapter> securityAdapterList,
      List<SymbolicTransformation> symbolicTransformationList,
      ResolverSessionFactory resolverSessionFactory,
      SystemResolverFactory systemResolverFactory,
      ResolverFactory temporaryResolverFactory,
      List<ResolverFactory> resolverFactoryList,
      Map<String,ResolverFactory> externalResolverFactoryMap,
      Map<URI,InternalResolverFactory> internalResolverFactoryMap,
      DatabaseMetadata metadata,
      ContentHandlerManager contentHandlers,
      Set<ResolverFactory> cachedResolverFactorySet,
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
    logger.debug("Bootstrapping System Model");
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

  public void insert(URI modelURI, Set<? extends Triple> statements) throws QueryException {
    modify(modelURI, statements, ASSERT_STATEMENTS);
  }


  public void insert(URI modelURI, Query query) throws QueryException {
    modify(modelURI, query, ASSERT_STATEMENTS);
  }


  public void delete(URI modelURI, Set<? extends Triple> statements) throws QueryException {
    modify(modelURI, statements, DENY_STATEMENTS);
  }


  public void delete(URI modelURI, Query query) throws QueryException {
    modify(modelURI, query, DENY_STATEMENTS);
  }


  /**
   * Backup all the data on the specified server. The database is not changed by this method.
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
   * @param inputStream a client supplied inputStream to obtain the restore
   *        content from. If null assume the sourceURI has been supplied.
   * @param serverURI The URI of the server to restore.
   * @param sourceURI The URI of the backup file to restore from.
   * @throws QueryException if the restore cannot be completed.
   */
  public void restore(InputStream inputStream, URI serverURI, URI sourceURI) throws QueryException {
    execute(new RestoreOperation(inputStream, serverURI, sourceURI), "Unable to restore from " + sourceURI);
    for (ResolverFactory resFactory: resolverFactoryList) {
      createDefaultGraphs(resFactory.getDefaultGraphs());
    }
  }


  public Answer query(Query query) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("QUERY: " + query);

    QueryOperation queryOperation = new QueryOperation(query, this);
    execute(queryOperation, "Query failed");
    return queryOperation.getAnswer();
  }


  public List<Answer> query(List<Query> queryList) throws QueryException {
    if (logger.isDebugEnabled()) {
      StringBuffer log = new StringBuffer("QUERYING LIST: ");
      for (int i = 0; i < queryList.size(); i++) log.append(queryList.get(i));
      logger.debug(log.toString());
    }

    QueryOperation queryOperation = new QueryOperation(queryList, this);
    execute(queryOperation, "Failed list query");
    return queryOperation.getAnswerList();
  }



  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("Creating Model " + modelURI + " with type " + modelTypeURI);

    execute(new CreateModelOperation(modelURI, modelTypeURI),
            "Could not commit creation of model " + modelURI + " of type " + modelTypeURI);
  }


  public boolean createDefaultGraph(URI modelURI, URI modelTypeURI) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("Creating Graph " + modelURI + " with type " + modelTypeURI + " in the system graph");

    CreateDefaultGraphOperation op = new CreateDefaultGraphOperation(modelURI, modelTypeURI);
    execute(op, "Could not commit creation of model " + modelURI + " of type " + modelTypeURI);
    return op.getResult();
  }


  public void removeModel(URI modelURI) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("REMOVE MODEL: " + modelURI);
    if (modelURI == null) throw new IllegalArgumentException("Null 'modelURI' parameter");

    execute(new RemoveModelOperation(modelURI), "Unable to remove " + modelURI);
  }


  public boolean modelExists(URI modelURI) throws QueryException {
    ModelExistsOperation operation = new ModelExistsOperation(modelURI);
    execute(operation, "Failed to determine model existence");
    return operation.getResult();
  }


  /**
   * Define the contents of a model.
   * @param uri the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return RETURNED VALUE TO DO
   * @throws QueryException if the model can't be modified
   */
  public synchronized long setModel(URI uri, ModelExpression modelExpression) throws QueryException {
    return this.setModel(null, uri, modelExpression);
  }


  /**
   * Define the contents of a model via an inputstream
   * @param inputStream a remote inputstream
   * @param destinationModelURI the {@link URI} of the model to be redefined
   * @param modelExpression the new content for the model
   * @return RETURNED VALUE TO DO
   * @throws QueryException if the model can't be modified
   */
  public synchronized long setModel(InputStream inputStream,
      URI destinationModelURI, ModelExpression modelExpression) throws QueryException {
    if (logger.isDebugEnabled()) {
      logger.debug("SET-MODEL " + destinationModelURI + " to " + modelExpression + " from " + inputStream);
    }

    // Validate parameters
    if (destinationModelURI == null) {
      throw new IllegalArgumentException("Null 'destinationModelURI' parameter");
    } else if (modelExpression == null) {
      throw new IllegalArgumentException("Null 'modelExpression' parameter");
    }

    // Convert the model expression into the source model URI
    if (!(modelExpression instanceof ModelResource)) {
      throw new QueryException("Unsupported model expression " + modelExpression + " (" + modelExpression.getClass() + ")");
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
  public RulesRef buildRules(URI ruleModel, URI baseModel, URI destModel) throws QueryException, org.mulgara.rules.InitializerException {
    if (logger.isDebugEnabled()) logger.debug("BUILD RULES: " + ruleModel);

    BuildRulesOperation operation = new BuildRulesOperation(ruleLoaderClassName, ruleModel, baseModel, destModel);
    execute(operation, "Failed to create rules");
    return operation.getResult();
  }


  /**
   * {@inheritDoc}
   */
  public void applyRules(RulesRef rulesRef) throws QueryException {
    execute(new ApplyRulesOperation(rulesRef), "Unable to apply rules");
  }


  public void setAutoCommit(boolean autoCommit) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("setAutoCommit(" + autoCommit + ") called.");
    assertInternallyManagedXA();
    try {
      internalFactory.setAutoCommit(this, autoCommit);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error setting autocommit", em);
    }
  }


  public void commit() throws QueryException {
    logger.debug("Committing transaction");
    assertInternallyManagedXA();
    try {
      internalFactory.commit(this);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error performing commit", em);
    }
  }


  public void rollback() throws QueryException {
    logger.debug("Rollback transaction");
    assertInternallyManagedXA();
    try {
      internalFactory.rollback(this);
    } catch (MulgaraTransactionException em) {
      throw new QueryException("Error performing rollback", em);
    }
  }


  public void close() throws QueryException {
    logger.debug("Closing session");
    try {
      transactionManager.closingSession(this);
      transactionFactory = null;
    } catch (MulgaraTransactionException em2) {
      logger.error("Error force-closing session", em2);
      throw new QueryException("Error force-closing session.", em2);
    }
  }


  public boolean isLocal() {
    return true;
  }


  public void login(URI securityDomain, String user, char[] password) {
    if (logger.isTraceEnabled()) logger.trace("Login of " + user + " to " + securityDomain);

    if (securityDomain.equals(metadata.getSecurityDomainURI())) {
      // Propagate the login event to the security adapters
      for (SecurityAdapter adapter: securityAdapterList) {
        adapter.login(user, password);
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
    execute(new BackupOperation(outputStream, serverURI, destinationURI),
        "Unable to backup to " + destinationURI);
  }


  //
  // Internal utility methods.
  //
  protected void modify(URI modelURI, Set<? extends Triple> statements, boolean insert) throws QueryException
  {
    if (logger.isDebugEnabled()) logger.debug("Modifying (ins:" + insert + ") : " + modelURI);
    if (logger.isTraceEnabled()) logger.trace("Modifying statements: " + statements);

    execute(new ModifyModelOperation(modelURI, statements, insert), "Could not commit modify");
  }


  private void modify(URI modelURI, Query query, boolean insert) throws QueryException {
    if (logger.isDebugEnabled()) {
      logger.debug((insert ? "INSERT" : "DELETE") + " QUERY: " + query + " into " + modelURI);
    }

    execute(new ModifyModelOperation(modelURI, query, insert, this), "Unable to modify " + modelURI);
  }


  /**
   * Execute an {@link Operation}.
   *
   * @param operation  the {@link Operation} to execute
   * @throws QueryException if the <var>operation</var> fails
   */
  private void execute(Operation operation, String errorString) throws QueryException {
    ensureTransactionFactorySelected();
    try {
      MulgaraTransaction transaction = transactionFactory.getTransaction(this, operation.isWriteOperation());
      transaction.execute(operation, metadata);
    } catch (MulgaraTransactionException em) {
      logger.debug("Error executing operation: " + errorString, em);
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

  /**
   * Creates a series of default graphs for a resolver.
   * @param graphs An array of the graph names and types to create. May be null.
   * @return <code>true</code> if any graphs were created, <code>false</code> otherwise.
   * @throws QueryException If it was not possible to detect if the graph already existed.
   */
  boolean createDefaultGraphs(ResolverFactory.Graph[] graphs) throws QueryException {
    boolean result = false;
    if (graphs != null) {
      for (ResolverFactory.Graph graph: graphs) {
        result = result || createDefaultGraph(graph.getGraph(), graph.getType());
      }
    }
    return result;
  }


  private void ensureTransactionFactorySelected() throws QueryException {
    if (transactionFactory == null) assertInternallyManagedXA();
  }

  private void assertInternallyManagedXA() throws QueryException {
    if (transactionFactory == null) {
      transactionFactory = internalFactory = transactionManager.getInternalFactory();
    } else if (internalFactory == null) {
      throw new QueryException("Attempt to use internal transaction control in externally managed session");
    }
  }


  private void assertExternallyManagedXA() throws QueryException {
    if (transactionFactory == null) {
      transactionFactory = externalFactory = transactionManager.getExternalFactory();
    } else if (externalFactory == null) {
      throw new QueryException("Attempt to use external transaction control in internally managed session");
    }
  }


  public XAResource getXAResource() throws QueryException {
    assertExternallyManagedXA();
    return externalFactory.getXAResource(this, true);
  }


  public XAResource getReadOnlyXAResource() throws QueryException {
    assertExternallyManagedXA();
    return externalFactory.getXAResource(this, false);
  }
  
  public boolean ping() {
    return true;
  }
}
