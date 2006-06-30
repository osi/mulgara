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

// JRDF
import org.jrdf.graph.Node;

// Locally written packages
import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.server.Session;

/**
 * Base class for related query handler implementations.
 *
 * @created 2003-10-27
 *
 * @author David Makepeace
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:24 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class RelatedQueryHandler {

  /**
   * The session currently being used.
   */
  protected Session session;

  /*
   * Create a new related queries.
   *
   * @param newSession the session being used to do the querying.
   * @throws IllegalArgumentException if the session is not DatabaseSession.
   */
  public RelatedQueryHandler(Session newSession) throws IllegalArgumentException {
    if (!(newSession instanceof DatabaseSession)) {
      throw new IllegalArgumentException("Session must be a DatabaseSession");
    }
    session = newSession;
  }

  /**
   * Performs an inner query required for {@link #related}
   *
   * @param query the query to execute.
   * @return Answer the result of the query.
   * @throws QueryException if there was an error executing the query.
   */
  protected Answer innerQuery(Query query) throws QueryException {
    return ((DatabaseSession) session).innerQuery(query);
  }

  /**
   * Find related RDF nodes.
   *
   * @param baseNode the base RDF node.
   * @param queries the queries to perform
   * @param maxRelated the maximum number of rows to return
   * @param minScore related nodes must have at least this score
   * @return a non-<code>null</code> answer with two columns. The first column
   *      is the RDF node and the second column is the score.
   * @throws QueryException if any of the <var>queries</var> can't be processed
   */
  public abstract Answer related(Node baseNode, Query[] queries,
      int maxRelated, double minScore) throws QueryException;

  /**
   * Show how two nodes are related.
   *
   * @param baseNode the base RDF node.
   * @param relatedNode the related RDF node.
   * @param queries the queries to perform.
   * @param nrPColumns the number of columns in the query results which identify
   *      a category of arc linking the two RDF nodes. The columns used for this
   *      purpose start at column 2 (the second column) of the result.
   * @return a non-<code>null</code> answer with two or more columns. The number
   *      of columns is the value of the <code>nrColumns</code> parameter plus
   *      one. The initial <code>nrPColumns</code> columns identify a particular
   *      category of arc linking the two RDF nodes and the last column is the
   *      score.
   * @throws QueryException if any of the <var>queries</var> can't be processed
   */
  public abstract Answer howRelated(Node baseNode, Node relatedNode,
      Query[] queries, int nrPColumns) throws QueryException;
}
