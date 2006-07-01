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

package org.mulgara.server.driver;

// Java 2 Standard Packages
import java.net.*;
import java.util.*;
import javax.naming.*;

// Third party packages
/*
import javax.jmdns.JmDNS;        // ZeroConf
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceInfo;
*/
import org.apache.log4j.Logger;  // Apache Log4J

// Locally written packages
import org.mulgara.server.*;

/**
 * Obtain a {@link SessionFactory} instance.
 *
 * @created 2004-03-30
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/07 05:43:51 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2004 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class SessionFactoryFinder {

  /**
   * Logger.
   *
   * This is named after the class.
   */
  private static final Logger logger =
      Logger.getLogger(SessionFactoryFinder.class.getName());

  /**
   * A ZeroConf peer.
   *
   * This listens for servers on the LAN.
   */
  /*
     private static JmDNS jmdns;
   */

  /**
   * Map from a database's URI scheme to the name of a {@link SessionFactory}
   * implementation for it.
   */
  private static final Map schemeMap = new HashMap();

  static {
    schemeMap.put("beep", "org.mulgara.server.beep.BEEPSessionFactory");
    schemeMap.put("rmi", "org.mulgara.server.rmi.RmiSessionFactory");
    schemeMap.put("local", "org.mulgara.server.local.LocalSessionFactory");

    /*
         try {
      jmdns = new JmDNS(InetAddress.getLocalHost());
     jmdns.addServiceListener("_itql._tcp.local", new ServerServiceListener());
         }
         catch (Exception e) {
      logger.warn("Couldn't start ZeroConf peer", e);
         }
     */
  }

  /**
   * Obtain a {@link SessionFactory} instance.  Assumes that this is being used
   * locally - within the same JVM.
   *
   * @param serverURI  the internet server to connect this session to; if
   *   <code>null</code>, try to find an RMI server named <q>server1</q> on the
   *   local host
   * @throws SessionFactoryFinderException if a connection can't be established
   *   to the server
   */
  public static SessionFactory newSessionFactory(URI serverURI) throws
      SessionFactoryFinderException, NonRemoteSessionException {
    return newSessionFactory(serverURI, false);
  }

  /**
   * Obtain a {@link SessionFactory} instance.
   *
   * @param serverURI  the internet server to connect this session to; if
   *   <code>null</code>, try to find an RMI server named <q>server1</q> on the
   *   local host
   * @param isRemote true if the client is trying to connect to a remote server.
   * @throws SessionFactoryFinderException if a connection can't be established
   *   to the server
   */
  public static SessionFactory newSessionFactory(URI serverURI,
      boolean isRemote) throws SessionFactoryFinderException, NonRemoteSessionException {

    // If no serverURI was specified, search the LAN for a local server
    if (serverURI == null) {
      serverURI = findServerURI();
    }
    assert serverURI != null;

    // Obtain the classname for the SessionFactory
    String className;

    // Handle RMI schemes differently.
    if (serverURI.getScheme().equals("rmi")) {
      logger.debug("Attempting to connect via RMI");
      // First attempt to connect via RMI.
      try {
        Hashtable environment = new Hashtable();
        environment.put("java.naming.factory.initial",
            "com.sun.jndi.rmi.registry.RegistryContextFactory");
        environment.put("java.naming.provider.url",
            "rmi://" + serverURI.getRawAuthority());
        Context rmiRegistryContext = new InitialContext(environment);
        rmiRegistryContext.lookup(serverURI.getPath()
          .substring(1));

        // If the lookup is successful connect using RMI.
        className = (String) schemeMap.get(serverURI.getScheme());
      } catch (NamingException ne) {
        logger.debug("Failed to connect via RMI", ne);

        // If there is an exception connect locally.
        if (!isRemote) {
          logger.debug("Attempting to fallback to local", ne);
          className = (String) schemeMap.get("local");
        }
        else {
          throw new SessionFactoryFinderException(
              "Cannot find server " + serverURI, ne);
        }
      }
    }
    else {
      className = (String) schemeMap.get(serverURI.getScheme());
    }

    if (className == null) {
      throw new SessionFactoryFinderException(
          serverURI + " has unsupported scheme (" + serverURI.getScheme() + ")"
      );
    }
    assert className != null;

    // Use reflection to create the SessionFactory
    try {
      try {
        return (SessionFactory) Class.forName(className).
            getConstructor(new Class[] { URI.class }).newInstance(new Object[] {
            serverURI});
      } catch (java.lang.reflect.InvocationTargetException ie) {
        // check if the exception thrown indicates we should retry
        Throwable e = ie.getCause();
        if (!(e instanceof NonRemoteSessionException)) {
          throw new SessionFactoryFinderException("Couldn't create session factory for " + serverURI, e);
        }
        // tell the calling code
        throw (NonRemoteSessionException)e;
      }

    } catch (Exception e) {
      throw new SessionFactoryFinderException(
          "Couldn't create session factory for " + serverURI, e
      );
    }
  }

  /**
   * Find a server.
   *
   * This currently only tries for a Java RMI server on the local host.
   *
   * @return a Java RMI server {@link URI} for the local host, never
   *   <code>null</code>
   * @throws SessionFactoryFinderException  if the server URI can't be composed
   */
  public static URI findServerURI() throws SessionFactoryFinderException {
    // Look for an RMI server named "server1" on the local host
    try {
      return new URI(
          "rmi", // Java RMI protocol
          InetAddress.getLocalHost().getCanonicalHostName(), // host
          "/server1", // default RMI server
          null // no fragment means this is a server, not a model
      );
    }
    catch (UnknownHostException e) {
      throw new SessionFactoryFinderException(
          "Couldn't determine local host name", e
      );
    }
    catch (URISyntaxException e) {
      throw new SessionFactoryFinderException(
          "Invalid local server URI", e
      );
    }

    /*
         throw new SessionFactoryFinderException(
      "Null \"serverURI\" parameter - LAN search not yet implemented"
         );
     */
  }

  /**
   * Method to ask the ServerInfo for the local database session.
   * This will return null if ServerInfo is not available -
   * ie. being run on a host which has no local database, such an an iTQL client.
   *
   * @throws SessionFactoryFinderException If ServerInfo can't be called.  This
   *         should only happen on a client, which should not have asked for a local session
   *         anyway.
   */
  private static SessionFactory getLocalSessionFactory() throws SessionFactoryFinderException {
    Object factory = null;
    try {
      Class rsf = Class.forName("org.mulgara.server.ServerInfo");
      java.lang.reflect.Method getLocalSessionFactory = rsf.getMethod("getLocalSessionFactory", null);
      factory = getLocalSessionFactory.invoke(null, null);
    } catch (Exception e) {
      throw new SessionFactoryFinderException(
          "Couldn't obtain a local session factory.  Unexpected problem.", e
      );
    }
    if (factory == null) {
      throw new SessionFactoryFinderException(
          "Couldn't obtain a local session factory.  Server has no access to database."
      );
    }
    return (SessionFactory)factory;
  }

  /**
   * Listens for notification via ZeroConf of servers appearing on the LAN.
   */
  /*
     static class ServerServiceListener implements ServiceListener
     {
    public void addService(JmDNS jmdns, String type, String name)
    {
      logger.info("ZeroConf added "+type+" "+name);
    }

    public void removeService(JmDNS jmdns, String type, String name)
    {
      logger.info("ZeroConf removed "+type+" "+name);
    }

    public void resolveService(JmDNS jmdns, String type, String name,
                               ServiceInfo serviceInfo)
    {
      logger.info("ZeroConf resolved "+type+" "+name+" to "+serviceInfo);
    }
     }
   */
}
