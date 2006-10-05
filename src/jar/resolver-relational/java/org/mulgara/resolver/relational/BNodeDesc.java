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
package org.mulgara.resolver.relational;

import java.util.Arrays;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.jrdf.graph.Node;
import org.jrdf.graph.URIReference;
import org.jrdf.graph.Literal;

import org.mulgara.query.TuplesException;
import org.mulgara.query.rdf.VariableNodeImpl;
import org.mulgara.resolver.relational.d2rq.ClassMapElem;
import org.mulgara.resolver.relational.d2rq.DatatypePropertyBridgeElem;
import org.mulgara.resolver.relational.d2rq.ObjectPropertyBridgeElem;


public class BNodeDesc extends VariableDesc {
  private static Logger logger = Logger.getLogger(RelationalResolver.class);

  private final Set tables;
  private final Set columns;
  private String bnodeClass;

  private Map columnIndices;

  public BNodeDesc(ClassMapElem cmap) {
    super(cmap);
    this.bnodeClass = cmap.klass;

    String[] split = cmap.bNodeIdColumns.split("\\s*,\\s*");
    tables =  new HashSet();
    columns = new HashSet();
    columnIndices = new HashMap();

    for (int i = 0; i < split.length; i++) {
      columns.add(split[i]);
      tables.add(RelationalResolver.parseTableFromColumn(split[i]));
    }
  }

  public void assignColumnIndex(String column, int index) {
    columnIndices.put(column, new Integer(index));
  }

  public Node getNode(ResultSet resultSet) throws SQLException, TuplesException {
    StringBuffer buff = new StringBuffer(bnodeClass);
    Iterator i = columns.iterator();
    while (i.hasNext()) {
      int index = ((Integer)columnIndices.get(i.next())).intValue();
      buff.append("|||");
      buff.append(resultSet.getString(index + 1));
    }

    return new VariableNodeImpl(buff.toString());
  }

  public Set getTables() {
    return tables;
  }

  public Set getColumns() {
    return columns;
  }

  public String restrict(String rdfValue) {
    throw new IllegalStateException("Cannot restrict blank-node");
  }
}
