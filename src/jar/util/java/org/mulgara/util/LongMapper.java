/*
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

package org.mulgara.util;

/**
 * Objects of this type are used for mapping longs to longs. This can be easily met
 * with a Map<Long,Long>, or an IntFile, but this interface abstracts the interface
 * to allow manipulation of the number space if this is more efficient.
 *
 * @created Sep 26, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface LongMapper {

  /**
   * Map a long value to another value.
   * @param key The key.
   * @param value The value to map the key to.
   * @throws Exception An implementation specific error.
   */
  public void putLong(long key, long value) throws Exception;

  /**
   * Retrieve a long value, given a key.
   * @param key The key to look up with.
   * @return The value the key is mapped to, or 0 if it is not mapped to anything.
   * @throws Exception An implementation specific error.
   */
  public long getLong(long key) throws Exception;

  /**
   * Removes any resources associated with this mapper.
   * @throws Exception An implementation specific error.
   */
  public void delete() throws Exception;
}
