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
 * Represents data in a table.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class TableData extends HtmlElement {

  /**
   * Creates a data element with a given indent.
   * @param indent The amount to indent by.
   */
  public TableData(int indent) {
    super(indent);
  }


  /**
   * Creates a data element with no indenting.
   */
  public TableData() {
    this(0);
  }


  /**
   * Creates a data element with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param subElements a list of sub elements inside this table cell.
   */
  public TableData(int indent, HtmlElement... subElements) {
    super(indent, subElements);
  }


  /**
   * Creates a data element with no initial indenting, and a list of sub elements.
   * @param subElements a list of sub elements inside this table cell.
   */
  public TableData(HtmlElement... subElements) {
    this(0, subElements);
  }


  /**
   * Creates a data element with a given indent, and embedded text.
   * @param indent The amount to indent by.
   * @param text The text to set for this element.
   */
  public TableData(int indent, String text) {
    super(indent, new Text(text));
  }


  /**
   * Creates a data element with no initial indenting, and embedded text.
   * @param text The text to set for this element.
   */
  public TableData(String text) {
    this(0, new Text(text));
  }


  /** @see org.mulgara.webquery.html.HtmlElement#getTag() */
  protected String getTag() {
    return "td";
  }

}
