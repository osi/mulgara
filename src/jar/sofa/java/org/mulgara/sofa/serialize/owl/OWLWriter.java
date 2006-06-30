
package org.mulgara.sofa.serialize.owl;

import net.java.dev.sofa.*;
import net.java.dev.sofa.serialize.Util;
import net.java.dev.sofa.serialize.Writer;
import nu.xom.*;
import java.io.*;
import java.util.*;

/**
 * OWLWriter.java
 * <br>
 * This is a version of the OWLWriter supplied with SOFA. The only difference
 * is the package name.  It is included for completeness with OWLReader.java
 * which does have source code changes.
 * <pre>
 * Modified by       Keith Ahern
 *                   [keith@tucanatech.com]
 * Created           26.08.2004 21:55:05
 * Revision info     $RCSfile: OWLWriter.java,v $ $Revision: 1.9 $ $State: Exp $
 *
 * Last modified on  $Date: 2005/01/05 04:59:03 $
 *               by  $Author: newmana $
 *
 * Version: 1.0
 * </pre>
 *
 * GNU Lesser General Public License (http://www.gnu.org/copyleft/lesser.txt)
 */
public class OWLWriter implements Writer {

    private static OWLWriter _instance = new OWLWriter();

    public static Writer getWriter() {
        return _instance;
    }

    Document doc = null;
    Element _root = null;

    static String OWL_NS = "http://www.w3.org/2002/07/owl#";
    static String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    static String RDFS_NS = "http://www.w3.org/2000/01/rdf-schema#";

    String ONTO_NS = "";

    public void write(Ontology ontology, OutputStream out) throws IOException {
        ONTO_NS = ontology.getNameSpace().toString();
        if (!ONTO_NS.endsWith("#"))
            ONTO_NS = ONTO_NS + "#";
        buildDoc(ontology);
        Serializer serializer = new Serializer(out, "UTF-8");
        serializer.write(doc);
    }

    public void write(Ontology ontology, String uri) throws Exception {
        write(ontology, new FileOutputStream(new java.net.URL(uri).getPath()));
    }

    String getNameSpace(String s) {
        String[] ss = s.split("#");
        if (ss.length > 1)
            return ss[0];
        return "";
    }

    String getLocalName(String s) {
        String[] ss = s.split("#");
        if (ss.length > 1)
            return ss[1];
        return s;
    }

    String getNameRef(String s) {
        if (getNameSpace(s).length() > 0)
            return s;
        if (!s.startsWith("#"))
            return "#"+s;
        return s;
    }


    private  Attribute getAttr(String prefix, String name, String uri, String value) {
        Attribute a = new Attribute(name, value);
        a.setNamespace(prefix, uri);
        return a;
    }

    private Element createElement(String prefix, String name, String uri) {
        Element el = new Element(name, uri);
        el.setNamespacePrefix(prefix);
        return el;
    }

    private Element createElement(String name) {
        return createElement("ns1", name, ONTO_NS);
    }

    private void addElement(Element parent, Element child, String text) {
        if (text.length() == 0) return;
        child.appendChild(text);
        parent.appendChild(child);
    }

    private void buildDoc(Ontology o) {
        _root = createElement("rdf", "RDF", RDF_NS);
        _root.addNamespaceDeclaration("rdf", RDF_NS);
        _root.addNamespaceDeclaration("rdfs", RDFS_NS);
        _root.addNamespaceDeclaration("owl", OWL_NS);
        _root.addNamespaceDeclaration("ns1", ONTO_NS);

        //_root.setNamespacePrefix("rdf");
        doc = new Document(_root);

        Element damlOntology = createElement("owl","Ontology", OWL_NS);
        addElement(damlOntology, createElement("owl", "versionInfo", OWL_NS), o.getVersionInfo());
        addElement(damlOntology, createElement("rdfs","comment", RDFS_NS), o.getComment());
        addElement(damlOntology, createElement("rdfs","label", RDFS_NS), o.getLabel());
        _root.appendChild(damlOntology);
        for (Iterator i = o.things(); i.hasNext();) {
            Thing t = (Thing) i.next();
            if (t.getId().startsWith("__")) continue;
            Element el;
            if (t.isConcept())
                el = buildClassElement(t.toConcept());
            else if (t.isRelation())
                el = buildPropertyElement(t.toRelation());
            else
                el = buildThingElement(t);
            buildCommon(t, el);
            _root.appendChild(el);
        }
    }

    private Element buildClassElement(Concept c) {
        Element el = createElement("owl","Class", OWL_NS);
        for (Iterator it = c.superConcepts(false); it.hasNext();) {
            Element se = createElement("rdfs","subClassOf", RDFS_NS);
            se.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(((Concept)it.next()).getId())));
            el.appendChild(se);
        }
        for (Iterator it = c.restrictions(false); it.hasNext();) {
            Element ze = createElement("rdfs","subClassOf", RDFS_NS);
            Element se = createElement("owl","Restriction", OWL_NS);
            Restriction re = (Restriction) it.next();
            Element on = createElement("owl","onProperty", OWL_NS);
            on.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(re.getRelation().getId())));
            se.appendChild(on);
            if (re.getMaxCardinality() != Restriction.UNBOUNDED)
                se.addAttribute(getAttr("owl","maxCardinality", OWL_NS,  new Integer(re.getMaxCardinality()).toString()));
            if (re.getMinCardinality() != Restriction.ZERO)
                se.addAttribute(getAttr("owl","minCardinality", OWL_NS, new Integer(re.getMinCardinality()).toString()));
            ze.appendChild(se);
            el.appendChild(ze);
        }
        return el;
    }

    private Element buildPropertyElement(Relation p) {
        Element el = createElement("owl","ObjectProperty", OWL_NS);
        for (Iterator it = p.superRelations(false); it.hasNext();) {
            Element se = createElement("rdfs","subPropertyOf", RDFS_NS);
            se.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(((Relation)it.next()).getId())));
            el.appendChild(se);
        }
        for (Iterator it = p.domainConcepts(false); it.hasNext();) {
            Element se = createElement("rdfs","domain", RDFS_NS);
            se.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(((Concept)it.next()).getId())));
            el.appendChild(se);
        }
        for (Iterator it = p.ranges(false); it.hasNext();) {
            Element se = createElement("rdfs","range", RDFS_NS);
            Object r = it.next();
            Attribute res = getAttr("rdf", "resource", RDF_NS, "");
            if (r instanceof Concept)
                res.setValue(getNameRef(((Concept)r).getId()));
            else if (r instanceof Class){
                res.setValue(Util.getXsdDatatype(((Class)r).getName()));
                el.setLocalName("DatatypeProperty");
            }
            se.addAttribute(res);
            el.appendChild(se);
        }
        for (Iterator it = p.getInversedRelations(false).iterator(); it.hasNext();) {
            Element se = createElement("owl","inverseOf", OWL_NS);
            se.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(((Relation)it.next()).getId())));
            el.appendChild(se);
        }
        if (p.isTransitive())
            el.setLocalName("TransitiveProperty");
        if (p.isSymmetric())
            el.setLocalName("SymmetricProperty");
        return el;
    }


    private Element buildThingElement(Thing t) {
        Element el = null;
        if (!t.getConcepts(false).isEmpty()) {
            String cid = ((Concept)t.getConcepts(false).toArray()[0]).getId();
            if (getNameSpace(cid).length() == 0)
                    el = createElement(cid);
        }
        if (el == null)
            el = createElement("owl","Thing", OWL_NS);
        return el;
    }

    private void buildCommon(Thing t, Element el) {
        for (Iterator it = t.concepts(false); it.hasNext();) {
            Concept c = (Concept)it.next();
            if (c.getId().startsWith("__")) continue;
            Element se = createElement("rdf","type", RDF_NS);
            se.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(c.getId())));
            el.appendChild(se);
        }
        for (Iterator it = t.relations(false); it.hasNext();) {
            Relation p = (Relation) it.next();
            if (p.getId().startsWith("__")) continue;
            for (Iterator jt = t.list(p).iterator(); jt.hasNext();) {
                Element se = createElement(p.getId());
                Object v = jt.next();
                if ((v instanceof Thing) && (!((Thing)v).getId().startsWith("__")))
                    se.addAttribute(getAttr("rdf", "resource", RDF_NS, getNameRef(((Thing)v).getId())));
                else if (!(v instanceof Thing))
                    se.appendChild(Util.externalize(v));
                el.appendChild(se);
            }
        }
        el.addAttribute(getAttr("rdf", "ID", RDF_NS, t.getId()));
        addElement(el, createElement("owl","versionInfo", OWL_NS), t.getVersionInfo());
        addElement(el, createElement("rdfs","comment", RDFS_NS), t.getComment());
        addElement(el, createElement("rdfs","label", RDFS_NS), t.getLabel());
    }

}
