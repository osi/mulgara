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

// Java 2 standard packages
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import javax.naming.*;

// Third party packages
/*
import javax.jmdns.*;       // ZeroConf (multicast DNS)
*/
import org.apache.log4j.*;  // Apache Log4J

// Locally written packages
import org.mulgara.config.MulgaraConfig;
import org.mulgara.query.*;
import org.mulgara.server.SessionFactory;

/**
 * JMX manageable {@link SessionFactory} and transport wrapper. Concrete
 * subclasses are expected to use the {@link #getSessionFactory} method to
 * obtain a reference to the local database, and implement the abstract {@link
 * #startService} and {@link #stopService} methods to start and stop a network
 * service for the database. A separate proxy {@link SessionFactory}
 * implementation must be created to use this network service to provide remote
 * access to the database.
 *
 * @created 2001-09-15
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:58:59 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 *
 * @see <a href="http://developer.java.sun.com/developer/JDCTechTips/2001/tt0327.html#jndi">
 *      <cite>JNDI lookup in distributed systems</cite> </a>
 */
public abstract class AbstractServer implements ServerMBean {

  /**
   * Logger. This is named after the classname.
   */
  private final static Logger logger =
      Logger.getLogger(AbstractServer.class.getName());

  /**
   * The class name of the {@link SessionFactory} implementation.
   */
  private String providerClassName;

  /**
   * The directory where the server can write files.
   */
  private File dir;

  /**
   * The directory where the server can write temporary files.
   */
  private File tempdir;

  /**
   * The server URI.
   */
  private URI uri;

  /**
   * The port number to bind to.
   */
  private int portNumber = -1;

  /**
   * The hostname of the server.
   */
  protected String hostname = null;

  /**
   * State.
   */
  private int state = UNINITIALIZED;

  /**
   * The server object.
   */
  private SessionFactory sessionFactory;

  /** The session factory config object */
  private MulgaraConfig sessionConfig;

  /**
   * ZeroConf server.
   */
  /*
  private JmDNS jmdns;
  */

  /**
   * Returns the hostname of the server.
   *
   * @return The Hostname value
   */
  public String getHostname() {

    return hostname;
  }

  public void setPortNumber(int newPortNumber) {

    portNumber = newPortNumber;
  }

  /**
   * Returns the port number that the server is to bind to.
   *
   * @return the port number value.
   */
  public int getPortNumber() {

    return portNumber;
  }

  /**
   * Sets the hostname of the server.
   *
   * @param newHostname  the hostname of the server, if <code>null</code>
   *     <code>localhost</code> will be used
   * @throws IllegalStateException if the service is started or if the
   *      underlying session factory already has a fixed hostname
   */
  public void setHostname(String newHostname) {

    // Prevent the hostname from being changed while the server is up
    if (this.getState() == STARTED) {

      throw new IllegalStateException(
          "Can't change hostname without first stopping the server");
    }

    // Reset the field
    if (hostname == null) {

      this.hostname = "localhost";
      logger.warn("Hostname supplied is null, defaulting to localhost");
    }
    else {

      hostname = newHostname;
    }
  }

  /**
   * Set the database directory.
   *
   * @param dir The new Dir value
   */
  public void setDir(File dir) {

    this.dir = dir;
  }

  /**
   * Set the temporary database directory.
   *
   * @param tempdir The new Dir value
   */
  public void setTempDir(File tempdir) {

    this.tempdir = tempdir;
  }

  /**
   * Set the database provider classname.
   *
   * @param providerClassName The new ProviderClassName value
   */
  public void setProviderClassName(String providerClassName) {

    this.providerClassName = providerClassName;
  }

  //
  // MBean properties
  //

  /**
   * Read the server state.
   *
   * @return The current server state.
   */
  public int getState() {

    return state;
  }

  /**
   * Read the database directory.
   *
   * @return The Dir value
   */
  public File getDir() {

    return dir;
  }

  /**
   * Read the temporary database directory.
   *
   * @return The tempdir value
   */
  public File getTempDir() {

    return tempdir;
  }

  /**
   * Read the database provider classname.
   *
   * @return The ProviderClassName value
   */
  public String getProviderClassName() {

    return providerClassName;
  }

  /**
   * Read the database URI.
   *
   * @return The URI value
   */
  public URI getURI() {

    return uri;
  }

  /**
   * Allow access by subclasses to the session factory.
   *
   * @return <code>null</code> in the {@link #UNINITIALIZED} state, the {@link
   *      SessionFactory} instance otherwise
   */
  public SessionFactory getSessionFactory() {

    return sessionFactory;
  }

  //
  // MBean actions
  //

  /**
   * Initialize the server. This involves creating the persistence files for the
   * database. Initialization requires a non-null <var>name</var> and <var>
   * providerClassName</var> .
   *
   * @throws ClassNotFoundException if the <var>providerClassName</var> isn't in
   *      the classpath
   * @throws IOException if the persistence directory doesn't exist and can't be
   *      created
   * @throws NoSuchMethodException if the <var>providerClassName</var> doesn't
   *      have a constructor with the correct signature
   * @throws IllegalAccessException EXCEPTION TO DO
   * @throws InstantiationException EXCEPTION TO DO
   * @throws InvocationTargetException EXCEPTION TO DO
   */
  public final void init() throws ClassNotFoundException,
      IllegalAccessException, InstantiationException, InvocationTargetException,
      IOException, NoSuchMethodException {

    // Create the server
    if (logger.isInfoEnabled()) {
      logger.info("Create server");
    }

    // Validate state
    if (dir == null) {

      throw new IllegalStateException("Must set \"dir\" property");
    }

    if (tempdir == null) {

      throw new IllegalStateException("Must set \"tempdir\" property");
    }

    if (providerClassName == null) {

      throw new IllegalStateException("Must set \"providerClassName\" property");
    }

    // Create the directory if necessary
    if (!dir.exists()) {

      dir.mkdir();
    }

    // create the temporary directory
    tempdir.mkdirs();

    // Log provider class name
    if (logger.isInfoEnabled()) {
      logger.info("Provider class is " + providerClassName);
    }

    if (sessionConfig == null) {

      // Create the session factory using the two parameter constructor if we
      // have no configuration
      sessionFactory =
          (SessionFactory) Class.forName(providerClassName)
          .getConstructor(new Class[] {
                          URI.class, File.class})
          .newInstance(new Object[] {
                       getURI(), dir});
    } else {

      // Create the session factory using the three parameter constructor if we
      // have a configuration to use
      sessionFactory =
          (SessionFactory) Class.forName(providerClassName)
          .getConstructor(new Class[] {
                          URI.class, File.class, MulgaraConfig.class})
          .newInstance(new Object[] {
                       getURI(), dir, sessionConfig});
    }

    //assert sessionFactory != null;
    state = STOPPED;

    // Log successful creation
    if (logger.isInfoEnabled()) {
      logger.info("Created server");
    }
  }

  /**
   * Make the server available over the network. If successful the new state
   * should be {@link #STARTED}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public final void start() throws Exception {

    logger.info("Starting");

    // Validate state
    switch (state) {

      case UNINITIALIZED:
        throw new IllegalStateException("Not initialized");

      case STARTED:
        throw new IllegalStateException("Already started");
    }

    //assert state == STOPPED
    // Invariant tests for the STOPPED state
    if (sessionFactory == null) {

      throw new AssertionError("Null \"sessionFactory\" parameter");
    }

    startService();

    /*
    // Start advertising the service via ZeroConf
    if (jmdns == null) {
      try {
        logger.info("Starting ZeroConf server");
        jmdns = new JmDNS(InetAddress.getLocalHost());
        logger.info("Started ZeroConf server");

        String type = "_rmi._tcp.local";
        logger.info(
          "Registering as itql."+type+" on port 1099 with ZeroConf server"
        );
        jmdns.registerService(new ServiceInfo(
          type,           // type
          "itql."+type,   // name
          1099,           // port
          0,              // weight
          0,              // priority
          "path=server1"  // text
        ));
        logger.info("Registered with ZeroConf server");
      }
      catch (IOException e) {
        logger.warn("Couldn't start ZeroConf server", e);
      }
    }
    */

    state = STARTED;
    logger.info("Started");
  }

  /**
   * Make the server unavailable over the network. If successful the new state
   * should be {@link #STOPPED}.
   *
   * @throws Exception EXCEPTION TO DO
   */
  public final void stop() throws Exception {

    logger.info("Shutting down");

    // Validate state
    if (state != STARTED) {

      throw new IllegalStateException("Server is not started");
    }

    //assert state == STARTED;
    /*
    if (jmdns != null) {
      logger.info("Unregistering from ZeroConf server");
      jmdns.unregisterAllServices();
    }
    */
    stopService();
    state = STOPPED;
    logger.info("Shut down");
  }

  /**
   * Destroy the server.
   *
   */
  public final void destroy() {

    logger.info("Destroying");

    try {
      sessionFactory.close();
    }
    catch (QueryException e) {
      logger.warn("Couldn't close server "+uri, e);
    }

    /*
    if (jmdns != null) {
      jmdns.unregisterAllServices();
    }
    */
    sessionFactory = null;
    state = UNINITIALIZED;
    logger.info("Destroyed");
  }

  /**
   * Set the database URI. Implementing subclasses would typically derive a URI
   * from other properties and call this method whenever those properties are
   * modified.
   *
   * @param uri the desired server URI, or <code>null</code>
   * @throws IllegalStateException if the service is started or if the
   *      underlying session factory already has a fixed URI
   */
  protected void setURI(URI uri) {

    // Prevent the URI from being changed while the server is up
    if (state == STARTED) {

      throw new IllegalStateException(
          "Can't change URI without first stopping the server");
    }

    // Reset the field
    this.uri = uri;
  }

  /**
 * Retrieves the configuration used when initialising a session factory.
 *
 * @return The configuration used when initialising a session factory
 */
public MulgaraConfig getConfig() {

  return sessionConfig;
}

/**
 * Sets the configuration to be used when bringing up a session factory.
 *
 * @param config The configuration to be used when bringing up a session
 *               factory
 */
public void setConfig(MulgaraConfig config) {

  // Store the new configuration item
  sessionConfig = config;
}


  /**
   * METHOD TO DO
   *
   * @throws Exception EXCEPTION TO DO
   */
  protected abstract void startService() throws Exception;

  /**
   * METHOD TO DO
   *
   * @throws Exception EXCEPTION TO DO
   */
  protected abstract void stopService() throws Exception;
}
