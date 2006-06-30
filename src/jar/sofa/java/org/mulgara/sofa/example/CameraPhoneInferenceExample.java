package org.mulgara.sofa.example;

/**
 * Camera Phone Inference Example
 */
import java.util.*;
import net.java.dev.sofa.*;
import net.java.dev.sofa.impl.*;

import org.mulgara.sofa.serialize.owl.*;

public class CameraPhoneInferenceExample {

  public static void main(String[] args) {

    try {
      // Create in memory based Ontology, use TKS ontology model  for persistence
      Ontology ontology = OntoConnector.getInstance().createOntology(
          "http://www.xfront.com/owl/ontologies/camera/");
      // Load Camera Phone Ontology
      OWLReader.getReader().read(ontology, "file:Resources/CameraPhone.owl ");
      // Get camera phone
      Concept cameraPhone = ontology.getConcept("CameraPhone");
      // Show super classes
      for (Iterator sc = cameraPhone.getSuperConcepts(true).iterator(); sc
          .hasNext();) {
        Concept superConcept = (Concept) sc.next();
        System.out.println(superConcept.getId());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
