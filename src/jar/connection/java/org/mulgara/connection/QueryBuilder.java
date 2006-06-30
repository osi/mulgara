package org.mulgara.connection;

import org.mulgara.query.Query;

/**
 * Builds queries in {@link String} form into {@link Query} objects.
 *
 * @author Tom Adams
 * @version $Revision: 1.1 $
 * @created 2005-04-03
 * @modified $Date: 2005/04/03 10:22:46 $
 * @copyright &copy; 2005 <a href="http://www.kowari.org/">Kowari Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface QueryBuilder {

  /**
   * Builds a query in {@link String} form into a {@link Query}, suitable for passing to a
   * {@link org.mulgara.server.Session}.
   *
   * @param query The query in {@link String} form of the query.
   * @return The <code>query</code> in {@link Query} form.
   * @throws InvalidQuerySyntaxException If the syntax of the <code>query</code> is incorrect.
   */
  Query buildQuery(String query) throws InvalidQuerySyntaxException;
}
