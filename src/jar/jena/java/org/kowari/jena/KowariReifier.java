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


package org.kowari.jena;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.vocabulary.RDF;

/**
 * An abstract class which shares the common implementation of a
 * {@link com.hp.hpl.jena.graph.Reifier}.
 *
 * @created 2004-09-24
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
public abstract class KowariReifier implements Reifier {

  /**
   * The parent graph.
   */
  protected GraphKowari parent;

  /**
   * The graph that stores the reification triples - maybe the same as the
   * parent graph depending on configuration.
   */
  protected GraphKowari reificationGraph;

  /**
   * Create a new reifier.
   *
   * @param parent the parent graph.
   * @param style the style of reification.
   */
  public static Reifier create(GraphKowari parent,
      ReificationStyle style) {

    if (style == ReificationStyle.Convenient) {
      return new KowariConvenientReifier(parent, parent.getReificationGraph());
    }
    else if (style == ReificationStyle.Minimal) {
      return new KowariMinimalReifier(parent, parent.getReificationGraph());
    }
    else if (style == ReificationStyle.Standard) {
      return new KowariStandardReifier(parent, parent.getReificationGraph());
    }
    else {
      throw new IllegalArgumentException("Unknown reification style");
    }
  }

  public Graph getParentGraph() {
    return parent;
  }

  public Triple getTriple(Node n) {
    if (reificationGraph.contains(n, RDF.type.asNode(), RDF.Statement.asNode())) {

      // Get subject node.
      Node subject = getObjectNode(n, RDF.subject.asNode());

      // Get predicate node.
      Node predicate = getObjectNode(n, RDF.predicate.asNode());;

      // Get object node.
      Node object = getObjectNode(n, RDF.object.asNode());

      // Create new triple if all are not null.
      if (subject != null && predicate != null && object != null) {
        return new Triple(subject, predicate, object);
      }
    }

    return null;
  }

  public boolean hasTriple(Node n) {
    return
        reificationGraph.contains(n, RDF.type.asNode(), RDF.Statement.asNode()) &&
        reificationGraph.contains(n, RDF.subject.asNode(), null) &&
        reificationGraph.contains(n, RDF.predicate.asNode(), null) &&
        reificationGraph.contains(n, RDF.object.asNode(), null);
  }

  /**
   * Returns the object value of a given triple or null if not found.
   *
   * @param subject the subject to find.
   * @param predicate the predicate to find.
   * @return the object found or null if not found.
   */
  private Node getObjectNode(Node subject, Node predicate) {
    ClosableIterator it = reificationGraph.find(subject, predicate, null);
    try {
      if (it.hasNext()) {
        return ((Triple) it.next()).getObject();
      }
    }
    finally {
      it.close();
    }
    return null;
  }

  public ExtendedIterator allNodes() {
    return new SubjectNodeIterator(new NullFilter(),
        reificationGraph.find(null, RDF.type.asNode(),
        RDF.Statement.asNode()));
  }

  public ExtendedIterator allNodes(Triple t) {
    return allNodes().filterKeep(getMatchingFilter(t));
  }

  public Node reifyAs(Node tag, Triple toReify) {

    // Check if triple exists, throw already reified exception for tag.
    if (reificationGraph.contains(tag, RDF.type.asNode(), RDF.Statement.asNode()) &&
        !(reificationGraph.contains(tag, RDF.subject.asNode(), toReify.getSubject()) &&
          reificationGraph.contains(tag, RDF.predicate.asNode(), toReify.getPredicate()) &&
          reificationGraph.contains(tag, RDF.object.asNode(), toReify.getObject()))) {
      throw new AlreadyReifiedException(tag);
    }

    // Add statements to reification graph.
    graphAddQuad(reificationGraph, tag, toReify);
    return tag;
  }

  public void remove(Node n, Triple t) {
    Triple boundTriple = getTriple(n);
    if ((boundTriple != null) && (boundTriple.equals(t))) {
      graphRemoveQuad(reificationGraph, n, t);
    }
  }

  public void remove(Triple t) {
    ClosableIterator iter = reificationGraph.find(null, RDF.subject.asNode(),
        t.getSubject());
    if (iter.hasNext()) {
      Triple subjectTriple = (Triple) iter.next();
      graphRemoveQuad(reificationGraph, subjectTriple.getSubject(), t);
    }
  }

  public boolean hasTriple(Triple t) {

    // Perform a query against the reification triples - returning true if there
    // was a result found.
    Query q = new KowariQuery();
    Node var = Node.create("?v");
    q.addMatch(var, RDF.subject.asNode(), t.getSubject());
    q.addMatch(var, RDF.predicate.asNode(), t.getPredicate());
    q.addMatch(var, RDF.object.asNode(), t.getObject());
    BindingQueryPlan plan = reificationGraph.queryHandler().prepareBindings(q,
        new Node[] {var} );
    ExtendedIterator iter = plan.executeBindings();
    return iter.hasNext();
  }

  public abstract ReificationStyle getStyle();

  public abstract boolean handledAdd(Triple fragment);

  public abstract boolean handledRemove(Triple fragment);

  public abstract Graph getHiddenTriples();

  public abstract Graph getReificationTriples();

  protected void graphRemoveQuad(Graph g, Node n, Triple t) {
    g.delete(Triple.create(n, RDF.Nodes.type, RDF.Nodes.Statement));
    g.delete(Triple.create(n, RDF.Nodes.subject, t.getSubject()));
    g.delete(Triple.create(n, RDF.Nodes.predicate, t.getPredicate()));
    g.delete(Triple.create(n, RDF.Nodes.object, t.getObject()));
  }

  protected void graphAddQuad(Graph g, Node node, Triple t) {
    g.add(Triple.create(node, RDF.Nodes.subject, t.getSubject()));
    g.add(Triple.create(node, RDF.Nodes.predicate, t.getPredicate()));
    g.add(Triple.create(node, RDF.Nodes.object, t.getObject()));
    g.add(Triple.create(node, RDF.Nodes.type, RDF.Nodes.Statement));
  }

  public String toString() {
    return "<R " + reificationGraph + ">";
  }

  /**
   * Filter that return true if the triples being iterated equals a given value.
   *
   * @param t Triple the triple to match.
   * @return the new filter.
   */
  public Filter getMatchingFilter(final Triple t) {
    return new Filter() {
      public boolean accept(Object o) {
        return t.equals(getTriple((Node) o));
      }
    };
  }

  /**
   * An iterator which takes an extended iterator that return triples and only
   * returns the SubjectNode.
   */
  protected class SubjectNodeIterator extends FilterIterator {
    public SubjectNodeIterator(Filter filter, ExtendedIterator iterator) {
      super(filter, iterator);
    }

    public Object next() {
      Triple triple = (Triple) super.next();
      return triple.getSubject();
    }
  }
}
