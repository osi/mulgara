/**
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
package org.mulgara.query.filter.value;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jrdf.vocabulary.RDF;
import org.mulgara.parser.MulgaraParserException;
import org.mulgara.query.QueryException;
import org.mulgara.query.filter.RDFTerm;
import org.mulgara.query.rdf.XSD;

/**
 * Executes a function that isn't defined in these packages.
 *
 * @created Apr 22, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ExternalFn extends AbstractAccessorFn {

  /** Generated Serialization ID for RMI */
  private static final long serialVersionUID = 5748124115023875223L;

  /** The logger */
  private final static Logger logger = Logger.getLogger(ExternalFn.class.getName());
  
  /** A URI containing the namespace for XSD */
  private final static URI XSD_URI = URI.create(XSD.NAMESPACE);

  /** The scheme for XSD */
  private final static String XSD_SCHEME = XSD_URI.getScheme();

  /** The scheme-specific part for XSD */
  private final static String XSD_PART = XSD_URI.getSchemeSpecificPart();

  /** The function to be run. This will be mapped to a functor or reflection code. */
  private URI fnUri;

  /** This is a constructor function. */
  private boolean isConstructor = false;

  /**
   * Create a new function instance.
   * @param fn The function to run.
   * @param operands The arguments of the function.
   */
  public ExternalFn(IRI fn, RDFTerm... operands) throws MulgaraParserException {
    super(operands);
    fnUri = fn.getValue();
    if (isCast(fnUri)) {
      if (operands.length != 1) throw new MulgaraParserException("Cast operation can only take a single parameter");
      isConstructor = true;
    } else {
      logger.error("Unknown function URI: " + fn);
    }
  }

  /**
   * Tests if the URI is used for casting a literal. Anything that is in the XSD namespace,
   * or the rdf:XMLLiteral type, is considered to be a construction operation.
   * @param u The URI to test.
   * @return <code>true</code> If the URI is a known type for casting.
   */
  private boolean isCast(URI u) {
    if (XSD_SCHEME.equals(fnUri.getScheme()) && XSD_PART.equals(fnUri.getSchemeSpecificPart())) return true;
    if (RDF.XML_LITERAL.equals(u)) return true;
    return false;
  }

  // The ValueLiteral interface

  /**
   * @see org.mulgara.query.filter.value.ValueLiteral#getLexical()
   * @throws QueryException if this function does not resolve to a literal.
   */
  public String getLexical() throws QueryException {
    RDFTerm result = resolve();
    if (result.isLiteral()) return ((ValueLiteral)result).getLexical();
    throw new QueryException("Type Error: Not valid to ask the lexical form of a: " + result.getClass().getSimpleName());
  }

  /**
   * @see org.mulgara.query.filter.value.ValueLiteral#getLang()
   * @throws QueryException if this function does not resolve to a literal.
   */
  public SimpleLiteral getLang() throws QueryException {
    RDFTerm result = resolve();
    if (result.isLiteral()) return ((ValueLiteral)result).getLang();
    throw new QueryException("Type Error: Not valid to ask the language of a: " + result.getClass().getSimpleName());
  }

  /**
   * @see org.mulgara.query.filter.value.ValueLiteral#getType()
   * @throws QueryException if this function does not resolve to a literal.
   */
  public IRI getType() throws QueryException {
    RDFTerm result = resolve();
    if (result.isLiteral()) return ((ValueLiteral)result).getType();
    throw new QueryException("Type Error: Not valid to ask the type of a: " + result.getClass().getSimpleName());
  }

  /** @see org.mulgara.query.filter.AbstractFilterValue#isSimple() */
  public boolean isSimple() throws QueryException {
    RDFTerm result = resolve();
    if (result.isLiteral()) return ((ValueLiteral)result).isSimple();
    throw new QueryException("Type Error: Not valid to check if a non-literal is a simple literal: " + result.getClass().getSimpleName());
  }

  // The RDFTerm interface

  /** @see org.mulgara.query.filter.RDFTerm#isBlank() */
  public boolean isBlank() throws QueryException { return resolve().isBlank(); }

  /** @see org.mulgara.query.filter.RDFTerm#isIRI() */
  public boolean isIRI() throws QueryException { return resolve().isIRI(); }

  /**
   * {@inheritDoc}
   * The operation of this method is depended on the context in which it was called.
   * If it is called without a context owner, then this means it was called during
   * Filter construction, and we want to indicate that it is valid to treat this as a literal.
   * @return <code>true</code> if there is no context, or else it calls isLiteral on the resolved value.
   */
  public boolean isLiteral() throws QueryException {
    return getContextOwner() == null ? true : resolve().isLiteral();
  }


  /**
   * Resolve the value of the function.
   * @return The resolution of the function
   * @throws QueryException if the function does not resolve
   */
  protected RDFTerm resolve() throws QueryException {
    if (isConstructor) {
      List<Object> args = resolveArgs();
      assert args.size() == 1;
      Object value = args.get(0);
      // being a cast we'll have to resort to the lowest common denominator of "string"
      // and let the TypedLiteral work it out for us
      if (XSD.isNumericType(fnUri) && value instanceof Number) return new NumericLiteral(NumericLiteral.getValueFor((Number)value, fnUri), fnUri);
      return TypedLiteral.newLiteral(value.toString(), fnUri, null);
    }
    logger.warn("Attempting to execute an unsupported function: " + fnUri + "(" + resolveArgs() + ")");
    return Bool.TRUE;
  }

  /**
   * A utility function to create a list of arguments to be passed to the external function.
   * @return A {@link List} of arbitrary objects to be passed as arguments to the external function.
   * @throws QueryException If any of the arguments could not be resolved.
   */
  private List<Object> resolveArgs() throws QueryException {
    List<Object> result = new ArrayList<Object>(operands.length);
    for (int i = 0; i < operands.length; i++) {
      RDFTerm op = operands[i];
      if (isConstructor && op.isBlank()) throw new QueryException("Type Error: Cannot cast a blank node.");
      result.add(op.getValue());
    }
    return result;
  }
}
