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

package org.kowari.jena;

// Java 2 standard packages
import java.util.*;

// Apache Log4J
import org.apache.log4j.Logger;

// Locally written classes
import org.kowari.query.*;

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.util.iterator.*;
import org.kowari.server.*;

/**
 * Takes an Answer and creates {@link Domain}s.  This is returned by
 * executeBindings.
 *
 * @created 2004-07-07
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/07 09:37:07 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class DomainIterator extends NiceIterator {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger logger =
      Logger.getLogger(DomainIterator.class.getName());

  /**
   * The answer to create domains with.
   */
  private Answer answer;

  /**
   * The session to use for globalization.
   */
  private LocalJenaSession jenaSession;

  /**
   * The number of variables.
   */
  private int numberOfNodes;

  /**
   * If there are more left in answer - basically caches answer.next().
   */
  private boolean hasNext = false;

  /**
   * Create a new DomainIterator with the Answer to iterate over.
   *
   * @param newAnswer the answer to iterator over.
   */
  public DomainIterator(Answer newAnswer, LocalJenaSession newJenaSession) {
    answer = newAnswer;
    jenaSession = newJenaSession;
    numberOfNodes = answer.getNumberOfVariables();

    try {
      if ((answer != null) && (answer.getRowCount() > 0)) {

        // Ensure tuples are at the start.
        answer.beforeFirst();

        // Go to first tuples
        hasNext = answer.next();
      }
    }
    catch (TuplesException te) {

      logger.error("Could not initalize tuples", te);
      throw new IllegalArgumentException("Could not initialize tuples");
    }
  }

  /**
   * Returns true if there is more in the answer.
   *
   * @return true if there is more in the answer.
   */
  public boolean hasNext() {
    return hasNext;
  }

  /**
   * Closes the answer object.
   */
  public void close() {
    try {
      answer.close();
    }
    catch (TuplesException te) {
      logger.error("Failed to close answer in DomainIterator", te);
    }
  }

  /**
   * Gets the next domain and returns it.
   *
   * @return the next domain.
   */
  public Object next() {
    if (hasNext()) {

      try {
        Node nodes[] = new Node[numberOfNodes];
        for (int index = 0; index < numberOfNodes; index++) {
           nodes[index] = jenaSession.getJenaFactory().convertValueToNode(
               (Value) answer.getObject(index));
        }

        hasNext = answer.next();
        return new Domain(nodes);
      }
      catch (TuplesException te) {
        te.printStackTrace();
      }
    }
    close();
    throw new NoSuchElementException("No more elements in iterator");
  }
}
