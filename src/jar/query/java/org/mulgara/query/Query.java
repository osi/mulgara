/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the Kowari Metadata Store.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.query;

// Java 2 standard packages;
import java.io.*;
import java.util.*;

// Third party packages
import org.apache.log4j.*;

/**
 * An ITQL query. This is a data structure used as an argument to the
 * {@link org.mulgara.server.Session#query} method.
 *
 * @created 2001-08-12
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:20 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Query implements Cloneable, Serializable {

 /**
  * Allow newer compiled version of the stub to operate when changes
  * have not occurred with the class.
  * NOTE : update this serialVersionUID when a method or a public member is
  * deleted.
  * NOTE: Incremented UID to reflect removal of relatedTo.
  */
  static final long serialVersionUID = 7973523792022156621L;

  /**
   * Logger.
   */
  private static Logger logger = Logger.getLogger(Query.class.getName());

  /**
   * The variable list. This may only contain {@link Variable}s. It corresponds
   * to the <code>select</code> clause. If it is <code>null</code>, it indicates
   * that there is no <code>select</code> clause and that no projection will be
   * performed.
   */
  private List variableList;

  /**
   * Mutable version of the variable list. This isn't exposed via {@link
   * #getVariableList} the way {@link #variableList} is.
   */
  private List mutableVariableList;

  /**
   * The model expression.
   *
   * It corresponds to the <code>from</code> clause.
   */
  private ModelExpression modelExpression;

  /**
   * The constraint expression.  It corresponds to the <code>where</code> clause.
   */
  private ConstraintExpression constraintExpression;

  /**
   * The having expression.  It corresponds to the <code>having</code> clause.
   */
  private ConstraintHaving havingConstraint;

  /**
   * The sort ordering. The elements of this list should be {@link Order}s, with
   * major orderings preceding minor orderings. It's only sensible for this to
   * contain orders on variables in the {@link #variableList}.
   */
  private List orderList;

  /**
   * The limit on rows in the result. If this is <code>null</code>, it indicates
   * that there is no limit.
   */
  private Integer limit;

  /**
   * The offset on rows in the result. This value is never negative.
   */
  private int offset;

  /**
   * The accumulated solutions. This can be <code>null</code>, indicating no
   * solutions.
   */
  private Answer answer;

  //
  // Constructors
  //

  /**
   * Construct a query.
   *
   * @param variableList {@link Variable}s or node values to appear as bindings
   *     in the solution (i.e. columns of the result {@link Answer});
   *     <code>null</code> indicates that all columns are to be retained
   * @param modelExpression an expression defining the model to query, never
   *     <code>null</code>
   * @param constraintExpression an expression defining the constraints to
   *     satisfy, never <code>null</code>
   * @param havingExpression an expression defining the conditions to apply to
   *     aggregate functions or null if not given.
   * @param orderList sort order column names, currently a list of {@link
   *     Variable}s with the order assumed to be ascending in all cases
   * @param limit the maximum number of rows to return, which must be
   *     non-negative; <code>null</code> indicates no limit
   * @param offset the number of rows to skip from the beginning of the result,
   *     never negative
   * @param answer an existing solution set to which results must belong, or
   *     {@link UnconstrainedAnswer} for no constraints; never
   *     <code>null</code> is
   * @throws IllegalArgumentException if <var>limit</var> or <var>offset</var>
   *     are negative, or if <var>modelExpression</var>,
   *     <var>constraintExpression</var>, <var>orderList<var> or
   *     <var>answer</var> are <code>null</code>
   */
  public Query(List variableList, ModelExpression modelExpression,
      ConstraintExpression constraintExpression,
      ConstraintHaving havingExpression, List orderList, Integer limit,
      int offset, Answer answer) {

    // Validate parameters
    if (modelExpression == null) {
      throw new IllegalArgumentException("Null \"modelExpression\" parameter");
    } else if (constraintExpression == null) {
      throw new IllegalArgumentException("Null \"constraintExpression\" parameter");
    } else if ((limit != null) && (limit.intValue() < 0)) {
      throw new IllegalArgumentException("Negative \"limit\" parameter");
    } else if (orderList == null) {
      throw new IllegalArgumentException("Null \"orderList\" parameter");
    } else if (offset < 0) {
      throw new IllegalArgumentException("Negative \"offset\" parameter");
    } else if (answer == null) {
      throw new IllegalArgumentException("Null \"answer\" parameter");
    } else if (variableList != null) {
      Set variableSet = new HashSet(constraintExpression.getVariables());
      variableSet.addAll(Arrays.asList(answer.getVariables()));

      Iterator i = variableList.iterator();
      while (i.hasNext()) {
        Object o = i.next();
        if (o instanceof Variable) {
          Variable var = (Variable) o;
          if (!variableSet.contains(var)) {
            logger.warn("Failed to find " + var + " in " + variableSet);
            throw new IllegalArgumentException("Failed to constrain all variables: " + var +
                " not constrained in WHERE or GIVEN clauses");
          }
        }
      }
    }

    // Initialize fields
    this.mutableVariableList =
        (variableList == null) ? null : new ArrayList(variableList);
    this.variableList =
        (variableList == null) ? null
        : Collections.unmodifiableList(mutableVariableList);
    this.modelExpression = modelExpression;
    this.constraintExpression = constraintExpression;
    this.havingConstraint = havingExpression;
    this.orderList = Collections.unmodifiableList(new ArrayList(orderList));
    this.limit = limit;
    this.offset = offset;
    this.answer = answer;
  }

  /**
   * Cloning must always be supported.
   */
  public Object clone() {

    Query cloned;
    try {
      cloned = (Query) super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw new RuntimeException("Query subclass "+getClass()+" not cloneable");
    }

    // Copy mutable fields by value
    cloned.mutableVariableList =
        (variableList == null) ? null : new ArrayList(variableList);
    cloned.variableList =
        (variableList == null) ? null
        : Collections.unmodifiableList(cloned.mutableVariableList);
    cloned.modelExpression = modelExpression;  // FIXME: should be cloned
    cloned.answer = (Answer) answer.clone();

    // Copy immutable fields by reference
    cloned.orderList = orderList;
    cloned.limit = limit;
    cloned.offset = offset;

    return cloned;
  }

  //
  // API methods
  //

  /**
   * Accessor for the <code>variableList</code> property.
   *
   * @return a {@link List} containing one or more {@link Variable}s
   */
  public List getVariableList() {
    return variableList;
  }

  /**
   * Accessor for the <code>constraintExpression</code> property.
   *
   * @return a {@link ConstraintExpression}
   */
  public ConstraintExpression getConstraintExpression() {
    return constraintExpression;
  }

  /**
   * Accesor for the <code>havingExpression</code> property.
   *
   * @return a {@link ConstraintExpression} containing only
   *   {@link ConstraintHaving} or <code>null</code> to indicate an empty
   *   having clause.
   */
  public ConstraintHaving getHavingExpression() {
    return havingConstraint;
  }

  /**
   * Accessor for the <code>modelExpression</code> property.
   *
   * @return a {@link ModelExpression}, or <code>null</code> to indicate the
   *      empty model
   */
  public ModelExpression getModelExpression() {
    return modelExpression;
  }

  /**
   * Accessor for the <code>orderList</code> property.
   *
   * @return a {@link List} containing one or more {@link Variable}s
   */
  public List getOrderList() {
    return orderList;
  }

  /**
   * Accessor for the <code>limit</code> property.
   *
   * @return the limit for this query, or <code>null</code> if unlimited
   */
  public Integer getLimit() {
    return limit;
  }

  /**
   * Accessor for the <code>offset</code> property.
   *
   * @return the offset for this query, a non-negative integer
   */
  public int getOffset() {
    return offset;
  }

  /**
   * Accessor for the <code>answer</code> property. If the <var>
   * constraintExpression</var> property is <code>null</code>, this is the
   * answer to the entire query.
   *
   * @return an {@link Answer}, or <code>null</code> to indicate the set of all
   *      statements
   */
  public Answer getGiven() {
    return answer;
  }

  //
  // Methods overriding Object
  //

  /**
   * Equality is by value.
   *
   * @param object PARAMETER TO DO
   * @return RETURNED VALUE TO DO
   */
  public boolean equals(Object object) {

    if (object == this) {

      return true;
    }

    if (object == null) {

      return false;
    }

    if (! (object instanceof Query)) {

      return false;
    }

    Query query = (Query) object;

    // Check the variableList field
    if (!variableList.equals(query.variableList)) {

      return false;
    }

    // Check the modelExpression field
    if ( (modelExpression == null) ? (query.modelExpression != null)
        : (!modelExpression.equals(
        query.modelExpression))) {

      return false;
    }

    // Check the constraintExpression field
    if ((constraintExpression == null) ? (query.constraintExpression != null)
        : (!constraintExpression.equals(query.constraintExpression))) {

      return false;
    }

    if ((havingConstraint == null) ? (query.havingConstraint != null)
        : (!havingConstraint.equals(query.havingConstraint))) {

      return false;
    }

    // Check the orderList field
    if ( (orderList == null) ^ (query.orderList == null)) {

      return false;
    }

    if ( (orderList != null) && !orderList.equals(query.orderList)) {

      return false;
    }

    // Check the limit field
    if ( (limit == null) ^ (query.limit == null)) {

      return false;
    }

    if ( (limit != null) && !limit.equals(query.limit)) {

      return false;
    }

    // Check the offset field
    if (offset != query.offset) {

      return false;
    }

    // Check the answer field
    if (!answer.equals(query.answer)) {

      return false;
    }

    // All checks passed, so the object is equal
    return true;
  }

  /**
   * Close this {@link Query}, and the underlying {@link Answer} objects.
   */
  public void close() throws TuplesException {
    answer.close();
    answer = null;

    if (mutableVariableList != null) {
      Iterator it = mutableVariableList.iterator();
      while (it.hasNext()) {
        Object v = it.next();
        if (v instanceof AggregateFunction)
          ((AggregateFunction)v).getQuery().close();
      }
    }
  }

  /**
   * Generate a legible representation of the query.
   *
   * @return RETURNED VALUE TO DO
   */
  public String toString() {

    StringBuffer buffer = new StringBuffer();

    // SELECT
    if (variableList != null) {

      buffer.append("SELECT");

      for (Iterator i = variableList.iterator(); i.hasNext(); ) {

        buffer.append(" ").append(i.next());
      }

      buffer.append(" ");
    }

    // FROM
    buffer.append("FROM ").append(modelExpression);

    // WHERE
    buffer.append(" WHERE ").append(constraintExpression);

    // HAVING
    if (havingConstraint != null) {
      buffer.append(" HAVING ").append(havingConstraint);
    }

    // ORDER BY
    if (!orderList.isEmpty()) {

      buffer.append(" ORDER BY");

      for (Iterator i = orderList.iterator(); i.hasNext(); ) {

        buffer.append(" ").append(i.next());
      }
    }

    // LIMIT
    if (limit != null) {

      buffer.append(" LIMIT ").append(limit.intValue());
    }

    // OFFSET
    if (offset != 0) {

      buffer.append(" OFFSET ").append(offset);
    }

    // GIVEN
    if (answer != null) {

      buffer.append(" GIVEN ").append(answer);
    }

    return buffer.toString();
  }


  /**
   * Serializes the current object to a stream.
   *
   * @param out The stream to write to.
   * @throws IOException If an I/O error occurs while writing.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    // convert answer to be serializable if needed
    if (!(answer instanceof Serializable)) {
      // TODO: use a remote answer object if the given is too large
      try {
        Answer tmpAnswer = answer;
        answer = new ArrayAnswer(answer);
        tmpAnswer.close();
      } catch (TuplesException e) {
        throw new IOException("Unable to serialize GIVEN clause");
      }
    }
    out.defaultWriteObject();
  }
}
