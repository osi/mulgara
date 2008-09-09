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

package org.mulgara.protocol.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.mulgara.connection.Connection;
import org.mulgara.connection.SessionConnection;
import org.mulgara.protocol.StreamedAnswer;
import org.mulgara.protocol.StreamedSparqlJSONAnswer;
import org.mulgara.protocol.StreamedSparqlJSONObject;
import org.mulgara.protocol.StreamedSparqlXMLAnswer;
import org.mulgara.protocol.StreamedSparqlXMLObject;
import org.mulgara.query.Answer;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.operation.Command;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.SessionFactoryProvider;
import org.mulgara.sparql.SparqlInterpreter;
import org.mulgara.util.functional.C;
import org.mulgara.util.functional.Fn1E;
import org.mulgara.util.functional.Fn2;

/**
 * A query gateway for SPARQL.
 *
 * @created Sep 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class SparqlServlet extends HttpServlet {

  /** Serialization ID */
  private static final long serialVersionUID = -830221563286477537L;

  /**
   * Internal type definition of a function that takes "something" and an output stream,
   * and returns a {@link StreamedAnswer}
   */
  private interface StreamConstructor<T> extends Fn2<T,OutputStream,StreamedAnswer> { }

  /**
   * Internal type definition of a function that takes an Answer and an output stream,
   * and returns a {@link StreamedAnswer}
   */
  private interface AnswerStreamConstructor extends StreamConstructor<Answer> { }

  /**
   * Internal type definition of a function that takes an Object and an output stream,
   * and returns a {@link StreamedAnswer}
   */
  private interface ObjectStreamConstructor extends StreamConstructor<Object> { }

  /** The parameter identifying the query. */
  private static final String QUERY_ARG = "query";

  /** The parameter identifying the graph. */
  private static final String DEFAULT_GRAPH_ARG = "default-graph-uri";

  /** The parameter identifying the graph. We don't set these in SPARQL yet. */
  @SuppressWarnings("unused")
  private static final String NAMED_GRAPH_ARG = "named-graph-uri";

  /** The parameter identifying the output type. */
  private static final String OUTPUT_ARG = "out";

  /** The output parameter value for indicating JSON output. */
  private static final String OUTPUT_JSON = "json";

  /** The output parameter value for indicating XML output. */
  private static final String OUTPUT_XML = "xml";

  /** The default output type to use. */
  private static final String DEFAULT_OUTPUT_TYPE = OUTPUT_XML;

  /** The content type of the results. */
  private static final String CONTENT_TYPE = "application/sparql-results+xml";

  /** Session value for database connection. */
  private static final String CONNECTION = "session.connection";

  /** Session value for interpreter. */
  private static final String INTERPRETER = "session.interpreter";

  /** The server for finding a session factory. */
  private SessionFactoryProvider server;

  /** Session factory for accessing the database. */
  private SessionFactory cachedSessionFactory;

  /** This object maps request types to the constructors for that output. */
  private static final Map<String,AnswerStreamConstructor> streamBuilders = new HashMap<String,AnswerStreamConstructor>();

  /** This object maps request types to the constructors for sending objects to that output. */
  private static final Map<String,ObjectStreamConstructor> objectStreamBuilders = new HashMap<String,ObjectStreamConstructor>();

  static {
    AnswerStreamConstructor jsonBuilder = new AnswerStreamConstructor() {
      public StreamedAnswer fn(Answer ans, OutputStream s) { return new StreamedSparqlJSONAnswer(ans, s); }
    };
    AnswerStreamConstructor xmlBuilder = new AnswerStreamConstructor() {
      public StreamedAnswer fn(Answer ans, OutputStream s) { return new StreamedSparqlXMLAnswer(ans, s); }
    };
    streamBuilders.put(OUTPUT_JSON, jsonBuilder);
    streamBuilders.put(OUTPUT_XML, xmlBuilder);
    streamBuilders.put(null, xmlBuilder);

    ObjectStreamConstructor jsonObjBuilder = new ObjectStreamConstructor() {
      public StreamedAnswer fn(Object o, OutputStream s) { return new StreamedSparqlJSONObject(o, s); }
    };
    ObjectStreamConstructor xmlObjBuilder = new ObjectStreamConstructor() {
      public StreamedAnswer fn(Object o, OutputStream s) { return new StreamedSparqlXMLObject(o, s); }
    };
    objectStreamBuilders.put(OUTPUT_JSON, jsonObjBuilder);
    objectStreamBuilders.put(OUTPUT_XML, xmlObjBuilder);
    objectStreamBuilders.put(null, xmlObjBuilder);
  }


  /**
   * Creates the servlet for communicating with the given server.
   * @param server The server that provides access to the database.
   */
  public SparqlServlet(SessionFactoryProvider server) throws IOException {
    this.cachedSessionFactory = null;
    this.server = server;
  }


  /**
   * Respond to a request for the servlet.
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String queryStr = req.getParameter(QUERY_ARG);
    try {
      Query query = getQuery(queryStr, req);
  
      Answer result = executeQuery(query, req);

      String outputType = req.getParameter(OUTPUT_ARG);
      sendAnswer(result, outputType, resp);

      try {
        result.close();
      } catch (TuplesException e) {
        throw new InternalErrorException("Error closing: " + e.getMessage());
      }

    } catch (ServletException e) {
      e.sendResponseTo(resp);
    }
  }


  /**
   * Respond to a request for the servlet. This may handle update queries, though SPARQL
   * is not yet standardized to handle these yet.
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String queryStr = req.getParameter(QUERY_ARG);
    try {
      Command cmd = getCommand(queryStr, req);
  
      Object result = executeCommand(cmd, req);

      String outputType = req.getParameter(OUTPUT_ARG);
      if (result instanceof Answer) {
        sendAnswer((Answer)result, outputType, resp);
      } else {
        sendStatus(result, outputType, resp);
      }

    } catch (ServletException e) {
      e.sendResponseTo(resp);
    }
  }


  /**
   * Provide a description for the servlet.
   * @see javax.servlet.GenericServlet#getServletInfo()
   */
  public String getServletInfo() {
    return "Mulgara SPARQL Query Endpoint";
  }


  /**
   * Converts a SPARQL query string into a Query object. This uses extra parameters from the
   * client where appropriate, such as the default graph.
   * @param query The query string issued by the client.
   * @param req The request from the client.
   * @return A new Query object, built from the query string.
   * @throws BadRequestException Due to an invalid command string.
   */
  Query getQuery(String query, HttpServletRequest req) throws BadRequestException {
    if (query == null) throw new BadRequestException("Query must be supplied");
    try {
      SparqlInterpreter interpreter = getInterpreter(req);
      interpreter.setDefaultGraphUris(getRequestedDefaultGraphs(req));
      return interpreter.parseQuery(query);
    } catch (Exception e) {
      throw new BadRequestException(e.getMessage());
    }
  }


  /**
   * Converts a SPARQL query to a Command. For normal SPARQL this will be a Query,
   * but SPARQL Update may create other command types.
   * @param cmd The command string.
   * @param req The client request object.
   * @return The Command object specified by the cmd string.
   * @throws BadRequestException Due to an invalid command string.
   */
  Command getCommand(String cmd, HttpServletRequest req) throws BadRequestException {
    if (cmd == null) throw new BadRequestException("Command must be supplied");
    try {
      SparqlInterpreter interpreter = getInterpreter(req);
      interpreter.setDefaultGraphUris(getRequestedDefaultGraphs(req));
      return interpreter.parseCommand(cmd);
    } catch (Exception e) {
      throw new BadRequestException(e.getMessage());
    }
  }


  /**
   * Execute a query on the database, and return the {@link Answer}.
   * @param query The query to run.
   * @param req The client request object.
   * @return An Answer containing the results of the query.
   * @throws ServletException Due to an error executing the query.
   * @throws IOException If there was an error establishing a connection.
   */
  Answer executeQuery(Query query, HttpServletRequest req) throws ServletException, IOException {
    try {
      return query.execute(getConnection(req));
    } catch (IllegalStateException e) {
      throw new ServiceUnavailableException(e.getMessage());
    } catch (QueryException e) {
      throw new InternalErrorException(e.getMessage());
    } catch (TuplesException e) {
      throw new InternalErrorException(e.getMessage());
    }
  }


  /**
   * Execute a command on the database, and return whatever the result is.
   * @param cmd The command to run.
   * @param req The client request object.
   * @return An Object containing the results of the query.
   * @throws ServletException Due to an error executing the query.
   * @throws IOException If there was an error establishing a connection.
   */
  Object executeCommand(Command cmd, HttpServletRequest req) throws ServletException, IOException {
    try {
      return cmd.execute(getConnection(req));
    } catch (IllegalStateException e) {
      throw new ServiceUnavailableException(e.getMessage());
    } catch (Exception e) {
      throw new InternalErrorException(e.getMessage());
    }
  }


  /**
   * Sends an Answer back to a client, using the request protocol.
   * @param answer The answer to send to the client.
   * @param outputType The protocol requested by the client.
   * @param resp The response object for communicating with the client.
   * @throws IOException Due to a communications error with the client.
   * @throws BadRequestException Due to a bad protocol type.
   * @throws InternalErrorException Due to an error accessing the answer.
   */
  void sendAnswer(Answer answer, String outputType, HttpServletResponse resp) throws IOException, BadRequestException, InternalErrorException {
    send(streamBuilders, answer, outputType, resp);
  }


  /**
   * Writes information to the client stream. This is a general catch-all for non-answer
   * information.
   * @param result The data to return to the client.
   * @param outputType The requested format for the response.
   * @param resp The object for responding to a client.
   * @throws IOException Due to an error communicating with the client.
   * @throws BadRequestException Due to a bad protocol type.
   * @throws InternalErrorException Due to an error accessing the result.
   */
  void sendStatus(Object result, String outputType, HttpServletResponse resp) throws IOException, BadRequestException, InternalErrorException {
    send(objectStreamBuilders, result, outputType, resp);
  }


  /**
   * Sends an result back to a client, using the requested protocol.
   * @param <T> The type of the data that is to be streamed to the client.
   * @param builders The map of protocol types to the objects that implement streaming for
   *        that protocol.
   * @param data The result to send to the client.
   * @param type The protocol type to use when talking to the client.
   * @param resp The respons object for talking to the client.
   * @throws IOException Due to a communications error with the client.
   * @throws BadRequestException Due to a bad protocol type.
   * @throws InternalErrorException Due to an error accessing the answer.
   */
  <T> void send(Map<String,? extends StreamConstructor<T>> builders, T data, String type, HttpServletResponse resp) throws IOException, BadRequestException, InternalErrorException {
    resp.setContentType(CONTENT_TYPE);
    resp.setHeader("pragma", "no-cache");

    // establish the output type
    if (type == null) type = DEFAULT_OUTPUT_TYPE;
    else type = type.toLowerCase();

    // get the constructor for the stream outputter
    StreamConstructor<T> constructor = builders.get(type);
    if (constructor == null) throw new BadRequestException("Unknown result type: " + type);

    try {
      OutputStream out = resp.getOutputStream();
      constructor.fn(data, out).emit();
      out.close();
    } catch (IOException ioe) {
      // There's no point in telling the client if we can't talk to the client
      throw ioe;
    } catch (Exception e) {
      throw new InternalErrorException(e.getMessage());
    }
  }


  /**
   * Gets the SPARQL interpreter for the current session,
   * creating it if it doesn't exist yet.
   * @param req The current request environment.
   * @return A connection that is tied to this HTTP session.
   */
  private SparqlInterpreter getInterpreter(HttpServletRequest req) {
    HttpSession httpSession = req.getSession();
    SparqlInterpreter interpreter = (SparqlInterpreter)httpSession.getAttribute(INTERPRETER);
    if (interpreter == null) {
      interpreter = new SparqlInterpreter();
      httpSession.setAttribute(INTERPRETER, interpreter);
    }
    return interpreter;
  }


  /**
   * Gets the default graphs the user requested.
   * @param req The request object from the user.
   * @return A list of URIs for graphs. This may be null if no URIs were requested.
   * @throws BadRequestException If a graph name was an invalid URI.
   */
  private List<URI> getRequestedDefaultGraphs(HttpServletRequest req) throws BadRequestException {
    String[] defaults = req.getParameterValues(DEFAULT_GRAPH_ARG);
    if (defaults == null) return null;
    try {
      return C.map(defaults, new Fn1E<String,URI,URISyntaxException>(){public URI fn(String s)throws URISyntaxException{return new URI(s);}});
    } catch (URISyntaxException e) {
      throw new BadRequestException("Invalid URI. " + e.getMessage());
    }
  }


  /**
   * Gets the connection for the current session, creating it if it doesn't exist yet.
   * @param req The current request environment.
   * @return A connection that is tied to this HTTP session.
   * @throws IOException When an error occurs creating a new session.
   */
  private Connection getConnection(HttpServletRequest req) throws IOException, IllegalStateException {
    HttpSession httpSession = req.getSession();
    Connection connection = (Connection)httpSession.getAttribute(CONNECTION);
    if (connection == null) {
      try {
        connection = new SessionConnection(getSessionFactory().newSession(), null, null);
      } catch (QueryException qe) {
        throw new IOException("Unable to create a connection to the database. " + qe.getMessage());
      }
      httpSession.setAttribute(CONNECTION, connection);
    }
    return connection;
  }


  /**
   * This method allows us to put off getting a session factory until the server is
   * ready to provide one.
   * @return A new session factory.
   */
  private SessionFactory getSessionFactory() throws IllegalStateException {
    if (cachedSessionFactory == null) {
      cachedSessionFactory = server.getSessionFactory();
      if (cachedSessionFactory == null) throw new IllegalStateException("Server not yet ready. Try again soon.");
    }
    return cachedSessionFactory;
  }

}
