package org.mulgara.connection;

import java.util.HashMap;

import org.mulgara.connection.param.MethodParameterUtil;
import org.mulgara.itql.ItqlInterpreter;
import org.mulgara.query.Query;

/**
 * Builds iTQL queries in {@link String} form into {@link Query} objects.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-03
 * @modified $Date: 2005/04/04 11:30:10 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ItqlQueryBuilder implements QueryBuilder {

  /**
   * Builds an iTQL query in {@link String} form into a {@link org.mulgara.query.Query}.
   *
   * @param query The query in {@link String} form of the query.
   * @return The <code>query</code> in {@link org.mulgara.query.Query} form.
   * @throws InvalidQuerySyntaxException If the syntax of the <code>query</code> is incorrect.
   */
  public Query buildQuery(String query) throws InvalidQuerySyntaxException {
    MethodParameterUtil.checkNotEmptyString("query", query);
    try {
      return new ItqlInterpreter(new HashMap()).parseQuery(query);
    } catch (Exception e) {
      throw new InvalidQuerySyntaxException("Unable to build query from string: "+query, e);
    }
  }
}
