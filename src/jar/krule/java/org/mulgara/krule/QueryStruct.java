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

package org.mulgara.krule;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jrdf.graph.Literal;
import org.jrdf.graph.Node;
import org.jrdf.graph.URIReference;
import org.mulgara.itql.VariableFactoryImpl;
import org.mulgara.query.ConstantValue;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ConstraintHaving;
import org.mulgara.query.ModelExpression;
import org.mulgara.query.ModelResource;
import org.mulgara.query.ModelUnion;
import org.mulgara.query.Order;
import org.mulgara.query.Query;
import org.mulgara.query.SelectElement;
import org.mulgara.query.UnconstrainedAnswer;
import org.mulgara.query.Variable;
import org.mulgara.query.VariableFactory;
import org.mulgara.query.rdf.LiteralImpl;
import org.mulgara.query.rdf.URIReferenceImpl;

/**
 * A structure to describe an iTQL query.
 *
 * @created 2005-5-27
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.2 $
 * @modified $Date: 2005/07/03 12:56:44 $
 * @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class QueryStruct implements Serializable {

  static final long serialVersionUID = -6472138136362435835L;
  
  /** Logger.  */
  private static Logger logger = Logger.getLogger(QueryStruct.class.getName());

  /** The selection list. */
  private ConstraintElement[] select = new ConstraintElement[3];

  /** List of elements which are variables, or ConstantValues. */
  private List<SelectElement> variables;

  /** The model expresison for the query. */
  private ModelExpression models;

  /** The where clause of the query. */
  private ConstraintExpression where;

  /** The having clause for the query. */
  private ConstraintHaving having;


  /**
   * Constructor.  Converts string descriptions of the values and variables into the constraint elements.
   *
   * @param vs The element nodes.
   * @param types The types of the elements, defined in the krule namespace.
   * @param alias The aliases used in the query process.
   * @param uriReferences A map of all krule:ref_* objects to the appropriate {@link org.jrdf.graph.URIReference}s. 
   * @param varReferences A map of all krule:var_* objects to the appropriate name.
   * @throws IllegalArgumentException If the types are incorrect, the elements are not named as expected,
   *         or the references are not found in the references map.
   */
  public QueryStruct(
      URIReference[] vs, URIReference[] types, Map<String,URI> alias,
      Map<URIReference,URIReference> uriReferences, Map<URIReference,Variable> varReferences,
      Map<Node,Literal> litReferences
  ) {

    if (vs.length != 3 && types.length != 3) {
      throw new IllegalArgumentException("Wrong number of elements for a rule query");
    }

    VariableFactory variableFactory = new VariableFactoryImpl();

    // set up a list of variables
    variables = new ArrayList<SelectElement>();

    // convert the parameters to usable objects
    for (int i = 0; i < 3; i++) {
      URIReference element = vs[i];
      // check the type
      if (types[i].equals(KruleLoader.URI_REF)) {

        // get the referred value from the map
        select[i] = (URIReferenceImpl)uriReferences.get(element);
        // assume that literals do not have the "Value" type inferred
        variables.add(new ConstantValue(variableFactory.newVariable(), (URIReferenceImpl)select[i]));

      } else if (types[i].equals(KruleLoader.VARIABLE)) {

        // get the variable
        select[i] = (Variable)varReferences.get(element);
        variables.add((Variable)select[i]);

      } else if (types[i].equals(KruleLoader.LITERAL)) {
        
        if (i != 2) {
          throw new IllegalArgumentException("Selection literal in illegal position in query");
        }
        // get the literal
        select[i] = (LiteralImpl)litReferences.get(element);
        variables.add(new ConstantValue(variableFactory.newVariable(), (LiteralImpl)select[i]));
      } else {
        throw new IllegalArgumentException("Unknown selection type in rule query.");
      }

      if (select[i] == null) {
        throw new IllegalArgumentException("Unable to resolve a reference for: " + element);
      }
    }

    models = null;
    having = null;
  }


  /**
   * Retrieve the element <em>n</em>.
   *
   * @param n The element number to retrieve.
   * @return The <em>n</em>th element.
   * @throws IndexOutOfBoundsException If n is larger than 3.
   */
  public ConstraintElement getElement(int n) {
    assert n < 3;
    return select[n];
  }


  /**
   * Sets the where clause for the query.
   *
   * @param constraints The constraint expression defining the where clause.
   */
  public void setWhereClause(ConstraintExpression constraints) {
    where = constraints;
  }


  /**
   * Sets the having clause for the query.
   *
   * @param having The having constraint expression defining the having clause.
   */
  public void setHavingClause(ConstraintHaving having) {
    this.having = having;
  }


  /**
   * Sets the default models for the query.
   *
   * @param modelUri The URI of the model for the query.
   */
  public void setModelExpression(URI modelUri) {
    this.models = new ModelResource(modelUri);
  }


  /**
   * Sets the default models for the query.
   *
   * @param firstModelUri The first URI of the model for the query.
   * @param secondModelUri The second URI of the model for the query.
   */
  public void setModelExpression(URI firstModelUri, URI secondModelUri) {
    if (firstModelUri.equals(secondModelUri)) {
      setModelExpression(firstModelUri);
    } else {
      this.models = new ModelUnion(new ModelResource(firstModelUri), new ModelResource(secondModelUri));
    }
  }


  /**
   * Constructs a new query based on the current data.
   *
   * @return a new {@link org.mulgara.query.Query}
   */
  @SuppressWarnings("unchecked")
  public Query extractQuery() {
    logger.debug("Extracting query");
    return new Query(variables, models, where, having, (List<Order>)Collections.EMPTY_LIST, null, 0, new UnconstrainedAnswer());
  }

}
