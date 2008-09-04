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

package org.mulgara.sparql;

/**
 * Indicates an error performing a symbolic transformation.
 *
 * @created Jul 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class SymbolicTransformationException extends Exception {

  /** Serialization ID */
  private static final long serialVersionUID = -1791046260155844890L;

  /**
   * @param message The exception message.
   */
  public SymbolicTransformationException(String message) {
    super(message);
  }

  /**
   * @param message The exception message.
   * @param cause The exception that caused the problem.
   */
  public SymbolicTransformationException(String message, Throwable cause) {
    super(message, cause);
  }

}