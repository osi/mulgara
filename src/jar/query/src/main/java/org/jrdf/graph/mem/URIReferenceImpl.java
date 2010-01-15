/*
 * $Header$
 * $Revision: 624 $
 * $Date: 2006-06-24 21:02:12 +1000 (Sat, 24 Jun 2006) $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 The JRDF Project.  All rights reserved.
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

package org.jrdf.graph.mem;

// Java 2 standard packages
import org.jrdf.graph.AbstractURIReference;

import java.net.URI;

/**
 * RDF URI reference - resource node. Some RDF resources are properties. It's
 * not always possible to say whether a resource is a property or not until
 * it's used as a predicate, because we don't always have access to an RDF
 * Schema that defines the property.
 *
 * A URI Reference can be any part of an RDF Triple: subject, predicate or
 * object.
 *
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 *
 * @version $Revision: 624 $
 */
public class URIReferenceImpl extends AbstractURIReference implements MemNode {

  /**
   * Allow newer compiled version of the stub to operate when changes
   * have not occurred with the class.
   * NOTE : update this serialVersionUID when a method or a public member is
   * deleted.
   */
  private static final long serialVersionUID = -4120822410726711976L;

  /** The internal identifier for this node. */
  private Long id;


  /**
   * Constructor for use by GraphElementFactory only.
   *
   * @param newUri The newUri of this node.
   * @param newId The internal identifier for this node.
   */
  URIReferenceImpl(URI newUri, Long newId) {
    super(newUri);
    id = newId;
  }

  /**
   * Constructor for use by GraphElementFactory only.
   *
   * @param newUri The uri of this node.
   * @param validate whether to enforce valid RDF URIs.
   * @param newId The internal identifier for this node.
   */
  URIReferenceImpl(URI newUri, boolean validate, Long newId) {
    super(newUri, validate);
    id = newId;
  }


  /**
   * Retrieves an internal identifier for this node.
   *
   * @return A numeric identifier for this node.
   */
  public Long getId() {
    return id;
  }
}
