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
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.mulgara.protocol.StreamedAnswer;
import org.mulgara.protocol.StreamedSparqlJSONAnswer;
import org.mulgara.protocol.StreamedSparqlJSONObject;
import org.mulgara.protocol.StreamedSparqlXMLAnswer;
import org.mulgara.protocol.StreamedSparqlXMLObject;
import org.mulgara.query.Answer;
import org.mulgara.server.SessionFactoryProvider;
import org.mulgara.sparql.SparqlInterpreter;
import org.mulgara.util.functional.C;
import org.mulgara.util.functional.Fn1E;

/**
 * A query gateway for SPARQL.
 *
 * @created Sep 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class SparqlServlet extends ProtocolServlet {

  /** Serialization ID */
  private static final long serialVersionUID = 5047396536306099528L;

  /** The parameter identifying the graph. */
  private static final String DEFAULT_GRAPH_ARG = "default-graph-uri";

  /** The parameter identifying the graph. We don't set these in SPARQL yet. */
  @SuppressWarnings("unused")
  private static final String NAMED_GRAPH_ARG = "named-graph-uri";

  /** An empty graph for those occasions when no graph is set. */
  private static final List<URI> DEFAULT_NULL_GRAPH_LIST = Collections.singletonList(URI.create("sys:null"));

  static {
  }


  /**
   * Creates the servlet for communicating with the given server.
   * @param server The server that provides access to the database.
   */
  public SparqlServlet(SessionFactoryProvider server) throws IOException {
    super(server);
  }


  /** @see org.mulgara.protocol.http.ProtocolServlet#initializeBuilders() */
  protected void initializeBuilders() {
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
   * Provide a description for the servlet.
   * @see javax.servlet.GenericServlet#getServletInfo()
   */
  public String getServletInfo() {
    return "Mulgara SPARQL Query Endpoint";
  }


  /**
   * Gets the SPARQL interpreter for the current session,
   * creating it if it doesn't exist yet.
   * @param req The current request environment.
   * @return A connection that is tied to this HTTP session.
   */
  protected SparqlInterpreter getInterpreter(HttpServletRequest req) throws BadRequestException {
    HttpSession httpSession = req.getSession();
    SparqlInterpreter interpreter = (SparqlInterpreter)httpSession.getAttribute(INTERPRETER);
    if (interpreter == null) {
      interpreter = new SparqlInterpreter();
      httpSession.setAttribute(INTERPRETER, interpreter);
    }
    interpreter.setDefaultGraphUris(getRequestedDefaultGraphs(req));
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
    if (defaults == null) return DEFAULT_NULL_GRAPH_LIST;
    try {
      return C.map(defaults, new Fn1E<String,URI,URISyntaxException>(){public URI fn(String s)throws URISyntaxException{return new URI(s);}});
    } catch (URISyntaxException e) {
      throw new BadRequestException("Invalid URI. " + e.getMessage());
    }
  }

}
