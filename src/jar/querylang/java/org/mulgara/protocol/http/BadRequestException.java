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

package org.mulgara.protocol.http;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;

/**
 * Encodes the condition of a bad HTTP servlet request.
 *
 * @created Sep 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class BadRequestException extends ServletException {

  /** The serialization ID */
  private static final long serialVersionUID = 6283252576312243830L;

  /** An default constructor to indicate a problem. */
  public BadRequestException() {
    super(SC_BAD_REQUEST);
  }

  /**
   * @param message The message to send with a bad request code.
   */
  public BadRequestException(String message) {
    super(SC_BAD_REQUEST, message);
  }

}
