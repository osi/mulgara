package org.mulgara.protocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.jrdf.graph.BlankNode;
import org.jrdf.graph.Literal;
import org.jrdf.graph.URIReference;
import org.mulgara.query.Answer;
import org.mulgara.query.TuplesException;
import org.mulgara.query.Variable;

/**
 * Converts an answer to a stream, acccording to the protocol of the implementing class.
 *
 * @created Sep 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class AbstractStreamedAnswer {

  /** The API {@link Answer} to convert to the stream. */
  protected final Answer answer;

  /** The number of columns in the Answer. */
  protected final int width;

  /** The {@link Variable}s in the Answer. */
  protected final Variable[] vars;

  /** The writer used for creating the XML. */
  protected OutputStreamWriter s = null;

  /** The byte output stream used for creating the XML. */
  protected OutputStream output = null;

  /** The charset encoding to use when writing to the output stream. */
  Charset charset = Charset.defaultCharset();

  /** Adds a literal to the stream */
  protected abstract void addLiteral(Literal literal) throws IOException;

  /** Adds a blank node to the stream */
  protected abstract void addBNode(BlankNode bnode) throws IOException;

  /** Adds a URI reference to the stream */
  protected abstract void addURI(URIReference uri) throws IOException;

  /** Adds a variable binding to the stream */
  protected abstract void addBinding(Variable var, Object value) throws TuplesException, IOException;

  /** Adds a single result to the stream */
  protected abstract void addResult() throws TuplesException, IOException;

  /** Adds all results to the stream */
  protected abstract void addResults() throws TuplesException, IOException;

  /** Adds a variable in the header to the stream */
  protected abstract void addHeaderVariable(Variable var) throws IOException;

  /** Adds the entire header to the stream */
  protected abstract void addHeader() throws IOException;

  /** Closes the document in the stream */
  protected abstract void addDocFooter() throws IOException;

  /** Oopens the document in the stream */
  protected abstract void addDocHeader() throws IOException;


  /**
   * Creates the object around the answer and output stream.
   * @param answer The answer to encode.
   * @param output The stream to write to.
   */
  public AbstractStreamedAnswer(Answer answer, OutputStream output) {
    this.answer = answer;
    this.output = output;
    width = (answer != null) ? answer.getNumberOfVariables() : 0;
    vars = (answer != null) ? answer.getVariables() : null;
  }

  /**
   * @see org.mulgara.protocol.StreamedXMLAnswer#emit()
   */
  public void emit() throws TuplesException, IOException {
    if (s == null) {
      s = new OutputStreamWriter(output, charset);
      generate();
      s.flush();
    }
  }

  /**
   * Generates the XML document in the {@link #s} buffer.
   * @throws TuplesException Indicates an error accessing the Answer.
   */
  void generate() throws TuplesException, IOException {
    addDocHeader();
    addHeader();
    addResults();
    addDocFooter();
  }

}