/*
 * Copyright 2008 Fedora Commons, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mulgara.util;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RemoteStub;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

/**
 * A utility to centralize the port handling for RMI objects.
 * This class is not set to handle different protocols. If this is needed, then the
 * super constructor {@link #UnicastRemoteObject(int,RMIClientSocketFactory,RMIServerSocketFactory)}
 * would need to be overridden.
 *
 * @created Sep 23, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class Rmi extends UnicastRemoteObject {

  /** Logger */
  private final static Logger logger = Logger.getLogger(Rmi.class.getName());

  /** Generation UID */
  private static final long serialVersionUID = -8087526398171872888L;

  public static final String CLIENT_OBJECT_PORT = "mulgara.rmi.objectPort";

  /** The default port used for exporting objects. */
  protected static int defaultPort = 0;

  // Check if a system property has been set for the default port
  static {
    String val = System.getProperty(CLIENT_OBJECT_PORT);
    if (val != null) {
      try {
        defaultPort = Integer.parseInt(val);
      } catch (NumberFormatException e) {
        logger.warn("Unable to parse the client peer port for RMI: " + val);
      }
    }
  }


  /**
   * Default constructor. Uses the default port.
   * @throws RemoteException If the object could not be exported.
   */
  protected Rmi() throws RemoteException {
    super(defaultPort);
  }


  /**
   * Constructor with a specified port.
   * @param port A specified port. If 0 then a default port will be used.
   * @throws RemoteException If the object could not be exported.
   */
  protected Rmi(int port) throws RemoteException {
    super(port == 0 ? defaultPort : port);
  }


  /**
   * We don't want users using this method, since we cannot control the port, and we cannot
   * control the return type if a port is specified through another method.
   * @param obj The object to export.
   * @return Not implemented.
   * @throws RemoteException There was an error exporting the object.
   */
  public static RemoteStub exportObject(Remote obj) throws RemoteException {
    throw new UnsupportedOperationException("Use the export() method instead.");
  }


  /**
   * Exports an object through RMI, using a known port if configured, or a random port otherwise.
   * This will not create a default exporter if one does not exist.
   * @param obj The object to export.
   * @return An exported object.
   * @throws RemoteException There was an error exporting the object.
   */
  public static Remote export(Remote obj) throws RemoteException {
    if (defaultPort == 0) return UnicastRemoteObject.exportObject(obj);
    return UnicastRemoteObject.exportObject(obj, defaultPort);
  }


  /**
   * Unexport an object from RMI.
   * @param obj The object to unexport.
   * @return <code>false</code> if the object could not be unexported. This may happen if it is
   *         still in use.
   * @throws RemoteException There was an error exporting the object.
   */
  public static boolean unexportObject(Remote obj) throws RemoteException {
    return UnicastRemoteObject.unexportObject(obj, false);
  }


  /**
   * Sets the port for the default exporter to use. 
   * @param port The port number to use.
   */
  public static void setDefaultPort(int port) {
    defaultPort = port;
  }


  /**
   * Gets the port to use. 
   * @return The port number, or 0 if a random port is to be used.
   */
  public static int getDefaultPort() {
    return defaultPort;
  }


  /**
   * Unexport this object from RMI.
   * @return <code>false</code> if the object could not be unexported. This may happen if it is
   *         still in use.
   * @throws RemoteException There was an error exporting the object.
   */
  public boolean unexport() throws RemoteException {
    return unexportObject(this);
  }


  /**
   * Unexport an object from RMI.
   * @param force If true, then unexport the object, even if it still in use.
   * @return <code>false</code> if the object could not be unexported. This may happen if
   *         <var>force</var> is <code>false</code> and <code>obj</code> is still in use.
   * @throws RemoteException There was an error exporting the object.
   */
  public boolean unexport(boolean force) throws RemoteException {
    return unexportObject(this, force);
  }

}
