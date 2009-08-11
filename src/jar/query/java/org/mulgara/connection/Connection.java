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
package org.mulgara.connection;

import java.net.URI;

import org.mulgara.query.Answer;
import org.mulgara.query.AskQuery;
import org.mulgara.query.BooleanAnswer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.server.Session;
import org.mulgara.query.operation.*;

/**
 * This interface abstracts connections to a server, holding any information relevant to that
 * connection.  For those operations that are to be performed on a server, this interface
 * is used to send the operations.  Other operations can be kept local, but the mechanism
 * appears the same to the user, thereby abstracting away the interaction that each command
 * has with servers.
 *
 * The preferred method for instantiating a Connection is using the {@link ConnectionFactory}
 * class.  The ConnectionFactory allows for re-use of underlying resources when connecting to
 * servers using the server URI.  It is synchronized for use by multiple clients in a 
 * multi-threaded environment; connections obtained concurrently by separate clients from the 
 * same factory will not interfere with each other.  Note that while the factory is synchronized
 * for concurrent access, the connection itself is not and should only be accessed by a single
 * thread.  When a client connection is no longer in use, its underlying resources are returned
 * to the factory for re-use by other clients.
 * 
 * Connections which are no longer in use should be closed using the {@link #close()} method.
 * Calling this method allows the underlying session backing the connection to be released back
 * to the factory for re-use by other clients.  This will result in increased performance in an
 * environment where there are many short-lived connections in use.  Since the session stores
 * credentials that are passed to the connection, a factory should only be used to cache
 * connections in a single-user environment.  Alternatively, the {@link #dispose()} method may
 * be used to explicitly destroy the underlying session, in which case it will not be cached and
 * re-used by the factory.  Calling either {@link #close()} or {@link #dispose()} will cause the
 * connection to be invalidated, and any subsequent attempts to execute an operation on it will
 * cause an exception to be thrown.
 *
 * {@link org.mulgara.query.operation.Command}s to be issued may be executed with a Connection
 * as a parameter, or can be passed to a Connection.  The appropriate use depends on the usage.
 * When creating commands in code, the preferred idiom is to pass to a Connection.  For instance:
 * <pre><code>
 *  ConnectionFactory factory = new ConnectionFactory();
 *  Connection conn = factory.newConnection(URI.create("rmi://localhost/server1"));
 *
 *  URI graph = URI.create("rmi://localhost/server1#graph");
 *  Command create = new CreateGraph(graph);
 *  Command load = new Load(new File("data.rdf").toURI(), graph, false);
 *  Command query = new TqlInterpreter().parseCommand(queryString);
 *
 *  conn.execute(create);
 *  conn.execute(load);
 *  Answer answer = conn.execute(query);
 * </code></pre>
 * 
 * This mechanism has the advantage of returning the appropriate type for each type of command
 * For instance, {@link Query} commands return an {@link Answer}, while most other commands
 * return a status string.
 *
 * Alternatively, if the commands are being generated by a query language parser, then the command
 * operation should be picked up via polymorphism by calling execute on the command instead.
 * <pre><code>
 *  ConnectionFactory factory = new ConnectionFactory();
 *  Connection conn = factory.newConnection(URI.create("rmi://localhost/server1"));
 *  
 *  Interpreter interpreter = new TqlInterpreter();
 *  Command cmd = interpreter.parseCommand(commandString);
 *  
 *  Object result = cmd.execute(conn);
 * </code></pre>
 * 
 * @created 2007-09-25
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface Connection {

  /**
   * Give login credentials and security domain to the current session.  This should only be needed
   * once since the session does not change.
   * @param securityDomainUri The security domain for the login.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(URI securityDomainUri, String user, char[] password);
  

  /**
   * Give login credentials for the current security domain to the current session.
   * This should only be needed
   * once since the session does not change.
   * @param user The username.
   * @param password The password for the given username.
   */
  public void setCredentials(String user, char[] password);


  /**
   * @return the session
   */
  public Session getSession();


  /**
   * Tests if this connection is over a network protocol.
   * @return <code>true</code> if this connection is being executed over a network protocol.
   */
  public boolean isRemote();


  /**
   * Starts and commits transactions on this connection, by turning the autocommit
   * flag on and off. 
   * @param autoCommit <code>true</code> if the flag is to be on.
   * @throws QueryException The session could not change state.
   */
  public void setAutoCommit(boolean autoCommit) throws QueryException;


  /**
   * @return the autoCommit value
   */
  public boolean getAutoCommit();


  /**
   * Closes the current connection.
   */
  public void close() throws QueryException;
  
  /**
   * Closes the current connection, disposing of any underlying resources rather
   * than returning them to the factory for re-use.
   */
  public void dispose() throws QueryException;

  // Central execution of Command operations

  /**
   * Generic command execution method.
   * @param cmd The command to execute.
   * @return A status message
   * @throws Exception A general exception catchall
   */
  public String execute(Command cmd) throws Exception;

  /**
   * Loads data from a file or URL
   * @param cmd The command to load the data
   * @return The number of loaded statements
   */
  public Long execute(Load cmd) throws QueryException;

  /**
   * Issues a query on the connection.
   * @param cmd The command to issue the query.
   * @return An Answer with the query results.
   */
  public Answer execute(Query cmd) throws QueryException, TuplesException;

  /**
   * Issues an ASK query on the connection.
   * @param cmd The ASK command to issue the query.
   * @return A BooleanAnswer with the true/false result of the query.
   */
  public BooleanAnswer execute(AskQuery cmd) throws QueryException, TuplesException;

}