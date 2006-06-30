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

package org.mulgara.server.rmi;


// Java 2 standard packages
import java.io.*;
import java.lang.reflect.Constructor;
import java.net.*;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import javax.naming.*;

// Third party packages
import org.apache.log4j.*;

// Locally written packages
import org.mulgara.server.AbstractServer;

/**
 * Java RMI server.
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @created 2002-01-12
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:02 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2002-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 *
 * @see <a href="http://developer.java.sun.com/developer/JDCTechTips/2001/tt0327.html#jndi"/>
 *      <cite>JNDI lookup in distributed systems</cite> </a>
 */
public class RmiServer extends AbstractServer implements RmiServerMBean {

  /**
   * Logger. This is named after the classname.
   */
  private final static Logger logger =
    Logger.getLogger(RmiServer.class.getName());

  /**
   * The Java RMI registry naming context.
   */
  private Context rmiRegistryContext;

  /**
   * The RMI registry name of this server.
   */
  private String name;

  /**
   * The local copy of the RMI session factory. This reference must be held
   * because the garbage collector isn't aware of remote stubs on distant JVMs.
   *
   */
  private RemoteSessionFactory remoteSessionFactory;

  /**
   * An RMI stub that proxies for {@link #remoteSessionFactory}. This instance
   * can be serialized and distributed to remote JVMs.
   *
   */
  private RemoteSessionFactory exportedRemoteSessionFactory;

  /**
   * Set the name the server is bound to in the RMI registry. It's possible to
   * set <var>name</var> to <code>null</code>, but the
   * {@link org.mulgara.server.ServerMBean#start} action can't be used in that
   * case. The <var>name</var> cannot be set while the server is started.
   *
   * @param name the new value
   * @throws IllegalStateException if the server is started or if the database
   *      already has a fixed URI
   */
  public void setName(String name) {

    // Prevent the name from being changed while the server is up
    if (this.getState() == STARTED) {

      throw new IllegalStateException(
          "Can't change name without first stopping the server");
    }

    // Set field
    this.name = name;
    updateURI();
  }

  /**
   * Sets the hostname of the server.
   *
   * @param hostname the hostname of the server, if <code>null</code> <code>localhost</code>
   *      will be used
   * @throws IllegalStateException if the service is started or if the
   *      underlying session factory already has a fixed hostname
   */
  public void setHostname(String hostname) {

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

      this.hostname = hostname;
    }

    updateURI();
  }

  //
  // MBean properties
  //

  /**
   * Read the name the server is bound to in the RMI registry.
   *
   * @return The bound name of the server.
   */
  public String getName() {

    return name;
  }

  //
  // Methods implementing AbstractServer
  //

  /**
   * Start the server.
   *
   * @throws IllegalStateException if <var>name</var> is <code>null</code>
   * @throws NamingException Error accessing RMI registry.
   * @throws RemoteException Error accessing RMI services.
   */
  protected void startService() throws NamingException, RemoteException {

    // Validate "name" property
    if (name == null) {

      throw new IllegalStateException("Must set \"name\" property");
    }

    // Initialize fields
    rmiRegistryContext = new InitialContext();

    // Apply RMI wrapper to the session factory
    remoteSessionFactory = new RemoteSessionFactoryImpl(getSessionFactory());
    exportedRemoteSessionFactory =
        (RemoteSessionFactory) UnicastRemoteObject.exportObject(
        remoteSessionFactory);

    // Bind the service to the RMI registry
    rmiRegistryContext.rebind(name, exportedRemoteSessionFactory);
  }

  /**
   * Stop the server.
   *
   * @throws NamingException EXCEPTION TO DO
   * @throws NoSuchObjectException EXCEPTION TO DO
   */
  protected void stopService() throws NamingException, NoSuchObjectException {

    rmiRegistryContext.unbind(name);
    UnicastRemoteObject.unexportObject(remoteSessionFactory, true);
  }

  private void updateURI() {

    URI newURI = null;

    // A not null name means to override the default.  A null name will derive
    // it from the RMIRegistry.
    if (name != null) {

      if (this.getHostname() == null) {

        // try to use the local host name
        try {

          hostname = InetAddress.getLocalHost().getCanonicalHostName();
        }
        catch (Exception e) {

          logger.warn("Problem getting host name! - using localhost");
          hostname = "localhost";
        }
      }
      else {

        hostname = this.getHostname();
      }
    }

    // Generate new server URI
    try {

      newURI = new URI("rmi", null, hostname, getPortNumber(), "/" + name, null,
          null);
    }
    catch (URISyntaxException e) {

      throw new Error("Bad generated URI", e);
    }

    // Set URI.
    setURI(newURI);
  }
}
