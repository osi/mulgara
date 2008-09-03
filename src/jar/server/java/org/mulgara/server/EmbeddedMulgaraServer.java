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
import java.util.*;

import javax.naming.*;
import javax.servlet.Servlet;
import javax.xml.parsers.*;
import org.xml.sax.SAXException;

// log4j packages
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;

// locally written packages
import org.mulgara.config.MulgaraConfig;
import org.mulgara.config.Connector;
import org.mulgara.server.SessionFactory;
import org.mulgara.store.StoreException;
import org.mulgara.store.xa.SimpleXAResourceException;
import org.mulgara.util.MortbayLogger;
import org.mulgara.util.Reflect;
import org.mulgara.util.TempDir;

import static org.mulgara.server.ServerMBean.ServerState;
import static org.mortbay.jetty.servlet.Context.SESSIONS;

// jetty packages
import org.mortbay.jetty.AbstractConnector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.nio.BlockingChannelConnector;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.webapp.WebAppClassLoader;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.MultiException;

/**
 * Embedded production Mulgara server.
 *
 * <p> Creates a Mulgara server instance, and a SOAP server instance to handle
 * <a href="http://www.w3.org/TR/SOAP">SOAP</a> requests for the Mulgara server.</p>
 *
 * @created 2001-10-04
 *
 * @author Tom Adams
 * @author Simon Raboczi
 * @author Paul Gearon
 * @author Tate Jones
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 * @see <a href="http://developer.java.sun.com/developer/JDCTechTips/2001/tt0327.html#jndi">
 *      <cite>JNDI lookup in distributed systems</cite> </a>
 */
public class EmbeddedMulgaraServer {

  /** Line separator. */
  protected static final String eol = System.getProperty("line.separator");

  /** Default port to listen for a shutdown. */
  public final static int SHUTDOWN_PORT = 6789;

  /** System property for the shutdown port. */
  public final static String SHUTDOWN_PROP = "shutdownhook.port";

  /** The request required to shutdown mulgara. */
  public final static String SHUTDOWN_MSG = "shutdownmulgara";

  /** The key to the bound host name in the attribute map of the servlet context. */
  public final static String BOUND_HOST_NAME_KEY = "boundHostname";

  /** Key to the bound server model uri in the attribute map of the servlet context. */
  public final static String SERVER_MODEL_URI_KEY = "serverModelURI";

  /** The web application file path. */
  private final static String WEBAPP_PATH = "webapps";

  /** The Web Services web application file. */
  private final static String WEBSERVICES_WEBAPP = "webservices.war";

  /** The Web Services path. */
  private final static String WEBSERVICES_PATH = "webservices";

  /** The Web UI web application file. */
  private final static String WEBUI_WEBAPP = "webui.war";

  /** The Web UI path. */
  private final static String WEBUI_PATH = "webui";

  /** The Web Query path. */
  private final static String WEBQUERY_PATH = "webquery";

  /** The logging category to log to. */
  protected static Logger log = Logger.getLogger(EmbeddedMulgaraServer.class.getName());

  /** The embedded configuration file path */
  protected static String CONFIG_PATH = "conf/mulgara-x-config.xml";

  /** The RMI permission security policy file path. */
  protected static String RMI_SECURITY_POLICY_PATH = "conf/mulgara-rmi.policy";

  /** The property for identifying the policy type. */
  private static final String SYSTEM_MAIL = "mail.smtp.host";

  /** The default server class to use for the Mulgara server. */
  private static final String DEFAULT_SERVER_CLASS_NAME = "org.mulgara.server.rmi.RmiServer";

  /** The registry context factory class. */
  private static final String CONTEXT_FACTORY = "com.sun.jndi.rmi.registry.RegistryContextFactory";

  /** The property for identifying the policy type. */
  private static final String SECURITY_POLICY_PROP = "java.security.policy";

  /** The system property to disable the HTTP service. */
  private static final String DISABLE_HTTP = "mulgara.http.disable";

  /** The system property to disable the RMI service. */
  private static final String DISABLE_RMI = "no_rmi";

  /** The maximum number of acceptors that Jetty can handle. It locks above this number. */
  private static final int WEIRD_JETTY_THREAD_LIMIT = 24;

  /** The Mulgara server instance. In this case, an RMIServer. */
  private ServerMBean serverManagement = null;

  /** The HTTP server instance. */
  private Server httpServer = null;

  /** The embedded Mulgara server configuration */
  private MulgaraConfig mulgaraConfig = null;

  /** The (RMI) name of the server. */
  private String rmiServerName = null;

  /** The path to persist server data to. */
  private String persistencePath = null;

  /** The hostname to accept SOAP requests on. */
  private String httpHostName = null;

  /** A flag to indicate if the server is configured to be started. */
  private boolean canStart = false;

  /**
   * Starts a Mulgara server and a WebServices (SOAP) server to handle SOAP queries to the
   * Mulgara server.
   * <p>Database files for the Mulgara server are written to the directory from
   * where this class was run.</p>
   * @param args command line arguments
   */
  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    // report the version and build number
    System.out.println("@@build.label@@");
  
    // Set up the configuration, using command line arguments to override configured options
    EmbeddedMulgaraOptionParser optsParser = new EmbeddedMulgaraOptionParser(args);

    // load up the basic logging configuration in case we get an error before
    // we've loaded the real logging configuration
    BasicConfigurator.configure();

    try {
      // parse the command line options to the server
      optsParser.parse();

    } catch (EmbeddedMulgaraOptionParser.UnknownOptionException uoe) {
      System.err.println("ERROR: Unknown option(s): " + uoe.getOptionName());
      printUsage();
      System.exit(3);
    } catch (EmbeddedMulgaraOptionParser.IllegalOptionValueException iove) {
      System.err.println("ERROR: Illegal value '" + iove.getValue() +
          "' for option " + iove.getOption().shortForm() + "/" + iove.getOption().longForm());
      printUsage();
      System.exit(4);
    }

    try {
      // TODO: Iterate over all configured servers and start each one
      // Create the server instance
      EmbeddedMulgaraServer standAloneServer = new EmbeddedMulgaraServer(optsParser);
    
      if (standAloneServer.isStartable()) {
        // start the server, including all the configured services
        standAloneServer.startServices();

        // Setup the network service for shutting down the server
        ShutdownService shutdownServer = new ShutdownService();
        shutdownServer.start();
      }
  
    } catch (MultiException me) {
      for (Throwable e: (List<Throwable>)me.getThrowables()) {
        log.error("MultiException", e);
        e.printStackTrace();
      }
      System.exit(2);
    } catch (Exception e) {
      log.error("Exception in main", e);
      e.printStackTrace();
      System.exit(5);
    }

}


  /**
   * Shutdown the Mulgara server
   */
  public static void shutdown(String[] args) {
    // create a basic Configurator for the shutdown
    BasicConfigurator.configure();

    // get the socket port
    int port = getShutdownHookPort();

    // create a socket to the local host and port
    Socket clientSocket = null;
    try {
      clientSocket = new Socket(InetAddress.getByName("localhost"), port);
      PrintWriter toServer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
      toServer.println(EmbeddedMulgaraServer.SHUTDOWN_MSG);
      toServer.flush();
      toServer.close();
    } catch (ConnectException ioCon) {
      System.out.println("Server is not currently running");
    } catch (IOException ioEx) {
      log.error("Unable to establish connection to shutdown server on port " + port, ioEx);
    } catch (SecurityException secEx) {
      log.error("Unable to establish connection shutdown server due to a security exception. Check security policy", secEx);
    } catch (Exception ex) {
      log.error("Unable to establish shutdown connection to shutdown server on port " + port, ex);
    } finally {
      // attempt to close the socket
      try {
        clientSocket.close();
      } catch (Exception ex) {
        /* skip */
      }
    }
  }


  /**
   * Loads the embedded logging configuration (from the JAR file).
   * @param loggingConfig the path to the logging configuration file
   */
  private static void loadLoggingConfig(String loggingConfig) {
    // get a URL from the classloader for the logging configuration
    URL log4jConfigURL = ClassLoader.getSystemResource(loggingConfig);

    // if we didn't get a URL, tell the user that something went wrong
    if (log4jConfigURL == null) {
      System.err.println("Unable to find logging configuration file in JAR " +
            "with " + loggingConfig + ", reverting to default configuration.");
      BasicConfigurator.configure();
    } else {
      try {
        // configure the logging service
        DOMConfigurator.configure(log4jConfigURL);
        if (log.isDebugEnabled()) log.debug("Using logging configuration from " + log4jConfigURL);
      } catch (FactoryConfigurationError e) {
        System.err.println("Unable to configure logging service, reverting to default configuration");
        BasicConfigurator.configure();
      } catch (Exception e) {
        System.err.println("Unable to configure logging service, reverting to default configuration");
        BasicConfigurator.configure();
      }
    }
  }


  /**
   * Loads the embedded logging configuration from an external URL.
   * @param loggingConfig the URL of the logging configuration file
   */
  private static void loadLoggingConfig(URL loggingConfig) {
    if (loggingConfig == null) throw new IllegalArgumentException("Null \"loggingConfig\" parameter");

    try {
      // configure the logging service
      DOMConfigurator.configure(loggingConfig);
      if (log.isDebugEnabled()) log.debug("Using logging configuration from " + loggingConfig);
    } catch (FactoryConfigurationError e) {
      System.err.println("Unable to configure logging service, reverting to default configuration");
      BasicConfigurator.configure();
    } catch (Exception e) {
      System.err.println("Unable to configure logging service, reverting to default configuration");
      BasicConfigurator.configure();
    }
  }


  /**
   * Attempts to obtain the localhost name or defaults to the IP address of the localhost
   * @return the hostname this Mulgara server is bound to.
   */
  public static String getResolvedLocalHost() {
    String hostname = null;

    try {
      // attempt for the localhost canonical host name
      hostname = InetAddress.getLocalHost().getCanonicalHostName();
    } catch (UnknownHostException uhe) {
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


  /**
   * Get the shutdown hook port to allow the BootStrap to shutdown the server
   * from the same machine but different JVM To override the default port of
   * 6789 set a system property called shutdownhook.port
   * @return the shutdown port for this server
   */
  private static int getShutdownHookPort() {
    int port = EmbeddedMulgaraServer.SHUTDOWN_PORT;

    // check if the default shutdown port has been overrided by a system property.
    String portString = System.getProperty(SHUTDOWN_PROP);
    if ((portString != null) && (portString.length() > 0)) {
      try {
        port = Integer.parseInt(portString);
        if (log.isInfoEnabled()) log.info("Override default shutdown hook port to " + port);
      } catch (NumberFormatException ex) {
        log.error("Unable to convert supplied port " + portString + " to int " +
            " for shutdown hook. Defaulting to port :" + portString, ex);
      }
    }

    return port;
  }


  ///////////////////////////////////////////////////////////////
  // Member methods
  ///////////////////////////////////////////////////////////////

  EmbeddedMulgaraServer(EmbeddedMulgaraOptionParser options) throws IOException, ClassNotFoundException,
            SAXException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    // TODO: Attach ServerInfo to all databases, so it can be instantiated per server
    // configure the server, and set up the global ServerInfo
    canStart = configure(options);

    // start the server if we're allowed to
    if (canStart) {
      // create the params need for a new Mulgara instance
      File statePath = new File(new File(getPersistencePath()), getServerName());
      String tripleStoreClassName = getConfig().getTripleStoreImplementation();
      String hostname = ServerInfo.getBoundHostname();
      int rmiPort = ServerInfo.getRMIPort();

      //set the tripleStoreImplemention property
      System.setProperty("triple.store.implementation", tripleStoreClassName);

      // create a Mulgara server instance
      serverManagement = createServer(
          getServerName(), statePath, hostname,
          rmiPort, tripleStoreClassName, DEFAULT_SERVER_CLASS_NAME
      );

      // install a shutdown hook for System.exit(#);
      if (log.isDebugEnabled()) log.debug("Registering shutdown hook");
      Runtime.getRuntime().addShutdownHook(new RuntimeShutdownHook(this));

      // create a web service
      if (System.getProperty(DISABLE_HTTP) == null && !mulgaraConfig.getJetty().getDisabled()) {
        // create a HTTP server instance
        if (log.isDebugEnabled()) log.debug("Configuring HTTP server");
        httpServer = createHttpServer();
      }
    }
  }


  /**
   * Starts the Mulgara server.
   * @throws IllegalStateException if this method is called before the servers have been created
   * @throws IOException if the Mulgara server cannot access its state keeping files
   * @throws NamingException if the Mulgara server cannot communicate with the RMI registry
   * @throws MultiException if an error ocurrs while starting up the SOAP server
   * @throws SimpleXAResourceException If operations on the database cannot be instigated
   * @throws Exception General catch all exception
   * @throws StoreException if the database could not be started
   */
  public void startServices() throws IOException, NamingException,
      MultiException, SimpleXAResourceException, StoreException, Exception {
  
    if (serverManagement == null) throw new IllegalStateException("Servers must be created before they can be started");
  
    // log that we're starting a Mulgara server
    if (log.isDebugEnabled()) log.debug("Starting server");
  
    // start the Mulgara server
    serverManagement.init();
    serverManagement.start();

    // get the configured factory and URI and set the ServerInfo for this Mulgara server
    ServerInfo.setLocalSessionFactory(((AbstractServer)serverManagement).getSessionFactory());
  
    // start the HTTP server if required
    if (httpServer != null) {
      if (log.isDebugEnabled()) log.debug("Starting HTTP server");
      httpServer.start();
    }
  }


  /**
   * Returns the flag that indicates if the server was configured to be started.
   * @return <code>true</code> if the server is configured to be started.
   */
  private boolean isStartable() {
    return canStart;
  }


  /**
   * Returns a reference to the local {@link org.mulgara.server.SessionFactory} of
   * the underlying database.
   * @return a {@link org.mulgara.server.SessionFactory} from the underlying database
   */
  public SessionFactory getSessionFactory() {
    SessionFactory sessionFactory = null;
    if (serverManagement != null) sessionFactory = ((AbstractServer)serverManagement).getSessionFactory();
    return sessionFactory;
  }


  /**
   * Returns the Mulgara server instance.
   * @return the Mulgara server instance
   */
  private ServerMBean getServerMBean() {
    return serverManagement;
  }


  /**
   * Returns the embedded Mulgara server configuration.
   * @return the embedded Mulgara server configuration
   */
  private MulgaraConfig getConfig() {
    return mulgaraConfig;
  }


  /**
   * Returns the (RMI) name of the server.
   * @return the (RMI) name of the server
   */
  private String getServerName() {
    return rmiServerName;
  }


  /**
   * Returns the path to persist server data to.
   * @return the path to persist server data to
   */
  private String getPersistencePath() {
    return persistencePath;
  }


  /**
   * Returns the hostname to accept HTTP requests on.
   *
   * @return the hostname to accept HTTP requests on
   */
  private String getHttpHostName() {
    return this.httpHostName;
  }


  /**
   * Returns the SOAP server instance.
   * @return the SOAP server instance
   */
  private Server getHttpServer() {
    return httpServer;
  }


  /**
   * Configures an embedded Mulgara server.
   * @param parser the options parser containing the command line arguments to the server
   * @return true if the server is allowed to start
   */
  private boolean configure(EmbeddedMulgaraOptionParser parser) {
    // flag to indicate whether we can start the server
    boolean startServer = true;

    try {
      // find out if the user wants help
      if (parser.getOptionValue(EmbeddedMulgaraOptionParser.HELP) != null) {
        // print the help
        printUsage();

        // don't start the server
        startServer = false;
      } else if (parser.getOptionValue(EmbeddedMulgaraOptionParser.SHUTDOWN) != null) {
        // shut down the remote server
        shutdown(new String[0]);

        // don't start the server
        startServer = false;
      } else {
        // load the Mulgara configuration file
        URL configURL = null;
        String configURLStr = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.SERVER_CONFIG);

        if (configURLStr == null) {
          // get a URL to the default server configuration file
          configURL = ClassLoader.getSystemResource(CONFIG_PATH);
          if (configURL == null) throw new IOException("Unable to locate embedded server configuration file");
        } else {
          configURL = new URL(configURLStr);
        }

        // configure the server
        mulgaraConfig = MulgaraConfig.unmarshal(new InputStreamReader(configURL.openStream()));
        mulgaraConfig.validate();

        // disable automatic starting of the RMI registry
        if (parser.getOptionValue(EmbeddedMulgaraOptionParser.NO_RMI) != null) {
          // disable automatic starting of the RMI Registry
          System.setProperty(DISABLE_RMI, DISABLE_RMI);
        }

        // disable automatic starting of the HTTP server
        if (parser.getOptionValue(EmbeddedMulgaraOptionParser.NO_HTTP) != null) {
          System.setProperty(DISABLE_HTTP, DISABLE_HTTP);
        }

        // set the hostname to bind Mulgara to
        String host = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.SERVER_HOST);

        if (host != null) {
          ServerInfo.setBoundHostname(host);
        } else {
          // get the hostname from configuration file
          String configHost = mulgaraConfig.getMulgaraHost();
          // obtain the default host name if none is configured
          if ((configHost == null) || configHost.equals("")) configHost = getResolvedLocalHost();
          // set the host name
          ServerInfo.setBoundHostname(configHost);
        }

        // set the port on which the RMI registry will be created
        String rmiPortStr = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.RMI_PORT);
        int rmiPort = (rmiPortStr != null) ? Integer.parseInt(rmiPortStr) : mulgaraConfig.getRMIPort();
        ServerInfo.setRMIPort(rmiPort);
        System.setProperty(Context.PROVIDER_URL, "rmi://" + ServerInfo.getBoundHostname() + ":" + rmiPort + "/");

        configureSystemProperties();

        // load an external logging configuration
        String loggingConfig = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.LOG_CONFIG);
        if (loggingConfig != null) {
          loadLoggingConfig(new URL(loggingConfig));
        } else {
          loadLoggingConfig(mulgaraConfig.getExternalConfigPaths().getMulgaraLogging());
        }

        Connector httpConnector = mulgaraConfig.getJetty().getConnector();

        String httpHost = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.HTTP_HOST);
        httpHostName = (httpHost != null || httpConnector == null) ? httpHost : httpConnector.getHost();

        // set the port on which to accept HTTP requests
        String httpPort = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.PORT);
        if (httpPort != null) {
          ServerInfo.setHttpPort(Integer.parseInt(httpPort));
        } else {
          if (httpConnector != null) ServerInfo.setHttpPort(httpConnector.getPort());
        }

        // set the (RMI) name of the server, preferencing the command line
        String serverName = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.SERVER_NAME);
        rmiServerName = (serverName != null) ? serverName : mulgaraConfig.getServerName();

        // set the server's persistence path
        persistencePath = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.PERSISTENCE_PATH);
        if (persistencePath == null) persistencePath = mulgaraConfig.getPersistencePath();

        // if the persistence path was one we know about, substitute it
        if (persistencePath.equalsIgnoreCase(".")) {
          persistencePath = System.getProperty("user.dir");
        } else if (persistencePath.equalsIgnoreCase("temp")) {
          persistencePath = System.getProperty("java.io.tmpdir");
        }

        // set the smtp name of the server
        String smtpServer = (String)parser.getOptionValue(EmbeddedMulgaraOptionParser.SMTP_SERVER);
        if (smtpServer == null) smtpServer = mulgaraConfig.getSmtp();
       // set the property for mail package to pickup
        System.setProperty(SYSTEM_MAIL, smtpServer);
      }
    } catch (MalformedURLException mue) {
      log.warn("Invalid URL on command line - " + mue.getMessage());
      printUsage();
      startServer = false;
    } catch (IOException ioe) {
      log.error(ioe.getMessage(), ioe);
      printUsage();
      startServer = false;
    } catch (NumberFormatException nfe) {
      log.warn("Invalid port specified on command line: " + nfe.getMessage());
      printUsage();
      startServer = false;
    } catch (org.exolab.castor.xml.MarshalException me) {
      log.warn("Castor Marshal Exception: " + me.getMessage(), me);
      printUsage();
      startServer = false;
    } catch (org.exolab.castor.xml.ValidationException ve) {
      log.warn("Unable to load configuration - " + ve.getMessage());
      printUsage();
      startServer = false;
    } catch (Exception e) {
      log.warn("Could not start embedded Mulgara server", e );
      startServer = false;
    }

    // return true if the server should be started, false otherwise
    return startServer;
  }


  /**
   * Creates a Mulgara server.
   * @param serverName the RMI binding name of the server
   * @param statePath the path to the directory containing server state
   * @param hostname the hostname to bind the Mulgara server to
   * @param providerClassName  class name of a {@link org.mulgara.server.Session} implementation
   * @param serverClassName    class name of a {@link org.mulgara.server.ServerMBean}
   * @return a Mulgara server
   * @throws ClassNotFoundException if <var>serverClassName</var> isn't in the classpath
   * @throws IOException if the <var>statePath</var> is invalid
   */
  public ServerMBean createServer(String serverName, File statePath, String hostname,
      int portNumber, String providerClassName, String serverClassName)
      throws ClassNotFoundException, IOException {

    // log that we're createing a Mulgara server
    if (log.isDebugEnabled()) {
      log.debug("Creating server instance at rmi://" + hostname + "/" + serverName + " in directory " + statePath);
    }

    // Create the Mulgara server
    ServerMBean mbean = (ServerMBean)Beans.instantiate(getClass().getClassLoader(),serverClassName);

    // Set ServerMBean properties
    mbean.setDir(statePath);
    File tempDir = new File(statePath,"temp");
    mbean.setTempDir(tempDir);
    mbean.setConfig(mulgaraConfig);

    if (log.isDebugEnabled()) log.debug("Set config to be: " + mulgaraConfig);

    // set the directory that all temporary files will be created in.
    tempDir.mkdirs();
    TempDir.setTempDir(tempDir);

    // remove any temporary files 
    cleanUpTemporaryFiles();

    mbean.setProviderClassName(providerClassName);

    // Check to see if the port number is not 1099 and we're using the RMI server.
    if ((portNumber != 1099) && (serverClassName.equals(DEFAULT_SERVER_CLASS_NAME))) {
      mbean.setPortNumber(portNumber);
    }
    mbean.setHostname(hostname);

    // Set protocol-specific properties (FIXME: hardcoded to do "name" only)
    try {
      Method setter = new PropertyDescriptor("name", mbean.getClass()).getWriteMethod();
      try {
        setter.invoke(mbean, new Object[] {serverName});
      } catch (InvocationTargetException e) {
        log.warn(mbean + " doesn't have a name property", e);
      }

      // Now the mbean has the hostname and server name, we can set the URI for the server info
      ServerInfo.setServerURI(mbean.getURI());

    } catch (IllegalAccessException e) {
      log.warn(serverClassName + " doesn't have a public name property", e);
    } catch (IntrospectionException e) {
      log.warn(serverClassName + " doesn't have a name property", e);
    }

    // return the newly created server instance
    return mbean;
  }


  /**
   * Creates a HTTP server.
   * @return an HTTP server
   * @throws IOException if the server configuration cannot be found
   * @throws SAXException if the HTTP server configuration file is invalid
   * @throws ClassNotFoundException if the HTTP server configuration file contains a reference to an unkown class
   * @throws NoSuchMethodException if the HTTP server configuration file contains a reference to an unkown method
   * @throws InvocationTargetException if an error ocurrs while trying to configure the HTTP server
   * @throws IllegalAccessException If a class loaded by the server is accessed in an unexpected way.
   */
  public Server createHttpServer() throws IOException, SAXException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    if (log.isDebugEnabled()) log.debug("Creating HTTP server instance");

    // Set the magic logging property for Jetty to use Log4j
    System.setProperty(MortbayLogger.LOGGING_CLASS_PROPERTY, MortbayLogger.class.getCanonicalName());

    // create and register a new HTTP server
    Server server;
    if (mulgaraConfig.getJetty().getConnector() == null) {
      // create a default server
      server = new Server(ServerInfo.getHttpPort());
    } else {
      // create a server with a configured connector
      server = new Server();
      addConnector(server);
    }

    // add the webapps
    try {
//    server.addHandler(new DefaultHandler());
      addWebServicesWebAppContext(server);
      addWebUIWebAppContext(server);
      addWebQueryContext(server);
    } catch (IllegalStateException e) {
      // not fatal, so just log the problem and go on
      log.warn("Unable to start web service", e.getCause());
    }

    // add our class loader as the classloader of all contexts, unless this is a webapp in which case we wrap it
    ClassLoader classLoader = this.getClass().getClassLoader();
    for (Handler handler: server.getChildHandlers()) {
      if (handler instanceof WebAppContext) ((WebAppContext)handler).setClassLoader(new WebAppClassLoader(classLoader, (WebAppContext)handler));
      else if (handler instanceof ContextHandler) ((ContextHandler)handler).setClassLoader(classLoader);
    }

    // return the server
    return server;
  }


  /**
   * Sets up any system properties needed by system components like JNDI and security.
   * @throws IOException if any files embedded within the JAR file cannot be found
   */
  protected void configureSystemProperties() throws IOException {
    boolean startedLocalRMIRegistry = false;

    // attempt to start a rmiregistry
    if (System.getProperty(DISABLE_RMI) == null) {
      try {
        // start the registry
        LocateRegistry.createRegistry(ServerInfo.getRMIPort());
        if (log.isInfoEnabled()) log.info("RMI Registry started automatically on port " + ServerInfo.getRMIPort());
        // set the flag
        startedLocalRMIRegistry = true;
      } catch (java.rmi.server.ExportException ex) {
        log.info("Existing RMI registry found on port " + ServerInfo.getRMIPort());
      } catch (Exception ex) {
        log.error("Failed to start or detect RMI Registry", ex);
      }
    }

    // set system properties needed for RMI
    System.setProperty(Context.INITIAL_CONTEXT_FACTORY, CONTEXT_FACTORY);

    if (log.isDebugEnabled()) log.debug("No system security manager set");

    // only set the security policy if a RMI registry has started
    if (startedLocalRMIRegistry) {
      if (System.getProperty("java.security.policy") == null) {
        if (log.isDebugEnabled()) log.debug("Started local RMI registry -> setting security policy");

        URL mulgaraSecurityPolicyURL = ClassLoader.getSystemResource(RMI_SECURITY_POLICY_PATH);
        System.setProperty(SECURITY_POLICY_PROP, mulgaraSecurityPolicyURL.toString());

        if (log.isInfoEnabled()) log.info("java.security.policy set to " + mulgaraSecurityPolicyURL.toString());
      }

      // create a security manager
      System.setSecurityManager(new RMISecurityManager());
    }
  }


  /**
   * Adds a listener to the <code>httpServer</code>. The listener is created and configured
   * according to the Jetty configuration.
   * @param httpServer the server to add the listener to
   * @throws UnknownHostException if an invalid hostname was specified in the Mulgara server configuration
   */
  private void addConnector(Server httpServer) throws UnknownHostException {
    if (httpServer == null) throw new IllegalArgumentException("Null \"httpServer\" parameter");

    if (log.isDebugEnabled()) log.debug("Adding socket listener");

    // create and configure a listener
    AbstractConnector connector = new BlockingChannelConnector();
    if ((httpHostName != null) && !httpHostName.equals("")) {
      connector.setHost(httpHostName);
      if (log.isDebugEnabled()) log.debug("Servlet container listening on host " + this.getHttpHostName());
    } else {
      httpHostName = getResolvedLocalHost();
      if (log.isDebugEnabled()) log.debug("Servlet container listening on all host interfaces");
    }

    // set the listener to the jetty configuration
    Connector jettyConfig = (Connector)mulgaraConfig.getJetty().getConnector();
    connector.setPort(ServerInfo.getHttpPort());

    if (jettyConfig.hasMaxIdleTimeMs()) connector.setMaxIdleTime(jettyConfig.getMaxIdleTimeMs());
    if (jettyConfig.hasLowResourceMaxIdleTimeMs()) connector.setLowResourceMaxIdleTime(jettyConfig.getLowResourceMaxIdleTimeMs());
    if (jettyConfig.hasAcceptors()) {
      int acceptors = jettyConfig.getAcceptors();
      if (acceptors > WEIRD_JETTY_THREAD_LIMIT) {
        log.warn("Acceptor threads set beyond HTTP Server limits. Reducing from" + acceptors + " to " + WEIRD_JETTY_THREAD_LIMIT);
        acceptors = WEIRD_JETTY_THREAD_LIMIT;
      }
      connector.setAcceptors(acceptors);
    }

    // add the listener to the http server
    httpServer.addConnector(connector);
  }


  /**
   * Creates the Mulgara Descriptor UI
   * TODO: Rebuild this as a simple servelet
   * @throws IOException if the driver WAR file is not readable
   */
  private void addWebServicesWebAppContext(Server server) throws IOException {
    // get the URL to the WAR file
    URL webServicesWebAppURL = ClassLoader.getSystemResource(WEBAPP_PATH + "/" + WEBSERVICES_WEBAPP);

    if (webServicesWebAppURL == null) {
      log.warn("Couldn't find resource: " + WEBAPP_PATH + "/" + WEBSERVICES_WEBAPP);
      return;
    }

    String warPath = extractToTemp(WEBAPP_PATH + "/" + WEBSERVICES_WEBAPP);
    
    // Add Descriptors and Axis
    WebAppContext descriptorWARContext = new WebAppContext(server, warPath, "/" + WEBSERVICES_PATH);

    // make some attributes available
    descriptorWARContext.setAttribute(BOUND_HOST_NAME_KEY, ServerInfo.getBoundHostname());
    descriptorWARContext.setAttribute(SERVER_MODEL_URI_KEY, ServerInfo.getServerURI().toString());

    // log that we're adding the test webapp context
    if (log.isDebugEnabled()) log.debug("Added Web Services webapp context");
  }


  /**
   * Creates the Mulgara Semantic Store Query Tool (webui).
   * @throws IOException if the driver WAR file i not readable
   */
  private void addWebUIWebAppContext(Server server) throws IOException {
    if (log.isDebugEnabled()) log.debug("Adding WebUI webapp context");

    // get the URL to the WebUI WAR file
    String warPath = extractToTemp(WEBAPP_PATH + "/" + WEBUI_WEBAPP);

    // load the webapp if the WAR file exists
    if (warPath != null) {
      // create the test webapp handler context
      new WebAppContext(server, warPath, "/" + WEBUI_PATH);
    } else {
      log.warn("Could not find WebUI webapp WAR file -> not adding to servlet container");
    }
  }


  /**
   * Creates the Mulgara Semantic Store Query Tool (webui).
   * @throws IOException if the driver WAR file i not readable
   */
  private void addWebQueryContext(Server server) throws IOException {
    if (log.isDebugEnabled()) log.debug("Adding WebQuery servlet context");

    // create the web query context
    try {
      Servlet servlet = (Servlet)Reflect.newInstance(Class.forName("org.mulgara.webquery.QueryServlet"), getHttpHostName(), getServerName(), (AbstractServer)serverManagement);
      new org.mortbay.jetty.servlet.Context(server, "/" + WEBQUERY_PATH, SESSIONS).addServlet(new ServletHolder(servlet), "/*");
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Not configured to use the requested Query servlet");
    }
  }


  /**
   * Extracts a resource from the environment (a jar in the classpath) and writes
   * this to a file in the working temporary directory.
   * @param resourceName The name of the resource. This is a relative file path in the jar file.
   * @return The absolute path of the file the resource is extracted to, or <code>null</code>
   *         if the resource does not exist.
   * @throws IOException If there was an error reading the resource, or writing to the extracted file.
   */
  private String extractToTemp(String resourceName) throws IOException {
    // Find the resource
    URL resourceUrl = ClassLoader.getSystemResource(resourceName);
    if (resourceUrl == null) return null;

    // open the resource and the file where it will be copied to
    InputStream in = resourceUrl.openStream();
    File outFile = new File(TempDir.getTempDir(), new File(resourceName).getName());
    log.info("Extracting: " + resourceUrl + " to " + outFile);
    OutputStream out = new FileOutputStream(outFile);

    // loop to copy from the resource to the output file
    byte[] buffer = new byte[10240];
    int len;
    while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
    in.close();
    out.close();

    // return the file that the resource was extracted to
    return outFile.getAbsolutePath();
  }


  /**
   * Prints the usage instructions for starting the server.
   */
  public static void printUsage() {
    // build the usage message
    StringBuilder usage = new StringBuilder();
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
    usage.append("-n, --normi         disable automatic starting of the RMI registry" + eol);
    usage.append("-w, --nohttp        disable the HTTP web service" + eol);
    usage.append("-x, --shutdown      shutdown the local running server" + eol);
    usage.append("-l, --logconfig     use an external logging configuration file" + eol);
    usage.append("-c, --serverconfig  use an external server configuration file" + eol);
    usage.append("-k, --serverhost    the hostname to bind the server to" + eol);
    usage.append("-o, --httphost      the hostname for HTTP requests" + eol);
    usage.append("-p, --port          the port for HTTP requests" + eol);
    usage.append("-r, --rmiport       the RMI registry port" + eol);
    usage.append("-s, --servername    the (RMI) name of the server" + eol);
    usage.append("-a, --path          the path server data will persist to, specifying " + eol +
        "                    '.' or 'temp' will use the current working directory " + eol +
        "                    or the system temporary directory respectively" + eol);
    usage.append("-m, --smtp          the SMTP server for email notifications" + eol);
    usage.append(eol);

    usage.append("Note 1. A server can be started without any options, all options" + eol +
        "override default settings." + eol + eol);
    usage.append("Note 2. If an external configuration file is used, and other options" + eol +
        "are specified, the other options will take precedence over any settings" + eol +
        "specified in the configuration file." + eol + eol);

    // print the usage
    System.out.println(usage.toString());
  }


  /**
   * Clean up any temporary files and directories. In some instances the VM does
   * not remove temporary files and directories. ie Windows JVM Some files maybe
   * left due to file locking, however they will be deleted on the next clean
   * up.
   *
   */
  private static void cleanUpTemporaryFiles() {
    File tempDirectory = TempDir.getTempDir();

    // Add a filter to ensure we only delete the correct files
    File[] list = tempDirectory.listFiles(new TemporaryFileNameFilter());

    // nothing to be removed
    if (list == null) return;

    // Remove the top level files and recursively remove all files in each directory.
    for (File f: list) {
      if (f.isDirectory()) removeDirectory(f);
      f.delete();
    }
  }


  /**
   * Remove the contents of a directory.
   * @param directory A {@link java.io.File} representing a directory.
   */
  private static void removeDirectory(File directory) {
    File[] list = directory.listFiles();

    if (list == null) {
      log.error("Unable to remove directory: \"" + directory + "\"");
      return;
    }

    for (File f: list) {
      if (f.isDirectory()) removeDirectory(f);
      f.delete();
    }
  }


  /**
   * Filter class for detecting temporary files created by Mulgara
   */
  private static class TemporaryFileNameFilter implements FilenameFilter {

    public boolean accept(File dir, String name) {
      // check for files and directories with
      // mulgara*.jar , Jetty-*.war and JettyContext*.tmp
      return (((name.indexOf("mulgara") == 0) && (name.indexOf(".jar") > 0)) ||
             ((name.indexOf("Jetty-") == 0) && (name.indexOf(".war") > 0)) ||
             ((name.indexOf("JettyContext") == 0) &&
             (name.indexOf(".tmp") > 0)));
    }
  }


  /**
   * A server side shutdown service to allow the BootStrap class to force a
   * shutdown from another JVM while on the same machine only. To override the
   * default port of 6789 set a system property called shutdownhook.port
   */
  private static class ShutdownService extends Thread {

    private ServerSocket shutdownSocket;

    public ShutdownService() {
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
        ServerSocket shutdownSocket = new ServerSocket(port, 0, InetAddress.getByName("localhost"));

        // wait until a request to stop the server
        while (!stop) {
          // wait for a shutdown request
          Socket socket = shutdownSocket.accept();

          // read the response from the client
          BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));

          // check if the require request is correct
          String message = input.readLine();

          if ((message != null) && message.equals(EmbeddedMulgaraServer.SHUTDOWN_MSG)) {
            socket.close();
            stop = true;
          } else {
            socket.close();
            if (message != null) log.error("Incorrect request to shutdown mulgara");
          }
        }
      } catch (IOException ioEx) {
        log.error("Unable to establish shutdown socket due to an I/O exception on port " + port, ioEx);
      } catch (SecurityException secEx) {
        log.error("Unable to establish shutdown socket due to a security exception. Check security policy", secEx);
      } catch (Exception ex) {
        log.error("Unable to establish shutdown socket on port " + port, ex);
      } finally {
        // attempt to close the socket
        try {
          shutdownSocket.close();
        } catch (Exception ex) {
          /* skip */
        }
      }

      // log that we're sutting down the servers
      if (log.isInfoEnabled()) {
        log.info("Started system exit.");
      }

      // finally
      // issue the shutdown
      if (stop) System.exit(0);
    }
  }


  /**
   * The standard shutdown hook that will get run when this server is killed normally.
   * This gets registered with the Runtime.
   */
  private static class RuntimeShutdownHook extends Thread {
    
    EmbeddedMulgaraServer server;
  
    public RuntimeShutdownHook(EmbeddedMulgaraServer server) {
      this.server = server;
      // register a thread name
      this.setName("Standard shutdown hook");
    }
  
    public void run() {
      // log that we're sutting down the servers
      if (log.isInfoEnabled()) {
        log.info("Shutting down server, please wait...");
      } else {
        // regardless of the log level output this to stdout.
        // Note. "\n" Will give us a new line beneath a Ctrl-C
        System.out.println("\nShutting down server, please wait...");
      }

      // stop RMI service
      ServerMBean mbean = server.getServerMBean();
      if (mbean != null) {
        ServerState state = mbean.getState();
  
        if (state == ServerState.STARTED) {
          try {
            mbean.stop();
          } catch (Exception e) {
            log.error("Couldn't stop server", e);
          }
        }
  
        // close the server
        if (state == ServerState.STARTED || state == ServerState.STOPPED) {
          try {
            mbean.destroy();
          } catch (Exception e) {
            log.error("Couldn't destroy server", e);
          }
        }
      }

      // shut down the SOAP server
      try {
        if (server.getHttpServer() != null) {
          server.getHttpServer().stop();
        }
      } catch (Exception e) {
        log.error("Couldn't destroy http server", e);
      }

      // log that we've shut down the servers
      if (log.isInfoEnabled()) {
        log.info("Completed shutting down server");
      } else {
        // regardless of the log level out this to stdout.
        System.out.println("Completed shutting down server");
      }

      // Clean up any temporary directories and files
      cleanUpTemporaryFiles();
    }
  }
}
