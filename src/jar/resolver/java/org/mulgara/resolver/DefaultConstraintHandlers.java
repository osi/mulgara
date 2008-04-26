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
 *   Various modifications to this file copyright:
 *     The Australian Commonwealth Government
 *     Department of Defense
 *   Developed by Netymon Pty Ltd
 *   under contract 4500430665
 *   contributed to the Kowari Project under the
 *     Mozilla Public License version 1.1
 *   per clause 4.1.3 of the above contract.
 *
 *   Various modifications to this file copyright:
 *     2005-2006 Netymon Pty Ltd: mail@netymon.com
 *
 *   Various modifications to this file copyright:
 *     2005-2007 Andrae Muys: andrae@muys.id.au
 *
 *   getModel() contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *   ConstraintLocalization contributed by Netymon Pty Ltd on behalf of
 *   The Australian Commonwealth Government under contract 4500507038.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.resolver;

// Java 2 standard packages
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;
import org.jrdf.graph.URIReference;

// Local packages
import org.mulgara.query.*;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.ConstraintBindingHandler;
import org.mulgara.resolver.spi.ConstraintLocalization;
import org.mulgara.resolver.spi.ConstraintModelRewrite;
import org.mulgara.resolver.spi.ConstraintResolutionHandler;
import org.mulgara.resolver.spi.ModelResolutionHandler;
import org.mulgara.resolver.spi.QueryEvaluationContext;
import org.mulgara.store.tuples.Tuples;
import org.mulgara.store.tuples.TuplesOperations;
import org.mulgara.util.NVPair;

/**
 * Provides handlers for the standard constraints included in mulgara.
 *
 * @created 2007-11-09
 * @author <a href="mailto:andrae@netymon.com">Andrae Muys</a>
 * @company <a href="http://www.netymon.com">Netymon Pty Ltd</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class DefaultConstraintHandlers
{
  /** Logger.  */
  @SuppressWarnings("unused")
  private static final Logger logger = Logger.getLogger(DefaultConstraintHandlers.class.getName());

  static void initializeHandlers() {
    initializeModelResolutionHandlers();
    initializeConstraintResolutionHandlers();
    initializeConstraintBindingHandlers();
    initializeConstraintModelRewrites();
    initializeConstraintLocalizations();
  }

  @SuppressWarnings("unchecked")
  static void initializeModelResolutionHandlers() {
    ConstraintOperations.addModelResolutionHandlers(new NVPair[]
      {
        new NVPair(ModelUnion.class, new ModelResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr,
                                Constraint constraint) throws Exception {
            Tuples lhs = ConstraintOperations.
                resolveModelExpression(context, ((ModelOperation)modelExpr).getLHS(), constraint);
            Tuples rhs = ConstraintOperations.
                resolveModelExpression(context, ((ModelOperation)modelExpr).getRHS(), constraint);
            try {
              return TuplesOperations.append(lhs, rhs);
            } finally {
              lhs.close();
              rhs.close();
            }
          }
        }),
        new NVPair(ModelIntersection.class, new ModelResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr,
                                Constraint constraint) throws Exception {
            Tuples lhs = ConstraintOperations.
                resolveModelExpression(context, ((ModelOperation)modelExpr).getLHS(), constraint);
            Tuples rhs = ConstraintOperations.
                resolveModelExpression(context, ((ModelOperation)modelExpr).getRHS(), constraint);
            try {
              return TuplesOperations.join(lhs, rhs);
            } finally {
              lhs.close();
              rhs.close();
            }
          }
        }),
        new NVPair(ModelResource.class, new ModelResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr,
                                Constraint constraint) throws Exception {
            return context.resolve((ModelResource)modelExpr, (Constraint)constraint);
          }
        }),
        new NVPair(ModelVariable.class, new ModelResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr,
                                Constraint constraint) throws Exception {
            return context.resolve(null, ConstraintOperations.rewriteConstraintModel(((ModelVariable)modelExpr).getVariable(), constraint));
          }
        })
      });
  }

  @SuppressWarnings("unchecked")
  static void initializeConstraintResolutionHandlers() {
    ConstraintOperations.addConstraintResolutionHandlers(new NVPair[]
      {
        new NVPair(ConstraintConjunction.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            List l =
                context.resolveConstraintOperation(modelExpr, (ConstraintOperation)constraintExpr);
            try {
              return TuplesOperations.join(l);
            } finally {
              Iterator i = l.iterator();
              while (i.hasNext()) {
                ((Tuples)i.next()).close();
              }
            }
          }
        }),
        new NVPair(ConstraintDisjunction.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            List l =
                context.resolveConstraintOperation(modelExpr, (ConstraintOperation)constraintExpr);
            try {
              return TuplesOperations.append(l);
            } finally {
              Iterator i = l.iterator();
              while (i.hasNext()) {
                ((Tuples)i.next()).close();
              }
            }
          }
        }),
        new NVPair(ConstraintDifference.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            List args = context.resolveConstraintOperation(modelExpr, (ConstraintOperation)constraintExpr);
            assert args.size() == 2;
            try {
              return TuplesOperations.subtract((Tuples)args.get(0), (Tuples)args.get(1));
            } finally {
              ((Tuples)args.get(0)).close();
              ((Tuples)args.get(1)).close();
            }
          }
        }),
        new NVPair(ConstraintOptionalJoin.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            List<Tuples> args = context.resolveConstraintOperation(modelExpr, (ConstraintOperation)constraintExpr);
            LinkedList<Tuples> stackedArgs;
            // we know this is a linked list, but test just in case it is ever changed.
            if (args instanceof LinkedList) stackedArgs = (LinkedList<Tuples>)args;
            else stackedArgs = new LinkedList<Tuples>(args);
            try {
              return TuplesOperations.optionalJoin(stackedArgs);
            } finally {
              for (Tuples t: stackedArgs) t.close();
            }
          }
        }),
        new NVPair(ConstraintIs.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            ConstraintIs constraint = (ConstraintIs)constraintExpr;
            return TuplesOperations.assign((Variable)context.localize(constraint.getVariable()),
                                           ((LocalNode)context.localize(constraint.getValueNode())).getValue());
          }
        }),
        new NVPair(ConstraintImpl.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            ConstraintElement constraintElem =
              ((ConstraintImpl) constraintExpr).getModel();
            assert constraintElem != null;
            if (constraintElem.equals(Variable.FROM)) {
              return ConstraintOperations.resolveModelExpression(context, modelExpr, (Constraint)constraintExpr);
            } else if (constraintElem instanceof URIReference) {
              return ConstraintOperations.resolveModelExpression(context, new ModelResource(((URIReference)constraintElem).getURI()), (Constraint)constraintExpr);
            } else if (constraintElem instanceof LocalNode) {
              return context.resolve(null, (ConstraintImpl)constraintExpr);
            } else if (constraintElem instanceof Variable) {
              return context.resolve(null, (ConstraintImpl)constraintExpr);
            }
            else {
              throw new QueryException("Specified model not a URIReference/Variable: " + constraintElem +" is a " + constraintElem.getClass().getName() );
            }
          }
        }),
        new NVPair(WalkConstraint.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            return WalkFunction.walk(context, (WalkConstraint)constraintExpr, modelExpr, context.getResolverSession());
          }
        }),
        new NVPair(SingleTransitiveConstraint.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            SingleTransitiveConstraint constraint = (SingleTransitiveConstraint)constraintExpr;
            if (constraint.isAnchored()) {
              return DirectTransitiveFunction.infer(context, constraint, modelExpr, context.getResolverSession());
            } else {
              return ExhaustiveTransitiveFunction.infer(context, constraint, modelExpr, context.getResolverSession());
            }
          }
        }),
        new NVPair(TransitiveConstraint.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            return ExhaustiveTransitiveFunction.infer(context, (TransitiveConstraint)constraintExpr, modelExpr, context.getResolverSession());
          }
        }),
        new NVPair(ConstraintFilter.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            Tuples unfiltered = ConstraintOperations.resolveConstraintExpression(context, modelExpr, ((ConstraintFilter)constraintExpr).getUnfilteredConstraint());
            try {
              return TuplesOperations.filter(unfiltered, ((ConstraintFilter)constraintExpr).getFilter(), context);
            } finally {
              unfiltered.close();
            }
          }
        }),
        new NVPair(ConstraintIn.class, new ConstraintResolutionHandler() {
          public Tuples resolve(QueryEvaluationContext context, ModelExpression modelExpr, ConstraintExpression constraintExpr) throws Exception {
            ConstraintIn constraint = (ConstraintIn)constraintExpr;
            ModelExpression graph;
            if (constraint.getGraph() instanceof URIReferenceImpl) {
              graph = new ModelResource(((URIReferenceImpl)constraint.getGraph()).getURI());
            } else {
              assert constraint.getGraph() instanceof Variable;
              graph = new ModelVariable((Variable)constraint.getGraph());
            }
            return ConstraintOperations.resolveConstraintExpression(context, graph, constraint.getConstraintParam());
          }
        }),
      });
  }

  @SuppressWarnings("unchecked")
  static void initializeConstraintBindingHandlers() {
    ConstraintOperations.addConstraintBindingHandlers(new NVPair[]
      {
        new NVPair(ConstraintTrue.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return constraintExpr;
          }
        }),
        new NVPair(ConstraintFalse.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return constraintExpr;
          }
        }),
        new NVPair(ConstraintImpl.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return ConstraintOperations.replace(bindings, (Constraint)constraintExpr);
          }
        }),
        new NVPair(ConstraintIs.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return ConstraintOperations.replace(bindings, (Constraint)constraintExpr);
          }
        }),
        new NVPair(SingleTransitiveConstraint.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return new SingleTransitiveConstraint(ConstraintOperations.replace(bindings, (Constraint)constraintExpr));
          }
        }),
        new NVPair(TransitiveConstraint.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            TransitiveConstraint tc = (TransitiveConstraint)constraintExpr;
            return new TransitiveConstraint(ConstraintOperations.replace(bindings, tc.getAnchoredConstraint()),
                                            ConstraintOperations.replace(bindings, tc.getUnanchoredConstraint()));
          }
        }),
        new NVPair(WalkConstraint.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            WalkConstraint wc = (WalkConstraint)constraintExpr;
            return new WalkConstraint(ConstraintOperations.replace(bindings, wc.getAnchoredConstraint()),
                                      ConstraintOperations.replace(bindings, wc.getUnanchoredConstraint()));
          }
        }),
        new NVPair(ConstraintFilter.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return new ConstraintFilter(ConstraintOperations.replace(bindings, (Constraint)constraintExpr), ((ConstraintFilter)constraintExpr).getFilter());
          }
        }),
        new NVPair(ConstraintConjunction.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return new ConstraintConjunction(ConstraintOperations.replaceOperationArgs(bindings, (ConstraintOperation)constraintExpr));
          }
        }),
        new NVPair(ConstraintDisjunction.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            return new ConstraintDisjunction(ConstraintOperations.replaceOperationArgs(bindings, (ConstraintOperation)constraintExpr));
          }
        }),
        new NVPair(ConstraintDifference.class, new ConstraintBindingHandler() {
          public ConstraintExpression bindVariables(Map bindings, ConstraintExpression constraintExpr) throws Exception {
            List args = ConstraintOperations.replaceOperationArgs(bindings, (ConstraintOperation)constraintExpr);
            return new ConstraintDifference((ConstraintExpression)args.get(0), (ConstraintExpression)args.get(1));
          }
        }),
      });
  }

  @SuppressWarnings("unchecked")
  static void initializeConstraintModelRewrites() {
    ConstraintOperations.addConstraintModelRewrites(new NVPair[]
      {
        new NVPair(ConstraintImpl.class, new ConstraintModelRewrite() {
          public Constraint rewrite(ConstraintElement newModel, Constraint constraint) throws Exception {
            return new ConstraintImpl(constraint.getElement(0), constraint.getElement(1), constraint.getElement(2), newModel);
          }
        }),
      });
  }

  @SuppressWarnings("unchecked")
  static void initializeConstraintLocalizations() {
    ConstraintOperations.addConstraintLocalizations(new NVPair[]
      {
        new NVPair(ConstraintImpl.class, new ConstraintLocalization() {
          public Constraint localize(QueryEvaluationContext context, Constraint constraint) throws Exception {
            return new ConstraintImpl(context.localize(constraint.getElement(0)),
                context.localize(constraint.getElement(1)),
                context.localize(constraint.getElement(2)),
                context.localize(constraint.getElement(3)));
          }
        }),
      });
  }
}
