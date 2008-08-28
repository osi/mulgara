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
 * Represents a paragraph element.
 *
 * @created Aug 4, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class Paragraph extends HtmlElement {

  /**
   * Creates a paragraph with a given indent.
   * @param indent The amount to indent by.
   */
  public Paragraph(int indent) {
    super(indent);
  }


  /**
   * Creates a paragraph with no indenting.
   */
  Paragraph() {
    this(-1);
  }


  /**
   * Creates a paragraph with a given indent, and a list of sub elements.
   * @param indent The amount to indent by.
   * @param subElements a list of sub elements inside this paragraph.
   */
  Paragraph(int indent, HtmlElement... subElements) {
    super(indent, subElements);
  }


  /**
   * Creates a paragraph with no initial indenting, and a list of sub elements.
   * @param subElements a list of sub elements inside this paragraph.
   */
  Paragraph( HtmlElement... subElements) {
    this(-1, subElements);
  }


  protected String getTag() {
    return "p";
  }

}
