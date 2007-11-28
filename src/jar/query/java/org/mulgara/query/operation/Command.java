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

package org.mulgara.query.operation;

import java.net.URI;

import org.mulgara.connection.Connection;

/**
 * A general Abstract Syntax Tree for TQL commands.
 *
 * @created 2007-08-09
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface Command {

  /**
   * Indicates that the command modifies the state in a transaction.
   * @return <code>true</code> If the transaction state is to be modified.
   */
  boolean isTxCommitRollback();
  
  /**
   * Indicates if an AST element is an operation for a local client.
   * Local operations do not create a connection to a server, though they
   * can modify the state of existing connections.
   * @return <code>true</code> if the operation is only relevant to a client.
   */
  boolean isLocalOperation();
  
  /**
   * Indicates if an AST represents a command for a user interface.
   * @return <code>true</code> if the operation is only relevant to a user interface
   */
  boolean isUICommand();
  
  /**
   * Gets the associated server for a non-local operation.
   * @return the server URI if one can be determined,
   *  or <code>null</code> if not a valid operation.
   * @throws UnsupportedOperationException If this command is local only.
   */
  URI getServerURI() throws UnsupportedOperationException;

  /**
   * Executes the operation. This is highly specific to each operation.
   * @return Data specific to the operation.
   * @throws Exception specific to the operation.
   */
  Object execute(Connection conn) throws Exception;

  /**
   * Gets a message text relevant to the operation.  Useful for the UI.
   * @return A text message associated with the result of this
   * operation.
   */
  String getResultMessage();

  /**
   * Indicates that this command returns an Answer. Saves the overhead of checking
   * the return type of execute.
   * @return <code>true</code> if the result of execute is an Answer.
   */
  boolean isAnswerable();
}
