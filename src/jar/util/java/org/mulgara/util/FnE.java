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
 * Functor template for a function that takes one type and returns another.
 * An exception may be thrown.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface FnE<T1,T2,E extends Exception> {

  /**
   * Declares a function template that takes one argument and returns a value of
   * another type.
   * @param arg The single argument.
   * @return A value based on arg.
   * @throws E Can throw an exception of this type.
   */
  T2 fn(T1 arg) throws E;
}
