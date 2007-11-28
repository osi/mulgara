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

import org.mulgara.query.QueryException;
import org.mulgara.server.Session;

/**
 * A connection for sending commands to a server.
 *
 * @created 2007-09-25
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface Connection {

  /**
   * Give login credentials and security domain to the current session.  This should only be needed
   * once since the session does not change.
   * @param securityDomainUri The security domain for the login.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(URI securityDomainUri, String user, char[] password);
  

  /**
   * Give login credentials for the current security domain to the current session.
   * This should only be needed
   * once since the session does not change.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(String user, char[] password);


  /**
   * @return the session
   */
  public Session getSession();


  /**
   * Starts and commits transactions on this connection, by turning the autocommit
   * flag on and off. 
   * @param autocommit <code>true</code> if the flag is to be on.
   * @throws QueryException The session could not change state.
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException;


  /**
   * @return the autoCommit value
   */
  public boolean getAutoCommit();


  /**
   * Closes the current connection.
   */
  public void close() throws QueryException;
}