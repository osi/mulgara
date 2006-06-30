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

package org.mulgara.client.jena.kmodel;
import java.util.*;

//Hewlwtt-Packard packages
import com.hp.hpl.jena.graph.*;

//Kowari packages
import org.kowari.query.*;


/**
 * A Jena BulkUpdateHandler for a KGraph.
 *
 * <p>An instance of this class is usually obtained via a KGraph.</p>
 *
 * @created 2001-08-16
 *
 * @author Chris Wilper
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:34 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2003 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class KBulkUpdateHandler
    implements BulkUpdateHandler {

  /**  */
  private GraphEventManager manager;

  /**  */
  private KGraph graph;

  /**
   * Construct a KBulkUpdateHandler for the given KGraph.
   *
   * @param graph KGraph
   */
  public KBulkUpdateHandler(KGraph graph) {

    //initialise members
    this.graph = graph;
    manager = graph.getEventManager();
  }

  /**
   * Add an array of Triples and notify the GraphEventManager.
   *
   * @param triples Triple[]
   */
  public void add(Triple[] triples) {

    graph.performAdd(triples);
    manager.notifyAddArray(triples);
  }

  /**
   * Add a List of Triples and notify the GraphEventManager.
   *
   * @param triples List
   */
  public void add(List triples) {

    add(triples, true);
  }

  /**
   * Add a List of Triples and optionally notify the GraphEventManager.
   *
   * @param triples List
   * @param notify boolean
   */
  private void add(List triples, boolean notify) {

    //convert to array and add
    graph.performAdd((Triple[]) triples.toArray(new Triple[triples.size()]));

    if (notify) {

      manager.notifyAddList(triples);
    }
  }

  /**
   * Add triples from an Iterator and notify the GraphEventManager.
   *
   * @param iter Iterator
   */
  public void add(Iterator iter) {

    addIterator(iter, true);
  }

  /**
   * Add triples from an Iterator and optionally notify the GraphEventManager.
   *
   * @param iter Iterator
   * @param notify boolean
   */
  public void addIterator(Iterator iter, boolean notify) {

    //convert to List and add
    List list = GraphUtil.iteratorToList(iter);
    add(list, false);

    if (notify) {

      manager.notifyAddIterator(list);
    }
  }

  /**
   * Add all triples from the given Graph, ignoring reifications, then notify
   * the GraphEventManager.
   *
   * @param graph Graph
   */
  public void add(Graph graph) {

    add(graph, false);
  }

  /**
   * Add all triples from the given Graph, optionally including reifications,
   * then notify the GraphEventManager.
   *
   * @param graph Graph
   * @param withReifications boolean
   */
  public void add(Graph graph, boolean withReifications) {

    //get all statements and add
    addIterator(GraphUtil.findAll(graph), false);

    if (withReifications) {

      addReifications(this.graph, graph);
    }

    manager.notifyAddGraph(graph);
  }

  /**
   * Adds the refied statements in 'graph' to 'ours'.
   *
   * @param ours Graph
   * @param graph Graph
   */
  private static void addReifications(Graph ours, Graph graph) {

    Reifier reifier = graph.getReifier();
    Iterator iter = reifier.allNodes();

    //add each reified statement
    while (iter.hasNext()) {

      Node node = (Node) iter.next();
      ours.getReifier().reifyAs(node, reifier.getTriple(node));
    }
  }

  /**
   * Adds the refied statements in 'graph' from 'ours'.
   *
   * @param ours Graph
   * @param graph Graph
   */
  private static void deleteReifications(Graph ours, Graph graph) {

    Reifier reifier = graph.getReifier();
    Iterator iter = reifier.allNodes();

    //remove each reified statement
    while (iter.hasNext()) {

      Node node = (Node) iter.next();
      ours.getReifier().remove(node, reifier.getTriple(node));
    }
  }

  /**
   * Delete an array of Triples and notify the GraphEventManager.
   *
   * @param triples Triple[]
   */
  public void delete(Triple[] triples) {

    graph.performDelete(triples);
    manager.notifyDeleteArray(triples);
  }

  /**
   * Delete a List of Triples and notify the GraphEventManager.
   *
   * @param triples List
   */
  public void delete(List triples) {

    delete(triples, true);
  }

  /**
   * Delete a List of Triples and optionally notify the GraphEventManager.
   *
   * @param triples List
   * @param notify boolean
   */
  private void delete(List triples, boolean notify) {

    //convert to array and delete
    graph.performDelete( (Triple[]) triples.toArray(new Triple[triples.size()]));

    if (notify) {

      manager.notifyDeleteList(triples);
    }
  }

  /**
   * Delete triples from an Iterator and notify the GraphEventManager.
   *
   * @param iter Iterator
   */
  public void delete(Iterator iter) {

    deleteIterator(iter, true);
  }

  /**
   * Delete triples from an Iterator and optionally notify the GraphEventManager.
   *
   * @param iter Iterator
   * @param notify boolean
   */
  public void deleteIterator(Iterator iter, boolean notify) {

    //convert to List and delete
    List list = GraphUtil.iteratorToList(iter);
    delete(list, false);

    if (notify) {

      manager.notifyDeleteIterator(list);
    }
  }

  /**
   * Returns a List of all triples in the graph.
   *
   * @param graph Graph
   * @return List
   */
  private List triplesOf(Graph graph) {

    //value to be returned
    ArrayList list = new ArrayList();

    //get all triples and add
    Iterator iter = graph.find(null, null, null);
    while (iter.hasNext()) {

      list.add(iter.next());
    }

    return list;
  }

  /**
   * Delete all triples from the given Graph, ignoring reifications, then notify
   * the GraphEventManager.
   *
   * @param graph Graph
   */
  public void delete(Graph graph) {

    delete(graph, false);
  }

  /**
   * Delete all triples from the given Graph, optionally including reifications,
   * then notify the GraphEventManager.
   *
   * @param graph Graph
   * @param withReifications boolean
   */
  public void delete(Graph graph, boolean withReifications) {

    if (graph.dependsOn(this.graph)) {

      delete(triplesOf(graph));
    }
    else {

      deleteIterator(GraphUtil.findAll(graph), false);
    }

    if (withReifications) {

      deleteReifications(this.graph, graph);
    }

    manager.notifyDeleteGraph(graph);
  }
}
