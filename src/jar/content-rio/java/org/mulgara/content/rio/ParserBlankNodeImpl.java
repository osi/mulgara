/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 *
 * The Original Code is the Kowari Metadata Store.
 *
 * The Initial Developer of the Original Code is Plugged In Software Pty
 * Ltd (http://www.pisoftware.com, mailto:info@pisoftware.com). Portions
 * created by Plugged In Software Pty Ltd are Copyright (C) 2001,2002
 * Plugged In Software Pty Ltd. All Rights Reserved.
 *
 * Contributor(s): N/A.
 *
 * [NOTE: The text of this Exhibit A may differ slightly from the text
 * of the notices in the Source Code files of the Original Code. You
 * should use the text of this Exhibit A rather than the text found in the
 * Original Code Source Code for Your Modifications.]
 *
 */

package org.mulgara.content.rio;

// Java 2 standard packages

// Third party packages
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.graph.*;

// Locally written packages

/**
 * A BlankNode that is valid in the context of an RDF/XML file that is being
 * parsed.
 *
 * @created 2004-12-22
 *
 * @author David Makepeace
 *
 * @version $Revision: 1.2 $
 *
 * @modified $Date: 2005/02/02 21:13:36 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy; 2004 <a href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ParserBlankNodeImpl extends AbstractBlankNode implements BlankNode
{

  /** Logger. */
  private static final Logger logger =
      Logger.getLogger(ParserBlankNodeImpl.class.getName());

  /**
   * The sequence number of the unlabelled blank node or zero if this blank
   * node has a label.
   */
  private long seqNo;

  /**
   * The label of the blank node or null if this blank node does not have a
   * label.
   */
  private String label;


  /**
   * Create a BlankNode representing an unlabelled blank node.
   *
   * @param seqNo the sequence number of the blank node.
   */
  public ParserBlankNodeImpl(long seqNo) {
    if (seqNo == 0) {
      throw new IllegalArgumentException("seqNo is zero");
    }
    this.seqNo = seqNo;
  }


  /**
   * Create a BlankNode representing an unlabelled blank node.
   *
   * @param label the label of the blank node.
   */
  public ParserBlankNodeImpl(String label) {
    if (label == null) {
      throw new IllegalArgumentException("label is null");
    }
    this.label = label;
  }


  /**
   * Returns the sequence number of this blank node or zero if this blank node
   * has a label.
   *
   * @return the sequence number.
   */
  public long getSeqNo() {
    return seqNo;
  }


  /**
   * Returns the label of this blank node or null if this blank node does not
   * have a label.
   *
   * @return the label.
   */
  public String getLabel() {
    return label;
  }

}
