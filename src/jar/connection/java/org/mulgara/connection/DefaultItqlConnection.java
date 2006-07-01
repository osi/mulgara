package org.mulgara.connection;

import java.net.URI;

import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.server.Session;

/**
 * Default implementation of an iTQL connection to a Mulgara store.
 *
 * @author Tom Adams
 * @version $Revision: 1.4 $
 * @created 2005-04-01
 * @modified $Date: 2005/04/04 11:30:10 $
 * @copyright &copy; 2005 <a href="http://www.mulgara.org/">Mulgara Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class DefaultItqlConnection implements ItqlConnection {

  // FIXME: Make connections threadsafe.

  private Session session;
  private URI securityDomain;

  /**
   * Creates a new iTQL connection to a Mulgara server.
   *
   * @param session The session to use to communicate to the Mulgara server.
   * @param securityDomain The security domain of the Mulgara server.
   */
  public DefaultItqlConnection(Session session, URI securityDomain) {
    this.session = session;
    this.securityDomain = securityDomain;
  }

  /**
   * Executes a command that affects a change on the database and returns no results.
   *
   * @param command The command to execute.
   * @throws InvalidQuerySyntaxException If the syntax of the query is incorrect.
   * @throws MulgaraConnectionException If an error occurs while executing the command.
   */
  public void executeUpdate(String command) throws InvalidQuerySyntaxException, MulgaraConnectionException {
    checkStringParam("command", command);
    throw new UnsupportedOperationException("Implement me...");
  }

  /**
   * Executes a query that returns results.
   *
   * @param query The query to execute.
   * @return The answer to the query, will never be <code>null</code>.
   * @throws InvalidQuerySyntaxException If the syntax of the query is incorrect.
   * @throws MulgaraConnectionException If an error occurs while executing the query.
   */
  public Answer executeQuery(String query) throws InvalidQuerySyntaxException, MulgaraConnectionException {
    checkStringParam("query", query);
    throw new UnsupportedOperationException("Implement me...");
  }

  /**
   * Closes the connection to the Mulgara store.
   * <p>
   * Calling this method will close the underlying {@link Session}, making it unusable for future use.
   * </p>
   *
   * @throws MulgaraConnectionException If an error occurs while closing the connection.
   */
  public void close() throws MulgaraConnectionException {
    try {
      session.close();
    } catch (QueryException e) {
      throw new MulgaraConnectionException("Unable to close underlying session", e);
    }
  }

  // FIXME: Delete this.
  private static void checkStringParam(String name, String param) {
    checkParamForNull(name, param);
    checkParamForEmptyString(name, param);
  }

  // FIXME: Delete this.
  private static void checkParamForNull(String name, Object param) {
    if (param == null) throw new IllegalArgumentException(name+" parameter cannot be null");
  }

  // FIXME: Delete this.
  private static void checkParamForEmptyString(String name, String param) {
    if (param.trim().length() == 0) throw new IllegalArgumentException(name+" parameter cannot be empty");
  }
}
