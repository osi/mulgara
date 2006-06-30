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
 * The Original Code is the Mulgara Metadata Store.
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

package org.mulgara.barracuda.exception;

import org.enhydra.barracuda.core.event.HttpResponseEvent;
import org.enhydra.barracuda.core.event.InterruptDispatchException;

/**
 * This event should be generated when an exception occurs in an event handler.
 * <p>
 * It will be caught by the <code>ExceptionHandlerServlet</code>, reported to
 * the user and logged. For methods that do not usually generate events (e.g.
 * constructors) use the
 * {@link org.mulgara.barracuda.gateway.ExceptionHandlerGateway} and register
 * the exception with it using the method {@link org.mulgara.barracuda.gateway.ExceptionHandlerGateway#registerException(Exception, String)}.
 * <p/>
 * <p>
 * Usage Example:
 * </p>
 * <code><pre>
 * try {
 *
 *   .....
 *
 * } catch (Exception e) {
 *
 *   ApplicationExceptionEvent.generateEvent(this, e, "Bad widget!");
 * }
 * </pre></code>
 *
 * @created 2002-01-16
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:48 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ApplicationExceptionEvent extends org.enhydra.barracuda.core.event.
    HttpResponseEvent {

  /**
   * The exception that was thrown.
   *
   */
  protected Exception exception;

  /**
   * A message for the user.
   *
   */
  protected String message;

  /**
   * Public constructor. If you use this method you should manually set the
   * source before dispatching the event.
   *
   * @param ex The exception that occurred.
   * @param mess A message for the user.
   */
  public ApplicationExceptionEvent(Exception ex, String mess) {
    super();
    exception = ex;
    message = mess;
  }

  /**
   * Public constructor. Automatically sets the source parameter. If you do not
   * use this method you should manually set the source before dispatching the
   * event.
   *
   * @param source The source of the event.
   * @param ex The exception that occurred.
   * @param mess A message for the user.
   */
  public ApplicationExceptionEvent(Object source, Exception ex, String mess) {
    super(source);
    exception = ex;
    message = mess;
  }

  /**
   * Generates an ApplicationExceptionEvent
   *
   * @param source The source of the event.
   * @param ex The exception that occurred.
   * @param mess A message for the user.
   * @throws InterruptDispatchException to generate an
   *      ApplicationExceptionEvent.
   */
  public static void generateEvent(Object source, Exception ex,
      String mess) throws InterruptDispatchException {

    throw new InterruptDispatchException(new ApplicationExceptionEvent(source,
        ex, mess));
  }

  /**
   * Get the exception that was thrown.
   *
   * @return The exception.
   */
  public Exception getException() {

    return exception;
  }

  /**
   * Get the user message.
   *
   * @return The message.
   */
  public String getMessage() {

    return message;
  }
}
