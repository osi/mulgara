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

import java.net.URI;

import org.apache.log4j.Logger;
import org.mulgara.query.QueryException;
import org.mulgara.server.NonRemoteSessionException;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.driver.SessionFactoryFinder;
import org.mulgara.server.driver.SessionFactoryFinderException;

/**
 * A connection for sending commands to a server using a session object.
 *
 * @created 2007-08-21
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class SessionConnection extends CommandExecutor implements Connection {

  /** Logger. */
  private static final Logger logger = Logger.getLogger(SessionConnection.class.getName());
  
  /** The URI for the server to establish a session on. */
  private URI serverUri;
  
  /** The security domain URI. */
  private URI securityDomainUri;
  
  /** The session to use for this connection. */
  private Session session;

  /** Indicates the current autocommit state */
  private boolean autoCommit = true;

  /**
   * Creates a new connection, given a URI to a server.
   * @param serverUri The URI to connect to.
   * @throws ConnectionException There was a problem establishing the details needed for a connection.
   */
  public SessionConnection(URI serverUri) throws ConnectionException {
    this(serverUri, true);
  }


  /**
   * Creates a new connection, given a URI to a server,
   * and a flag to indicate if the server should be "remote".
   * @param serverUri The URI to connect to.
   * @param isRemote <code>true</code> for a remote session, <code>false</code> for local.
   * @throws ConnectionException There was a problem establishing the details needed for a connection.
   */
  SessionConnection(URI serverUri, boolean isRemote) throws ConnectionException {
    setServerUri(serverUri, isRemote);
  }


  /**
   * Creates a new connection, given a preassigned session.
   * @param session The session to connect with.
   * @throws ConnectionException There was a problem establishing the details needed for a connection.
   */
  public SessionConnection(Session session) {
    this(session, null);
  }
  
  
  /**
   * Creates a new connection, given a preassigned session.
   * @param session The session to connect with.
   * @throws ConnectionException There was a problem establishing the details needed for a connection.
   */
  public SessionConnection(Session session, URI securityDomainUri) {
    if (session == null) throw new IllegalArgumentException("Cannot create a connection without a server.");
    setSession(session, securityDomainUri);
  }
  
  
  /**
   * Give login credentials and security domain to the current session.  This should only be needed
   * once since the session does not change.
   * @param securityDomainUri The security domain for the login.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(URI securityDomainUri, String user, char[] password) {
    if (securityDomainUri == null) throw new IllegalArgumentException("Must have a security domain to yuse credentials");
    this.securityDomainUri = securityDomainUri;
    setCredentials(user, password);
  }


  /**
   * Give login credentials for the current security domain to the current session.
   * This should only be needed
   * once since the session does not change.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(String user, char[] password) {
    if (securityDomainUri == null) throw new IllegalArgumentException("Must have a security domain to yuse credentials");
    session.login(securityDomainUri, user, password);
  }


  /**
   * @return the session
   */
  public Session getSession() {
    return session;
  }


  /**
   * Starts and commits transactions on this connection, by turning the autocommit
   * flag on and off. 
   * @param autocommit <code>true</code> if the flag is to be on.
   * @throws QueryException The session could not change state.
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException {
    if (this.autoCommit != autoCommit) {
      this.autoCommit = autoCommit;
      session.setAutoCommit(autoCommit);
    }
  }


  /**
   * @return the autoCommit value
   */
  public boolean getAutoCommit() {
    return autoCommit;
  }


  /**
   * Closes the current connection.
   */
  public void close() throws QueryException {
    if (session != null) {
      session.close();
      session = null;
    }
  }

  // Private methods //

  /**
   * @return the serverUri
   */
  URI getServerUri() {
    return serverUri;
  }


  /**
   * @return the securityDomainUri
   */
  URI getSecurityDomainUri() {
    return securityDomainUri;
  }


  /**
   * Sets the session information for this connection
   * @param session The session to set to.
   * @param securityDomainURI The security domain to use for the session.
   */
  private void setSession(Session session, URI securityDomainUri) {
    this.session = session;
    this.securityDomainUri = securityDomainUri;
  }


  /**
   * Establishes a session for this connection.
   * @param uri The URI to set for the server.
   * @param isRemote <code>true</code> for a remote session, <code>false</code> for local.
   * @throws ConnectionException There was a problem establishing a session.
   */
  private void setServerUri(URI uri, boolean isRemote) throws ConnectionException {
    
    try {
      if (uri == null) {
        // no model given, and the factory didn't cache a connection, so make one up.
        serverUri = SessionFactoryFinder.findServerURI();
      } else {
        serverUri = uri;
      }
      if (logger.isDebugEnabled()) logger.debug("Set server URI to: " + serverUri);

      if (logger.isDebugEnabled()) logger.debug("Finding session factory for " + uri);
      
      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(serverUri, isRemote);
      if (logger.isDebugEnabled()) logger.debug("Found " + sessionFactory.getClass() +
          " session factory, obtaining session with " + uri);

      // create a new session and set this connection to it
      if (securityDomainUri == null) securityDomainUri = sessionFactory.getSecurityDomain();
      setSession(sessionFactory.newSession(), sessionFactory.getSecurityDomain());

    } catch (SessionFactoryFinderException e) {
      throw new ConnectionException("Unable to connect to a server", e);
    } catch (NonRemoteSessionException e) {
      throw new ConnectionException("Error connecting to the local server", e);
    } catch (QueryException e) {
      throw new ConnectionException("Data error in connection attempt", e);
    }
    assert session != null;
  }
  
}
