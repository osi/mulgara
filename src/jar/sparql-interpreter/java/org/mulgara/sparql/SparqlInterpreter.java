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
package org.mulgara.sparql;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.mulgara.parser.Interpreter;
import org.mulgara.parser.MulgaraLexerException;
import org.mulgara.parser.MulgaraParserException;
import org.mulgara.query.ConstraintConjunction;
import org.mulgara.query.ConstraintDisjunction;
import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ConstraintIs;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.ModelResource;
import org.mulgara.query.ModelUnion;
import org.mulgara.query.Order;
import org.mulgara.query.Query;
import org.mulgara.query.SelectElement;
import org.mulgara.query.UnconstrainedAnswer;
import org.mulgara.query.Variable;
import org.mulgara.query.operation.Command;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.sparql.parser.ParseException;
import org.mulgara.sparql.parser.QueryStructure;
import org.mulgara.sparql.parser.SparqlParser;
import org.mulgara.sparql.parser.cst.Expression;
import org.mulgara.sparql.parser.cst.GroupGraphPattern;
import org.mulgara.sparql.parser.cst.IRIReference;
import org.mulgara.sparql.parser.cst.Node;
import org.mulgara.sparql.parser.cst.Ordering;


/**
 * Converts a parsed SPARQL query into a Command for execution.
 *
 * @created Apr 18, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class SparqlInterpreter implements Interpreter {

  /** The default graph to use if none has been set. */
  private static final URI INTERNAL_DEFAULT_GRAPH_URI = URI.create("local:null");

  /** The default graph to use when none has been parsed. */
  private URI defaultGraphUri = null;

  /**
   * Sets the default graph to use in parsed queries.
   * @param graph The graph URI to use as the default graph.
   */
  public void setDefaultGraphUri(URI graphUri) {
    defaultGraphUri = graphUri;
  }

  /**
   * Gets the default graph to use when none has been parsed from the query.
   * @return The graph that parsed queries will default to when no FROM graph is supplied.
   */
  public URI getDefaultGraphUri() {
    return (defaultGraphUri != null) ? defaultGraphUri : INTERNAL_DEFAULT_GRAPH_URI;
  }

  /**
   * @see org.mulgara.parser.Interpreter#parseCommand(java.lang.String)
   * The only commands that SPARQL current handles are queries.
   */
  public Command parseCommand(String command) throws MulgaraParserException, MulgaraLexerException, IllegalArgumentException, IOException {
    return parseQuery(command);
  }

  /**
   * @see org.mulgara.parser.Interpreter#parseCommands(java.lang.String)
   * Since SPARQL has no separator character, there can only be one command per string.
   */
  public List<Command> parseCommands(String command) throws MulgaraParserException, MulgaraLexerException, IOException, IllegalArgumentException {
    return Collections.singletonList(parseCommand(command));
  }

  /**
   * @see org.mulgara.parser.Interpreter#parseQuery(java.lang.String)
   */
  public Query parseQuery(String queryString) throws IOException, MulgaraLexerException, MulgaraParserException {
    QueryStructure struct;
    try {
      struct = SparqlParser.parse(queryString);
    } catch (ParseException pe) {
      throw new MulgaraParserException(pe);
    }
    switch (struct.getType()) {
      case select:
        return buildSelectQuery(struct);
      case construct:
        return unhandledType(struct);
      case describe:
        return unhandledType(struct);
      case ask:
        return unhandledType(struct);
      default:
        throw new MulgaraParserException("Unknown query type: " + struct.getType().name());
    }
  }

  /**
   * Respond to an unhandled query type.
   * @param struct The structure representing the query
   * @return Nothing. An exception is always thrown.
   * @throws UnsupportedOperationException An exception explaining that this query type is not handled.
   */
  private Query unhandledType(QueryStructure struct) throws UnsupportedOperationException {
    throw new UnsupportedOperationException("Query type not yet supported: " + struct.getType().name());
  }

  /**
   * Converts the elements of a {@link QueryStructure} into a Mulgara {@link Query}.
   * @param queryStruct The structure to analyze and convert.
   * @return A new query that can be run as a {@link org.mulgara.query.operation.Command} on a connection.
   * @throws MulgaraParserException If the query structure contains elements that are not supported by Mulgara.
   */
  Query buildSelectQuery(QueryStructure queryStruct) throws MulgaraParserException {
    List<? extends SelectElement> selection = getSelection(queryStruct);
    ModelExpression defaultGraphs = getFrom(queryStruct);
    ConstraintExpression whereClause = getWhere(queryStruct);
    List<Order> orderBy = getOrdering(queryStruct);
    Integer limit = getLimit(queryStruct);
    int offset = queryStruct.getOffset();
    // null having, unconstrained answer
    return new Query(selection, defaultGraphs, whereClause, null, orderBy, limit, offset, new UnconstrainedAnswer());
  }

  /**
   * Extract the requested variables from this query into a list.
   * @param queryStruct The query to get the selected variables from.
   * @return A new list containing Mulgara {@link Variable}s.
   * @throws MulgaraParserException If and selected elements are not variables.
   */
  List<? extends SelectElement> getSelection(QueryStructure queryStruct) throws MulgaraParserException {
    List<Variable> result = new ArrayList<Variable>();
    if (queryStruct.isSelectAll()) {
      Collection<org.mulgara.sparql.parser.cst.Variable> allVars = queryStruct.getAllVariables();
      for (org.mulgara.sparql.parser.cst.Variable v: allVars) result.add(new Variable(v.getName()));
    } else {
      List<? extends Node> selection = queryStruct.getSelection();
      for (Node n: selection) {
        // SPARQL only permits variables
        if (!(n instanceof org.mulgara.sparql.parser.cst.Variable)) throw new MulgaraParserException("Unexpected non-variable in the SELECT clause");
        org.mulgara.sparql.parser.cst.Variable cv = (org.mulgara.sparql.parser.cst.Variable)n;
        result.add(new Variable(cv.getName()));
      }
    }
    return result;
  }

  /**
   * Gets the graph expression ({@link ModelExpression}) the represents the FROM clause, or the default
   * graph if none was provided.
   * @param queryStruct The structure to query for the FROM clause.
   * @return A ModelExpression containing all the required graphs as a union. TODO: this should be a merge.
   */
  ModelExpression getFrom(QueryStructure queryStruct) {
    List<IRIReference> iris = queryStruct.getDefaultFroms();
    // if there were no named graphs, then use the default
    if (iris.isEmpty()) return new ModelResource(getDefaultGraphUri());
    // accumulate the remaining graphs as a union
    return graphUnion(iris);
  }

  /**
   * Convert a list of IRIs into a model resource union of minimal depth. This recurses through construction
   * of a tree of binary unions, rather than creating a linear linked list of unions.
   * @param iris The list to convert.
   * @return A ModelExpression which is a union of all the elements in the list,
   *   or a {@link ModelResource} if the list contains only one element.
   */
  private ModelExpression graphUnion(List<IRIReference> iris) {
    int listSize = iris.size();
    // terminate on singleton lists
    if (listSize == 1) return new ModelResource(iris.get(0).getUri());
    // short circuit for 2 element lists - optimization
    if (listSize == 2) return new ModelUnion(new ModelResource(iris.get(0).getUri()), new ModelResource(iris.get(1).getUri()));
    // general case
    return new ModelUnion(graphUnion(iris.subList(0, listSize / 2)), graphUnion(iris.subList(listSize / 2, listSize)));
  }

  /**
   * Creates a list of the ordering to apply to the results. While SPARQL permits ordering by complex
   * expressions, this is not supported.
   * @param queryStruct The query structure.
   * @return A list of {@link Order}, which are each ordered ascending or descending by variable.
   * @throws MulgaraParserException If the ORDER BY expression was more complex than a simple variable.
   */
  List<Order> getOrdering(QueryStructure queryStruct) throws MulgaraParserException {
    List<Ordering> orderings = queryStruct.getOrderings();
    List<Order> result = new ArrayList<Order>(orderings.size());
    for (Ordering order: orderings) {
      Expression v = order.getExpr();
      if (!(v instanceof org.mulgara.sparql.parser.cst.Variable)) throw new MulgaraParserException("Unable to support arbitrarily complex ORDER BY clauses.");
      org.mulgara.sparql.parser.cst.Variable var = (org.mulgara.sparql.parser.cst.Variable)v;
      result.add(new Order(new Variable(var.getName()), order.isAscending()));
    }
    return result;
  }

  /**
   * Get the limit described by the query.
   * @param queryStruct The structure of the query.
   * @return A {@link java.lang.Integer} containing the limit, or <code>null</code> if there is no limit.
   */
  Integer getLimit(QueryStructure queryStruct) {
    int limit = queryStruct.getLimit();
    return limit == -1 ? null : limit;
  }

  /**
   * Build a WHERE clause for a Mulgara query out of a SPARQL WHERE clause.
   * @param queryStruct The SPARQL query structure to analyze for the WHERE clause.
   * @return A Mulgara WHERE clause, as a {@link ConstraintExpression}.
   * @throws MulgaraParserException The structure of the pattern was incorrect.
   */
  ConstraintExpression getWhere(QueryStructure queryStruct) throws MulgaraParserException {
    // get the basic pattern
    GroupGraphPattern pattern = queryStruct.getWhereClause();
    PatternMapper patternMapper = new PatternMapper(pattern);
    ConstraintExpression result = patternMapper.mapToConstraints();
    // apply the FROM NAMED expression
    // TODO: This needs to become a Constraint that wraps LiteralTuples.
    List<IRIReference> namedFroms = queryStruct.getNamedFroms();
    if (!namedFroms.isEmpty()) result = addNamedFroms(result, namedFroms, patternMapper.getGraphVars());
    // possible to ask for non-variables that were employed in GRAPH statements as a parser check.
    return result;
  }

  /**
   * Add in the FROM NAMED values to provide a binding list for each variable used in GRAPH statements.
   * @param expr The total expression to be modified. This is the WHERE clause.
   * @param graphs The list of graphs given in the FROM NAMED clauses.
   * @param graphVars The variables that are used in GRAPH statements.
   * @return A modified form of expr, with all the graph variables pre-bound.
   */
  ConstraintExpression addNamedFroms(ConstraintExpression expr, List<IRIReference> graphs, Set<Variable> graphVars) {
    List<ConstraintExpression> params = new ArrayList<ConstraintExpression>(graphVars.size() + 1);
    params.add(expr);
    for (Variable var: graphVars) params.add(newListConstraint(var, graphs));
    return new ConstraintConjunction(params);
  }

  /**
   * Construct a constraint expression that binds a variable to a list of values.
   * TODO: This needs to be represented by a new Constraint that gets converted to a LiteralTuples.
   * @param var The variable to bind.
   * @param bindingList The list of values to bind the variable to.
   * @return A new {@link org.mulgara.query.Constraint} that represents the variable binding.
   */
  ConstraintExpression newListConstraint(Variable var, List<IRIReference> bindingList) {
    List<ConstraintExpression> isConstraints = new ArrayList<ConstraintExpression>(bindingList.size());
    for (IRIReference iri: bindingList) {
      // does this need a graph node that isn't variable?
      isConstraints.add(new ConstraintIs(var, new URIReferenceImpl(iri.getUri())));
    }
    return new ConstraintDisjunction(isConstraints);
  }
}
