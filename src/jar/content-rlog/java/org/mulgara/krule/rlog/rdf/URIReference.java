/*
 * Copyright 2008 Fedora Commons, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mulgara.krule.rlog.rdf;

import java.net.URI;
import java.net.URISyntaxException;

import org.mulgara.krule.rlog.parser.NSUtils;

/**
 * A reference to a URI.
 *
 * @created May 2, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 * @licence <a href="http://www.opensource.org/licenses/apache2.0.php">Apache License, Version 2.0</a>
 */
public class URIReference implements RDFNode {

  private final URI uri;

  private final String prefix;

  private final String value;

  public URIReference(String prefix, String value) throws URISyntaxException {
    this.prefix = prefix;
    this.value = value;
    this.uri = NSUtils.newURI(prefix, value);
  }

  public URIReference(String value) throws URISyntaxException {
    this.prefix = NSUtils.getDefaultPrefix();
    this.value = value;
    this.uri = NSUtils.newURI(prefix, value);
  }

  /** Internal mechanism for setting each element manually from a factory. */
  private URIReference(String prefix, String value, URI uri) {
    this.prefix = prefix;
    this.value = value;
    this.uri = uri;
  }

  /**
   * Creates a URIReference. This requires the URI to be clean. If it is possible that this
   * can fail then use the constructor instead.
   * @param prefix A namespace prefix.
   * @param value The value within the namespace.
   * @return A new URIReference.
   */
  public static URIReference create(String prefix, String value) {
    try {
      return new URIReference(prefix, value);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to create a URI for: " + prefix + ":" + value);
    }
  }

  /**
   * Creates a URIReference. This requires the URI to be clean. If it is possible that this
   * can fail then use the constructor instead.
   * @param value The value within the namespace.
   * @return A new URIReference.
   */
  public static URIReference create(String value) {
    try {
      return new URIReference(value);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Unable to create a URI for: " + NSUtils.getDefaultDomain() + ":" + value);
    }
  }

  /**
   * Creates a URIReference. This requires the URI to be pre-built.
   * @param prefix A namespace prefix.
   * @param value The value within the namespace.
   * @param uri The string for the URI to wrap, and that is represented by prefix:value
   * @return A new URIReference.
   */
  public static URIReference create(String prefix, String value, String uriStr) {
    return new URIReference(prefix, value, URI.create(uriStr));
  }

  /** @see org.mulgara.krule.rlog.rdf.RDFNode#isVariable() */
  public boolean isVariable() {
    return false;
  }

  /** @see org.mulgara.krule.rlog.rdf.RDFNode#isReference() */
  public boolean isReference() {
    return true;
  }

  /** Get the URI this references. */
  public URI getURI() {
    return uri;
  }

  /** Get the prefix used for the URI. */
  public String getPrefix() {
    return prefix;
  }

  /** Get the value used for the URI. */
  public String getValue() {
    return value;
  }

  /** {@inheritDoc} */
  public String getRdfLabel() {
    return "#ref_" + prefix + value;
  }

  /** {@inheritDoc} */
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof URIReference)) return false;
    return uri.equals(((URIReference)o).getURI());
  }

  /** {@inheritDoc} */
  public int hashCode() {
    return uri.hashCode();
  }
}
