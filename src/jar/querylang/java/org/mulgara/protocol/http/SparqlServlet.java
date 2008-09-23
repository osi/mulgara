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

}
