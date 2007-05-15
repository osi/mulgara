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
 *
 */

package org.mulgara.resolver.distributed.remote;

import java.rmi.RemoteException;
import java.util.Set;

import org.jrdf.graph.Triple;
import org.mulgara.query.TuplesException;
import org.mulgara.resolver.spi.GlobalizeException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.resolver.spi.Statements;

/**
 * Creates a Set of statements that be be shipped across a network.
 *
 * @created 2007-04-23
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class StatementSetFactory {
  
  /** The size for transitioning between a serializable set and a remote set. */
  static final long WATER_MARK = 2048L;

  public static Set newStatementSet(Statements statements, ResolverSession session) throws TuplesException, GlobalizeException {
    // make sure the WATER_MARK refers to a set that is indexable by integer
    assert (long)(int)WATER_MARK == WATER_MARK;
    if (statements.getRowUpperBound() < WATER_MARK) return new ShortGlobalStatementSet(statements, session);
    try {
      RemotePager<Triple> pager = new RemotePagerImpl<Triple>(Triple.class, new TripleSetAdaptor(statements, session));
      return new SetProxy<Triple>(pager);
    } catch (RemoteException re) {
      throw new TuplesException("Error accessing remote data", re);
    }
  }
}
