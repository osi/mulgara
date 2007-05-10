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
 *
 */

package org.mulgara.resolver.distributed;

// Java 2 standard packages
import java.io.*;
import java.net.*;
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverFactory;
import org.mulgara.resolver.spi.ResolverFactoryException;
import org.mulgara.resolver.spi.ResolverFactoryInitializer;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.InitializerException;

/**
 * Factory for a resolver that delegates resolution to another server.
 *
 * @created 2007-03-20
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: $
 * @modified $Date: $
 * @maintenanceAuthor $Author: $
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class DistributedResolverFactory implements ResolverFactory
{
  /** Logger. */
  private static Logger logger = Logger.getLogger(DistributedResolverFactory.class.getName());

  /** Collection of constructed resolvers. */
  private Collection<DistributedResolver> openResolvers = new HashSet<DistributedResolver>();

  /** Protocols which are handled by the served resolver. */
  private static final String[] protocols = new String[] { "rmi" };

  /** Set of the handled protocols. */
  private static Set<String> protocolSet = new HashSet<String>();

  // initialize the set to contain the elements of the array
  static {
    for (String p: protocols) protocolSet.add(p);
  }

  /**
   * Instantiate a {@link DistributedResolverFactory}.
   * @param initializer The system initializer to be registered with.
   * @throws InitializerException An error occurred while registering this resolver type.
   */
  private DistributedResolverFactory(ResolverFactoryInitializer initializer) throws InitializerException {
    // Validate "resolverFactoryInitializer" parameter
    if (initializer == null) {
      throw new IllegalArgumentException("Null \"resolverFactoryInitializer\" parameter");
    }

    // Claim the protocols supported by the resolver, and initialize the local protocol set
    for (String p: protocols) {
      initializer.addProtocol(p, this);
      protocolSet.add(p);
    }
  }

  /**
   * {@inheritDoc ResolverFactory}
   */
  public void close() {
    for (DistributedResolver r: openResolvers) r.close();
  }

  /**
   * {@inheritDoc ResolverFactory}
   */
  public void delete() {
    // null implementation
  }


  /**
   * Register this resolver upon database startup.
   *
   * @param initializer the database within which to find or create
   *        the various XML Schema resources
   * @throws InitializerException if the XML Schema resources can't be found or created
   */
  public static ResolverFactory newInstance(ResolverFactoryInitializer initializer) throws InitializerException {
    return new DistributedResolverFactory(initializer);
  }


  /**
   * Obtain a distributed resolver.
   *
   * @param resolverSession the session which this query is local to
   * @param canWrite {@inheritDoc}; ignored, as these models are read only
   * @throws IllegalArgumentException if <var>resolverSession</var> is
   *         <code>null</code> or canWrite is <code>true</code>
   * @throws ResolverFactoryException {@inheritDoc}
   */
  public Resolver newResolver(
      boolean canWrite, ResolverSession resolverSession, Resolver systemResolver
  ) throws ResolverFactoryException {

    if (resolverSession == null) throw new IllegalArgumentException("No session provided for the resolver!");
    if (canWrite) throw new IllegalArgumentException("Cannot write to a remote model!");
    DistributedResolver r = new DistributedResolver(resolverSession);
    openResolvers.add(r);
    return r;
  }


  /**
   * Gets the protocols recognized by this resolver.
   * @return A set of the recognized protocols.
   */
  public static Set<String> getProtocols() {
    return protocolSet;
  }
}