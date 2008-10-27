/*
 * Copyright 2008 The Topaz Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * Contributions:
 */

package org.mulgara.resolver.lucene;

import org.apache.log4j.Logger;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.jrdf.graph.URIReference;

import org.mulgara.query.ConstraintExpression;
import org.mulgara.query.ConstraintElement;
import org.mulgara.query.ConstraintImpl;
import org.mulgara.query.ConstraintConjunction;
import org.mulgara.query.ConstraintDisjunction;
import org.mulgara.query.QueryException;
import org.mulgara.query.rdf.URIReferenceImpl;
import org.mulgara.resolver.spi.MutableLocalQuery;
import org.mulgara.resolver.spi.SymbolicTransformation;
import org.mulgara.resolver.spi.SymbolicTransformationContext;
import org.mulgara.resolver.spi.SymbolicTransformationException;

/**
 * A query transformer for lucene. This accept constraints like in the following example:
 * <pre>
 *   select $x $score from ... where
 *     $x <mulgara:search> $s in <lucene> and
 *     $s <foo> 'story' in <lucene> and
 *     $s <mulgara:score> $score in <lucene>
 * </pre>
 *
 * @created 2008-09-28
 * @author Ronald Tschalär
 * @licence Apache License v2.0
 */
public class LuceneTransformer implements SymbolicTransformation {
  private static final Logger logger = Logger.getLogger(LuceneTransformer.class);

  private final URI modelTypeURI;
  private final URIReference searchPred;
  private final URIReference scorePred;

  /**
   * Create a new tranformer to rewrite lucene queries.
   *
   * @param modelTypeURI  the uri of the lucene model type
   * @param searchPredURI the uri of the search predicate
   * @param scorePredURI  the uri of the score predicate
   */
  public LuceneTransformer(URI modelTypeURI, URI searchPredUri, URI scorePredUri) {
    this.modelTypeURI = modelTypeURI;
    searchPred = new URIReferenceImpl(searchPredUri);
    scorePred = new URIReferenceImpl(scorePredUri);
  }

  public void transform(SymbolicTransformationContext context, MutableLocalQuery query)
      throws SymbolicTransformationException {
    ConstraintExpression expr = query.getConstraintExpression();
    ConstraintExpression trans = transformExpr(context, expr);

    if (expr != trans) {
      query.setConstraintExpression(trans);
    }
  }

  private ConstraintExpression transformExpr(SymbolicTransformationContext context, ConstraintExpression expr) throws SymbolicTransformationException {
    if (expr instanceof ConstraintImpl) {
      return transformConstr(context, (ConstraintImpl)expr);
    }
    if (expr instanceof ConstraintConjunction) {
      return transformConj(context, (ConstraintConjunction)expr);
    }
    if (expr instanceof ConstraintDisjunction) {
      return transformDisj(context, (ConstraintDisjunction)expr);
    }

    return expr;
  }

  private ConstraintExpression transformConstr(SymbolicTransformationContext context, ConstraintImpl c) throws SymbolicTransformationException {
    try {
      ConstraintElement ce = c.getModel();
      if (ce instanceof URIReference) {
        URI constraintModelType = context.mapToModelTypeURI(((URIReference)ce).getURI());
        if (constraintModelType != null && constraintModelType.equals(modelTypeURI)) {
          return new LuceneConstraint(c, searchPred, scorePred);
        }
      }
      return c;
    } catch (QueryException eq) {
      throw new SymbolicTransformationException("Failed to map model to model-type", eq);
    }
  }

  public ConstraintExpression transformConj(SymbolicTransformationContext context, ConstraintConjunction cc) throws SymbolicTransformationException {
    List<ConstraintExpression> retainedArgs = new ArrayList<ConstraintExpression>();
    Map<ConstraintElement, List<LuceneConstraint>> luceneArgs =
                                    new HashMap<ConstraintElement, List<LuceneConstraint>>();

    boolean transformed = false;

    for (ConstraintExpression arg : cc.getElements()) {
      ConstraintExpression trans = transformExpr(context, arg);
      if (trans != arg) {
        transformed = true;
      }

      if (trans instanceof LuceneConstraint) {
        LuceneConstraint lc = (LuceneConstraint)trans;
        List<LuceneConstraint> cumulative = luceneArgs.get(lc.getBindingVar());
        if (cumulative == null) {
          cumulative = new ArrayList<LuceneConstraint>();
          cumulative.add(lc);
          luceneArgs.put(lc.getBindingVar(), cumulative);
        } else if (cumulative.size() > 1 ||
                   cumulative.get(0).getSubject() == null &&
                   cumulative.get(0).getPredicate() != null &&
                   lc.getSubject() == null && lc.getPredicate() != null) {
          // backwards compat hack for multiple simple queries
          cumulative.add(lc);
        } else {
          cumulative.iterator().next().conjoinWith(lc);
          transformed = true;
        }
      } else {
        retainedArgs.add(trans);
      }
    }

    if (transformed) {
      for (List<LuceneConstraint> c : luceneArgs.values()) {
        for (LuceneConstraint lc : c) {
          lc.validate();
          retainedArgs.add(lc);
        }
      }

      return new ConstraintConjunction(retainedArgs);
    } else {
      return cc;
    }
  }

  private ConstraintExpression transformDisj(SymbolicTransformationContext context, ConstraintDisjunction cd) throws SymbolicTransformationException {
    List<ConstraintExpression> transArgs = new ArrayList<ConstraintExpression>();
    boolean transformed = false;
    for (ConstraintExpression ce : cd.getElements()) {
      ConstraintExpression trans = transformExpr(context, ce);
      if (trans != ce) {
        transformed = true;
      }
      transArgs.add(trans);
    }

    return transformed ? new ConstraintDisjunction(transArgs) : cd;
  }
}
