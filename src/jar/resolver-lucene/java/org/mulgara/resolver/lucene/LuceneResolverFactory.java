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

package org.mulgara.resolver.lucene;

// Java 2 standard packages
import java.net.*;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.vocabulary.RDF;

// Locally written packages
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.*;

/**
 * Resolves constraints in models defined by static RDF documents.
 *
 * @created 2004-03-31
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:47 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class LuceneResolverFactory implements ResolverFactory {

  /** Logger. */
  private static Logger logger = Logger.getLogger(LuceneResolverFactory.class.getName());

  private String directory;
  private URI modelTypeURI;

  //
  // Constructors
  //

  /**
   * Instantiate a {@link LuceneResolverFactory}.
   */
  private LuceneResolverFactory(ResolverFactoryInitializer initializer) throws
      InitializerException {
    if (initializer == null) {
      throw new IllegalArgumentException(
          "Null initializer passed to LuceneResolverFactory");
    }

    try {
      modelTypeURI = URI.create(Mulgara.NAMESPACE + "LuceneModel");
      assert modelTypeURI != null;

      // Initialize fields
      directory = initializer.getDirectory().toString();

      // Claim mulgara:LuceneModel
      initializer.addModelType(modelTypeURI, this);

    } catch (NoSystemResolverFactoryException en) {
      throw new InitializerException("Unable to obtain system resolver", en);
    }
  }

  //
  // Methods implementing ResolverFactory
  //

  /**
   * {@inheritDoc ResolverFactory}
   *
   * This is actually a non-operation, because the only persistent resources
   * are outside the database.
   */
  public void close() {
    // null implementation
  }

  /**
   * {@inheritDoc ResolverFactory}
   *
   * This is actually a non-operation, because the only persistent resources
   * are outside the database.
   */
  public void delete() {
    // null implementation
  }

  /**
   * {@inheritDoc}
   * @return <code>null</code> - no default graphs for this resolver
   */
  public Graph[] getDefaultGraphs() { return null; }
  
  /**
   * {@inheritDoc}
   * @return <code>false</code> - this graph does not support exports.
   */
  public boolean supportsExport() {
    return false;
  }

  /**
   * Register this resolver upon database startup.
   *
   * @param resolverFactoryInitializer  the database within which to find or
   *   create the various XML Schema resources
   * @throws InitializerException if the XML Schema resources
   *   can't be found or created
   */
  public static ResolverFactory newInstance(
      ResolverFactoryInitializer resolverFactoryInitializer
      ) throws InitializerException {
    if (logger.isDebugEnabled()) logger.debug("Creating Lucene resolver factory");
    return new LuceneResolverFactory(resolverFactoryInitializer);
  }

  /**
   * Obtain a Lucene resolver.
   *
   * @param resolverSession  the session which this query is local to
   * @param canWrite  {@inheritDoc}; ignored in this implementation
   * @throws IllegalArgumentException if <var>resolverSession</var> is
   *   <code>null</code>
   * @throws ResolverFactoryException {@inheritDoc}
   */
  public Resolver newResolver(boolean canWrite, ResolverSession resolverSession, Resolver systemResolver)
      throws ResolverFactoryException {
    if (logger.isDebugEnabled()) logger.debug("Creating Lucene resolver");
    return canWrite
      ? new LuceneResolver(modelTypeURI, resolverSession, directory, this, true)
      : new ReadOnlyLuceneResolver(modelTypeURI, resolverSession, directory, this);
  }
}
