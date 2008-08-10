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
 * An abstraction of a table.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public abstract class TableAbstr extends HtmlElement implements TableStructure {

  /** The width of this table structure */
  private int width = 0;


  /**
   * Creates a table with a given indent.
   * @param indent The amount to indent by.
   */
  TableAbstr(int indent) {
    super(indent);
  }


  /**
   * Creates a table with no indenting.
   */
  TableAbstr() {
    this(0);
  }


  /**
   * Creates a table with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param subElements a list of sub elements inside this table cell.
   */
  TableAbstr(int indent, HtmlElement... subElements) {
    super(indent, subElements);
    for (HtmlElement e: subElements) if (e instanceof TableStructure) updateWidth((TableStructure)e);
  }


  /**
   * Creates a table body with no initial indenting, and a list of sub elements.
   * @param subElements a list of sub elements inside this table cell.
   */
  TableAbstr(HtmlElement... subElements) {
    this(0, subElements);
  }


  /**
   * Adds a table element to the end of the list of table elements for this table.
   * @param elt The new element to be added.
   * @return The current table.
   * @throws IllegalArgumentException If elt is anything other than a table structure.
   */
  public HtmlElement add(HtmlElement elt) {
    if (!(elt instanceof TableStructure)) throw new IllegalArgumentException("Tables can only add table structural elements");
    updateWidth((TableStructure)elt);
    return super.add(elt);
  }


  /**
   * Gets the width of this table body.
   * @return The maximum width in this table structure.
   */
  public int getWidth() {
    return width;
  }


  /** @see org.mulgara.webquery.html.HtmlElement#getTag() */
  abstract protected String getTag();


  /**
   * Update the width of this structure to its widest element.
   * @param s A new element that has been added.
   */
  private void updateWidth(TableStructure s) {
    int w = s.getWidth();
    if (w > width) width = w;
  }
}
