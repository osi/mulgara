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
import java.net.URI;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import org.mulgara.query.QueryException;
import org.mulgara.query.TuplesException;
import org.mulgara.rules.InitializerException;
import org.mulgara.rules.Rules;
import org.mulgara.rules.RulesException;
import org.mulgara.server.Session;

/**
 * Represents a structure of rules.
 *
 * @created 2005-5-16
 * @author <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @version $Revision: 1.3 $
 * @modified $Date: 2005/07/03 12:53:41 $
 * @maintenanceAuthor $Author: pgearon $
 * @copyright &copy; 2005 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class RuleStructure implements Rules, Serializable {

  static final long serialVersionUID = 452741614702661812L;

  /** Logger.  */
  private static Logger logger = Logger.getLogger(RuleStructure.class.getName());

  /** The rules in the framework. */
  private Set rules;

  /** Map of rule names to the rule. */
  private Map ruleMap;

  /** The model containing the base data. */
  private URI baseModel;

  /** The terget model to contain the entailments. */
  private URI targetModel;

  /** The current list of rules that have to be run. */
  private LinkedHashSet runQueue;

  /** The set of axioms pertinent to these rules. */
  private Set axioms;


  /**
   * Principle constructor.
   */
  public RuleStructure() {
    rules = new HashSet();
    ruleMap = new HashMap();
    runQueue = new LinkedHashSet();
    axioms = null;
  }


  /**
   * Adds a new rule to the rule set.
   *
   * @param rule The rule to be added.
   */
  public void add(Rule rule) {
    rules.add(rule);
    ruleMap.put(rule.getName(), rule);
    rule.setRuleStruct(this);
  }


  /**
   * Links a pair of rules for triggering.
   *
   * @param src The name of the trigger generating rule.
   * @param dest The name of the rule which is triggered.
   */
  public void setTrigger(String src, String dest) throws InitializerException {
    // get the rules
    Rule srcRule = (Rule)ruleMap.get(src);
    Rule destRule = (Rule)ruleMap.get(dest);
    // check that the rules exist
    if (srcRule == null || destRule == null) {
      throw new InitializerException("Nonexistent rule: " + srcRule == null ? src : dest);
    }

    // set the link
    srcRule.addTriggerTarget(destRule);
  }


  /**
   * Sets a set of axioms for these rules.
   *
   * @param axioms A {@link java.util.Set} of {@link org.jrdf.graph.Triple}s
   *   comprising axiomatic statements.
   */
  public void setAxioms(Set axioms) {
    this.axioms = axioms;
  }


  /**
   * Gets the number of axioms for these rules.
   *
   * @return The number of axiomatic statements.
   */
  public int getAxiomCount() {
    return axioms == null ? 0 : axioms.size();
  }


  /**
   * Returns the number of rules.
   *
   * @return The number of rules.
   */
  public int getRuleCount() {
    return rules.size();
  }


  /**
   * Returns an iterator for the rules.
   *
   * @return An iterator for the rules.
   */
  public Iterator getRuleIterator() {
    return rules.iterator();
  }


  /**
   * Debug method to view the contents of a rule structure.
   * 
   * @return A string representation of this structure.
   */
  public String toString() {
    String result = "Rules = {\n";
    Iterator i = rules.iterator();
    while (i.hasNext()) {
      Rule r = (Rule)i.next();
      result += r.getName() + "\n";
    }
    result += "}";
    return result;
  }


  /**
   * Set the base model for the rules.
   *
   * @param base The URI of the base data to apply rules to.
   */
  public void setBaseModel(URI base) {
    Iterator it = rules.iterator();
    while (it.hasNext()) {
      Rule rule = (Rule)it.next();
      rule.setBaseModel(base);
    }
  }


  /**
   * Set the target model for the rules.
   *
   * @param target The URI of the target model to insert inferences into.
   */
  public void setTargetModel(URI target) {
    targetModel = target;
    Iterator it = rules.iterator();
    while (it.hasNext()) {
      Rule rule = (Rule)it.next();
      rule.setTargetModel(target);
    }
  }


  /**
   * Starts the rules engine.  This is a breadth first evaluation engine.
   * This means that any triggered rules are scheduled for evaluation,
   * and are not run immediately.
   *
   * @param params the session to use.
   */
  public void run(Object params) throws RulesException {
    logger.debug("Run called");
    if (!(params instanceof Session)) {
      throw new IllegalArgumentException("Rules must be run with a session");
    }
    Session session = (Session)params;
    // set up the run queue
    runQueue = new LinkedHashSet(rules);
    // fill the run queue
    runQueue.addAll(rules);
    Rule currentRule = null;
    try {
      // use a single transaction
      session.setAutoCommit(false);
      // start by inserting the axioms
      insertAxioms(session);
      // process the queue
      while (runQueue.size() > 0) {
        // get the first rule from the queue
        currentRule = popRunQueue();
        logger.debug("Executing rule: " + currentRule);
        // execute the rule
        currentRule.execute(session);
      }
    } catch (TuplesException te) {
      logger.error("Error getting data within rule: " + currentRule);
      try {
        session.rollback();
      } catch (QueryException e) {
        logger.error("Error during rollback: " + currentRule);
      }
      throw new RulesException("Error getting data within rule: " + currentRule, te);
    } catch (QueryException qe) {
      logger.error("Error executing rule: " + currentRule);
      try {
        session.rollback();
      } catch (QueryException e) {
        logger.error("Error during rollback: " + currentRule);
      }
      throw new RulesException("Error executing rule: " + currentRule, qe);
    } finally {
      try {
        // this will commit the current phase, or do nothing if we rolled back
        session.setAutoCommit(true);
      } catch (QueryException e) {
        logger.error("Unable to close transaction.", e);
        throw new RulesException("Unable to close transaction", e);
      }
    }
    logger.debug("All rules complete");
  }


  /**
   * Schedules a rule to be run.
   * 
   * @param rule The rule to schedule.
   */
  public void schedule(Rule rule) {
    logger.debug("Scheduling: " + rule.getName());
    // re-insertions do NOT affect the order in the queue
    runQueue.add(rule);
  }


  /**
   * Remove the head of the queue.
   *
   * @return The first item in the queue.
   */
  private Rule popRunQueue() {
    // get an iterator for the queue
    Iterator iterator = runQueue.iterator();
    // this queue must have data in it
    assert iterator.hasNext();
    Rule head = (Rule)iterator.next();
    iterator.remove();
    return head;
  }


  /**
   * Inserts all axioms into the output model in the current session.
   *
   * @param session The session to use for writing.
   */
  private void insertAxioms(Session session) throws QueryException {
    logger.debug("Inserting axioms");
    // check if axioms were provided
    if (axioms == null) {
      logger.debug("No axioms provided");
      return;
    }
    // insert the statements
    session.insert(targetModel, axioms);
  }

}
