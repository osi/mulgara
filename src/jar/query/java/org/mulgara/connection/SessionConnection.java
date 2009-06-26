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
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.mulgara.jena.GraphMulgara;
import org.mulgara.query.QueryException;
import org.mulgara.server.NonRemoteSessionException;
import org.mulgara.server.Session;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.driver.SessionFactoryFinder;
import org.mulgara.server.driver.SessionFactoryFinderException;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.JenaException;

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
  
  /** The factory used to create this connection */
  private ConnectionFactory factory = null;
  
  /** Indicates the current autocommit state */
  private boolean autoCommit = true;
  
  /** Indicates the connection has been closed */
  private boolean closed = false;

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
  public SessionConnection(URI serverUri, boolean isRemote) throws ConnectionException {
    setServerUri(serverUri, isRemote);
  }


  /**
   * Creates a new connection, given a preassigned session.
   * @param session The session to connect with.
   */
  public SessionConnection(Session session) {
    this(session, null, null);
  }
  
  /**
   * Creates a new connection, given a preassigned session.
   * @param session The session to connect with.
   * @param securityDomainUri The security domain URI for the session
   */
  public SessionConnection(Session session, URI securityDomainUri) {
    this(session, securityDomainUri, null);
  }
  
  
  /**
   * Creates a new connection, given a preassigned session
   * @param session The session to connect with
   * @param securityDomainUri The security domain URI for the session
   * @param serverUri The server URI, needed for re-caching the session with the factory
   */
  public SessionConnection(Session session, URI securityDomainUri, URI serverUri) {
    if (session == null) throw new IllegalArgumentException("Cannot create a connection without a server.");
    setSession(session, securityDomainUri, serverUri);    
  }
  
    
  /**
   * If a Connection was abandoned by the client without being closed first, attempt to
   * reclaim the session for use by future clients.
   */
  protected void finalize() throws QueryException {
    if (!closed) {
      close();
    }
  }
  
  /**
   * Used to set a reference back to the factory that created it.  If the factory
   * reference is set, then the session will be re-cached when this connection is closed.
   * @param factory The factory that created this connection.
   */
  void setFactory(ConnectionFactory factory) {
    this.factory = factory;
  }
  
  
  /**
   * Give login credentials and security domain to the current session.  This should only be needed
   * once since the session does not change.
   * @param securityDomainUri The security domain for the login.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(URI securityDomainUri, String user, char[] password) {
    checkState();
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
    checkState();
    if (securityDomainUri == null) throw new IllegalArgumentException("Must have a security domain to yuse credentials");
    session.login(securityDomainUri, user, password);
  }


  /**
   * @return the session
   */
  public Session getSession() {
    checkState();
    return session;
  }


  /**
   * Starts and commits transactions on this connection, by turning the autocommit
   * flag on and off. 
   * @param autoCommit <code>true</code> if the flag is to be on.
   * @throws QueryException The session could not change state.
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException {
    checkState();
    if (this.autoCommit != autoCommit) {
      this.autoCommit = autoCommit;
      session.setAutoCommit(autoCommit);
    }
  }


  /**
   * @return the autoCommit value
   */
  public boolean getAutoCommit() {
    checkState();
    return autoCommit;
  }


  /**
   * Closes the current connection.
   */
  public void close() throws QueryException {
    checkState();
    closed = true;
    
    if (factory != null) {
      factory.releaseSession(serverUri, session);
    }
  }
  
  
  /**
   * Disposes of the current connection and any underlying resources.
   */
  public void dispose() throws QueryException {
    checkState();
    closed = true;
    
    if (factory != null) {
      factory.disposeSession(session);
    }
    
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
   * Throws an IllegalStateException if the connection has already been closed.
   */
  private void checkState() {
    if (closed) {
      throw new IllegalStateException("Attempt to access a closed connection");
    }
  }

  /**
   * Sets the session information for this connection
   * @param session The session to set to.
   * @param securityDomainUri The security domain to use for the session.
   * @param serverUri The server the session is connected to.
   */
  private void setSession(Session session, URI securityDomainUri, URI serverUri) {
    this.session = session;
    this.securityDomainUri = securityDomainUri;
    this.serverUri = serverUri;
    if (logger.isDebugEnabled()) logger.debug("Set server URI to: " + serverUri);
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
        uri = SessionFactoryFinder.findServerURI();
      }

      if (logger.isDebugEnabled()) logger.debug("Finding session factory for " + uri);
      
      SessionFactory sessionFactory = SessionFactoryFinder.newSessionFactory(uri, isRemote);
      if (logger.isDebugEnabled()) logger.debug("Found " + sessionFactory.getClass() +
          " session factory, obtaining session with " + uri);

      // create a new session and set this connection to it
      if (securityDomainUri == null) securityDomainUri = sessionFactory.getSecurityDomain();
      setSession(sessionFactory.newSession(), sessionFactory.getSecurityDomain(), uri);

    } catch (SessionFactoryFinderException e) {
      throw new ConnectionException("Unable to connect to a server", e);
    } catch (NonRemoteSessionException e) {
      throw new ConnectionException("Error connecting to the local server", e);
    } catch (QueryException e) {
      throw new ConnectionException("Data error in connection attempt", e);
    }
    assert session != null;
  }


  /**
   * Tests if the Connection is being conducted over a network.
   * @return <code>true</code> if the underlying session is not local.
   */
  public boolean isRemote() {
    return session != null && !session.isLocal();
  }


  ///////////////////////////////////////////////////////////////////////
  // The JenaConnection interface
  ///////////////////////////////////////////////////////////////////////

  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Model. Does not create the remote model.
   * @param graphURI The URI,as a string, of the Mulgara model in the server 
   * @return A Jena Model
   */
  public Graph connectGraph(String graphURI) {
    return connectGraph(graphURI, false);
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Graph.
   * @param graphURI The URI,as a string, of the Mulgara model in the server 
   * @param createIfDoesNotExist Create the Mulgara model if it does not already exist.
   * @return A Jena Model
   */
  public Graph connectGraph(String graphURI, boolean createIfDoesNotExist) {
    try {
      return connectGraph(new URI(graphURI), createIfDoesNotExist);
    } catch (URISyntaxException ex) {
      throw new JenaException("JenaMulgara.connectGraph", ex);
    }
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Graph.
   * @param graphURI The URI of the Mulgara model.
   * @param createIfDoesNotExist Create the Mulgara model if it does not already exist.
   * @return A Jena Graph
   */
  public Graph connectGraph(URI graphURI, boolean createIfDoesNotExist) {
    if (createIfDoesNotExist) {
      try {
        if (!session.modelExists(graphURI)) session.createModel(graphURI, null);
      } catch (QueryException ex) {
        throw new JenaException(ex);
      }
    }
    return new GraphMulgara(session, graphURI) ;
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Model.  
   * Does not create the remote model.
   * @param graphURI The URI,as a string, of the Mulgara model in the server 
   * @return A Jena Model
   */
  public Model connectModel(String graphURI) {
    return connectModel(graphURI, false);
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Model.
   * @param graphURI The URI,as a string, of the Mulgara model in the server 
   * @param createIfDoesNotExist Create the Mulgara model if it does not already exist.
   * @return A Jena Model
   */
  public Model connectModel(String graphURI, boolean createIfDoesNotExist) {
    try {
      return connectModel(new URI(graphURI), createIfDoesNotExist);
    } catch (URISyntaxException ex) {
      throw new JenaException("JenaMulgara.createModel", ex);
    }
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Model.
   * @param graphURI The URI of the Mulgara model.
   * @param createIfDoesNotExist Create the Mulgara model if it does not already exist.
   * @return A Jena Model
   */
  public Model connectModel(URI graphURI, boolean createIfDoesNotExist) {
    Graph g = connectGraph(graphURI, createIfDoesNotExist);
    return ModelFactory.createModelForGraph(g);
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Model.  
   * Creates the remote graph if it does not already exist.
   * @param graphURI The URI,as a string, of the Mulgara model in the server 
   * @return A Jena Model
   */
  public Graph createGraph(String graphURI) {
    return connectGraph(graphURI, true);
  }


  /**
   * Connect to RDF data stored in a Mulgara server as a Jena Model,
   * creating the model if it does not already exist.
   * @param graphURI The URI,as a string, of the Mulgara model in the server 
   * @return A Jena Model
   */
  public Model createModel(String graphURI) {
    return connectModel(graphURI, true);
  }


  /**
   * Drop the Mulgara graph/model.
   * @param graphURI The URI of the graph
   */
  public void dropGraph(String graphURI) {
    try {
      dropGraph(new URI(graphURI)) ;
    } catch (URISyntaxException ex) {
      throw new JenaException("JenaMulgara.dropGraph", ex);
    }
  }


  /**
   * Drop the Mulgara graph/model.
   * @param graphURI The URI of the graph
   */
  public void dropGraph(URI graphURI) {
    try {
      session.removeModel(graphURI);
    } catch (Exception ex) {
      throw new JenaException("JenaMulgara.dropGraph", ex);
    }
  }

}
