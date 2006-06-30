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

package org.mulgara.resolver.spi;

import java.util.*;

// Third party packages
import org.apache.log4j.*;
import org.jrdf.graph.Node;

// Locally written packages
import org.mulgara.query.Answer;
import org.mulgara.query.TuplesException;
import org.mulgara.store.tuples.AbstractTuples;

/**
 * Wrapper around a globally valid {@link Answer} instance, converting into
 * a local {@link org.mulgara.store.tuples.Tuples}.
 *
 * @created 2003-10-28
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:50 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */

public class LocalizedTuples extends AbstractTuples {
  /**
   * Logger.
   */
  private static Logger logger =
      Logger.getLogger(LocalizedTuples.class.getName());

  /** The session to localize into. */
  private final ResolverSession session;

  /**
   * The wrapped {@link Answer} instance.
   * Treat as final except in clone.
   */
  protected Answer answer;


  /**
   * Wrap an {@link Answer} instance.
   *
   * @param globalAnswer  the global answer to wrap
   * @throws IllegalArgumentException  if <var>globalAnswer</var> is
   *                                   <code>null</code>
   */
  public LocalizedTuples(ResolverSession session, Answer globalAnswer)
  {
    if (session == null) {
      throw new IllegalArgumentException("Null \"session\" parameter");
    }
    if (globalAnswer == null) {
      throw new IllegalArgumentException("Null \"globalAnswer\" parameter");
    }

    this.session = session;
    answer = (Answer) globalAnswer.clone();
    setVariables(answer.getVariables());
  }


  public void beforeFirst() throws TuplesException {
    answer.beforeFirst();
  }


  public void beforeFirst(long[] prefix, int suffixTruncation)
      throws TuplesException
  {
    if (prefix.length == 0 && suffixTruncation == 0) {
      answer.beforeFirst();
    } else {
      throw new TuplesException(
          "LocalizedTuples.beforeFirst not implemented for prefix length " +
          prefix.length + " and suffix length " + suffixTruncation);
    }
  }


  public Object clone() {
    LocalizedTuples copy = (LocalizedTuples)super.clone();
    copy.answer = (Answer)answer.clone();
    return copy;
  }


  public void close() throws TuplesException {
    answer.close();
  }


  public long getColumnValue(int column) throws TuplesException {
    try {
      Object node = answer.getObject(column);
      assert node instanceof Node;

      return session.localize((Node)node);
    } catch (LocalizeException e) {
      throw new TuplesException("Couldn't localize column " + column, e);
    }
  }


  public long getRowCount() throws TuplesException {
    return answer.getRowCount();
  }

  public long getRowUpperBound() throws TuplesException
  {
    return answer.getRowUpperBound();
  }


  /**
   * We can't possibly know whether an {@link Answer} column might be
   * <code>null</code> without materializing it, so we have to assume it could
   * be.
   *
   * @return <code>true</code>
   */
  public boolean isColumnEverUnbound(int column) throws TuplesException {
    return true;
  }


  // We may not be able to trust the Answer received from a distributed query.
  public boolean hasNoDuplicates() {
    return false;
  }


  public List getOperands() {
    return new ArrayList(0);
  }


  public boolean next() throws TuplesException {
    return answer.next();
  }
}
