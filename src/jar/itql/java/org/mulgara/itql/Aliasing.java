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
package org.mulgara.itql;

import java.net.URI;
import java.util.Map;

/**
 * An interface for indicating that an object holds and manages aliases.
 * @created Sep 26, 2007
 * @author Paul Gearon
 * @copyright &copy;2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
interface Aliasing {
  
  /**
   * Sets the alias map associated with this session.
   *
   * @param aliasMap the alias map associated with this interpreter
   */
  void setAliasMap(Map<String,URI> aliasMap);


  /**
   * Returns the alias map associated with this session.
   *
   * @return the alias namespace map associated with this session
   */
  Map<String,URI> getAliasMap();

}
