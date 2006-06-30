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

package org.mulgara.resolver.lucene;

// Java 2 standard packages
import java.io.*;
import java.nio.*;
import java.util.*;

// 3rd party
import junit.framework.*;
import org.apache.log4j.*;
import org.apache.lucene.search.*;

import org.mulgara.util.TempDir;


/**
 * Test cases for FullTextStringIndex.
 *
 * @author Tate Jones
 *
 * @created 2002-03-17
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:47 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2002-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class FullTextStringIndexUnitTest extends TestCase {

  /**
   * Directory for the indexes
   */
  private final static String indexDirectory =
      TempDir.getTempDir().getPath() + File.separator + "fulltextsp";

  /**
   * Directory for the indexes *
   */
  private final static String indexDirectory2 =
      TempDir.getTempDir().getPath() + File.separator + "fulltextsp2";

  /**
   * The directory containing the text documents
   */
  private final static String textDirectory =
      System.getProperty("cvs.root") + File.separator + "data" + File.separator +
      "fullTextTestData";

  /**
   * Logger.
   */
  private final static Logger logger =
      Logger.getLogger(FullTextStringIndexUnitTest.class.getName());

  /**
   * Hold a list of test data
   */
  private ArrayList theStrings = new ArrayList();

  /**
   * Create the testing class
   *
   * @param name The name of the test.
   */
  public FullTextStringIndexUnitTest(String name) {
    super(name);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   *
   * @return The test suite to run.
   */
  public static Test suite() {

    TestSuite suite = new TestSuite();
    suite.addTest(new FullTextStringIndexUnitTest("testFullTextStringPool"));
    suite.addTest(new FullTextStringIndexUnitTest("testFullTextStringPoolwithFiles"));
    suite.addTest(new FullTextStringIndexUnitTest("testWithoutClosing"));

    return suite;
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   */
  public static void main(String[] args) {

    junit.textui.TestRunner.run(suite());
  }

  /**
   * Creates a new index required to do the testing.
   *
   * @throws IOException Description of Exception
   */
  public void setUp() throws IOException {

    //Populate a list of strings
    theStrings.add("AACP Pneumothorax Consensus Group");
    theStrings.add("ALS-HPS Steering Group");
    theStrings.add(
        "ALSPAC (Avon Longitudinal Study of Parents and Children) Study Team");
    theStrings.add("ALTS Study group");
    theStrings.add("American Academy of Asthma, Allergy and Immunology");
    theStrings.add("American Association for the Surgery of Trauma");
    theStrings.add("American College of Chest Physicians");
    theStrings.add(
        "Antiarrhythmics Versus Implantable Defibrillator (AVID) Trial Investigators");
    theStrings.add("Antibiotic Use Working Group");
    theStrings.add("Atypical Squamous Cells Intraepithelial");
    theStrings.add("Lesion Triage Study (ALTS) Group");
    theStrings.add(
        "Australasian Society for Thrombosis and Haemostasis (ASTH) Emerging Technologies Group");
    theStrings.add("Benefit Evaluation of Direct Coronary Stenting Study Group");
    theStrings.add("Biomarkers Definitions Working Group.");
    theStrings.add(
        "Canadian Colorectal Surgery DVT Prophylaxis Trial investigators");
    theStrings.add("Cancer Research Campaign Phase I - II Committee");
    theStrings.add("Central Technical Coordinating Unit");
    theStrings.add(
        "Clinical Epidemiology Group from the French Hospital Database on HIV");
    theStrings.add("CNAAB3005 International Study Team");
    theStrings.add("Commissione ad hoc");
    theStrings.add("Committee to Advise on Tropical Medicine and Travel");
    theStrings.add(
        "Comparison of Candesartan and Amlodipine for Safety, Tolerability and Efficacy (CASTLE) Study Investigators");
    theStrings.add(
        "Council on Scientific Affairs, American Medical Association");
    theStrings.add(
        "Dana Consortium on the Therapy of HIV-Dementia and Related Cognitive Disorders");
    theStrings.add("Danish Committee on Scientific Dishonesty");
    theStrings.add("Dengue Network Philippines");
    theStrings.add("Donepezil Study Group");
    theStrings.add("EBPG (European Expert Group on Renal Transplantation)");
    theStrings.add(
        "Arbeitsgemeinschaft Dermatologische Histologie (ADH) der DDG.");
    theStrings.add("EORTC Early Clinical Studies Group");
    theStrings.add("European Renal Association (ERA-EDTA)");
    theStrings.add("European Society for Organ Transplantation (ESOT)");
    theStrings.add("European Study Investigators");
    theStrings.add("European Canadian Glatiramer Acetate Study Group");
    theStrings.add("FAMI Investigator Group");
    theStrings.add("French EGEA study");
    theStrings.add("French National Medical and Health Research Institute");
    theStrings.add(
        "French Parkinson's Disease Genetics Study Group. The European Consortium on Genetic");
    theStrings.add("Susceptibility in Parkinson's Disease");
    theStrings.add("German Hodgkin Study Group");
    theStrings.add("Groupe d'Etude des Lymphomes de l'Adulte (GELA)");
    theStrings.add(
        "Groupe d'Etude et de Recherche Clinique en Oncologie Radiotherapies");
    theStrings.add("Hemophilia Behavioral Intervention Study Group");
    theStrings.add("Hepatitis Interventional Therapy Group");
    theStrings.add("HIV Epidemiology Research Study Group");
    theStrings.add("Houston Congenital CMV Longitudinal Study Group");
    theStrings.add(
        "International Council for Science's Standing Committee on Responsibility and Ethics in Science");
    theStrings.add("International Evidence-Based Group for Neonatal Pain");

    theStrings.add("one");
    theStrings.add("one two");
    theStrings.add("one two three");
    theStrings.add("holidays");
  }

  /**
   * Closes the index used for testing.
   *
   * @throws IOException Description of Exception
   */
  public void tearDown() throws IOException {

  }

  /**
   * 1. Test the loading of strings into the fulltext string pool 2. Checking
   * for existance 3. Test non-stemming 4. Test removal of strings
   *
   * @throws Exception Test fails
   */
  public void testFullTextStringPool() throws Exception {

    FullTextStringIndex index = new FullTextStringIndex(indexDirectory,
        "fulltextsp", true);

    try {

      // Ensure that reverse search is enabled.
      int stringsAdded = 0;
      int stringsFound = 0;

      String document = "http://tucana.org/tucana/document#";
      String has = "http://tucana.org/tucana/document#has";

      //Clean any existing indexes.
      index.removeAll();

      //re-create the index
      index = new FullTextStringIndex(indexDirectory, "fulltextsp", true);

      // Add strings to the index
      ListIterator i = theStrings.listIterator();

      while (i.hasNext()) {

        String literal = (String) i.next();
        index.add(document, has, literal);
        stringsAdded++;
      }

      // Find the strings from the index with both subject & predicate
      i = theStrings.listIterator();

      while (i.hasNext()) {

        String literal = (String) i.next();

        Hits hits = index.find(document, has, literal);

        if (hits.length() == 0) {

          this.fail("failed to find '" + literal + "'");
        }
      }

      // Find the strings from the index with only subject
      i = theStrings.listIterator();

      while (i.hasNext()) {

        String literal = (String) i.next();
        Hits hits = index.find(document, null, literal);

        if (hits.length() == 0) {

          this.assertTrue("failed to find '" + literal + "'", false);
        }
      }

      // Find the strings from the index with only predicate
      i = theStrings.listIterator();

      while (i.hasNext()) {

        String literal = (String) i.next();
        Hits hits = index.find(null, has, literal);

        if (hits.length() == 0) {

          this.assertTrue("failed to find '" + literal + "'", false);
        }
      }

      Hits result = index.find(null, null, "\"holiday\"");

      this.assertTrue("Stemming match search failed - should not found " +
          "stemmed word(" + result.length() + ")", (result.length() == 0));

      this.assertTrue("Should not be able to delete fulltext " +
          "literal due to incorrect value",
          index.remove(document, has, "holiday") == false);

      index.remove(document, has, "one two");
      index.remove(document, has, "one");
      index.remove(document, has, "one two three");

      this.assertTrue("Presumed deleted but found 'one two'",
          index.find(document, has, "one two").length() == 0);
      this.assertTrue("Presumed deleted but found 'one'",
          index.find(document, has, "one").length() == 0);
      this.assertTrue("Presumed deleted but found 'one two three'",
          index.find(document, has, "one two three").length() == 0);

      // don't add empty literals
      this.assertTrue("Adding an empty literal string should fail",
            !index.add("subject","predicate", ""));
      this.assertTrue("Adding an empty literal string should fail",
            !index.add("subject","predicate", "  "));


      this.assertTrue(
          "Adding a string containing slashes to the fulltext string pool",
          index.add("subject", "predicate", "this/is/a/slash/test"));

      long returned = index.find(document, has, "?ommittee").length();
      this.assertTrue("Reverse lookup was expecting 4 document returned found : " +
          returned, returned == 4);

      returned = index.find(document, has, "*iv").length();
      this.assertTrue("Reverse lookup was expecting 3 document returned found : " +
          returned, returned == 3);

      returned = index.find(document, has, "study *roup").length();
      this.assertTrue(
          "Reverse lookup was expecting 26 document returned found : " + returned,
          returned == 26);

      returned = index.find(document, has, "+study +*roup").length();
      this.assertTrue(
          "Reverse lookup was expecting 10 document returned found : " + returned,
          returned == 10);

      returned = index.find(document, has, "-study +*roup").length();
      this.assertTrue(
          "Reverse lookup was expecting 11 document returned found : " + returned,
          returned == 11);

      returned = index.find(document, has, "+*hrombosis").length();
      this.assertTrue("Reverse lookup was expecting 1 document returned found : " +
          returned, returned == 1);
    }
    finally {

      if (index != null) {
        index.close();
        this.assertTrue("Unable to remove all index files", index.removeAll());
      }
    }
  }

  /**
   * 1. Test the loading of text files into the fulltext string pool 2. Checking
   * for existance 3. Test removal of files
   *
   * @throws Exception Test fails
   */
  public void testFullTextStringPoolwithFiles() throws Exception {

    /*
    File indexDir = new File(indexDirectory);

    if (!indexDir.exists() || indexDir.canRead()) {

      throw new Exception("Can't access: " + indexDirectory);
    }
    */

    // create a new index direcotry
    FullTextStringIndex index = new FullTextStringIndex(indexDirectory,
        "fulltextsp", true);

    try {

      // make sure the index directory is empty
      index.close();
      this.assertTrue("Unable to remove all index files", index.removeAll());

      // create a new index
      index = new FullTextStringIndex(indexDirectory, "fulltextsp", true);

      logger.debug("Obtaining text text documents from " + textDirectory);

      File directory = new File(textDirectory);
      File[] textDocuments = directory.listFiles();

      // keep a track of the number of documents added.
      int docsAdded = 0;

      // Loop over the text documents locatd in the text directory
      for (int fileNo = 0; fileNo < textDocuments.length; fileNo++) {

        if (textDocuments[fileNo].isFile()) {

          // open a reader to the text file.
          Reader reader =
              new InputStreamReader(new FileInputStream(textDocuments[fileNo]));

          // Add the text document to the index
          if (index.add(textDocuments[fileNo].toURI().toString(),
              "http://tucana.org/tucana/Document#Content",
              textDocuments[fileNo].toURI().toString(), reader)) {

            logger.debug("Indexed text document " +
                textDocuments[fileNo].toString());
            docsAdded++;
          }

          // clean up the stream
          reader.close();
        }
      }

      logger.debug("Text documents indexed :" + docsAdded);

      // check if all text documents were indexed
      this.assertTrue("Expected 114 text documents to be index", docsAdded == 114);

      // Perform a search for 'supernatural' in the
      // document content predicate
      Hits hits = index.find(null, "http://tucana.org/tucana/Document#Content",
          "supernatural");

      // check if all text documents were indexed
      this.assertTrue("Expected 6 hits with the word 'supernatural', " +
          "only found " + hits.length(), hits.length() == 6);

      // loop through the results and remove the documents containing
      // the word 'supernatural'
      int docsRemoved = 0;

      for (int docNo = 0; docNo < hits.length(); docNo++) {

        String uri =
            hits.doc(docNo).getField(FullTextStringIndex.SUBJECT_KEY).stringValue();

        logger.debug("Found supernatural in :" + uri);

        // Remove the text documents from the index
        if (index.remove(uri, "http://tucana.org/tucana/Document#Content", uri)) {

          docsRemoved++;
        }
      }

      // check the document were removed
      this.assertTrue("Expected 6 documents to be removed'", docsRemoved == 6);

      // Perform a search for 'supernatural' in the
      // document content predicate
      hits =
          index.find(null, "http://tucana.org/tucana/Document#Content",
          "supernatural");

      // check if all text documents are not present.
      this.assertTrue("Expected 0 hits with the word 'supernatural', " +
          "only found " + hits.length(), hits.length() == 0);

      logger.debug("Found supernatural in " + hits.length() + " documents");
    }
    finally {

      // close the fulltextstringpool
      if (index != null) {
        index.close();
        this.assertTrue("Unable to remove all index files", index.removeAll());
      }
    }
  }

  /**
   * Test creating two text indexes.
   *
   * @throws Exception Test fails
   */
  public void testWithoutClosing() throws Exception {

    FullTextStringIndex index, index2, index3, index4;

    index = new FullTextStringIndex(indexDirectory, "fulltextsp", true);
    index2 = new FullTextStringIndex(indexDirectory2, "fulltextsp2", true);
    index3 = new FullTextStringIndex(indexDirectory, "fulltextsp", true);
    index4 = new FullTextStringIndex(indexDirectory2, "fulltextsp2", true);

    index = new FullTextStringIndex(indexDirectory, "fulltextsp", true);
    index2 = new FullTextStringIndex(indexDirectory2, "fulltextsp2", true);
    index3 = new FullTextStringIndex(indexDirectory, "fulltextsp", true);
    index4 = new FullTextStringIndex(indexDirectory2, "fulltextsp2", true);
  }
}
