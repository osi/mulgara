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

package org.mulgara.resolver.memory;

// Java 2 standard packages
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.vocabulary.RDF;

// Locally written packages
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.*;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.xa.SimpleXAResourceException;
import org.mulgara.store.xa.XAResolverSession;
import org.mulgara.store.xa.XAResolverSessionFactory;

/**
 * Resolves constraints in models stored on the Java heap.
 *
 * @created 2004-04-28
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:48 $ @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MemoryResolverFactory implements SystemResolverFactory
{
  /**
   * Logger.
   */
  private static final Logger logger =
    Logger.getLogger(MemoryResolverFactory.class.getName());

  /**
   * The preallocated local node representing the <code>rdf:type</code>
   * property.
   */
  private long rdfType;

  /**
   * The preallocated local node representing models stored on the Java heap.
   */
  private long modelType;

  private static final URI modelTypeURI;
  static {
    try {
      modelTypeURI = new URI(Mulgara.NAMESPACE+"MemoryModel");
      assert modelTypeURI != null;
    } catch (URISyntaxException e) {
      throw new Error("Bad hardcoded memory model URI", e);
    }
  }


  /**
   * The preallocated local node representing the system model (<code>#</code>).
   */
  private long systemModel;

  /**
   * The {@link Stating}s which occur in all models created by resolvers
   * created by this factory.
   */
  private final Set statingSet = new HashSet();

  private XAResolverSessionFactory sessionFactory;

  //
  // Constructors
  //

  /**
   * Instantiate a {@link MemoryResolverFactory}.
   */
  private MemoryResolverFactory(ResolverFactoryInitializer initializer)
      throws InitializerException
  {
    // Validate parameters
    if (initializer == null) {
      throw new IllegalArgumentException(
        "Null 'resolverFactoryInitializer' parameter");
    }

    // Initialize fields
    rdfType = initializer.preallocate(new URIReferenceImpl(RDF.TYPE));

    modelType = initializer.preallocate(new URIReferenceImpl(modelTypeURI));

    systemModel = initializer.getSystemModel();

    // Claim tucana:MemoryModel
    initializer.addModelType(modelTypeURI, this);

    this.sessionFactory = null;
  }


  private MemoryResolverFactory(FactoryInitializer initializer,
                                XAResolverSessionFactory sessionFactory)
      throws InitializerException
  {
    if (initializer == null) {
      throw new IllegalArgumentException("resolverFactoryInitializer null");
    }

    this.sessionFactory = sessionFactory;
  }

  //
  // Methods implementing SystemResolverFactory (excluding newResolver)
  //

  public URI getSystemModelTypeURI()
  {
    return modelTypeURI;
  }


  public void setDatabaseMetadata(DatabaseMetadata metadata) {
    rdfType = metadata.getRdfTypeNode();
    modelType = metadata.getSystemModelTypeNode();
    systemModel = metadata.getSystemModelNode();
  }


  //
  // Methods implementing ResolverFactory
  //

  /**
   * {@inheritDoc ResolverFactory}
   *
   * This is actually a non-operation, because there are no persistent
   * resources.
   */
  public void close()
  {
    // null implementation
  }

  /**
   * {@inheritDoc ResolverFactory}
   *
   * This is actually a non-operation, because there are no persistent
   * resources.
   */
  public void delete()
  {
    // null implementation
  }


  /**
   * Register this resolver upon database startup.
   *
   * @param initializer  the database within which to find or
   *   create the various XML Schema resources
   * @throws InitializerException if the XML Schema resources can't be
   *   found or created
   */
  public static ResolverFactory newInstance(ResolverFactoryInitializer initializer)
      throws InitializerException
  {
    return new MemoryResolverFactory(initializer);
  }


  public static ResolverFactory newInstance(FactoryInitializer initializer,
                                            XAResolverSessionFactory sessionFactory)
      throws InitializerException
  {
    return new MemoryResolverFactory(initializer, sessionFactory);
  }


  public int[] recover() throws SimpleXAResourceException
  {
    return new int[] {};
  }

  public void selectPhase(int phaseNumber) throws IOException,
      SimpleXAResourceException
  {
    throw new SimpleXAResourceException("Unable to selectPhase on MemoryResolver");
  }

  public void clear() throws IOException, SimpleXAResourceException
  {
    return;
  }

  public void clear(int phaseNumber) throws IOException,
      SimpleXAResourceException
  {
    return;
  }

  //
  // newResolver methods.
  //

  /**
   * Obtain a memory resolver.
   *
   * @param resolverSession  the session which this query is local to
   * @param canWrite  {@inheritDoc}; ignored by this implementation
   * @throws IllegalArgumentException if <var>resolverSession</var> is
   *   <code>null</code>
   * @throws ResolverFactoryException {@inheritDoc}
   */
  public Resolver newResolver(boolean canWrite,
      ResolverSession resolverSession, Resolver systemResolver) throws ResolverFactoryException
  {
    return new MemoryResolver(resolverSession,
                              systemResolver,
                              rdfType,
                              systemModel,
                              modelTypeURI,
                              statingSet);
  }


  public SystemResolver newResolver(boolean canWrite) throws ResolverFactoryException
  {
    assert sessionFactory != null;
    try {
      return new MemoryResolver(rdfType, systemModel, modelTypeURI, statingSet,
                                (XAResolverSession) sessionFactory.newWritableResolverSession());
    } catch (ResolverSessionFactoryException er) {
      throw new ResolverFactoryException("Failed to obtain a new ResolverSession", er);
    }
  }
}
