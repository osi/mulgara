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

package org.mulgara.resolver.distributed;

import org.apache.log4j.Logger;  // Apache Log4J

import org.mulgara.query.Answer;
import org.mulgara.query.Constraint;
import org.mulgara.query.rdf.URIReferenceImpl;

import org.mulgara.resolver.spi.GlobalizeException;
import org.mulgara.resolver.spi.LocalizedTuples;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Resolution;

import org.mulgara.store.tuples.Annotation;
import org.mulgara.store.tuples.RowComparator;
import org.mulgara.store.tuples.Tuples;

/**
 * A {@link Resolution} which extends a LocalizedTuples, which in turn wraps an Answer.
 *
 * @created 2007-03-23
 * @author Paul Gearon
 * @version $Revision: $
 * @modified $Date: $ @maintenanceAuthor $Author: $
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
class AnswerResolution extends LocalizedTuples implements Resolution {

  /** Logger.  */
  private static final Logger logger = Logger.getLogger(AnswerResolution.class.getName());

  /** The constraint. */
  private final Constraint constraint;


  /**
   * Construct an AnswerResolution, passing most of the work off to the LocalizedTuples.
   * @param session The current session.
   * @param answer The answer to be wrapped by the parent class.
   * @param constraint the constraint.
   * @throws IllegalArgumentException if <var>constraint<var> is <code>null</code>
   */
  AnswerResolution(ResolverSession session, Answer answer, Constraint constraint) {
    super(session, answer);
    if (constraint == null) throw new IllegalArgumentException("Null constraint parameter");
    this.constraint = constraint;
  }


  /**
   * Get the constraint leading to this resolution.
   * @return The constraint for the resolution.
   */
  public Constraint getConstraint() {
    return constraint;
  }


  /**
   * {@inheritDoc}
   */
  public boolean isComplete() {
    return true;
  }

}
