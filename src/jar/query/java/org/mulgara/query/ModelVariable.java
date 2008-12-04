/**
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

package org.mulgara.query;

// Java 2 standard packages
import java.net.*;
import java.util.*;

// Third party packages
import org.apache.log4j.Logger;

/**
 * A leaf expression containing a variable that is used for the model expression.
 *
 * @created Apr 22, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ModelVariable implements Model {

  /** Used for serializing. */
  static final long serialVersionUID = 5132086338306266830L;

  /** Logger. */
  @SuppressWarnings("unused")
  private final static Logger logger = Logger.getLogger(ModelVariable.class);

  /** The variable for the graph */
  private Variable variable;

  //
  // Constructors
  //

  /**
   * Construct a model from a {@link URL}.
   *
   * @param uri the {@link URI} of the model to query
   * @throws IllegalArgumentException if <var>url</var> is <code>null</code>
   */
  public ModelVariable(Variable variable) {
    if (variable == null) throw new IllegalArgumentException("Null variable parameter");
    this.variable = variable;
  }

  //
  // Methods implementing ModelExpression
  //

  /**
   * Gets a set of database URIs to operate against.
   * @return We don't know what is in the variable, so return the empty {@link Set}
   */
  public Set<URI> getDatabaseURIs() {
    return Collections.emptySet();
  }

  /**
   * Gets a set of graph URIs this represents.
   * @return We don't know what is in the variable, so return the empty {@link Set}
   */
  public Set<URI> getGraphURIs() {
    return Collections.emptySet();
  }

  //
  // API methods
  //

  /**
   * Accessor for the <var>variable</var> property.
   * @return a {@link Variable} instance
   */
  public Variable getVariable() {
    return variable;
  }

  //
  // Methods extending Object
  //

  /**
   * The text representation of the URI.
   * @return the text representation of the URI.
   */
  public String toString() {
    return variable.toString();
  }

  //
  // Methods overriding Object
  //

  /**
   * Return true if the variables of a ModelVariable are equal.
   * @param object ModelVariable to test equality.
   * @return true if the variables of a ModelVariable are equal.
   */
  public boolean equals(Object object) {
    if (object == null) return false;
    if (object == this) return true;

    if (!(object instanceof ModelVariable)) return false;
    ModelVariable modelVar = (ModelVariable)object;
    return variable.equals(modelVar.variable);
  }

  /**
   * Returns the hashCode of a Variable.
   * @return the hashCode of a Variable.
   */
  public int hashCode() {
    return variable.hashCode();
  }

  /**
   * Returns just the default Object clone.
   * @return just the default Object clone.
   */
  public Object clone() {
    try {
      ModelVariable cloned = (ModelVariable)super.clone();
      cloned.variable = variable;
      return cloned;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException("ModelVariable not cloneable");
    }
  }

}
