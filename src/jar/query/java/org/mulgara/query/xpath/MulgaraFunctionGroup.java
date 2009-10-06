/*
 * Copyright 2009 DuraSpace.
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

package org.mulgara.query.xpath;

import java.net.URI;
import java.util.Set;

/**
 * Represents a group of functions exposed as XPathFunctions in a single namespace.
 *
 * @created Oct 5, 2009
 * @author Paul Gearon
 * @copyright &copy; 2009 <a href="http://www.duraspace.org/">DuraSpace</a>
 */
public abstract class MulgaraFunctionGroup {

  /**
   * Get the prefix used for the namespace of these operations.
   * @return The short string used for a prefix in a QName.
   */
  public abstract String getPrefix();

  /**
   * Get the namespace of these operations.
   * @return The string of the namespace URI.
   */
  public abstract String getNamespace();

  /**
   * Get the set of function in this group.
   * @return A set of MulgaraFunction for this entire group.
   */
  public abstract Set<MulgaraFunction> getAllFunctions();

}
