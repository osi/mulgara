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
import java.net.URI;
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.*;

// Local packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.*;
import org.mulgara.resolver.spi.*;
import org.mulgara.server.Session;
import org.mulgara.store.nodepool.NodePool;

/**
 * An {@link Operation} that implements the {@link Session#createModel} method.
 * TODO: Rename to CreateGraphOperation.
 *
 * @created 2004-11-24
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.9 $
 * @modified $Date: 2005/02/22 08:16:08 $ by $Author: newmana $
 * @maintenanceAuthor $Author: newmana $
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana  Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class CreateModelOperation implements Operation {
  /** Logger. This is named after the class. */
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(CreateModelOperation.class.getName());

  /** The URI of the model to be created. */
  private final URI graphURI;

  /** The URI of the type of the model to be created. */
  private URI graphTypeURI;


  /**
   * Sole constructor.
   *
   * @param graphURI  the {@link URI} of the graph to be created, never <code>null</code>
   * @param graphTypeURI  thie {@link URI} of the type of graph to create, or
   *       <code>null</code> for the same type as the system graph (<code>#</code>)
   * @throws IllegalArgumentException if <var>graphURI</var> is <code>null</code>
   */
  CreateModelOperation(URI graphURI, URI graphTypeURI) throws QueryException {
    // Validate "graphURI" parameter
    if (graphURI == null) throw new IllegalArgumentException("Null \"graphURI\" parameter");
    if (!graphURI.isOpaque() && fragmentScheme(graphURI) && graphURI.getFragment() == null) {
      throw new QueryException("Graph URI does not have a fragment (graphURI:\"" + graphURI + "\")");
    }

    // Initialize fields
    this.graphURI     = graphURI;
    this.graphTypeURI = graphTypeURI;
  }

  //
  // Methods implementing Operation
  //

  public void execute(OperationContext       operationContext,
                      SystemResolver         systemResolver,
                      DatabaseMetadata       metadata) throws Exception {
    // Default to the system graph type
    if (graphTypeURI == null) graphTypeURI = metadata.getSystemModelTypeURI();

    verifyGraphUri(graphURI, metadata);
    
    // Look up the resolver factory for the model type
    ResolverFactory resolverFactory = operationContext.findModelTypeResolverFactory(graphTypeURI);
    if (resolverFactory == null) {
      throw new QueryException("Couldn't find resolver factory in internal resolver map " + graphTypeURI);
    }

    // PREVIOUSLY WITHIN TRANSACTION

    // Obtain an appropriate resolver bound to this session
    Resolver resolver = operationContext.obtainResolver(resolverFactory);
    assert resolver != null;

    // Find the local node identifying the model
    long graph = systemResolver.localizePersistent(new URIReferenceImpl(graphURI));
    assert graph != NodePool.NONE;

    // Check model does not already exist with a different model type.
    // TODO: there's a node leak here, if the model has already been created.
    Resolution resolution = systemResolver.resolve(new ConstraintImpl(
        new LocalNode(graph),
        new LocalNode(metadata.getRdfTypeNode()),
        new Variable("x"),
        new LocalNode(metadata.getSystemModelNode())));

    try {
      resolution.beforeFirst();
      if (resolution.next()) {
        Node eNode = systemResolver.globalize(resolution.getColumnValue(0));
        try {
          URIReferenceImpl existing = (URIReferenceImpl)eNode;
          if (!new URIReferenceImpl(graphTypeURI).equals(existing)) {
            throw new QueryException(graphURI + " already exists with model type " + existing +
                " in attempt to create it with type " + graphTypeURI);
          }
        } catch (ClassCastException ec) {
          throw new QueryException("Invalid model type entry in system model: " + graphURI + " <rdf:type> " + eNode);
        }
      }
    } finally {
      resolution.close();
    }


    // TODO: there's a node leak here, because the model node was created
    //       persistently, but may never end up linked into the graph if the
    //       following security check doesn't succeed

    // Make sure security adapters are satisfied
    for (Iterator<SecurityAdapter> i = operationContext.getSecurityAdapterList().iterator(); i.hasNext();) {
      SecurityAdapter securityAdapter = i.next();

      // Tell the truth to the user
      if (!securityAdapter.canCreateModel(graph, systemResolver) || !securityAdapter.canSeeModel(graph, systemResolver)) {
        throw new QueryException("You aren't allowed to create " + graphURI);
      }
    }

    // Use the session to create the model
    resolver.createModel(graph, graphTypeURI);
  }

  /**
   * @return <code>true</code>
   */
  public boolean isWriteOperation() {
    return true;
  }

  /**
   * Verify that the graph URI is relative to the database URI.  The graph
   * URI can use one of the hostname aliases instead of the canonical
   * hostname of the database URI.  No checking of the scheme specific part
   * of the graph URI is performed if the database URI is opaque.
   * @param graphURI
   * @param metadata
   * @throws QueryException
   */
  private void verifyGraphUri(URI graphURI, DatabaseMetadata metadata) throws QueryException {
    // only both if this is a scheme which uses fragments - for the moment only RMI
    if (!fragmentScheme(graphURI)) return;

    boolean badModelURI = true;
    URI databaseURI = metadata.getURI();
    String scheme = graphURI.getScheme();
    String fragment = graphURI.getFragment();

    if (scheme != null && scheme.equals(databaseURI.getScheme()) && fragment != null) {
      if (databaseURI.isOpaque()) {
        // databaseURI is opaque.
        if (graphURI.isOpaque()) {
          // Strip out the query string.
          String ssp = graphURI.getSchemeSpecificPart();
          int qIndex = ssp.indexOf('?');
          if (qIndex >= 0) ssp = ssp.substring(0, qIndex);

          if (ssp.equals(databaseURI.getSchemeSpecificPart())) {
            // graphURI is relative to databaseURI.
            badModelURI = false;
          }
        }
      } else {
        // databaseURI is hierarchial.
        String path;
        String host;

        if (
            !graphURI.isOpaque() && (
                graphURI.getSchemeSpecificPart().equals(
                    databaseURI.getSchemeSpecificPart()
                ) || (
                    (host = graphURI.getHost()) != null &&
                    graphURI.getPort() == databaseURI.getPort() &&
                    (path = graphURI.getPath()) != null &&
                    path.equals(databaseURI.getPath()) &&
                    metadata.getHostnameAliases().contains(host.toLowerCase())
                )
            )
        ) {
          // graphURI is relative to databaseURI.
          badModelURI = false;
        }
      }
    } else {
      badModelURI = !graphURI.isOpaque();
    }

    if (badModelURI) {
      throw new QueryException(
          "Model URI is not relative to the database URI (graphURI:\"" +
          graphURI + "\", databaseURI:\"" + databaseURI + "\")"
      );
    }
  }

  /** Schemes with fragments are handled for backward compatibility */
  private static final Set<String> fragmentSchemes = new HashSet<String>();
  static {
    fragmentSchemes.add("rmi");
    fragmentSchemes.add("beep");
  }

  /**
   * Test if the given URI is in a scheme which differentiates graphs based on fragments.
   * @param graphUri The URI to test for the graph.
   * @return <code>true</code> only of the URI is in the known graph schemes.
   */
  private static boolean fragmentScheme(URI graphUri) {
    return fragmentSchemes.contains(graphUri.getScheme());
  }
}
