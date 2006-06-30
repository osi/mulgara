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

package org.mulgara.client.drools;

import org.drools.*;
import org.mulgara.itql.ItqlInterpreterBean;
import org.mulgara.itql.ItqlInterpreterException;

/**
 * Startup wrapper to infer rules on a TKS session using the Drools framework.
 *
 * @created 2004-07-12
 *
 * @author Paul Gearon
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:33 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2004
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Infer {

  public static void main(String[] args) {
    // confirm that there are 2 arguments
    if (args.length != 2) {
      System.err.println("usage: java org.tks.rules.Infer base_model inference_model");
    }

    RuleBase ruleBase;
    try {
      // Build the rule set
      ruleBase = org.drools.io.RuleBaseBuilder.buildFromUrl(Infer.class.getResource("rdfs.drl"));
    } catch (Exception e) {
      System.err.println("Error: Unable to load rdfs.drl: " + e.getMessage());
      e.printStackTrace();
      return;
    }

    // instantiate the working memory
    WorkingMemory workingMemory = ruleBase.newWorkingMemory();

    // get an iTQL session
    ItqlInterpreterBean itql = new ItqlInterpreterBean();

    // start the system with a bootstrap object
    Bootstrap bootstrap;
    try {
      bootstrap = new Bootstrap(itql, args[1], args[0]);
    } catch (ItqlInterpreterException e) {
      System.err.println("Unable to initialize bootstrap loader: " + e.getMessage());
      return;
    }

    try {
      // load the bootstrap object into memory
      workingMemory.assertObject(bootstrap);

      System.out.println("Commencing inferencing...");

      // run the rules
      workingMemory.fireAllRules();
    } catch (FactException e) {
      System.err.println("Error running rules: " + e.getMessage());
      return;
    }

    System.out.println("Inferencing completed.");
  }

}

