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
 * An AST element for controlling transactions.  These commands are considered
 * local, as they do not establish a new connection to a server.  However, if
 * there are any known connections in the current transaction, then the
 * command will update them.
 *
 * @created 2007-08-09
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class TransactionCommand extends LocalCommand {

  /**
   * Indicates that the command modifies the state in a transaction.
   * @return <code>true</code> If the transaction state is to be modified.
   */
  public final boolean isTxCommitRollback() {
    return true;
  }
  
  /**
   * Indicates that this operation is not specific to a UI.
   * @return <code>false</code> as operation is not specific to UIs.
   */
  public boolean isUICommand() {
    return false;
  }

  /**
   * Indicates that this command cannot return an Answer
   * @return <code>false</code>.
   */
  public boolean isAnswerable() {
    return false;
  }

  /**
   * Requests a server URI for this operation.  None available, as it
   * operates on the local connection.
   * @return <code>null</code>
   */
  public URI getServerURI() {
    return null;
  }

}
