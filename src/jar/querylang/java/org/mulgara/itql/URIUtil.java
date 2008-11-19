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

package org.mulgara.itql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * A set of methods for managing common URI operations.
 *
 * @created 2007-08-09
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class URIUtil {

  /**
   * Replace an alias in a URI, if one is recognized.
   * @param uriString A string with the initial uri to check for aliases.
   * @param aliasMap The map of known aliases to the associated URIs
   * @return A new URI with the alias replaced, or the original if no alias is found.
   */
  public static URI convertToURI(String uriString, Map<String,URI> aliasMap) {
    try {
      URI uri = new URI(uriString);
      if (uri.isOpaque()) {
        // Attempt qname-to-URI substitution for aliased namespace prefixes
        URI mapping = aliasMap.get(uri.getScheme());
        if (mapping != null) {
          uri = new URI(mapping + uri.getSchemeSpecificPart());
        }
      }
      return uri;
    } catch (URISyntaxException e) {
      throw new RuntimeException("Bad URI syntax in resource", e);
    }
  }
  
}
