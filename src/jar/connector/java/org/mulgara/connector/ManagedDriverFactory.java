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

package org.mulgara.connector;

// Java 2 standard packages
import java.io.*;
import java.util.Set;
import java.util.logging.*;

// Java 2 enterprise packages
import javax.resource.*;
import javax.resource.spi.*;
import javax.security.auth.Subject;

import org.kowari.server.SessionFactory;
//import org.kowari.server.driver.SessionFactoryImpl;

/**
 *
 *
 * @created 2001-07-21
 *
 * @author Simon Raboczi
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:38 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2002 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ManagedDriverFactory implements ManagedConnectionFactory,
    Serializable {

  /**
   * Logger.
   */
  private Logger logger = Logger.getLogger(toString());

  /**
   * Description of the Field
   */
  private PrintWriter logWriter = null;

  /**
   * The session factory.
   */
  private SessionFactory sessionFactory;

  /**
   * @param logWriter The new LogWriter value
   * @throws ResourceException
   */
  public void setLogWriter(PrintWriter logWriter) throws ResourceException {

    this.logWriter = logWriter;
  }

  /**
   * @return The LogWriter value
   * @throws ResourceException
   */
  public PrintWriter getLogWriter() throws ResourceException {

    return logWriter;
  }

  //
  // Methods implementing ManagedConnectionFactory
  //

  /**
   * @param connectionManager PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws ResourceException
   */
  public Object createConnectionFactory(ConnectionManager connectionManager) throws
      ResourceException {

    logger.info("createConnectionFactory connectionManager=" +
        connectionManager);

    try {

//    FIX ME!
//      return new SessionFactoryImpl();
      return null;
    }
    catch (Exception e) {

      ResourceException re =
          new ResourceException("Couldn't create connection factory");
      re.setLinkedException(e);
      throw re;
    }
  }

  /**
   * @return RETURNED VALUE TO DO
   * @throws ResourceException
   */
  public Object createConnectionFactory() throws ResourceException {

    logger.info("createConnectionFactory");

    return createConnectionFactory(new DriverManager());
  }

  /**
   * @param subject PARAMETER TO DO
   * @param connectionRequestInfo PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws ResourceException
   */
  public ManagedConnection createManagedConnection(Subject subject,
      ConnectionRequestInfo connectionRequestInfo) throws ResourceException {

    logger.info("createManagedConnection");

    return new ManagedDriver();
  }

  /**
   * @param connectionSet PARAMETER TO DO
   * @param subject PARAMETER TO DO
   * @param connectionRequestInfo PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   * @throws ResourceException
   */
  public ManagedConnection matchManagedConnections(Set connectionSet,
      Subject subject,
      ConnectionRequestInfo connectionRequestInfo) throws ResourceException {

    logger.info("matchManagedConnection");

    return createManagedConnection(subject, connectionRequestInfo);
  }

  /**
   * METHOD TO DO
   *
   * @return RETURNED VALUE TO DO
   */
  public int hashCode() {

    return super.hashCode();
  }

  /**
   * METHOD TO DO
   *
   * @param object PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public boolean equals(Object object) {

    return super.equals(object);
  }
}
