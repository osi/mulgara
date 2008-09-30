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

package org.mulgara.store.stringpool.xa11;

import java.io.IOException;

import org.mulgara.util.IntFile;
import org.mulgara.util.LongMapper;

/**
 * Manages node-to-node mapping, including our blank nodes that need to be mapped to other nodes.
 *
 * @created Sep 26, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class BlankNodeMapper implements LongMapper {

  /** Maps normal node values to other node values. */
  private IntFile nodeMap;

  /** Maps blank node values to other node values. */
  private IntFile blankNodeMap;

  public BlankNodeMapper(String baseName) throws IOException {
    nodeMap = IntFile.newTempIntFile(baseName);
    blankNodeMap = IntFile.newTempIntFile(baseName + "_b");
  }

  /**
   * @see org.mulgara.util.LongMapper#delete()
   */
  public void delete() throws IOException {
    try {
      nodeMap.delete();
    } finally {
      blankNodeMap.delete();
    }
  }

  /**
   * @see org.mulgara.util.LongMapper#getLong(long)
   */
  public long getLong(long key) throws Exception {
    if (BlankNodeAllocator.isBlank(key)) {
      return blankNodeMap.getLong(BlankNodeAllocator.nodeToCounter(key));
    } else {
      return nodeMap.getLong(key);
    }
  }

  /**
   * @see org.mulgara.util.LongMapper#putLong(long, long)
   */
  public void putLong(long key, long value) throws Exception {
    if (BlankNodeAllocator.isBlank(key)) {
      blankNodeMap.putLong(BlankNodeAllocator.nodeToCounter(key), value);
    } else {
      nodeMap.putLong(key, value);
    }
  }

}
