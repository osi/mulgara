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
import java.io.OutputStreamWriter;
import java.net.URI;

import org.jrdf.graph.BlankNode;
import org.jrdf.graph.Literal;
import org.jrdf.graph.URIReference;
import org.mulgara.query.Answer;
import org.mulgara.query.BooleanAnswer;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;

/**
 * Represents an Answer as JSON.
 * The format is specified at: {@link http://www.w3.org/TR/rdf-sparql-json-res/}
 *
 * @created Sep 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class StreamedSparqlJSONAnswer extends AbstractStreamedAnswer implements StreamedJSONAnswer {

  /** Additional metadata about the results. */
  URI additionalMetadata = null;
  
  /** Boolean answer. */
  boolean booleanResult = false;

  /** Internal flag to indicate that a comma may be needed. */
  boolean prependComma = false;

  /**
   * Creates an XML Answer conforming to SPARQL XML results.
   * @param answer The Answer to wrap.
   */
  public StreamedSparqlJSONAnswer(Answer answer, OutputStream output) {
    super((answer instanceof BooleanAnswer) ? null : answer, output);
    if (answer instanceof BooleanAnswer) booleanResult = ((BooleanAnswer)answer).getResult();
  }

  /**
   * Creates an XML Answer with additional metadata.
   * @param answer The Answer to wrap.
   * @param metadata Additional metadata for the answer.
   */
  public StreamedSparqlJSONAnswer(Answer answer, URI metadata, OutputStream output) {
    this(answer, output);
    additionalMetadata = metadata;
  }

  /**
   * Creates an XML Answer conforming to SPARQL XML results.
   * @param result The boolean result to encode.
   */
  public StreamedSparqlJSONAnswer(boolean result, OutputStream output) {
    super(null, output);
    booleanResult = result;
  }

  /**
   * Creates an XML Answer with additional metadata.
   * @param result The boolean result to encode.
   * @param metadata Additional metadata for the answer.
   */
  public StreamedSparqlJSONAnswer(boolean result, URI metadata, OutputStream output) {
    super(null, output);
    booleanResult = result;
    additionalMetadata = metadata;
  }

  /** {@inheritDoc} */
  protected void addDocHeader() throws IOException {
    s.append("{ ");
  }

  /** {@inheritDoc} */
  protected void addDocFooter() throws IOException {
    s.append(" }");
  }

  /** {@inheritDoc} */
  protected void addHeader() throws IOException {
    s.append("\"head\": {");
    boolean wroteVars = false;
    if (answer != null && answer.getVariables() != null) {
      s.append("\"vars\": [");
      prependComma = false;
      for (Variable v: answer.getVariables()) addHeaderVariable(v);
      s.append("]");
      wroteVars = true;
    }
    if (additionalMetadata != null) {
      if (wroteVars) s.append(", ");
      s.append("\"link\": [\"").append(additionalMetadata.toString()).append("\"]");
    }
    s.append("}, ");
  }

  /** {@inheritDoc} */
  protected void addHeaderVariable(Variable var) throws IOException {
    comma().append("\"").append(var.getName()).append("\"");
  }

  /** {@inheritDoc} */
  protected void addResults() throws TuplesException, IOException {
    if (answer != null) {
      comma().append("\"results\": { ");
      s.append("\"bindings\": [ ");
      answer.beforeFirst();
      prependComma = false;
      while (answer.next()) addResult();
      s.append(" ] }");
    } else {
      comma().append("\"boolean\": ").append(Boolean.toString(booleanResult));
    }
  }

  /** {@inheritDoc} */
  protected void addResult() throws TuplesException, IOException {
    comma().append("{ ");
    prependComma = false;
    for (int c = 0; c < width; c++) addBinding(vars[c], answer.getObject(c));
    s.append(" }");
  }

  /**
   * {@inheritDoc}
   * No binding will be emitted if the value is null (unbound).
   */
  protected void addBinding(Variable var, Object value) throws IOException {
    if (value != null) {
      comma().append("\"").append(var.getName()).append("\": { ");
      // no dynamic dispatch, so use if/then
      if (value instanceof URIReference) addURI((URIReference)value);
      else if (value instanceof Literal) addLiteral((Literal)value);
      else if (value instanceof BlankNode) addBNode((BlankNode)value);
      else throw new IllegalArgumentException("Unable to create a SPARQL response with an answer containing: " + value.getClass().getSimpleName());
      s.append(" }");
    }
  }

  /** {@inheritDoc} */
  protected void addURI(URIReference uri) throws IOException {
    s.append("\"type\": \"uri\", \"value\": \"").append(uri.getURI().toString()).append("\"");
  }

  /** {@inheritDoc} */
  protected void addBNode(BlankNode bnode) throws IOException {
    s.append("\"type\": \"bnode\", \"value\": \"").append(bnode.toString()).append("\"");
  }

  /** {@inheritDoc} */
  protected void addLiteral(Literal literal) throws IOException {
    if (literal.getDatatype() != null) {
      s.append("\"type\": \"typed-literal\", \"datatype\": \"").append(literal.getDatatype().toString()).append("\", ");
    } else {
      s.append("\"type\": \"literal\", ");
      if (literal.getLanguage() != null) s.append(" \"xml:lang\": \"").append(literal.getLanguage()).append("\", ");
    }
    s.append("\"value\": \"").append(literal.getLexicalForm()).append("\"");
  }

  /**
   * Adds a comma if needed at this point. Commas are usually needed.
   * @throws IOException An error writing to the stream.
   */
  protected OutputStreamWriter comma() throws IOException {
    if (prependComma) s.append(", ");
    prependComma = true;
    return s;
  }
}
