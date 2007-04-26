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
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

// Java 2 enterprise packages
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.InvalidTransactionException;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.*;

// Local packages
import org.mulgara.content.Content;
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerManager;
import org.mulgara.content.ContentLoader;
import org.mulgara.query.*;
import org.mulgara.query.rdf.*;
import org.mulgara.resolver.spi.*;
import org.mulgara.resolver.url.URLResolver;
import org.mulgara.server.Session;
import org.mulgara.store.nodepool.NodePool;

/**
 * An {@link Operation} that implements the {@link Session#createModel} method.
 *
 * @created 2004-11-24
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/02/22 08:16:08 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class CreateModelOperation implements Operation
{
  /**
   * Logger.
   *
   * This is named after the class.
   */
  private static final Logger logger =
    Logger.getLogger(CreateModelOperation.class.getName());

  /**
   * The URI of the model to be created.
   */
  private final URI modelURI;

  /**
   * The URI of the type of the model to be created.
   */
  private URI modelTypeURI;

  //
  // Constructor
  //

  /**
   * Sole constructor.
   *
   * @param modelURI  the {@link URI} of the model to be created, never
   *   <code>null</code>
   * @param modelTypeURI  thie {@link URI} of the type of model to create, or
   *   <code>null</code> for the same type as the system model (<code>#</code>)
   * @throws IllegalArgumentException if <var>modelURI</var> is
   *   <code>null</code>
   */
  CreateModelOperation(URI modelURI, URI modelTypeURI) throws QueryException
  {
    // Validate "modelURI" parameter
    if (modelURI == null) {
      throw new IllegalArgumentException("Null \"modelURI\" parameter");
    }
    if (modelURI.getFragment() == null) {
      throw new QueryException(
          "Model URI does not have a fragment (modelURI:\"" + modelURI + "\")"
      );
    }

    // Initialize fields
    this.modelURI     = modelURI;
    this.modelTypeURI = modelTypeURI;
  }

  //
  // Methods implementing Operation
  //

  public void execute(OperationContext       operationContext,
                      SystemResolver         systemResolver,
                      ResolverSessionFactory resolverSessionFactory,
                      DatabaseMetadata       metadata) throws Exception
  {
    // Default to the system model type
    if (modelTypeURI == null) {
      modelTypeURI = metadata.getSystemModelTypeURI();
    }

    // Verify that the model URI is relative to the database URI.  The model
    // URI can use one of the hostname aliases instead of the canonical
    // hostname of the database URI.  No checking of the scheme specific part
    // of the model URI is performed if the database URI is opaque.
    boolean badModelURI = true;
    URI databaseURI = metadata.getURI();
    String scheme = modelURI.getScheme();
    String fragment = modelURI.getFragment();
    if (scheme != null && scheme.equals(databaseURI.getScheme()) &&
        fragment != null) {
      if (databaseURI.isOpaque()) {
        // databaseURI is opaque.
        if (modelURI.isOpaque()) {
          // Strip out the query string.
          String ssp = modelURI.getSchemeSpecificPart();
          int qIndex = ssp.indexOf('?');
          if (qIndex >= 0) {
            ssp = ssp.substring(0, qIndex);
          }

          if (ssp.equals(databaseURI.getSchemeSpecificPart())) {
            // modelURI is relative to databaseURI.
            badModelURI = false;
          }
        }
      } else {
        // databaseURI is hierarchial.
        String path;
        String host;

        if (
            !modelURI.isOpaque() && (
                modelURI.getSchemeSpecificPart().equals(
                    databaseURI.getSchemeSpecificPart()
                ) || (
                    (host = modelURI.getHost()) != null &&
                    modelURI.getPort() == databaseURI.getPort() &&
                    (path = modelURI.getPath()) != null &&
                    path.equals(databaseURI.getPath()) &&
                    metadata.getHostnameAliases().contains(host.toLowerCase())
                )
            )
        ) {
          // modelURI is relative to databaseURI.
          badModelURI = false;
        }
      }
    }
    if (badModelURI) {
      throw new QueryException(
          "Model URI is not relative to the database URI (modelURI:\"" +
          modelURI + "\", databaseURI:\"" + databaseURI + "\")"
      );
    }

    // Look up the resolver factory for the model type
    ResolverFactory resolverFactory =
      operationContext.findModelTypeResolverFactory(modelTypeURI);
    if (resolverFactory == null) {
      throw new QueryException(
          "Couldn't find resolver factory in internal resolver map " +
          modelTypeURI);
    }

    // PREVIOUSLY WITHIN TRANSACTION

    // Obtain an appropriate resolver bound to this session
    Resolver resolver = operationContext.obtainResolver(resolverFactory);
    assert resolver != null;

    // Find the local node identifying the model
    long model = systemResolver.localizePersistent(new URIReferenceImpl(
        modelURI));
    assert model != NodePool.NONE;

    // Check model does not already exist with a different model type.
    // TODO: there's a node leak here, if the model has already been created.
    Resolution resolution = systemResolver.resolve(new ConstraintImpl(
        new LocalNode(model),
        new LocalNode(metadata.getRdfTypeNode()),
        new Variable("x"),
        new LocalNode(metadata.getSystemModelNode())));

    try {
      resolution.beforeFirst();
      if (resolution.next()) {
        Node eNode = systemResolver.globalize(resolution.getColumnValue(0));
        try {
          URIReferenceImpl existing = (URIReferenceImpl)eNode;
          if (!new URIReferenceImpl(modelTypeURI).equals(existing)) {
            throw new QueryException(modelURI + " already exists with model type " + existing +
                " in attempt to create it with type " + modelTypeURI);
          }
        } catch (ClassCastException ec) {
          throw new QueryException("Invalid model type entry in system model: " + modelURI + " <rdf:type> " + eNode);
        }
      }
    } finally {
      resolution.close();
    }


    // TODO: there's a node leak here, because the model node was created
    //       persistently, but may never end up linked into the graph if the
    //       following security check doesn't succeed

    // Make sure security adapters are satisfied
    for (Iterator i = operationContext.getSecurityAdapterList().iterator();
         i.hasNext();)
    {
      SecurityAdapter securityAdapter = (SecurityAdapter) i.next();

      // Tell the truth to the user
      if (!securityAdapter.canCreateModel(model, systemResolver) ||
          !securityAdapter.canSeeModel(model, systemResolver))
      {
        throw new QueryException("You aren't allowed to create " + modelURI);
      }
    }

    // Use the session to create the model
    resolver.createModel(model, modelTypeURI);
  }

  /**
   * @return <code>true</code>
   */
  public boolean isWriteOperation()
  {
    return true;
  }
}
