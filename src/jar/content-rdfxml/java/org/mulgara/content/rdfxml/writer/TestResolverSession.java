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

package org.mulgara.content.rdfxml.writer;

// Java 2 standard packages
import java.util.HashMap;
import java.util.Map;
import java.net.URI;

// Third party packages
import org.jrdf.graph.Node;

// Local packages
import org.mulgara.query.rdf.BlankNodeImpl;
import org.mulgara.resolver.spi.GlobalizeException;
import org.mulgara.resolver.spi.LocalizeException;
import org.mulgara.resolver.spi.ResolverSession;
import org.mulgara.store.stringpool.SPObject;
import org.mulgara.store.stringpool.SPObjectFactory;
import org.mulgara.store.stringpool.StringPoolException;
import org.mulgara.store.tuples.Tuples;

/**
 * A minimal implementation of {@link ResolverSession}.
 *
 * This isn't capable of persistence, and is only appropriate for use in
 * unit tests.
 *
 * @created 2004-09-17
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:03 $
 * @maintenanceAuthor $Author: newmana $
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana Technology,
 *   Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */

public class TestResolverSession implements ResolverSession
{
  /**
   * Our pretend node pool, a counter used to generate new local node values.
   */
  private long top = 0;

  /**
   * ID's used to reference BlankNodes (top counter for blank nodes)
   * Will cause problems if more nodes (non-blank) than the initial value
   * are inserted.
   */
  private long bNode = 1000000;

  /**
   * Our pretend string pool, a map from global JRDF nodes to local
   * {@link Long}s.
   */
  private final Map map = new HashMap();

  /**
   * Mirror to the string pool map that allows globalization.
   */
  private final Map mirrorMap = new HashMap();

  //
  // Methods implementing ResolverSession
  //

  public Node globalize(long node) throws GlobalizeException
  {

    Node gNode = (Node) mirrorMap.get(new Long(node));
    return gNode;
  }

  public long lookup(Node node) throws LocalizeException
  {
    Object object = map.get(node);
    if (object == null) {
      throw new LocalizeException(node, "No such node");
    }
    else {
      return ((Long) object).longValue();
    }
  }

  public long lookupPersistent(Node node) throws LocalizeException
  {
    throw new LocalizeException(node, "Not implemented");
  }

  public long localize(Node node) throws LocalizeException
  {
    Object object = map.get(node);
    if (object == null) {
      top++;

      Long id = new Long(top);

      if (node instanceof BlankNodeImpl) {

        bNode++;
        id = new Long(bNode);
        ((BlankNodeImpl) node).setNodeId(bNode);
      }

      map.put(node, id);
      mirrorMap.put(id, node);

      return id.longValue();
    }
    else {

      return ((Long) object).longValue();
    }
  }

  public long localizePersistent(Node node) throws LocalizeException
  {
    throw new LocalizeException(node, "Not implemented");
  }

  public Tuples findStringPoolRange(
      SPObject lowValue, boolean inclLowValue,
      SPObject highValue, boolean inclHighValue
  ) throws StringPoolException {
    throw new UnsupportedOperationException("Not Implemented on test class");
  }

  public Tuples findStringPoolType(
      SPObject.TypeCategory typeCategory, URI typeURI
  ) throws StringPoolException {
    throw new UnsupportedOperationException("Not Implemented on test class");
  }

  public SPObject findStringPoolObject(long gNode) throws StringPoolException {
    throw new UnsupportedOperationException("Not Implemented on test class");
  }

  /**
   * Retrieve the SPObject factory from the stringpool to allow for the creation
   * of new SPObjects.
   *
   * @return The factory to allow for creation of SPObjects
   */
  public SPObjectFactory getSPObjectFactory() {

    throw new UnsupportedOperationException("Not Implemented on test class");
  }
}
