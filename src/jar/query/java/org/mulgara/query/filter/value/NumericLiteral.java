/**
 * The contents of this file are subject to the Open Software License
 * Version 3.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.opensource.org/licenses/osl-3.0.txt
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 */
package org.mulgara.query.filter.value;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * A numeric value.  Expect that this will be extended into Double, Integer, Long, etc.
 *
 * @created Mar 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class NumericLiteral extends TypedLiteral implements NumericExpression {

  /** Generated Serialization ID for RMI */
  private static final long serialVersionUID = -2601769069462524423L;

  /**
   * Creates the value to wrap the number
   * @param n The number to wrap
   */
  public NumericLiteral(Number n) {
    super(n, typeMap.get(n.getClass()));
  }

  /** @see org.mulgara.query.filter.value.NumericExpression#getNumber() */
  public Number getNumber() {
    return (Number)value;
  }

  /**
   * Gets the IRI that is used to represent the given numeric type.
   * @param n The number to get the type for.
   * @return An IRI containing the XSD datatype of n.
   */
  public static IRI getTypeFor(Number n) {
    return new IRI(typeMap.get(n.getClass()));
  }

  /** A mapping of numeric types to their URIs */
  private static final Map<Class<? extends Number>,URI> typeMap = new HashMap<Class<? extends Number>,URI>();
  
  static {
    typeMap.put(Float.class, URI.create(XSD_NS + "float"));
    typeMap.put(Double.class, URI.create(XSD_NS + "double"));
    typeMap.put(Long.class, URI.create(XSD_NS + "long"));
    typeMap.put(Integer.class, URI.create(XSD_NS + "int"));
    typeMap.put(Short.class, URI.create(XSD_NS + "short"));
    typeMap.put(Byte.class, URI.create(XSD_NS + "byte"));
  }
}
