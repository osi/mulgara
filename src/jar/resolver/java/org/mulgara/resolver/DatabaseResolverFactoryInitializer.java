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
import java.io.File;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import javax.naming.NamingException;

// Third party packages
import org.apache.log4j.Logger;  // Apache Log4J
import org.jrdf.graph.*;         // JRDF
import org.jrdf.vocabulary.RDF;
import org.mulgara.content.ContentHandler;
import org.mulgara.content.ContentHandlerManager;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.Mulgara;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.*;
import org.mulgara.util.NVPair;
import org.objectweb.jotm.Jotm;  // JOTM transaction manager
import org.objectweb.transaction.jta.TMService;
import org.objectweb.transaction.jta.TransactionManager;

// Local packages

/**
 * Initialiser for {@link ResolverFactory} instances.
 *
 * @created 2004-04-26
 * @author <a href="http://www.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.14 $
 * @modified $Date: 2005/06/10 07:56:41 $
 * @maintenanceAuthor $Author: amuys $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class DatabaseResolverFactoryInitializer
  extends DatabaseFactoryInitializer implements ResolverFactoryInitializer
{
  /** Logger.  */
  private static final Logger logger =
    Logger.getLogger(DatabaseResolverFactoryInitializer.class.getName());

  private final Set cachedResolverFactorySet;
  private final Database database;
  private final DatabaseMetadata metadata;
  private final File persistenceDirectory;
  private final ContentHandlerManager contentHandlerManager;
  private final ResolverFactory systemResolverFactory;

  /**
   * Sole constructor.
   *
   * @throws IllegalArgumentException if the <var>cachedResolveFactorySet</var>,
   *   <var>database</var>, <var>metadata</var>, or
   *   <var>contentHandlerManager</var> arguments are <code>null</code>
   */
  public DatabaseResolverFactoryInitializer(
           Set                   cachedResolverFactorySet,
           Database              database,
           DatabaseMetadata      metadata,
           File                  persistenceDirectory,
           ContentHandlerManager contentHandlerManager,
           ResolverFactory       systemResolverFactory
         )
    throws InitializerException
  {
    super(metadata.getURI(),
          metadata.getHostnameAliases(),
          persistenceDirectory);

    // Validate parameters
    if (cachedResolverFactorySet == null) {
      throw new IllegalArgumentException(
        "Null \"cachedResolverFactorySet\" parameter"
      );
    }
    if (database == null) {
      throw new IllegalArgumentException("database null");
    }
    if (metadata == null) {
      throw new IllegalArgumentException("metadata null");
    }
    if (contentHandlerManager == null) {
      throw new IllegalArgumentException("contentHandlerManager null");
    }

    // Initialize fields
    this.cachedResolverFactorySet = cachedResolverFactorySet;
    this.database                 = database;
    this.metadata                 = metadata;
    this.persistenceDirectory     = persistenceDirectory;
    this.contentHandlerManager    = contentHandlerManager;
    this.systemResolverFactory    = systemResolverFactory;
  }


  //
  // Methods implementing ResolverFactoryInitializer
  //

  public void addModelType(URI modelType, ResolverFactory resolverFactory)
      throws InitializerException {
    database.addModelType(modelType, resolverFactory);
  }

  public void addProtocol(String protocol, ResolverFactory resolverFactory)
      throws InitializerException {
    database.addProtocol(protocol, resolverFactory);
  }

  public void addSymbolicTransformation(SymbolicTransformation symbolicTransformation)
      throws InitializerException {
    database.addSymbolicTransformation(symbolicTransformation);
  }

  public void cacheModelAccess(ResolverFactory resolverFactory)
    throws InitializerException
  {
    if (resolverFactory == null) {
      throw new IllegalArgumentException("Null \"resolverFactory\" parameter");
    }

    cachedResolverFactorySet.add(resolverFactory);
  }

  public ContentHandlerManager getContentHandlers()
  {
    return contentHandlerManager;
  }

  public ResolverFactory getSystemResolverFactory()
    throws NoSystemResolverFactoryException
  {
    if (systemResolverFactory == null) {
      throw new NoSystemResolverFactoryException();
    }
    return systemResolverFactory;
  }

  public long getRdfType() {
    return metadata.getRdfTypeNode();
  }

  public long getSystemModel() {
    checkState();

    return metadata.getSystemModelNode();
  }

  public long getSystemModelType() throws NoSystemResolverFactoryException {
    checkState();

    return metadata.getSystemModelTypeNode();
  }

  public long preallocate(Node node) throws InitializerException {
    if (logger.isDebugEnabled()) {
      logger.debug("Preallocating " + node);
    }

    checkState();

    try {
      //!!FIXME: Can't add preallocate to Session until we switch over.
      DatabaseSession session = (DatabaseSession)database.newSession();
      return session.preallocate(node);
    } catch (QueryException eq) {
      throw new InitializerException("Failed to preallocate node", eq);
    }
  }


  public void registerNewConstraint(ConstraintDescriptor descriptor) throws InitializerException {
//    logger.warn("!!!!!!!!!!!!!!!!Registering new Constraint: " + descriptor, new Throwable());
    Class constraintClass = descriptor.getConstraintClass();
    if (!ConstraintOperations.constraintRegistered(constraintClass)) {
      // FIXME: This needs refactoring.  With the constraint registration in place, ConstraintOperations can be simplifed.
      ConstraintOperations.addConstraintResolutionHandlers(new NVPair[] { new NVPair(constraintClass, descriptor), });
      ConstraintOperations.addConstraintModelRewrites(new NVPair[] { new NVPair(constraintClass, descriptor) });
    } else {
      // FIXME: We need to eliminate the use of static variables (as opposed to constants).
      // FIXME: This will allow multiple database instances within the same JVM
      logger.warn("Attempted to register " + constraintClass + " twice");
    }
  }
}
