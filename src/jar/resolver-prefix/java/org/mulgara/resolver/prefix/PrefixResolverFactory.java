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

package org.mulgara.resolver.prefix;

// Java 2 standard packages
import java.net.*;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.*;

/**
 * Factory for a resolver that gets type information from the string pool
 *
 * @created 2005-4-19
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @copyright &copy; 2005 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class PrefixResolverFactory implements ResolverFactory {
  /** Logger. */
  private static Logger logger = Logger.getLogger(PrefixResolverFactory.class.getName());

  /** The URI for the modelType.  */
  private static final URI modelTypeURI = URI.create(Mulgara.NAMESPACE + "PrefixModel");

  /** The URI for prefixes. */
  private static final URI mulgaraPrefixURI = URI.create(Mulgara.NAMESPACE + "prefix");

  /** The preallocated local node representing the prefix predicate. */
  private long mulgaraPrefix;

  //
  // Constructors
  //

  /**
   * Instantiate a {@link PrefixResolverFactory}.
   * @param initializer The environment for the constructor.
   */
  private PrefixResolverFactory(ResolverFactoryInitializer initializer) throws InitializerException {

    // Validate "resolverFactoryInitializer" parameter
    if (initializer == null) {
      throw new IllegalArgumentException("Null \"resolverFactoryInitializer\" parameter");
    }

    // intialize the fields
    mulgaraPrefix = initializer.preallocate(new URIReferenceImpl(mulgaraPrefixURI));

    // Claim the type supported by the resolver
    initializer.addModelType(modelTypeURI, this);
  }

  //
  // Methods implementing ResolverFactory
  //

  /**
   * {@inheritDoc ResolverFactory}
   */
  public void close() {
    // null implementation
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
   * @param resolverFactoryInitializer  the database within which to find or
   *   create the various XML Schema resources
   * @throws InitializerException if the XML Schema resources can't be found or
   *   created
   */
  public static ResolverFactory newInstance(
    ResolverFactoryInitializer resolverFactoryInitializer
  ) throws InitializerException {
    return new PrefixResolverFactory(resolverFactoryInitializer);
  }

  /**
   * Obtain a Node Type resolver.
   *
   * @param resolverSession  the session which this query is local to
   * @param canWrite  {@inheritDoc}; ignored, as these models are read only
   * @throws IllegalArgumentException if <var>resolverSession</var> is
   *   <code>null</code> or canWrite is <code>true</code>
   * @throws ResolverFactoryException {@inheritDoc}
   */
  public Resolver newResolver(
      boolean canWrite, ResolverSession resolverSession, Resolver systemResolver
  ) throws ResolverFactoryException {
    if (logger.isDebugEnabled()) logger.debug("Creating new Prefix resolver");
    return new PrefixResolver(resolverSession, systemResolver, mulgaraPrefix, modelTypeURI);
  }
}
