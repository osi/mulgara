/*
 * ThingMemoryImplUnitTest.java
 * -----------------------------------------------------------------------------
 * Project           SOFAImpl
 * Package           net.java.dev.sofa.junit
 * Original author   Alex V. Alishevskikh
 *                   [alexeya@dev.java.net]
 * Created           16.04.2004 22:01:42
 * Revision info     $RCSfile: ThingMemoryImplUnitTest.java,v $ $Revision: 1.9 $ $State: Exp $
 *
 * Last modified on  $Date: 2005/01/05 04:59:04 $
 *               by  $Author: newmana $
 *
 * Version: 1.0
 *
 * Copyright (c) 2004 Alex Alishevskikh
 *
 * GNU Lesser General Public License (http://www.gnu.org/copyleft/lesser.txt)
 */

package org.mulgara.sofa.test;

import net.java.dev.sofa.*;
import net.java.dev.sofa.impl.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import junit.framework.TestCase;

/**
 * JAVADOC: <code>ThingMemoryImplUnitTest</code>
 *
 * @version $Id: ThingMemoryImplUnitTest.java,v 1.9 2005/01/05 04:59:04 newmana Exp $
 * @author Alex
 */
public class ThingMemoryImplUnitTest extends TestCase {

    Ontology onto = null;
    Concept c = null;
    Thing t = null;

    public static final String ID = "thing";
    public static final String LABEL = "Test thing";
    public static final String COMMENT = "Test thing for SOFA implementation testing.";
    public static final String VERSIONINFO = "v1.0";

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ThingMemoryImplUnitTest.class);
    }

    public ThingMemoryImplUnitTest(String arg0) {
        super(arg0);
    }
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        onto = OntoConnector.getInstance().createOntology(OntologyMemoryImplUnitTest.ONTO_NAMESPACE);
        try {
            c = onto.createConcept("class");
            t = onto.createThing(ID, c);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        t.setLabel(LABEL);
        t.setComment(COMMENT);
        t.setVersionInfo(VERSIONINFO);
        /*DEBUG dump
        System.out.println("---------");
        ((OntologyMemoryModel)((OntologyImpl)onto).getModel()).dump();
        System.out.println("---------");
        ((OntologyMemoryModel)((OntologyImpl)SOFA.getSystemOntology()).getModel()).dump();
        System.out.println("---------");
        */
    }

    public void testThingBasics() {
        assertNotNull(t);
        assertSame(onto, t.getOntology());
        assertEquals(ID, t.getId());
        assertEquals(LABEL, t.getLabel());
        assertEquals(COMMENT, t.getComment());
        assertEquals(VERSIONINFO, t.getVersionInfo());
        try {
            assertEquals(new URI(t.getOntology().getNameSpace()+"#"+t.getId()), t.getURI());
        }
        catch (URISyntaxException ex) {
            fail(ex.getMessage());
        }
        assertEquals(LABEL, t.toString());
        assertTrue(t.equals(onto.getThing(ID)));
    }

    public void testThingClasses() throws Exception{
        Concept c1 = null;
        Concept c2 = null;
        Concept c3 = null;
        try {
            c1 = onto.createConcept("class1");
            c2 = c1.createSubConcept("class2");
            c3 = c1.createSubConcept("class3");
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        assertTrue(t.addConcept(c2));

        assertEquals(t.getConcepts(false).size(), 2);
        assertTrue(t.isInstanceOf(c2, false));
        assertFalse(t.isInstanceOf(c1, false));
        assertTrue(t.isInstanceOf(c1, true));
        assertFalse(t.isInstanceOf(c3, true));
        assertTrue(t.addConcept(c3));
        assertEquals(t.getConcepts(false).size(), 3);
        assertTrue(t.isInstanceOf(c3, true));
        assertTrue(t.removeConcept(c2));

        assertEquals(t.getConcepts(false).size(), 2);
        assertFalse(t.isInstanceOf(c2, false));
        assertTrue(t.isInstanceOf(c1, true));

        assertTrue(t.removeConcept(c3));
        assertEquals(t.getConcepts(true).size(), 1);
        assertFalse(t.isInstanceOf(c1, true));
    }

    public void testThingProperties() {
        Relation p1 = null;
        try {
            p1 = onto.createRelation("prop1");
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        assertFalse(t.hasRelation(p1, true));
        assertNull(t.get(p1));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 0);

        assertTrue(t.set(p1, "One"));

        assertTrue(t.hasRelation(p1, true));
        assertNotNull(t.get(p1));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 1);
        assertEquals(t.get(p1), "One");

        assertTrue(t.add(p1, "Two"));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 2);
        assertTrue(t.hasRelation(p1, "Two", true));

        assertTrue(t.addAll(p1, new String[] {"Three", "Four", "Five"}));


        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 5);
        assertTrue(t.hasRelationWith("Three", true));

        assertFalse(t.hasRelationWith("Seven", true));
        Vector v = new Vector();
        assertTrue(v.add("Six")); assertTrue(v.add("Seven")); assertTrue(v.add("Eight"));
        assertTrue(t.addAll(p1, v));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 8);

        assertTrue(t.hasRelationWith("One", true));
        assertTrue(t.hasRelationWith("Seven", true));
        assertTrue(t.list(p1).contains("Two"));
        assertTrue(t.list(p1).contains("Five"));

        assertTrue(t.getRelations(true).contains(p1));
        assertTrue(t.getRelationsWith("Three", true).contains(p1));

        assertTrue(t.remove(p1, "Three"));
        assertFalse(t.hasRelationWith("Three", true));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 7);

        assertTrue(t.setAll(p1, new String[] {"Red", "Green", "Blue"}));
        assertTrue(t.hasRelationWith("Green", true));
        assertFalse(t.hasRelationWith("One", true));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 3);

        assertTrue(t.removeAll(p1));
        assertFalse(t.hasRelation(p1, true));
        assertNull(t.get(p1));
        assertEquals(t.list(p1, Thing.INCLUDE_ALL).size(), 0);
    }

    public void testTransitiveProperties() {
        Relation p = null;
        Thing x = null;
        Thing y = null;
        Thing z = null;
        try {
            p = onto.createRelation("p");
            x = onto.createThing("x", c);
            y = onto.createThing("y", c);
            z = onto.createThing("z", c);

        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        p.setTransitive(true);
        assertTrue(x.set(p, y));
        assertTrue(y.set(p, z));


        assertTrue(p.isTransitive());
        assertTrue(x.hasRelation(p, z, true));
        assertFalse(x.hasRelation(p, z, false));
        assertTrue(x.list(p, Thing.INCLUDE_TRANSITIVE).contains(z));
        assertFalse(x.list(p, Thing.DIRECT_ONLY).contains(z));
        p.setTransitive(false);
        assertFalse(x.hasRelation(p, z, true));
        assertFalse(x.list(p, Thing.INCLUDE_TRANSITIVE).contains(z));
        p.setTransitive(true);
        assertTrue(x.hasRelation(p, z, true));
        assertTrue(x.list(p, Thing.INCLUDE_TRANSITIVE).contains(z));
        assertTrue(y.remove(p, z));
        assertFalse(x.hasRelation(p, z, true));
        assertFalse(x.list(p, Thing.INCLUDE_TRANSITIVE).contains(z));
        onto.remove(p);
        onto.remove(x);
        onto.remove(y);
        onto.remove(z);
    }

    public void testSymmetricProperties() {
        Relation p = null;
        Thing x = null;
        Thing y = null;
        try {
            p = onto.createRelation("p");
            x = onto.createThing("x", c);
            y = onto.createThing("y", c);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        p.setSymmetric(true);
        assertTrue(p.isSymmetric());
        assertFalse(y.getRelations(true).contains(p));

        assertTrue(x.set(p, y));

        assertTrue(y.hasRelation(p, x, true));
        assertFalse(y.hasRelation(p, x, false));
        assertTrue(y.list(p, Thing.INCLUDE_SYMMETRIC).contains(x));
        assertFalse(y.list(p, Thing.DIRECT_ONLY).contains(x));
        assertTrue(y.getRelations(true).contains(p));
        assertFalse(y.getRelations(false).contains(p));
/*FIXED: remove symmetric value*/
        assertTrue(y.remove(p, x));
        assertFalse(y.hasRelation(p, x, true));
        assertFalse(x.hasRelation(p, y, true));
        assertTrue(x.set(p, y));



                p.setSymmetric(false);
        assertFalse(y.hasRelation(p, x, true));
        assertFalse(y.list(p, Thing.INCLUDE_SYMMETRIC).contains(x));

        assertTrue(x.remove(p, y));
        assertFalse(y.hasRelation(p, x, true));
        assertFalse(y.list(p, Thing.INCLUDE_SYMMETRIC).contains(x));
        onto.remove(p);
        onto.remove(x);
        onto.remove(y);
    }

    public void testInversedProperties() {
        Relation p1 = null;
        Relation p2 = null;
        Thing x = null;
        Thing y = null;
        Thing z = null;
        try {
            p1 = onto.createRelation("p1");
            p2 = onto.createRelation("p2");
            x = onto.createThing("x", c);
            y = onto.createThing("y", c);
            z = onto.createThing("z", c);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        p2.setInverseOf(p1, true);
        assertTrue(p2.getInversedRelations(true).contains(p1));
        assertTrue(p1.getInversedRelations(true).contains(p2));
        assertTrue(p1.isInverseOf(p2));
        assertTrue(p2.isInverseOf(p1));
        assertFalse(y.getRelations(true).contains(p2));

        assertTrue(x.set(p1, y));

        assertTrue(y.hasRelation(p2, x, true));
        assertFalse(y.hasRelation(p2, x, false));
        assertTrue(y.list(p2, Thing.INCLUDE_INVERSED).contains(x));
        assertFalse(y.list(p2, Thing.DIRECT_ONLY).contains(x));

        assertTrue(y.getRelations(true).contains(p2));
        assertFalse(y.getRelations(false).contains(p2));

        assertTrue(x.remove(p1, y));
        assertFalse(y.hasRelation(p2, x, true));
        assertFalse(y.list(p2, Thing.INCLUDE_INVERSED).contains(x));
        assertTrue(y.set(p2, x));
        assertTrue(x.hasRelation(p1, y, true));
        assertFalse(x.hasRelation(p1, y, false));
        assertTrue(x.list(p1, Thing.INCLUDE_INVERSED).contains(y));
        assertFalse(x.list(p1, Thing.DIRECT_ONLY).contains(y));


/*FIXED: remove inversed value*/
        assertTrue(x.remove(p1, y));
        assertFalse(y.hasRelation(p2, x, true));

        p2.setInverseOf(p1, false);
        assertFalse(x.hasRelation(p1, y, true));
        assertFalse(x.list(p1, Thing.INCLUDE_INVERSED).contains(y));

        p2.setInverseOf(p1, true);
        p1.setTransitive(true);
        assertTrue(x.set(p1, y));
        assertTrue(y.set(p1, z));
        assertTrue(x.hasRelation(p1, z, true));
        assertTrue(y.hasRelation(p2, x, true));
        assertTrue(z.list(p2, Thing.INCLUDE_INVERSED+Thing.INCLUDE_TRANSITIVE).contains(x));
        //assertTrue(z.hasProperty(p2, x, true));

        onto.remove(p1);
        onto.remove(p2);
        onto.remove(x);
        onto.remove(y);
    }

    public void testThingSuperProperties() {
        Relation p1 = null;
        Relation p2 = null;
        Thing x = null;
        try {
            p1 = onto.createRelation("p1");
            p2 = p1.createSubRelation("p2");
            x = onto.createThing("x", c);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }
        assertTrue(x.set(p2, "foo"));
        assertTrue(x.hasRelation(p2, false));
        assertFalse(x.hasRelation(p1, false));
        assertTrue(x.hasRelation(p1, true));
        assertTrue(x.hasRelation(p2, "foo", false));
        assertFalse(x.hasRelation(p1, "foo", false));
        assertTrue(x.hasRelation(p1, "foo", true));
        /*assertEquals(x.getProperties(false).size(), 1);
        assertEquals(x.getProperties(true).size(), 2);*/
        assertFalse(x.getRelationsWith("foo", false).contains(p1));
        assertTrue(x.getRelationsWith("foo", true).contains(p1));
        assertFalse(x.list(p1, Thing.DIRECT_ONLY).contains("foo"));
        assertTrue(x.list(p1, Thing.INCLUDE_SUBPROPERTIES).contains("foo"));
    }

   /* public void testValidation() throws Exception{
        Relation p1 = null;
        Concept c1 = null;
        Concept c2 = null;
        Concept c3 = null;
        Thing x = null;
        Thing y = null;
        Thing z = null;
        try {
            p1 = onto.createProperty("p1");
            c1 = onto.createClass("c1");
            c2 = onto.createClass("c2");
            c3 = onto.createClass("c3");
            x = onto.createThing("x", c);
            y = onto.createThing("y", c);
            z = onto.createThing("z", c);
        } catch (Exception ex) {
            fail(ex.getMessage());
        }*/
        // FAILURE:



        /*DEBUG dump
        System.out.println("---------");
        ((OntologyMemoryModel)((OntologyImpl)onto).getModel()).dump();
        System.out.println("---------");*/
        /*((OntologyMemoryModel)((OntologyImpl)SOFA.getSystemOntology()).getModel()).dump();
        System.out.println("---------");*/

        /*assertTrue(x.validate());
        assertTrue(p1.addDomainClass(c1));
        assertTrue(p1.addRange(c2));
        c1.setRestrictionOn(p1, 1, 1);
        assertTrue(x.addClass(c1));
        assertTrue(y.addClass(c2));
        assertTrue(x.add(p1, y));
        assertTrue(x.validate());
        assertTrue(z.addClass(c2));
        assertTrue(x.add(p1, z));

        assertFalse(x.validate());
        assertTrue(x.remove(p1, z));
        assertTrue(x.validate());


        assertTrue(p1.removeDomainClass(c1));
        assertFalse(x.validate());

        assertTrue(y.removeClass(c2));
        assertTrue(y.addClass(c3));
        assertFalse(x.validate());

        c1.removeRestrictionOn(p1);
        assertTrue(x.remove(p1, y));
        assertTrue(x.validate());
        assertTrue(x.add(p1, "Hello!"));
        assertFalse(x.validate());
        assertTrue(p1.removeRange(c2));
        assertTrue(x.validate());
        assertTrue(p1.addRange(String.class));
        assertTrue(x.add(p1, new Integer(2004)));
        assertFalse(x.validate());
        assertTrue(x.removeAll(p1));
        Vector v = new Vector();
        v.add("One"); v.add("Two"); v.add("Three"); v.add("Four");
        c1.setRestrictionOn(p1, v, 0, Restriction.UNBOUNDED);
        assertTrue(x.add(p1, "One"));
        assertTrue(x.add(p1, "Four"));
        assertTrue(x.validate());
        assertTrue(x.add(p1, "Foo"));
        assertFalse(x.validate());
        onto.remove(p1);
        onto.remove(c1);
        onto.remove(c2);
        onto.remove(c3);
        onto.remove(x);
        onto.remove(y);
        onto.remove(z);
    }*/
}
