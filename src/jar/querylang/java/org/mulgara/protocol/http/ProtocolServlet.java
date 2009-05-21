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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import static javax.servlet.http.HttpServletResponse.SC_NO_CONTENT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;

import org.apache.log4j.Logger;
import org.jrdf.graph.ObjectNode;
import org.jrdf.graph.URIReference;
import org.mulgara.connection.Connection;
import org.mulgara.parser.Interpreter;
import org.mulgara.protocol.StreamedAnswer;
import org.mulgara.query.Answer;
import org.mulgara.query.ConstructQuery;
import org.mulgara.query.Query;
import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.query.operation.Command;
import org.mulgara.query.operation.CreateGraph;
import org.mulgara.query.operation.Deletion;
import org.mulgara.query.operation.DropGraph;
import org.mulgara.query.operation.Insertion;
import org.mulgara.query.operation.Load;
import org.mulgara.server.ServerInfo;
import org.mulgara.server.SessionFactoryProvider;
import org.mulgara.util.functional.C;
import org.mulgara.util.functional.Fn1E;
import org.mulgara.util.functional.Fn2;
import org.mulgara.util.functional.Pair;

/**
 * A query gateway for query languages.
 *
 * @created Sep 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public abstract class ProtocolServlet extends MulgaraServlet {

  /** Generated serialization ID. */
  private static final long serialVersionUID = -6510062000251611536L;

  /** The logger. */
  final static Logger logger = Logger.getLogger(ProtocolServlet.class.getName());

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
  
  /** The default output type to use for queries that return graph results. */
  protected static final Output DEFAULT_GRAPH_OUTPUT_TYPE = Output.RDFXML;

  /** The parameter identifying the graph. */
  protected static final String DEFAULT_GRAPH_ARG = "default-graph-uri";

  /** The parameter identifying the graph. We don't set these in SPARQL yet. */
  protected static final String NAMED_GRAPH_ARG = "named-graph-uri";

  /** The content type of the results. */
  protected static final String CONTENT_TYPE = "application/sparql-results+xml";

  /** Session value for interpreter. */
  protected static final String INTERPRETER = "session.interpreter";

  /** Posted RDF data content type. */
  protected static final String POSTED_DATA_TYPE = "multipart/form-data;";

  /** The name of the posted data. */
  protected static final String GRAPH_DATA = "graph";

  /** The header used to indicate a statement count. */
  protected static final String HDR_STMT_COUNT = "Statements-Loaded";

  /** The header used to indicate a part that couldn't be loaded. */
  protected static final String HDR_CANNOT_LOAD = "Cannot-Load";

  /** A made-up scheme for data uploaded through http-put, since http means "downloaded". */
  protected static final String HTTP_PUT_NS = "http-put://upload/";

  /** The various parameter names used to identify a graph in a request */
  private static final String[] GRAPH_PARAM_NAMES = { DEFAULT_GRAPH_ARG, GRAPH_DATA };

  /** The various parameter names used to identify a subject in a request */
  private static final String[] SUBJECT_PARAM_NAMES = { "subject", "subj", "s" };

  /** The various parameter names used to identify a predicate in a request */
  private static final String[] PREDICATE_PARAM_NAMES = { "predicate", "pred", "p" };

  /** The various parameter names used to identify an object in a request */
  private static final String[] OBJECT_PARAM_NAMES = { "object", "obj", "o" };

  /** A query to get the entire contents of a graph */
  private static final String CONSTRUCT_ALL_QUERY = "CONSTRUCT { ?s ?p ?o } WHERE { ?s ?p ?o }";

  /** This object maps request types to the constructors for that output. */
  protected final Map<Output,AnswerStreamConstructor> streamBuilders = new EnumMap<Output,AnswerStreamConstructor>(Output.class);

  /** This object maps request types to the constructors for sending objects to that output. */
  protected final Map<Output,ObjectStreamConstructor> objectStreamBuilders = new EnumMap<Output,ObjectStreamConstructor>(Output.class);

  /**
   * Creates the servlet for communicating with the given server.
   * @param server The server that provides access to the database.
   */
  public ProtocolServlet(SessionFactoryProvider server) throws IOException {
    super(server);
    initializeBuilders();
  }


  /**
   * Creates the servlet for communicating with the given server. Default construction,
   * meaning that the connectionFactory will be used for establishing a connection.
   */
  public ProtocolServlet() throws IOException {
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
    try {
      RestParams params = new RestParams(req);
      RestParams.ResourceType type = params.getType();

      // build a query based on either the resource being requested, or an explicit query
      Query query = null;
      if (type == RestParams.ResourceType.QUERY) {
        query = getQuery(params.getQuery(), req);
      } else if (type == RestParams.ResourceType.GRAPH) {
        query = getQuery(CONSTRUCT_ALL_QUERY, req);
      } else if (type == RestParams.ResourceType.STATEMENT) {
        query = getQuery(createAskQuery(params.getTriple()), req);
      }

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
   * Responds to requests to create graphs or triples.
   */
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      Pair<URI,LocalTriple> params = getModifyParams(req);
      URI graph = params.first();
      LocalTriple triple = params.second();
      Connection conn = getConnection(req);
      if (triple == null) createGraph(conn, graph);
      else createTriple(conn, graph, triple);
      resp.setStatus(SC_CREATED);
    } catch (ServletException e) {
      e.sendResponseTo(resp);
    }
  }


  /**
   * Responds to requests to create graphs or triples.
   */
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      Pair<URI,LocalTriple> params = getModifyParams(req);
      URI graph = params.first();
      LocalTriple triple = params.second();
      Connection conn = getConnection(req);
      if (triple == null) deleteGraph(conn, graph);
      else deleteTriple(conn, graph, triple);
      resp.setStatus(SC_NO_CONTENT);
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
    // establish the output type
    if (type == null) type = DEFAULT_OUTPUT_TYPE;

    resp.setContentType(type.mimeText);
    resp.setHeader("pragma", "no-cache");

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
    if (defaults == null) return null;
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
    return ServerInfo.getDefaultGraphURI();
  }


  /**
   * Creates a new graph.
   * @param conn A connection to the database.
   * @param graph The graph to create.
   * @throws ServletException If there was an error creating the graph.
   */
  protected void createGraph(Connection conn, URI graph) throws ServletException {
    try {
      new CreateGraph(graph).execute(conn);
    } catch (QueryException e) {
      throw new InternalErrorException("Unable to create graph: " + e.getMessage());
    }
  }


  /**
   * Creates a triple in a graph.
   * @param conn A connection to the database.
   * @param graph The graph to create the triple in.
   * @param triple The triple to create.
   * @throws ServletException If there was an error creating the triple, or the graph does not exist.
   */
  protected void createTriple(Connection conn, URI graph, LocalTriple triple) throws ServletException {
    try {
      new Insertion(graph, triple.toSet()).execute(conn);
    } catch (QueryException e) {
      throw new InternalErrorException("Unable to create triple: " + e.getMessage());
    }
  }


  /**
   * Deletes a graph.
   * @param conn A connection to the database.
   * @param graph The graph to delete.
   * @throws ServletException If there was an error removing the graph.
   */
  protected void deleteGraph(Connection conn, URI graph) throws ServletException {
    try {
      new DropGraph(graph).execute(conn);
    } catch (QueryException e) {
      throw new InternalErrorException("Unable to drop graph: " + e.getMessage());
    }
  }


  /**
   * Deletes a triple in a graph.
   * @param conn A connection to the database.
   * @param graph The graph to delete the triple from.
   * @param triple The triple to delete.
   * @throws ServletException If there was an error deleting the triple.
   */
  protected void deleteTriple(Connection conn, URI graph, LocalTriple triple) throws ServletException {
    try {
      new Deletion(graph, triple.toSet()).execute(conn);
    } catch (QueryException e) {
      throw new InternalErrorException("Unable to delete triple: " + e.getMessage());
    }
  }


  /**
   * Creates a string query asking if a given triple exists.
   * @param triple The triple to ask the existence of.
   * @return A string containing an ASK query.
   */
  private String createAskQuery(LocalTriple triple) {
    StringBuffer query = new StringBuffer("ASK ?s ?p ?o { ?s ?p ?o ");
    query.append(". ?s <http://mulgara.org/is> <").append(triple.getSubject()).append(">");
    query.append(". ?p <http://mulgara.org/is> <").append(triple.getPredicate()).append(">");
    ObjectNode o = triple.getObject();
    query.append(". ?o <http://mulgara.org/is> ");
    if (o instanceof URIReference) query.append("<");
    query.append(triple.getObject());
    if (o instanceof URIReference) query.append(">");
    query.append(" }");
    return query.toString();
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
   * Get the parameters of a request to modify the store.
   * @param req The HTTP request.
   * @return A Pair containing the graph for the modification, and an optional triple to be
   *         modified in the graph.
   * @throws ServletException If the request was improperly formed.
   */
  @SuppressWarnings("unchecked")
  private Pair<URI,LocalTriple> getModifyParams(HttpServletRequest req) throws ServletException {
    Map<String,String[]> params = req.getParameterMap();
    String graphParam = getParam(GRAPH_PARAM_NAMES, params);
    if (graphParam == null) throw new BadRequestException("No graph parameter defined.");
    URI g;
    try {
      g = new URI(graphParam);
    } catch (URISyntaxException e) {
      throw new BadRequestException("Invalid graph name: " + graphParam);
    }
    String s = getParam(SUBJECT_PARAM_NAMES, params);
    String p = getParam(PREDICATE_PARAM_NAMES, params);
    String o = getParam(OBJECT_PARAM_NAMES, params);
    if (s == null && p == null && o == null) return new Pair<URI,LocalTriple>(g, null);
    return new Pair<URI,LocalTriple>(g, new LocalTriple(s, p, o, true));
  }


  /**
   * Get the value of a parameter from a map. The parameter is identified by one of the elements
   * found in the <var>keyNames</var> parameter.
   * @param keyNames An array of alternative names for the one parameter.
   * @param params A map of parameter names to values.
   * @return The value for the parameter, or <code>null</code> if not found.
   * @throws ServletException If the parameter appear more than once.
   */
  private static String getParam(String[] keyNames, Map<String,String[]> params) throws ServletException {
    boolean found = false;
    String result = null;
    for (String k: keyNames) {
      if (params.containsKey(k)) {
        if (found) throw new BadRequestException("Duplicate parameter: " + k);
        String[] values = params.get(k);
        if (values.length == 0) throw new BadRequestException("Unsupplied parameter: " + k);
        if (values.length > 1) throw new BadRequestException("Duplicate values for " + k + ": " + Arrays.asList(values));
        result = values[0];
        found = true;
      }
    }
    return result;
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
      try {
        type = Output.valueOf(reqOutputName.toUpperCase());
      } catch (IllegalArgumentException e) {
        // no-op: ignore unknown enumeration values.
      }
    }

    // need graph types if constructing a graph
    if (cmd instanceof ConstructQuery) {
      if (!type.isGraphType) type = DEFAULT_GRAPH_OUTPUT_TYPE;
    } else {
      if (!type.isBindingType) type = DEFAULT_OUTPUT_TYPE;
    }

    return type;
  }


  /**
   * Enumeration of the various output types, depending on mime type.
   */
  enum Output {
    XML("application/sparql-results+xml", false, true),
    JSON("application/sparql-results+json", true, true),
    RDFXML("application/rdf+xml", true, false),
    N3("text/rdf+n3", true, false);

    final String mimeText;
    final boolean isGraphType;
    final boolean isBindingType;
    private Output(String mimeText, boolean isGraphType, boolean isBindingType) { 
      this.mimeText = mimeText;
      this.isGraphType = isGraphType;
      this.isBindingType = isBindingType;
    }

    static private Map<String,Output> outputs = new HashMap<String,Output>();
    static {
      for (Output o: Output.values()) outputs.put(o.mimeText, o);
    }
    
    static Output forMime(String mimeText) { return outputs.get(mimeText); }
  }

  /**
   * A structure containing the possible params for a read request.
   */
  static class RestParams {

    /** The default graph URIs for use in the SPARQL protocol. */
    List<URI> defaultGraphUris = null;

    /** The named graph URIs for use in the SPARQL protocol. */
    List<URI> namedGraphUris = null;

    /** The query or command, for use in the SPARQL protocol. */
    String query = null;

    /** A graph, for use in defining a resource. */
    URI graph = null;

    /** A triple, for use in defining a resource. */
    LocalTriple triple = null;

    /** The type of resource. */
    final ResourceType type;

    @SuppressWarnings("unchecked")
    public RestParams(HttpServletRequest req) throws ServletException {
      Map<String,String[]> params = req.getParameterMap();

      defaultGraphUris = getUriParamList(DEFAULT_GRAPH_ARG, params);
      namedGraphUris = getUriParamList(NAMED_GRAPH_ARG, params);
      
      graph = getUriParam(GRAPH_DATA, params);

      // a single default-graph-uri is equivalent to a graph
      if (graph == null && defaultGraphUris != null && defaultGraphUris.size() == 1) {
        graph = defaultGraphUris.get(0);
      }

      // a graph is equivalent to a single default-graph-uri
      if (graph != null && defaultGraphUris == null) {
        defaultGraphUris = Collections.singletonList(graph);
      }

      query = getStringParam(QUERY_ARG, params);

      String s = getParam(SUBJECT_PARAM_NAMES, params);
      String p = getParam(PREDICATE_PARAM_NAMES, params);
      String o = getParam(OBJECT_PARAM_NAMES, params);
      if (s != null || p != null || o != null) triple = new LocalTriple(s, p, o, true);

      type = testForType();
    }


    /**
     * @return the default-graph-uri parameters in a list, or <code>null</code> if not set.
     */
    public List<URI> getDefaultGraphUris() {
      return defaultGraphUris;
    }


    /**
     * @return the named-graph-uri parameters in a list, or <code>null</code> if not set.
     */
    public List<URI> getNamedGraphUris() {
      return namedGraphUris;
    }


    /**
     * @return the query parameter, or <code>null</code> if not set.
     */
    public String getQuery() {
      return query;
    }


    /**
     * @return the graph parameter, or <code>null</code> if not set.
     */
    public URI getGraph() {
      return graph;
    }


    /**
     * @return the triple, or <code>null</code> if not set.
     */
    public LocalTriple getTriple() {
      return triple;
    }


    /**
     * @return the type of resource represented by these parameters.
     */
    public ResourceType getType() {
      return type;
    }


    /**
     * Test that all the parameters are as expected, and determine the resource type.
     * @return The type of resource represented with these parameters.
     * @throws ServletException If the combination of parameters is not valid for a resource.
     */
    private ResourceType testForType() throws ServletException {
      if (triple != null) {
        // if a triple is defined, then a graph must be defined
        if (graph == null) throw new BadRequestException("No graph parameter defined for triple.");
        // no query allowed
        if (query != null) throw new BadRequestException("Cannot define a statement resource with a query.");
        return ResourceType.STATEMENT;
      }

      // If a query, then the only parameters we may not have are the triple,
      // which has already been tested for.
      if (query != null) return ResourceType.QUERY;

      // So this is a graph resource

      // May not have named graphs, or more than one default graph
      if (defaultGraphUris != null && defaultGraphUris.size() != 1) {
        throw new BadRequestException("Multiple graph resources not permitted.");
      }

      if (namedGraphUris != null && !namedGraphUris.isEmpty()) {
        throw new BadRequestException("Named graphs not valid with a graph resource.");
      }
      
      return ResourceType.GRAPH;
    }


    /**
     * Retrieves all parameters with a given name, and convert to a list of URIs
     * @param paramName The name of the parameter to retrieve.
     * @param params The full set of parameters to retrieve.
     * @return A List of URIs, or <code>null</code> if there are no parameters for <var>paramName</var>.
     * @throws ServletException If one of the parameter strings is not a valid URI.
     */
    static List<URI> getUriParamList(String paramName, Map<String,String[]> params) throws ServletException {
      String[] uris = params.get(paramName);
      if (uris == null || uris.length == 0) return null;
      // convert the default graph strings to URIs
      // report an error if an invalid URI is found
      return C.map(uris,
          new Fn1E<String,URI,ServletException>() {
            public URI fn(String u) throws ServletException {
              try {
                return new URI(u);
              } catch (URISyntaxException e) {
                throw new BadRequestException("Bad graph URI: " + e.getMessage());
              }
            }
          }
      );
    }


    /**
     * Retrieves a parameter with a given name, and converts it into a URI.
     * @param paramName The name of the parameter to retrieve.
     * @param params The full set of parameters to retrieve.
     * @return A URI, or <code>null</code> if there are no parameters for <var>paramName</var>.
     * @throws ServletException If the parameter string is not a valid URI, or if there is more than one value.
     */
    static URI getUriParam(String paramName, Map<String,String[]> params) throws ServletException {
      try {
        String p = getStringParam(paramName, params);
        return p == null ? null : new URI(p);
      } catch (URISyntaxException e) {
        throw new BadRequestException("Bad graph URI: " + e.getMessage());
      }
    }


    /**
     * Retrieves a parameter with a given name.
     * @param paramName The name of the parameter to retrieve.
     * @param params The full set of parameters to retrieve.
     * @return A string value, or <code>null</code> if there are no parameters for <var>paramName</var>.
     * @throws ServletException If there is more than one value.
     */
    static String getStringParam(String paramName, Map<String,String[]> params) throws ServletException {
      String[] vals = params.get(paramName);
      if (vals == null || vals.length == 0) return null;
      if (vals.length > 1) throw new BadRequestException("More that one value for: " + paramName);
      return vals[0];
    }


    /**
     * Resources being referenced by an HTTP method.
     */
    enum ResourceType {
      STATEMENT,
      GRAPH,
      QUERY
    }
  }
}
