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

package org.mulgara.connection;

import org.mulgara.query.Answer;
import org.mulgara.query.AskQuery;
import org.mulgara.query.BooleanAnswer;
import org.mulgara.query.ConstructQuery;
import org.mulgara.query.GraphAnswer;
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
    return cmd.execute(this).toString();
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

  /**
   * @see org.mulgara.connection.Connection#execute(org.mulgara.query.AskQuery)
   */
  public BooleanAnswer execute(AskQuery cmd) throws QueryException, TuplesException {
    return (BooleanAnswer)cmd.execute(this);
  }

  /**
   * @see org.mulgara.connection.Connection#execute(org.mulgara.query.AskQuery)
   */
  public GraphAnswer execute(ConstructQuery cmd) throws QueryException, TuplesException {
    return (GraphAnswer)cmd.execute(this);
  }

}
