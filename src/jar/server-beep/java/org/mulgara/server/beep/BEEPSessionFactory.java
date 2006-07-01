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

package org.mulgara.server.beep;

// Java 2 standard packages
import java.net.*;

// Third party packages
import org.apache.log4j.*;        // Log4J
import org.beepcore.beep.core.*;  // BEEP Core
import org.beepcore.beep.transport.tcp.*;

// Locally written packages
import org.mulgara.query.QueryException;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;

/**
 * Proxy for a remote SessionFactory connected via BEEP.
 *
 * @created 2002-01-15
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @author <a href="http://staff.pisoftware.com/david">David Makepeace</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:00 $
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
public class BEEPSessionFactory implements SessionFactory {

  /**
   * Logger.
   */
  Logger logger = Logger.getLogger(BEEPSessionFactory.class.getName());

  /**
   * Description of the Field
   */
  private URI serverURI;

  //
  // Constructor
  //

  /**
   * Generate a proxy for a {@link SessionFactory} which is being served via a
   * {@link BEEPServer}.
   *
   * @param serverURI the URI of the server to proxy
   */
  public BEEPSessionFactory(URI serverURI) {

    // Validate "serverURI" parameter
    if (serverURI == null) {

      throw new IllegalArgumentException("Null \"serverURI\" parameter");
    }

    if (!"beep".equals(serverURI.getScheme())) {

      throw new IllegalArgumentException(serverURI +
          " doesn't use the beep: protocol");
    }

    this.serverURI = serverURI;
  }

  //
  // Methods implementing SessionFactory
  //

  /**
  * Accessor for the factory's security domain. The URI returned should
  * uniquely identify the {@link javax.security.auth.login.Configuration}
  * (usually a JAAS configuration file) used by the factory.
  *
  * @return a unique resource name for the security domain this
  *   {@link SessionFactory} lies within, or <code>null</code> if the factory
  *   is unsecured
  * @throws QueryException if the security domain couldn't be determined
  */
  public URI getSecurityDomain() throws QueryException
  {
    return null;
  }

  /**
  * Factory method.
  *
  * The session generated will be an unauthenticated (<q>guest</q>) session.
  * To authenticate it, the {@link Session#login} method must be used.
  *
  * @return an unauthenticated session
  * @throws QueryException if a session couldn't be generated
  */
  public Session newSession() throws QueryException
  {
    try {
      // If the server URI didn't specify a port, we use BEEP's default (10288)
      int port = serverURI.getPort();
      if (port == -1) {
        port = 10288;
      }

      // Create a Mulgara session by wrapping a BEEP session
      return new BEEPSession(
        TCPSessionCreator.initiate(serverURI.getHost(), port)  // BEEP session
      );
    }
    catch (BEEPException e) {
      throw new QueryException(e.toString());
    }
  }

  public Session newJRDFSession() throws QueryException {
    return null;
  }

  public Session newJenaSession() throws QueryException {
    return null;
  }

  /**
  * METHOD TO DO
  *
  */
  public void close()
  {
    // null implementation
  }

  /**
  * METHOD TO DO
  *
  */
  public void delete()
  {
    // null implementation
  }
}
