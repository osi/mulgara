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
package org.mulgara.query.filter;

import org.mulgara.query.QueryException;


/**
 * Filters the iteration of a Constraint.
 *
 * @created Mar 7, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="mailto:pgearon@users.sourceforge.net">Paul Gearon</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface Filter extends ContextOwner, RDFTerm {

  /**
   * Tests a binding according to the filter.
   * @param context The context to resolve the filter in.
   * @return <code>true</code> when the filter is matched.
   * @throws QueryException The filter found an error during testing.
   */
  public boolean test(Context context) throws QueryException;

  /** A filter that does no filtering. */
  public static final Filter NULL = new Filter() {
    private static final long serialVersionUID = -1561779107566375359L;
    public boolean test(Context context) { return true; }
    public void setCurrentContext(Context context) { }
    public Context getCurrentContext() { return null; }
    // RDFTerm methods
    public boolean equals(RDFTerm v) throws QueryException { return v == this; }
    public ContextOwner getContextOwner() { return null; }
    public Object getValue() throws QueryException { return true; }
    public boolean isBlank() throws QueryException { return false; }
    public boolean isIRI() throws QueryException { return false; }
    public boolean isLiteral() throws QueryException { return true; }
    public boolean isURI() throws QueryException { return false; }
    public boolean sameTerm(RDFTerm v) throws QueryException { return equals(v); }
    public void setContextOwner(ContextOwner owner) { }
    public void addContextListener(ContextOwner l) { }
  };

}
