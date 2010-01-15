/*
 * $Header$
 * $Revision: 624 $
 * $Date: 2006-06-24 21:02:12 +1000 (Sat, 24 Jun 2006) $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003, 2004 The JRDF Project.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        the JRDF Project (http://jrdf.sf.net/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The JRDF Project" and "JRDF" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, please contact
 *    newmana@users.sourceforge.net.
 *
 * 5. Products derived from this software may not be called "JRDF"
 *    nor may "JRDF" appear in their names without prior written
 *    permission of the JRDF Project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the JRDF Project.  For more
 * information on JRDF, please see <http://jrdf.sourceforge.net/>.
 */

package org.jrdf.graph;

import java.io.Serializable;

/**
 * A base implementation of an RDF {@link Triple}.
 *
 * @author Andrew Newman
 *
 * @version $Revision: 624 $
 */
public abstract class AbstractTriple implements Triple, Serializable {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  private static final long serialVersionUID = 8737092494833012690L;

  /**
   * Subject of this statement.
   */
  protected SubjectNode subjectNode;

  /**
   * Predicate of this statement.
   */
  protected PredicateNode predicateNode;

  /**
   * Object of this statement.
   */
  protected ObjectNode objectNode;

  /**
   * Obtains the subject of this statement.
   *
   * @return an {@link SubjectNode} which is either a {@link BlankNode} or
   *     {@link URIReference}
   */
  public SubjectNode getSubject() {
    return subjectNode;
  }

  /**
   * Obtains the predicate of this statement.
   *
   * @return a {@link PredicateNode} which is a {@link URIReference}
   */
  public PredicateNode getPredicate() {
    return predicateNode;
  }

  /**
   * Obtains the object of this statement.
   *
   * @return a {@link ObjectNode} which is either a {@link BlankNode},
   *     {@link URIReference} or {@link Literal}
   */
  public ObjectNode getObject() {
    return objectNode;
  }

  public boolean equals(Object obj) {

    // Check equal by reference
    if (this == obj) {
      return true;
    }

    boolean returnValue = false;

    // Check for null and ensure exactly the same class - not subclass.
    if (null != obj) {

      try {

        Triple tmpTriple = (Triple) obj;
        returnValue = getSubject().equals(tmpTriple.getSubject()) &&
            getPredicate().equals(tmpTriple.getPredicate()) &&
            getObject().equals(tmpTriple.getObject());
      }
      catch (ClassCastException cce) {
        // Leave return value to be false.
      }
    }
    return returnValue;
  }

  public int hashCode() {
    return getSubject().hashCode() ^ getPredicate().hashCode() ^
        getObject().hashCode();
  }

  /**
   * Provide a legible representation of a triple. Currently, square brackets
   * with toString values of the parts of the triple.
   *
   * @return the string value of the subject, predicate and object in square
   *   brackets.
   */
  public String toString() {
    return "[" + getSubject() + ", " + getPredicate() + ", " + getObject() +
        "]";
  }
}
