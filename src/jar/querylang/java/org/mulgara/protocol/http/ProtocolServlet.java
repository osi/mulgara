/*
 * Copyright 2008 Fedora Commons, Inc.
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

package org.mulgara.protocol.http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;

import org.mulgara.connection.Connection;
import org.mulgara.connection.SessionConnection;
import org.mulgara.parser.Interpreter;
import org.mulgara.protocol.StreamedAnswer;
import org.mulgara.query.Answer;
import org.mulgara.query.ConstructQuery;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.operation.Command;
import org.mulgara.query.operation.CreateGraph;
import org.mulgara.query.operation.Load;
import org.mulgara.server.SessionFactory;
import org.mulgara.server.SessionFactoryProvider;
import org.mulgara.util.functional.C;
import org.mulgara.util.functional.Fn1E;
import org.mulgara.util.functional.Fn2;

/**
 * A query gateway for query languages.
 *
 * @created Sep 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public abstract class ProtocolServlet extends HttpServlet {

  /** Generated serialization ID. */
  private static final long serialVersionUID = -6510062000251611536L;

  /**
   * Internal type definition of a function that takes "something" and an output stream,
   * and returns a {@link StreamedAnswer}
   */
  protected interface StreamConstructor<T> extends Fn2<T,OutputStream,StreamedAnswer> { }

  /**
   * Internal type definition of a function that takes an Answer and an output stream,
   * and returns a {@link StreamedAnswer}
   */
  protected interface AnswerStreamConstructor extends StreamConstructor<Answer> { }

  /**
   * Internal type definition of a function that takes an Object and an output stream,
   * and returns a {@link StreamedAnswer}
   */
  protected interface ObjectStreamConstructor extends StreamConstructor<Object> { }

  /** The parameter identifying the query. */
  private static final String QUERY_ARG = "query";

  /** The parameter identifying the output type. */
  protected static final String OUTPUT_ARG = "format";

  /** The header name for accepted mime types. */
  protected static final String ACCEPT_HEADER = "Accept";

  /** The default output type to use. */
  protected static final Output DEFAULT_OUTPUT_TYPE = Output.XML;

  /** The parameter identifying the graph. */
  protected static final String DEFAULT_GRAPH_ARG = "default-graph-uri";

  /** The parameter identifying the graph. We don't set these in SPARQL yet. */
  protected static final String NAMED_GRAPH_ARG = "named-graph-uri";

  /** The name of the default graph. This is a null graph. */
  protected static final URI DEFAULT_NULL_GRAPH = URI.create("sys:null");

  /** An empty graph for those occasions when no graph is set. */
  protected static final List<URI> DEFAULT_NULL_GRAPH_LIST = Collections.singletonList(DEFAULT_NULL_GRAPH);

  /** The content type of the results. */
  protected static final String CONTENT_TYPE = "application/sparql-results+xml";

  /** Session value for database connection. */
  private static final String CONNECTION = "session.connection";

  /** Session value for interpreter. */
  protected static final String INTERPRETER = "session.interpreter";

  /** Posted RDF data content type. */
  protected static final String POSTED_DATA_TYPE = "multipart/form-data;";

  /** The name of the posted data. */
  protected static final String GRAPH_DATA = "graph";

  /** The header used to indicate a statement count. */ //HDR_CANNOT_LOAD
  protected static final String HDR_STMT_COUNT = "Statements-Loaded";

  /** The header used to indicate a part that couldn't be loaded. */
  protected static final String HDR_CANNOT_LOAD = "Cannot-Load";

  /** A made-up scheme for data uploaded through http-put, since http means "downloaded". */
  protected static final String HTTP_PUT_NS = "http-put://upload/";

  /** The server for finding a session factory. */
  private SessionFactoryProvider server;

  /** Session factory for accessing the database. */
  private SessionFactory cachedSessionFactory;

  /** This object maps request types to the constructors for that output. */
  protected final Map<Output,AnswerStreamConstructor> streamBuilders = new EnumMap<Output,AnswerStreamConstructor>(Output.class);

  /** This object maps request types to the constructors for sending objects to that output. */
  protected final Map<Output,ObjectStreamConstructor> objectStreamBuilders = new EnumMap<Output,ObjectStreamConstructor>(Output.class);

  /**
   * Creates the servlet for communicating with the given server.
   * @param server The server that provides access to the database.
   */
  public ProtocolServlet(SessionFactoryProvider server) throws IOException {
    this.cachedSessionFactory = null;
    this.server = server;
    initializeBuilders();
  }


  /**
   * Initialize the functional mappings of output types to the objects that manage them.
   */
  abstract protected void initializeBuilders();

  /**
   * Respond to a request for the servlet.
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String queryStr = req.getParameter(QUERY_ARG);
    try {
      Query query = getQuery(queryStr, req);
  
      Answer result = executeQuery(query, req);

      Output outputType = getOutputType(req, query);
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
   * Respond to a request for the servlet. This may handle update queries.
   * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
   */
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    String type = req.getContentType();
    try {
      if (type != null && type.startsWith(POSTED_DATA_TYPE)) handleDataUpload(req, resp);
      else handleUpdateQuery(req, resp);
    } catch (ServletException e) {
      e.sendResponseTo(resp);
    }
  }


  /**
   * Provide a description for the servlet.
   * @see javax.servlet.GenericServlet#getServletInfo()
   */
  public abstract String getServletInfo();


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
      Interpreter interpreter = getInterpreter(req);
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
      Interpreter interpreter = getInterpreter(req);
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
  void sendAnswer(Answer answer, Output outputType, HttpServletResponse resp) throws IOException, BadRequestException, InternalErrorException {
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
  void sendStatus(Object result, Output outputType, HttpServletResponse resp) throws IOException, BadRequestException, InternalErrorException {
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
  <T> void send(Map<Output,? extends StreamConstructor<T>> builders, T data, Output type, HttpServletResponse resp) throws IOException, BadRequestException, InternalErrorException {
    resp.setContentType(CONTENT_TYPE);
    resp.setHeader("pragma", "no-cache");

    // establish the output type
    if (type == null) type = DEFAULT_OUTPUT_TYPE;

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
   * Uploads data into a graph.
   * @param req The object containing the client request to upload data.
   * @param resp The object to respond to the client with.
   * @throws IOException If an error occurs when communicating with the client.
   */
  protected void handleDataUpload(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    try {
      // parse in the data to be uploaded
      MimeMultiNamedPart mime = new MimeMultiNamedPart(new ServletDataSource(req, GRAPH_DATA));

      // validate the request
      if (mime.getCount() == 0) throw new BadRequestException("Request claims to have posted data, but none was supplied.");

      // Get the destination graph, and ensure it exists
      URI destGraph = getRequestedDefaultGraph(req, mime);
      Connection conn = getConnection(req);
      try {
        new CreateGraph(destGraph).execute(conn);
      } catch (QueryException e) {
        throw new InternalErrorException("Unable to create graph: " + e.getMessage());
      }

      // upload the data
      for (int partNr = 0; partNr < mime.getCount(); partNr++) {
        BodyPart part = mime.getBodyPart(partNr);
        String partName = mime.getPartName(partNr);
        try {
          if (!knownParam(partName)) resp.addHeader(HDR_STMT_COUNT, Long.toString(loadData(destGraph, part, conn)));
        } catch (QueryException e) {
          resp.addHeader(HDR_CANNOT_LOAD, partName);
        }
      }
    } catch (MessagingException e) {
      throw new BadRequestException("Unable to process received MIME data: " + e.getMessage());
    }
    resp.setStatus(SC_NO_CONTENT);
  }


  /**
   * Respond to a request for a query that may update the data.
   * @param req The query request object.
   * @param resp The HTTP response object.
   * @throws IOException If an error occurs when communicating with the client.
   * @throws ServletException 
   */
  protected void handleUpdateQuery(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    String queryStr = req.getParameter(QUERY_ARG);
    Command cmd = getCommand(queryStr, req);

    Object result = executeCommand(cmd, req);

    Output outputType = getOutputType(req, cmd);
    if (result instanceof Answer) {
      sendAnswer((Answer)result, outputType, resp);
    } else {
      sendStatus(result, outputType, resp);
    }
  }


  /**
   * Load MIME data into a graph.
   * @param graph The graph to load into.
   * @param data The data to be loaded, with associated meta-data.
   * @param cxt The connection to the database.
   * @return The number of statements loaded.
   * @throws IOException error reading from the client.
   * @throws BadRequestException Bad data passed to the load request.
   * @throws InternalErrorException A query exception occurred during the load operation.
   */
  protected long loadData(URI graph, BodyPart data, Connection cxt) throws IOException, ServletException, QueryException {
    String contentType = "";
    try {
      contentType = data.getContentType();
      Load loadCmd = new Load(graph, data.getInputStream(), new MimeType(contentType), data.getFileName());
      return (Long)loadCmd.execute(cxt);
    } catch (MessagingException e) {
      throw new BadRequestException("Unable to process data for loading: " + e.getMessage());
    } catch (MimeTypeParseException e) {
      throw new BadRequestException("Bad Content Type in request: " + contentType + " (" + e.getMessage() + ")");
    }
  }


  /**
   * Gets the SPARQL interpreter for the current session,
   * creating it if it doesn't exist yet.
   * @param req The current request environment.
   * @return A connection that is tied to this HTTP session.
   */
  abstract protected Interpreter getInterpreter(HttpServletRequest req) throws BadRequestException;


  /**
   * Gets the default graphs the user requested.
   * @param req The request object from the user.
   * @return A list of URIs for graphs. This may be null if no URIs were requested.
   * @throws BadRequestException If a graph name was an invalid URI.
   */
  protected List<URI> getRequestedDefaultGraphs(HttpServletRequest req) throws BadRequestException {
    String[] defaults = req.getParameterValues(DEFAULT_GRAPH_ARG);
    if (defaults == null) return DEFAULT_NULL_GRAPH_LIST;
    try {
      return C.map(defaults, new Fn1E<String,URI,URISyntaxException>(){public URI fn(String s)throws URISyntaxException{return new URI(s);}});
    } catch (URISyntaxException e) {
      throw new BadRequestException("Invalid URI. " + e.getMessage());
    }
  }


  /**
   * Gets the default graphs the user requested.
   * @param req The request object from the user.
   * @return A list of URIs for graphs. This may be null if no URIs were requested.
   * @throws BadRequestException If a graph name was an invalid URI.
   */
  protected URI getRequestedDefaultGraph(HttpServletRequest req, MimeMultiNamedPart mime) throws ServletException {
    // look in the parameters
    String[] defaults = req.getParameterValues(DEFAULT_GRAPH_ARG);
    if (defaults != null) {
      if (defaults.length != 1) throw new BadRequestException("Multiple graphs requested.");
      try {
        return new URI(defaults[0]);
      } catch (URISyntaxException e) {
        throw new BadRequestException("Invalid URI. " + e.getInput());
      }
    }
    // look in the mime data
    if (mime != null) {
      try {
        String result = mime.getParameterString(DEFAULT_GRAPH_ARG);
        if (result != null) {
          try {
            return new URI(result);
          } catch (URISyntaxException e) {
            throw new BadRequestException("Bad graph URI: " + result);
          }
        }
      } catch (Exception e) {
        throw new BadRequestException("Bad MIME data: " + e.getMessage());
      }
    }
    return DEFAULT_NULL_GRAPH;
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


  /**
   * Compare a parameter name to a set of known parameter names.
   * @param name The name to check.
   * @return <code>true</code> if the name is known. <code>false</code> if not known or <code>null</code>.
   */
  private boolean knownParam(String name) {
    final String[] knownParams = new String[] { DEFAULT_GRAPH_ARG, NAMED_GRAPH_ARG, GRAPH_DATA };
    for (String p: knownParams) if (p.equalsIgnoreCase(name)) return true;
    return false;
  }


  /**
   * Determine the type of response we need.
   * @param req The request object for the servlet connection.
   * @return xml, json, rdfXml or rdfN3.
   */
  private Output getOutputType(HttpServletRequest req, Command cmd) {
    Output type = DEFAULT_OUTPUT_TYPE;

    // get the accepted types
    String accepted = req.getHeader(ACCEPT_HEADER);
    if (accepted != null) {
      // if this is a known type, then return it
      Output t = Output.forMime(accepted);
      if (t != null) type = t;
    }

    // check the URI parameters
    String reqOutputName = req.getParameter(OUTPUT_ARG);
    if (reqOutputName != null) {
      Output reqOutput = Output.valueOf(reqOutputName.toUpperCase());
      if (reqOutput != null) type = reqOutput;
    }

    // need graph types if constructing a graph
    if (cmd instanceof ConstructQuery) {
      if (type == Output.XML) type = Output.RDFXML;
    } else {
      if (type == Output.RDFXML || type == Output.N3) type = Output.XML;
    }

    return type;
  }


  /**
   * Enumeration of the various output types, depending on mime type.
   */
  enum Output {
    XML("application/sparql-results+xml"),
    JSON("application/sparql-results+json"),
    RDFXML("application/rdf+xml"),
    N3("text/rdf+n3");

    final String mimeText;
    private Output(String mimeText) { this.mimeText = mimeText; }

    static private Map<String,Output> outputs = new HashMap<String,Output>();
    static {
      for (Output o: Output.values()) outputs.put(o.mimeText, o);
    }
    
    static Output forMime(String mimeText) { return outputs.get(mimeText); }
  }
}
