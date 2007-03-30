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

package org.mulgara.server;

// java 2 standard packages
import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.rmi.RMISecurityManager;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

import javax.naming.*;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

// log4j packages
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

// locally written packages
import org.mulgara.config.ExternalConfigPaths;
import org.mulgara.config.Jetty;
import org.mulgara.config.MulgaraConfig;
import org.mulgara.config.Listener;
import org.mulgara.server.SessionFactory;
import org.mulgara.store.StoreException;
import org.mulgara.store.xa.SimpleXAResourceException;
import org.mulgara.util.TempDir;

// jetty packages
import org.mortbay.http.HttpContext;
import org.mortbay.http.HashUserRealm;
import org.mortbay.http.HttpListener;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.handler.SecurityHandler;

// jetty packages
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.WebApplicationContext;
import org.mortbay.jetty.servlet.WebApplicationHandler;
import org.mortbay.util.InetAddrPort;
import org.mortbay.util.MultiException;
import org.mortbay.util.Resource;
import org.mortbay.xml.XmlConfiguration;

/**
 * Canonical embedded production Mulgara server. <p>
 *
 * Creates a Mulgara server instance, and a SOAP server instance to handle <a
 * href="http://www.w3.org/TR/SOAP">SOAP</a> requests for the Mulgara server.
 * </p>
 *
 * @created 2001-10-04
 *
 * @author Tom Adams
 * @author Simon Raboczi
 * @author Paul Gearon
 * @author Tate Jones
 *
 * @modified $Date: 2005/01/13 01:55:32 $ by $Author: raboczi $
 *
 * @maintenanceAuthor $Author: raboczi $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 *
 * @see <a href="http://developer.java.sun.com/developer/JDCTechTips/2001/tt0327.html#jndi">
 *      <cite>JNDI lookup in distributed systems</cite> </a>
 */
public class EmbeddedMulgaraServer extends Thread {

  /**
   * Get line separator.
   */
  protected static final String eol = System.getProperty("line.separator");

  /**
   * the default port to listen for a shutdown
   */
  public final static int SHUTDOWN_PORT = 6789;

  /**
   * the request required to shutdown mulgara
   */
  public final static String SHUTDOWN_MSG = "shutdownmulgara";

  /**
   * the key to the bound host name in the attribute map of the servlet context
   */
  public final static String BOUND_HOST_NAME_KEY = "boundHostname";

  /**
   * the key to the bound server model uri in the attribute map of the
   * servlet context
   */
  public final static String SERVER_MODEL_URI_KEY = "serverModelURI";

  //
  // Constants
  //

  /**
   * the documentation file path
   */
  private final static String DOCS_PATH = "docs";

  /**
   * the sample data file path
   */
  private final static String DATA_PATH = "data";

  /**
   * the MULGARAV web application file
   */
  private static final String MULGARAV_WEBAPP = "mulgarav.war";

  /**
   * the MULGARAV path
   */
  private static final String MULGARAV_PATH = "mulgarav";

  /**
   * the documentation file path
   */
  private final static String WEBAPP_PATH = "webapps";

  /**
   * the Web Services web application file
   */
  private final static String WEBSERVICES_WEBAPP = "webservices.war";

  /**
   * the Web Services path.
   */
  private final static String WEBSERVICES_PATH = "webservices";

  /**
   * the Web UI web application file
   */
  private final static String WEBUI_WEBAPP = "webui.war";

  /**
   * the Web UI path.
   */
  private final static String WEBUI_PATH = "webui";

  /**
   * the logging category to log to
   */
  protected static Logger log = null;

  //
  // Members
  //

  /**
   * the embedded configuration file path
   */
  protected static String CONFIG_PATH;

  /**
   * the RMI permission security policy file path
   */
  protected static String RMI_SECURITY_POLICY_PATH;

  /**
   * static reference to the local session factory
   */
  private static SessionFactory localSessionFactory = null;

  /**
   * static reference to the hostname this Mulgara server is bound to
   */
  private static String boundHostname = null;

  /**
   * static reference to the port the server will accept SOAP requests on
   */
  private static int httpPort = 8080;

  /**
   * the port the RMI registry is bound to
   */
  private static int rmiPort = 1099;

  /**
   * the server URI
   */
  private static URI serverURI = null;

  /**
   * the Mulgara server instance
   */
  private ServerMBean server = null;

  /**
   * the HTTP server instance
   */
  private Server httpServer = null;

  /**
   * the embedded Mulgara server configuration
   */
  private MulgaraConfig config = null;

  /**
   * the (RMI) name of the server
   */
  private String serverName = null;

  /**
   * the path to persist server data to
   */
  private String persistencePath = null;

  /**
   * the hostname to bind the server to
   */
  private String host = null;

  /**
   * the hostname to accept SOAP requests on
   */
  private String httpHost = null;

  /**
   * the smtp server for email notications
   */
  private String smtp = null;

  /**
   * a shutdown hook to allow a local client to shutdown the server *
   */
  private ShutdownHook shutdownHook = null;

  // Static block
  static {

    CONFIG_PATH = "conf/mulgara-x-config.xml";
    RMI_SECURITY_POLICY_PATH = "conf/mulgara-rmi.policy";

    // Configure the logger for this class
    log = Logger.getLogger(EmbeddedMulgaraServer.class.getName());

  }

  //
  // Constructors
  //

  /**
   * Creates a new embedded Mulgara server. <p>
   *
   * Users of this constructor should call {@link #setServer(ServerMBean)}
   * and {@link #setHttpServer(Server)}. </p>
   */
  protected EmbeddedMulgaraServer() {

    // nothing
  }

  // EmbeddedMulgaraServer()

  /**
   * Creates a new embedded Mulgara server.
   *
   * @param server PARAMETER TO DO
   * @param httpServer the SOAP server
   */
  private EmbeddedMulgaraServer(ServerMBean server, Server httpServer) {

    // set the members
    this.setServer(server);
    this.setHttpServer(httpServer);

    // log that we've created a new server
    //log.info("Created embedded Mulgara server");
  }

  // setLocalSessionFactory()

  /**
   * Sets a static reference to the hostname this Mulgara server is bound to.
   *
   * @param boundHostname to the hostname this Mulgara server is bound to
   */
  public static void setBoundHostname(String boundHostname) {
    EmbeddedMulgaraServer.boundHostname = boundHostname;
    ServerInfo.setBoundHostname(boundHostname);
  }

  /**
   * Attempts to obtain the localhost name or defaults to the
   * IP address of the localhost
   *
   * @return the hostname this Mulgara server is bound to
   */
  public static String getResolvedLocalHost() {

    String hostname = null;

    try {

      // attempt for the localhost canonical host name
      hostname = InetAddress.getLocalHost().getCanonicalHostName();

    } catch ( UnknownHostException uhe ) {

      try {

        // attempt to get the IP address for the localhost
        hostname = InetAddress.getByName("localhost").getHostAddress();
        log.info("Obtain localhost IP address of " + hostname);

      } catch (UnknownHostException uhe2 ) {

        // default to the localhost IP
        hostname = "127.0.0.1";
        log.info("Defaulting to 127.0.0.1 IP address");
      }
    }

    return hostname;

  }

  // getSessionFactory()

  /**
   * Returns a static reference to the local
   * {@link org.mulgara.server.SessionFactory}.
   * <p>
   * This can be used to obtain a session for sending queries directly to the
   * underlying database without going across the network. For example:
   * </p>
   * <pre>
   * Session session = EmbeddedMulgaraServer.getLocalSessionFactory().newSession();
   * ItqlInterpreterBean interpreter = new ItqlInterpreterBean(session);
   * String answer = interpreter.executeQueryToString(
   *     "select $s $p $o from <rmi://localhost/server1#model> where $s $p $o;");
   * </pre>
   *
   * @return the local {@link org.mulgara.server.SessionFactory} of this
   *      Mulgara database instance, or <code>null</code> if no local session
   *      factory has been set
   */
  public static SessionFactory getLocalSessionFactory() {

    return EmbeddedMulgaraServer.localSessionFactory;

  } // getLocalSessionFactory()


  /**
   * Returns the canonical server URI.
   *
   * For example, <code>rmi://localhost/server1</code>.
   *
   * @return URI the canonical server URI.
   */
  public static URI getServerURI() {

    return serverURI;
  }

  /**
   * Returns a static reference to the hostname this Mulgara server is bound to.
   * <p>
   *
   * Local clients (in the same JVM) should use this method to determine the
   * hostname Mulgara is bound to, rather than assuming the local hostname. </p>
   *
   * @return to the hostname this Mulgara server is bound to
   */
  public static String getBoundHostname() {
    return EmbeddedMulgaraServer.boundHostname;
  }

  /**
   * Sets the canonical server URI.
   *
   * For example, <code>rmi://localhost/server1</code>.
   *
   * @param serverURI  the canonical server URI.
   */
  private static void setServerURI(URI serverURI) {
    EmbeddedMulgaraServer.serverURI = serverURI;
    ServerInfo.setServerURI(serverURI);
  }

  //
  // Main
  //

  /**
   * Starts a Mulgara server and a WebServices (SOAP) server to handle SOAP queries to the
   * Mulgara server. <p>
   *
   * Database files for the Mulgara server are written to the directory from
   * where this class was run. </p>
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {

    // report the version and build number
    System.out.println("@@build.label@@");

    EmbeddedMulgaraServer server = new EmbeddedMulgaraServer();
    EmbeddedMulgaraOptionParser optsParser =
        new EmbeddedMulgaraOptionParser(args);

    try {

      // load up the basic logging configuration in case we get an error before
      // we've loaded the real logging configuration

      BasicConfigurator.configure();

      // parse the command line options to the server
      optsParser.parse();

      // configure the server, overriding the defaults if command line
      // arguments specify it
      boolean startServer = server.configure(optsParser);

      // start the server if we're allowed to
      if (startServer) {

        // create the params need for a new Mulgara instance
        File statePath =
            new File(new File(server.getPersistencePath()),
            server.getServerName());
        String tripleStore = server.getConfig().getTripleStoreImplementation();
        String hostname = server.getHost();

        //set the tripleStoreImplemention property
        System.setProperty("triple.store.implementation", tripleStore);

        // create a Mulgara server instance
        ServerMBean newServer = server.createServer(
          server.getServerName(), statePath,
          hostname,
          getRMIPort(),
          tripleStore,
          //"org.mulgara.server.beep.BEEPServer"
          "org.mulgara.server.rmi.RmiServer"
        );
        server.setServer(newServer);

        // install a shutdown hook for System.exit(#);
        if (log.isDebugEnabled()) {

          log.debug("Registering shutdown hook");
        }

        // Remove the need for shutdownhook
        // force exit when shutdown port is triggered 
        // Runtime.getRuntime().addShutdownHook(server);

        // tell the RMI server who we are
        // setRmiLocalURI(server.getServerURI());

        // start the mulgara server
        server.startServer();

        // create a HTTP server instance
        server.setHttpServer(server.createHttpServer());

        // start the HTTP server
        server.startHttpServer();

        // start the shutdown hook
        server.startShutdownHookServer();


        // log that we've start a Mulgara server
        if (log.isInfoEnabled()) {

          log.info("Successfully started Mulgara server at " +
              EmbeddedMulgaraServer.getServerURI().toString() +
              " in directory " +
              ( (AbstractServer) server.getServer()).getDir());

          log.info(eol + eol + "Typing Ctrl-C in this console or killing " +
              "this process id will shutdown this server");

          //Launch a browser if specified by a 'browser' system property
          server.launchBrowser();
        }

        // end if
      }

      // end if
    }
    catch (MultiException me) {

      // get the list of exceptions
      List exceptions = me.getExceptions();

      // log each one
      for (Iterator ei = exceptions.iterator(); ei.hasNext(); ) {

        Exception e = (Exception)ei.next();
        log.error("MultiException", e);
        e.printStackTrace();
      }

      // end for
      System.exit(2);
    }
    catch (EmbeddedMulgaraOptionParser.UnknownOptionException uoe) {

      // let the user know
      System.err.println("ERROR: Unknown option(s): " + uoe.getOptionName());
      server.printUsage();

      System.exit(3);
    }
    catch (EmbeddedMulgaraOptionParser.IllegalOptionValueException iove) {

      // let the user know
      System.err.println("ERROR: Illegal value '" + iove.getValue() +
          "' for option " + iove.getOption().shortForm() + "/" +
          iove.getOption().longForm());
      server.printUsage();

      System.exit(4);
    }
    catch (Exception e) {

      // log the error
      log.error("Exception in main", e);
      e.printStackTrace();

      System.exit(5);
    }

    // try-catch
  }

  /**
   * Shutdown the Mulgara server
   *
   * @param args command line arguments
   */
  public static void shutdown(String[] args) {

    // create a basic Configurator for the shutdown
    BasicConfigurator.configure();

    // get the socket port
    int port = EmbeddedMulgaraServer.getShutdownHookPort();

    Socket clientSocket = null;

    // create a socket to the local host and port
    try {

      clientSocket = new Socket(InetAddress.getByName("localhost"), port);

      PrintWriter toServer =
          new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

      toServer.println(EmbeddedMulgaraServer.SHUTDOWN_MSG);
      toServer.flush();
      toServer.close();
    }
    catch (ConnectException ioCon) {

      //log.warn("Unable to establish connection to server on port " + port +
      //". The server may not be running."  );
      System.out.println("Server is not currently running");
    }
    catch (IOException ioEx) {

      log.error("Unable to establish connection to shutdown server " +
          "on port " + port, ioEx);
    }
    catch (SecurityException secEx) {

      log.error("Unable to establish connection shutdown server due to a " +
          "security exception. Check security policy", secEx);
    }
    catch (Exception ex) {

      log.error("Unable to establish shutdown connection to shutdown server " +
          "on port " + port, ex);
    }
    finally {

      // attempt to close the socket
      try {

        clientSocket.close();
      }
      catch (Exception ex) {

        /* skip */
      }
    }

    // finally
  }

  /**
   * Sets a static reference to the local session factory.
   *
   * @param localSessionFactory The new LocalSessionFactory value
   */
  private static void setLocalSessionFactory(SessionFactory localSessionFactory) {

    EmbeddedMulgaraServer.localSessionFactory = localSessionFactory;
    ServerInfo.setLocalSessionFactory(localSessionFactory);

  }

  /**
   * Sets the port the RMI registry is bound to.
   *
   * @param rmiPort the port the RMI registry is bound to
   */
  protected static void setRMIPort(int rmiPort) {

    EmbeddedMulgaraServer.rmiPort = rmiPort;
    System.setProperty(Context.PROVIDER_URL, "rmi://" + getBoundHostname() + ":"
        + rmiPort + "/");
    ServerInfo.setRMIPort(rmiPort);

  } // setRMIPort()

  /**
   * Returns the port the RMI registry is bound to.
   *
   * @return the port the RMI registry is bound to
   */
  public static int getRMIPort() {

    return EmbeddedMulgaraServer.rmiPort;
  }

  /**
   * Get the shutdown hook port to allow the BootStrap to shutdown the server
   * from the same machine but different JVM To override the default port of
   * 6789 set a system property called shutdownhook.port
   *
   * @return the shutdown port for this server
   */
  private static int getShutdownHookPort() {

    int port = EmbeddedMulgaraServer.SHUTDOWN_PORT;

    // check if the default shutdown port has been
    // overrided by a system property.
    String portString = System.getProperty("shutdownhook.port");

    if ( (portString != null) && (portString.length() > 0)) {

      try {

        port = Integer.parseInt(portString);

        if (log.isInfoEnabled()) {

          log.info("Override default shutdown hook port to " + port);
        }
      }
      catch (NumberFormatException ex) {

        log.error("Unable to convert supplied port " + portString + " to int " +
            " for shutdown hook. Defaulting to port :" + portString, ex);
      }
    }

    return port;
  }

  // getRMIPort()
  //
  // Helper class methods
  //

  /**
   * Sets up any system properties needed by components.
   *
   * @throws IOException if any files embedded within the JAR file cannot be
   *      found
   */
  protected void configureSystemProperties() throws IOException {

    boolean startedLocalRMIRegistry = false;

    // attempt to start a rmiregistry
    if (System.getProperty("no_rmi") == null) {

      try {

        // start the registry
        LocateRegistry.createRegistry(EmbeddedMulgaraServer.getRMIPort());

        if (log.isInfoEnabled()) {

          log.info("RMI Registry started automatically on port " +
              EmbeddedMulgaraServer.getRMIPort());
        }

        // end if
        // set the flag
        startedLocalRMIRegistry = true;
      }
      catch (java.rmi.server.ExportException ex) {

        log.info("Existing RMI registry found on port " +
            EmbeddedMulgaraServer.getRMIPort());
      }
      catch (Exception ex) {

        log.error("Failed to start or detect RMI Registry", ex);
      } // try-catch
    } // end if

    // set system properties needed for RMI
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY,
        "com.sun.jndi.rmi.registry.RegistryContextFactory");

    if (log.isDebugEnabled()) {

      log.debug("No system security manager set");
    }
    // end if

    // only set the security policy if a RMI registry has started
    if (startedLocalRMIRegistry) {

      if (System.getProperty("java.security.policy") == null) {

        // log that we're about to set a security policy
        if (log.isDebugEnabled()) {

          log.debug("Started local RMI registry -> setting security policy");
        }
        // end if

        URL mulgaraSecurityPolicyURL =
            ClassLoader.getSystemResource(RMI_SECURITY_POLICY_PATH);
        System.setProperty("java.security.policy",
            mulgaraSecurityPolicyURL.toString());

        // log the policy we've just set
        if (log.isInfoEnabled()) {

          log.info("java.security.policy set to " +
              mulgaraSecurityPolicyURL.toString());
        } // end if
      } // end if

      // create a security manager
      System.setSecurityManager(new RMISecurityManager());
    }
  }

  // configureSystemProperties()

  /**
   * Loads the embedded logging configuration (from the JAR file).
   *
   * @param loggingConfig the path to the logging configuration file
   */
  private static void loadLoggingConfig(String loggingConfig) {

    // get a URL from the classloader for the logging configuration
    URL log4jConfigURL = ClassLoader.getSystemResource(loggingConfig);

    // if we didn't get a URL, tell the user that something went wrong
    if (log4jConfigURL == null) {

      System.err.println("Unable to find logging configuration file in JAR " +
          "with " + loggingConfig +
          ", reverting to default configuration.");
      BasicConfigurator.configure();
    }
    else {

      try {

        // configure the logging service
        DOMConfigurator.configure(log4jConfigURL);

        if (log.isDebugEnabled()) {

          log.debug("Using logging configuration from " + log4jConfigURL);
        }

        // end if
      }
      catch (FactoryConfigurationError e) {

        System.err.println("Unable to configure logging service, reverting " +
            "to default configuration");
        BasicConfigurator.configure();
      }
      catch (Exception e) {

        System.err.println("Unable to configure logging service, reverting " +
            "to default configuration");
        BasicConfigurator.configure();
      }

      // try-catch
    }

    // end if
  }

  // loadLoggingConfig()

  /**
   * Loads the embedded logging configuration from an external URL.
   *
   * @param loggingConfig the URL of the logging configuration file
   */
  private static void loadLoggingConfig(URL loggingConfig) {

    // validate the loggingConfig parameter
    if (loggingConfig == null) {

      throw new IllegalArgumentException("Null \"loggingConfig\" parameter");
    }

    // end if
    try {

      // configure the logging service
      DOMConfigurator.configure(loggingConfig);

      if (log.isDebugEnabled()) {

        log.debug("Using logging configuration from " + loggingConfig);
      }

      // end if
    }
    catch (FactoryConfigurationError e) {

      System.err.println("Unable to configure logging service, reverting " +
          "to default configuration");
      BasicConfigurator.configure();
    }
    catch (Exception e) {

      System.err.println("Unable to configure logging service, reverting " +
          "to default configuration");
      BasicConfigurator.configure();
    }

    // try-catch
  }

  // loadLoggingConfig()

  /**
   * Copies a file to the system temp directory.
   *
   * @param fileURL a URL to the file to be copied
   * @return the path to the temporary file
   * @throws IOException if an error occurs while reading from the <code>fileURL</code>
   *      , or writing it to the new location
   */
  private static String copyFileToTemp(URL fileURL) throws IOException {

    // validate fileURL parameter
    if (fileURL == null) {

      throw new IllegalArgumentException("Null \"fileURL\" parameter");
    }

    // end if
    // log that we're dumping a file to the temp dir
    log.debug("Dumping " + fileURL + " to system temp directory");

    // open a stream to the file
    InputStream in = fileURL.openStream();

    // check that the file is valid
    if (in == null) {

      throw new IOException(fileURL + " does not exist");
    }

    // end if
    // create a temporary file to write the file to
    File tmpFile = TempDir.createTempFile("mulgara", ".war");
    tmpFile.deleteOnExit();

    // log the filename we're copying to
    log.debug(fileURL + " is being copied to " + tmpFile.toString());

    // get a stream so we can write to it
    FileOutputStream out = new FileOutputStream(tmpFile);

    // write the file to the temp directory
    int n;
    byte[] buf = new byte[102400];

    while ( (n = in.read(buf)) != -1) {

      out.write(buf, 0, n);
    }

    // end if
    // close the stream
    out.close();

    // log that
    log.debug("Completed copying to temp file " + tmpFile.toString());

    // return the temp filename
    return tmpFile.toString();
  }

  // run()
  //
  // Public API
  //

  /**
   * Returns a reference to the local
   * {@link org.mulgara.server.SessionFactory} of the underlying database.
   *
   * @return a {@link org.mulgara.server.SessionFactory}
   *         from the underlying database
   */
  public SessionFactory getSessionFactory() {

    SessionFactory sessionFactory = null;

    if (this.getServer() != null) {

      sessionFactory =
          ( (AbstractServer)this.getServer()).getSessionFactory();
    }

    // end if
    return sessionFactory;
  }

  // main()
  //
  // Methods overriding Thread
  //

  /**
   * Shutdown handler. <p>
   *
   * Stops and cleanly destroys the Mulgara and SOAP servers. </p>
   *
   */
  public void run() {

    // log that we're sutting down the servers
    if (log.isInfoEnabled()) {

      log.info("Shutting down server, please wait...");
    }
    else {

      // regardless of the log level output this to stdout.
      // Note. "\n" Will give us a new line beneath a Ctrl-C
      System.out.println("\nShutting down server, please wait...");
    }

    // stop RMI service
    if (this.getServer().getState() == 3) {

      // TODO: magic number 3 means "started"
      try {

        this.getServer().stop();
      }
      catch (Exception e) {

        log.error("Couldn't stop server", e);
      }

      // try-catch
    }

    // end if
    // close the server
    if (this.getServer().getState() >= 2) {

      // TODO: magic number 2 means "stopped"
      try {

        this.getServer().destroy();
      }
      catch (Exception e) {

        log.error("Couldn't destroy server", e);

      } // try-catch

    } // end if

    // shut down the SOAP server
    try {

      if (this.getHttpServer() != null) {

        this.getHttpServer().stop();

      } // end if
    }
    catch (Exception e) {

      log.error("Couldn't destroy http server", e);
    }

    // try-catch
    // log that we've shut down the servers
    if (log.isInfoEnabled()) {

      log.info("Completed shutting down server");
    }
    else {

      // regardless of the log level out this to stdout.
      System.out.println("Completed shutting down server");
    }

    //Clean up any temporary directories and files
    this.cleanUpTemporaryFiles();
  } // run()

  /**
   * Sets the server instance.
   *
   * @param server the server instance
   */
  public void setServer(ServerMBean server) {

    this.server = server;
  } // setMulgaraServer()

  /**
   * Sets the SOAP server instance.
   *
   * @param httpServer the SOAP server instance
   */
  public void setHttpServer(Server httpServer) {

    this.httpServer = httpServer;
  } // setHttpServer()

  /**
   * Sets the embedded Mulgara server configuration.
   *
   * @param config the embedded Mulgara server configuration
   */
  public void setConfig(MulgaraConfig config) {

    this.config = config;
  } // setConfig()

  /**
   * Sets the (RMI) name of the server.
   *
   * @param serverName the (RMI) name of the server
   */
  public void setServerName(String serverName) {

    this.serverName = serverName;
  } // setServerName()

  /**
   * Sets the path to persist server data to.
   *
   * @param persistencePath the path to persist server data to
   */
  public void setPersistencePath(String persistencePath) {

    this.persistencePath = persistencePath;
  } // setPersistencePath()

  /**
   * Sets the hostname to bind the Mulgara server to.
   *
   * @param host the hostname to bind the server to
   */
  public void setHost(String host) {

    this.host = host;
  } // setHost()

  /**
   * Sets the hostname to accept HTTP requests on.
   *
   * @param httpHost the hostname to accept HTTP requests on
   */
  public void setHttpHost(String httpHost) {

    this.httpHost = httpHost;
  } // setHttpHost()

  /**
   * Sets the port the server will accept HTTP requests on.
   static reference to *
   * @param httpPort the port the server will accept HTTP requests on
   */
  public static void setHttpPort(int httpPort) {

    EmbeddedMulgaraServer.httpPort = httpPort;
    ServerInfo.setHttpPort(httpPort);
  } // setHttpPort()

  /**
   * Sets the smtp server name for email notifications
   *
   * @param smtp The new SMTP value
   */
  public void setSMTP(String smtp) {

    this.smtp = smtp;
  } // setSMTP()

  /**
   * Returns the Mulgara server instance.
   *
   * @return the Mulgara server instance
   */
  public ServerMBean getServer() {

    return this.server;
  } // getMulgaraServer()

  /**
   * Returns the SOAP server instance.
   *
   * @return the SOAP server instance
   */
  public Server getHttpServer() {

    return this.httpServer;
  } // getHttpServer()

  /**
   * Returns the embedded Mulgara server configuration.
   *
   * @return the embedded Mulgara server configuration
   */
  public MulgaraConfig getConfig() {

    return this.config;
  } // getConfig()

  /**
   * Returns the (RMI) name of the server.
   *
   * @return the (RMI) name of the server
   */
  public String getServerName() {

    return this.serverName;
  } // getServerName()

  /**
   * Returns the path to persist server data to.
   *
   * @return the path to persist server data to
   */
  public String getPersistencePath() {

    return this.persistencePath;
  } // getPersistencePath()

  /**
   * Returns the hostname to bind the Mulgara server to.
   *
   * @return the hostname to bind the Mulgara server to
   */
  public String getHost() {

    return this.host;
  } // getHost()

  /**
   * Returns the hostname to accept HTTP requests on.
   *
   * @return the hostname to accept HTTP requests on
   */
  public String getHttpHost() {

    return this.httpHost;
  } // getHttpHost()

  /**
   * Returns the port the server will accept HTTP requests on.
   *
   * @return the port the server will accept HTTP requests on
   */
  public static int getHttpPort() {

    return EmbeddedMulgaraServer.httpPort;
  } // getHttpPort()

  /**
   * Returns the smtp server for email noticiations
   *
   * @return the smtp server
   */
  public String getSMTP() {

    return this.smtp;
  } // getSMTP()

  //
  // Internal methods
  //

  /**
   * Configures an embedded Mulgara server.
   *
   * @param parser the options parser containing the command line arguments to
   *      the server
   * @return true if the server is allowed to start
   */
  public boolean configure(EmbeddedMulgaraOptionParser parser) {

    // flag to indicate whether we can start the server
    boolean startServer = true;

    try {

      // find out if the user wants help
      if (parser.getOptionValue(EmbeddedMulgaraOptionParser.HELP) != null) {

        // print the help
        printUsage();

        // don't start the server
        startServer = false;
      }
      else {

        // load the Mulgara configuration file
        Object configURL =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.SERVER_CONFIG);

        if (configURL == null) {

          // get a URL to the default server configuration file
          URL defaultConfigURL =
              ClassLoader.getSystemResource(CONFIG_PATH);

          if (defaultConfigURL == null) {

            throw new IOException("Unable to locate embedded server " +
                "configuration file");
          }

          // end if
          // use the fefault configuration file
          configURL = defaultConfigURL.toString();
        }

        // end if
        // log what we're doing
        /*
        if (log.isInfoEnabled()) {

          log.info("Configuring embedded server from " +
              configURL);
        }
        */

        // end if
        // configure the server
        MulgaraConfig config =
            MulgaraConfig.unmarshal(new InputStreamReader(
            (new URL( (String) configURL)).openStream()));
        config.validate();
        this.setConfig(config);

        // disable automatic starting of the RMI registry
        if (parser.getOptionValue(EmbeddedMulgaraOptionParser.NO_RMI) != null) {

          // disable automatic starting of the RMI Registry
          System.setProperty("no_rmi", "no_rmi");
        }

        // set the hostname to bind Mulgara to
        Object host =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.SERVER_HOST);

        if (host != null) {

          this.setHost( (String) host);
          setBoundHostname((String) host);
        }
        else {

          // get the hostname from configuration file
          String configHost = this.getConfig().getMulgaraHost();

          if ( (configHost == null) || configHost.equals("")) {

            // obtain the host name
            configHost = getResolvedLocalHost();

          } // end if

          // set the host name
          this.setHost(configHost);
          setBoundHostname(configHost);
        }

        // set the port on which the RMI registry will be created
        Object rmiPort =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.RMI_PORT);

        if (rmiPort != null) {

          EmbeddedMulgaraServer.setRMIPort(Integer.parseInt((String) rmiPort));
        }
        else {

          EmbeddedMulgaraServer.setRMIPort(this.getConfig().getRMIPort());
        }

        configureSystemProperties();

        // load an external logging configuration
        Object loggingConfig =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.LOG_CONFIG);

        if (loggingConfig != null) {

          EmbeddedMulgaraServer.loadLoggingConfig(new URL( (String)
              loggingConfig));
        }
        else {

          EmbeddedMulgaraServer.loadLoggingConfig(config.getExternalConfigPaths()
              .getMulgaraLogging());
        } // end if

        EmbeddedMulgaraServer.setBoundHostname(this.getHost());

        Object httpHost =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.HTTP_HOST);

        if (httpHost != null) {

          this.setHttpHost( (String) httpHost);
        }
        else {

          // use the hostname from configuration file
          this.setHttpHost( ( (Listener)this.getConfig().getJetty().getListener()).
              getHost());
        }

        // end if
        // set the port on which to accept HTTP requests
        Object httpPort =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.PORT);

        if (httpPort != null) {

          this.setHttpPort(Integer.parseInt( (String) httpPort));
        }
        else {

          // use the port from configuration file
          this.setHttpPort( ( (Listener)this.getConfig().getJetty().getListener()).
              getPort());
        }

        // end if
        // set the (RMI) name of the server
        Object serverName =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.SERVER_NAME);

        if (serverName != null) {

          // use the server name specified on the command line
          this.setServerName( (String) serverName);
        }
        else {

          // use the name specified in the configuration file
          this.setServerName(this.getConfig().getServerName());
        }

        // end if
        // set the server's persistence path
        Object persistencePath =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.PERSISTENCE_PATH);

        if (persistencePath == null) {

          // use the persistence path specified in the configuration file
          persistencePath = this.getConfig().getPersistencePath();
        } // end if

        // if the persistence path was one we know about, substitute it
        if ( ( (String) persistencePath).equalsIgnoreCase(".")) {

          persistencePath = System.getProperty("user.dir");
        }
        else if ( ( (String) persistencePath).equalsIgnoreCase("temp")) {

          persistencePath = System.getProperty("java.io.tmpdir");
        } // end if

        // set the persistence path
        this.setPersistencePath( (String) persistencePath);

        // set the smtp name of the server
        Object smtpServer =
            parser.getOptionValue(EmbeddedMulgaraOptionParser.SMTP_SERVER);

        if (smtpServer != null) {

          // use the smtp server name specified on the command line
          this.setSMTP( (String) smtpServer);
        }
        else {

          // use the smtp server name specified in the configuration file
          this.setSMTP(this.getConfig().getSmtp());
        } // end if

        // set the property for mail package to pickup
        System.setProperty("mail.smtp.host", this.getSMTP());
      } // end if
    }
    catch (MalformedURLException mue) {

      // log the error
      log.warn("Invalid URL on command line - " + mue.getMessage());

      // print the usage
      //System.err.println("Invalid URL - " + mue.getMessage());
      printUsage();

      // don't start the server
      startServer = false;
    }
    catch (IOException ioe) {

      // log the error
      log.error(ioe.getMessage(), ioe);

      // print the usage
      //System.err.println("Invalid URL - " + ioe.getMessage());
      printUsage();

      // don't start the server
      startServer = false;
    }
    catch (NumberFormatException nfe) {

      // log the error
      log.warn("Invalid port specified on command line: " + nfe.getMessage());

      // print the usage
      //System.err.println("Invalid port - " + nfe.getMessage());
      printUsage();

      // don't start the server
      startServer = false;
    }
    catch (org.exolab.castor.xml.MarshalException me) {

      // log the error
      log.warn("Castor Marshal Exception: " + me.getMessage(), me);

      // print the usage
      //System.err.println("Invalid configuration - " + me.getMessage());
      printUsage();

      // don't start the server
      startServer = false;
    }
    catch (org.exolab.castor.xml.ValidationException ve) {

      // log the error
      //log.warn("Castor XML validation exception while loading configuration - " + ve.getMessage());
      log.warn("Unable to load configuration - " + ve.getMessage());

      // print the usage
      //System.err.println("Invalid XML in configuration - " + ve.getMessage());
      printUsage();

      // don't start the server
      startServer = false;
    }
    catch (Exception e) {

      // log the error
      log.warn("Could not start embedded Mulgara server", e );

      // let the user know
      //System.err.println("Error - " + e.getMessage());
      //e.printStackTrace();
      // don't start the server
      startServer = false;
    } // try-catch

    // return the server
    return startServer;
  } // configure()

  /**
   * Starts the Mulgara server.
   *
   * @throws IllegalStateException if this method is called before the servers
   *      have been created
   * @throws IOException if the Mulgara server cannot access its state keeping
   *      files
   * @throws NamingException if the Mulgara server cannot communicate with the
   *      RMI registry
   * @throws MultiException if an error ocurrs while starting up the SOAP server
   * @throws SimpleXAResourceException EXCEPTION TO DO
   * @throws Exception EXCEPTION TO DO
   * @throws StoreException EXCEPTION TO DO
   */
  public void startServer() throws IOException, NamingException,
      MultiException, SimpleXAResourceException, StoreException, Exception {

    if (this.getServer() == null) {

      throw new IllegalStateException("Servers must be created before they " +
          "can be started");
    } // end if

    // log that we're starting a Mulgara server
    if (log.isDebugEnabled()) {

      log.debug("Starting server");
    }

    // start the Mulgara server
    this.getServer().init();
    this.getServer().start();
    EmbeddedMulgaraServer.setLocalSessionFactory(this.getSessionFactory());

    // set the server uri.
    EmbeddedMulgaraServer.setServerURI(((AbstractServer) this.getServer()).getURI());

    // log that we're starting a SOAP server
    if (log.isDebugEnabled()) {

      log.debug("Starting HTTP server");
    }
  }


  /**
   * Starts the Shutdown Hook server.
   */
  public void startShutdownHookServer() {
    // shutdown hook
    this.shutdownHook = new ShutdownHook();
    this.shutdownHook.start();
  }


  /**
   * Starts the Http server.
   *
   * @throws IllegalStateException if this method is called before the servers
   *      have been created
   * @throws IOException if the Mulgara server cannot access its state keeping
   *      files
   * @throws NamingException if the Mulgara server cannot communicate with the
   *      RMI registry
   *      Mulgara server node pool
   * @throws MultiException if an error ocurrs while starting up the SOAP server
   * @throws Exception EXCEPTION TO DO
   */
  public void startHttpServer() throws IOException, NamingException,
      MultiException, Exception {

    if (this.getHttpServer() == null) {

      throw new IllegalStateException("HTTP Server must be created before they " +
          "can be started");
    }

    // log that we're starting a SOAP server
    if (log.isDebugEnabled()) {

      log.debug("Starting HTTP server");
    }

    // start the Http server
    this.getHttpServer().start();

    // install a shutdown hook for a socket request
    if (log.isDebugEnabled()) {
      log.debug("Starting shutdown Hook server");
    }

  } // startHttpServer()

  /**
   * Creates a Mulgara server.
   *
   * @param serverName the RMI binding name of the server
   * @param statePath the path to the directory containing server state
   * @param hostname the hostname to bind the Mulgara server to
   * @param providerClassName  class name of a
   *                           {@link org.mulgara.server.Session} implementation
   * @param serverClassName    class name of a
   *                           {@link org.mulgara.server.ServerMBean}
   * @return a Mulgara server
   * @throws ClassNotFoundException if <var>serverClassName</var> isn't in the
   *      classpath
   * @throws IOException if the <var>statePath</var> is invalid
   */
  public ServerMBean createServer(String serverName,
      File statePath, String hostname, int portNumber, String providerClassName,
      String serverClassName) throws ClassNotFoundException, IOException {

    // log that we're createing a Mulgara server
    if (log.isDebugEnabled()) {

      log.debug("Creating server instance at rmi://" + hostname + "/" +
          serverName + " in directory " + statePath);
    }

    // end if
    // Create the server
    ServerMBean server =
        (ServerMBean) Beans.instantiate(getClass().getClassLoader(),
        serverClassName);

    // Set ServerMBean properties
    server.setDir(statePath);
    File tempDir = new File(statePath,"temp");
    server.setTempDir(tempDir);
    server.setConfig(getConfig());


    if (log.isDebugEnabled()) {

       log.debug("Set config to be: " + getConfig());
     }

    // set the directory that all temporary files will be created in.
    tempDir.mkdirs();
    TempDir.setTempDir(tempDir);
    
    // remove any temporary files 
    cleanUpTemporaryFiles();

    server.setProviderClassName(providerClassName);

    // Check to see if the port number is not 1099 and we're using the RMI
    // server.
    if ((portNumber != 1099) &&
        (serverClassName.equals("org.mulgara.server.rmi.RmiServer"))) {
      server.setPortNumber(portNumber);
    }
    server.setHostname(hostname);

    // Set protocol-specific properties (FIXME: hardcoded to do "name" only)
    try {

      Method setter =
          (new PropertyDescriptor("name", server.getClass())).getWriteMethod();

      try {

        setter.invoke(server, new Object[] {
            serverName});
      }
      catch (InvocationTargetException e) {

        log.warn(server + " doesn't have a name property", e);
      }
    }
    catch (IllegalAccessException e) {

      log.warn(serverClassName + " doesn't have a public name property", e);
    }
    catch (IntrospectionException e) {

      log.warn(serverClassName + " doesn't have a name property", e);
    }

    // return the newly created server instance
    return server;
  } // createServer()

  /**
   * Creates a HTTP  server.
   *
   * @return a HTTP server
   * @throws IOException if the server configuration cannot be found
   * @throws SAXException if the HTTP server configuration file is invalid
   * @throws ClassNotFoundException if the HTTP server configuration file
   *      contains a reference to an unkown class
   * @throws NoSuchMethodException if the HTTP server configuration file
   *      contains a reference to an unkown method
   * @throws InvocationTargetException if an error ocurrs while trying to
   *      configure the HTTP server
   * @throws IllegalAccessException EXCEPTION TO DO
   */
  public Server createHttpServer() throws IOException,
      SAXException, ClassNotFoundException, NoSuchMethodException,
      InvocationTargetException, IllegalAccessException {

    // log that we're creating a HTTP server
    if (log.isDebugEnabled()) {
      log.debug("Creating HTTP server instance");
    }

    // TODO remove
    //org.mortbay.util.Code.setDebug(true);

    // Set the magic logging property for Jetty to use Log4j
    System.setProperty("LOG_CLASSES", "org.mortbay.util.log4j.Log4jSink");

    // create a new server
    setHttpServer(new Server());
    Server httpServer = getHttpServer();

    // add a listener to the server
    this.addListener(httpServer);

    // get the URL to the default webapp configuration
    URL webappDefaultURL =
        ClassLoader.getSystemResource(this.getConfig().getExternalConfigPaths()
        .getWebDefault());

    if (webappDefaultURL == null) {

      throw new IOException("Could not find default webapp configuration");
    }

    // end if


    // add the static HTML contexts
    this.addDefaultContext();
    this.addDocsContext();
    this.addDataContext();


    // add the webapps
    this.addWebServicesWebAppContext();
    this.addWebUIWebAppContext();

    // add our class loader as the classloader of all contexts
    HttpContext contexts[] = httpServer.getContexts();
    for (int i = 0; i < contexts.length; i++) {
    //  System.out.println("Setting classload on: " + contexts[i].toString(true));
      //contexts[i].setClassLoader(this.getClass().getClassLoader());
      contexts[i].setParentClassLoader(this.getClass().getClassLoader());
    }

    // return the server
    return httpServer;
  }

  // createHttpServer()

  /**
   * Adds a listener to the <code>httpServer</code>.
   *
   * @param httpServer the server to add the listener to
   * @throws UnknownHostException if an invalid hostname was specified in the
   *      Mulgara server configuration
   */
  private void addListener(Server httpServer) throws UnknownHostException {

    // validate httpServer parameter
    if (httpServer == null) {

      throw new IllegalArgumentException("Null \"httpServer\" parameter");
    }

    // end if
    // log that we're adding a listener
    log.debug("Adding socket listener");

    // create a listener
    SocketListener listener = new SocketListener();

    // configure the listener
    if ( (this.getHttpHost() != null) && !this.getHttpHost().equals("")) {

      listener.setHost(this.getHttpHost());
      log.debug("Servlet container listening on host " + this.getHttpHost());
    }
    else {

      this.setHttpHost(this.getResolvedLocalHost());
      log.debug("Servlet container listening on all host interfaces");
    }

    // end if
    // get the jetty configuration
    Listener jettyConfig = (Listener)this.getConfig().getJetty().getListener();

    listener.setPort(this.getHttpPort());
    listener.setMinThreads(jettyConfig.getMinThreads());
    listener.setMaxThreads(jettyConfig.getMaxThreads());
    listener.setMaxIdleTimeMs(jettyConfig.getMaxIdleTimeMs());
    // listener.setMaxReadTimeMs(jettyConfig.getMaxReadTimeMs());
    listener.setLowResourcePersistTimeMs(jettyConfig.
        getLowResourcePersistTimeMs());

    // add it to the server
    httpServer.addListener(listener);
  } // addListener()


  /**
   * Adds a default context to the <code>httpServer</code>. <p>
   *
   * This adds the index page that references other contexts. </p>
   *
   * @throws IOException if a URL to the embedded document directory cannot be
   *      created
   */
  private void addDefaultContext() throws IOException {

    // log that we're adding the default context
    log.debug("Adding default context");

    // create a static file handler for the documentation
    ResourceHandler defaultHandler = new ResourceHandler();
    defaultHandler.setDirAllowed(true);

    // create a documentation context
    HttpContext fileContext = getHttpServer().addContext("/");
    fileContext.setBaseResource(Resource.newResource(
        ClassLoader.getSystemResource(DOCS_PATH)));
    fileContext.addHandler(defaultHandler);
    fileContext.addHandler(new NotFoundHandler());
    //fileContext.setServingResources(true);
  } // addDefaultContext()


  /**
   * Adds a documentation context to the <code>httpServer</code>.
   *
   * @throws IOException if a URL to the embedded document directory cannot be
   *      created
   */
  private void addDocsContext() throws IOException {

    // log that we're adding the documentation context
    log.debug("Adding documentation context");

    // create a static file handler for the documentation
    ResourceHandler docsHandler = new ResourceHandler();
    docsHandler.setDirAllowed(true);

    // create a documentation context
    HttpContext fileContext = getHttpServer().addContext("/docs/*");
    fileContext.setBaseResource(Resource.newResource(
        ClassLoader.getSystemResource(DOCS_PATH)));
    fileContext.addHandler(docsHandler);
    fileContext.addHandler(new NotFoundHandler());
    //fileContext.setServingResources(true);
    fileContext.addWelcomeFile("index.html");
  }

  // addDocsContext()

  /**
   * Adds a sample data context to the <code>httpServer</code>.
   *
   * @throws IOException if a URL to the embedded sample data directory cannot
   *      be created
   */
  private void addDataContext() throws IOException {

    // log that we're adding the documentation context
    log.debug("Adding sample data context");

    // create a static file handler for the documentation
    ResourceHandler dataHandler = new ResourceHandler();
    dataHandler.setDirAllowed(true);

    // create a documentation context
    HttpContext fileContext = getHttpServer().addContext("/data/*");
    fileContext.setBaseResource(Resource.newResource(
        ClassLoader.getSystemResource(DATA_PATH)));
    fileContext.addHandler(dataHandler);
    fileContext.addHandler(new NotFoundHandler());
    //fileContext.setServingResources(true);
  }

  // addDataContext()

  /**
   * Creates the Mulgara Descriptor UI
   *
   * @throws IOException if the driver WAR file could not be found
   */
  private void addWebServicesWebAppContext() throws IOException {

    // Create a servlet handler for web services
    WebApplicationHandler descriptorServletHandler = new WebApplicationHandler();

    // get the URL to the test WAR file
    URL webServicesWebAppURL =
        ClassLoader.getSystemResource(WEBAPP_PATH + "/" + WEBSERVICES_WEBAPP);

    if (webServicesWebAppURL == null) {

      log.warn("Couldn't find resource: " + WEBAPP_PATH +
          "/" + WEBSERVICES_WEBAPP);
      return;
    }

    // Get the path to the War file
    String tempWebServicesWebAppFile = webServicesWebAppURL.toString();

    // Adds Descriptors and Axis
    WebApplicationContext descriptorWARContext =
        getHttpServer().addWebApplication(
        null, // virtual host
        "/" + WEBSERVICES_PATH + "/*",
        tempWebServicesWebAppFile);

    // make some attributes available
    descriptorWARContext.setAttribute(BOUND_HOST_NAME_KEY, getBoundHostname());
    descriptorWARContext.setAttribute(SERVER_MODEL_URI_KEY, getServerURI().toString());

    // add the handler for the servlets
    descriptorWARContext.addHandler(descriptorServletHandler);

    // log that we're adding the test webapp context
    log.debug("Adding Web Services webapp context");
  }


  /**
   * Creates the Mulgara Semantic Store Query Tool (webui).
   *
   * @throws IOException if the driver WAR file could not be found
   */
  private void addWebUIWebAppContext() throws IOException {

    // log that we're adding the WebUI webapp context
    log.debug("Adding WebUI webapp context");

    // get the URL to the WebUI WAR file
    URL webUIWebAppURL =
        ClassLoader.getSystemResource(WEBAPP_PATH + "/" + WEBUI_WEBAPP);

    // load the webapp if the WAR file exists
    if (webUIWebAppURL != null) {

      // dump the embedded war file into the temp directory
      String webUITestWebAppFile = webUIWebAppURL.toString();

      // create the test webapp handler context
      WebApplicationContext webUIWARContext =
          getHttpServer().addWebApplication(
            null, "/" + WEBUI_PATH + "/*",
            webUITestWebAppFile);

      //webUIWARContext.setClassLoader(this.getClass().getClassLoader());
      //webUIWARContext.setParentClassLoader(URLClassLoader.newInstance(new URL[]{}));
      webUIWARContext.setParentClassLoader(this.getClass().getClassLoader());

      /*
      WebApplicationContext webUIWARContext =
          httpServer.addWebApplication("/webui/*", webUITestWebAppFile,
          defaultWebAppConf.toString(), true);
          */
    }
    else {

      // log that we couldn't find the webapp
      log.warn("Could not find WebUI webapp WAR file -> not adding to " +
          "servlet container");
    }

    // end if
  }

  // addWebUIWebAppContext()


  /**
   * Prints the usage instructions for starting the server.
   */
  public void printUsage() {

    // build the usage message
    StringBuffer usage = new StringBuffer();
    usage.append("Usage: java -jar <jarfile> ");
    usage.append("[-h] ");
    usage.append("[-n] ");
    usage.append("[-x] ");
    usage.append("[-l <url>] ");
    usage.append("[-c <path>] ");
    usage.append("[-k <hostname>] ");
    usage.append("[-o <hostname>] ");
    usage.append("[-p <port>] ");
    usage.append("[-r <port>] ");
    usage.append("[-s <servername>] ");
    usage.append("[-a <path>] ");
    usage.append("[-m <smtp>]" + eol);
    usage.append("" + eol);
    usage.append("-h, --help          display this help screen" + eol);
    usage.append("-n, --normi         disable automatic starting of the RMI " +
        "registry" + eol);
    usage.append("-x, --shutdown      shutdown the local running server" + eol);
    usage.append("-l, --logconfig     use an external logging " +
        "configuration file" + eol);
    usage.append("-c, --serverconfig  use an external server " +
        "configuration file" + eol);
    usage.append("-k, --serverhost    the hostname to bind the server to" + eol);
    usage.append("-o, --httphost      the hostname for HTTP requests" + eol);
    usage.append("-p, --port          the port for HTTP requests" + eol);
    usage.append("-r, --rmiport       the RMI registry port" + eol);
    usage.append("-s, --servername    the (RMI) name of the server" + eol);
    usage.append("-a, --path          the path server data will persist " +
        "to, specifying " + eol +
        "                    '.' or 'temp' will use the current working " +
        "directory " + eol +
        "                    or the system temporary directory respectively" +
        eol);
    usage.append("-m, --smtp          the SMTP server for email notifications" +
        eol);
    usage.append(eol);

    usage.append(
        "Note 1. A server can be started without any options, all " +
        "options" + eol + "override default settings." + eol + eol);
    usage.append("Note 2. If an external configuration file is used, and " +
        "other options" + eol + "are specified, the other options will take " +
        "precedence over any settings" + eol + "specified in the " +
        "configuration file." + eol + eol);

    // print the usage
    System.out.println(usage.toString());
  }

  // copyFileToTemp()

  /**
   * Launch a browser if specified by a 'browser.url/exe' system property.
   *
   * If 'auto' is supplied then the system will automaticallly open to the
   * default Mulgara home page based on the Mulgara configuration settings.
   */
  public void launchBrowser() {

    // grab the system properties
    String url = System.getProperty("browser.url");

    // Check for a request to launch a browser
    if (url == null) {

      return;
    }

    // Automatically configure url for the Mulgara home page.
    if (url.equalsIgnoreCase("auto")) {

      url = "http://" + this.getHttpHost() + ":" + this.getHttpPort() + "/";
    } else {

      // check if the url is relative
      if ( url.indexOf("http://") == -1 ) {
        // relative url requires a local host name prefix
        url = "http://"+this.getHttpHost()+":"+this.getHttpPort()+"/"+url ;
      }
    }

    // Used to identify the windows platform.
    String win_id = "Windows";

    // Used to identify the Mac OS X platform
    String macosx_id = "Mac OS X";

    // The default system browser under windows.
    String win_path = "rundll32";

    // The flag to display a url.
    String win_flag = "url.dll,FileProtocolHandler";

    // The default browser under unix.
    String unix_path = "netscape";
    String mozilla_path = "mozilla";

    // The flag to display a url.
    String unix_flag = "-remote openURL";

    // the command to be executed
    String cmd = null;

    String os = System.getProperty("os.name");
    boolean windows = ((os != null) && os.startsWith(win_id));
    boolean macosx = ((os != null) && os.startsWith(macosx_id));

    Process p = null;

    try {

      if (windows) {

        // cmd = 'rundll32 url.dll,FileProtocolHandler http://...'
        cmd = win_path + " " + win_flag + " " + url;
        p = Runtime.getRuntime().exec(cmd);
      }
      else if (macosx) {

        // cmd = 'open http://...'
        cmd = "open " + url;
        p = Runtime.getRuntime().exec(cmd);
      }
      else {

        // Under Unix, Netscape has to be running for the "-remote"
        // command to work.  So, we try sending the command and
        // check for an exit value.  If the exit command is 0,
        // it worked, otherwise we need to start the browser.
        // cmd = 'netscape -remote openURL(http://www.javaworld.com)'
        cmd = unix_path + " " + unix_flag + "(" + url + ")";

        try {

          p = Runtime.getRuntime().exec(cmd);

          // wait for exit code -- if it's 0, command worked,
          // otherwise we need to start the browser up.
          int exitCode = p.waitFor();

          if (exitCode != 0) {

            // Command failed, start up the browser
            // cmd = 'netscape http://www.javaworld.com'
            cmd = unix_path + " " + url;
            p = Runtime.getRuntime().exec(cmd);
          }
        }
        catch (Exception ex) {

          log.info("Unable open browser to URL " + url + " caused by " +
              ex.getMessage() + ". Trying Mozilla");

          // try mozilla
          cmd = mozilla_path + " " + unix_flag + "(" + url + ")";
          p = Runtime.getRuntime().exec(cmd);

          try {
            int exitCode = p.waitFor();
            if (exitCode != 0) {
              cmd = mozilla_path + " "  + url;
              p = Runtime.getRuntime().exec(cmd);
            }
          } catch(Exception ex2) {
           log.error( "Unable open browser to URL "+url +
                      " caused by "+ ex2.getMessage());
          }


        }
      }
    }
    catch (IOException ex) {

      log.error("Unable open browser to URL " + url + " caused by " +
          ex.getMessage());
    }
  }

  /**
   * Clean up any temporary files and directories. In some instances the VM does
   * not remove temporary files and directories. ie Windows JVM Some files maybe
   * left due to file locking, however they will be deleted on the next clean
   * up.
   *
   */
  private void cleanUpTemporaryFiles() {

    // only perform this clean up on windows only.
    /*
    String os = System.getProperty("os.name");
    boolean windows = ( (os != null) && os.startsWith("Windows"));

    // NOT running on windows then return
    if (!windows) {

      return;
    }
    */
    
    File tempDirectory = TempDir.getTempDir();

    //Add a filter to ensure we only delete the correct files
    File[] list = tempDirectory.listFiles(new TemporaryFileNameFilter());

    //nothing to be removed
    if (list == null) {

      return;
    }

    // Remove the top level files and
    // recursively remove all files in each directory.
    for (int i = 0; i < list.length; i++) {

      if (list[i].isDirectory()) {

        this.removeDirectory(list[i]);
      }

      list[i].delete();
    }
  }

  /**
   * Remove the contents of a directory.
   *
   * @param directory PARAMETER TO DO
   */
  private void removeDirectory(File directory) {

    File[] list = directory.listFiles();

    if (list == null) {
      log.error("Unable to remove directory: \"" + directory + "\"");
      return;
    }

    for (int i = 0; i < list.length; i++) {

      if (list[i].isDirectory()) {

        this.removeDirectory(list[i]);
      }

      list[i].delete();
    }
  }

  /**
   * Filter class for detecting temporary files created by Mulgara
   */
  private class TemporaryFileNameFilter implements java.io.FilenameFilter {

    public boolean accept(File dir, String name) {

      // check for files and directories with
      // mulgara*.jar , Jetty-*.war and JettyContext*.tmp
      return ( ( (name.indexOf("mulgara") == 0) && (name.indexOf(".jar") > 0)) ||
          ( (name.indexOf("Jetty-") == 0) && (name.indexOf(".war") > 0)) ||
          ( (name.indexOf("JettyContext") == 0) &&
          (name.indexOf(".tmp") > 0)));
    }
  }

  /**
   * A server side shutdown hook to allow the BootStrap class to force a
   * shutdown from another JVM while on the same machine only. To override the
   * default port of 6789 set a system property called shutdownhook.port
   */
  private class ShutdownHook extends Thread {

    private ServerSocket shutdownSocket;

    public void ShutdownHook() {

      // register a thread name
      this.setName("Server side shutdown hook");
      this.setDaemon(true);
    }

    public void run() {

      boolean stop = false;

      // get the current shutdownhook port
      int port = EmbeddedMulgaraServer.getShutdownHookPort();

      // bind to the specified socket on the local host
      try {

        ServerSocket shutdownSocket =
            new ServerSocket(port, 0, InetAddress.getByName("localhost"));

        // wait until a request to stop the server
        while (!stop) {

          //wait for a shutdown request
          Socket socket = shutdownSocket.accept();

          // read the response from the client
          BufferedReader input =
              new BufferedReader(new InputStreamReader(socket.getInputStream()));

          // check if the require request is correct
          String message = input.readLine();

          if ( (message != null) &&
              message.equals(EmbeddedMulgaraServer.SHUTDOWN_MSG)) {

            socket.close();
            stop = true;
          }
          else {

            socket.close();

            if (message != null) {

              log.error("Incorrect request to shutdown mulgara");
            }
          }
        } // while
      }
      catch (IOException ioEx) {

        log.error("Unable to establish shutdown socket due to an I/O " +
            "exception on port " + port, ioEx);
      }
      catch (SecurityException secEx) {

        log.error("Unable to establish shutdown socket " +
            "due to a security exception. Check security policy", secEx);
      }
      catch (Exception ex) {

        log.error("Unable to establish shutdown socket on port " + port, ex);
      }
      finally {

        // attempt to close the socket
        try {

          shutdownSocket.close();
        }
        catch (Exception ex) {

          /* skip */
        }
      }

      // log that we're sutting down the servers
      if (log.isInfoEnabled()) {

        log.info("Shutting down server");
      }
      else {

        // regardless of the log level output this to stdout.
        // Note. "\n" Will give us a new line beneath a Ctrl-C
        System.out.println("\nShutting down server");
      }

      
      // finally
      // issue the shutdown
      if (stop) {

        System.exit(0);
      }
    }

    // run
  }
}
