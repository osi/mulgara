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

package org.mulgara.krule.rlog.ast;

import org.mulgara.krule.rlog.rdf.RDFNode;
import org.mulgara.krule.rlog.rdf.Var;

/**
 * Represents an RLog variable.
 * 
 * @created May 16, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class Variable extends Node implements TypeLabel, BPredicateLabel, PredicateParam {

  public final String name;

  public Variable(String name) {
    this.name = name;
  }

  public void accept(TreeWalker walker) {
    walker.visit(this);
  }

  public void print(int indent) {
    System.out.println(sp(indent) + "Variable (" + name + ")");
  }

  public String toString() {
    return "var(" + name + ")";
  }

  /** {@inheritDoc} */
  public RDFNode getRDFNode() {
    return new Var(name);
  }

  /** {@inheritDoc} */
  public boolean equals(Object o) {
    if (!(o instanceof Variable)) return false;
    return name.equals(((Variable)o).name);
  }

  /** {@inheritDoc} */
  public int hashCode() {
    return name.hashCode();
  }

}

