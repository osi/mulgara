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

package org.mulgara.rules;

import java.net.URI;
import java.rmi.RemoteException;

/**
 * A dummy implementation of the rule loader.
 *
 * @created 2005-7-1
 * @author <a href="mailto:gearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.1 $
 * @modified $Date: 2005/07/01 23:21:33 $
 * @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class DummyRuleLoader implements RuleLoader {

  /**
   * Reads the ruleModel in the database and constructs the rules from it.
   *
   * @return Nothing.
   */
  public Rules readRules(Object session, URI systemModel) throws InitializerException {
    throw new InitializerException("No rule loader available.");
  }

  /**
   * Placeholder implementation.
   *
   * @param ruleModel Ignored.
   * @param baseModel Ignored.
   * @param destModel Ignored.
   */
  public DummyRuleLoader(URI ruleModel, URI baseModel, URI destModel) {
    // No-op
  }

  /**
   * Factory method.
   *
   * @param ruleModel The name of the model with the rules to run.
   * @param baseModel The name of the model with the base data.
   * @param destModel The name of the model which will receive the entailed data.
   * @return A new DummyRuleLoader instance.
   */
  public static RuleLoader newInstance(URI ruleModel, URI baseModel, URI destModel) {
    return new DummyRuleLoader(ruleModel, baseModel, destModel);
  }

}
