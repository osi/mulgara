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

// Jena
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.rdf.model.impl.*;
import com.hp.hpl.jena.rdf.model.*;
import org.kowari.server.*;

/**
 * An implementation of {@link com.hp.hpl.jena.rdf.model.ModelMaker} that
 * extends {@link com.hp.hpl.jena.rdf.model.impl.ModelMakerImpl}.
 *
 * @created 2004-02-20
 *
 * @author Andrew Newman
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/07 09:37:07 $ by $Author: newmana $
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
public class ModelMulgaraMaker extends ModelMakerImpl implements ModelMaker {

  /**
   * A constructor to create model on top of the Kowari store.
   *
   * @param gkm GraphKowariMaker the graph Kowari maker to use.
   */
  public ModelMulgaraMaker(GraphMulgaraMaker gkm) {
    super(gkm);
  }

  public Model makeModel(Graph graphKowari) {
    return new ModelMulgara((GraphMulgara) graphKowari);
  }
}
