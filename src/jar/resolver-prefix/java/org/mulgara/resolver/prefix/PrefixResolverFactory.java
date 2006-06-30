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

package org.mulgara.resolver.prefix;

// Java 2 standard packages
import java.io.*;
import java.net.*;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.vocabulary.*;

// Locally written packages
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.*;

/**
 * Factory for a resolver that gets type information from the string pool
 *
 * @created 2005-4-19
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.1 $
 * @modified $Date: 2005/05/15 00:58:05 $ @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class PrefixResolverFactory implements ResolverFactory
{
  /**
   * Logger.
   */
  private static Logger logger =
    Logger.getLogger(PrefixResolverFactory.class.getName());


  /** The URI for the modelType.  */
  private static final URI modelTypeURI;

  /** The URI for prefixes. */
  private static final URI kowariPrefixURI;

  /** The preallocated local node representing the prefix predicate. */
  private long kowariPrefix;

  static {
    try {
      modelTypeURI = new URI(Mulgara.NAMESPACE + "PrefixModel");
      kowariPrefixURI = new URI(Mulgara.NAMESPACE + "prefix");
      assert modelTypeURI != null;
      assert kowariPrefixURI != null;
    } catch (URISyntaxException e) {
      throw new Error("Bad hardcoded internal URIs for Node Types", e);
    }
  }


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
    kowariPrefix = initializer.preallocate(new URIReferenceImpl(kowariPrefixURI));

    // Claim the type supported by the resolver
    initializer.addModelType(modelTypeURI, this);
  }

  //
  // Methods implementing ResolverFactory
  //

  /**
   * {@inheritDoc ResolverFactory}
   */
  public void close()
  {
    // null implementation
  }

  /**
   * {@inheritDoc ResolverFactory}
   */
  public void delete()
  {
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

    return new PrefixResolver(
        resolverSession, systemResolver, kowariPrefix, modelTypeURI
    );
  }
}
