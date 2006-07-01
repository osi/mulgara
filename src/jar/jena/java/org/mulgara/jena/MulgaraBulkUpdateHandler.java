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

import java.util.*;

// Log4J
import org.apache.log4j.*;

// Locally written classes
import org.mulgara.server.Session;
import org.mulgara.query.*;

// Jena classes
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;

/**
 * A bulk update handler that takes into consideration mass updates of triples.
 * Based on link {@link com.hp.hpl.jena.graph.impl.SimpleBulkUpdateHandler}.
 *
 * @created 2004-03-10
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
public class MulgaraBulkUpdateHandler implements BulkUpdateHandler {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(MulgaraBulkUpdateHandler.class.getName());

  /**
   * The graph.
   */
  private GraphMulgara graphMulgara;

  /**
   * The graph event manager.
   */
  private GraphEventManager manager;

  /**
   * Create a new bulk update handler.
   *
   * @param newGraphMulgara the graph.
   */
  public MulgaraBulkUpdateHandler(GraphMulgara newGraphMulgara) {

    graphMulgara = newGraphMulgara;
    manager = graphMulgara.getEventManager();
  }

  public void add(Triple[] triples) {

    graphMulgara.performAdd(new ArrayList(Arrays.asList(triples)));
    manager.notifyAddArray(triples);
  }

  public void add(List triples) {

    add(triples, true);
  }

  protected void add(List triples, boolean notify) {

    graphMulgara.performAdd(triples);
    if (notify) {

      manager.notifyAddList(triples);
    }
  }

  public void add(Iterator it) {

    addIterator(it, true);
  }

  public void addIterator(Iterator it, boolean notify) {

    List s = GraphUtil.iteratorToList(it);
    add(s, false);

    if (notify) {

      manager.notifyAddIterator(s);
    }
  }

  public void add(Graph g) {

    add(g, false);
  }

  public void add(Graph g, boolean withReifications) {

    addIterator(GraphUtil.findAll(g), false);
    if (withReifications) {

      addReifications(graphMulgara, g);
    }
    manager.notifyAddGraph(g);
  }

  public static void addReifications(Graph ours, Graph g) {

    Reifier r = g.getReifier();
    Iterator it = r.allNodes();
    while (it.hasNext()) {

      Node node = (Node) it.next();
      ours.getReifier().reifyAs(node, r.getTriple(node));
    }
  }

  public static void deleteReifications(Graph ours, Graph g) {

    Reifier r = g.getReifier();
    Iterator it = r.allNodes();
    while (it.hasNext()) {

      Node node = (Node) it.next();
      ours.getReifier().remove(node, r.getTriple(node));
    }
  }

  public void delete(Triple[] triples) {

    graphMulgara.performDelete(new ArrayList(Arrays.asList(triples)));
    manager.notifyDeleteArray(triples);
  }

  public void delete(List triples) {

    delete(triples, true);
  }

  protected void delete(List triples, boolean notify) {

   graphMulgara.performDelete(triples);

    if (notify) {

      manager.notifyDeleteList(triples);
    }
  }

  public void delete(Iterator it) {

    deleteIterator(it, true);
  }

  public void deleteIterator(Iterator it, boolean notify) {

    List L = GraphUtil.iteratorToList(it);
    delete(L, false);
    if (notify) {

      manager.notifyDeleteIterator(L);
    }
  }

  private List triplesOf(Graph g) {

    ArrayList L = new ArrayList();
    Iterator it = g.find(null, null, null);
    while (it.hasNext()) {

      L.add(it.next());
    }
    return L;
  }

  public void delete(Graph g) {

    delete(g, false);
  }

  public void delete(Graph g, boolean withReifications) {

    if (g.dependsOn(graphMulgara)) {

      delete(triplesOf(g));
    }
    else {

      deleteIterator(GraphUtil.findAll(g), false);
    }
    if (withReifications) {

      deleteReifications(graphMulgara, g);
    }
    manager.notifyDeleteGraph(g);
  }
}
