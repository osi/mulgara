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

package org.mulgara.protocol;

import java.io.IOException;
import java.io.OutputStream;

import org.jrdf.graph.BlankNode;
import org.jrdf.graph.Literal;
import org.jrdf.graph.URIReference;
import org.mulgara.query.Answer;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;

/**
 * Represents an Answer as TQL XML.
 *
 * @created Jul 8, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class StreamedTqlXMLAnswer extends AbstractStreamedXMLAnswer {

  /**
   * Creates an XML Answer for XML results. Pretty printing is off by default.
   * @param answer The Answer to wrap.
   */
  public StreamedTqlXMLAnswer(Answer answer, OutputStream output) {
    super(answer, output);
    setPrettyPrint(false);
  }

  /** {@inheritDoc} */
  protected void addDocHeader() throws IOException {
    s.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    s.append("<answer xmlns=\"http://mulgara.org/tql#\">");
    s.append(i(1)).append("<query>");
  }

  /** {@inheritDoc} */
  protected void addDocFooter() throws IOException {
    s.append(i(1)).append("</query>");
    s.append(i(0)).append("</answer>");
  }

  /** {@inheritDoc} */
  protected void addHeader() throws IOException {
    addHeader(answer, 0);
  }

  void addHeader(Answer a, int indent) throws IOException {
    s.append(i(indent + 2)).append("<variables>");
    if (a != null && a.getVariables() != null) for (Variable v: a.getVariables()) addHeaderVariable(v, indent);
    s.append(i(indent + 2)).append("</variables>");
  }

  /** {@inheritDoc} */
  protected void addHeaderVariable(Variable var) throws IOException {
    addHeaderVariable(var, 0);
  }

  /** {@inheritDoc} */
  protected void addHeaderVariable(Variable var, int indent) throws IOException {
    s.append(i(indent + 3)).append("<").append(var.getName()).append("/>");
  }

  /** {@inheritDoc} */
  protected void addResults() throws TuplesException, IOException {
    answer.beforeFirst();
    while (answer.next()) addResult();
  }

  /** {@inheritDoc} */
  protected void addResults(Answer a, int indent) throws TuplesException, IOException {
    a.beforeFirst();
    while (a.next()) addResult(a, indent);
  }

  /** {@inheritDoc} */
  protected void addResult() throws TuplesException, IOException {
    addResult(answer, 0);
  }

  /** {@inheritDoc} */
  protected void addResult(Answer a, int indent) throws TuplesException, IOException {
    s.append(i(indent + 2)).append("<solution>");
    for (int c = 0; c < width; c++) addBinding(vars[c], answer.getObject(c), indent);
    s.append(i(indent + 2)).append("</solution>");
  }

  /**
   * {@inheritDoc}
   * No binding will be emitted if the value is null (unbound).
   */
  protected void addBinding(Variable var, Object value) throws TuplesException, IOException {
    addBinding(var, value, 0);
  }

  /**
   * {@inheritDoc}
   * No binding will be emitted if the value is null (unbound).
   * @throws TuplesException Indicates an error accessing the Answer.
   */
  protected void addBinding(Variable var, Object value, int indent) throws TuplesException, IOException {
    if (value != null) {
      s.append(i(indent + 3)).append("<").append(var.getName());
      // no dynamic dispatch, so use if/then
      if (value instanceof URIReference) {
        addURI((URIReference)value);
      } else if (value instanceof BlankNode) {
        addBNode((BlankNode)value);
      } else if (value instanceof Literal) {
        addLiteral((Literal)value);
        s.append("</").append(var.getName()).append(">");
      } else if (value instanceof Answer) {
        addHeader((Answer)value, indent + 4);
        addResults((Answer)value, indent + 4);
      } else throw new IllegalArgumentException("Unable to create a SPARQL response with an answer containing: " + value.getClass().getSimpleName());
    }
  }

  /** {@inheritDoc} */
  protected void addURI(URIReference uri) throws IOException {
    s.append(" resource=\"").append(uri.getURI().toString()).append("\"/>");
  }

  /** {@inheritDoc} */
  protected void addBNode(BlankNode bnode) throws IOException {
    s.append(" blank-node=\"").append(bnode.toString()).append("\"/>");
  }

  /** {@inheritDoc} */
  protected void addLiteral(Literal literal) throws IOException {
    if (literal.getLanguage() != null) s.append(" language=\"").append(literal.getLanguage()).append("\"");
    else if (literal.getDatatype() != null) s.append(" datatype=\"").append(literal.getDatatype().toString()).append("\"");
    s.append(">").append(literal.getLexicalForm());
  }

}
