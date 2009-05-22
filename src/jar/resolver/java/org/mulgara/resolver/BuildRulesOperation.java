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
 */

package org.mulgara.resolver;

// Java 2 standard packages
import java.net.URI;

import org.apache.log4j.Logger;
import org.mulgara.query.GraphExpression;
import org.mulgara.resolver.spi.DatabaseMetadata;
import org.mulgara.resolver.spi.SystemResolver;
import org.mulgara.rules.RuleLoader;
import org.mulgara.rules.RuleLoaderFactory;
import org.mulgara.rules.Rules;
import org.mulgara.rules.RulesRef;
import org.mulgara.rules.RulesRefImpl;

/**
 * Represents an operation to build a Rule structure for later execution
 *
 * @created Mar 25, 2008
 * @author Paul Gearon
 * @copyright &copy; 2007 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
class BuildRulesOperation implements Operation {

  @SuppressWarnings("unused")
  /** Logger. */
  private static final Logger logger = Logger.getLogger(BuildRulesOperation.class.getName());

  /** The graph containing the rules to run */
  private URI ruleGraph = null;

  /** The graph containing the intrinsic data */
  private GraphExpression baseGraph = null;

  /** The graph to contain the generated extrinsic data */
  private URI destGraph = null;

  /** The name of the class that loads rules */
  private String ruleLoaderClassName = null;

  /** The rules structure that can be shipped over RMI */
  private RulesRefImpl result;


  /**
   * Create an configure this operation.
   * @param ruleLoaderClassName The name of the class that can load the configured rules.
   * @param ruleGraph The graph containing the rules to read.
   * @param baseGraph The graph the rules will be run on.
   * @param destGraph The graph the rules will insert generated statements into.
   */
  BuildRulesOperation(String ruleLoaderClassName, URI ruleGraph, GraphExpression baseGraph, URI destGraph) {
    this.ruleLoaderClassName = ruleLoaderClassName;
    this.ruleGraph = ruleGraph;
    this.baseGraph = baseGraph;
    this.destGraph = destGraph;
  }

  /**
   * @see org.mulgara.resolver.Operation#execute(OperationContext, SystemResolver, DatabaseMetadata)
   */
  public void execute(OperationContext       operationContext,
                      SystemResolver         systemResolver,
                      DatabaseMetadata       metadata) throws Exception {
    // Set up the rule parser
    RuleLoader ruleLoader = RuleLoaderFactory.newRuleLoader(ruleLoaderClassName, ruleGraph, baseGraph, destGraph);
    if (ruleLoader == null) throw new org.mulgara.rules.InitializerException("No rule loader available");
  
    // read in the rules
    Rules rules =  ruleLoader.readRules(operationContext);
    result = new RulesRefImpl(rules);
  }

  /**
   * @see org.mulgara.resolver.Operation#isWriteOperation()
   */
  public boolean isWriteOperation() {
    return false;
  }

  /**
   * Obtains the results of this operation, after the transactional work is completed.
   * @return A reference to a set of rules.
   */  
  RulesRef getResult() {
    return result;
  }

}
