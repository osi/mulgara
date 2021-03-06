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

package org.mulgara.query;

import java.util.List;

import org.mulgara.connection.Connection;

/**
 * A query type similar to SELECT, though it may only contain results with multiples of
 * 3 columns, with the following rules:
 * First column: contains URIs or BlankNodes
 * Second column: contains URIs
 * Third column: contains URIs, BlankNodes, or Literals
 *
 * @created Jun 26, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ConstructQuery extends Query {

  /** Required serialization ID */
  private static final long serialVersionUID = -6024259961466362580L;

  public ConstructQuery(List<? extends SelectElement> variableList, GraphExpression graphExpression,
        ConstraintExpression constraintExpression,
        List<Order> orderList, Integer limit, int offset) {
    super(variableList, graphExpression, constraintExpression,
        null, // no having
        orderList,
        limit,
        offset,
        true,
        new UnconstrainedAnswer());
  }

  /**
   * Executes this query on a connection.
   * @param conn The connection to a database session to execute the query against.
   * @return The answer to this query.  This must be closed by the calling code.
   */
  public GraphAnswer execute(Connection conn) throws QueryException, TuplesException {
    // pipe all the query types through the one Session method
    GraphAnswer answer = (GraphAnswer)conn.getSession().query(this);
    if (answer == null) throw new QueryException("Invalid answer received");
    // move to the first row
    answer.beforeFirst();
    return answer;
  }

}
