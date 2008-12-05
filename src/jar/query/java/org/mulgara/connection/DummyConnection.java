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

import org.apache.log4j.Logger;
import org.mulgara.query.QueryException;
import org.mulgara.server.Session;

/**
 * A connection for accepting state changes at the local end with no server involvement
 *
 * @created 2007-09-25
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class DummyConnection extends CommandExecutor implements Connection {

  /** Logger. */
  private static final Logger logger = Logger.getLogger(DummyConnection.class.getName());
  
  /** Indicates the current autocommit state */
  private boolean autoCommit = true;

  /**
   * Creates a new connection.
   */
  public DummyConnection() {
  }


  /**
   * Give login credentials and security domain to a session.  This operation is ignored.
   * @param securityDomainUri The security domain for the login.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(URI securityDomainUri, String user, char[] password) {
    logger.warn("Setting credentials on a dummy connection");
  }


  /**
   * Give login credentials for the current security domain to the current session.
   * This operation is ignored.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(String user, char[] password) {
    logger.warn("Setting credentials on a dummy connection");
  }


  /**
   * @return always null
   */
  public Session getSession() {
    return null;
  }


  /**
   * Starts and commits transactions on this connection, by turning the autocommit
   * flag on and off. 
   * @param autocommit <code>true</code> if the flag is to be on.
   * @throws QueryException The session could not change state.
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException {
    this.autoCommit = autoCommit;
  }


  /**
   * @return the autoCommit value
   */
  public boolean getAutoCommit() {
    return autoCommit;
  }


  /**
   * Closes the current connection.  Does nothing for this class.
   */
  public void close() throws QueryException {
  }
  
  
  /**
   * Disposes of the current connection.  Does nothing for this class.
   */
  public void dispose() throws QueryException {
  }


  /**
   * Always returns <code>false</code>.
   */
  public boolean isRemote() {
    return false;
  }
  
}
