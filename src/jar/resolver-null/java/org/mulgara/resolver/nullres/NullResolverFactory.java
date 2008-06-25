/*
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */

package org.mulgara.resolver.nullres;

// Third party packages
import java.net.URI;

import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.resolver.spi.*;

/**
 * The registered factory for creating a Null resolver.
 *
 * @created May 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class NullResolverFactory implements ResolverFactory {
  /** Logger. */
  @SuppressWarnings("unused")
  private static Logger logger = Logger.getLogger(NullResolverFactory.class.getName());

  /** The URI to be used for a null graph */
  public final static URI DEFAULT_GRAPH = URI.create(Mulgara.NULL_GRAPH);

  /** The URI for the graphType.  */
  private static final URI nullTypeURI = URI.create(Mulgara.NAMESPACE + "Null");

  /**
   * Instantiate a {@link NullResolverFactory}.
   */
  private NullResolverFactory(ResolverFactoryInitializer resolverFactoryInitializer) throws InitializerException {
    // Validate "resolverFactoryInitializer" parameter
    if (resolverFactoryInitializer == null) throw new IllegalArgumentException("Null \"resolverFactoryInitializer\" parameter");

    // No need to claim the type supported by the resolver as this is detected in the default graph
  }

  /** {@inheritDoc ResolverFactory} */
  public void close() {
    // null implementation
  }

  /** {@inheritDoc ResolverFactory} */
  public void delete() {
    // null implementation
  }

  /**
   * {@inheritDoc}
   * @return <code>null</code> - no default graphs for this resolver
   */
  public Graph[] getDefaultGraphs() {
    return new Graph[] { new Graph(DEFAULT_GRAPH, nullTypeURI) };
  }
  
  /**
   * {@inheritDoc}
   * @return <code>false</code> - this graph does not support exports.
   */
  public boolean supportsExport() {
    return false;
  }

  /**
   * Register this resolver upon database startup.
   * @param resolverFactoryInitializer The database within which to find or create the various XML Schema resources
   * @throws InitializerException If the XML Schema resources can't be found or created
   */
  public static ResolverFactory newInstance(ResolverFactoryInitializer resolverFactoryInitializer) throws InitializerException {
    return new NullResolverFactory(resolverFactoryInitializer);
  }

  /**
   * Obtain a file resolver.
   * @param resolverSession  the session which this query is local to
   * @param canWrite {@inheritDoc}; ignored in this implementation
   * @throws IllegalArgumentException if <var>resolverSession</var> is <code>null</code>
   * @throws ResolverFactoryException {@inheritDoc}
   */
  public Resolver newResolver(boolean canWrite, ResolverSession resolverSession, Resolver systemResolver) throws ResolverFactoryException {
    return new NullResolver(nullTypeURI);
  }
}
