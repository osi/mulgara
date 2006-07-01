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

package org.mulgara.barracuda.exception;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.enhydra.barracuda.core.comp.*;
import org.enhydra.barracuda.core.event.*;
import org.enhydra.barracuda.core.event.helper.*;
import org.enhydra.barracuda.core.forms.*;
import org.enhydra.barracuda.core.forms.validators.*;
import org.enhydra.barracuda.core.util.dom.*;
import org.enhydra.barracuda.core.util.http.*;
import org.enhydra.barracuda.plankton.data.MapStateMap;

import org.apache.log4j.*;

import org.mulgara.webui.viewer.events.*;

import org.w3c.dom.*;
import org.w3c.dom.html.*;

/**
 * Handles {@link ApplicationExceptionEvent} events. The mesaage and exception
 * will displayed on a page for the user and also logged as an error.
 *
 * @created 2002-02-16
 *
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:48 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ApplicationExceptionHandler extends DefaultEventGateway {

  /**
   * The logger for this class
   */
  protected static Logger log =
    Logger.getLogger(ApplicationExceptionHandler.class.getName());

  /**
   * The directives
   */
  private static Properties properties;

  // Event handlers

  /**
   * Listener for GetViewerScreen events
   */
  private ListenerFactory applicationExceptionEventFactory;

  /**
   * Public constructor
   */
  public ApplicationExceptionHandler() {

    // Load directives.
    if (properties == null) {

      try {

        properties = new Properties();
        properties.load(this.getClass().getResourceAsStream("ApplicationExceptionHandler.directives"));
      }
       catch (IOException e) {

        log.fatal("Fatal error loading directives file:", e);
        throw new RuntimeException("Fatal error loading directives file:", e);
      }
    }

    // Create the event listeners and register handlers for possible events.
    // Render the query screen event
    applicationExceptionEventFactory =
      new DefaultListenerFactory() {

          public BaseEventListener getInstance() {

            return new ApplicationExceptionEventHandler();
          }

          public String getListenerID() {

            return getID(ApplicationExceptionEventHandler.class);
          }
        };
    specifyLocalEventInterests(applicationExceptionEventFactory,
      ApplicationExceptionEvent.class);
  }

  //------------------------------------------------------------
  //                View Event Handlers
  //------------------------------------------------------------

  /**
   * This is where we handle any ApplicationExceptionEvent events and actually
   * generate the view.
   */
  class ApplicationExceptionEventHandler extends DefaultViewHandler {

    /**
     * Handle the view event.
     *
     * @param root The root component which will get rendered as a result of
     *      this request.
     * @return The document to be rendered.
     */
    public Document handleViewEvent(BComponent root) {

      return handleViewEvent(root,
          (ViewEventContext) getViewContext().getEventContext());
    }

    /**
     * Handle the view event.
     *
     * @param root The root component which will get rendered as a result of
     *      this request.
     * @param vc The ViewEventContext object describes what features the client
     *      view is capable of supporting.
     * @return The document to be rendered.
     */
    public Document handleViewEvent(BComponent root, ViewEventContext vc) {

      Document dom = null;
      ApplicationExceptionEvent event =
        (ApplicationExceptionEvent) vc.getEvent();

      try {

        dom =
          DefaultDOMLoader.getGlobalInstance().getDOM(ApplicationExceptionHandlerHTML.class);
      }
       catch (IOException e) {

        log.fatal("Fatal Error loading DOM template:", e);
      }

      BTemplate template = new BTemplate();
      root.addChild(template);

      Node node = dom.getElementById("ExceptionHandler");
      TemplateView view =
        new DefaultTemplateView(node, "id", new MapStateMap(properties));
      template.addView(view);

      // Build the page.
      template.addModel(new ExceptionModel(event));

      // Log the exception
      log.error("Caught an ApplicationExceptionEvent:\n\n" + "Message: " +
        event.getMessage() + "\n" + "Exception: " + event.getException());

      return dom;
    }
  }

  //------------------------------------------------------------
  //                Components - TemplateModel
  //------------------------------------------------------------

  /**
   * ExceptionModel fills the page.
   */
  class ExceptionModel extends AbstractTemplateModel {

    /**
     * The exception that occurred
     */
    private ApplicationExceptionEvent event;

    /**
     * The exception that the event wraps
     */
    private Exception exception;

    /**
     * Public constructor that tales the exception that occurred.
     */
    public ExceptionModel(ApplicationExceptionEvent ev) {

      event = ev;
      exception = ev.getException();
    }

    /**
     * Registers the model by name.
     */
    public String getName() {

      return "Exception";
    }

    /**
     * Provides items by key.
     *
     * @param key The name of the item to get.
     * @return A value for the item. Can be a BComponent, String, DOM node or
     *      event.
     */
    public Object getItem(String key) {

      ViewContext vc = getViewContext();
      HttpSession session = SessionServices.getSession(vc, true);

      if (key.equals("Message")) {

        String message = event.getMessage();

        if (message == null) {

          message = "No message supplied";
        }

        return new BText(message);
      }

      if (key.equals("ExceptionType")) {

        String className;

        if (exception == null) {

          className = "No exception supplied";
        }
        else {

          className = exception.getClass().getName();
        }

        return new BText(className);
      }

      // Don't show the trace header if no exception.
      else if (key.equals("StackTraceHeader")) {

        BComponent component = new BComponent();

        if (exception == null) {

          component.setVisible(false, false);
        }

        return component;
      }

      // Don't show the trace body if no exception.
      else if (key.equals("StackTraceBody")) {

        BComponent component = new BComponent();

        if (exception == null) {

          component.setVisible(false, false);
        }

        return component;
      }

      else if (key.equals("StackTrace")) {

        if (exception != null) {

          StringWriter stringWriter = new StringWriter();
          PrintWriter printWriter = new PrintWriter(stringWriter);
          exception.printStackTrace(printWriter);
          printWriter.flush();
          stringWriter.flush();

          return new BText(stringWriter.getBuffer().toString());
        }
        else {

          return new BText();
        }
      }
      else {

        return super.getItem(key);
      }
    }
  }
}
