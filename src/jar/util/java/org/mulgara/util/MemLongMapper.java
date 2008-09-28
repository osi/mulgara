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

import java.util.HashMap;

/**
 * A minimal LongMapper implementation for use in tests.
 *
 * @created Sep 26, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class MemLongMapper extends HashMap<Long,Long> implements LongMapper {

  /** Generated UID */
  private static final long serialVersionUID = -2373833159148862815L;

  /**
   * @see org.mulgara.util.LongMapper#delete()
   */
  public void delete() throws Exception {
  }

  /**
   * @see org.mulgara.util.LongMapper#getLong(long)
   */
  public long getLong(long key) {
    return get(key);
  }

  /**
   * @see org.mulgara.util.LongMapper#putLong(long, long)
   */
  public void putLong(long key, long value) {
    put(key,value);
  }

}
