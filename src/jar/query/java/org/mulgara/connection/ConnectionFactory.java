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

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.mulgara.query.QueryException;
import org.mulgara.server.Session;


/**
 * Creates new connections or reloads from a cache when possible connections.
 * This must NOT be shared between users, as it is designed to cache security credentials!
 *
 * @created 2007-08-21
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ConnectionFactory {

  /** The logger. */
  private final static Logger logger = Logger.getLogger(ConnectionFactory.class.getName());

  /** String constant for localhost */
  private final static String LOCALHOST_NAME = "localhost";
  
  /** IP constant for localhost, saved as a string */
  private final static String LOCALHOST_IP = "127.0.0.1";
  
  /** The scheme name for the local protocol */
  private final static String LOCAL_PROTOCOL = "local";
  
  /** The list of known protocols. */
  private final static String[] PROTOCOLS = { "rmi", "beep", LOCAL_PROTOCOL };
  
  /** The list of known local host aliases. */
  private final static List<String> LOCALHOSTS = new LinkedList<String>();
  
  /** Initialize the list of local host aliases. */
  static {
    LOCALHOSTS.add(LOCALHOST_NAME);
    LOCALHOSTS.add(LOCALHOST_IP);
    try {
      LOCALHOSTS.add(InetAddress.getLocalHost().getHostAddress());
      LOCALHOSTS.add(InetAddress.getLocalHost().getHostName());
    } catch (UnknownHostException e) {
      logger.error("Unable to get local host address", e);
    }
  }

  /** Cache of Connections, based on their server URI. */
  private Map<URI,SessionConnection> cacheOnUri;
  /** Cache of Connections, based on their session data. */
  private Map<Session,SessionConnection> cacheOnSession;
  
  /** A local connection.  This is only used if a local session is provided. */
  private SessionConnection localConnection = null;

  /**
   * Default constructor.
   */
  public ConnectionFactory() {
    cacheOnUri = new HashMap<URI,SessionConnection>();
    cacheOnSession = new HashMap<Session,SessionConnection>();
  }

  /**
   * Retrieve a connection based on a server URI.
   * @param serverUri The URI to get the connection to.
   * @return The new Connection.
   * @throws ConnectionException There was an error getting a connection.
   */
  public Connection newConnection(URI serverUri) throws ConnectionException {
    SessionConnection c = cacheOnUri.get(serverUri);
    if (c == null) {
      if (isLocalServer(serverUri)) {
        c = (localConnection != null) ? localConnection : new SessionConnection(serverUri, false);
        addLocalConnection(serverUri, c);
      } else {
        c = new SessionConnection(serverUri);
      }
      cacheOnUri.put(serverUri, c);
      cacheOnSession.put(c.getSession(), c);
    }
    return c;
  }


  /**
   * Retrieve a connection for a given session.
   * @param session The Session the Connection will use..
   * @return The new Connection.
   * @throws ConnectionException There was an error getting a connection.
   */
  public Connection newConnection(Session session) throws ConnectionException {
    SessionConnection c = cacheOnSession.get(session);
    if (c == null) {
      c = new SessionConnection(session);
      cacheOnSession.put(session, c);
      URI serverURI = c.getServerUri();
      if (serverURI != null) {
        cacheOnUri.put(serverURI, c);
        if (session.isLocal()) addLocalConnection(serverURI, c);
      }
      if (session.isLocal()) localConnection = c;
    }
    return c;
  }


  /**
   * Close all connections served by this factory. Exceptions are logged, but not acted on.
   */
  public void closeAll() {
    Set<SessionConnection> connectionsToClose = new HashSet<SessionConnection>(cacheOnSession.values());
    connectionsToClose.addAll(cacheOnUri.values());
    safeCloseAll(connectionsToClose);
  }


  /**
   * Closes all connections in a collection. Exceptions are logged, but not acted on.
   * @param connections The connections to close.
   */
  private void safeCloseAll(Iterable<SessionConnection> connections) {
    for (Connection c: connections) {
      try {
        c.close();
      } catch (QueryException qe) {
        logger.warn("Unable to close connection", qe);
      }
    }
  }
  

  /**
   * Test if a given URI is a local URI.
   * @param serverUri The URI to test.
   * @return <code>true</code> if the URI is local.
   */
  static boolean isLocalServer(URI serverUri) {
    if (serverUri == null) return false;

    String scheme = serverUri.getScheme();
    if (LOCAL_PROTOCOL.equals(scheme)) return true;
    
    // check for known protocols
    boolean found = false;
    for (String protocol: PROTOCOLS) {
      if (protocol.equals(serverUri.getScheme())) {
        found = true;
        break;
      }
    }
    if (found == false) return false;

    // protocol found.  Now test if the host appears in the localhost list
    String host = serverUri.getHost();
    for (String h: LOCALHOSTS) if (h.equalsIgnoreCase(host)) return true;

    // no matching hostnames
    return false;
  }


  /**
   * Maps all the possible localhost aliases onto the requested connection.
   * @param serverUri The basic form of the localhost URI.
   * @param connection The connection to associate with the local host.
   */
  private void addLocalConnection(URI serverUri, SessionConnection connection) {
    String path = serverUri.getRawPath();
    for (String protocol: PROTOCOLS) {
      for (String alias: LOCALHOSTS) {
        try {
          URI uri = new URI(protocol, alias, path, null);
          cacheOnUri.put(uri, connection);
        } catch (URISyntaxException e) {
          logger.error("Unable to create a localhost alias URI.");
        }
      }
    }
  }
}
