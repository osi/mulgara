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

package org.mulgara.client.jrdf.test;

import java.io.*;

import org.jrdf.graph.*;
import org.jrdf.vocabulary.*;

// Third party packages
import junit.framework.*;
import java.net.*;
import java.util.*;
import org.jrdf.graph.mem.*;
import org.mulgara.client.jrdf.*;
import org.mulgara.jrdf.*;
import org.mulgara.query.*;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.TripleImpl;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.server.*;
import org.mulgara.server.driver.*;


/**
 * Unit test for client-side JRDF Graph representing a kowari model (modelURI)
 * and uses an ItqlInterpreterBeean and a Session.
 *
 * @created 2004-08-24
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/13 11:57:00 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class RemoteAnswerCloneUnitTest extends TestCase {

  /** name used for the server */
  private static String SERVER_NAME = "server1";

  /** name of the model */
  private static String MODEL_NAME = "remoteAnswerCloneModel";

  /** URI for the kowari server */
  private static URI serverURI = null;

  /** URI for the test model */
  private static URI modelURI = null;

  /** The session used by the graph (and setting up) */
  private JRDFSession session = null;

  /** Nodes used for test data */
  private URIReference reference1 = null;
  private URIReference reference2 = null;
  private Literal literal = null;


  /**
   * Constructs a new test with the given name.
   *
   * @param name the name of the test
   */
  public RemoteAnswerCloneUnitTest(String name) {
    super(name);
  }

  /**
   * Create a graph implementation.
   *
   * @return A new RemoteAnswerCloneUnitTest.
   * @throws Exception
   */
  public Graph newGraph() throws Exception {

    //reset graph
    this.dropModel(this.modelURI);
    this.createModel(this.modelURI);

    //create and return graph
    return AbstractGraphFactory.createGraph(this.serverURI, this.modelURI);
  }

  /**
   * Hook for test runner to obtain a test suite from.
   *
   * @return The test suite
   */
  public static Test suite() {

    TestSuite suite = new TestSuite();
    suite.addTest(new RemoteAnswerCloneUnitTest("testClone"));
    suite.addTest(new RemoteAnswerCloneUnitTest("testCloneOfClones"));
    return suite;
  }

  /**
   * Default test runner.
   *
   * @param args The command line arguments
   * @throws Exception
   */
  public static void main(String[] args) throws Exception {

    junit.textui.TestRunner.run(suite());
  }

  /**
   * Ensures operations can be performed on a cloned Answer after the original
   * has been closed.
   *
   * @throws Exception
   */
  public void testClone() throws Exception {

    try {

      //test answer with entire graph
      Answer answer = this.session.find(this.modelURI, null, null, null);
      this.testClone(answer);

      //test constrained answers
      Answer constrainedS = this.session.find(this.modelURI, this.reference1,
          null, null);
      this.testClone(constrainedS);

      Answer constrainedP = this.session.find(this.modelURI, null,
          this.reference1, null);
      this.testClone(constrainedP);

      Triple constrainedTripleO = new TripleImpl(null, null, this.reference1);
      Answer constrainedO = this.session.find(this.modelURI,
          constrainedTripleO.getSubject(), constrainedTripleO.getPredicate(),
          constrainedTripleO.getObject());
      this.testClone(constrainedO);

      Answer constrainedSP = this.session.find(this.modelURI, this.reference1,
          this.reference1, null);
      this.testClone(constrainedSP);

      Answer constrainedPO = this.session.find(this.modelURI, null,
          this.reference1, this.reference1);
      this.testClone(constrainedPO);

      Answer constrainedOS = this.session.find(this.modelURI, this.reference1,
          null, this.reference1);
      this.testClone(constrainedOS);

      //test unconstrained answer
      Answer unconstrained = this.session.find(this.modelURI, this.reference1,
          this.reference1, this.reference1);
      this.testClone(unconstrained);
    } catch (Exception exception) {

      exception.printStackTrace();
      throw exception;
    }
  }

  /**
   * clones the orignal, closes it and then performs operations on the clone.
   *
   * @param original Answer
   * @throws Exception
   */
  private void testClone(Answer original) throws Exception {

    try {

      //clone it
      Answer clone = (Answer) original.clone();
      assertNotNull("Answer.clone() returned null.", clone);

      //close it
      original.close();

      //perform operations on clone
      clone.beforeFirst();
      assertNotNull("clone.getVariables() returned null.", clone.getVariables());

      //iterate over the answer
      int columns = clone.getNumberOfVariables();
      while (clone.next()) {

        //get each object
        for (int i = 0; i < columns; i++) {
          clone.getObject(i);
        }
      }

      //close the clone
      clone.close();
    } catch (Exception e) {

      //print and re-throw
      e.printStackTrace();
      throw e;
    }
  }

  /**
   * Ensures operations can be performed on Answer's that are cloned multiple
   * times.
   *
   * @throws Exception
   */
  public void testCloneOfClones() throws Exception {

    try {

      //test answer with entire graph
      Answer answer = this.session.find(this.modelURI, null, null, null);
      this.testCloneOfClones(answer);

      //test constrained answers
      Answer constrainedS = this.session.find(this.modelURI, this.reference1,
          null, null);
      this.testCloneOfClones(constrainedS);

      Answer constrainedP = this.session.find(this.modelURI, null,
          this.reference1, null);
      this.testCloneOfClones(constrainedP);

      Answer constrainedO = this.session.find(this.modelURI, null, null,
          this.reference1);
      this.testCloneOfClones(constrainedO);

      Answer constrainedSP = this.session.find(this.modelURI, this.reference1,
          this.reference1, null);
      this.testCloneOfClones(constrainedSP);

      Answer constrainedPO = this.session.find(this.modelURI, null,
          this.reference1, this.reference1);
      this.testCloneOfClones(constrainedPO);

      Answer constrainedOS = this.session.find(this.modelURI, this.reference1,
          null, this.reference1);
      this.testCloneOfClones(constrainedOS);

      //test unconstrained answer
      Answer unconstrained = this.session.find(this.modelURI, this.reference1,
          this.reference1, this.reference1);
      this.testCloneOfClones(unconstrained);
    } catch (Exception exception) {

      exception.printStackTrace();
      throw exception;
    }
  }

  /**
   * clones the orignal, closes it and then performs operations on the clone.
   *
   * @param original Answer
   * @throws Exception
   */
  private void testCloneOfClones(Answer original) throws Exception {

    //test a clone of a clone
    Answer answerClone = (Answer) original.clone();
    Answer answerCloneClone = (Answer) answerClone.clone();
    Answer answerCloneCloneClone = (Answer) answerCloneClone.clone();

    //test each level (also closes)
    this.testClone(original);
    this.testClone(answerClone);
    this.testClone(answerCloneClone);
    this.testClone(answerCloneCloneClone);
  }

  /**
   * Adds triples to the graph.
   *
   * @throws Exception
   */
  private void populate() throws Exception {

    Set triples = new HashSet();

    //create triples that can be searched for
    triples.add(new TripleImpl(reference1, reference1, literal));
    triples.add(new TripleImpl(reference1, reference2, literal));
    triples.add(new TripleImpl(reference2, reference1, literal));
    triples.add(new TripleImpl(reference2, reference2, literal));

    triples.add(new TripleImpl(reference1, reference1, reference1));
    triples.add(new TripleImpl(reference1, reference2, reference1));
    triples.add(new TripleImpl(reference2, reference1, reference1));
    triples.add(new TripleImpl(reference2, reference2, reference1));

    triples.add(new TripleImpl(reference1, reference1, reference2));
    triples.add(new TripleImpl(reference1, reference2, reference2));
    triples.add(new TripleImpl(reference2, reference1, reference2));
    triples.add(new TripleImpl(reference2, reference2, reference2));

    //create arbitary triples
    int NUM_TRIPLES = 5000;
    String baseURI = "http://tucana.org/tucana#triple_";
    URIReference currentNode = null;

    for (int i = 0; i < NUM_TRIPLES; i++) {

      //add triple with new node
      currentNode = new URIReferenceImpl(new URI(baseURI + i));
      triples.add(new TripleImpl(currentNode, currentNode,
                                            currentNode));
    }

    //insert
    this.session.insert(this.modelURI, triples);

  }

  //set up and tear down

  /**
   * Initialise members.
   *
   * @throws Exception if something goes wrong
   */
  public void setUp() throws Exception {

    // Store persistence files in the temporary directory
    try {

      String hostname = InetAddress.getLocalHost().getCanonicalHostName();
      this.serverURI = new URI("rmi", hostname, "/" + SERVER_NAME, null);
      this.modelURI = new URI("rmi", hostname, "/" + SERVER_NAME, MODEL_NAME);

      //get session
      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(
          this.serverURI, true);
      this.session = (JRDFSession) sessionFactory.newJRDFSession();

      //create test triples
      this.reference1 = new URIReferenceImpl(new URI("http://tucana.org/tucana#testReference"));
      this.reference2 = new URIReferenceImpl(new URI("http://tucana.org/tucana#testReference2"));
      this.literal = new LiteralImpl("test Literal");

      //initialize model
      this.createModel(this.modelURI);
      this.populate();

      //let superclass set up too
      super.setUp();
    }
    catch (Exception exception) {

      //try to tear down first
      try {

        tearDown();
      }
      finally {

        throw exception;
      }
    }
  }

  /**
   * The teardown method for JUnit
   *
   * @throws Exception EXCEPTION TO DO
   */
  public void tearDown() throws Exception {

    this.dropModel(this.modelURI);

    //allow super to close down too
    super.tearDown();
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @param modelURI URI
   * @throws Exception
   */
  private void createModel(URI modelURI) throws Exception {

    this.session.createModel(modelURI, new URI("http://tucana.org/tucana#Model"));
  }

  /**
   * Returns an answer that contains all the statements for the graph.
   *
   * @param modelURI URI
   * @throws Exception
   */
  private void dropModel(URI modelURI) throws Exception {

    this.session.removeModel(modelURI);
  }
}
