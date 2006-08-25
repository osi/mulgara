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

package org.mulgara.krule;

// Java 2 standard packages
import java.io.Serializable;
import java.net.*;
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;

// Locally written packages
import org.mulgara.query.*;
import org.mulgara.server.Session;

/**
 * Represents a single executable rule.
 *
 * @created 2005-5-16
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.2 $
 * @modified $Date: 2005/06/30 01:12:28 $
 * @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="mailto:pgearon@users.sorceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Rule implements Serializable {

  static final long serialVersionUID = -3762458306756060166L;
  
  /** Logger.  */
  private static Logger logger = Logger.getLogger(Rule.class.getName());

  /** The name of this rule. */
  private String name;

  /** The rules to be triggered when this rule generates statements.*/
  private Set triggerSet;

  /** The query for this rule. */
  private Query query;

  /** The model containing the base data. */
  private URI baseModel;
  
  /** The model containing the inferred data. */
  private URI targetModel;

  /** The most recent size of the data matching this rule. */
  private long lastCount;
  
  /** The structure containing this rule */
  private RuleStructure ruleStruct;

  // TODO: Change this to a map of constraints to longs

  /**
   * Principle constructor.
   *
   * @param name The name of the rule.
   */
  public Rule(String name) {
    triggerSet = new HashSet();
    this.name = name;
  }


  /**
   * Gets the name of the rule.
   *
   * @return The rule name.
   */
  public String getName() {
    return name;
  }


  /**
   * Adds a target for triggering.
   *
   * @param target The rule to be triggered when this rule is executed.
   */
  public void addTriggerTarget(Rule target) {
    triggerSet.add(target);
  }


  /**
   * Set the base model for the rule.
   *
   * @param base The URI of the base data to apply rules to.
   */
  public void setBaseModel(URI base) {
    baseModel = base;
  }


  /**
   * Set the target model for the rule.
   *
   * @param target The URI of the model to insert inferences into.
   */
  public void setTargetModel(URI target) {
    targetModel = target;
  }


  /**
   * Sets the query for this rule.
   *
   * @param queryStruct The query which retrieves data for this rule.
   */
  public void setQueryStruct(QueryStruct queryStruct) {
    this.query = queryStruct.extractQuery();
  }


  /**
   * Retrieves the query from this rule.
   *
   * @return The query which retrieves data for this rule.
   */
  public Query getQuery() {
    return query;
  }


  /**
   * Retrieves the list of subordinate rules.
   *
   * @return an immutable set of the subordinate rules.
   */
  public Set getTriggerTargets() {
    return Collections.unmodifiableSet(triggerSet);
  }

  
  /**
   * Sets the rule system structure containing this rule.
   * 
   * @param ruleStruct The structure for this rule.
   */
  public void setRuleStruct(RuleStructure ruleStruct) {
  	this.ruleStruct = ruleStruct;
  }


  /**
   * Runs this rule.
   * TODO: count the size of each individual constraint
   * 
   * @param session The session to execute the rule against.
   */
  public void execute(Session session) throws QueryException, TuplesException {
    // see if this rule needs to be run
  	Answer answer = session.query(query);
  	// compare the size of the result data  	
  	long newCount = answer.getRowCount(); 
  	if (newCount == lastCount) {
  	  logger.debug("Rule <" + name + "> is up to date.");
  	  // this rule does not need to be run
  	  return;
  	}
  	logger.debug("Rule <" + name + "> has increased by " + (newCount - lastCount) + " entries");
    logger.debug("Inserting results of: " + query);
    if (answer instanceof AnswerImpl) {
      AnswerImpl a = (AnswerImpl)answer;
      String list = "[ ";
      Variable[] v = a.getVariables();
      for (int i = 0; i < v.length; i++) {
        list += v[i] + " ";
      }
      list += "]";
      logger.debug("query has " + a.getNumberOfVariables() + " variables: " + list);
    }
  	// insert the resulting data
  	session.insert(targetModel, query);
  	// update the count
  	lastCount = newCount;
  	logger.debug("Insertion complete, triggering rules for scheduling.");
  	// trigger subsequent rules
  	scheduleTriggeredRules();
  }

  
  /**
   * Schedule subsequent rules.
   */
  private void scheduleTriggeredRules() {
  	Iterator it = triggerSet.iterator();
  	while (it.hasNext()) {
  	  Rule rule = (Rule)it.next();
  	  ruleStruct.schedule(rule);
  	}
  }

}
