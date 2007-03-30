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

package org.mulgara.resolver.distributed;

// Java 2 standard packages
import java.io.*;
import java.net.*;
import java.util.*;
import javax.transaction.xa.XAResource;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.LocalNode;
import org.mulgara.query.QueryException;
import org.mulgara.resolver.spi.DummyXAResource;
import org.mulgara.resolver.spi.Resolution;
import org.mulgara.resolver.spi.Resolver;
import org.mulgara.resolver.spi.ResolverException;
import org.mulgara.resolver.spi.ResolverFactoryException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;

/**
 * Resolves constraints accessible through a session.
 *
 * @created 2007-03-20
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: $
 * @modified $Date: $
 * @maintenanceAuthor $Author: $
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class DistributedResolver implements Resolver
{
  /** Logger.  */
  private static Logger logger = Logger.getLogger(DistributedResolver.class.getName());

  /** The delegator that resolves the constraint on another server.  */
  private final Delegator delegator;

  /** A map of servers to sessions.  This acts as a cache, and also so we may close the sessions.  */
  private Map<URI,Session> serverToSession = new HashMap<URI,Session>();

  /** A collections of session factories.  This is so we may close these factories.  */
  private Collection<SessionFactory> sessionFactories = new HashSet<SessionFactory>();



  /**
   * Construct a Distributed Resolver.
   * @param resolverSession the session this resolver is associated with.
   * @throws IllegalArgumentException if <var>resolverSession</var> is <code>null</code>
   * @throws ResolverFactoryException if the superclass is unable to handle its arguments
   */
  DistributedResolver(ResolverSession resolverSession) throws ResolverFactoryException {

    if (logger.isDebugEnabled()) logger.debug("Instantiating a distributed resolver");

    // Validate "resolverSession" parameter
    if (resolverSession == null) throw new IllegalArgumentException( "Null \"resolverSession\" parameter");

    delegator = new NetworkDelegator(resolverSession);
  }


  /**
   * Model creation method.  Not supported in this resolver.
   * @throws ResolverException The server should not ask this resolver to create a model.
   */
  public void createModel(long model, URI modelType) throws ResolverException {
    throw new ResolverException("Requesting model creation from a distributed resolver.");
  }


  /**
   * Expose a callback object for enlistment by a transaction manager.
   * Uses a dumy xa resource for the moment, but may need to create a fully
   * functional xa resource which is mapped to a session.
   *
   * @return an {@link XAResource} that can be used by a transaction manager to
   *   coordinate this resolver's participation in a distributed transaction.
   *   For now this is a {@link DummyXAResource} with a 10 second transaction timeout
   * @see javax.resource.spi.ManagedConnection#getXAResource
   */
  public XAResource getXAResource() {
    return new DummyXAResource(
      10  // seconds before transaction timeout
    );
  }


  /**
   * Insert or delete RDF statements in an existing model.
   * This is illegal for this model type.
   *
   * @param model  the local node identifying an existing model
   * @param statements  the {@link Statements} to insert into the
   *   <var>model</var>
   * @param occurs  whether to assert the <var>statements</var>, or (if
   *   <code>false</code>) to deny it
   * @throws ResolverException The server should not ask this resolver to modify data.
   */
  public void modifyModel(long model, Statements statements, boolean occurs) throws ResolverException {
    throw new ResolverException("Distributed models are read only");
  }


  /**
   * Remove the cached model containing the contents of a URL.
   * @throws ResolverException The server should not ask this resolver to modify data.
   */
  public void removeModel(long model) throws ResolverException {
    throw new ResolverException("Distributed models cannot be removed");
  }


  /**
   * Resolve a constraint against an RDF/XML document.
   * @param constraint The constraint pattern to be resolved.
   * @return A resolution for the constraint against a model.
   * @throws IllegalArgumentException The constraint is <code>null</code>, or not set to a non-local model.
   * @throws QueryException There was a problem resolving the constraint.
   */
  public Resolution resolve(Constraint constraint) throws QueryException {
    if (logger.isDebugEnabled()) logger.debug("Resolve " + constraint);

    // validate the parameter
    if (constraint == null) throw new IllegalArgumentException();
    ConstraintElement modelElement = constraint.getElement(3);
    if (!(modelElement instanceof LocalNode)) throw new QueryException("Constraint not set to a distributed model.");
    
    return delegator.resolve(constraint, (LocalNode)modelElement);
  }


  /**
   * Close all sessions and factories used by this resolver.
   */
  public void close() {
    for (Session s: serverToSession.values()) {
      try {
        s.close();
      } catch (QueryException qe) {
        logger.error("Exception while closing session", qe);
      }
    }
    for (SessionFactory sf: sessionFactories) {
      try {
        sf.close();
      } catch (QueryException qe) {
        logger.error("Exception while closing session", qe);
      }
    }
  }


  public void abort() {
    // no-op
  }
}
