package org.mulgara.sofa.test;

import java.util.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;
import net.java.dev.sofa.*;
import net.java.dev.sofa.impl.*;
import net.java.dev.sofa.model.mem.OntologyMemoryModel;
import net.java.dev.sofa.serialize.daml.*;
import net.java.dev.sofa.serialize.rdfs.*;

import org.mulgara.sofa.serialize.owl.*;

import net.java.dev.sofa.serialize.visual.*;
import net.java.dev.sofa.vocabulary.SOFA;

public class CameraOntologyUnitTest extends TestCase {

  public static void main(String[] args) {
    junit.textui.TestRunner.run(CameraOntologyUnitTest.class);
  }

  public void testCameraOntology() throws Exception {

    System.out.println("Creating empty Ontology");
    Ontology onto = OntoConnector.getInstance().createOntology(
        "http://www.xfront.com/owl/ontologies/camera/");

    System.out.println("Loading Camera Ontology");
    OWLReader.getReader().read(onto,
        "file:///" + System.getProperty("cvs.root") + "/data/kamera.owl");

    System.out.println("Loaded Ontology");

    // get the Digital Camera Thing, a Concept is like an OWL Class,
    // Thing is more like an Instance
    Concept digiCam = onto.getConcept("Digital");

    // Create a new type of camera: camera phone

    // create phone first
    Concept phone = onto.createConcept("Phone");
    // give it a property/relation of GSM or CDMA
    Relation standard = onto.createRelation("standard");
    Set standards = new HashSet();
    standards.add("GSM");
    standards.add("CDMA");
    phone.setRestrictionOn(standard, standards, 1, 2); // 1=minCard, 2=maxCard

    // make phone a sub class of purchaseable item
    Concept purchaseableItem = onto.getConcept("PurchaseableItem");
    phone.addSuperConcept(purchaseableItem);

    // create camera phone
    Concept cameraPhone = onto.createConcept("CameraPhone");
    cameraPhone.addSuperConcept(phone);
    cameraPhone.addSuperConcept(digiCam);

    // Show super classes
    System.out.println("SUPER CLASSES");
    Concept superConcept = null;
    Collection superConcepts = cameraPhone.getSuperConcepts(true);

    // test for N superclasses
    assertEquals(4, superConcepts.size());

    // test a phone is our superclass
    assertTrue(superConcepts.contains(phone));

    for (Iterator sc = superConcepts.iterator(); sc.hasNext();) {
      superConcept = (Concept) sc.next();

      System.out.println(superConcept.getId());
    }

    // show properties, including super properties 'true'
    System.out.println("PROPERTIES");

    for (Iterator ri = cameraPhone.definedRelations(true); ri.hasNext();) {
      Relation relation = (Relation) ri.next();

      System.out.println(relation.getId());
    }

    // test camera phones have 'standard'
    assertTrue(cameraPhone.hasDefinedRelation(standard, true));

    // Write new ontology to Standard out
    OWLWriter.getWriter().write(onto, System.out);

  }

}
