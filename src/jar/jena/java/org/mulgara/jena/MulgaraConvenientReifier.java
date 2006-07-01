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


package org.mulgara.jena;

import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.shared.*;
import com.hp.hpl.jena.graph.impl.*;
import com.hp.hpl.jena.util.iterator.*;

import com.hp.hpl.jena.vocabulary.RDF;

/**
 * An implementation of {@link com.hp.hpl.jena.graph.Reifier} that uses a
 * MulgaraGraph for storage.
 *
 * @created 2004-09-24
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:17 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2004 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class MulgaraConvenientReifier extends MulgaraReifier {

  public MulgaraConvenientReifier(GraphMulgara newParent,
      GraphMulgara newReificationGraph) {
    parent = newParent;
    reificationGraph = newReificationGraph;
  }

  public ReificationStyle getStyle() {
    return ReificationStyle.Convenient;
  }

  public Graph getParentGraph() {
    return parent;
  }

  public boolean handledAdd(Triple t) {
    int s = Fragments.getFragmentSelector(t);
    if (s < 0)
      return false;
    else {
      reificationGraph.add(t);
      return true;
    }
  }

  public boolean handledRemove(Triple t) {
    int s = Fragments.getFragmentSelector(t);
    if (s < 0)
      return false;
    else {
      reificationGraph.delete(t);
      return true;
    }
  }

  public Graph getHiddenTriples() {
    return reificationGraph;
  }

  public Graph getReificationTriples() {
    return reificationGraph;
  }
}