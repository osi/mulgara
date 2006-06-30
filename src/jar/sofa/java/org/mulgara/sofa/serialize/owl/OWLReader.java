package org.mulgara.sofa.serialize.owl;

import net.java.dev.sofa.*;
import net.java.dev.sofa.serialize.*;
import net.java.dev.sofa.serialize.Util;

import java.util.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.vocabulary.*;

import com.hp.hpl.jena.mem.*;
import com.hp.hpl.jena.rdf.model.impl.*;

/**
 * OWLReader.java
 * <br>
 * This is a version of the OWLReader supplied with SOFA which is compatible with
 * Jena 2.1 as used in kowari/tks.
 * <pre>
 * Modified by       Keith Ahern
 *                   [keith@tucanatech.com]
 * Created           26.08.2004 21:55:05
 * Revision info     $RCSfile: OWLReader.java,v $ $Revision: 1.9 $ $State: Exp $
 *
 * Last modified on  $Date: 2005/01/05 04:59:03 $
 *               by  $Author: newmana $
 *
 * Version: 1.0
 * </pre>
 *
 * GNU Lesser General Public License (http://www.gnu.org/copyleft/lesser.txt)
 */

public class OWLReader implements Reader {

    private static OWLReader _instance = new OWLReader();

    public static Reader getReader() {
        return _instance;
    }

    Ontology onto = null;
    Model _model = new ModelMem();
    String _uri;

    static class OWL {
        public static final String NS = "http://www.w3.org/2002/07/owl#";
        public static final Resource Thing = new ResourceImpl(NS + "Thing");
        public static final Resource Class = new ResourceImpl(NS + "Class");
        public static final Resource ObjectProperty = new ResourceImpl(NS + "ObjectProperty");
        public static final Resource DataTypeProperty = new ResourceImpl(NS + "DataTypeProperty");
        public static final Resource TransitiveProperty = new ResourceImpl(NS + "TransitiveProperty");
        public static final Resource SymmetricProperty = new ResourceImpl(NS + "SymmetricProperty");
        public static final Resource Restriction = new ResourceImpl(NS + "Restriction");
        public static com.hp.hpl.jena.rdf.model.Property inverseOf = null;
        public static com.hp.hpl.jena.rdf.model.Property Cardinality = null;
        public static com.hp.hpl.jena.rdf.model.Property maxCardinality = null;
        public static com.hp.hpl.jena.rdf.model.Property minCardinality = null;
        public static com.hp.hpl.jena.rdf.model.Property onProperty = null;

        static {
            try {
                Cardinality = new PropertyImpl(NS, "Cardinality");
                minCardinality = new PropertyImpl(NS, "minCardinality");
                maxCardinality = new PropertyImpl(NS, "maxCardinality");
                onProperty = new PropertyImpl(NS, "onProperty");
                inverseOf = new PropertyImpl(NS, "inverseOf");
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void read(Ontology ontology, String uri) throws Exception {
        _model.read(uri);
        _uri = uri;
        onto = ontology;
        buildOntology();
    }

    void buildOntology() throws Exception {
        for (ResIterator classes = _model.listSubjectsWithProperty(RDF.type, OWL.Class); classes.hasNext();)
            buildClass((Resource)classes.next());
        for (ResIterator props = _model.listSubjectsWithProperty(RDF.type, OWL.ObjectProperty); props.hasNext();)
            buildProperty((Resource)props.next());
        for (ResIterator props = _model.listSubjectsWithProperty(RDF.type, OWL.TransitiveProperty); props.hasNext();)
            buildProperty((Resource)props.next());
        for (ResIterator props = _model.listSubjectsWithProperty(RDF.type, OWL.SymmetricProperty); props.hasNext();)
            buildProperty((Resource)props.next());
        for (ResIterator props = _model.listSubjectsWithProperty(RDF.type, OWL.DataTypeProperty); props.hasNext();)
            buildProperty((Resource)props.next());
        for (ResIterator things = _model.listSubjects(); things.hasNext();)
            buildThing((Resource)things.next());
    }

    String getNS(String ns) {
        if (ns.startsWith(_uri))
            return "";
        return ns;
    }

    boolean checkNS(String ns) {
        if (ns == null)
            return true;
        if (ns.startsWith(RDF.getURI()))
            return false;
        if (ns.startsWith(RDFS.getURI()))
            return false;
        if (ns.startsWith(OWL.NS))
            return false;
        return true;
    }

    Concept buildClass(Resource dcl) throws Exception {
        if (dcl == null)
            return null;
        if (!checkNS(dcl.getNameSpace()))
            return null;
        String id = null;
        if (dcl.getLocalName() != null)
            id = /*getNS(dcl.getNameSpace()) + */dcl.getLocalName();
        else
            return null;
        Concept c;
        if (onto.hasConcept(id))
            return onto.getConcept(id);
        else
            c = onto.createConcept(id);
        for (ResIterator it = _model.listSubjectsWithProperty(RDFS.subClassOf, dcl); it.hasNext();) {
            Concept sc = buildClass((Resource)it.next());
            if (sc != null)
                c.addSubConcept(sc);
        }
        for (ResIterator it = _model.listSubjectsWithProperty(RDF.type, dcl); it.hasNext();) {
            Resource di = (Resource)it.next();
            if (!checkNS(di.getNameSpace()))
                continue;
            //Thing t = null;
            // TUCANA FIX TO LOAD INSTANCES if (onto.hasThing(getNS(di.getNameSpace()) + di.getLocalName()))
            if (onto.hasThing(di.getLocalName()))
              c.addInstance(onto.getThing(getNS(di.getNameSpace()) + di.getLocalName()));
            else
              // TUCANA FIX TO LOAD INSTANCES c.createInstance(getNS(di.getNameSpace()) + di.getLocalName());
              c.createInstance(di.getLocalName());
        }
        for (ResIterator it = _model.listSubjectsWithProperty(RDFS.domain, dcl); it.hasNext();) {
            net.java.dev.sofa.Relation p = buildProperty((Resource)it.next());
            if (p != null)
                c.addRelation(p);
        }
        for (StmtIterator it = dcl.listProperties(RDFS.subClassOf); it.hasNext();) {
            RDFNode o = it.nextStatement().getObject();
            if (o instanceof Resource) {
                Resource rs = (Resource) o;
                if (rs.hasProperty(RDF.type, OWL.Restriction)) {
                    net.java.dev.sofa.Relation onp =
                        buildProperty((Resource) rs.getProperty(OWL.onProperty).getObject());
                    if (onp != null) {
                        int maxC = -1, minC = 0;
                        if (rs.hasProperty(OWL.Cardinality)) {
                            maxC = rs.getProperty(OWL.Cardinality).getLiteral().getInt();
                            minC = maxC;
                        } else {
                            if (rs.hasProperty(OWL.minCardinality))
                                minC = rs.getProperty(OWL.minCardinality).getLiteral().getInt();
                            if (rs.hasProperty(OWL.maxCardinality))
                                maxC = rs.getProperty(OWL.maxCardinality).getLiteral().getInt();
                        }
                        c.setRestrictionOn(onp, minC, maxC);
                    }
                }
                else {
                    Concept pc = buildClass(rs);
                    if ((pc != null) && (!pc.hasSubConcept(c, false)))
                        pc.addSubConcept(c);
                }
            }
        }
        return c;
    }

    net.java.dev.sofa.Relation buildProperty(Resource dpl) throws Exception {
        if (dpl == null)
            return null;
        if (!checkNS(dpl.getNameSpace()))
            return null;
        String id = null;
        if (dpl.getLocalName() != null)
            id = /*getNS(dpl.getNameSpace()) + */dpl.getLocalName();
        else
            return null;
        net.java.dev.sofa.Relation p;
        if (onto.hasRelation(id))
            return onto.getRelation(id);
        else
            p = onto.createRelation(id);

        for (ResIterator it = _model.listSubjectsWithProperty(RDFS.subPropertyOf, dpl); it.hasNext();) {
            net.java.dev.sofa.Relation sp = buildProperty((Resource)it.next());
            if (sp != null)
                p.addSubRelation(sp);
        }
        for (StmtIterator it = dpl.listProperties(RDFS.subPropertyOf); it.hasNext();) {
            RDFNode o = it.nextStatement().getObject();
            if (o instanceof Resource) {
                net.java.dev.sofa.Relation pp = buildProperty((Resource) o);
                if ((pp != null) && (!pp.hasSubRelation(p, false)))
                    pp.addSubRelation(p);
            }
        }
        for (StmtIterator it = dpl.listProperties(RDFS.range); it.hasNext();) {
            Resource o = (Resource)it.nextStatement().getObject();
            String did = o.getLocalName();
            if (Util.isDatatype(o.getURI()))
                p.addRange(Class.forName(Util.getJavaDatatype(o.getURI())));
            else {
                Concept dcc = buildClass(o);
                if (dcc != null)
                    p.addRange(dcc);
            }
        }
        for (StmtIterator it = dpl.listProperties(RDFS.domain); it.hasNext();) {
            RDFNode o = it.nextStatement().getObject();
            if (o instanceof Resource) {
                Concept dc = buildClass((Resource) o);
                if (dc != null)
                    p.addDomainConcept(dc);
            }
        }
        for (StmtIterator it = dpl.listProperties(OWL.inverseOf); it.hasNext();) {
            RDFNode o = it.nextStatement().getObject();
            if (o instanceof Resource) {
                net.java.dev.sofa.Relation pp = buildProperty((Resource) o);
                if (pp != null)
                    p.setInverseOf(pp, true);
            }
        }
        if (dpl.hasProperty(RDF.type, OWL.TransitiveProperty))
            p.setTransitive(true);
        else if (dpl.hasProperty(RDF.type, OWL.SymmetricProperty))
            p.setSymmetric(true);
        return p;
    }

    Thing getThing(Resource dtl) throws Exception {
        if (dtl == null)
            return null;
        if (!checkNS(dtl.getNameSpace()))
            return null;
        String id = null;
        if (dtl.getLocalName() != null)
            id = /*getNS(dtl.getNameSpace()) + */dtl.getLocalName().toString();
        else
            return null;
        if (onto.hasThing(id))
            return onto.getThing(id);
        else
            return null;
    }

    Thing buildThing(Resource dtl) throws Exception {
        if (dtl == null)
            return null;
        if (!checkNS(dtl.getNameSpace()))
            return null;
        String id = null;
        if (dtl.getLocalName() != null)
            id = /*getNS(dtl.getNameSpace()) + */dtl.getLocalName().toString();
        else
            return null;
        Thing t = null;
        if (onto.hasThing(id))
            t = onto.getThing(id);
        else
            return null;

        Set props = new HashSet();
        for (StmtIterator it = dtl.listProperties(); it.hasNext();) {
            Resource rs = it.nextStatement().getPredicate();
            if (checkNS(rs.getNameSpace()))
                props.add(rs);
        }

        for (Iterator it = props.iterator(); it.hasNext();) {
            com.hp.hpl.jena.rdf.model.Property dap = (com.hp.hpl.jena.rdf.model.Property) it.next();
            net.java.dev.sofa.Relation sp = buildProperty(dap);
            for (StmtIterator jt = dtl.listProperties(dap); jt.hasNext();) {
                RDFNode v = jt.nextStatement().getObject();
                if (v instanceof Resource) {
                    Thing tt = getThing((Resource) v);
                    if (tt != null)
                        t.add(sp, tt);
                }
                else if (v instanceof Literal) {
                    Object c = null;
                    Iterator rs = sp.ranges(false);
                    if (rs.hasNext()) {
                        do {
                            c = rs.next();
                        }
                        while (!(c instanceof Class) && rs.hasNext());
                    }
                    if (!(c instanceof Class)) {
                        rs = sp.ranges(true);
                        if (rs.hasNext())
                        do {
                            c = rs.next();
                        }
                        while (!(c instanceof Class) && rs.hasNext());
                    }
                    if ((c == null) || !(c instanceof Class)) c = String.class;
                    t.add(sp, Util.deexternalize(((Literal) v).getString(), (Class)c));
                }
                else
                    t.add(sp, v.toString());
            }

        }

        if (dtl.hasProperty(RDFS.comment))
            t.setComment(dtl.getProperty(RDFS.comment).getLiteral().getString());
        if (dtl.hasProperty(RDFS.label))
            t.setLabel(dtl.getProperty(RDFS.label).getLiteral().getString());
        return t;
    }


}
