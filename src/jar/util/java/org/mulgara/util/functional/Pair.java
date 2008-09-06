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

package org.mulgara.util.functional;

import java.util.Map;

/**
 * A pair of elements.
 *
 * @created Aug 5, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Pair<T1,T2> implements Map.Entry<T1,T2> {

  private T1 first;

  private T2 second;

  public Pair(T1 f, T2 s) { first = f; second = s; }

  public T1 first() { return first; }

  public T2 second() { return second; }

  public T1 getKey() { return first; }

  public T2 getValue() { return second; }

  public T2 setValue(T2 value) { throw new UnsupportedOperationException("Pairs are an immutable type"); }

  public static <C1,C2> Pair<C1,C2> p(C1 c1, C2 c2) { return new Pair<C1,C2>(c1, c2); }

  public Map<T1,T2> add(Map<T1,T2> map) { map.put(first, second); return map; }
}
