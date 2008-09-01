/*
 * Copyright 2008 Fedora Commons
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mulgara.sparql.parser.cst;

import java.util.LinkedList;
import java.util.List;


/**
 * Represents a list of triples to be eventually put together in a conjunction.
 * The first subject in this list may be considered significant.
 *
 * @created Feb 26, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 * @licence <a href="{@docRoot}/../LICENCE.txt">Apache License, Version 2.0</a>
 */
public class TripleList extends BasicGraphPattern {

  /** The list of triples */
  private List<Triple> triples;

  /**
   * Default constructor, creating an empty list.
   */
  public TripleList() {
    triples = new LinkedList<Triple>();
  }

  /**
   * Build a list of triples about a common subject
   * @param subject The subject of every triple in this list
   * @param properties The properties of the subject
   */
  public TripleList(Node subject, PropertyList properties) {
    this();
    for (PropertyList.Property p: properties) triples.add(new Triple(subject, p.getPredicate(), p.getObject()));
  }
  
  /**
   * Build a list of triples about a common subject that is already in a list
   * @param an An annotated node that is the suject of the given properties
   * @param properties The properties of the an
   */
  public TripleList(AnnotatedNode an, PropertyList properties) {
    // start with the existing triples
    triples = new LinkedList<Triple>(an.getAnnotation().triples);
    // add in the triples for the properties
    for (PropertyList.Property p: properties) triples.add(new Triple(an.getSubject(), p.getPredicate(), p.getObject()));
  }
  
  /**
   * Package constructor, used by other classes that build a list of triples
   * @param triples Preallocated list of triples
   */
  TripleList(List<Triple> triples) {
    this.triples = triples;
  }
  
  /**
   * Retrieve the node that represents this list.
   * @return A single list node.
   */
  public Node getSubject() {
    return triples.isEmpty() ? Nil.NIL_NODE : triples.get(0).getSubject();
  }

  /**
   * Appends a triple to the list.
   * @param t The triple to add.
   */
  public void add(Triple t) {
    triples.add(t);
  }

  /**
   * Concatenates another list to this one
   * @param l The TripleList to add.
   */
  public void concat(TripleList l) {
    triples.addAll(l.triples);
  }
  
  /**
   * Convert this list into a GroupGraphPattern
   * @return a conjunction of all triples in the list, or a single triple if there is only one
   */
  public GroupGraphPattern asGroupGraphPattern() {
    if (triples.size() == 1) return triples.get(0);
    return new GraphPatternConjunction(this);
  }

  /**
   * @see org.mulgara.sparql.parser.cst.BasicGraphPattern#getElements()
   */
  @Override
  public List<? extends GroupGraphPattern> getElements() {
    return triples;
  }

  /**
   * @see org.mulgara.sparql.parser.cst.BasicGraphPattern#getImage()
   */
  @Override
  public String getImage() {
    StringBuffer result = new StringBuffer();
    for (Triple t: triples) result.append(" ").append(t.getImage()).append(" .");
    return addPatterns(result.toString());
  }

}
