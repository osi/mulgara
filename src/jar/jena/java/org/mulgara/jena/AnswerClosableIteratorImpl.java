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

package org.mulgara.jena;

// Standard java
import java.net.URI;
import java.util.NoSuchElementException;

// Log4J
import org.apache.log4j.*;

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.util.iterator.ClosableIterator;

// Internal packages
import org.mulgara.query.Answer;
import org.mulgara.query.Cursor;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Value;
import org.mulgara.store.statement.StatementStore;
import org.mulgara.server.*;

/**
 * An implementation {@link com.hp.hpl.jena.util.iterator.ClosableIterator}
 * that wraps {@link org.mulgara.query.Answer} and produces Jena triples.
 *
 * @created 2003-02-12
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.11 $
 *
 * @modified $Date: 2005/02/02 21:14:02 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class AnswerClosableIteratorImpl implements ClosableIterator {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(AnswerClosableIteratorImpl.class.getName());

  /**
   * The answer object that is being wrapped.
   */
  private Answer answer;

  /**
   * The Jena graph that created the iterator.  Used to tell it that it was
   * closed correctly.
   */
  //private GraphMulgara graph;

  /**
   * The triple representing the search term requested.  Nulls represent
   * wildcards.
   */
  private Triple tripleFilter;

  /**
   * Indexes to the subject, predicate and object in the tuples object.
   */
  private int subjectIndex, predicateIndex, objectIndex;

  /**
   * Jena factory.
   */
  private JenaFactory jenaFactory;

  /**
   * Graph URI.
   */
  private URI graphURI;

  /**
   * Whether there is a next element.
   */
  private boolean hasNext = false;

  /**
   * Iterator handler
   */
  private IteratorHandler iteratorHandler;

  /**
   * Creates a new iterator.
   *
   * @param newAnswer the answer object to wrap.
   * @param newTripleFilter the triple where null values in the subject,
   *   predicate or object represent wildcards.
   * @param graphURI the identifier of the graph.
   * @param factory the factory to create Jena objects from JRDF objects.
   * @param newHandler the handler to be used to ensure results (iterators) are
   *   closed correctly.
   * @throws IllegalArgumentException if the session, filter or graph objects
   *   are invalid.
   */
  public AnswerClosableIteratorImpl(Answer newAnswer, Triple newTripleFilter,
      URI graphURI, JenaFactory factory, IteratorHandler newHandler)
      throws IllegalArgumentException {

    if (newTripleFilter == null) {
      throw new IllegalArgumentException("Triple filter cannot be null");
    }
    if (newHandler == null) {
      throw new IllegalArgumentException("Iterator Handler cannot be null");
    }


    answer = newAnswer;
    tripleFilter = newTripleFilter;
    this.graphURI = graphURI;
    jenaFactory = factory;
    iteratorHandler = newHandler;

    try {

      if ((answer != null) && (answer.getRowCardinality() > Cursor.ZERO)) {

        // Ensure answer is at the start.
        answer.beforeFirst();

        // Go to first answer.
        hasNext = answer.next();

        // Set the indexes of the columns into the answer.
        if (tripleFilter.getSubject() == Node.ANY) {
          subjectIndex = answer.getColumnIndex(StatementStore.VARIABLES[0]);
        }
        if (tripleFilter.getPredicate() == Node.ANY) {
          predicateIndex = answer.getColumnIndex(StatementStore.VARIABLES[1]);
        }
        if (tripleFilter.getObject() == Node.ANY) {
          objectIndex = answer.getColumnIndex(StatementStore.VARIABLES[2]);
        }
      }
    }
    catch (TuplesException te) {
      logger.error("Could not initalize tuples", te);
      throw new IllegalArgumentException("Could not initialize tuples");
    }
  }

  /**
   * Returns true if another object is available.  Simply calls the wrapped
   * closableIterator.
   *
   * @return if another object is available.
   */
  public boolean hasNext() {
    return hasNext;
  }

  /**
   * Returns the current triple that the iterator is on.  This will be a
   * {@link com.hp.hpl.jena.graph.Triple}.  Will return NoSuchElement if next()
   * fails.
   *
   * @throws NoSuchElementException if there are no more elements.
   * @return a {@link com.hp.hpl.jena.graph.Triple}.
   */
  public Object next() throws NoSuchElementException {
    try {
      Node s, p, o;

      // If the subject was a wild card get it from the answer
      if (tripleFilter.getSubject() == Node.ANY) {
        Value v = (Value) answer.getObject(subjectIndex);
        s = jenaFactory.convertValueToNode(v);
      }
      else {
        s = tripleFilter.getSubject();
      }

      // If the predicate was a wild card get it from the answer
      if (tripleFilter.getPredicate() == Node.ANY) {
        Value v = (Value) answer.getObject(predicateIndex);
        p = jenaFactory.convertValueToNode(v);
      }
      else {
        p = tripleFilter.getPredicate();
      }

      // If the object was a wild card get it from the answer
      if (tripleFilter.getObject() == Node.ANY) {
        Value v = (Value) answer.getObject(objectIndex);
        o = jenaFactory.convertValueToNode(v);
      }
      else {
        o = tripleFilter.getObject();
      }

      // Get the next answer.
      hasNext = answer.next();

      //Recreate the triple
      return new Triple(s, p, o);
    }
    catch (TuplesException te) {
      te.printStackTrace();
      try {
        answer.beforeFirst();
      }
      catch (Exception e) {}
      logger.warn("Failed to create triple", te);
    }

    // Close iterator and throw NoSuchElementException
    close();
    throw new NoSuchElementException("No more elements in iterator");
  }

  /**
   * Not supported.
   *
   * @throws UnsupportedOperationException this will be thrown if called.
   */
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Does not support remove");
  }

  /**
   * Returns the hashcode based on the triple filter and graph URI hash code.
   *
   * @return the hashcode based on the triple filter and graph URI hash code.
   */
  public int hashCode() {
    return  tripleFilter.hashCode() ^ graphURI.hashCode() ^
        answer.hashCode();
  }

  /**
   * Returns equals on a answer if the triple filter
   */
  public boolean equals(Object obj) {

    // Always return true by identity.
    if (obj == this) {
      return true;
    }

    // Gotta be non-null and of matching type
    if ((obj != null) && (obj instanceof AnswerClosableIteratorImpl)) {
      AnswerClosableIteratorImpl tmpAns = (AnswerClosableIteratorImpl)
          obj;
      if (tmpAns.tripleFilter.equals(tripleFilter) &&
          tmpAns.graphURI.equals(graphURI) &&
          System.identityHashCode(tmpAns.answer) == System.identityHashCode(answer)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Closes the resources held by this iterator.
   */
  public void close() {
    try {
      closeAnswer();
    }
    finally {

      // Inform handler of successful closure.
      iteratorHandler.deregisterIterator(this);
    }
  }

  /**
   * Closes the underlying answer.
   */
  void closeAnswer() {
    try {

      // Close tuple.
      answer.close();
    }
    catch (TuplesException te) {
      logger.warn("Failed to close tuples", te);
    }
  }
}
