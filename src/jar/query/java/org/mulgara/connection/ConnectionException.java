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
package org.mulgara.connection;

/**
 * An exception indicating a connection problem.
 *
 * @created 2007-08-22
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ConnectionException extends Exception {

  /** Regenerate this ID if non-private methods are added or removed. */
  private static final long serialVersionUID = 3768510944925963668L;


  /**
   * Create an exception with a message.
   * @param message The message to use.
   */
  public ConnectionException(String message) {
    super(message);
  }


  /**
   * Create an exception caused by another exception, and with a message.
   * @param message The message to use.
   * @param cause The original throwable causing this exception.
   */
  public ConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
  
}
