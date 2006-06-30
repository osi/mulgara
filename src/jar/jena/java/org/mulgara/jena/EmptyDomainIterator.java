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

// Java 2 standard packages
import java.util.*;

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.*;
import com.hp.hpl.jena.util.iterator.*;

/**
 * An empty representation of a DomainIterator - used when we want to
 * represent an empty answer.
 *
 * @created 2004-07-07
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $ by $Author: newmana $
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
public class EmptyDomainIterator implements ExtendedIterator {

  /**
   * An empty node list - equivalent to an empty SELECT.
   */
  private static final Node[] EMPTY = new Node[] {};

  /**
   * The position we are - just 1 or 0.
   */
  private int index = 0;

  /**
   * Whether the domain should return a Domain with an EMPTY node list in it
   * or a list with 1 null entry.
   */
  private boolean returnNull = false;

  /**
   * Create a EmptyDomainIterator that returns an EMPTY node list.
   */
  public EmptyDomainIterator() {
  }

  /**
   * Create a EmptyDomainIterator that return a list with 1 null entry if
   * doReturnNull is true otherwise returns the Domain with an EMPTY node
   * list.
   *
   * @param doReturnNull true to return a list with 1 null entry.
   */
  public EmptyDomainIterator(boolean doReturnNull) {
    returnNull = doReturnNull;
  }

  /**
   * True if we haven't called next once.
   *
   * @return true if we haven't called next once.
   */
  public boolean hasNext() {
    return index != 1;
  }

  /**
   * Returns the appropriate representation of an empty result set.
   *
   * @return the appropriate representation of an empty result set.
   */
  public Object next() {
    if (index == 0) {
      index++;
      if (returnNull) {
        return new Domain(1);
      }
      else {
        return new Domain(EMPTY);
      }
    }
    throw new NoSuchElementException("No more elements in iterator");
  }

  public void remove() {
  }

  public void close() {
  }

  public ExtendedIterator andThen(ClosableIterator iter) {
    return null;
  }

  public ExtendedIterator filterDrop(Filter filter) {
    return null;
  }

  public ExtendedIterator filterKeep(Filter filter) {
    return null;
  }

  public ExtendedIterator mapWith(Map1 map) {
    return null;
  }

  public Object removeNext() {
    return null;
  }
}
