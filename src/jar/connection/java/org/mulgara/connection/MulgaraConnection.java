package org.mulgara.connection;

import org.mulgara.query.Answer;

/**
 * A connection to a Mulgara store through which to send textual commands.
 * <p>
 * {@link MulgaraConnection} is the (Java) interface that query-oriented interfaces should use to connect to a Mulgara store.
 * </p>
 *
 * @created 2005-03-31
 * @author Tom Adams
 * @version $Revision: 1.3 $
 * @modified $Date: 2005/04/03 10:22:46 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public interface MulgaraConnection {

  /**
   * Executes a command that affects a change on the database and returns no results.
   *
   * @param command The command to execute.
   * @throws InvalidQuerySyntaxException If the syntax of the <code>command</code> is incorrect.
   * @throws MulgaraConnectionException If an error occurs while executing the command.
   */
  void executeUpdate(String command) throws InvalidQuerySyntaxException, MulgaraConnectionException;

  /**
   * Executes a query that returns results.
   *
   * @param query The query to execute.
   * @return The answer to the query, will never be <code>null</code>.
   * @throws InvalidQuerySyntaxException If the syntax of the <code>query</code> is incorrect.
   * @throws MulgaraConnectionException If an error occurs while executing the query.
   */
  Answer executeQuery(String query) throws InvalidQuerySyntaxException, MulgaraConnectionException;

  /**
   * Closes the connection to the Mulgara store.
   *
   * @throws MulgaraConnectionException If an error occurs while closing the connection.
   */
  void close() throws MulgaraConnectionException;
}
