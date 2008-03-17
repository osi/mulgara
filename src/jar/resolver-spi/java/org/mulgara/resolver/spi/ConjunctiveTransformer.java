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
 * Northrop Grumman Corporation. All Rights Reserved.
 *
 * This file is an original work and contains no Original Code.  It was
 * developed by Netymon Pty Ltd under contract to the Australian 
 * Commonwealth Government, Defense Science and Technology Organisation
 * under contract #4500507038 and is contributed back to the Kowari/Mulgara
 * Project as per clauses 4.1.3 and 4.1.4 of the above contract.
 *
 * Contributor(s): N/A.
 *
 * Copyright:
 *   The copyright on this file is held by:
 *     The Australian Commonwealth Government
 *     Department of Defense
 *   Developed by Netymon Pty Ltd
 * Copyright (C) 2006
 * The Australian Commonwealth Government
 * Department of Defense
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */
package org.mulgara.resolver.spi;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.net.URI;

import org.jrdf.graph.URIReference;

import org.mulgara.query.ConstraintImpl;
import org.mulgara.query.Constraint;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.Variable;
import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ConstraintConjunction;
import org.mulgara.query.ConstraintDisjunction;
import org.mulgara.query.QueryException;
import org.mulgara.resolver.spi.SymbolicTransformation;
import org.mulgara.resolver.spi.SymbolicTransformationContext;
import org.mulgara.resolver.spi.SymbolicTransformationException;

/**
 * A transformer that works on the basis of combining multiple conjoined constraints
 * into a single compound constraint.
 *
 * @modified $Date: 2005/10/30 19:21:17 $ by $Author: prototypo $
 * @maintenanceAuthor $Author: prototypo $
 * @copyright &copy;2006 <a href="http://www.defence.gov.au/">
 *      Australian Commonwealth Government, Department of Defence</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class ConjunctiveTransformer implements SymbolicTransformation
{
  /** Logger */
  private static final Logger logger = Logger.getLogger(ConjunctiveTransformer.class.getName());

  protected URI modelTypeURI;

  public ConjunctiveTransformer(URI modelTypeURI) {
    this.modelTypeURI = modelTypeURI;
  }

  public abstract ConstraintExpression constructConstraintExpression(ConstraintElement model, Map byVarSubject, Map byConstSubject) throws SymbolicTransformationException;

  public void transform(SymbolicTransformationContext context, MutableLocalQuery query)
      throws SymbolicTransformationException {

    ConstraintExpression expr = query.getConstraintExpression();
    ConstraintExpression trans = transformExpr(context, expr);

    if (expr != trans) {
      query.setConstraintExpression(trans);
    }
  }


  public ConstraintExpression transformExpr(SymbolicTransformationContext context, ConstraintExpression expr) throws SymbolicTransformationException {

    // This is the main case.
    if (expr instanceof ConstraintConjunction) {
      return transformConj(context, (ConstraintConjunction)expr);
    }

    // In the case of a Disjunction we need to attempt to transform it's arguments
    // should the query be in sum of product form.
    if (expr instanceof ConstraintDisjunction) {
      return transformDisj(context, (ConstraintDisjunction)expr);
    }

    // A single constraint could still be transformed as a singleton conjunction.
    // Therefore pack in conjunction, attempt transform, and check to see if it was.
    if (expr instanceof ConstraintImpl) {
      ConstraintConjunction conj = new ConstraintConjunction(Arrays.asList(new ConstraintExpression[] { expr }));
      ConstraintConjunction trans = transformConj(context, conj);
      if (conj == trans) {
        return expr;
      } else {
        return trans;
      }
    }

    // By default we do not recognise the constraint type, so pass it unchanged.
    return expr;
  }


  public ConstraintConjunction transformConj(SymbolicTransformationContext context, ConstraintConjunction cc) throws SymbolicTransformationException {

    ConjAccumulator acc = transformConj(context, cc, new ConjAccumulator(context));

    if (!acc.isTransformed()) {
      return cc;
    }

    List args = new ArrayList();
    args.addAll(acc.getResidualArgs());

    Map varSubByModel = acc.getVarArgsByModel();
    Map constSubByModel = acc.getConstArgsByModel();

    Set modelSet = new HashSet();
    modelSet.addAll(varSubByModel.keySet());
    modelSet.addAll(constSubByModel.keySet());
    Iterator models = modelSet.iterator();
    while (models.hasNext()) {
      ConstraintElement model = (ConstraintElement)models.next();
      args.add(constructConstraintExpression(model, (Map)varSubByModel.get(model), (Map)constSubByModel.get(model)));
    }

    return new ConstraintConjunction(args);
  }


  public ConjAccumulator transformConj(SymbolicTransformationContext context, ConstraintConjunction cc, ConjAccumulator acc)
      throws SymbolicTransformationException {

    Iterator args = cc.getElements().iterator();
    while (args.hasNext()) {
      ConstraintExpression arg = (ConstraintExpression)args.next();
      if (arg instanceof ConstraintConjunction) {
        acc = transformConj(context, (ConstraintConjunction)arg, acc);
      } else if (arg instanceof ConstraintDisjunction) {
        ConstraintExpression expr = transformDisj(context, (ConstraintDisjunction)arg);
        acc.accumulate(expr);
      } else {
        acc.accumulate(arg);
      }
    }

    return acc;
  }


  public ConstraintExpression transformDisj(SymbolicTransformationContext context, ConstraintDisjunction cd) throws SymbolicTransformationException {

    List transArgs = new ArrayList();
    boolean transformed = false;
    Iterator i = cd.getElements().iterator();
    while (i.hasNext()) {
      ConstraintExpression ce = (ConstraintExpression)i.next();
      ConstraintExpression trans = transformExpr(context, ce);
      if (trans != ce) {
        transformed = true;
      }
      transArgs.add(trans);
    }

    return transformed ? new ConstraintDisjunction(transArgs) : cd;
  }
  

  private class ConjAccumulator {
    private SymbolicTransformationContext context;

    private Map varArgsByModel;
    private Map constArgsByModel;
    private List residualArgs;
    private boolean transformed;

    public ConjAccumulator(SymbolicTransformationContext context) {
      this.context = context;
      varArgsByModel = new HashMap();
      constArgsByModel = new HashMap();
      residualArgs = new ArrayList();
    }


    public void accumulate(ConstraintExpression arg) throws SymbolicTransformationException {
      if (arg instanceof ConstraintImpl) {
        accumulateConstraint((ConstraintImpl)arg);
      } else if (arg instanceof ConstraintConjunction) {
        throw new IllegalStateException("ConstraintConjunction should have been handled by caller");
      } else {
        residualArgs.add(arg);
      }
    }

    private void accumulateConstraint(ConstraintImpl arg) throws SymbolicTransformationException {
      try {
        ConstraintElement model = arg.getGraph();
        if (model instanceof URIReference) {
          URIReference cu = (URIReference)model;
          URI constraintModelType = context.mapToModelTypeURI(cu.getURI());
          if (constraintModelType.equals(modelTypeURI)) {
            ConstraintElement subject = arg.getElement(0);
            if (subject instanceof Variable) {
              insertByModel(model, varArgsByModel, arg);
            } else {
              insertByModel(model, constArgsByModel, arg);
            }
          } else {
            residualArgs.add(arg);
          }
        } else {
          residualArgs.add(arg);
        }
      } catch (QueryException eq) {
          throw new SymbolicTransformationException("Failed to match model with modeltype", eq);
      }
    }


    private void insertByModel(ConstraintElement model, Map target, ConstraintImpl arg) {
      Map bySubject = (Map)target.get(model);
      if (bySubject == null) {
        bySubject = new HashMap();
        target.put(model, bySubject);
      }

      Map byPredicate = (Map)bySubject.get(arg.getElement(0));
      if (byPredicate == null) {
        byPredicate = new HashMap();
        bySubject.put(arg.getElement(0), byPredicate);
      }

      List objects = (List)byPredicate.get(arg.getElement(1));
      if (objects == null) {
        objects = new ArrayList();
        byPredicate.put(arg.getElement(1), objects);
      }

      objects.add(arg.getElement(2));
    }


    public boolean isTransformed() {
      return (varArgsByModel.size() != 0) || (constArgsByModel.size() != 0);
    }

    public List getResidualArgs() {
      return residualArgs;
    }

    public Map getVarArgsByModel() {
      return varArgsByModel;
    }

    public Map getConstArgsByModel() {
      return constArgsByModel;
    }
  }
}
