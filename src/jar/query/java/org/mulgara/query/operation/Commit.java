/*
 * Copyright 2009 DuraSpace.
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

package org.mulgara.query.operation;


import org.mulgara.connection.Connection;
import org.mulgara.query.QueryException;
import org.mulgara.server.Session;


/**
 * An AST element for the COMMIT command. Not called directly for a TQL commit, as that
 * would have to go to all open connections. Consequently, TqlAutoInterpreter handles
 * commits through a different method to the standard call path, while the normal
 * call will pass in a {@link org.mulgara.connection.DummyConnection}.
 *
 * After performing a commit, all connections are left in place and
 * the transaction is kept open {@link #stayInTx()}.
 * 
 * @created 2007-08-09
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Commit extends TransactionCommand implements Command, TxOp {
  
  /**
   * Commits the transaction on a connection.
   * @param conn Contains the session to commit. 
   * @throws QueryException There was a server error commiting the transaction.
   */
  public Object execute(Connection conn) throws QueryException {
    Session session = conn.getSession();
    if (session != null) {
      session.commit();
      return setResultMessage("Successfully committed transaction");
    } else {
      assert conn instanceof org.mulgara.connection.DummyConnection;
      return setResultMessage("Skipped commit for internal connection");
    }
  }


  /**
   * {@inheritDoc}
   */
  public boolean stayInTx() {
    return true;
  }


  /**
   * Sets message text relevant to the operation.  Exposes this publicly, but only for internal use.
   * @return The set text.
   */
  public String setResultMessage(String resultMessage) {
    return super.setResultMessage(resultMessage);
  }
}
