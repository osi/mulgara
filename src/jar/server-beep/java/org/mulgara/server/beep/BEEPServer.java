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
import java.io.*;
import java.net.*;

// Third party packages
import org.apache.log4j.*;

// BEEP Core
import org.beepcore.beep.core.*;
import org.beepcore.beep.transport.tcp.*;

// Locally written packages
import org.mulgara.server.AbstractServer;

/**
* Mulgara server using a BEEP-based ITQL application protocol.
*
* @created 2004-03-21
* @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
* @author <a href="http://staff.pisoftware.com/david">David Makepeace</a>
* @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
* @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
*/
public class BEEPServer extends AbstractServer {
  /** Logger. */
  private static final Logger logger = Logger.getLogger(BEEPServer.class.getName());

  /** BEEP's default port is 10288. */
  static final int DEFAULT_PORT = 10228;

  /** The server port. BEEP's default port is 10288. */
  final int port = DEFAULT_PORT;

  /** Socket accepting session requests from clients. */
  private ServerSocket serverSocket = null;

  /** The collection of supported BEEP profiles. We add only the ITQL profile. */
  private ProfileRegistry profileRegistry = null;

  /**
   * The thread which accepts session requests on the {@link #serverSocket}.
   * This field is <code>null</code> whenever the server isn't {@link #STARTED}.
   */
  private Thread thread;

  /**
   * Create a BEEP server MBean.
   * @throws UnknownHostException if the DNS name of the local host can't be determined
   */
  public BEEPServer() throws UnknownHostException {
    // Generate server URI
    try {
      setURI(new URI("beep",                                    // scheme
                     null,                                      // user info
                     InetAddress.getLocalHost().getHostName(),  // host
                     (port == 10288) ? ( -1) : port,            // port
                     null,                                      // path
                     null,                                      // query
                     null));
    } catch (URISyntaxException e) {
      throw new Error("Bad generated URI", e);
    }
  }

  //
  // Server-specific configuration bean methods
  //

  /**
   * Sets the hostname of the server.
   * @param hostname  the hostname of the server; if <code>null</code>, the
   *   local host name of the machine will be used if it can be found,
   *   otherwise <code>localhost</code> will be used.
   */
  public void setHostname(String hostname) {
    // prevent the hostname from being changed while the server is up
    if (this.getState() == ServerState.STARTED) {
      throw new IllegalStateException("Can't change hostname without first stopping the server");
    }

    // get the hostname
    if (hostname == null) {
      // try to use the local host name
      try {
        hostname = InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
        logger.warn("Problem getting host name! - using localhost");
        hostname = "localhost";
      }
    }

    // set the hostname
    this.hostname = hostname;

    // create a new URI
    try {
      setURI(new URI("beep",                          // scheme
                     null,                            // user info
                     this.getHostname(),              // host
                     (port == 10288) ? ( -1) : port,  // port
                     null,                            // path
                     null,                            // query
                     null));
    } catch (URISyntaxException e) {
      throw new Error("Bad generated URI", e);
    }
  }

  //
  // Methods implementing AbstractServer
  //

  /**
   * Returns the hostname of the server.
   * @return the hostname of the server
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Start the server.
   * @throws Exception EXCEPTION TO DO
   */
  protected void startService() throws Exception {
    if (thread == null) {
      serverSocket = new ServerSocket(port);

      // The profile registry will contain a different profile for each
      // operation we perform on the Session interface
      profileRegistry = new ProfileRegistry();

      // Register the protocol for the Session.query(Query) method
      profileRegistry.addStartChannelListener(
        QueryProfile.URI,                       // URI of the ITQL query profile
        new QueryProfile(getSessionFactory()),  // start channel listener
        null                                    // no session tuning properties
      );

      // Register the protocol for the rest of the Session methods
      profileRegistry.addStartChannelListener(
        CommandProfile.URI,                       // URI of ITQL command profile
        new CommandProfile(getSessionFactory()),  // start channel listener
        null                                      // no session tuning
      );

      // Launch the server thread
      thread = new Thread() {
        public void run() {
          try {
            while (true) {
              Socket socket = serverSocket.accept();
              TCPSession.createListener(socket, profileRegistry);
            }
          } catch (InterruptedIOException ex) {
            // Stop the thread.
          } catch (Exception ex) {
            logger.error("Exception in ServerThread: " + ex);
          } finally {
            if (serverSocket != null) {
              try {
                serverSocket.close();
              } catch (IOException ex) {
                // ignore failures
              }
            }

            thread = null;
          }
        }
      };

      thread.start();
    }
  }

  /**
   * Stop the server.
   * @throws IOException I/O error closing the server socket.
   */
  protected void stopService() throws IOException {
    if (thread != null) {
      thread.interrupt();
      thread          = null;
      profileRegistry = null;
    }

    if (serverSocket != null) {
      try {
        serverSocket.close();
      } finally {
        serverSocket = null;
      }
    }
  }

  /**
   * Informs the base class what the default port for this protocol is.
   * @see org.mulgara.server.AbstractServer#getDefaultPort()
   */
  protected int getDefaultPort() {
    return DEFAULT_PORT;
  }
}
