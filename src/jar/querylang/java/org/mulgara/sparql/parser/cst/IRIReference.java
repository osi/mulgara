/*
 * Copyright 2008 Fedora Commons
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mulgara.sparql.parser.cst;

import java.net.URI;
import java.net.URISyntaxException;

import org.mulgara.sparql.parser.ParseException;


/**
 * Represents IRI references in SPARQL. This is a superset of URI references.
 * For the moment, just wrap a URI, even though this doesn't meet the strict
 * definition of an IRI.
 *
 * @created Feb 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 * @licence <a href="{@docRoot}/../LICENCE.txt">Apache License, Version 2.0</a>
 */
public class IRIReference implements Node, PrimaryExpression {
  
  /** The RDF namespace */
  private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

  /** Constant IRI for RDF_TYPE */
  public static final IRIReference RDF_TYPE = new IRIReference(URI.create(RDF_NS + "type"));

  /** Constant IRI for RDF_FIRST */
  public static final IRIReference RDF_FIRST = new IRIReference(URI.create(RDF_NS + "first"));

  /** Constant IRI for RDF_REST */
  public static final IRIReference RDF_REST = new IRIReference(URI.create(RDF_NS + "rest"));

  /** Constant IRI for RDF_NIL */
  public static final IRIReference RDF_NIL = new IRIReference(URI.create(RDF_NS + "nil"));

  /** The internal URI value */
  private URI uri;

  /** The original text of the URI */
  private String text;

  /**
   * Create an IRI reference from a URI.
   * @param uri The URI referred to.
   */
  public IRIReference(URI uri) {
    this.uri = uri;
    text = "<" + uri.toString() + ">";
  }

  /**
   * Create an IRI reference from a URI.
   * @param uri The URI referred to.
   */
  public IRIReference(String uri) throws ParseException {
    try {
      this.uri = new URI(uri);
    } catch (URISyntaxException e) {
      throw new ParseException("Unable to create a URI from: " + uri);
    }
    text = "<" + uri + ">";
  }

  /**
   * Create an IRI reference from a URI with an abbreviated namespace.
   * @param uri The URI referred to.
   * @param text The abbreviated form.
   */
  public IRIReference(String uri, String text) throws ParseException {
    this(uri);
    this.text = text;
  }

  /**
   * @return the IRI value
   */
  public URI getUri() {
    return uri;
  }

  /**
   * @see org.mulgara.sparql.parser.cst.Node#getImage()
   */
  public String getImage() {
    return text;
  }
  
}
