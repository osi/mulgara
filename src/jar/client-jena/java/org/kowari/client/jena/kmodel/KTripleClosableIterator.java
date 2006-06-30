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

package org.kowari.client.jena.kmodel;

//Java 2 standard packages
import java.net.*;
import java.util.*;

//Apache packages
import org.apache.log4j.Logger;

//Hewlett-Packard packages
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.util.iterator.*;

//JRDF packages
import org.jrdf.graph.SubjectNode;
import org.jrdf.graph.PredicateNode;
import org.jrdf.graph.ObjectNode;

//Kowari packages
import org.kowari.query.*;
import org.jrdf.graph.GraphException;

/**
 * A ClosableIterator wrapper around a Kowari Answer object.
 *
 * <p>This class is used internally to support the ExtendedIterator returned
 * by KGraph.find(...)</p>
 *
 * @created 2001-08-16
 *
 * @author Chris Wilper
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:34 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001-2003 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class KTripleClosableIterator
    implements ClosableIterator {

  /** Logger for this class  */
  private final static Logger log = Logger.getLogger(KTripleClosableIterator.class.
      getName());

  /** Wrapped answer object */
  private Answer answer = null;

  /** pre-cached next Triple */
  private Triple next = null;

  /** KGraph to be notified when this iterator closes */
  private KGraph closeListener = null;

  /** Indicates the Iterator has been closed */
  private boolean closed = false;

  /**
   * Construct a KTripleClosableIterator for the given Answer.
   *
   * @param ans Answer
   * @param closeListener KGraph
   * @throws JenaException
   */
  public KTripleClosableIterator(Answer ans,
                                 KGraph closeListener) throws JenaException {
    //reset answer
    try {

      ans.beforeFirst();
    }
    catch (TuplesException tuplesException) {

      throw new JenaException("Could not reset Answer.", tuplesException);
    }

    //initialise members
    this.closeListener = closeListener;
    this.answer = ans;

    //cache the next Triple
    next = getNext();
  }

  /**
   * Queue up the next Triple from the underlying Answer. This is necessary to
   * support the Iterator interface's hasNext() method.
   *
   * @throws JenaException
   * @return Triple
   */
  private Triple getNext() throws JenaException {

    try {

      //increment Answer
      if (answer.next()) {

        //get the next JRDF triple
        SubjectNode subject = (SubjectNode) answer.getObject(0);
        PredicateNode predicate = (PredicateNode) answer.getObject(1);
        ObjectNode object = (ObjectNode) answer.getObject(2);

        //convert to Jena triple and return
        return JenaUtil.convert(JRDFUtil.createTriple(subject, predicate,
            object));
      }
      else {

        //no more triples
        return null;
      }
    }
    catch (GraphException graphException) {

      throw new JenaException("Could not create Triple.", graphException);
    }
    catch (TuplesException tuplesException) {

      throw new JenaException("Could not get answer.next()", tuplesException);
    }
    catch (ClassCastException classException) {

      throw new JenaException("Answer is invalid. Does not contain: " +
                              "SubjectNode, PredicateNode, ObjectNode.",
                              classException);
    }
  }

  /**
   * Is there another item?
   *
   * @return boolean
   */
  public boolean hasNext() {

    return next != null;
  }

  /**
   * Get the next item, as a Jena Triple.
   *
   * @throws JenaException
   * @return Object
   */
  public Object next() throws JenaException {

    if (this.next == null || closed) {

      throw new NoSuchElementException("Closed or no more elements.");
    }

    //fetch pre-cached triple
    Triple next = this.next;

    //update next Triple
    this.next = getNext();

    if (log.isDebugEnabled()) {

      log.debug("Returning triple: " + next);
    }

    return next;
  }

  /**
   * Unsupported.
   *
   * @throws UnsupportedOperationException
   */
  public void remove() throws UnsupportedOperationException {

    throw new UnsupportedOperationException("remove() unsupported.");
  }

  /**
   * Close the iterator if it's not already closed.
   */
  public void close() {

    try {

      //close answer and notify graph
      if (!closed) {

        answer.close();
        closed = true;
        closeListener.iteratorClosed(this);
      }
    }
    catch (TuplesException tuplesException) {

      log.warn("Could not close Answer.", tuplesException);
    }
  }

}
