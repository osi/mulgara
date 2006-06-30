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
 * The Original Code is the Kowari Metadata Store.
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

package org.kowari.barracuda.gateway;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.apache.log4j.*;

import org.enhydra.barracuda.core.event.*;
import org.enhydra.barracuda.core.helper.servlet.ScriptDetector;
import org.kowari.barracuda.exception.ApplicationExceptionEvent;
import org.kowari.barracuda.exception.ApplicationExceptionHandler;

/**
 * This gateway allows you to register an exception which will be desplayed to
 * the user and logged upon the next HttpRequestEvent by the {@link
 * ApplicationExceptionHandler}. <p/>
 *
 * If you wish to do this from an event handler a better method is {@link
 * ApplicationExceptionEvent#generateEvent(Object, Exception, String)} method.
 * <p/>
 *
 * Usage Example: <p/>
 *
 * <code>
 * <pre>
 * try {
 *
 *   .....
 *
 * } catch (Exception e) {
 *
 *   ExceptionHandlerGateway.getInstance().registerException(e, "Bad widget!");
 * }
 * </pre> <code>
 * <p/>
 *
 * You can also turn off client side script detection by setting the init param
 * DisableScriptDetection to true in the web.xml file.
 *
 * @created 2002-01-16
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:49 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ExceptionHandlerGateway extends ApplicationGateway {

  /**
   * Reference to this gateway
   */
  private static ExceptionHandlerGateway instance;

  /**
   * the category to log to
   */
  private final static Logger log =
    Logger.getLogger(ExceptionHandlerGateway.class.getName());

  /**
   * The exception that was thrown.
   */
  protected Exception exception;

  /**
   * A message for the user.
   */
  protected String message;

  /**
   * Has an exception been registered
   */
  protected boolean exceptionOccurred = false;

  /**
   * Listener for HttpRequestEvent events
   */
  private ListenerFactory httpRequestEventFactory;

  /**
   * Public constructor. This gateway turns off the javascript URL re-writing.
   */
  public ExceptionHandlerGateway() {

    // Don't allow calls to get instance during init..
    synchronized (this.getClass()) {

      httpRequestEventFactory =
        new DefaultListenerFactory() {

            public BaseEventListener getInstance() {

              return new HttpRequestEventHandler();
            }

            public String getListenerID() {

              return getID(HttpRequestEventHandler.class);
            }
          };

      // Register interest in HttpRequestEvents.
      eventGateway.specifyLocalEventInterests(httpRequestEventFactory,
        HttpRequestEvent.class);

      // Register the ApplicationExceptionHandler
      this.specifyEventGateways(new ApplicationExceptionHandler());

      instance = this;
    }
  }

  /**
   * Get a reference to the gateway.
   *
   * @return a reference to this gateway.
   */
  public static synchronized ExceptionHandlerGateway getInstance() {

    if (instance == null) {

      throw new RuntimeException(
        "The ExceptionHandlerGateway is not instanciated.");
    }
    else {

      return instance;
    }
  }

  /**
   * Turns off client side scripting support if configured.
   *
   * @throws ServletException ??
   */
  public void init() throws ServletException {

    // call the super class init()
    super.init();

    // Turn off client-side script detection if configured
    ServletConfig config = getServletConfig();
    String disableScriptDetection =
      config.getInitParameter("DisableScriptDetection");

    if ((disableScriptDetection != null) &&
        disableScriptDetection.equals("true")) {

      ScriptDetector.DETECT_CLIENT_SCRIPTING_ENABLED = false;
      log.info("Client side script detection has been disabled");
    }
    else {

      ScriptDetector.DETECT_CLIENT_SCRIPTING_ENABLED = true;
      log.info("Client side script detection is enabled");
    }
  }

  /**
   * Register an exception for display to the user. While one exception is
   * registered all others will be ignored until it is displayed.
   *
   * @param ex The exception.
   * @param mess The message.
   */
  public synchronized void registerException(Exception ex, String mess) {

    if (!exceptionOccurred) {

      exception = ex;
      message = mess;
      exceptionOccurred = true;
    }
  }

  /**
   * Handles HttpRequestEvent events.
   */
  class HttpRequestEventHandler extends DefaultBaseEventListener {

    /**
     * Does the work of handling HttpRequestEvent events.
     *
     * @param context The event context.
     * @throws EventException EXCEPTION TO DO
     * @throws ServletException EXCEPTION TO DO
     */
    public void handleControlEvent(ControlEventContext context)
      throws EventException, ServletException {

      BaseEvent event = context.getEvent();

      // Don't allow new exceptions to be registered while in progress.
      synchronized (ExceptionHandlerGateway.this) {

        if (exceptionOccurred) {

          try {

            exceptionOccurred = false;

            // Generate the ApplicationExceptionEvent
            ApplicationExceptionEvent.generateEvent(this, exception, message);
          }
           finally {

            event.setHandled(true);
          }
        }
        else {

          event.setHandled(false);
        }
      }
    }
  }
}
