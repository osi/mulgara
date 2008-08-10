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
 * Represents header data in a table.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class TableHeader extends TableData {

  /**
   * Creates a header element with a given indent.
   * @param indent The amount to indent by.
   */
  public TableHeader(int indent) {
    super(indent);
  }


  /**
   * Creates a header element with no indenting.
   */
  public TableHeader() {
    super();
  }


  /**
   * Creates a header element with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param subElements a list of sub elements inside this header cell.
   */
  public TableHeader(int indent, HtmlElement... subElements) {
    super(indent, subElements);
  }


  /**
   * Creates a header element with no initial indenting, and a list of sub elements.
   * @param subElements a list of sub elements inside this header cell.
   */
  public TableHeader(HtmlElement... subElements) {
    super(subElements);
  }


  /**
   * Creates a header element with a given indent, and embedded text.
   * @param indent The amount to indent by.
   * @param text The text to set for this header.
   */
  public TableHeader(int indent, String text) {
    super(indent, text);
  }


  /**
   * Creates a header element with no initial indenting, and embedded text.
   * @param text The text to set for this header.
   */
  public TableHeader(String text) {
    super(text);
  }


  /** @see org.mulgara.webquery.html.HtmlElement#getTag() */
  protected String getTag() {
    return "th";
  }

}
