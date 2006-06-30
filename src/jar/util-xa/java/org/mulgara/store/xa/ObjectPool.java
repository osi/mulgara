/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the Kowari Metadata Store.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.store.xa;

// Java 2 standard packages
import java.util.*; // Third party packages

// Third party packages
import org.apache.log4j.Category;

/**
 * A pool of objects to avoid calling constructors unnecessarily.
 * Pools are linked to a root pool, which handles overflows for
 * local pools.
 *
 * @created 2002-09-09
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.10 $
 *
 * @modified $Date: 2005/07/05 04:23:54 $
 *
 * @maintenanceAuthor: $Author: pgearon $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2002 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public final class ObjectPool {

  /** A globally shared pool.  This is an instance of the current class. */
  public final static ObjectPool SHARED_POOL = new ObjectPool(null);

  /** Size of the object stack */
  public final static int OBJECT_STACK_SIZE_LIMIT = 512;

  /** Description of the Field */
  public final static int TYPE_OBJECTPOOL = 0;

  /** ID for keys in the pool */
  public final static int TYPE_KEY = 1;

  /** ID for object stack in the pool */
  public final static int TYPE_OBJECTSTACK = 2;

  /** ID of AVL Nodes in the pool */
  public final static int TYPE_AVLNODE = 3;

  /** ID of Array of AVLNode of length 1.  */
  public final static int TYPE_AVLNODE_1 = 4;

  /** ID of Array of AVLNode of length 2.  */
  public final static int TYPE_AVLNODE_2 = 5;

  /** ID of Block */
  public final static int TYPE_BLOCK = 6;

  /** ID of 8KB object */
  public final static int TYPE_BLOCK_8192 = 7;

  /** The number of different object types (all the TYPE_ fields) */
  public final static int NR_OBJECT_TYPES = 8;

  /** ID of a sized block */
  public final static int TYPE_S_BLOCK = 0;

  /** Logger */
  private final static Category logger =
      Category.getInstance(ObjectPool.class.getName());

  /** The next cascaded object pool (the root pool) */
  private ObjectPool nextPool;

  /** An array of object arrays.  One array per type.  */
  private Object[][] objectStacks =
      new Object[NR_OBJECT_TYPES][OBJECT_STACK_SIZE_LIMIT];

  /** The sizes of each object stack.  */
  private int[] objectStackCounts = new int[NR_OBJECT_TYPES];

  /** Mapping of sizes to object stacks containing that size */
  private Map sizedObjectStacks = new HashMap();

  /** Number of references to this pool by other pools (applies to the root pool). */
  private int refCount;

  /**
   * Constructs an object pool.
   *
   * @param nextPool The next pool in the chain.  Normally the shared pool.
   */
  private ObjectPool(ObjectPool nextPool) {

    init(nextPool);
  }

  /**
   * Factory method to build a new root object pool.
   *
   * @return The new object pool.
   */
  public static ObjectPool newInstance() {

    return newInstance(SHARED_POOL);
  }

  /**
   * Factory method to build a new local object pool.
   *
   * @param nextPool The next pool in the chain.  Normally the shared pool
   * @return The new object pool.
   */
  public static ObjectPool newInstance(ObjectPool nextPool) {

    ObjectPool op;

    if (nextPool != null) {

      synchronized (nextPool) {

        op = (ObjectPool) nextPool.get(TYPE_OBJECTPOOL);
      }
    }
    else {

      op = null;
    }

    if (op != null) {

      op.init(nextPool);
    }
    else {

      op = new ObjectPool(nextPool);
    }

    return op;
  }

  /**
   * Gets a pooled object of a given type from this pool.
   *
   * @param type The ID of the type of object to find.
   * @return A pooled object, or <code>null</code> if no objects are available.
   */
  public Object get(int type) {

    assert refCount > 0;

    int count = objectStackCounts[type];

    if (count > 0) {

      Object[] os = objectStacks[type];
      Object o = os[--count];
      os[count] = null;
      objectStackCounts[type] = count;

      return o;
    }

    if (nextPool != null) {

      synchronized (nextPool) {

        return nextPool.get(type);
      }
    }

    return null;
  }

  /**
   * Gets a pooled object of a given type and size from this pool.
   *
   * @param type The ID of the type of object to find.
   * @param key The size of the object to find.
   * @return A pooled object, or <code>null</code> if no objects are available.
   */
  public Object get(int type, int key) {

    assert refCount > 0;

    if ( (type == TYPE_S_BLOCK) && (key == 8192)) {

      return get(TYPE_BLOCK_8192);
    }

    Key k = newKeyInstance(type, key);
    ObjectStack os = (ObjectStack) sizedObjectStacks.get(k);
    put(TYPE_KEY, k);

    if ( (os != null) && !os.isEmpty()) {

      return os.pop();
    }

    if (nextPool != null) {

      synchronized (nextPool) {

        return nextPool.get(type, key);
      }
    }

    return null;
  }

  /**
   * Increments the number of times this pool is referred to.
   */
  public void incRefCount() {

    assert refCount > 0;

    ++refCount;
  }

  /**
   * Adds an object into the pool.
   *
   * @param type The type of object being added.
   * @param o The object to add.
   */
  public void put(int type, Object o) {

    assert refCount > 0;

    int count = objectStackCounts[type];

    if (count < OBJECT_STACK_SIZE_LIMIT) {

      objectStacks[type][count++] = o;
      objectStackCounts[type] = count;
    }
  }

  /**
   * METHOD TO DO
   *
   * @param type PARAMETER TO DO
   * @param key PARAMETER TO DO
   * @param o PARAMETER TO DO
   */
  public void put(int type, int key, Object o) {

    assert refCount > 0;

    if ( (type == TYPE_S_BLOCK) && (key == 8192)) {

      put(TYPE_BLOCK_8192, o);

      return;
    }

    Key k = newKeyInstance(type, key);
    ObjectStack os = (ObjectStack) sizedObjectStacks.get(k);

    if (os == null) {

      os = newObjectStackInstance();
      sizedObjectStacks.put(k, os);
    }
    else {

      put(TYPE_KEY, k);
    }

    os.push(o);
  }

  /**
   * Removes a reference to this object pool.
   * Clears the pool if it is no longer in use.
   */
  public void release() {

    assert refCount > 0;

    if (--refCount == 0) {

      flush(true);
    }
  }

  /**
   * Push all objects out of this pool and into the next (root) pool.
   */
  public void flush() {

    flush(false);
  }

  /**
   * Initialize the references for this pool.
   *
   * @param nextPool The next pool in the chain (either the root, or null).
   */
  private void init(ObjectPool nextPool) {

    this.nextPool = nextPool;
    refCount = 1;
  }

  /**
   * Push all objects into the next pool, clearing this pool.
   *
   * @param dispose Get rid of this pool once it is flushed.
   */
  private void flush(boolean dispose) {

    if (nextPool != null) {

      synchronized (nextPool) {

        if (!sizedObjectStacks.isEmpty()) {

          if (nextPool.sizedObjectStacks.isEmpty()) {

            // Swap the HashMaps.
            Map tMap = nextPool.sizedObjectStacks;
            nextPool.sizedObjectStacks = sizedObjectStacks;
            sizedObjectStacks = tMap;
          }
          else {

            for (Iterator it = sizedObjectStacks.entrySet().iterator();
                it.hasNext(); ) {

              Map.Entry entry = (Map.Entry) it.next();
              Key k = (Key) entry.getKey();
              ObjectStack os = (ObjectStack) entry.getValue();
              ObjectStack npOS =
                  (ObjectStack) nextPool.sizedObjectStacks.get(k);

              if (npOS == null) {

                nextPool.sizedObjectStacks.put(k, os);
              }
              else {

                nextPool.put(TYPE_KEY, k);
                npOS.copy(os);
                nextPool.put(TYPE_OBJECTSTACK, os);
              }
            }

            sizedObjectStacks.clear();
          }
        }

        for (int i = 0; i < NR_OBJECT_TYPES; ++i) {

          int count = objectStackCounts[i];

          if (count > 0) {

            Object[] os = objectStacks[i];
            int npCount = nextPool.objectStackCounts[i];
            Object[] npOS = nextPool.objectStacks[i];

            if (npCount == 0) {

              nextPool.objectStacks[i] = os;
              nextPool.objectStackCounts[i] = count;
              objectStacks[i] = npOS;
              objectStackCounts[i] = 0;
            }
            else {

              int len = OBJECT_STACK_SIZE_LIMIT - npCount;

              if (len > count) {

                len = count;
              }

              if (len > 0) {

                System.arraycopy(os, count - len, npOS, npCount, len);
                npCount += len;
              }

              while (count > 0) {

                os[--count] = null;
              }

              nextPool.objectStackCounts[i] = npCount;
              objectStackCounts[i] = 0;
            }
          }
        }

        if (dispose) {

          nextPool.put(TYPE_OBJECTPOOL, this);
          nextPool = null;
        }
      }
    }
    else if (dispose) {

      sizedObjectStacks = null;
      objectStacks = null;
      objectStackCounts = null;
    }
  }

  /**
   * Create a new key, based on a given type and size.
   *
   * @param type The ID of the type of object to store.
   * @param key The key for this object type.  This is the size of the data it holds.
   * @return A new {@link #Key} instance.
   */
  private Key newKeyInstance(int type, int key) {

    Key k = (Key) get(TYPE_KEY);

    if (k != null) {

      k.init(type, key);
    }
    else {

      k = new Key(type, key);
    }

    return k;
  }

  /**
   * Creates a new ObjectStack.
   *
   * @return A fresh {@link ObjectStack}.
   */
  private ObjectStack newObjectStackInstance() {

    ObjectStack os = (ObjectStack) get(TYPE_OBJECTSTACK);

    if (os == null) {

      os = new ObjectStack(OBJECT_STACK_SIZE_LIMIT);
    }

    return os;
  }

  //  protected void finalize() {
  //    if (refCount > 0) logger.warn("Unpooled ObjectPool.");
  //  }

  /**
   * This class is used as a key for object types, so that all objects
   * of the same type can be found using the same key.
   */
  private final static class Key {

    /** The ID of the object type */
    int type;

    /** The size of the data in the object */
    int key;

    /**
     * Constructs a new object type Key.
     *
     * @param type The ID of the object type.
     * @param key The key for the object type, from the size.
     */
    Key(int type, int key) {

      init(type, key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {

      Key k = (Key) o;

      return (k.type == type) && (k.key == key);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {

      return type + (key * 17);
    }

    /**
     * Stores the parameters for this Key.
     *
     * @param type The ID of the object type.
     * @param key The key for the object type, from the size.
     */
    void init(int type, int key) {

      this.type = type;
      this.key = key;
    }
  }

  /**
   * A stack of objects, all of the same type.
   */
  private final static class ObjectStack {

    /** The stack containing the objects. */
    private Object[] stack;

    /** The number of objects in the stack. */
    private int count = 0;

    /**
     * Constructor for the stack.
     *
     * @param maxSize The maximum number of objects in the stack.
     */
    ObjectStack(int maxSize) {

      stack = new Object[maxSize];
    }

    /**
     * Checks if the stack contains any objects.
     *
     * @return <code>true</code> when the stack is empty.
     */
    boolean isEmpty() {

      return count == 0;
    }

    /**
     * Removes every object from the stack.
     */
    void clear() {

      while (count > 0) {

        stack[--count] = null;
      }
    }

    /**
     * Moves the contents of another stack into this one,
     * but only up to the limit of this stack.  If the source stack
     * contains more objects than free positions in this stack, then objects
     * will be lost.
     *
     * @param os The object stack to copy from.  This stack will be cleared.
     */
    void copy(ObjectStack os) {

      int len = stack.length - count;

      if (len > os.count) {

        len = os.count;
      }

      if (len > 0) {

        System.arraycopy(os.stack, os.count - len, stack, count, len);
        count += len;
      }

      os.clear();
    }

    /**
     * Adds a new object to the stack.
     * Currently drops the object if the stack is full.
     *
     * @param o The object to add.
     */
    void push(Object o) {

      if (count < stack.length) {

        stack[count++] = o;
      }

      // TODO discard oldest entries when stack is full.
    }

    /**
     * Removes the last object from the top of the stack and returns it.
     *
     * @return The top object from the stack, or <code>null</code> if the stack is empty.
     */
    Object pop() {

      if (count > 0) {

        Object o = stack[--count];
        stack[count] = null;

        return o;
      }
      else {

        return null;
      }
    }
  }
}
