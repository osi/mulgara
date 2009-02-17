package org.mulgara.protocol.http;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.mulgara.connection.Connection;
import org.mulgara.connection.ConnectionException;
import org.mulgara.connection.ConnectionFactory;
import org.mulgara.connection.SessionConnection;
import org.mulgara.query.QueryException;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.SessionFactoryProvider;

public abstract class MulgaraServlet extends HttpServlet {

  /** Serialization ID. */
  private static final long serialVersionUID = -8019499041312491482L;

  /** The logger. */
  final static Logger logger = Logger.getLogger(MulgaraServlet.class.getName());

  /** Session value for database connection. */
  static final String CONNECTION = "session.connection";

  /** The name of the parameter for the host name. */
  protected static final String HOST_NAME_PARAM = "mulgara.config.hostname";

  /** The name of the parameter for the server name. */
  protected static final String SERVER_NAME_PARAM = "mulgara.config.servername";

  /** The default name to use for the host. */
  protected static final String DEFAULT_HOSTNAME = "localhost";

  /** The default name to use for the server. */
  protected static final String DEFAULT_SERVERNAME = "server1";

  /** The server for finding a session factory. */
  protected SessionFactoryProvider server;

  /** A URI for the server, to be used if no server is provided. */
  private URI serverUri;

  /** Session factory for accessing the database. */
  private SessionFactory cachedSessionFactory;

  /** Factory for building and caching connections, based on URI. */
  private ConnectionFactory connectionFactory;


  public MulgaraServlet() {
    server = null;
    cachedSessionFactory = null;
    connectionFactory = new ConnectionFactory();
    serverUri = null;
  }


  public MulgaraServlet(SessionFactoryProvider server) {
    this.server = server;
    cachedSessionFactory = null;
    connectionFactory = null;
    serverUri = null;
  }


  /**
   * This is irrelevant except when the server is not provided, for instance, when deployed
   * in a Web ARchive (WAR) file.
   * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
   */
  public void init(ServletConfig config) {
    ServletContext context = config.getServletContext();
    String host = context.getInitParameter(HOST_NAME_PARAM);
    if (host == null) host = DEFAULT_HOSTNAME;

    String servername = context.getInitParameter(SERVER_NAME_PARAM);
    if (servername == null) servername = DEFAULT_SERVERNAME;

    String uri = "rmi://" + host + "/" + servername;
    try {
      serverUri = new URI(uri);
    } catch (URISyntaxException e) {
      logger.error("Badly formed server URI: " + uri);
    }
  }


  /**
   * Gets the connection for the current session, creating it if it doesn't exist yet.
   * @param req The current request environment.
   * @return A connection that is tied to this HTTP session.
   * @throws IOException When an error occurs creating a new session.
   */
  protected Connection getConnection(HttpServletRequest req) throws IOException, IllegalStateException {
    HttpSession httpSession = req.getSession();
    Connection connection = (Connection)httpSession.getAttribute(CONNECTION);
    if (connection == null) {
      try {
        connection = getConnection();
      } catch (QueryException qe) {
        throw new IOException("Unable to create a connection to the database. " + qe.getMessage());
      }
      httpSession.setAttribute(CONNECTION, connection);
    }
    return connection;
  }


  /**
   * Get an existing connection to the configured server, or else create a new one.
   * @return A Connection to the required server.
   * @throws QueryException If there was an error asking an internal server for a connection.
   */
  private Connection getConnection() throws QueryException, IOException {
    assert connectionFactory != null ^ server != null;
    if (connectionFactory == null) {
      return new SessionConnection(getSessionFactory().newSession(), null, null);
    } else {
      try {
        return connectionFactory.newConnection(serverUri);
      } catch (ConnectionException e) {
        throw new IOException("Unable to create a connection to the database identified by: " + serverUri + " (" + e.getMessage() + ")");
      }
    }
  }

  /**
   * This method allows us to put off getting a session factory until the server is
   * ready to provide one.
   * @return A new session factory.
   */
  private SessionFactory getSessionFactory() throws IllegalStateException {
    if (cachedSessionFactory == null) {
      cachedSessionFactory = server.getSessionFactory();
      if (cachedSessionFactory == null) throw new IllegalStateException("Server not yet ready. Try again soon.");
    }
    return cachedSessionFactory;
  }

}