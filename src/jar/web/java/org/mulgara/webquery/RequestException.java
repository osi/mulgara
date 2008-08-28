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

package org.mulgara.webquery;

/**
 * An internal exception for communicating issues that need to be sent out as HTTP responses.
 *
 * @created Aug 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class RequestException extends Exception {

  /** The generated serialization ID. */
  private static final long serialVersionUID = 1578918131388079524L;

  /**
   * @param message The description of the problem.
   */
  public RequestException(String message) {
    super(message);
  }

  /**
   * @param message The description of the problem.
   * @param cause A throwable that caused the problem.
   */
  public RequestException(String message, Throwable cause) {
    super(message, cause);
  }

}
