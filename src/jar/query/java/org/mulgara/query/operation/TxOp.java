/**
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

import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;

/**
 * This interface is for use by TqlAutoInterpreter for managing operations that
 * operate on transactions in some way.
 * 
 * @created Nov 8, 2007
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface TxOp {

  /**
   * Perform the operation for manipulating this transaction.
   * @param s The session to perform the operation on.
   * @return The resulting message.
   * @throws QueryException The operation failed.
   */
  Object execute(Connection conn) throws QueryException;

  /**
   * Indicates if this operation should result in the transaction finishing or continuing.
   * @return <code>true</code> if the transaction stays open, <code>false</code> if it closes.
   */
  boolean stayInTx();

  /**
   * Sets the result message.  This is for internal use only.
   * @param msg A text message associated with the result of this operation.
   */
  String setResultMessage(String msg);
  
  /**
   * Gets the result message.  This will be mixed in from the Command interface.
   * @return A text message associated with the result of this operation.
   */
  String getResultMessage();
  
}
