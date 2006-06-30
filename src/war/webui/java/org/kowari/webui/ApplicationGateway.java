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

package org.kowari.webui;

// java 2 standard packages
import javax.servlet.*;

import org.apache.log4j.*;

// barracuda classes
import org.enhydra.barracuda.core.event.*;
import org.kowari.barracuda.gateway.ExceptionHandlerGateway;
import org.kowari.webui.viewer.ViewerScreen;

/**
 * The Kowari Viewer application gateway. <p>
 *
 * This class defines the event gateways that handle events presented to the
 * application. </p>
 *
 * @created 2002-01-22
 *
 * @author Tom Adams
 * @author Ben Warren
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2004/12/22 05:04:49 $ by $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2002 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ApplicationGateway extends ExceptionHandlerGateway {

  /**
   * the category to log to
   */
  private final static Logger log =
    Logger.getLogger(ApplicationGateway.class.getName());

  /**
   * Initialise the application event gateways.
   */
  public void initializeLocal() {

    // log that we're specifying the event gateways
    if (log.isInfoEnabled()) {

      log.info("Specifying event gateways");
    }

    this.specifyEventGateways(new ViewerScreen());
  }

  // initializeLocal()
}


// ApplicationGateway
