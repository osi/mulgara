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

package org.mulgara.jena;

// Java 2 standard packages
import java.net.*;
import java.util.*;

// Apache Log4J
import org.apache.log4j.*;

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.query.QueryHandler}.
 *
 * @created 2004-07-07
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MulgaraQueryHandler implements QueryHandler {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(MulgaraQueryHandler.class.getName());

  /**
   * The current session.
   */
  protected LocalJenaSession session;

  /**
   * The graph object that created this query handler.
   */
  private GraphMulgara graph;

  /**
   * Create a new query handler.
   *
   * @param existingDatabaseSession the session to use for all graph operations.
   *   Cannot be null.
   * @param newGraph the graph that is creating this query handler.
   * @throws JenaException if there was a failure (generally I/O) creating
   *   the database.
   * @throws IllegalArgumentException if the either of the given parameters
   *   are null.
   */
  public MulgaraQueryHandler(LocalJenaSession existingDatabaseSession,
      GraphMulgara newGraph) throws JenaException {

    // Session cannot be null.
    if (existingDatabaseSession == null) {
      throw new IllegalArgumentException("Null \"existingDatabaseSession\" " +
         " parameter");
    }

    // Name of the graph cannot be null.
    if (newGraph == null) {
      throw new IllegalArgumentException("Null \"newGraphURI\" " +
         " parameter");
    }

    // Set the session
    session = existingDatabaseSession;

    // Set graph
    graph = newGraph;
  }

  public Stage patternStage(Mapping map, ExpressionSet constraints, Triple[] t) {
    return new PatternStage(graph, map, constraints, t);
  }

  public BindingQueryPlan prepareBindings(Query q, Node[] variables)
      throws IllegalArgumentException {
    if (!(q instanceof MulgaraQuery)) {
      throw new IllegalArgumentException("Can only use MulgaraQuery objects");
    }

    return new MulgaraQueryPlan((GraphMulgara) graph, (MulgaraQuery) q, variables, session,
        graph.graphURI);
  }

  public TreeQueryPlan prepareTree(Graph pattern) {
    TreeQueryPlan plan = new MulgaraTreeQueryPlan(graph, pattern, session,
        graph.getURI());
    return plan;
  }

  public ExtendedIterator objectsFor(Node s, Node p) {
    return new NodeIterator(graph.find(s, p, Node.ANY), 2);
  }

  public ExtendedIterator subjectsFor(Node p, Node o) {
    return new NodeIterator(graph.find(Node.ANY, p, o), 0);
  }

  public boolean containsNode(Node node) throws JenaException {
    boolean exists = false;

    if (node.isBlank()) {
      exists = graph.contains(node, Node.ANY, Node.ANY) ||
      graph.contains(Node.ANY, Node.ANY, node);
    }
    else if (node.isLiteral()) {
      exists = graph.contains(Node.ANY, Node.ANY, node);
    }
    else if (node.isURI()) {
      exists = graph.contains(node, Node.ANY, Node.ANY) ||
          graph.contains(Node.ANY, node, Node.ANY) ||
          graph.contains(Node.ANY, Node.ANY, node);
    }
    return exists;
  }

  /**
   * An iterator that returns a node object while iterating through a given
   * iterator that contains {@link Triple}s.
   */
  private class NodeIterator extends WrappedIterator {

    /**
     * The index into the triple to return the node.
     */
    private int index;

    /**
     * Create a new iterator.
     *
     * @param newIterator an iterator containing triples.
     * @param newIndex the index 0 = subject, 1 = predicate, 2 = object.
     * @throws IllegalArugmentException if the index is greater than 2.
     */
    public NodeIterator(ExtendedIterator newIterator, int newIndex) {
      super(newIterator);

      if (index > 2) {
        throw new IllegalArgumentException("Index cannot be greater than 2 " +
            "(Object)");
      }

      index = newIndex;
    }

    public Object next() {

      // Return the correct node in the triple.
      Triple tmpTriple = (Triple) super.next();
      if (index == 0) {
        return tmpTriple.getSubject();
      }
      else if (index == 1) {
        return tmpTriple.getPredicate();
      }
      else if (index == 2) {
        return tmpTriple.getObject();
      }

      // This shouldn't happen.
      close();
      throw new NoSuchElementException("Cannot get next element");
    }
  }
}
