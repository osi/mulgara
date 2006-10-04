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
 *   SymbolicTransformationContext contributed by Netymon Pty Ltd on behalf of
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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.transaction.xa.XAResource;

// Java 2 enterprise packages
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.Node;
import org.jrdf.graph.URIReference;

// Local packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.GlobalizeException;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverFactory;
import org.mulgara.resolver.spi.ResolverFactoryException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.SecurityAdapter;
import org.mulgara.resolver.spi.SymbolicTransformationContext;
import org.mulgara.resolver.spi.SystemResolver;
import org.mulgara.resolver.view.ViewMarker;
import org.mulgara.store.nodepool.NodePool;

/**
 * Services provided by {@link DatabaseSession} to invocations of the
 * {@link Operation#execute} method.
 *
 * @created 2004-11-08
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.10 $
 * @modified $Date: 2005/05/02 20:07:56 $ by $Author: raboczi $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class DatabaseOperationContext implements OperationContext, SymbolicTransformationContext
{
  /**
   * Logger.
   *
   * This is named after the class.
   */
  private static final Logger logger =
    Logger.getLogger("DatabaseOperationContext.class,getName()");

  /**
   * The models from external resolvers which have been cached as temporary
   * models.
   *
   * Every model in this set can be manipulated by resolvers from the
   * {@link #temporaryResolverFactory}.
   */
  private final Set cachedModelSet;

  /**
   * The models from external resolvers which have been cached as temporary
   * models and modified.
   *
   * Every model in this set can be manipulated by resolvers from the
   * {@link #temporaryResolverFactory}.
   */
  private final Set changedCachedModelSet;

  /**
   * A map from {@link URI}s of models to {@link LocalNode}s representing the
   * localized type of the model.
   *
   * This is populated by {@link #findModelTypeURI} and cleared by
   * {@link #clearSystemModelCache}.
   */
  private final Map systemModelCacheMap = new WeakHashMap();

  // Immutable properties of the containing DatabaseSession
  private final Set                cachedResolverFactorySet;
  private final DatabaseSession    databaseSession;
  private final Map                enlistedResolverMap;
  private final Map                externalResolverFactoryMap;
  private final Map                internalResolverFactoryMap;
  private final DatabaseMetadata   metadata;
  private final List               securityAdapterList;
  private final URI                temporaryModelTypeURI;
  private final ResolverFactory    temporaryResolverFactory;
  private final TransactionManager transactionManager;

  //
  // Constructor
  //

  /**
   * Sole constructor.
   */
  DatabaseOperationContext(Set                cachedModelSet,
                           Set                cachedResolverFactorySet,
                           Set                changedCachedModelSet,
                           DatabaseSession    databaseSession,
                           Map                enlistedResolverMap,
                           Map                externalResolverFactoryMap,
                           Map                internalResolverFactoryMap,
                           DatabaseMetadata   metadata,
                           List               securityAdapterList,
                           URI                temporaryModelTypeURI,
                           ResolverFactory    temporaryResolverFactory,
                           TransactionManager transactionManager)
  {
    assert cachedModelSet             != null;
    assert cachedResolverFactorySet   != null;
    assert changedCachedModelSet      != null;
    assert databaseSession            != null;
    assert enlistedResolverMap        != null;
    assert externalResolverFactoryMap != null;
    assert internalResolverFactoryMap != null;
    assert metadata                   != null;
    assert securityAdapterList        != null;
    assert temporaryModelTypeURI      != null;
    assert temporaryResolverFactory   != null;
    assert transactionManager         != null;

    // Initialize fields
    this.cachedModelSet             = cachedModelSet;
    this.cachedResolverFactorySet   = cachedResolverFactorySet;
    this.changedCachedModelSet      = changedCachedModelSet;
    this.databaseSession            = databaseSession;
    this.enlistedResolverMap        = enlistedResolverMap;
    this.externalResolverFactoryMap = externalResolverFactoryMap;
    this.internalResolverFactoryMap = internalResolverFactoryMap;
    this.metadata                   = metadata;
    this.securityAdapterList        = securityAdapterList;
    this.temporaryModelTypeURI      = temporaryModelTypeURI;
    this.temporaryResolverFactory   = temporaryResolverFactory;
    this.transactionManager         = transactionManager;
  }

  //
  // Methods implementing OperationContext
  //

  public ResolverFactory findModelResolverFactory(long model)
    throws QueryException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Finding resolver factory for model " + model);
    }

    // See if the model is an internal one, with a model type
    try {
      URI modelTypeURI = findModelTypeURI(model);
      if (modelTypeURI != null) {
        // The model had a type recorded in the system model, so it's internal
        if (logger.isDebugEnabled()) {
          logger.debug("Model " + model + " type is " + modelTypeURI);
        }
        InternalResolverFactory internalResolverFactory =
          (InternalResolverFactory) internalResolverFactoryMap.get(modelTypeURI);

        if (internalResolverFactory == null) {
          throw new QueryException("Unsupported model type for model " + model);        }

        return internalResolverFactory;
      }
      else {
        // This might be an external model or an aliased internal model.
        // get the model URI
        Node modelNode = databaseSession.getSystemResolver().globalize(model);
        if (!(modelNode instanceof URIReference)) {
          throw new QueryException(modelNode.toString() + " is not a valid Model");
        }
        URI modelURI = ((URIReference)modelNode).getURI();

        // check if this is really a reference to a local model, using a different server name
        Node aliasedNode = getCanonicalAlias(modelURI);
        if (aliasedNode != null) {
          long aliasedModel = databaseSession.getSystemResolver().localize(aliasedNode);
          return findModelResolverFactory(aliasedModel);
        }

        // test the model URI against the current server
        try {
          if (logger.isDebugEnabled()) {
            logger.debug("Comparing " + metadata.getURI().toString() + " to " + (new URI(modelURI.getScheme(),
                    modelURI.getSchemeSpecificPart(), null)).toString());
          }

          // Check all the hostname aliases to see if we're attempting to
          // contact the local server.
          URI tmpModelName = new URI(modelURI.getScheme(),
              modelURI.getSchemeSpecificPart(), null);
          String host = tmpModelName.getHost();

          // Ensure that the host name can be extracted - in case there's an
          // opaque hostname.
          if (tmpModelName.isOpaque()) {
            throw new QueryException("Unable to extract hostname from: " +
                tmpModelName);
          }

          // Do not test for locality if jar or file protocol
          if (!(modelURI.getScheme().startsWith("file")) &&
            !(modelURI.getScheme().startsWith("jar"))) {

            // Check that it's the same host name and server name.
            if ((metadata.getHostnameAliases().contains(host)) &&
                (metadata.getServerName().equals(metadata.getServerName(modelURI)))) {
              // should be on the current server, but was not found here
              throw new QueryException(modelNode.toString() + " is not a Model");
            }
          }
        }
        catch (URISyntaxException use) {
          throw new QueryException("Internal error.  Model URI cannot be manipulated.");
        }

        // This is not a local model, get the protocol
        String modelProtocol = findProtocol(model);
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

        // For the moment, not applying caching to any external models
        // TODO: add a method to ResolverFactory interface to test whether
        //       caching is appropriate for that particular implementation
        if (cachedResolverFactorySet.contains(resolverFactory)) {
          return new CacheResolverFactory(resolverFactory,
                                          temporaryResolverFactory,
                                          temporaryModelTypeURI,
                                          cachedModelSet,
                                          changedCachedModelSet);
        }
        else {
          return resolverFactory;
        }
      }
    }
    catch (GlobalizeException eg) {
      throw new QueryException("Unable to globalize modeltype", eg);
    }
    catch (LocalizeException el) {
      throw new QueryException("Unable to localize model", el);
    }
  }

  public ResolverFactory findModelTypeResolverFactory(URI modelTypeURI)
    throws QueryException
  {
    return (InternalResolverFactory) internalResolverFactoryMap.get(modelTypeURI);
  }

  public List getSecurityAdapterList()
  {
    return securityAdapterList;
  }

  public Resolver obtainResolver(ResolverFactory resolverFactory,
                                 SystemResolver  systemResolver)
    throws QueryException
  {
    ResolverSession session;

    // Obtain a resolver
    Resolver resolver = (Resolver) enlistedResolverMap.get(resolverFactory);
    if (resolver != null) {
      return resolver;
    }

    try {
      resolver = resolverFactory.newResolver(databaseSession.isWriting(),
                                             systemResolver,
                                             systemResolver);

      // FIXME: This is a kludge.  This should be done using a query rewriting
      //        hook in the ResolverFactory interface.  This hook is also
      //        required for efficient evaluation of XSD/Type constraints
      //        (specifically intervals), and distributed queries
      //        (specificially appended joins).
      if (resolver instanceof ViewMarker) {
        ((ViewMarker) resolver).setSession(databaseSession);
      }
    }
    catch (ResolverFactoryException e) {
      throw new QueryException("Unable to obtain resolver", e);
    }
    assert resolver != null;

    enlistedResolverMap.put(resolverFactory, resolver);

    return enlistResolver(resolver);
  }


  /**
   * Returns the canonical form of the model, leaving the model alone if it is recognised or unknown.
   *
   * @param model The model to check.
   * @return The new model node, or the current model if it is already canonical or unknown.
   */
  public long getCanonicalModel(long model) {
    // globalize to a URI
    try {
      Node modelNode = databaseSession.getSystemResolver().globalize(model);
      if (!(modelNode instanceof URIReference)) {
        logger.warn(modelNode.toString() + " is not a valid Model");
        return model;
      }
      URI modelURI = ((URIReference)modelNode).getURI();

      // check if this is really a reference to a local model, using a different server name
      Node aliasedNode = getCanonicalAlias(modelURI);
      if (aliasedNode != null) {
        return databaseSession.getSystemResolver().localize(aliasedNode);
      }
    } catch (Exception e) {
      // unable to get a canonical form, so leave this model alone
    }
    // model was not recognised as being on this server, so leave it alone
    return model;
  }

  //
  // Methods required by SymbolicTransformationContext
  //

  public URI mapToModelTypeURI(URI modelURI) throws QueryException {
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("Finding modelTypeURI for " + modelURI);
      }
      long rawModel = databaseSession.getSystemResolver().localize(new URIReferenceImpl(modelURI));
      long canModel = getCanonicalModel(rawModel);

      URI modelTypeURI = findModelTypeURI(canModel);

      if (logger.isInfoEnabled()) {
        logger.info("Mapped " + modelURI + " via " + rawModel + ":" + canModel + " to ModelTypeURI: " + modelTypeURI);
      }

      return modelTypeURI;
    } catch (GlobalizeException eg) {
      throw new QueryException("Failed to map model to modelType", eg);
    } catch (LocalizeException el) {
      throw new QueryException("Failed to map model to modelType", el);
    }
  }

  //
  // Methods used only by DatabaseSession
  //

  /**
   * Remove all the cached entries resulting from calls to
   * {@link #findModelTypeURI}.
   *
   * This needs to be called at the end of transactions by
   * {@link DatabaseSession} because the system model may change thereafter.
   */
  void clearSystemModelCache()
  {
    systemModelCacheMap.clear();
  }

  //
  // Internal methods
  //

  /**
   * @param resolver  a resolver to enlist into the current transaction
   * @throws QueryException  if the <var>resolver</var> can't be enlisted
   */
  Resolver enlistResolver(Resolver resolver) throws QueryException
  {
    // Obtain the transaction over the current thread
    Transaction transaction;
    try {
      transaction = transactionManager.getTransaction();
    }
    catch (Exception e) {
      throw new QueryException("Unable to obtain transaction", e);
    }
    if (transaction == null) {
      if (databaseSession.getTransaction() != null) {
        logger.error("Transaction suspended and not resumed when enlisting resolver");
      }
      else {
        logger.error("Not in Transaction when enlisting resolver");
      }
      throw new QueryException("Failed to find transaction when enlisting resolver");
    }

    // Enlist the resolver into the transaction
    XAResource xaResource = resolver.getXAResource();
    if (logger.isDebugEnabled()) {
      logger.debug("Enlisting " + resolver);
    }

    try {
      transaction.enlistResource(xaResource);
    }
    catch (Exception e) {
      throw new QueryException("Unable to enlist " + resolver + " into transaction", e);
    }

    return resolver;
  }

  /**
   * Find the type of a model.
   *
   * @param model  the local node of a model
   * @return the local node representing the type of the <var>model</var>, or
   *   {@link NodePool#NONE} if the <var>model</var> isn't stored within the
   *   system
   * @throws QueryException if the model type can't be determined
   */
  private URI findModelTypeURI(long model)
    throws QueryException, GlobalizeException
  {
    // If model is a query-node, model cannot exist in the system model so return null.
    if (model < 0) {
      return null;
    }

    // Check our cached version of the system model
    LocalNode modelLocalNode = new LocalNode(model);
    URI modelTypeURI = (URI) systemModelCacheMap.get(modelLocalNode);
    if (modelTypeURI != null) {
      return modelTypeURI;
    }

    // Query the system model for the type of the model
    Variable modelTypeVariable = new Variable("modelType");
    Constraint modelConstraint =
      new ConstraintImpl(new LocalNode(model),
                         new LocalNode(metadata.getRdfTypeNode()),
                         modelTypeVariable,
                         new LocalNode(metadata.getSystemModelNode()));
    Resolution resolution = databaseSession.getSystemResolver().resolve(modelConstraint);
    assert resolution != null;

    // Check the solution and extract the model type (if any) from it
    try {
      resolution.beforeFirst();
      if (resolution.next()) {
        long modelType = resolution.getColumnValue(
            resolution.getColumnIndex(modelTypeVariable));

        if (resolution.next()) {
          throw new QueryException("Model " + model +
              " has more than one type!");
        }
        Node modelNode = databaseSession.getSystemResolver().globalize(modelType);
        assert modelNode instanceof URIReferenceImpl;
        modelTypeURI = ((URIReferenceImpl) modelNode).getURI();
        systemModelCacheMap.put(modelLocalNode, modelTypeURI);
        return modelTypeURI;
      }
      else {
        return null;
      }
    }
    catch (TuplesException e) {
      throw new QueryException("Unable to determine model type of " + model, e);
    }
    finally {
      if ( resolution != null ) {
        try {
          resolution.close();
        }
        catch (TuplesException e) {
          logger.warn("Unable to close find model type resolution to model " + model, e);
        }
      }
    }
  }

  /**
   * @param n  the local node corresponding to the URI reference
   * @return  the scheme part of the <var>node</var>'s URI reference
   * @throws QueryException if the <var>node</var> can't be globalized or
   *   isn't a URI reference
   */
  /*private*/ String findProtocol(long n) throws QueryException
  {
    try {
      // Globalize the node
      Node node = (Node) databaseSession.getSystemResolver().globalize(n);
      if (!(node instanceof URIReference)) {
        throw new QueryException(node + " is not a URI reference");
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Model URI for model " + n + " is " + node);
      }
      // Return the protocol
      return ((URIReference) node).getURI().getScheme();
    }
    catch (GlobalizeException e) {
      throw new QueryException("Unable to globalize node " + n, e);
    }
  }

  /**
   * Check if the given model actually refers to a model on the local server.
   *
   * @param modelURI The URI of the model being searched for.
   * @return The Node for the local model, or <code>null</code> if not found.
   * @throws QueryException When the model URI cannot be manipulated.
   */
  private Node getCanonicalAlias(URI modelURI) throws QueryException {
    if (logger.isDebugEnabled()) {
      logger.debug("Checking for an alias on: " + modelURI);
    }

    // extract the host name
    String host = modelURI.getHost();
    if (host == null) {
      return null;
    }
    // Check if this host has been heard of before
    if (metadata.getHostnameAliases().contains(host)) {
      // this name is acceptable, so leave it alone
      return null;
    }
    // Check with a DNS server to see if this host is recognised
    InetAddress addr = null;
    try {
      addr = InetAddress.getByName(host);
    } catch (UnknownHostException uhe) {
      // The host was unknown, so allow resolution to continue as before
      return null;
    }
    // check the various names against known aliases
    if (
        metadata.getHostnameAliases().contains(addr.getHostName()) ||
        metadata.getHostnameAliases().contains(addr.getCanonicalHostName()) ||
        metadata.getHostnameAliases().contains(addr.getHostAddress())
    ) {
      // change the host name to one that is recognised
      return getLocalURI(modelURI);
    }

    // not found, so return nothing
    return null;
  }


  /**
   * Convert a URI to a URIReference which refers to the canonical local machine name.
   *
   * @param uri The URI to update.
   * @return The URIReference representing the same URI as the parameter, with the host name updated.
   * @throws QueryException When the uri cannot be manipulated.
   */
  private Node getLocalURI(URI uri) throws QueryException {
    // use the system model to find the local host name
    String newHost = metadata.getSystemModelURI().getHost();
    // update the URI
    try {
      URI newModelURI = new URI(uri.getScheme(), newHost, uri.getPath(), uri.getFragment());
      logger.debug("Changing model URI from " + uri + " to " + newModelURI);
      return new URIReferenceImpl(newModelURI);
    } catch (URISyntaxException e) {
      throw new QueryException("Internal error.  Model URI cannot be manipulated.");
    }
  }

}
