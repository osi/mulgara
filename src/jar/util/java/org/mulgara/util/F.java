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
 * 
 *
 * @created Aug 16, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class F {

  static final <T1,T2> Fn<T2> curry(final Fn1<T1,T2> fna, final T1 arg) {
    return new Fn<T2>() { public T2 fn() { return fna.fn(arg); } };
  }

  static final <T1,T2,R> Fn1<T2,R> curry(final Fn2<T1,T2,R> fna, final T1 arg) {
    return new Fn1<T2,R>() { public R fn(T2 a) { return fna.fn(arg, a); } };
  }

}
