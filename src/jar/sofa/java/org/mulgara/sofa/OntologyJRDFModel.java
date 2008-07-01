package org.mulgara.sofa;

/**
 * OntologyJRDFModel.java
 * -----------------------------------------------------------------------------
 * Project           SOFAImpl
 * Package           net.java.dev.sofa.model.jrdf
 * Original author   Keith Ahern
 *                   [keith@tucanatech.com]
 * Created           26.08.2004 21:54:39
 * Revision info     $RCSfile: OntologyJRDFModel.java,v $ $Revision: 1.9 $ $State: Exp $
 *
 * Last modified on  $Date: 2005/01/05 04:59:03 $
 *               by  $Author: newmana $
 *
 * Version: 1.0
 *
 * Copyright (c) 2004 Alex Alishevskikh
 *
 * GNU Lesser General Public License (http://www.gnu.org/copyleft/lesser.txt)
 */

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import net.java.dev.sofa.model.OntologyModel;
import net.java.dev.sofa.model.ThingModel;

import org.jrdf.graph.*;
import org.jrdf.graph.mem.*;
import org.jrdf.util.ClosableIterator;

/**
 * <h4>OntologyJRDFModel</h4>
 *
 * <p>An OntologyModel implmentation backed by JRDF http://jrdf.sourceforge.net/
 * </p>
 *
 * <p><b>Data representation</b><br>
 * The following conventions are used for storing data:
 * <ul>
 * <li>Subjects and Predicates are stored as URI Resources.
 * <li>Objects storage is determined by the type of value:
 * 	<dl>
 * 	<dt>The value is a Thing
 * 	<dd>Objects are stored as resources if the Thing has a URI.
 *  <dt>The value is a datatype
 * 	<dd>The format is <code>&quot;<i>value</i>&quot;^^&lt;<i>datatype</i>&gt;
 * </code>, where <i>value</i> is a string representation of the value and
 * <i>datatype</i> is the datatype identifier.
 *  <dt>The value is a Java class (in the relation range statements)
 * 	<dd>The format is <code>@&lt;<i>datatype</i>&gt;</code>, where
 * <i>datatype</i> is the datatype identifier.
 * 	</dl>
 * </li>
 * </ul>
 * <p>The datatype representation and naming mechanism is conform to
 * <b>net.java.dev.sofa.serialization</b> package conventions.
 *
 * @see ThingJRDFModel
 * @version $Id: OntologyJRDFModel.java,v 1.9 2005/01/05 04:59:03 newmana Exp $
 * @author $Author: newmana $
 */
public class OntologyJRDFModel implements OntologyModel {

  Graph graph;

  public OntologyJRDFModel() {
    try {

      graph = new GraphImpl();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public OntologyJRDFModel(Graph graph) {
    this.graph = graph;
  }

  /**
   * @see net.java.dev.sofa.model.OntologyModel#getThingModel(java.net.URI)
   */
  public ThingModel getThingModel(URI key) {
    return new ThingJRDFModel(this, key);
  }

  /**
   * @see net.java.dev.sofa.model.OntologyModel#contains(java.net.URI)
   */
  public boolean contains(URI key) {

    try {
      return graph.contains(graph.getElementFactory().createResource(key),
          null, null);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return false;
  }

  /**
   * @see net.java.dev.sofa.model.OntologyModel#keys()
   */
  public Set keys() {
    Set s = new HashSet();
    ClosableIterator ci = null;
    try {

      ci = graph.find(null, null, null);

      Triple triple;
      SubjectNode subjectNode;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();

        subjectNode = triple.getSubject();

        if (subjectNode instanceof URIReference) {
          s.add(((URIReference) subjectNode).getURI());
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (ci != null) {
        ci.close();
      }
    }

    return s;

  }

  /**
   * @see net.java.dev.sofa.model.OntologyModel#remove(java.net.URI)
   */
  public boolean remove(URI key) {
    boolean modelChanged = false;

    ClosableIterator ci = null;
    try {
      GraphElementFactory gef = graph.getElementFactory();
      URIReference ref = gef.createResource(key);
      ci = graph.find(ref, null, null);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        graph.remove(triple);

        modelChanged = true;
      }
      ci.close();

      ci = graph.find(null, ref, null);

      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        graph.remove(triple);

        modelChanged = true;
      }
      ci.close();

      ci = graph.find(null, null, ref);

      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        graph.remove(triple);

        modelChanged = true;
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (ci != null) {
        ci.close();
      }
    }

    return modelChanged;
  }

  /**
   * DEBUG: dump()
   *
   */
  public void dump() {

    ClosableIterator ci = null;
    try {
      ci = graph.find(null, null, null);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        System.out.println(triple.getSubject() + "  " + triple.getPredicate()
            + "  " + triple.getObject());

      }
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (ci != null) {
        ci.close();
      }
    }

  }

  protected Graph getGraph() {
    return graph;
  }

}
