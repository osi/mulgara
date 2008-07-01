package org.mulgara.sofa;
/**
 * ThingJRDFModel.java
 * -----------------------------------------------------------------------------
 * Project           SOFAImpl
 * Package           net.java.dev.sofa.model.jrdf
 * Original author   Keith Ahern
 *                   [keith@tucanatech.com]
 * Created           26.08.2004 21:55:05
 * Revision info     $RCSfile: ThingJRDFModel.java,v $ $Revision: 1.9 $ $State: Exp $
 *
 * Last modified on  $Date: 2005/01/05 04:59:03 $
 *               by  $Author: newmana $
 *
 * Version: 1.0
 *
 * Copyright (c) 2004 Keith Ahern
 *
 * GNU Lesser General Public License (http://www.gnu.org/copyleft/lesser.txt)
 */

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import org.jrdf.graph.Graph;
import org.jrdf.graph.GraphElementFactory;
import org.jrdf.graph.Literal;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.Triple;
import org.jrdf.graph.URIReference;
import org.jrdf.util.ClosableIterator;

import net.java.dev.sofa.*;
import net.java.dev.sofa.model.OntologyModel;
import net.java.dev.sofa.model.ThingModel;
import net.java.dev.sofa.serialize.Util;

/**
 * <h4>ThingJRDFModel</h4>
 *
 * @version $Id: ThingJRDFModel.java,v 1.9 2005/01/05 04:59:03 newmana Exp $
 * @author $Author: newmana $
 */
public class ThingJRDFModel implements ThingModel {

  OntologyJRDFModel _ojm;
  URI _uri;

  protected ThingJRDFModel(OntologyJRDFModel ojm, URI uri) {
    _ojm = ojm;
    _uri = uri;
  }

  /**
   * @see ThingModel#getURI()
   */
  public URI getURI() {
    return _uri;
  }

  /**
   * @see ThingModel#getOntologyModel()
   */
  public OntologyModel getOntologyModel() {
    return _ojm;
  }

  Object parse(String s) {
    if ((s.startsWith("<")) && (s.endsWith(">"))) {
      URI uri;
      try {
        uri = new URI(s.substring(1, s.length() - 1).trim());
      } catch (URISyntaxException e) {
        e.printStackTrace();
        return null;
      }
      if (_ojm.contains(uri))
        return new ThingJRDFModel(_ojm, uri);
      else {
        return uri;
        /*
         * Object tm = OntoConnector.getInstance().getThingModel(uri); if (tm !=
         * null) return tm; else return uri;
         */
      }
    } else if ((s.startsWith("@<")) && (s.endsWith(">"))) {
      String cname = s.substring(2, s.length() - 1).trim();
      try {
        return Class.forName(Util.getJavaDatatype(cname));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    } else if (s.startsWith("\"")) {
      String[] d = s.split("\\^\\^<");
      if (d.length < 2)
        throw new ArrayIndexOutOfBoundsException(s);
      String data = d[0].substring(1, d[0].length() - 1).trim();
      String type = d[1].substring(0, d[1].length() - 1).trim();
      Class jclass = Object.class;
      try {
        jclass = Class.forName(Util.getJavaDatatype(type));
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      return Util.deexternalize(data, jclass);
    }
    return s;
  }

  Object wrap(Object o) {
    if (o instanceof Thing)
      return ((Thing) o).getURI();
    else if (o instanceof Class)
      return "@<" + Util.getXsdDatatype(((Class) o).getName()) + ">";
    else
      return "\"" + Util.externalize(o) + "\"^^<"
          + Util.getXsdDatatype(o.getClass().getName()) + ">";
  }

  /**
   * @see ThingModel#getObjects(URI)
   */
  public Set getObjects(URI p) {

    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      // return where thing is a subject
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();

      ci = jGraph.find(gef.createResource(_uri), gef.createResource(p), null);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();

        Object o = triple.getObject();
        if (o instanceof URIReference) {
          // resource
          s.add(new ThingJRDFModel(_ojm, ((URIReference) o).getURI()));
        } else if (o instanceof Literal) {
          // literal
          s.add(parse(((Literal) o).getLexicalForm()));
        }
        else {
          s.add(parse(o.toString()));
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
   * @see ThingModel#getSubjects(URI)
   */
  public Set getSubjects(URI p) {

    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();
      ci = jGraph.find(null, gef.createResource(p), gef.createResource(_uri));

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        s.add(new ThingJRDFModel(_ojm, ((URIReference) triple.getSubject())
            .getURI()));
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
   * @see ThingModel#getObjects()
   */
  public Set getObjects() {

    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();

      ci = jGraph.find(jGraph.getElementFactory().createResource(_uri), null,
          null);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();

        Object o = triple.getObject();
        if (o instanceof URIReference) {
          // resource
          s.add(new ThingJRDFModel(_ojm, ((URIReference) o).getURI()));
        } else {
          // literal
          s.add(parse(o.toString()));
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
   * @see ThingModel#getSubjects()
   */
  public Set getSubjects() {
    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      // return where thing is a object
      Graph jGraph = _ojm.getGraph();

      ci = jGraph.find(null, null, jGraph.getElementFactory().createResource(
          _uri));

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        s.add(new ThingJRDFModel(_ojm, ((URIReference) triple.getSubject())
            .getURI()));
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
   * @see ThingModel#put(URI, Object)
   */
  public boolean put(URI p, Object object) {

    boolean newTriple = false;

    try {

      Graph graph = _ojm.getGraph();
      GraphElementFactory gef = graph.getElementFactory();

      // test of it exists first, we need to return if we've changed the model
      SubjectNode subjectNode = gef.createResource(_uri); /* SubjectNode */
      PredicateNode predicateNode = gef.createResource(p); /* PredicateNode */
      ObjectNode objectNode = null;

      Object wo = wrap(object); /* ObjectNode */
      if (wo instanceof URI) {
        objectNode = gef.createResource((URI) wo);
      } else {
        objectNode = gef.createLiteral((String) wo);
      }

      Triple triple = gef.createTriple(subjectNode, predicateNode, objectNode);
      newTriple = !graph.contains(triple);

      if (newTriple) {
        graph.add(triple);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    return newTriple;
  }

  /**
   * @see ThingModel#remove(URI, Object)
   */
  public boolean remove(URI p, Object object) {

    boolean modelChanged = false;

    ClosableIterator ci = null;
    try {
      // return where thing is a subject
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();

      Object wo = wrap(object); /* ObjectNode */
      ObjectNode objectNode = null;
      if (wo instanceof URI) {
        objectNode = gef.createResource((URI) wo);
      } else {
        objectNode = gef.createLiteral((String) wo);
      }

      ci = jGraph.find(gef.createResource(_uri), gef.createResource(p),
          objectNode);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        jGraph.remove(triple);
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
   * @see ThingModel#removeAll(URI)
   */
  public boolean removeAll(URI p) {

    boolean modelChanged = false;

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();
      ci = jGraph.find(gef.createResource(_uri), gef.createResource(p), null);

      while (ci.hasNext()) {
        ci.next();
        ci.remove();
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
   * @see ThingModel#getRelationsWithObject(Object)
   */
  public Set getRelationsWithObject(Object object) {

    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();
      Object wo = wrap(object); /* ObjectNode */
      ObjectNode objectNode = null;
      if (wo instanceof URI) {
        objectNode = gef.createResource((URI) wo);
      } else {
        objectNode = gef.createLiteral((String) wo);
      }

      ci = jGraph.find(gef.createResource(_uri), null, objectNode);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        s.add(new ThingJRDFModel(_ojm, ((URIReference) (triple.getPredicate()))
            .getURI()));
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
   * @see ThingModel#getRelationsWithSubject(Object)
   */
  public Set getRelationsWithSubject(Object subject) {

    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();
      ci = jGraph.find(gef.createResource(((Thing) subject).getURI()), null,
          gef.createResource(_uri));

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        s.add(new ThingJRDFModel(_ojm, ((URIReference) (triple.getPredicate()))
            .getURI()));
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
   * @see ThingModel#hasRelation(URI)
   */
  public boolean hasRelation(URI p) {

    boolean hasRelation = false;

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();
      ci = jGraph.find(gef.createResource(_uri), gef.createResource(p), null);
      hasRelation = ci.hasNext();
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (ci != null) {
        ci.close();
      }
    }

    return hasRelation;
  }

  /**
   * @see ThingModel#hasRelation(URI, Object)
   */
  public boolean hasRelation(URI p, Object object) {
    boolean hasRelation = false;

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();
      GraphElementFactory gef = jGraph.getElementFactory();
      Object wo = wrap(object);
      ObjectNode objectNode = null;
      if (wo instanceof URI) {
        objectNode = gef.createResource((URI) wo);
      } else {
        objectNode = gef.createLiteral((String) wo);
      }

      ci = jGraph.find(gef.createResource(_uri), gef.createResource(p),
          objectNode);

      hasRelation = ci.hasNext();
    } catch (Exception ex) {
      ex.printStackTrace();
    } finally {
      if (ci != null) {
        ci.close();
      }
    }

    return hasRelation;
  }

  /**
   * @see ThingModel#getRelations()
   */
  public Set getRelations() {

    Set s = new HashSet();

    ClosableIterator ci = null;
    try {
      Graph jGraph = _ojm.getGraph();

      ci = jGraph.find(jGraph.getElementFactory().createResource(_uri), null,
          null);

      Triple triple;
      while (ci.hasNext()) {
        triple = (Triple) ci.next();
        s.add(new ThingJRDFModel(_ojm, ((URIReference) (triple.getPredicate()))
            .getURI()));
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

}
