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

/**
 * An AST element for non-server commands.
 *
 * @created 2007-08-09
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class LocalCommand implements Command {

  /** The message set by the result of this command. */
  private String resultMessage = "";

  /**
   * Indicates that the command modifies the state in a transaction.
   * @return <code>true</code> If the transaction state is to be modified.
   */
  public boolean isTxCommitRollback() {
    return false;
  }
  
  /**
   * Indicates that this operation is local.
   * @return Always <code>true</code> to indicate this command is local.
   */
  public final boolean isLocalOperation() {
    return true;
  }

  /**
   * Queries if this command is a request to quit.
   * @return <code>false</code> for most operations, but <code>true</code> if quitting.
   */
  public boolean isQuitCommand() {
    return false;
  }


  /**
   * Indicates that this command cannot return an Answer.
   * @return <code>false</code> by default.
   */
  public boolean isAnswerable() {
    return false;
  }


  /**
   * Gets the associated server for a non-local operation.
   * @return <code>null</code>
   */
  public URI getServerURI() {
    return null;
  }
  
  /**
   * Executes the operation. This is highly specific to each operation.
   * @return Data specific to the operation.
   * @throws Exception specific to the operation.
   */
  public Object execute() throws Exception {
    return execute(null);
  }
  
  /**
   * Gets a message text relevant to the operation.  Useful for the UI.
   * @return A text message associated with the result of this
   * operation.
   */
  public String getResultMessage() {
    return resultMessage;
  }

  /**
   * Sets message text relevant to the operation.  Useful for the UI.
   * @return The set text.
   */
  String setResultMessage(String resultMessage) {
    return this.resultMessage = resultMessage;
  }
}
