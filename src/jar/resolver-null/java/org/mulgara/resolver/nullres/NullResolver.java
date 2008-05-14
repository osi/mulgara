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

// Java 2 standard packages
import java.net.URI;

import javax.transaction.xa.XAResource;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.Constraint;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.resolver.spi.*;

/**
 * A resolver for accepting and discarding any data and returning valid empty results.
 *
 * @created May 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class NullResolver implements Resolver {
  /** Logger. */
  private static Logger logger = Logger.getLogger(NullResolver.class.getName());

  /** The URI of the type describing null type graphs.  */
  private URI graphTypeUri;

  /**
   * Construct a null resolver.
   * @param graphTypeUri The URI representing the NULL graph type.
   * @throws IllegalArgumentException if <var>graphTypeUri</var> is <code>null</code>
   */
  NullResolver(URI graphTypeUri) {
    if (graphTypeUri == null) throw new IllegalArgumentException("Null graph type provided.");
    this.graphTypeUri = graphTypeUri;
  }

  /**
   * @see org.mulgara.resolver.spi.Resolver#createModel(long, java.net.URI)
   * Do nothing.
   */
  public void createModel(long graph, URI graphType) throws ResolverException, LocalizeException {
    if (logger.isDebugEnabled()) logger.debug("createGraph called on Null resolver: graph gNode=" + graph);
    if (!graphTypeUri.equals(graphType)) {
      throw new ResolverException("Wrong graph type provided as a Null graph");
    }
  }

  /**
   * @see org.mulgara.resolver.spi.Resolver#modifyModel(long, org.mulgara.resolver.spi.Statements, boolean)
   * Do nothing. If the statements that are being ignored cannot be accessed, then report the problem.
   */
  public void modifyModel(long graph, Statements statements, boolean occurs) throws ResolverException {
    if (logger.isDebugEnabled()) {
      try {
        logger.debug((occurs ? "adding" : "removing") + " up to "+ statements.getRowUpperBound() + " statements");
      } catch (TuplesException e) {
        logger.warn("Called modifyGraph on Null graph with bad statements", e);
      }
    }
  }

  /**
   * @see org.mulgara.resolver.spi.Resolver#removeModel(long)
   * Do nothing.
   */
  public void removeModel(long graph) throws ResolverException {
    if (logger.isDebugEnabled()) logger.debug("calling removeGraph from Null resolver: graph gNode=" + graph);
  }

  /**
   * @see org.mulgara.resolver.spi.Resolver#resolve(org.mulgara.query.Constraint)
   * Return empty results.
   */
  public Resolution resolve(Constraint constraint) throws QueryException {
    return new NullResolution(constraint);
  }

  /** @see org.mulgara.resolver.spi.EnlistableResource#abort() */
  public void abort() { /* no-op */ }

  /** @see org.mulgara.resolver.spi.EnlistableResource#getXAResource() */
  public XAResource getXAResource() {
    return new DummyXAResource(10);
  }

}
