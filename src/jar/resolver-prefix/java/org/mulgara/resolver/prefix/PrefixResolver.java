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
 * Contributor(s):
 *   getModel() contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
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
import java.util.*;
import javax.transaction.xa.XAResource;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.*;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.resolver.*;
import org.mulgara.resolver.spi.*;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.store.stringpool.SPObject;
import org.mulgara.store.stringpool.SPObjectFactory;
import org.mulgara.store.stringpool.SPURI;
import org.mulgara.store.stringpool.StringPoolException;
import org.mulgara.store.stringpool.xa.SPObjectFactoryImpl;
import org.mulgara.store.tuples.LiteralTuples;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.tuples.TuplesOperations;

/**
 * Resolves constraints accessible through a session.
 *
 * @created 2005-4-19
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.1 $
 * @modified $Date: 2005/05/15 00:58:05 $ @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class PrefixResolver implements Resolver
{
  /** Logger.  */
  private static Logger logger = Logger.getLogger(PrefixResolver.class.getName());

  /** The session that this resolver is associated with.  */
  private final ResolverSession resolverSession;

  /** A map of servers to sessions.  This acts as a cache, and also so we may close the sessions.  */
  private Map sessions;

  /** The URI of the type describing node type models.  */
  private URI modelTypeURI;

  /** The preallocated local node representing the mulgara:prefix property. */
  private long mulgaraPrefix;

  //
  // Constructors
  //

  /**
   * Construct a resolver.
   *
   * @param resolverSession  the session this resolver is associated with
   * @throws IllegalArgumentException  if <var>resolverSession</var> is
   *   <code>null</code>
   * @throws ResolverFactoryException  if the superclass is unable to handle its arguments
   */
  PrefixResolver(
      ResolverSession resolverSession,
      Resolver systemResolver,
      long mulgaraPrefix,
      URI modelTypeURI
  ) throws ResolverFactoryException {

    if (logger.isDebugEnabled()) {
      logger.debug("Instantiating a node type resolver");
    }

    // Validate "resolverSession" parameter
    if (resolverSession == null) {
      throw new IllegalArgumentException( "Null \"resolverSession\" parameter");
    }

    // Initialize fields
    this.resolverSession = resolverSession;
    this.modelTypeURI = modelTypeURI;
    this.mulgaraPrefix = mulgaraPrefix;
    sessions = new Hashtable();
  }

  //
  // Methods implementing Resolver
  //

  /**
   * Create a model for node types.
   *
   * @param model  {@inheritDoc}.
   * @param modelTypeUri  {@inheritDoc}.  This must be the URI for Prefix models.
   */
  public void createModel(long model, URI modelTypeUri) throws ResolverException {

    if (logger.isDebugEnabled()) {
      logger.debug("Create type model " + model);
    }

    if (!this.modelTypeURI.equals(modelTypeUri)) {
      throw new ResolverException("Wrong model type provided as a Prefix model");
    }
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
  public XAResource getXAResource()
  {
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
   * @throws ResolverException if the <var>statements</var> can't be
   *   added to the <var>model</var>
   */
  public void modifyModel(long model, Statements statements, boolean occurs)
    throws ResolverException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Modify prefix model " + model);
    }

    throw new ResolverException("Prefix models are read only");
  }


  /**
   * Remove the cached model containing the contents of a URL.
   */
  public void removeModel(long model) throws ResolverException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Remove prefix model " + model);
    }
  }

  /**
   * Resolve a constraint against an RDF/XML document.
   *
   * Resolution is by filtration of a URL stream, and thus very slow.
   */
  public Resolution resolve(Constraint constraint) throws QueryException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Resolve " + constraint);
    }

    // Validate "constraint" parameter
    if (constraint == null) {
      throw new IllegalArgumentException("Null \"constraint\" parameter");
    }

    if (!(constraint.getModel() instanceof LocalNode)) {
      logger.warn("Ignoring solutions for " + constraint);
      return new EmptyResolution(constraint, false);
    }

    if (
        !(constraint.getElement(1) instanceof LocalNode) ||
        !(constraint.getElement(2) instanceof LocalNode)
    ) {
      throw new QueryException("Prefix resolver can only be used for fixed prefixes: " + constraint);
    }

    try {

      long property = ((LocalNode)constraint.getElement(1)).getValue();
      LocalNode object = (LocalNode)constraint.getElement(2);
      Node prefixNode = resolverSession.globalize(object.getValue());

      // check the constraint for consistency
      if (property != mulgaraPrefix || !(prefixNode instanceof Literal || prefixNode instanceof URIReference)) {
        logger.error("property = " + property +", mulgaraPrefix = " + mulgaraPrefix);
        logger.error("element(2): " + prefixNode + " [" + prefixNode.getClass().getName() + "]");
        throw new QueryException("Prefix resolver can only be used for prefix constraints: " + constraint);
      }

      String prefix;
      // extract the string from the literal
      if (prefixNode instanceof Literal) {
        prefix = ((Literal)prefixNode).getLexicalForm();
      } else {
        prefix = ((URIReference)prefixNode).getURI().toString();
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Evaluating " + constraint.getElement(0) +
            " has prefix " +
            constraint.getElement(2));
      }
      URI startPrefixUri;
      URI endPrefixUri;
      try {
        startPrefixUri = new URI(prefix);
        endPrefixUri = new URI(prefix + Character.MAX_VALUE);
      } catch (URISyntaxException e) {
        throw new QueryException("Prefix resolver can only be used for URI prefixes: " + e.getMessage());
      }

      ConstraintElement node = constraint.getElement(0);
      assert node != null;

      Tuples tuples;

      if (node instanceof Variable) {
        // extract the variable from the constraint
        Variable variable = (Variable)node;

        // convert the prefix into a string pool object
        SPObjectFactory spoFact = SPObjectFactoryImpl.getInstance();
        SPURI startPrefixObj = spoFact.newSPURI(startPrefixUri);
        SPURI endPrefixObj = spoFact.newSPURI(endPrefixUri);

        // get the extents of the prefix from the string pool
        tuples = resolverSession.findStringPoolRange(startPrefixObj, true, endPrefixObj, false);
        assert tuples != null;
        // rename variables away from subject, predicate and object
        tuples.renameVariables(constraint);

        long resultSize;
        try {
          // Get the size of the final result.
          resultSize = tuples.getRowCount();
        } catch (TuplesException e) {
          throw new QueryException("Unable to build result", e);
        }


        if (logger.isDebugEnabled()) {
          try {
            logger.debug("tuples size = " + tuples.getRowCount() + " (should be " + resultSize + ")");
          } catch (TuplesException e) {
            logger.debug("Error getting the length of the tuples object");
          }
        }

        return new TuplesWrapperResolution(tuples, constraint);

      } else {    // if (node instanceof Variable)
        
        // node must therefore be an instanceof LocalNode
        // we can shortcut the process here
        assert node instanceof LocalNode;
        LocalNode n = (LocalNode)node;

        // get the node out of the string pool
        SPObject spo = resolverSession.findStringPoolObject(n.getValue());

        // check that the node exists
        if (spo == null) {
          tuples = TuplesOperations.empty();
        } else {

          // see if the node starts with the required prefix
          if (spo.getLexicalForm().startsWith(prefix)) {
            tuples = TuplesOperations.unconstrained();
          } else {
            tuples = TuplesOperations.empty();
          }

        }

      }

      // convert the tuples to a resolution
      return new TuplesWrapperResolution(tuples, constraint);

    } catch (GlobalizeException ge) {
      throw new QueryException("Couldn't convert internal data into a string", ge);
    } catch (StringPoolException e) {
      throw new QueryException("Couldn't query constraint", e);
    }

  }


  /**
   * Close all sessions and factories used by this resolver.
   */
  public void close() {
    // no-op
  }

  public void abort() {}
}
