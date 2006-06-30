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

package org.mulgara.itql;

// Java 2 standard packages
import java.net.*;

// Third party packages
import org.apache.log4j.Logger; // Apache Log4J

// Locally written packages

// Automatically generated packages (SableCC)
import org.kowari.itql.analysis.*;
import org.kowari.itql.node.*;
import org.mulgara.query.*;
import org.mulgara.query.rdf.*;

/**
 * A implementation of SableCC's Adapter which creates query constraint
 * depending on the type of object given (visitor pattern).  Tied heavily to
 * the ItqlInterpreter.
 *
 * @created 2004-06-18
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/04/10 00:00:06 $ by $Author: pgearon $
 *
 * @maintenanceAuthor $Author: pgearon $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ConstraintExpressionBuilder extends AnalysisAdapter {

  /**
   * The logger
   */
  private final static Logger logger =
      Logger.getLogger(ConstraintExpressionBuilder.class.getName());

  /**
   * The internal result of parsing a constraint expression.
   */
  private ConstraintExpression constraintExpression = null;

  /**
   * URI Syntax Exception - not null if exception occurred since last get.
   */
  private URISyntaxException uriException = null;

  /**
   * Query Exception - not null if exception occurred since last get.
   */
  private QueryException queryException = null;

  /**
   * The iTQL interpreter
   */
  private ItqlInterpreter interpreter;

  /**
   * Create a new builder.  Requires methods on the interpreter in order to
   * function correctly.
   *
   * @param newInterpreter the interpreter to use.
   */
  public ConstraintExpressionBuilder(ItqlInterpreter newInterpreter) {
    interpreter = newInterpreter;
  }

  /**
   * Returns the latest constraint expression or throws an exception if there
   * was an error creating the last expression.  Once called the constraint
   * expression object and exceptions are nulled.
   *
   * @throws QueryException if constraint does not represent a valid query
   * @throws URISyntaxException if the constraint contains a resource whose
   *     text violates <a href="http://www.isi.edu/in-notes/rfc2396.txt">RFC?2396</a>
   */
  public ConstraintExpression getConstraintExpression() throws QueryException,
      URISyntaxException {
    try {
      ConstraintExpression tmpExpression = constraintExpression;

      if (uriException != null) {
        throw uriException;
      }
      else if (queryException != null) {
        throw queryException;
      }
      else {
        return tmpExpression;
      }
    }
    finally {
      uriException = null;
      queryException = null;
      constraintExpression = null;
    }
  }

  /**
   * Sets constraint expression.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param newConstraintExpression the new expression.
   */
  private void setConstraintExpression(ConstraintExpression
      newConstraintExpression) {
    constraintExpression = newConstraintExpression;
  }

  public void caseAConstraintConstraintFactor(
      AConstraintConstraintFactor rawConstraintFactor) {

    // log that we've got a constraint constraint factor
    if (logger.isDebugEnabled()) {
      logger.debug("Found constraint constraint factor " + rawConstraintFactor);
    }

    // get the constraint
    PConstraint constraint =
        ((AConstraintConstraintFactor) rawConstraintFactor).getConstraint();

    // log that we've found a constraint
    if (logger.isDebugEnabled()) {
      logger.debug("Found constraint " + constraint + ", resolving components");
    }

    // get the constraint's components
    try {
      Constraint tmpConstraint;

      ConstraintElement subject =
          toConstraintElement(((AConstraint) constraint).getSubject());
      ConstraintElement predicate =
          toConstraintElement(((AConstraint) constraint).getPredicate());
      ConstraintElement object =
          toConstraintElement(((AConstraint) constraint).getObject());

      // log the components we've found
      if (logger.isDebugEnabled()) {
        logger.debug("Found subject " + subject);
        logger.debug("Found predicate " + predicate);
        logger.debug("Found object " + object);
      }

      // check for an IN clause
      AInClause inClause = (AInClause) ((AConstraint) constraint).getInClause();

      // bundle them into a constraint (order is probably important here...?)
      if (inClause != null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Found model " + inClause.getElement());
        }

        ConstraintElement model =
            toConstraintElement(inClause.getElement());
        tmpConstraint =
            ConstraintFactory.newConstraint(subject, predicate, object, model);
      }
      else {
        tmpConstraint = ConstraintFactory.newConstraint(subject, predicate, object);
      }

      // handle negated constraints
      if (((AConstraintConstraintFactor) rawConstraintFactor).getExclude() != null) {
        tmpConstraint = new ConstraintNegation(tmpConstraint);
      }

      // Set new value.
      if (logger.isDebugEnabled()) {
        logger.debug("Setting constraint: " + tmpConstraint);
      }

      setConstraintExpression(tmpConstraint);
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a constraint expression.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param rawConstraintFactor the expression to create a constraint expression
   *   from.
   */
  public void caseAExpressionConstraintFactor(
      AExpressionConstraintFactor rawConstraintFactor) {

    try {
      ConstraintExpression tmpConstraintExpression;

      // log that we've got an expression constraint factor
      if (logger.isDebugEnabled()) {
        logger.debug("Found factor expression constraint factor " +
            rawConstraintFactor);
      }

      // get the constraint expression
      PConstraintExpression embeddedConstraintExpression =
          ((AExpressionConstraintFactor) rawConstraintFactor).
          getConstraintExpression();

      // log that we're recursing with a constraint expression
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with constraint factor " +
            embeddedConstraintExpression);
      }

      // build the constraint expression
      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);
      embeddedConstraintExpression.apply((Switch) builder);

      tmpConstraintExpression = builder.getConstraintExpression();

      // handle negated expressions
      if (((AExpressionConstraintFactor) rawConstraintFactor).getExclude() != null) {
        tmpConstraintExpression = new ConstraintNegation(
            (Constraint) tmpConstraintExpression);
      }

      setConstraintExpression(tmpConstraintExpression);
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a transitive constraint.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param rawConstraintFactor a transitive constraint.
   */
  public void caseATransitiveConstraintFactor(
      ATransitiveConstraintFactor rawConstraintFactor) {

    try {
      ConstraintExpression tmpConstraintExpression = null;

      // log that we've found a transitive predicate expression
      if (logger.isDebugEnabled()) {
        logger.debug("Found factor of transitive expression" +
            rawConstraintFactor);
      }

      // get the constraint transitive
      PTransitiveClause embeddedTransitiveConstraint =
          ((ATransitiveConstraintFactor) rawConstraintFactor).
          getTransitiveClause();

      if (embeddedTransitiveConstraint instanceof ATransitive1TransitiveClause) {

        // build the transitive constraint expression
        tmpConstraintExpression = new SingleTransitiveConstraint(
            buildConstraint(((ATransitive1TransitiveClause)
            embeddedTransitiveConstraint).getConstraint()));
      }
      else if (embeddedTransitiveConstraint instanceof ATransitive2TransitiveClause) {

        // build the transitive constraint expression
        ATransitive2TransitiveClause tmpClause = (ATransitive2TransitiveClause)
            embeddedTransitiveConstraint;
        Constraint constraint1 = buildConstraint(tmpClause.getConstraint1());
        Constraint constraint2 = buildConstraint(tmpClause.getConstraint2());
        tmpConstraintExpression = new TransitiveConstraint(constraint1,
            constraint2);
      }

      setConstraintExpression(tmpConstraintExpression);
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a walk constraint.
   *
   * @param rawConstraintFactor a walk constraint.
   */
  public void caseAWalkConstraintFactor(AWalkConstraintFactor
      rawConstraintFactor) {

    try {

      // Build the walk constraint
      AWalk1WalkClause embeddedWalkConstraint = (AWalk1WalkClause)
          ((AWalkConstraintFactor) rawConstraintFactor).getWalkClause();

      ConstraintExpression tmpConstraintExpression = new WalkConstraint(
          buildConstraint(embeddedWalkConstraint.getConstraint1()),
          buildConstraint(embeddedWalkConstraint.getConstraint2()));

      setConstraintExpression(tmpConstraintExpression);
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle an OR constraint.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param rawConstraintExpression an OR constraint.
   */
  public void caseAOrConstraintExpression(
      AOrConstraintExpression rawConstraintExpression) {

    try {

      // log that we've found a OR constraint expression
      if (logger.isDebugEnabled()) {
        logger.debug("Found OR constraint expression " +
            rawConstraintExpression);
      }

      // get the OR constraint expression
      PConstraintExpression orConstraintExpression =
          ((AOrConstraintExpression) rawConstraintExpression).
          getConstraintExpression();

      // get the constraint term
      PConstraintTerm constraintTerm =
          ((AOrConstraintExpression) rawConstraintExpression).getConstraintTerm();

      // log that we've found the operands of the disjunction
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with constraint expression " +
            orConstraintExpression + " & constraint term " +
            constraintTerm);
      }

      // Construct a builder to process the constraints.
      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);

      // get the LHS and RHS operands of the disjunction
      orConstraintExpression.apply((Switch) builder);
      ConstraintExpression lhs = builder.getConstraintExpression();
      constraintTerm.apply((Switch) builder);
      ConstraintExpression rhs = builder.getConstraintExpression();

      // log that we've resolved the operands
      if (logger.isDebugEnabled()) {
        logger.debug("Resolved LHS disjunction operand " + lhs);
        logger.debug("Resolved RHS disjunction operand " + rhs);
      }

      // apply the disjunction
      setConstraintExpression(new ConstraintDisjunction(lhs, rhs));
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a term constraint.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param rawConstraintExpression a term constraint.
   */
  public void caseATermConstraintExpression(
      ATermConstraintExpression rawConstraintExpression) {

    try {

      // log that we've got a term constraint expression
      if (logger.isDebugEnabled()) {
        logger.debug("Found term constraint expression " +
            rawConstraintExpression);
      }

      // get the constraint term
      PConstraintTerm constraintTerm =
          ((ATermConstraintExpression) rawConstraintExpression).
          getConstraintTerm();

      // log that we're about to resolve the term into an expression
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with constraint term " + constraintTerm);
      }

      // Create a new builder.
      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);
      constraintTerm.apply((Switch) builder);

      // drill down into the constraint term
      setConstraintExpression(builder.getConstraintExpression());
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a dterm constraint term.  Will set URIException or QueryException
   * if an exception occurs.
   *
   * @param rawConstraintTerm a dterm constraint term.
   */
  public void caseADtermConstraintTerm(
      ADtermConstraintTerm rawConstraintTerm) {

    try {

      // log that we've got a dterm constraint term
      logger.debug("Found dterm contraint term " + rawConstraintTerm);

      // get the constraint factor
      PConstraintDterm constraintDterm =
          ((ADtermConstraintTerm) rawConstraintTerm).getConstraintDterm();

      ConstraintExpression tmpConstraintExpression = null;

      // drill down into the constraint factor
      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);
      constraintDterm.apply((Switch) builder);
      tmpConstraintExpression = builder.getConstraintExpression();
      setConstraintExpression(tmpConstraintExpression);

      // log that we're recursing with a constraint factor
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with constraint factor " + constraintDterm);
        logger.debug("Got: " + tmpConstraintExpression);
      }
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a AND constraint term.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param rawConstraintTerm a AND constraint term.
   */
  public void caseAAndConstraintTerm(AAndConstraintTerm rawConstraintTerm) {

    try {

      // log that we've got a AND constraint term
      if (logger.isDebugEnabled()) {
        logger.debug("Found AND contraint term " + rawConstraintTerm);
      }

      // get the constraint term
      PConstraintTerm constraintTerm =
          ((AAndConstraintTerm) rawConstraintTerm).getConstraintTerm();

      // get the constraint factor
      PConstraintDterm constraintDterm =
          ((AAndConstraintTerm) rawConstraintTerm).getConstraintDterm();

      // log that we've found the operands of the disjunction
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with constraint term " + constraintTerm +
            " & constraint factor " + constraintDterm);
      }

      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);

      // get the LHS and RHS operands of the conjunction
      constraintTerm.apply((Switch) builder);
      ConstraintExpression lhs = builder.getConstraintExpression();

      // Create another constraint builder and assign to RHS.
      constraintDterm.apply((Switch) builder);
      ConstraintExpression rhs = builder.getConstraintExpression();

      // log that we've resolved the operands
      if (logger.isDebugEnabled()) {
        logger.debug("Resolved LHS conjunction operand " + lhs);
        logger.debug("Resolved RHS conjunction operand " + rhs);
      }

      // apply the conjunction
      setConstraintExpression(new ConstraintConjunction(lhs, rhs));
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a factor constraint dterm.  Will set URIException or QueryException
   * if an exception occurs.
   *
   * @param rawConstraintTerm a factor constraint dterm.
   */
  public void caseAFactorConstraintDterm(
      AFactorConstraintDterm rawConstraintTerm) {

    try {

      // log that we've got a factor constraint term
      logger.debug("Found factor contraint term " + rawConstraintTerm);

      // get the constraint factor
      PConstraintFactor constraintFactor =
          ((AFactorConstraintDterm) rawConstraintTerm).getConstraintFactor();

      ConstraintExpression tmpConstraintExpression = null;

      // drill down into the constraint factor
      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);
      constraintFactor.apply((Switch) builder);
      tmpConstraintExpression = builder.getConstraintExpression();
      setConstraintExpression(tmpConstraintExpression);

      // log that we're recursing with a constraint factor
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with constraint factor " + constraintFactor);
        logger.debug("Got: " + tmpConstraintExpression);
      }
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Handle a MINUS constraint dterm.  Will set URIException or QueryException if
   * an exception occurs.
   *
   * @param rawConstraintTerm a MINUS constraint dterm.
   */
  public void caseAMinusConstraintDterm(AMinusConstraintDterm rawConstraintTerm) {
    // TODO

    try {

      // log that we've got a MINUS constraint dterm
      if (logger.isDebugEnabled()) {
        logger.debug("Found MINUS contraint dterm " + rawConstraintTerm);
      }

      // get the minuend expression
      PConstraintDterm minuendExpr =
          ((AMinusConstraintDterm) rawConstraintTerm).getMinuend();

      // get the subtrahend expression
      PConstraintFactor subtrahendExpr =
          ((AMinusConstraintDterm) rawConstraintTerm).getSubtrahend();

      // log that we've found the operands of the subtraction
      if (logger.isDebugEnabled()) {
        logger.debug("Recursing with minuend " + minuendExpr +
            " & subtrahend " + subtrahendExpr);
      }

      ConstraintExpressionBuilder builder =
          new ConstraintExpressionBuilder(interpreter);

      // get the LHS and RHS operands of the conjunction
      minuendExpr.apply((Switch) builder);
      ConstraintExpression minuend = builder.getConstraintExpression();

      // Create another constraint builder and assign to RHS.
      subtrahendExpr.apply((Switch) builder);
      ConstraintExpression subtrahend = builder.getConstraintExpression();

      // log that we've resolved the operands
      if (logger.isDebugEnabled()) {
        logger.debug("Resolved minuend operand " + minuend);
        logger.debug("Resolved subtrahend operand " + subtrahend);
      }

      // apply the conjunction
      setConstraintExpression(new ConstraintDifference(minuend, subtrahend));
    }
    catch (URISyntaxException use) {
      uriException = use;
    }
    catch (QueryException qe) {
      queryException = qe;
    }
  }

  /**
   * Helper method used to build up a Constraint object from a SableCC
   * PConstraint object.
   *
   * @param pconstraint The parser constraint object to convert.
   * @return A Constraint object for use in transitive constraints.
   * @throws QueryException if the constraint contains a NOT or IN section.
   */
  Constraint buildConstraint(PConstraint pconstraint) throws QueryException,
      URISyntaxException {

    // log that we've found a constraint
    if (logger.isDebugEnabled()) {
      logger.debug("Found constraint " + pconstraint + ", resolving components");
    }

    // get the constraint's components
    ConstraintElement subject =
        toConstraintElement(((AConstraint) pconstraint).getSubject());
    ConstraintElement predicate =
        toConstraintElement(((AConstraint) pconstraint).getPredicate());
    ConstraintElement object =
        toConstraintElement(((AConstraint) pconstraint).getObject());

    // log the components we've found
    if (logger.isDebugEnabled()) {
      logger.debug("Found subject " + subject);
      logger.debug("Found predicate " + predicate);
      logger.debug("Found object " + object);
    }

    // check for an IN - this is illegal
    if (((AConstraint) pconstraint).getInClause() != null) {
      throw new QueryException("Illegal in clause on transitive constraint.");
    }

    // bundle them into a constraint (order is probably important here...?)
    return ConstraintFactory.newConstraint(subject, predicate, object);
  }

  /**
   * Constructs a {@link org.mulgara.query.ConstraintElement} from a
   * {@link org.kowari.itql.node.PElement}.
   *
   * @param element an element of a constraint (variable, resource or literal)
   * @return a constraint element for the given <code>element</code>
   * @throws QueryException if <code>element</code> is a
   *      {@link org.kowari.itql.node.AResourceElement} whose text contains a
   *      <a href="http://www.w3.org/TR/REC-xml-names/#ns-qualnames">qualified name</a>
   *      with a prefix not defined in the <code>aliasMap</code>
   * @throws URISyntaxException if <code>element</code> is a
   *      {@link org.kowari.itql.node.AResourceElement} whose text doesn't
   *      conform to
   *      <a href="http://www.isi.edu/in-notes/rfc2396.txt">RFC 2396</a>
   */
  private ConstraintElement toConstraintElement(PElement element) throws
      QueryException, URISyntaxException {

    // validate the element parameter
    if (element == null) {
      throw new IllegalArgumentException("Null \"element\" parameter");
    }

    // log what we're doing
    if (logger.isDebugEnabled()) {
      logger.debug("Resolving " + element + "to a constraint element");
    }

    // create a constraint element to return
    ConstraintElement constraintElement = null;

    // get the appropriate element type
    if (element instanceof AVariableElement) {

      // get the name of the variable
      PVariable rawVariable = ((AVariableElement) element).getVariable();
      String variableName = ((AVariable) rawVariable).getIdentifier().getText();

      // log what we're doing
      if (logger.isDebugEnabled()) {
        logger.debug("Resolved " + element + " to variable " + variableName);
      }

      // create a new variable
      constraintElement = new Variable(variableName);
    }
    else if (element instanceof AResourceElement) {

      // create a new resource
      constraintElement = new URIReferenceImpl(interpreter.toURI(
          ((AResourceElement) element).getResource()));
    }
    else if (element instanceof ALiteralElement) {

      // create a new literal
      constraintElement = interpreter.toLiteralImpl(((ALiteralElement) element).
          getLiteral());
    }

    // return the constraint element
    return constraintElement;
  }
}
