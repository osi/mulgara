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

package org.mulgara.client.jrdf.itql;

// Java 2 standard packages
import java.util.*;
import java.io.*;
import java.net.*;

// Log4J
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.*;
import org.jrdf.util.ClosableIterator;

//Mulgara
import org.mulgara.client.jrdf.*;
import org.mulgara.client.jrdf.answer.*;
import org.mulgara.client.jrdf.exception.*;
import org.mulgara.query.*;
import org.mulgara.server.*;

/**
 * RemoteGraphProxy implementation that retrieves it's data from a Session.
 *
 * @created 2004-07-29
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.3 $
 *
 * @modified $Date: 2005/01/28 00:27:14 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 *
 * Portions by Paul Gearon.
 * @copyright &copy;2006 <a href="http://www.herzumsoftware.com/">Herzum Software LLC</a>
 */
public class SessionGraphProxy implements RemoteGraphProxy {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger log = Logger.getLogger(SessionGraphProxy.class.
      getName());

  /** Database Session */
  private JRDFSession session = null;

  /** URI for model */
  private URI modelURI = null;

  /** Used to create local objects */
  private GraphElementBuilder builder = null;

  /** Map of Clasable iterator to be closed with this graph */
  private List iterators = null;

  /** Indicates the Proxy has been closed */
  private boolean closed = false;

  /**
   * Constructor.
   *
   * @param session Session
   * @param modelURI URI
   * @throws GraphException
   */
  public SessionGraphProxy(JRDFSession session, URI modelURI) throws GraphException {

    super();

    //validate
    if (session == null) {

      throw new GraphException("Session cannot be null.");
    }

    if (modelURI == null) {

      throw new GraphException("Model URI cannot be null.");
    }

    //initialize members
    this.session = session;
    this.modelURI = modelURI;
    this.builder = new GraphElementBuilder();
    this.iterators = new ArrayList();
  }

  /**
   * Test the graph for the occurrence of the triple.  A null value for any
   * of the parts of a triple are treated as unconstrained, any values will be
   * returned.
   *
   * @param triple The triple to find.
   * @return True if the triple is found in the graph, otherwise false.
   * @throws GraphException If there was an error accessing the graph.
   */
  public boolean contains(Triple triple) throws GraphException {

    //ensure the graph is not closed
    if (this.closed) {
      throw new GraphException("Graph has been closed.");
    }

    if (this.session == null) {
      throw new IllegalStateException("Session not initialized");
    }
    if (triple == null) {
      throw new IllegalArgumentException("Cannot search for null Triple");
    }
    return this.session.contains(this.modelURI, triple.getSubject(),
        triple.getPredicate(), triple.getObject());
  }

  /**
   * Returns an iterator to a set of statements that match a given subject,
   * predicate and object. A null value for any of the parts of a triple are
   * treated as unconstrained, any values will be returned.
   *
   * @param triple The triple to find.
   * @throws GraphException If there was an error accessing the graph.
   * @return ClosableIterator
   */
  public ClosableIterator find(Triple triple) throws GraphException {

    //ensure the graph is not closed
    if (this.closed) {
      throw new GraphException("Graph has been closed.");
    }

    //execute the query and create a closable iterator from the answer
    try {
      Answer answer = this.session.find(this.modelURI, triple.getSubject(),
          triple.getPredicate(), triple.getObject());
      return this.createClosableIterator(answer);
    }
    catch (JRDFClientException clientException) {
      throw new GraphException("Could not create new ClosableIterator.",
                               clientException);
    }
  }

  /**
   * Executes an iTQL command to insert the triples.
   *
   * @param triples The triple iterator.
   * @throws GraphException If the statements can't be made.
   */
  public void add(Iterator triples) throws GraphException {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    //insert a set of triples into the session
    try{

      Set tripleSet = new HashSet();
      while (triples.hasNext()) {

        tripleSet.add(triples.next());
      }
      session.insert(this.modelURI, tripleSet);
    } catch (QueryException queryException) {

      throw new GraphException("Could not add triples.", queryException);
    }
  }

  /**
   * Executes an iTQL command to delete the triples.
   *
   * @param triples The triple iterator.
   * @throws GraphException If the statements can't be revoked.
   */
  public void remove(Iterator triples) throws GraphException {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    //insert a set of triples into the session
    try {

      Triple currentTriple = null;
      Set tripleSet = new HashSet();
      while (triples.hasNext()) {

        currentTriple = (Triple) triples.next();
        if (currentTriple == null) {
          throw new IllegalStateException("Calling remove with no current triple");
        }

        //check that the triple occurs before adding it
        if (!this.contains(currentTriple)) {
          throw new GraphException("Cannot delete Triple: '" + currentTriple +
              "'. Graph does not contain Triple.");
        }

        tripleSet.add(currentTriple);
      }
      session.delete(this.modelURI, tripleSet);
    } catch (QueryException queryException) {

      throw new GraphException("Could not remove triples.", queryException);
    }
  }

  /**
   * Returns the node factory for the graph, or creates one.
   *
   * @return the node factory for the graph, or creates one.
   */
  public GraphElementFactory getElementFactory() {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    return this.builder;
  }

  /**
   * Returns the number of rows in the Answer.
   *
   * @return the number of rows in the Answer.
   */
  public long getNumberOfTriples() {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    Answer answer = null;

    try {

      //execute and get row count
      answer = this.session.find(this.modelURI, null, null, null);
      return answer.getRowCount();
    }
    catch (Exception exception) {

      //rethrow
      throw new JRDFClientException("Could not determine number of Triples.",
                                    exception);
    }
    finally {

      //close the answer
      if (answer != null) {

        try {

          answer.close();
        }
        catch (TuplesException tuplesException) {

          throw new JRDFClientException("Could not close Answer.",
                                        tuplesException);
        }
      }
    }
  }

  /**
   * Returns true if the answer's row cardinality is 0.
   *
   * @return true if the answer's row cardinality is 0.
   */
  public boolean isEmpty() {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    //check number of triples
    return (this.getNumberOfTriples() <= 0);
  }

  /**
   * Closes the underlying Answer.
   *
   * @throws JRDFClientException
   */
  public void close() throws JRDFClientException {

    //only close once
    if (!this.closed) {

      //close all created ClosableIterators
      while (iterators.size() != 0) {
        ((ClosableIterator) iterators.remove(0)).close();
      }
    }

    this.closed = true;
  }

  /**
   * Removes the iterator from the list to be closed with the graph.
   *
   * @param iter Iterator
   */
  public void unregister(Iterator iter) {
    if (iterators.contains(iter)) {
      this.iterators.remove(iter);
    }
  }

  /**
   * Factory method used to create an iterator for the Triples.
   *
   * @param triples Triple[]
   * @return ClientClosableIterator
   */
  public ClientClosableIterator createClosableIterator(Triple[] triples) {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    //create iterator and hold a reference to it
    ClientClosableIterator iterator = this.builder.createClosableIterator(
       this, triples);
    this.iterators.add(iterator);

    return iterator;
  }

  /**
   * Creates a closable iterator for the Answer, using the Triple as a Filter.
   *
   * @param filter Triple
   * @param answer Answer
   * @return ClientClosableIterator
   * @throws JRDFClientException
   */
  public ClientClosableIterator createClosableIterator(Triple filter,
      Answer answer) throws JRDFClientException {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    ClosableAnswerIteratorProxy proxy = new ClosableAnswerIteratorProxy(this,
        filter, answer);

    //create iterator and hold a reference to it (for closing)
    ClientClosableIterator iterator = new ClientClosableIterator(this, proxy);
    this.iterators.add(iterator);

    return iterator;
  }

  /**
   * Creates a closable iterator for the Answer, using no Filter.
   *
   * @param answer Answer
   * @return ClientClosableIterator
   * @throws JRDFClientException
   */
  public ClientClosableIterator createClosableIterator(Answer answer) throws
      JRDFClientException {

    //ensure the graph is not closed
    if (this.closed) {

      throw new JRDFClientException("Graph has been closed.");
    }

    try {

      //this filter will allow all triples through
      Triple filter = this.builder.createTriple(null, null, null);
      return this.createClosableIterator(filter, answer);
    } catch (GraphElementFactoryException factoryException) {

      throw new JRDFClientException("Could not create null filter Triple.",
                                    factoryException);
    }
  }

  public Session getSession() {
    return session;
  }
}
