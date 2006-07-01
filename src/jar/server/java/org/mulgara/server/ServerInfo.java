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

// Java packages
import java.net.URI;
import java.util.Set;

// log4j packages
import org.apache.log4j.*;
import org.apache.log4j.xml.DOMConfigurator;
import org.mulgara.server.SessionFactory;

/**
 * Holds details on the currently running server.  All data set by {@link EmbeddedMulgaraServer}.
 * This class allows access to data which would normally be stored in EmbeddedMulgaraServer without
 * incurring the overhead of the entire classpath needed by that class.
 *
 * @created 2004-12-01
 *
 * @author Paul Gearon
 *
 * @modified $Date: 2005/01/05 04:58:59 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2001-2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ServerInfo {

  /** The session factory for the local database. */
  private static SessionFactory localSessionFactory = null;

  /** The server URI for this server. */
  private static URI serverURI = null;

  /** The host name that this server is bound to. */
  private static String boundHostname = null;

  /** The port used for RMI. */
  private static int rmiPort = 0;

  /** The port used for HTTP. */
  private static int httpPort = 0;

  /** The set of hostname aliases. */
  private static Set hostnames = null;


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
    return localSessionFactory;
  }


  /**
   * Returns the canonical server URI.
   *
   * For example, <code>rmi://thishost.thisdomain.com/server1</code>.
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
   * @return the hostname this Mulgara server is bound to
   */
  public static String getBoundHostname() {
    return boundHostname;
  }


  /**
   * Returns the port the RMI registry is bound to.
   *
   * @return the port the RMI registry is bound to
   */
  public static int getRMIPort() {
    return rmiPort;
  }


  /**
   * Returns the port the HTTP server is bound to.
   *
   * @return the port the HTTP server is bound to
   */
  public static int getHttpPort() {
    return httpPort;
  }


  /**
   * Returns the set of host name aliases for the server.
   *
   * @return the set of host names
   */
  public static Set getHostnameAliases() {
    return hostnames;
  }


  /**
   * Sets the hostname alias set.  This would be package scope, but has to be called from
   * {@link org.mulgara.resolver.Database}.
   *
   * @param hostnames The set of host names
   * @throws IllegalStateException If the host names have already been set 
   */
  public static void setHostnameAliases(Set hostnames) {
    if (ServerInfo.hostnames != null) {
      throw new IllegalStateException("Host names have already been set");
    }
    ServerInfo.hostnames = hostnames;
  }


  ////////////////////////////////////////////
  // setter methods are all package scope only
  ////////////////////////////////////////////

  /**
   * Sets the static reference to the local
   * {@link org.mulgara.server.SessionFactory}.
   *
   * @param localSessionFactory The new LocalSessionFactory value
   */
  static void setLocalSessionFactory(SessionFactory localSessionFactory) {
    ServerInfo.localSessionFactory = localSessionFactory;
  }


  /**
   * Sets the server URI.
   *
   * @param serverURI The new server URI value.
   */
  static void setServerURI(URI serverURI) {
    ServerInfo.serverURI = serverURI;
    hostnames.add(serverURI.getHost().toLowerCase());
  }


  /**
   * Sets the bound host name.
   *
   * @param boundHostname The name that this server is bound to.
   */
  static void setBoundHostname(String boundHostname) {
    ServerInfo.boundHostname = boundHostname;
  }


  /**
   * Sets the port the RMI registry is bound to.
   *
   * @param rmiPort the port the RMI registry is bound to
   */
  static void setRMIPort(int rmiPort) {
    ServerInfo.rmiPort = rmiPort;
  }


  /**
   * Sets the port the HTTP server is bound to.
   *
   * @param httpPort the port the HTTP server is bound to
   */
  static void setHttpPort(int httpPort) {
    ServerInfo.httpPort = httpPort;
  }

}
