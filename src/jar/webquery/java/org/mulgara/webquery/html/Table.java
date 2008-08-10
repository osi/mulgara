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

package org.mulgara.webquery.html;

/**
 * Represents a table.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Table extends TableAbstr {

  /**
   * Creates a table with a given indent.
   * @param indent The amount to indent by.
   */
  public Table(int indent) {
    super(indent);
  }


  /**
   * Creates a table with no indenting.
   */
  public Table() {
    super();
  }


  /**
   * Creates a table with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param subElements a list of sub elements inside this table.
   */
  public Table(int indent, HtmlElement... subElements) {
    super(indent, subElements);
  }


  /**
   * Creates a table with no initial indenting, and a list of sub elements.
   * @param subElements a list of sub elements inside this table.
   */
  public Table(HtmlElement... subElements) {
    super(subElements);
  }


  /**
   * Adds a table element to the end of the list of table elements for this table.
   * @param elt The new element to be added.
   * @return The current table.
   * @throws IllegalArgumentException If elt is anything other than a table structure.
   */
  public HtmlElement add(HtmlElement elt) {
    if (elt instanceof Table) throw new IllegalArgumentException("Tables can only be embedded in cell, not directly into table elements");
    return super.add(elt);
  }


  /** @see org.mulgara.webquery.html.HtmlElement#getTag() */
  protected String getTag() {
    return "table";
  }

}
