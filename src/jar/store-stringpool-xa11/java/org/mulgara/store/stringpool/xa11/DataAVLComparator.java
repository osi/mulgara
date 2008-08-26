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
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.mulgara.store.stringpool.SPComparator;
import org.mulgara.store.stringpool.SPObject;
import org.mulgara.store.xa.AVLComparator;
import org.mulgara.store.xa.AVLNode;

/**
 * Comparator for objects in the data pool.
 *
 * @created Aug 12, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class DataAVLComparator implements AVLComparator {

  private final SPComparator spComparator;
  private final int typeCategoryId;
  private final int typeId;
  private final int subtypeId;
  private final ByteBuffer data;
  private final RandomAccessFile readOnlyFlatFile;

  DataAVLComparator(SPComparator spComparator, SPObject.TypeCategory typeCategory, int typeId, int subtypeId, ByteBuffer data, RandomAccessFile flatFile) {
    this.spComparator = spComparator;
    this.typeCategoryId = typeCategory.ID;
    this.typeId = typeId;
    this.subtypeId = subtypeId;
    this.data = data;
    this.readOnlyFlatFile = flatFile;
  }

  DataAVLComparator(SPComparator spComparator, DataStruct dataStruct, RandomAccessFile readOnlyFlatFile) {
    this.spComparator = spComparator;
    this.typeCategoryId = dataStruct.getTypeCategoryId();
    this.typeId = dataStruct.getTypeId();
    this.subtypeId = dataStruct.getSubtypeId();
    this.data = dataStruct.getData();
    this.readOnlyFlatFile = readOnlyFlatFile;
  }

  /**
   * @see org.mulgara.store.xa.AVLComparator#compare(long[], org.mulgara.store.xa.AVLNode)
   */
  public int compare(long[] key, AVLNode avlNode) {
    // NOTE: ignore key.

    // First, order by type category ID.
    int nodeTypeCategoryId = DataStruct.getTypeCategoryId(avlNode);
    int c = typeCategoryId - nodeTypeCategoryId;
    if (c != 0) return c;

    // Second, order by type node.
    int nodeTypeId = DataStruct.getTypeId(avlNode);
    if (typeId != nodeTypeId) return typeId < nodeTypeId ? -1 : 1;

    int nodeSubtypeId = DataStruct.getSubtypeId(avlNode);

    // Finally, defer to the SPComparator.
    int dataSize = DataStruct.getDataSize(avlNode);

    // Retrieve the binary representation as a ByteBuffer.
    ByteBuffer nodeData = DataStruct.getDataPrefix(avlNode, dataSize);

    if (dataSize > DataStruct.MAX_DATA_SIZE) {
      // Save the limit of data so it can be restored later in case it is
      // made smaller by the comparePrefix method of the spComparator.
      int savedDataLimit = data.limit();

      data.rewind();
      nodeData.rewind();
      c = spComparator.comparePrefix(data, nodeData, dataSize);
      if (c != 0) return c;

      data.limit(savedDataLimit);

      try {
        // Retrieve the remaining bytes if any.
        // Set the limit before the position in case the limit was made
        // smaller by the comparePrefix method.
        nodeData.limit(dataSize);
        nodeData.position(DataStruct.MAX_DATA_SIZE);
        DataStruct.getRemainingBytes(nodeData, readOnlyFlatFile, DataStruct.getGNode(avlNode));
      } catch (IOException ex) {
        throw new Error("I/O Error while retrieving SPObject data", ex);
      }
    }

    data.rewind();
    nodeData.rewind();
    return spComparator.compare(data, subtypeId, nodeData, nodeSubtypeId);
  }

}
