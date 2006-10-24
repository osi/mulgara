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
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

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
import org.mulgara.resolver.view.SessionView;
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
class DatabaseSession implements Session, LocalSession, SessionView, AnswerDatabaseSession {
  public static final boolean ASSERT_STATEMENTS = true;
  public static final boolean DENY_STATEMENTS = false;

  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(DatabaseSession.class.getName());

  /** Logger for {@link SymbolicTransformation} plugins. */
  private static final Logger symbolicLogger =
    Logger.getLogger(DatabaseSession.class.getName() + "#symbolic");

  private static DatabaseSession writeSession = null;
  private boolean writing;

  /**
   * The models from external resolvers which have been cached as temporary
   * models.
   *
   * Every model in this set can be manipulated by resolvers from the
   * {@link #temporaryResolverFactory}.
   */
  private final Set cachedModelSet = new HashSet();

  /**
   * Resolver factories that should be have access to their models cached.
   *
   * This field is read-only.
   */
  private final Set cachedResolverFactorySet;

  /**
   * The models from external resolvers which have been cached as temporary
   * models and modified.
   *
   * Every model in this set can be manipulated by resolvers from the
   * {@link #temporaryResolverFactory}.
   */
  private final Set changedCachedModelSet = new HashSet();

  /**
   * The list of all registered {@link ResolverFactory} instances.
   */
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

  private final DatabaseOperationContext operationContext;

  /** Security adapters this instance should enforce. */
  private final List securityAdapterList;

  /** Symbolic transformations this instance should apply. */
  private final List symbolicTransformationList;

  /** Persistent string pool. */
  private final ResolverSessionFactory resolverSessionFactory;

  /** Factory used to obtain the SystemResolver */
  private final SystemResolverFactory systemResolverFactory;

  /** Resolver used for accessing the system model (<code>#</code>).  */
  protected SystemResolver systemResolver;

  /** Factory used to obtain the SystemResolver */
  private final ResolverFactory temporaryResolverFactory;

  /** Source of transactions.  */
  private final TransactionManager transactionManager;

  /** Session transaction */
  private Transaction transaction;

  /** The name of the rule loader to use */
  private String ruleLoaderClassName;

  /** A fallback rule loader */
  private static final String DUMMY_RULE_LOADER = "org.mulgara.rules.DummyRuleLoader";

  private int opState;
  private static final int UNINIT = 0;
  private static final int BEGIN = 1;
  private static final int RESUME = 2;
  private static final int FINISH = 3;
  private static final String[] opStates = {
      "UNINIT", "BEGIN", "RESUME", "FINISH", };

  private final Map enlistedResolverMap;

  private Set outstandingAnswers;

  /**
   * Whether each method call of the {@link Session} interface should
   * implicitly have a transaction created for it and be performed within that
   * transaction.
   *
   * This defaults to <code>true</code> until modified by the
   * {@link #setAutoCommit} method.
   */
  private boolean autoCommit = true;
  private boolean inFailedTransaction = false;

  /**
   * If a transaction is marked for rollback by the
   * {@link #rollbackTransactionalBlock} method, this field holds the exception
   * that caused the rollback so that the {@link #endTransactionalBlock} method
   * can add it as the cause of the {@link RollbackException} it will
   * subsequently throw.
   */
  private Throwable rollbackCause = null;

  private boolean explicitRollback = false;

  /**
   * The registered {@link ContentHandler} instances.
   */
  private ContentHandlerManager contentHandlers;

  //
  // Constructor
  //

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
  DatabaseSession(TransactionManager transactionManager,
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
      throw new IllegalArgumentException("Null \"transactionManager\" parameter");
    } else if (securityAdapterList == null) {
      throw new IllegalArgumentException("Null \"securityAdapterList\" parameter");
    } else if (symbolicTransformationList == null) {
      throw new IllegalArgumentException("Null \"symbolicTransformationList\" parameter");
    } else if (resolverSessionFactory == null) {
      throw new IllegalArgumentException("Null \"resolverSessionFactory\" parameter");
    } else if (systemResolverFactory == null) {
      throw new IllegalArgumentException("Null \"systemResolverFactory\" parameter");
    } else if (temporaryResolverFactory == null) {
      throw new IllegalArgumentException("Null \"temporaryResolverFactory\" parameter");
    } else if (resolverFactoryList == null) {
      throw new IllegalArgumentException("Null \"resolverFactoryList\" parameter");
    } else if (externalResolverFactoryMap == null) {
      throw new IllegalArgumentException("Null \"externalResolverFactoryMap\" parameter");
    } else if (internalResolverFactoryMap == null) {
      throw new IllegalArgumentException("Null \"internalResolverFactoryMap\" parameter");
    } else if (contentHandlers == null) {
      throw new IllegalArgumentException("Null \"contentHandlers\" parameter");
    } else if (metadata == null) {
      throw new IllegalArgumentException("Null \"metadata\" parameter");
    } else if (ruleLoaderClassName == null) {
      ruleLoaderClassName = DUMMY_RULE_LOADER;
    }

    if (cachedResolverFactorySet == null) {
      throw new IllegalArgumentException(
        "Null \"cachedResolverFactorySet\" parameter"
      );
    }

    if (temporaryModelTypeURI == null) {
      throw new IllegalArgumentException(
        "Null \"temporaryModelTypeURI\" parameter"
      );
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
    this.ruleLoaderClassName        = ruleLoaderClassName;

    this.outstandingAnswers         = new HashSet();
    this.transaction                = null;
    this.enlistedResolverMap        = new HashMap();
    this.opState                    = FINISH;
    this.operationContext           = new DatabaseOperationContext(
                                        cachedModelSet,
                                        cachedResolverFactorySet,
                                        changedCachedModelSet,
                                        this,
                                        enlistedResolverMap,
                                        externalResolverFactoryMap,
                                        internalResolverFactoryMap,
                                        metadata,
                                        securityAdapterList,
                                        temporaryModelTypeURI,
                                        temporaryResolverFactory,
                                        transactionManager
                                      );

    if (logger.isDebugEnabled()) {
      logger.debug("Constructed DatabaseSession");
    }

    // Set the transaction timeout to an hour
    try {
      transactionManager.setTransactionTimeout(3600);
    } catch (SystemException e) {
      logger.warn("Unable to set transaction timeout to 3600s", e);
    }
  }

  long bootstrapSystemModel(DatabaseMetadataImpl metadata) throws
      QueryException {
    logger.info("Bootstrapping System Model");
    // Validate parameters
    if (metadata == null) {
      throw new IllegalArgumentException("metadata null");
    }

    // Create the model
    systemResolver = beginTransactionalBlock(true);
    try {
      // Find the local node identifying the model
      long model = systemResolver.localizePersistent(
          new URIReferenceImpl(metadata.getSystemModelURI()));
      long rdfType = systemResolver.localizePersistent(
          new URIReferenceImpl(metadata.getRdfTypeURI()));
      long modelType = systemResolver.localizePersistent(
          new URIReferenceImpl(metadata.getSystemModelTypeURI()));

      // Use the session to create the model
      systemResolver.modifyModel(model, new SingletonStatements(model, rdfType,
          modelType), true);
      metadata.initializeSystemNodes(model, rdfType, modelType);

      long preSubject = systemResolver.localizePersistent(
          new URIReferenceImpl(metadata.getPreallocationSubjectURI()));
      long prePredicate = systemResolver.localizePersistent(
          new URIReferenceImpl(metadata.getPreallocationPredicateURI()));
      long preModel = systemResolver.localizePersistent(
          new URIReferenceImpl(metadata.getPreallocationModelURI()));

      // Every node cached by DatabaseMetadata must be preallocated
      systemResolver.modifyModel(preModel,
          new SingletonStatements(preSubject, prePredicate, model),
          true);
      systemResolver.modifyModel(preModel,
          new SingletonStatements(preSubject, prePredicate, rdfType),
          true);
      systemResolver.modifyModel(preModel,
          new SingletonStatements(preSubject, prePredicate, modelType),
          true);
      systemResolver.modifyModel(preModel,
          new SingletonStatements(preSubject, prePredicate, preSubject),
          true);
      systemResolver.modifyModel(preModel,
          new SingletonStatements(preSubject, prePredicate, prePredicate),
          true);
      systemResolver.modifyModel(preModel,
          new SingletonStatements(preSubject, prePredicate, preModel),
          true);

      metadata.initializePreallocationNodes(preSubject, prePredicate, preModel);

      systemResolverFactory.setDatabaseMetadata(metadata);

      return model;
    } catch (Throwable e) {
      rollbackTransactionalBlock(e);
      return -1; // Should be discarded by exception in endTransactionalBlock.
    } finally {
      endTransactionalBlock("Could not commit system model bootstrap");
    }
  }


  /**
   * Non-rule version of the constructor.  Accepts all parameters except ruleLoaderClassName.
   */
  DatabaseSession(TransactionManager transactionManager,
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

  public ResolverSession getResolverSession() {
    return systemResolver;
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
  public void backup(URI sourceURI,
      OutputStream outputStream) throws QueryException {
    this.backup(outputStream, sourceURI, null);
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
  private synchronized void backup(OutputStream outputStream,
      URI serverURI,
      URI destinationURI) throws QueryException {
    execute(
        new BackupOperation(outputStream, serverURI, destinationURI),
        "Unable to backup to " + destinationURI
        );
  }

  public void close() throws QueryException {
    logger.info("Closing session");
    if (!autoCommit) {
      logger.warn("Closing session while holding write-lock");

      try {
        resumeTransactionalBlock();
      } catch (Throwable th) {
        releaseWriteLock();
        throw new QueryException("Error while resuming transaction in close", th);
      }

      try {
        rollbackTransactionalBlock(
            new QueryException("Attempt to close session whilst in transaction"));
      } finally {
        endTransactionalBlock("Failed to release write-lock in close");
      }
    } else {
      if (this.transaction != null) {
        resumeTransactionalBlock();
        endPreviousQueryTransaction();
      }
    }
  }

  public void commit() throws QueryException {
    logger.info("Committing transaction");
    if (!autoCommit) {
      synchronized (DatabaseSession.class) {
        setAutoCommit(true);
        setAutoCommit(false);
      }
    }
  }

  public void createModel(URI modelURI, URI modelTypeURI) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("Creating Model " + modelURI + " with type " + modelTypeURI);
    }

    execute(new CreateModelOperation(modelURI, modelTypeURI),
            "Could not commit creation of model " + modelURI + " of type " +
              modelTypeURI);
  }

  public void delete(URI modelURI, Set statements) throws QueryException {
    modify(modelURI, statements, DENY_STATEMENTS);
  }

  public void delete(URI modelURI, Query query) throws QueryException {
    modify(modelURI, query, DENY_STATEMENTS);
  }

  public void insert(URI modelURI, Set statements) throws QueryException {
    modify(modelURI, statements, ASSERT_STATEMENTS);
  }

  public void insert(URI modelURI, Query query) throws QueryException {
    modify(modelURI, query, ASSERT_STATEMENTS);
  }

  protected void modify(URI modelURI, Set statements, boolean insert)
    throws QueryException
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

  private void modify(URI modelURI, Query query,
      boolean insert) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("INSERT QUERY: " + query + " into " + modelURI);
    }

    execute(new ModifyModelOperation(modelURI, query, insert, this),
            "Unable to modify " + modelURI);
  }

  protected void doModify(SystemResolver systemResolver, URI modelURI,
      Statements statements, boolean insert) throws Throwable {
    long model = systemResolver.localize(new URIReferenceImpl(modelURI));
    model = operationContext.getCanonicalModel(model);

    // Make sure security adapters are satisfied
    for (Iterator i = securityAdapterList.iterator(); i.hasNext(); ) {
      SecurityAdapter securityAdapter = (SecurityAdapter) i.next();

      // Lie to the user
      if (!securityAdapter.canSeeModel(model, systemResolver)) {
        throw new QueryException("No such model " + modelURI);
      }

      // Tell the truth to the user
      if (!securityAdapter.canModifyModel(model, systemResolver)) {
        throw new QueryException("You aren't allowed to modify " + modelURI);
      }
    }

    // Obtain a resolver for the destination model type
    Resolver resolver = operationContext.obtainResolver(
                          operationContext.findModelResolverFactory(model),
                          systemResolver
                        );
    assert resolver != null;

    if (logger.isDebugEnabled()) {
      logger.debug("Modifying " + modelURI + " using " + resolver);
    }

    resolver.modifyModel(model, statements, insert);

    if (logger.isDebugEnabled()) {
      logger.debug("Modified " + modelURI);
    }
  }

  public void login(URI securityDomain, String user, char[] password) {
    if (logger.isDebugEnabled()) {
      logger.debug("Login of " + user + " to " + securityDomain);
    }

    /*
    execute(new LoginOperation(securityDomain, user, password),
            "Unable to login " + user + " to " + securityDomain);
    */
    if (securityDomain.equals(metadata.getSecurityDomainURI())) {
      // Propagate the login event to the security adapters
      for (Iterator i = securityAdapterList.iterator(); i.hasNext();) {
        ((SecurityAdapter) i.next()).login(user, password);
      }
    }
  }

  public Answer query(Query query) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("QUERY: " + query);
    }

    // Evaluate the query
    QueryOperation queryOperation = new QueryOperation(query, this);
    executeQuery(queryOperation);
    return queryOperation.getAnswer();
  }

  public Answer innerQuery(Query query) throws QueryException {
    // Validate "query" parameter
    if (query == null) {
      throw new IllegalArgumentException("Null \"query\" parameter");
    }

    if (logger.isInfoEnabled()) {
      logger.info("Query: " + query);
    }

    boolean resumed;

    if (this.transaction != null) {
      resumeTransactionalBlock();
      resumed = true;
    } else {
      resumed = false;
    }

    Answer result = null;
    try {
      result = doQuery(this, systemResolver, query);
    } catch (Throwable th) {
      try {
        logger.warn("Inner Query failed", th);
        rollbackTransactionalBlock(th);
      } finally {
        endPreviousQueryTransaction();
        logger.error("Inner Query should have thrown exception", th);
        throw new IllegalStateException(
            "Inner Query should have thrown exception");
      }
    }

    try {
      if (resumed) {
        suspendTransactionalBlock();
      }

      return result;
    } catch (Throwable th) {
      endPreviousQueryTransaction();
      logger.error("Failed to suspend Transaction", th);
      throw new QueryException("Failed to suspend Transaction");
    }
  }

  Tuples innerCount(LocalQuery localQuery) throws QueryException {
    // Validate "query" parameter
    if (localQuery == null) {
      throw new IllegalArgumentException("Null \"query\" parameter");
    }

    if (logger.isInfoEnabled()) {
      logger.info("Inner Count: " + localQuery);
    }

    boolean resumed;

    if (this.transaction != null) {
      resumeTransactionalBlock();
      resumed = true;
    } else {
      resumed = false;
    }

    Tuples result = null;
    try {
      LocalQuery lq = (LocalQuery)localQuery.clone();
      transform(this, lq);
      result = lq.resolve();
      lq.close();
    } catch (Throwable th) {
      try {
        logger.warn("Inner Query failed", th);
        rollbackTransactionalBlock(th);
      } finally {
        endPreviousQueryTransaction();
        logger.error("Inner Query should have thrown exception", th);
        throw new IllegalStateException(
            "Inner Query should have thrown exception");
      }
    }

    try {
      if (resumed) {
        suspendTransactionalBlock();
      }

      return result;
    } catch (Throwable th) {
      endPreviousQueryTransaction();
      logger.error("Failed to suspend Transaction", th);
      throw new QueryException("Failed to suspend Transaction");
    }
  }

  static Answer doQuery(DatabaseSession databaseSession,
                        SystemResolver  systemResolver,
                        Query           query) throws Exception
  {
    Answer result;

    LocalQuery localQuery =
      new LocalQuery(query, systemResolver, databaseSession);

    transform(databaseSession, localQuery);

    // Complete the numerical phase of resolution
    Tuples tuples = localQuery.resolve();
    result = new SubqueryAnswer(databaseSession, systemResolver, tuples,
        query.getVariableList());
    tuples.close();
    localQuery.close();

    if (logger.isDebugEnabled()) {
      logger.debug("Answer rows = " + result.getRowCount());
    }
    return result;
  }


  /**
   *
   * Perform in-place transformation of localQuery.
   * Note: we really want to convert this to a functional form eventually.
   */
  private static void transform(DatabaseSession databaseSession, LocalQuery localQuery) throws Exception {
    // Start with the symbolic phase of resolution
    LocalQuery.MutableLocalQueryImpl mutableLocalQueryImpl =
      localQuery.new MutableLocalQueryImpl();
    if (symbolicLogger.isDebugEnabled()) {
      symbolicLogger.debug("Before transformation: " + mutableLocalQueryImpl);
    }
    Iterator i = databaseSession.symbolicTransformationList.iterator();
    while (i.hasNext()) {
      SymbolicTransformation symbolicTransformation =
        (SymbolicTransformation) i.next();
      assert symbolicTransformation != null;
      symbolicTransformation.transform(databaseSession.operationContext, mutableLocalQueryImpl);
      if (mutableLocalQueryImpl.isModified()) {
        // When a transformation succeeds, we rewind and start from the
        // beginning of the symbolicTransformationList again
        if (symbolicLogger.isDebugEnabled()) {
          symbolicLogger.debug("Symbolic transformation: " +
                               mutableLocalQueryImpl);
        }
        mutableLocalQueryImpl.close();
        mutableLocalQueryImpl = localQuery.new MutableLocalQueryImpl();
        i = databaseSession.symbolicTransformationList.iterator();
      }
    }
    mutableLocalQueryImpl.close();
  }

  private void endPreviousQueryTransaction() throws QueryException {
    logger.debug("Clearing previous transaction");

    // Save the exception.
    Throwable tmpThrowable = rollbackCause;

    Set answers = new HashSet(outstandingAnswers);
    Iterator i = answers.iterator();
    while (i.hasNext()) {
      try {
        SubqueryAnswer s = (SubqueryAnswer) i.next();
        deregisterAnswer(s);
        //Do not close tuples - for Jena and JRDF.
        //s.close();
      } catch (Throwable th) {
        logger.debug("Failed to close preexisting answer", th);
      }
    }

    // If by closing the answer we have called endTransactionalBlock then
    // throw the saved exception.
    if ((rollbackCause == null) && (tmpThrowable != null) &&
        (systemResolver == null)) {
      throw new QueryException("Failure ending previous query", tmpThrowable);
    }

    try {
      if (!outstandingAnswers.isEmpty()) {
        throw new QueryException("Failed to clear preexisting transaction");
      }
      if (this.transaction != null) {
        throw new QueryException("Failed to void suspended transaction");
      }
      if (transactionManager.getTransaction() != null) {
        throw new QueryException("Failed to end transaction");
      }
    } catch (QueryException eq) {
      endTransactionalBlock("Error ending previous query");
      throw eq;
    } catch (Throwable th) {
      endTransactionalBlock("Error ending previous query");
      throw new QueryException("Failure ending previous query", th);
    }
  }

  public void registerAnswer(SubqueryAnswer answer) {
    if (logger.isDebugEnabled()) {
      logger.debug("registering Answer: " + System.identityHashCode(answer));
    }
    outstandingAnswers.add(answer);
  }

  public void deregisterAnswer(SubqueryAnswer answer) throws QueryException {
    if (logger.isDebugEnabled()) {
      logger.debug("deregistering Answer: " + System.identityHashCode(answer));
    }

    if (!outstandingAnswers.contains(answer)) {
      logger.info("Stale answer being closed");
    } else {
      outstandingAnswers.remove(answer);
      if (autoCommit && outstandingAnswers.isEmpty()) {
        if (transaction != null) {
          resumeTransactionalBlock();
        }
        endTransactionalBlock("Could not commit query");
      }
    }
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
    executeQuery(queryOperation);
    return queryOperation.getAnswerList();
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


  public void removeModel(URI modelURI) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("REMOVE MODEL: " + modelURI);
    }
    // Validate "modelURI" parameter
    if (modelURI == null) {
      throw new IllegalArgumentException("Null \"modelURI\" parameter");
    }

    execute(new RemoveModelOperation(modelURI), "Unable to remove " + modelURI);
  }

  public boolean modelExists(URI modelURI) throws QueryException {

    // Attempt to create a read only transaction
    try {
      startTransactionalOperation(false);
    }
    catch (IllegalArgumentException iae) {

      // If we're in a transaction then try to do it anyway.
      try {
        long model = systemResolver.lookupPersistent(new URIReferenceImpl(
            modelURI));

        return systemResolver.modelExists(model);
      }
      catch (ResolverException re) {
        throw new QueryException("Failed to resolve URI: " + modelURI, re);
      }
      catch (LocalizeException le) {
        // Return false - we failed to find the node.
        return false;
      }
    }

    // If we created a new transaction perform the same operation with
    // try/catches for correct handling of exceptions.
    try {
      long model;

      try {
        model = systemResolver.lookupPersistent(new URIReferenceImpl(
            modelURI));
      }
      catch (LocalizeException le) {
        // Return false - we failed to find the node.
        return false;
      }
      return systemResolver.modelExists(model);

    } catch (Throwable e) {
      rollbackTransactionalBlock(e);
    } finally {
      finishTransactionalOperation("Could not find model " + modelURI);
    }
    throw new QueryException("Illegal transactional state in session");
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

  public void rollback() throws QueryException {
    logger.info("Rollback transaction");
    if (autoCommit) {
      throw new QueryException(
          "Attempt to rollback transaction outside transaction");
    }
    resumeTransactionalBlock();
    try {
      explicitRollback = true;
      rollbackTransactionalBlock(new QueryException(
          "Explicit rollback requested on session"));
    } catch (Throwable th) {
      logger.error("Failed to rollback transaction", th);
      throw new QueryException("Rollback failed", th);
    } finally {
      synchronized (DatabaseSession.class) {
        try {
          endTransactionalBlock("Rollback failed, ending transaction");
        } catch (Throwable th) {
          throw new QueryException("Rollback failed", th);
        }
        setAutoCommit(false);
      }
    }
  }

  public void setAutoCommit(boolean autoCommit) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("setAutoCommit(" + autoCommit + ") called with autoCommit = " + this.autoCommit);
    }

    if (!this.autoCommit && autoCommit) { // Turning autoCommit on
      try {
        resumeTransactionalBlock();
      } finally {
        this.autoCommit = true;
        this.inFailedTransaction = false;
        endTransactionalBlock("Extended transaction failed");
      }
    } else if (this.autoCommit && !autoCommit) { // Turning autoCommit off
      if (this.transaction != null) {
        resumeTransactionalBlock();
        endPreviousQueryTransaction();
      }
      systemResolver = beginTransactionalBlock(true);
      try {
        suspendTransactionalBlock();
      } catch (Throwable th) {
        logger.error("Failed to suspend transaction", th);
        rollbackTransactionalBlock(th);
        endTransactionalBlock("Could set auto Commit off");
      }
      this.autoCommit = false;
    } else if (this.inFailedTransaction) { // Reset after failed autoCommit off based transaction.
      this.inFailedTransaction = false;
    } else { // Leaving autoCommit the same
      if (logger.isInfoEnabled()) {
        logger.info("Invalid call to setAutoCommit(" + autoCommit + ") called with autoCommit = " + this.autoCommit);
      }
    }
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
    }
    if (modelExpression == null) {
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

  //
  // Local query methods
  //

  /**
   * Resolve a localized constraint into the tuples which satisfy it.
   *
   * This method must be called within a transactional context.
   *
   * @deprecated Will be made package-scope as soon as the View kludge is resolved.
   * @param constraint  a localized constraint
   * @return the tuples satisfying the <var>constraint</var>
   * @throws IllegalArgumentException if <var>constraint</var> is
   *   <code>null</code>
   * @throws QueryException if the <var>constraint</var> can't be resolved
   */
  public Tuples resolve(Constraint constraint) throws QueryException {
    if (logger.isDebugEnabled()) {
      logger.debug("Resolving " + constraint);
    }

    // Validate "constraint" parameter
    if (constraint == null) {
      throw new IllegalArgumentException("Null \"constraint\" parameter");
    }

    ConstraintElement modelElem = constraint.getModel();
    if (modelElem instanceof Variable) {
      return resolveVariableModel(constraint);
    } else if (modelElem instanceof LocalNode) {
      long model = ((LocalNode) modelElem).getValue();
      long realModel = operationContext.getCanonicalModel(model);

      // Make sure security adapters are satisfied
      for (Iterator i = securityAdapterList.iterator(); i.hasNext();) {
        SecurityAdapter securityAdapter = (SecurityAdapter) i.next();

        // Lie to the user
        if (!securityAdapter.canSeeModel(realModel, systemResolver))
        {
          try {
            throw new QueryException(
              "No such model " + systemResolver.globalize(realModel)
            );
          }
          catch (GlobalizeException e) {
            logger.warn("Unable to globalize model " + realModel);
            throw new QueryException("No such model");
          }
        }
      }
      for (Iterator i = securityAdapterList.iterator(); i.hasNext();) {
        SecurityAdapter securityAdapter = (SecurityAdapter) i.next();

        // Tell a different lie to the user
        if (!securityAdapter.canResolve(realModel, systemResolver))
        {
          return TuplesOperations.empty();
        }
      }

      // if the model was changed then update the constraint
      if (model != realModel) {
        constraint = ConstraintOperations.rewriteConstraintModel(new LocalNode(realModel), constraint);
      }

      // Evaluate the constraint
      Tuples result = operationContext.obtainResolver(
                        operationContext.findModelResolverFactory(realModel),
                        systemResolver
                      ).resolve(constraint);
      assert result != null;

      return result;
    } else {
      throw new QueryException("Non-localized model in resolve: " + modelElem);
    }
  }

  /**
  * Resolve a {@link Constraint} in the case where the model isn't fixed.
  *
  * This is mostly relevant in the case where the <code>in</code> clause takes
  * a variable parameter.  It's tricky to resolve because external models may
  * be accessible to the system, but aren't known to it unless they're named.
  * The policy we take is to only consider internal models.
  *
  * @param constraint  a constraint with a {@link Variable}-valued model
  *   element, never <code>null</code>
  * @return the solutions to the <var>constraint</var> occurring in all
  *   internal models, never <code>null</code>
  * @throws QueryException if the solution can't be evaluated
  */
  private Tuples resolveVariableModel(Constraint constraint)
    throws QueryException
  {
    assert constraint != null;
    assert constraint.getElement(3) instanceof Variable;

    Tuples tuples = TuplesOperations.empty();

    // This is the alternate code we'd use if we were to consult external
    // models as well as internal models during the resolution of variable IN
    // clauses:
    //
    //Iterator i = resolverFactoryList.iterator();

    Iterator i = internalResolverFactoryMap.values().iterator();
    while (i.hasNext()) {
      ResolverFactory resolverFactory = (ResolverFactory) i.next();
      assert resolverFactory != null;

      // Resolve the constraint
      Resolver resolver = operationContext.obtainResolver(resolverFactory, systemResolver);
      if (logger.isDebugEnabled()) {
        logger.debug("Resolving " + constraint + " against " + resolver);
      }
      Resolution resolution = resolver.resolve(constraint);
      assert resolution != null;

      try {
        // If this is a complete resolution of the constraint, we won't have to
        // consider any of the other resolvers
        if (resolution.isComplete()) {
          if (logger.isDebugEnabled()) {
            logger.debug("Returning complete resolution from " + resolver);
          }
          tuples.close();

          return resolution;
        } else {
          // Append the resolution to the overall solutions
          if (logger.isDebugEnabled()) {
            logger.debug("Appending " + resolver);
          }
          Tuples oldTuples = tuples;
          tuples = TuplesOperations.append(tuples, resolution);
          oldTuples.close();
        }
      } catch (TuplesException e) {
        throw new QueryException("Unable to resolve " + constraint, e);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("Resolved " + constraint + " to " +
          TuplesOperations.formatTuplesTree(tuples));
    }

    return tuples;
  }

  //
  // Internal methods
  //

  /**
   * Mark the beginning of a transactional block.
   *
   * This begins a transaction if {@link #autoCommit} is <code>true</code>.
   *
   * @throws QueryException if a transaction needed to be begun and couldn't be
   */
  private SystemResolver beginTransactionalBlock(boolean allowWrites) throws
      QueryException {
    if (logger.isInfoEnabled()) {
      logger.info("Beginning transactional block: autocommit = " + autoCommit);
    }

    // Start the transaction
    if (inFailedTransaction == true) {
      throw new IllegalStateException("Transaction already failed, set autocommit true to reset");
    } else if (!enlistedResolverMap.isEmpty()) {
      throw new QueryException("Stale resolvers found in enlistedResolverMap");
    }


    if (allowWrites) {
      try {
        obtainWriteLock();
      } catch (InterruptedException ei) {
        throw new QueryException("Unable to obtain write lock", ei);
      }
    }

    try {
      transactionManager.begin();
      if (systemResolver != null) {
        throw new QueryException("beginning nested transaction");
      }
      systemResolver = systemResolverFactory.newResolver(allowWrites);
      return (SystemResolver) operationContext.enlistResolver(systemResolver);
    } catch (Exception e) {
      throw new QueryException("Unable to begin transaction", e);
    }
  }

  /**
   * Execute an {@link Operation}.
   *
   * @param operation  the {@link Operation} to execute
   * @param failureMessage  text to appear as the exception message if the
   *   <var>operation</var> fails
   * @throws QueryException if the <var>operation</var> fails
   */
  private void execute(Operation operation, String failureMessage)
    throws QueryException
  {
    assert operation != null;

    startTransactionalOperation(operation.isWriteOperation());

    assert systemResolver != null;
    try {
      operation.execute(operationContext, systemResolver, resolverSessionFactory, metadata);
    } catch (Throwable e) {
      rollbackTransactionalBlock(e);
    } finally {
      finishTransactionalOperation(failureMessage);
    }
  }

  /**
   * Execute an {@link Operation}.
   *
   * @param operation  the {@link Operation} to execute
   * @throws QueryException if the <var>operation</var> fails
   */
  private void executeQuery(Operation operation) throws QueryException
  {
    /*
     * Transaction semantics:
     * AC && Suspended  -> R clr E B . S
     * AC && !Suspended -> B . S
     * !AC              -> R clr . S
     */
    if (autoCommit) {
      if (this.transaction != null) {
        resumeTransactionalBlock();
        endPreviousQueryTransaction();
      }
      beginTransactionalBlock(operation.isWriteOperation());
    }
    else {
      resumeTransactionalBlock();
    }

    try {
      operation.execute(operationContext,
                        systemResolver,
                        resolverSessionFactory,
                        metadata);
    }
    catch (Throwable th) {
      try {
        logger.warn("Query failed", th);
        rollbackTransactionalBlock(th);
      } finally {
        endPreviousQueryTransaction();
        throw new QueryException("Failed to rollback failed transaction", th);
      }
    }

    try {
      suspendTransactionalBlock();
    } catch (Throwable th) {
      endPreviousQueryTransaction();
      logger.error("Query should have thrown exception", th);
      throw new IllegalStateException("Query should have thrown exception");
    }
  }

  private void obtainWriteLock() throws InterruptedException {
    logger.info("Trying to obtain write lock. ");
    synchronized (DatabaseSession.class) {
      if (DatabaseSession.writeSession == this) {
        return;
      }
      while (DatabaseSession.writeSession != null) {
        DatabaseSession.class.wait();
      }
      DatabaseSession.writeSession = this;
      this.writing = true;
      logger.info("Obtained write lock. ");
    }
  }

  private void releaseWriteLock() {
    synchronized (DatabaseSession.class) {
      if (DatabaseSession.writeSession == this) {
        logger.info("Releasing write lock");
        DatabaseSession.writeSession = null;
        this.writing = false;
        DatabaseSession.class.notifyAll();
      }
    }
  }

  /**
   * Mark the end of a transactional block.
   *
   * This commits the current transaction if {@link #autoCommit} is
   * <code>true</code>.
   *
   * @throws QueryException if a transaction needed to be committed and
   *   couldn't be
   */
  private void endTransactionalBlock(String failureMessage) throws QueryException {
    if (logger.isInfoEnabled()) {
      logger.info(
        "End Transactional Block autocommit=" + autoCommit +
        " transaction status=" + StatusFormat.formatStatus(transactionManager)
      );
    }

    try {
      // Commit the transaction
      if (rollbackCause == null) {
        transactionManager.commit();
      } else {
        try {
          transactionManager.commit();
        } catch (RollbackException e) {
          // Sneakily reinsert the exception recorded earlier by the
          // rollbackTransactionalBlock method.  Without this feature, it's
          // very difficult to determine why a rollback occurred.
          e.initCause(rollbackCause);
          throw e;
        } finally {
          rollbackCause = null;
        }
      }
    } catch (Exception e) {
      if (!explicitRollback) {
        throw new QueryException(failureMessage, e);
      }
    } finally {
      releaseWriteLock();
      enlistedResolverMap.clear();
      outstandingAnswers.clear();
      clearCache();
      operationContext.clearSystemModelCache();

      systemResolver = null;
      explicitRollback = false;
      autoCommit = true;
    }
  }

  /**
   * Clear the cache of temporary models.
   */
  private void clearCache()
  {
    // Clear the temporary models
    if (!cachedModelSet.isEmpty()) {
      try {
        Resolver temporaryResolver =
          temporaryResolverFactory.newResolver(true,
                                               systemResolver,
                                               systemResolver);
        for (Iterator i = cachedModelSet.iterator(); i.hasNext();) {
          LocalNode modelLocalNode = (LocalNode) i.next();
          long model = modelLocalNode.getValue();

          if (changedCachedModelSet.contains(modelLocalNode)) {
            // Write back the modifications to the original model
            try {
              Resolver resolver =
                findResolverFactory(model).newResolver(true,
                                                       systemResolver,
                                                       systemResolver);
              Variable s = new Variable("s"),
                       p = new Variable("p"),
                       o = new Variable("o");
              resolver.modifyModel(
                model,
                new TuplesWrapperStatements(
                  temporaryResolver.resolve(
                    new ConstraintImpl(s, p, o, modelLocalNode)
                  ),
                  s, p, o
                ),
                true  // insert the content
              );
            }
            catch (Exception e) {
              logger.error("Failed to write back cached model " + model +
                           " after transaction", e);
            }
            changedCachedModelSet.remove(modelLocalNode);
          }

          // Remove the cached model
          try {
            temporaryResolver.removeModel(model);
          }
          catch (Exception e) {
            logger.error(
              "Failed to clear cached model " + model + " after transaction",
               e
            );
          }
          i.remove();
        }
      }
      catch (Exception e) {
        logger.error("Failed to clear cached models after transaction", e);
      }
    }
  }

  /**
   * Find a cached resolver factory for write back.
   *
   * @return a completely unwrapped resolver factory
   */
  // TODO: Common code with DatabaseOperationContent.findModelResolverFactory
  //       should be consolidated.
  private ResolverFactory findResolverFactory(long model) throws QueryException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Finding raw resolver factory for model " + model);
    }

    try {
      // get the model URI
      Node modelNode = systemResolver.globalize(model);
      if (!(modelNode instanceof URIReference)) {
        throw new QueryException(modelNode.toString() + " is not a valid Model");
      }
      URI modelURI = ((URIReference)modelNode).getURI();

      // test the model URI against the current server
      try {
        if (logger.isDebugEnabled()) {
          logger.debug("Comparing " + metadata.getURI().toString() + " to " + (new URI(modelURI.getScheme(),
                  modelURI.getSchemeSpecificPart(), null)).toString());
        }
        if (metadata.getURI().equals(new URI(modelURI.getScheme(), modelURI.getSchemeSpecificPart(), null))) {
          // should be on the current server, but was not found here
          throw new QueryException(modelNode.toString() + " is not a Model");
        }
      }
      catch (URISyntaxException use) {
        throw new QueryException("Internal error.  Model URI cannot be manipulated.");
      }

      // This is not a local model, get the protocol
      String modelProtocol = operationContext.findProtocol(model);
      if (logger.isDebugEnabled()) {
        logger.debug("Model " + model + " protocol is " + modelProtocol);
      }

      // find the factory for this protocol
      ResolverFactory resolverFactory =
          (ResolverFactory) externalResolverFactoryMap.get(modelProtocol);
      if (resolverFactory == null) {
        throw new QueryException(
            "Unsupported protocol for destination model (" +
            modelProtocol + ", " + model + " : '" + modelProtocol + "')");
      }

      return resolverFactory;
    }
    catch (GlobalizeException eg) {
      throw new QueryException("Unable to globalize modeltype", eg);
    }
  }

  /**
   * Mark the current transaction for rollback due to an exception.
   *
   * This records the exception which caused the rollback in the
   * {@link #rollbackCause} field.
   */
  public void rollbackTransactionalBlock(Throwable throwable) throws
      QueryException {
    logger.info("Rollback Transactional Block");
    assert throwable != null;

    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Marking transaction for rollback", throwable);
      }
      transactionManager.setRollbackOnly();
    } catch (Throwable e) {
      logger.error("Needed to mark transaction for rollback", throwable);
      logger.error("Unable to mark transaction for rollback", e);
      throw new QueryException("Unable to mark transaction for rollback", e);
    }

    rollbackCause = throwable;
  }

  /**
   * Suspends current transaction, storing it in session for latter resumption.
   *
   * @throws Throwable Must be called inside the try/catch(Throwable) block
   * protecting the transaction.
   */
  public void suspendTransactionalBlock() throws Throwable {
    logger.info("Suspend Transactional Block");
    if (transaction != null) {
      throw new IllegalStateException(
          "Attempt to suspend unresumed transaction.");
    }
    if (logger.isInfoEnabled()) {
      logger.info(
         "Suspend Transactional Block autocommit=" + autoCommit +
         " transaction status=" + StatusFormat.formatStatus(transactionManager)
      );
    }

    int status = transactionManager.getStatus();
    if (!autoCommit &&
        (status == Status.STATUS_MARKED_ROLLBACK ||
         status == Status.STATUS_ROLLEDBACK ||
         status == Status.STATUS_ROLLING_BACK)) {
      inFailedTransaction = true;
      throw new QueryException("Transaction marked for rollback");
    }

    this.transaction = transactionManager.suspend();
  }

  /**
   * Resumes the previously suspended transaction from the current session.
   *
   * @throws QueryException Must be called outside the try/catch(Throwable) block
   * protecting the transaction.
   */
  public void resumeTransactionalBlock() throws QueryException {
    logger.info("Resume Transactional Block");
    if (transaction == null) {
      throw new IllegalStateException("Attempt to resume unsuspended transaction");
    } else if (inFailedTransaction == true) {
      throw new IllegalStateException("Transaction already failed, set autocommit true to reset");
    }

    try {
      transactionManager.resume(this.transaction);
      this.transaction = null;
    } catch (Exception e) {
      logger.error("Resume failed", e);
      throw new QueryException("Failed to resume transaction", e);
    }
  }

  /**
   * Start's or resumes a transaction for an operation.
   *
   * Using start/finish TransactionalOperation ensures properly matched pairs of
   * begin/end and suspend/resume.
   */
  public void startTransactionalOperation(boolean needsWrite) throws
      QueryException {
    logger.info("Starting Transactional Operation");
    if (opState != FINISH) {
      throw new IllegalArgumentException(
          "Attempt to start transactional operation during: " +
          opStates[opState]);
    }
    if (autoCommit) {
      if (this.transaction != null) {
        resumeTransactionalBlock();
        endPreviousQueryTransaction();
      }
      beginTransactionalBlock(needsWrite);
      logger.info("BEGIN new transaction.");
      opState = BEGIN;
    } else {
      resumeTransactionalBlock();
      logger.info("RESUME old transaction.");
      opState = RESUME;
    }
  }

  /**
   * Ends's or suspends a transaction for an operation.
   *
   * Using start/finish TransactionalOperation ensures properly matched pairs of
   * begin/end and suspend/resume.
   */
  public void finishTransactionalOperation(String errorString) throws
      QueryException {
    logger.info("Finishing Transactional Operation");
    if (logger.isDebugEnabled()) {
      logger.debug("opState = " + opStates[opState]);
      logger.debug("autoCommit = " + autoCommit);
    }
    if (opState == FINISH) {
      throw new IllegalArgumentException(
          "Attempt to finish transactional operation during: " + opStates[opState]);
    }
    if (autoCommit) {
      try {
        endTransactionalBlock(errorString);
      } finally {
        logger.info("FINISH(end) implicit transaction.");
        opState = FINISH;
      }
    } else {
      try {
        suspendTransactionalBlock();
      } catch (Throwable th) {
        logger.error("Failed to suspend transaction", th);
        try {
          rollbackTransactionalBlock(new QueryException("Failed to suspend transaction"));
        } finally {
          endTransactionalBlock("Failed to suspend transaction at end of operation");
        }
      } finally {
        logger.info("FINISH(suspend) explicit transaction.");
        opState = FINISH;
      }
    }
  }

  public boolean isLocal() {
    return true;
  }

  //
  // Private accessors intended only for DatabaseOperationContext
  //

  SystemResolver getSystemResolver() {
    return systemResolver;
  }

  Transaction getTransaction() {
    return transaction;
  }

  boolean isWriting() {
    return writing;
  }
}
