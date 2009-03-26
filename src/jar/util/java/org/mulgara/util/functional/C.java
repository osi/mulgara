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

package org.mulgara.util.functional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Generic Collections utility class.
 * This class defines static methods for operating on Collections and the functors found in
 * {@link org.mulgara.util.functional.Fn1} and {@link org.mulgara.util.functional.Fn2}.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class C {

  /**
   * Creates a new list by applying an operator to each element of an initial list.
   * The final result meets the condition:
   * <pre>result.get(i) == op( args.get(i) )   for 0 &lt;= i &lt; args.size()</pre>
   * The operation in op may throw an exception.
   * @param <T1> The type of the elements in the arg list, which is also the
   *             argument type for the operation.
   * @param <T2> The type of the elements in the result list, which is also
   *             the return type of the operation.
   * @param <E> The exception that my be thrown from {@link Fn1E#fn(Object)}.
   * @param args The input list.
   * @param op The operation to apply to the elements of the input list.
   * @return A list whose elements are the result of applying op to each element of args.
   * @throws E An exception that may be thrown from the {@link Fn1E#fn(Object)} method.
   */
  public static final <T1,T2,E extends Exception> List<T2> map(Collection<T1> args, Fn1E<T1,T2,E> op) throws E {
    List<T2> result = new LinkedList<T2>();
    for (T1 a: args) result.add(op.fn(a));
    return result;
  }

  /**
   * The same method as {@link #map(Collection, Fn1E)} for arrays.
   */
  public static final <T1,T2,E extends Exception> List<T2> map(T1[] args, Fn1E<T1,T2,E> op) throws E {
    List<T2> result = new ArrayList<T2>(args.length);
    for (T1 a: args) result.add(op.fn(a));
    return result;
  }

  /**
   * Creates a new list by applying an operator to each element of an initial list.
   * The final result meets the condition:
   * <pre>result.get(i) == op( args.get(i) )   for 0 &lt;= i &lt; args.size()</pre>
   * The operation in op may <em>not</em> throw an exception.
   * @param <T1> The type of the elements in the arg list, which is also the
   *             argument type for the operation.
   * @param <T2> The type of the elements in the result list, which is also
   *             the return type of the operation.
   * @param args The input list.
   * @param op The operation to apply to the elements of the input list.
   * @return A list whose elements are the result of applying op to each element of args.
   */
  public static final <T1,T2> List<T2> map(Collection<T1> args, Fn1<T1,T2> op) {
    return map(args, (Fn1E<T1,T2,RuntimeException>)op);
  }

  /**
   * The same method as {@link #map(Collection, Fn1)} for arrays.
   */
  public static final <T1,T2> List<T2> map(T1[] args, Fn1<T1,T2> op) {
    List<T2> result = new ArrayList<T2>(args.length);
    for (T1 a: args) result.add(op.fn(a));
    return result;
  }

  ///////////////
  // List methods
  ///////////////

  /**
   * Returns the head of a linked list. This is a simple wrapper for {@link LinkedList#getFirst()}
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The first element in the list.
   * @throws NoSuchElementException If the list is empty.
   */
  public static final <T1> T1 head(LinkedList<T1> arg) throws NoSuchElementException {
    return arg.getFirst();
  }

  /**
   * Returns the head of a list.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The first element in the list.
   * @throws NoSuchElementException If the list is empty.
   */
  public static final <T1> T1 head(List<T1> arg) throws NoSuchElementException {
    if (arg instanceof LinkedList) return ((LinkedList<T1>)arg).getFirst();
    if (arg.size() == 0) throw new NoSuchElementException("Empty list");
    return arg.get(0);
  }

  /**
   * Returns the head of a linked list, with a <code>null</code> for an empty list.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The first element in the list, or <code>null</code> if the list is empty.
   */
  public static final <T1> T1 headN(LinkedList<T1> arg) {
    return arg.isEmpty() ? null : arg.getFirst();
  }

  /**
   * Returns the head of a list.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The first element in the list, or <code>null</code> if the list is empty.
   */
  public static final <T1> T1 headN(List<T1> arg) {
    return arg.isEmpty() ? null : (arg instanceof LinkedList) ? ((LinkedList<T1>)arg).getFirst() : arg.get(0);
  }

  /**
   * Returns the tail of a linked list. This is a simple wrapper for {@link LinkedList#getLast()}.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The last element in the list.
   * @throws NoSuchElementException If the list is empty.
   */
  public static final <T1> T1 tail(LinkedList<T1> arg) throws NoSuchElementException {
    return arg.getLast();
  }

  /**
   * Returns the tail of a list.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The last element in the list.
   * @throws IndexOutOfBoundsException If the list is empty.
   */
  public static final <T1> T1 tail(List<T1> arg) throws NoSuchElementException {
    if (arg instanceof LinkedList) return ((LinkedList<T1>)arg).getLast();
    if (arg.size() == 0) throw new NoSuchElementException("Empty list");
    return arg.get(arg.size() - 1);
  }

  /**
   * Returns the tail of a list.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The last element in the list, or <code>null</code> if the list is empty.
   */
  public static final <T1> T1 tailN(LinkedList<T1> arg) {
    return arg.isEmpty() ? null : arg.getLast();
  }

  /**
   * Returns the tail of a list.
   * @param <T1> The list element type.
   * @param arg The list.
   * @return The last element in the list, or <code>null</code> if the list is empty.
   */
  public static final <T1> T1 tailN(List<T1> arg) {
    return arg.isEmpty() ? null : arg.get(arg.size() - 1);
  }

  /**
   * Returns the first item from a Collection. This is a simple wrapper for {@link LinkedList#getFirst()}
   * @param <T1> The element type.
   * @param arg The list.
   * @return The first element in the list.
   * @throws NoSuchElementException If the list is empty.
   */
  public static final <T1> T1 first(LinkedList<T1> arg) throws NoSuchElementException {
    return arg.getFirst();
  }

  /**
   * Returns the first item from an Iterable.
   * @param <T1> The element type.
   * @param arg The iterable collection.
   * @return The first element in the collection.
   * @throws NoSuchElementException If the collection is empty.
   */
  public static final <T1> T1 first(Collection<T1> arg) throws NoSuchElementException {
    if (arg instanceof LinkedList) return ((LinkedList<T1>)arg).getFirst();
    if (arg.isEmpty()) throw new NoSuchElementException("Empty Collection");
    return arg.iterator().next();
  }

  /**
   * Inserts an element into a list in ascending order.
   * @param <T> The type of the element to be inserted. Must be comparable on itself.
   * @param list The list to insert into. This must already be ordered.
   * @param c The element to insert.
   * @return The newly modified list with all elements in ascending.
   */
  public static final <T extends Comparable<T>> List<T> ascendingInsert(List<T> list, T c) {
    return orderedInsert(list, c, true);
  }

  /**
   * Inserts an element into an ordered list in descending order.
   * @param <T> The type of the element to be inserted. Must be comparable on itself.
   * @param list The list to insert into. This must already be ordered.
   * @param c The element to insert.
   * @return The newly modified list, with all elements in descending order.
   */
  public static final <T extends Comparable<T>> List<T> descendingInsert(List<T> list, T c) {
    return orderedInsert(list, c, false);
  }

  /**
   * Inserts an element into an ordered list in a given order.
   * @param <T> The type of the element to be inserted. Must be comparable on itself.
   * @param list The list to insert into. This must already be ordered in the same order as this insert.
   * @param c The element to insert.
   * @return The newly modified list, with all elements in order.
   */
  private static final <T extends Comparable<T>> List<T> orderedInsert(List<T> list, T c, boolean smaller) {
    ListIterator<T> i = list.listIterator();
    while (i.hasNext()) {
      if (orderTest(c.compareTo(i.next()), smaller)) {
        i.previous();
        break;
      }
    }
    i.add(c);
    return list;
  }

  /**
   * Tests if a comparison value indicates that smaller or larger values.
   * @param value The value returned from a comparison.
   * @param smaller When <code>true</code> this returns true for a comparison indicating a
   *                smaller value was first, otherwise it tests if the comparison indicates
   *                a larger value first.
   * @return <code>true</code> when the value is in the same direction as the smaller flag indicates.
   */
  private static final boolean orderTest(int value, boolean smaller) {
    return smaller ? value < 0 : value > 0;
  }

}
