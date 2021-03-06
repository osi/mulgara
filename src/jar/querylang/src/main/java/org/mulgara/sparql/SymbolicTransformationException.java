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

package org.mulgara.sparql;

/**
 * Indicates an error performing a symbolic transformation.
 *
 * @created Jul 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
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
