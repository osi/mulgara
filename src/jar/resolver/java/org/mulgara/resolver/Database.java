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

package org.mulgara.resolver;

// Java 2 standard packages
import gnu.trove.TIntHashSet;
import gnu.trove.TIntIterator;

import java.beans.Beans;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.apache.log4j.Logger;
import org.jrdf.vocabulary.RDF;
import org.mulgara.content.Content;
import org.mulgara.content.ContentHandler;
import org.mulgara.query.LocalNode;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.FactoryInitializer;
import org.mulgara.resolver.spi.InitializerException;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverException;
import org.mulgara.resolver.spi.ResolverFactory;
import org.mulgara.resolver.spi.ResolverFactoryException;
import org.mulgara.resolver.spi.SecurityAdapter;
import org.mulgara.resolver.spi.SecurityAdapterFactory;
import org.mulgara.resolver.spi.SecurityAdapterFactoryException;
import org.mulgara.resolver.spi.SymbolicTransformation;
import org.mulgara.resolver.spi.SystemResolverFactory;
import org.mulgara.rules.RuleLoader;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.nodepool.NodePoolException;
import org.mulgara.store.nodepool.NodePoolFactory;
import org.mulgara.store.stringpool.StringPool;
import org.mulgara.store.stringpool.StringPoolException;
import org.mulgara.store.stringpool.StringPoolFactory;
import org.mulgara.store.xa.SimpleXARecoveryHandler;
import org.mulgara.store.xa.SimpleXAResourceException;
import org.mulgara.transaction.TransactionManagerFactory;

/**
 * A database capable of managing and querying RDF models using a collection of
 * {@link ResolverFactory} instances.
 *
 * This class is essentially a transaction manager for the {@link Resolver}s.
 *
 * @created 2004-04-26
 * @author <a href="http://www.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.13 $
 * @modified $Date: 2005/06/26 12:48:11 $
 * @maintenanceAuthor $Author: pgearon $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Database implements SessionFactory
{
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(Database.class.getName());

  /** Startup Logger - will always have info enabled  */
  private static final Logger startupLogger =
    Logger.getLogger("Startup");

  /**
   * Placeholder indicating a nonexistent local node.
   *
   * This is a stopgap measure to deal with the lack of a defined negative
   * return value from {@link StringPool#findGNode}.
   */
  private final long NONE = NodePool.NONE;

  /** The directory where persistence files are stored.  */
  private final File directory;

  /**
   * Session to use for the initialization of new resolvers.
   *
   * Outside of initialization, this will always be <code>null</code>.
   */
  private StringPoolSessionFactory spSessionFactory = null;

  /**
   * ResolverSessionFactory for creating JRDFSessions.
   *
   * Outside of initialization, this will always be <code>null</code>.
   */
  private JRDFResolverSessionFactory jrdfSessionFactory = null;

  /**
   * The set of resolver factories that should have access to their models
   * cached.
   */
  private final Set cachedResolverFactorySet = new HashSet();

  /**
   * Read-only view of {@link #cachedResolverFactorySet}.
   */
  private final Set unmodifiableCachedResolverFactorySet =
    Collections.unmodifiableSet(cachedResolverFactorySet);

  /**
   * The set of all registered {@link ContentHandler} instances.
   */
  private final ContentHandlerManagerImpl contentHandlers;

  /**
   * The set of all registered {@link ResolverFactory} instances.
   */
  private final List resolverFactoryList = new ArrayList();

  /**
   * Read-only view of {@link #resolverFactoryList}, safe to be handed out to
   * the resolver sessions.
   */
  private final List unmodifiableResolverFactoryList =
    Collections.unmodifiableList(resolverFactoryList);

  /**
   * Keyed on URL protocol {@link String}s, mapping to the
   * {@link ResolverFactory} instance that manages external models via that
   * protocol.
   */
  private final Map externalResolverFactoryMap = new HashMap();

  /**
   * Read-only view of {@link #externalResolverFactoryMap}, safe to be handed
   * out to the resolver sessions.
   */
  private final Map unmodifiableExternalResolverFactoryMap =
    Collections.unmodifiableMap(externalResolverFactoryMap);

  /**
   * Keyed on model type {@link LocalNode}s, mapping to an
   * {@link InternalResolverFactory} instance that wraps the actual
   * {@link ResolverFactory} instance which manages that model type.
   */
  private final Map internalResolverFactoryMap = new HashMap();

  /**
   * Read-only view of {@link #internalResolverFactoryMap}, safe to be handed
   * out to the resolver sessions.
   */
  private final Map unmodifiableInternalResolverFactoryMap =
    Collections.unmodifiableMap(internalResolverFactoryMap);

  private DatabaseMetadata metadata;

  /** The URI of the model type used for system models.  */
  URI systemModelTypeURI = null;

  /** The resolver factory used to handle system models.  */
  SystemResolverFactory systemResolverFactory = null;

  /** The resolver factory used to handle temporary models.  */
  ResolverFactory temporaryResolverFactory;

  /** The rule loader factory used to make rule loaders. */
  String ruleLoaderClassName = null;

  /**
   * The set of {@link SecurityAdapter} instances.
   *
   * This will always contain the {@link SystemModelSecurityAdapter} which
   * safeguards the system model (<code>#</code>).  It will additionally
   * contain any additional adapters registered via the
   * {@link #addSecurityAdapter} method.
   */
  private final List securityAdapterList = new ArrayList(1);

  /**
   * Read-only view of {@link #securityAdapterList}, safe to be handed out to
   * the {@link DatabaseSession}s.
   */
  private final List unmodifiableSecurityAdapterList =
    Collections.unmodifiableList(securityAdapterList);

  /** The {@link URI} of the security domain this database is within.  */
  private final URI securityDomainURI;

  /**
   * The list of {@link SymbolicTransformation} instances.
   */
  private final List symbolicTransformationList = new ArrayList();

  /**
   * Read-only view of {@link #symbolicTransformationList}, safe to be handed
   * out to the {@link DatabaseSession}s.
   */
  private final List unmodifiableSymbolicTransformationList =
    Collections.unmodifiableList(symbolicTransformationList);

  /**
   * Model type to use to cache of external models.
   */
  private final URI temporaryModelTypeURI;

  /**
   * Factory for the {@link #transactionManager}.
   *
   * This only reason we hold a reference to this is so that it can be closed
   * when the database shuts down.
   */
  private final TransactionManagerFactory transactionManagerFactory;

  /**
   * The internal transaction manager.
   *
   * This class is a singleton with respect to a database instance.
   * Passed to new DatabaseSession's.
   */
  private final MulgaraTransactionManager transactionManager;

  /** The unique {@link URI} naming this database.  */
  private final URI uri;

  /** The set of alternative hostnames for the current host. */
  private final Set hostnameAliases;

  /**
   * A {@link SessionFactory} that produces {@link Session}s which aren't
   * subject to the registered {@link SecurityAdapter}s.
   */
  private final SessionFactory unsecuredSessionFactory =
    new UnsecuredSessionFactory();

  //
  // Constructor
  //

  /**
   * Creates a Database in a similar fashion to the stopgap constructor,
   * however, this version contains a configuration object which will allow the
   * server to configure the String and Node Pools as well as resolvers
   * (including the system resolver) and content handlers.
   *
   * @param uri  the unique {@link URI} naming this database, never
   *   <code>null</code>; this mustn't have a fragment part, because the
   *   fragment is used to represent models within the database
   * @param directory  an area on the filesystem for the database's use; if this
   *   is <code>null</code>, resolvers which require a filesystem can't be added
   * @param config The configuration object that will be used to determine the
   *               missing class names
   *
   * @throws InitializerException
   * @throws LocalizeException
   * @throws NamingException
   * @throws NodePoolException
   * @throws QueryException
   * @throws ResolverException
   * @throws ResolverFactoryException
   * @throws StringPoolException
   * @throws SystemException
   * @throws URISyntaxException
   *
   * @deprecated This constructor will be removed once back-compatibility to
   *   version 1.0 is not longer required by AbstractServer.  Use
   *   {@link DatabaseFactory#newDatabase} instead.
   */
  public Database(URI                            uri,
                  File                           directory,
                  org.mulgara.config.MulgaraConfig config)
      throws ConfigurationException, InitializerException, LocalizeException,
             NamingException, NodePoolException, QueryException,
             ResolverException, ResolverFactoryException,
             SecurityAdapterFactoryException, StringPoolException,
             SystemException, URISyntaxException
  {
    // Construct this resolver backed by in-memory components
    this(uri,        // database name
         directory,  // persistence directory
         uri,        // security domain
         new JotmTransactionManagerFactory(),
         config.getTransactionTimeout(),
         config.getPersistentNodePoolFactory().getType(),
         DatabaseFactory.subdir(
           directory,
           config.getPersistentNodePoolFactory().getDir()
         ),
         config.getPersistentStringPoolFactory().getType(),
         DatabaseFactory.subdir(
           directory,
           config.getPersistentStringPoolFactory().getDir()
         ),
         config.getPersistentResolverFactory().getType(),
         DatabaseFactory.subdir(
           directory,
           config.getPersistentResolverFactory().getDir()
         ),
         config.getTemporaryNodePoolFactory().getType(),
         DatabaseFactory.subdir(
           directory,
           config.getTemporaryNodePoolFactory().getDir()
         ),
         config.getTemporaryStringPoolFactory().getType(),
         DatabaseFactory.subdir(
           directory,
           config.getTemporaryStringPoolFactory().getDir()
         ),
         config.getTemporaryResolverFactory().getType(),
         DatabaseFactory.subdir(
           directory,
           config.getTemporaryResolverFactory().getDir()
         ),
         config.getRuleLoader().getType(),
         config.getDefaultContentHandler().getType());

    if (logger.isDebugEnabled()) {
      logger.debug("Constructed database via three method constructor.");
    }

    DatabaseFactory.configure(this, config);
  }


  /**
   * Construct a database.
   *
   * @param uri  the unique {@link URI} naming this database, never
   *   <code>null</code>; this mustn't have a fragment part, because the
   *   fragment is used to represent models within the database
   * @param directory  an area on the filesystem for the database's use; if this
   *   is <code>null</code>, resolvers which require a filesystem can't be added
   * @param securityDomainURI  the {@link URI} of the security domain this
   *   database is within, or <code>null</code> if this database is unsecured
   * @param transactionManagerFactory  the source for the
   *   {@link TransactionManager}, never <code>null</code>
   * @param transactionTimeout  the number of seconds before transactions time
   *   out, or zero to take the <var>transactionManagerFactory</var>'s default;
   *   never negative
   * @param persistentNodePoolFactoryClassName  the name of a
   *   {@link NodePoolFactory} implementation which will be used to generate
   *   persistent local nodes, never <code>null</code>
   * @param persistentStringPoolFactoryClassName  the name of a
   *   {@link StringPoolFactory} implementation which will be used to manage
   *   persistent RDF literals, never <code>null</code>
   * @param systemResolverFactoryClassName  the name of a
   *   {@link ResolverFactory} implementation which will be used to store
   *   system models; this class is required to register a model type
   * @param temporaryNodePoolFactoryClassName  the name of a
   *   {@link NodePoolFactory} implementation which will be used to generate
   *   temporary local nodes, never <code>null</code>
   * @param temporaryStringPoolFactoryClassName  the name of a
   *   {@link StringPoolFactory} implementation which will be used to manage
   *   temporary RDF literals, never <code>null</code>
   * @param temporaryResolverFactoryClassName  the name of a
   *   {@link ResolverFactory} implementation which will be used to store
   *   temporary statements, never <code>null</code>
   * @param ruleLoaderClassName  the name of a
   *   {@link RuleLoader} implementation which will be used for loading
   *   rule frameworks, never <code>null</code>
   * @param defaultContentHandlerClassName the name of the class that should be
   *   used to parse external content of unknown MIME type, or
   *   <code>null</code> to disable blind parsing
   * @throws IllegalArgumentException if <var>uri</var>,
   *   <var>systemResolverFactory</var> are <code>null</code>, or if the
   *   <var>uri</var> has a fragment part
   * @throws InitializerException  if the {@link NodePoolFactory},
   *   {@link ResolverFactory}, or {@link StringPoolFactory} instances
   *   generated from the various class names can't be initialized
   * @throws SystemException if <var>transactionTimeout</var> is negative
   */
  public Database(URI    uri,
                  File   directory,
                  URI    securityDomainURI,
                  TransactionManagerFactory transactionManagerFactory,
                  int    transactionTimeout,
                  String persistentNodePoolFactoryClassName,
                  File   persistentNodePoolDirectory,
                  String persistentStringPoolFactoryClassName,
                  File   persistentStringPoolDirectory,
                  String systemResolverFactoryClassName,
                  File   persistentResolverDirectory,
                  String temporaryNodePoolFactoryClassName,
                  File   temporaryNodePoolDirectory,
                  String temporaryStringPoolFactoryClassName,
                  File   temporaryStringPoolDirectory,
                  String temporaryResolverFactoryClassName,
                  File   temporaryResolverDirectory,
                  String ruleLoaderClassName,
                  String defaultContentHandlerClassName)
    throws ConfigurationException, InitializerException, LocalizeException,
           NamingException, NodePoolException, QueryException,
           ResolverException, ResolverFactoryException, StringPoolException,
           SystemException, URISyntaxException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Constructing database");
      logger.debug("Persistent node pool factory: class=" +
                   persistentNodePoolFactoryClassName + " directory=" +
                   persistentNodePoolDirectory);
      logger.debug("Persistent string pool factory: class=" +
                   persistentStringPoolFactoryClassName + " directory=" +
                   persistentStringPoolDirectory);
      logger.debug("Persistent resolver factory: class=" +
                   systemResolverFactoryClassName + " directory=" +
                   persistentResolverDirectory);
      logger.debug("Temporary node pool factory: class=" +
                   temporaryNodePoolFactoryClassName + " directory=" +
                   temporaryNodePoolDirectory);
      logger.debug("Temporary string pool factory: class=" +
                   temporaryStringPoolFactoryClassName + " directory=" +
                   temporaryStringPoolDirectory);
      logger.debug("Temporary resolver factory: class=" +
                   temporaryResolverFactoryClassName + " directory=" +
                   temporaryResolverDirectory);
      logger.debug("Rule loader: class=" +
                   ruleLoaderClassName);
    }

    // Validate parameters.
    if (uri == null) {
      throw new IllegalArgumentException("Null 'uri' parameter");
    }
    if (uri.getFragment() != null) {
      throw new IllegalArgumentException("Database URI can't have a fragment part: " + uri);
    }
    if (uri.getQuery() != null) {
      throw new IllegalArgumentException("Database URI can't have a query part: " + uri);
    }
    if (transactionManagerFactory == null) {
      throw new IllegalArgumentException("Null 'transactionManagerFactory' parameter");
    }
    if (persistentNodePoolFactoryClassName == null) {
      throw new IllegalArgumentException("Null 'persistentNodePoolFactoryClassName' parameter");
    }
    if (persistentStringPoolFactoryClassName == null) {
      throw new IllegalArgumentException("Null 'persistentStringPoolFactoryClassName' parameter");
    }
    if (systemResolverFactoryClassName == null) {
      throw new IllegalArgumentException("Null 'systemResolverFactoryClassName' parameter");
    }
    if (temporaryNodePoolFactoryClassName == null) {
      throw new IllegalArgumentException("Null 'temporaryNodePoolFactoryClassName' parameter");
    }
    if (temporaryStringPoolFactoryClassName == null) {
      throw new IllegalArgumentException("Null 'temporaryStringPoolFactoryClassName' parameter");
    }
    if (temporaryResolverFactoryClassName == null) {
      throw new IllegalArgumentException("Null 'temporaryResolverFactoryClassName' parameter");
    }

    // Initialize fields
    this.uri                       = uri;
    this.directory                 = directory;
    this.securityDomainURI         = securityDomainURI;
    this.transactionManagerFactory = transactionManagerFactory;

    try {
      ContentHandler defaultContentHandler = null;
      if (defaultContentHandlerClassName != null) {
        defaultContentHandler = (ContentHandler)
          Beans.instantiate(null, defaultContentHandlerClassName);
      }
      this.contentHandlers =
        new ContentHandlerManagerImpl(defaultContentHandler);
    }
    catch (Exception e) {
      throw new ConfigurationException(
        "Couldn't instantiate default content handler", e
      );
    }
    assert this.contentHandlers != null;

    // FIXME: Migrate this code inside StringPoolSession.  Pass config to StringPoolSession.
    this.transactionManager = new MulgaraTransactionManager(transactionManagerFactory);

    transactionManager.setTransactionTimeout(transactionTimeout);

    // Enable resolver initialization
    if (logger.isDebugEnabled()) {
      logger.debug("Creating initialization session");
    }

    // Create the set of alternative names for the current host.
    Set hostNames = new HashSet();
    hostNames.addAll(Arrays.asList(new String[] {"localhost", "127.0.0.1"}));

    // Attempt to obtain the IP address
    try {
    	hostNames.add(InetAddress.getLocalHost().getHostAddress());
    } catch(UnknownHostException ex) {
      logger.info("Unable to obtain local host address "+
          "aliases", ex);
    }
    // Attempt to obtain the localhost name
    try {
    	hostNames.add(InetAddress.getLocalHost().getHostName());
    } catch(UnknownHostException ex) {
      logger.info("Unable to obtain local host name for  "+
          "aliases", ex);
    }
    if (startupLogger.isInfoEnabled()) {
      StringBuffer aliases =
        new StringBuffer("Host name aliases for this server are: [");
      for (Iterator it = hostNames.iterator(); it.hasNext(); ) {
        aliases.append(it.next().toString());
        if (it.hasNext()) {
          aliases.append(", ");
        }
      }
      aliases.append("]");
      startupLogger.info(aliases.toString());
    }

    if (!uri.isOpaque()) {
      String currentHost = uri.getHost();
      if (currentHost != null) {
        hostNames.add(currentHost.toLowerCase());
      }
    }
    hostnameAliases = Collections.unmodifiableSet(hostNames);
    setHostnameAliases(hostNames);

    // Create an instance of ResolverSessionFactory
    DatabaseFactoryInitializer persistentStringPoolFactoryInitializer =
      new DatabaseFactoryInitializer(uri,
                                     hostnameAliases,
                                     persistentStringPoolDirectory);

    DatabaseFactoryInitializer persistentNodePoolFactoryInitializer =
      new DatabaseFactoryInitializer(uri,
                                     hostnameAliases,
                                     persistentNodePoolDirectory);

    DatabaseFactoryInitializer temporaryStringPoolFactoryInitializer =
      new DatabaseFactoryInitializer(uri,
                                     hostnameAliases,
                                     temporaryStringPoolDirectory);

    DatabaseFactoryInitializer temporaryNodePoolFactoryInitializer =
      new DatabaseFactoryInitializer(uri,
                                     hostnameAliases,
                                     temporaryNodePoolDirectory);

    spSessionFactory = new StringPoolSessionFactory(
      uri,
      hostnameAliases,
      persistentStringPoolFactoryClassName,
      persistentStringPoolFactoryInitializer,
      persistentNodePoolFactoryClassName,
      persistentNodePoolFactoryInitializer,
      temporaryStringPoolFactoryClassName,
      temporaryStringPoolFactoryInitializer,
      temporaryNodePoolFactoryClassName,
      temporaryNodePoolFactoryInitializer
    );

    // Ensure that no further initialization is provided
    persistentStringPoolFactoryInitializer.close();
    persistentNodePoolFactoryInitializer.close();
    temporaryStringPoolFactoryInitializer.close();
    temporaryNodePoolFactoryInitializer.close();

    DatabaseFactoryInitializer initializer =
        new DatabaseFactoryInitializer(uri, hostnameAliases, directory);

    jrdfSessionFactory =
        new JRDFResolverSessionFactory(initializer, spSessionFactory);

    // Ensure that no further initialization is provided
    initializer.close();

    // Create the temporary resolver factory
    initializer = new DatabaseFactoryInitializer(
      uri, hostnameAliases, temporaryResolverDirectory
    );
    temporaryResolverFactory = ResolverFactoryFactory.newSystemResolverFactory(
      temporaryResolverFactoryClassName, initializer, spSessionFactory
    );
    initializer.close();
    resolverFactoryList.add(temporaryResolverFactory);

    // Determine temporary model type URI
    /*
    // TODO: be less "clever" and more fault-tolerant in doing this
    temporaryModelTypeURI =
      (URI) ((Map.Entry) internalResolverFactoryMap.entrySet()
                                                   .iterator()
                                                   .next()).getKey();
    */
    temporaryModelTypeURI = new URI(Mulgara.NAMESPACE + "MemoryModel");
    assert temporaryModelTypeURI != null;

    /*
    // Discard any temporary resolver state
    if (temporaryResolverFactory instanceof SystemResolverFactory) {
      try {
        recoverDatabase(new SimpleXARecoveryHandler[] {
          spSessionFactory,
          (SystemResolverFactory) temporaryResolverFactory
        });
      }
      catch (SimpleXAResourceException es) {
        logger.fatal("Failed to recover cache");
        throw new InitializerException("Failed to recover cache", es);
      }
    }
    */

    // Create the system resolver factory
    initializer = new DatabaseFactoryInitializer(
      uri, hostnameAliases, persistentResolverDirectory
    );
    systemResolverFactory = ResolverFactoryFactory.newSystemResolverFactory(
      systemResolverFactoryClassName, initializer, spSessionFactory
    );
    initializer.close();
    resolverFactoryList.add(systemResolverFactory);

    // Recover the system resolver state
    try {
      recoverDatabase(new SimpleXARecoveryHandler[] { spSessionFactory, systemResolverFactory });
    } catch (SimpleXAResourceException es) {
      logger.fatal("Failed to recover existing database");
      throw new InitializerException("Failed to recover existing database", es);
    }

    // Initialize the system resolver
    if (logger.isDebugEnabled()) {
      logger.debug("Added system resolver " + systemResolverFactoryClassName);
    }


    URI systemModelURI = new URI(uri.getScheme(), uri.getSchemeSpecificPart(), "");
    metadata =
      new DatabaseMetadataImpl(uri,
                               hostnameAliases,
                               securityDomainURI,
                               systemModelURI,
                               RDF.TYPE,
                               systemResolverFactory.getSystemModelTypeURI());

    DatabaseSession session = new DatabaseSession(
        transactionManager,
        unmodifiableSecurityAdapterList,
        unmodifiableSymbolicTransformationList,
        spSessionFactory,
        systemResolverFactory,
        temporaryResolverFactory,
        unmodifiableResolverFactoryList,
        unmodifiableExternalResolverFactoryMap,
        unmodifiableInternalResolverFactoryMap,
        metadata,
		    contentHandlers,
        unmodifiableCachedResolverFactorySet,
        temporaryModelTypeURI,
        ruleLoaderClassName);

    // Updates metadata to reflect bootstrapped system model.
    session.bootstrapSystemModel((DatabaseMetadataImpl)metadata);
    session.close();

    if (metadata.getSystemModelTypeURI() == null) {
      logger.fatal("bootstrap failed to initialize metadata");
      throw new InitializerException("bootstrap failed to initialize metadata");
    }

    internalResolverFactoryMap.put(
      metadata.getSystemModelTypeURI(),
      new InternalSystemResolverFactory(systemResolverFactory,
                                        metadata.getRdfTypeNode(),
                                        metadata.getSystemModelNode())
    );

    // Add the mandatory security adapter that protects the system model
    securityAdapterList.add(
      new SystemModelSecurityAdapter(metadata.getSystemModelNode())
    );

    this.ruleLoaderClassName = ruleLoaderClassName;

    if (logger.isDebugEnabled()) {
      logger.debug("Constructed database");
    }
  }


  /**
   * Register a new kind of {@link Content} with this database.
   *
   * @param className  the name of a class implementing {@link ContentHandler}
   * @throws IllegalArgumentException if <var>className</var> is
   *   <code>null</code> or isn't a valid {@link ContentHandler}
   */
  public void addContentHandler(String className)
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding content handler " + className);
    }

    // Instantiate the content handler
    contentHandlers.registerContentHandler(className);

    if (logger.isDebugEnabled()) {
      logger.debug("Added content handler " + className);
    }
  }


  /**
   * Add a resolver to this database.
   *
   * Although the {@link ResolverFactory} interface can't specify it, all
   * implementations are expected to provide a static <code>newInstance</code>
   * method.
   *
   * @param className  the name of a class implementing
   *   {@link ResolverFactory}, never <code>null</code>
   * @param dir  a subdirectory under the systemwide persistence directory
   *   which this resolver factory may request via the
   *   {@link FactoryInitializer#getDirectory} method; if <code>null</code>,
   *   no persistence directory will be offered
   * @throws IllegalArgumentException if <var>className</var> is
   *   <code>null</code> or doesn't implement {@link ResolverFactory}
   * @throws InitializerException  if the {@link ResolverFactory}
   *   generated from the <var>className</var> can't be initialized
   */
  public void addResolverFactory(String className, File dir)
    throws InitializerException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding resolver factory " + className);
    }

    DatabaseResolverFactoryInitializer initializer =
      new DatabaseResolverFactoryInitializer(cachedResolverFactorySet,
                                             this,
                                             metadata,
                                             dir,
                                             contentHandlers,
                                             systemResolverFactory);

    resolverFactoryList.add(
      ResolverFactoryFactory.newResolverFactory(className, initializer)
    );

    initializer.close();  // ensure that no further initialization is provided

    if (logger.isDebugEnabled()) {
      logger.debug("Added resolver factory " + className);
    }
  }

  /**
   * Add a {@link SecurityAdapter} to this {@link Database}.
   *
   * @param securityAdapterFactory  the factory to use to create the security
   *   adapter, never <code>null</code>
   * @throws SecurityAdapterFactoryException if the
   *   <var>securityAdapterFactory</var> fails to create the
   *   {@link SecurityAdapter}
   * @throws IllegalArgumentException if <var>securityAdapterFactory</var> is
   *   <code>null</code>
   */
  public void addSecurityAdapter(SecurityAdapterFactory securityAdapterFactory)
    throws SecurityAdapterFactoryException
  {
    // Create the security adapter even if in admin mode, because we need the
    // side effects like model creation to be trigger
    DatabaseSecurityAdapterInitializer initializer =
      new DatabaseSecurityAdapterInitializer(this,
                                             metadata,
                                             unsecuredSessionFactory);

    SecurityAdapter securityAdapter =
      securityAdapterFactory.newSecurityAdapter(initializer);

    initializer.close();  // ensure that no further initialization is provided

    if ((System.getProperty("admin") == null)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Adding security adapter " +
                     securityAdapterFactory.getClass());
      }

      securityAdapterList.add(securityAdapter);

      if (logger.isDebugEnabled()) {
        logger.debug("Added security adapter");
      }
    }
    else {
      // Skip the addition of security adapters if we're started in admin mode
      logger.warn("Skipping addition of security adapter " +
                  securityAdapterFactory.getClass().getName() +
                  " because the database is running in admin mode");
    }
  }

  /**
   * Flush all resources associated with the database into a recoverable state.
   */
  public void close()
  {

    // Transaction management
    transactionManagerFactory.close();

    // Resolver factories
    for (Iterator i = resolverFactoryList.iterator(); i.hasNext();) {
      ResolverFactory resolverFactory = (ResolverFactory) i.next();
      try {
        resolverFactory.close();
      }
      catch (ResolverFactoryException e) {
        logger.warn("Unable to close " + resolverFactory, e);
      }
    }

    spSessionFactory.close();
    jrdfSessionFactory.close();
  }

  /**
   * Remove all persistent resources associated with the database.
   *
   * In other words, erase all the data.  This is generally only useful for
   * testing.
   */
  public void delete()
  {
    // Resolver factories
    for (Iterator i = resolverFactoryList.iterator(); i.hasNext();) {
      ResolverFactory resolverFactory = (ResolverFactory) i.next();
      try {
        resolverFactory.delete();
      } catch (ResolverFactoryException e) {
        logger.warn("Unable to delete " + resolverFactory, e);
      }
    }

    spSessionFactory.delete();
    jrdfSessionFactory.delete();
  }

  //
  // Methods implementing SessionFactory
  //

  public URI getSecurityDomain()
  {
    assert securityDomainURI != null;
    return securityDomainURI;
  }

  public Session newSession() throws QueryException
  {
    try {
      return new DatabaseSession(
        transactionManager,
        unmodifiableSecurityAdapterList,
        unmodifiableSymbolicTransformationList,
        spSessionFactory,
        systemResolverFactory,
        temporaryResolverFactory,
        unmodifiableResolverFactoryList,
        unmodifiableExternalResolverFactoryMap,
        unmodifiableInternalResolverFactoryMap,
        metadata,
        contentHandlers,
        unmodifiableCachedResolverFactorySet,
        temporaryModelTypeURI,
        ruleLoaderClassName);
    } catch (ResolverFactoryException e) {
      throw new QueryException("Couldn't create session", e);
    }
  }

  /**
   * Creates a session that can be used for a JRDF Graph.
   *
   * @throws QueryException
   * @return Session
   */
  public Session newJRDFSession() throws QueryException {
    try {
      return new LocalJRDFDatabaseSession(
          transactionManager,
          unmodifiableSecurityAdapterList,
          unmodifiableSymbolicTransformationList,
          jrdfSessionFactory,
          systemResolverFactory,
          temporaryResolverFactory,
          unmodifiableResolverFactoryList,
          unmodifiableExternalResolverFactoryMap,
          unmodifiableInternalResolverFactoryMap,
          metadata,
          contentHandlers,
          cachedResolverFactorySet,
          temporaryModelTypeURI);
    }
    catch (ResolverFactoryException e) {
      throw new QueryException("Couldn't create JRDF session", e);
    }
  }

  //
  // Internal methods
  //

  void addModelType(URI modelTypeURI, ResolverFactory resolverFactory)
    throws InitializerException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Registering model type " + modelTypeURI + " for " +  resolverFactory);
    }

    // Validate "modelType" parameter
    if (modelTypeURI == null) {
      throw new IllegalArgumentException("Null 'modelType' parameter");
    }

    // Validate "resolverFactory" parameter
    if (resolverFactory == null) {
      throw new IllegalArgumentException("Null \"resolverFactory\" parameter");
    }

    // Make sure some other resolver factory hasn't claimed this model type
    if (internalResolverFactoryMap.containsKey(modelTypeURI)) {
      throw new InitializerException(
          "Model type " + modelTypeURI + " is already registered to " +
          internalResolverFactoryMap.get(modelTypeURI));
    }

    // Register this resolver factory as handling this model type
    internalResolverFactoryMap.put(
      modelTypeURI,
      new InternalResolverFactory(resolverFactory,
                                  metadata.getRdfTypeNode(),
                                  metadata.getSystemModelNode())
    );
  }


  void addProtocol(String protocol, ResolverFactory resolverFactory)
    throws InitializerException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Registering protocol " + protocol + " for " +  resolverFactory);
    }

    // Validate "protocol" parameter
    if (protocol == null) {
      throw new IllegalArgumentException("Null \"protocol\" parameter");
    }

    // Validate "resolverFactory" parameter
    if (resolverFactory == null) {
      throw new IllegalArgumentException("Null \"resolverFactory\" parameter");
    }

    // Make sure some other resolver factory hasn't claimed this protocol
    if (externalResolverFactoryMap.containsKey(protocol)) {
      throw new InitializerException(
          "Protocol " + protocol + " is already registered to " +
          externalResolverFactoryMap.get(protocol));
    }

    // Register this resolver factory as handling this model type
    externalResolverFactoryMap.put(protocol, resolverFactory);
  }


  void addSymbolicTransformation(SymbolicTransformation symbolicTransformation)
    throws InitializerException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Registering symbolic transformation " +
                   symbolicTransformation.getClass());
    }

    // Validate "symbolicTransformation" parameter
    if (symbolicTransformation == null) {
      throw new IllegalArgumentException("Null \"symbolicTransformation\" parameter");
    }

    // Register the rule
    symbolicTransformationList.add(symbolicTransformation);
  }


  File getRootDirectory()
  {
    return directory;
  }


  private void recoverDatabase(SimpleXARecoveryHandler[] handlers)
      throws SimpleXAResourceException
  {
    assert handlers != null;

    TIntHashSet[] phaseSets = recoverRecoveryHandlers(handlers);

    if (phaseSets == null) {
      clearDatabase(handlers);
    } else {
      TIntHashSet phaseSet = intersectPhaseSets(phaseSets);
      if (phaseSet.isEmpty()) {
        throw new SimpleXAResourceException("No matching phases between Resource Handlers.");
      }

      selectCommonPhase(highestCommonPhaseNumber(phaseSet), handlers);
    }
  }


  private TIntHashSet[] recoverRecoveryHandlers(SimpleXARecoveryHandler[] handlers)
      throws SimpleXAResourceException
  {
    TIntHashSet[] phaseSets = new TIntHashSet[handlers.length];
    boolean allEmpty = true;
    for (int i = 0; i < handlers.length; i++) {
      phaseSets[i] = new TIntHashSet(handlers[i].recover());
      if (!allEmpty && phaseSets[i].isEmpty()) {
        throw new SimpleXAResourceException("Unable to find common phase in pre-existing database");
      } else if (!phaseSets[i].isEmpty()) {
        allEmpty = false;
      }
    }

    return !allEmpty ? phaseSets : null;
  }


  private TIntHashSet intersectPhaseSets(TIntHashSet[] phaseSets)
  {
    TIntHashSet phaseSet = phaseSets[0];
    for (int i = 1; i < phaseSets.length; i++) {
      phaseSet.retainAll(phaseSets[i].toArray());
    }

    return phaseSet;
  }


  private void clearDatabase(SimpleXARecoveryHandler[] handlers)
      throws SimpleXAResourceException
  {
    try {
      for (int i = 0; i < handlers.length; i++) {
        handlers[i].clear();
      }
    } catch (IOException ei) {
      throw new SimpleXAResourceException("IO failure clearing database", ei);
    }
  }


  private int highestCommonPhaseNumber(TIntHashSet phaseSet)
  {
    int hcpn = -1;

    TIntIterator i = phaseSet.iterator();
    while (i.hasNext()) {
      int phase = i.next();
      hcpn = phase > hcpn ? phase : hcpn;
    }

    return hcpn;
  }

  private void selectCommonPhase(int phaseNumber, SimpleXARecoveryHandler[] handlers)
      throws SimpleXAResourceException
  {
    try {
      for (int i = 0; i < handlers.length; i++) {
        handlers[i].selectPhase(phaseNumber);
      }
    } catch (IOException ei) {
      throw new SimpleXAResourceException("IO failure selecting phase on database", ei);
    }
  }

  //
  // Inner classes
  //

  /**
   * A view of the outer {@link Database}s's {@link SessionFactory} interface
   * that produces unsecured {@link DatabaseSession}s.
   *
   * @created 2004-10-21
   * @author <a href="http://www.pisoftware.com/raboczi">Simon Raboczi</a>
   * @version $Revision: 1.13 $
   * @modified $Date: 2005/06/26 12:48:11 $
   * @maintenanceAuthor $Author: pgearon $
   * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
   * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
   *   Technology, Inc</a>
   * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
   */
  private class UnsecuredSessionFactory implements SessionFactory
  {
    //
    // Methods implementing SessionFactory
    //

    /**
     * @return {@inheritDoc}; this is the same value as the outer
     *   {@link Database} returns
     */
    public URI getSecurityDomain() throws QueryException
    {
      return Database.this.getSecurityDomain();
    }

    /**
     * @return an unsecured {@link Session} to the outer {@link Database}
     */
    public Session newSession() throws QueryException
    {
      try {
        return new DatabaseSession(
          transactionManager,
          Collections.singletonList(
            new SystemModelSecurityAdapter(metadata.getSystemModelNode())
          ),
          unmodifiableSymbolicTransformationList,
          spSessionFactory,
          systemResolverFactory,
          temporaryResolverFactory,
          unmodifiableResolverFactoryList,
          unmodifiableExternalResolverFactoryMap,
          unmodifiableInternalResolverFactoryMap,
          metadata,
          contentHandlers,
          unmodifiableCachedResolverFactorySet,
          temporaryModelTypeURI,
          ruleLoaderClassName);
      }
      catch (ResolverFactoryException e) {
        throw new QueryException("Couldn't create session", e);
      }
    }

    /**
     * @return an unsecured {@link Session} to the outer {@link Database}
     */
    public Session newJRDFSession() throws QueryException
    {
      try {
        return new LocalJRDFDatabaseSession(
          transactionManager,
          Collections.singletonList(
            new SystemModelSecurityAdapter(metadata.getSystemModelNode())
          ),
          unmodifiableSymbolicTransformationList,
          jrdfSessionFactory,
          systemResolverFactory,
          temporaryResolverFactory,
          unmodifiableResolverFactoryList,
          unmodifiableExternalResolverFactoryMap,
          unmodifiableInternalResolverFactoryMap,
          metadata,
          contentHandlers,
          cachedResolverFactorySet,
          temporaryModelTypeURI);
      }
      catch (ResolverFactoryException e) {
        throw new QueryException("Couldn't create session", e);
      }
    }

    /**
     * {@inheritDoc}  This method is a no-op in this implementation.
     */
    public void close() throws QueryException
    {
      // null implementation
    }

    /**
     * {@inheritDoc}  This method is a no-op in this implementation.
     */
    public void delete() throws QueryException
    {
      // null implementation
    }
  }


  /**
   * Sets the hostnames on the ServerInfo object, if it is visible.
   *
   * @param names The set of hostnames to set on ServerInfo
   */
  private void setHostnameAliases(Set names) {
    try {
      Class si = Class.forName("org.mulgara.server.ServerInfo");
      java.lang.reflect.Method setter = si.getMethod("setHostnameAliases", new Class[] { Set.class });
      setter.invoke(null, new Object[] { names });
    } catch (Exception e) {
      /* Not much that can be done here */
      logger.warn("Unable to set the host names for Server Info", e);
    }
  }
}
