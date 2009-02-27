/*
 * Copyright 2008 Fedora Commons, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mulgara.krule.rlog;

import java.io.*;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.mulgara.krule.rlog.ast.*;
import org.mulgara.krule.rlog.ast.output.KruleWriter;
import org.mulgara.krule.rlog.parser.TypeException;
import org.mulgara.krule.rlog.parser.URIParseException;
import org.mulgara.krule.rlog.rdf.MulgaraGraphs;
import org.mulgara.krule.rlog.rdf.URIReference;
import org.mulgara.krule.rlog.rdf.Var;

/**
 * This class is used to interpret a string into an AST.  The AST is then transformed into a krule encoding in XML.
 * 
 * @created Feb 22, 2009
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class Rlog implements Interpreter {

  /** The label used on all generated rules. */
  private static String ruleLabel = "rule";

  /** All the parsed statements to be processed. */
  private List<Statement> statements;

  /** All the parsed Axioms. */
  private List<Axiom> axioms;

  /** All the parsed Rules. */
  private List<Rule> rules;

  /**
   * Parse input data into an AST and process.
   * @param args May contain a filename with the code to be parsed.  All other parameters ignored.
   * @throws IOException If there was an error reading from the specified file, or standard in.
   * @throws beaver.Parser.Exception If there was a parsing exception.
   */
  public static void main(String[] args) throws IOException, ParseException {
    // get the input
    Reader intReader;
    if (args.length > 0) {
      File inFile = new File(args[0]);
      if (!inFile.exists() || !inFile.canRead()) {
        System.err.println("Unable to open file: " + inFile);
        return;
      }
      // borrow the filename for labelling rules
      intReader = new FileReader(inFile);
      String filename = inFile.getName();
      ruleLabel = filename.substring(0, filename.lastIndexOf('.'));
    } else {
      intReader = new InputStreamReader(System.in);
    }
    Reader input = new BufferedReader(intReader);

    try {
      // parse the input
      Rlog rlog = new Rlog(input);
      // emit to output
      new KruleWriter(rlog).emit(System.out);
    } catch (TypeException te) {
      System.err.println(te.getMessage());
      return;
    } catch (URIParseException e) {
      System.err.println(e.getMessage());
      return;
    }

  }

  /**
   * Sets the label to use for each rule. The rule names are made up of this label
   * plus an incremening number.
   * @param label The text label to use on rules.
   */
  public static void setRuleLabel(String label) {
    ruleLabel = label;
  }

  /**
   * Create an rlog interpreter for building an AST from a stream Reader object.
   * @param input The stream Reader.
   * @throws IOException There was an IO Exception on the input.
   * @throws beaver.Parser.Exception There was a parser exception in the input data.
   * @throws URIParseException If the rules contain illegal URIs.
   */
  public Rlog(Reader input) throws IOException, ParseException, TypeException, URIParseException {
    // parse the rlog into statements
    RlogParser parser = new RlogParser(input);

    statements = parser.statements();

    // separate out the rules from the axioms
    int ruleCount = 0;
    rules = new ArrayList<Rule>();
    axioms = new ArrayList<Axiom>();
    for (Statement s: statements) {
      if (s instanceof Axiom) axioms.add((Axiom)s);
      else if (s instanceof Rule) {
        rules.add((Rule)s);
        ((Rule)s).setName(ruleLabel + ++ruleCount);
      } else throw new IllegalStateException("Unknown statement type found: " + s.getClass().getName());
    }

    // calculate dependencies between the rules
    calculateRuleDependencies();
  }

  /**
   * Find all the variables in every rule.
   * @return A complete collection of all the variables that were parsed.
   */
  public Collection<Var> getVariables() {
    LinkedHashSet<Var> vars = new LinkedHashSet<Var>();
    for (Rule r: rules) vars.addAll(r.getVariables());
    return vars;
  }

  /**
   * Gets all the URIs referenced in the rules.
   * @return All URIs in order of appearance within axioms, then rules.
   * @throws URIParseException The referenced URIs had bad syntax.
   */
  public Set<URIReference> getReferences() throws URIParseException {
    Set<URIReference> refs = new LinkedHashSet<URIReference>();
    for (Axiom a: axioms) refs.addAll(a.getReferences());
    for (Rule r: rules) refs.addAll(r.getReferences());
    refs.addAll(MulgaraGraphs.getSpecialUriRefs());
    return refs;
  }

  /**
   * Get all the axioms appearing in the rule set.
   * @return A list of axioms.
   */
  public List<Axiom> getAxioms() {
    return Collections.unmodifiableList(axioms);
  }

  /**
   * Get all the rules appearing in the rule set.
   * @return A list of rules.
   */
  public List<Rule> getRules() {
    return Collections.unmodifiableList(rules);
  }

  /**
   * Determine which rules are dependent on the result of which other rules,
   * and set the rule objects accordingly.
   * @throws URIParseException If the rules contain illegal URIs.
   */
  private void calculateRuleDependencies() throws TypeException, URIParseException {
    for (Rule trigger: rules) {
      for (Rule potentialTarget: rules) {
        if (potentialTarget.triggeredBy(trigger)) potentialTarget.addTrigger(trigger);
      }
    }
  }

}
