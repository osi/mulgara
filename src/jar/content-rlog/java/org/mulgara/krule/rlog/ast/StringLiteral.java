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

import org.mulgara.krule.rlog.rdf.Literal;
import org.mulgara.krule.rlog.rdf.RDFNode;

/**
 * A quoted string in the AST.
 * 
 * @created May 16, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class StringLiteral extends Node implements PredicateParam {

  /** The string value. */
  public final String value;

  /**
   * A new string literal.
   * @param value The contents of the quoted string.
   */
  public StringLiteral(String value) {
    this.value = value;
  }

  // inheritdoc
  public void accept(TreeWalker walker) {
    walker.visit(this);
  }

  // inheritdoc
  public void print(int indent) {
    System.out.println(sp(indent) + "StringLiteral ('" + value + "')");
  }

  //inheritdoc
  public boolean equals(Object o) {
    return o instanceof StringLiteral && value.equals(((StringLiteral)o).value);
  }

  // inheritdoc
  public int hashCode() {
    return value.hashCode();
  }

  /** {@inheritDoc} */
  public RDFNode getRDFNode() {
    return new Literal(value.toString());
  }
}

