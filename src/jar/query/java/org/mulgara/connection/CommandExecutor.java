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
package org.mulgara.connection;

import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.operation.Command;
import org.mulgara.query.operation.Load;


/**
 * A central point to direct to commands on a connection.
 *
 * @created Feb 22, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class CommandExecutor implements Connection {

  /**
   * @see org.mulgara.connection.Connection#execute(org.mulgara.query.operation.Command)
   */
  public String execute(Command cmd) throws Exception {
    return (String)cmd.execute(this);
  }

  /**
   * @see org.mulgara.connection.Connection#execute(org.mulgara.query.operation.Load)
   */
  public Long execute(Load cmd) throws QueryException {
    return (Long)cmd.execute(this);
  }

  /**
   * @see org.mulgara.connection.Connection#execute(org.mulgara.query.Query)
   */
  public Answer execute(Query cmd) throws QueryException, TuplesException {
    return (Answer)cmd.execute(this);
  }

}