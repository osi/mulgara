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

import org.mulgara.store.xa.AVLComparator;
import org.mulgara.store.xa.AVLNode;

/**
 * Comparator for category information only.
 *
 * @created Aug 14, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class DataCategoryAVLComparator implements AVLComparator {

  private final int typeCategoryId;

  DataCategoryAVLComparator(int typeCategoryId) {
    this.typeCategoryId = typeCategoryId;
  }

  public int compare(long[] key, AVLNode avlNode) {
    // NOTE: ignore key.

    // First, order by type category ID.
    int nodeTypeCategoryId = DataStruct.getTypeCategoryId(avlNode);
    return typeCategoryId <= nodeTypeCategoryId ? -1 : 1;
  }

}
