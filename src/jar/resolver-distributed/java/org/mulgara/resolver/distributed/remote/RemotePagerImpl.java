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

package org.mulgara.resolver.distributed.remote;

import java.util.*;
import java.rmi.*;
import java.lang.reflect.Array;
import java.io.Serializable;
import java.util.logging.*;

import org.mulgara.util.Rmi;

/**
 * Implements the remote pager by iterating on a list and moving pages of elements over RMI.
 * @param <E> The elements of the paged list.
 *
 * @created 2007-04-23
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class RemotePagerImpl<E extends Serializable> implements RemotePager<E> {

  /** Logger. */
  protected static final Logger logger = Logger.getLogger(RemotePagerImpl.class.getName());

  /** The type of the wrapped class.  Used for creating pages of elements in arrays. */
  private Class<E> type;

  /** Stored size of the wrapped collection. */
  private final int size;
  
  /** The objects to page over RMI. */
  private Collection<E> collection;

  /** Internal iterator for the collection. */
  private Iterator<E> iter;

  /** The latest page of data. */
  private E[] currentPage;

  /** The size of a data page. */
  private final int pageSize = Config.getPageSize();

  /**
   * Creates a new remote paging object.
   * @param type The java.lang.Class of the elements to be paged.
   * @param collection The data to be paged.
   * @throws RemoteException If the data cannot be sent over RMI.
   */
  @SuppressWarnings("unchecked")
  public RemotePagerImpl(Class<E> type, Collection<E> collection) throws RemoteException {
    this.type = type;
    this.collection = collection;
    size = collection.size();
    iter = null;
    currentPage = (E[])Array.newInstance(type, pageSize);
    Rmi.export(this);
  }


  /**
   * Gets the number of items in the underlying data.
   */
  public int size() {
    return size;
  }

  
  /**
   * Gets the first page of data as an array with length equal to the size of the page.
   * @return an array of elements.
   */
  public E[] firstPage() throws RemoteException {
    iter = collection.iterator();
    return fillPage();
  }

  
  /**
   * Gets the next page of data as an array with length equal to the size of the page.
   * @return an array of elements.
   */
  public E[] nextPage() throws RemoteException {
    return fillPage();
  }

  
  /**
   * Populates the current page with elements from the underlying collection.
   * @return The current page.
   */
  private E[] fillPage() {
    logger.finest("Filling page");
    for (int i = 0; i < pageSize; i++) {
      if (!iter.hasNext()) return truncatePage(i);
      currentPage[i] = iter.next();
    }
    return currentPage;
  }

  
  /**
   * Reduces the size of an array if there are fewer valid elements than the length of the array.
   * @param The size of the array.
   * @return A new current page.
   */
  @SuppressWarnings("unchecked")
  private E[] truncatePage(int offset) {
    if (offset == 0) return null;
    logger.finest("Building array of type: " + type +", with length: " + offset);
    E[] result = (E[])Array.newInstance(type, offset);
    System.arraycopy(currentPage, 0, result, 0, offset);
    return result;
  }

}
