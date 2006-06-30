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

package org.mulgara.demo.mp3.swing.search.constraints;

// Java 2 standard packages
import javax.swing.*;

// Logging
import org.apache.log4j.Logger;

// JRDF
import org.jrdf.util.*;
import org.jrdf.graph.*;

// Local packages
import org.mulgara.demo.mp3.swing.widgets.NodeListRenderer;


/**
 * List of Values to choose from.
 *
 * @created 2004-12-13
 *
 * @author <a href="mailto:robert.turner@tucanatech.com">Robert Turner</a>
 *
 * @version $Revision: 1.3 $
 *
 * @modified $Date: 2005/01/05 04:58:10 $
 *
 * @maintenanceAuthor: $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *   Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class ValueList extends JList {

  /**
   * Logger. This is named after the class.
   */
  private final static Logger log = Logger.getLogger(ValueList.class.getName());

  /** The values to be listed */
  private ClosableIterator values = null;

  /** Used to edit list */
  private DefaultListModel model = null;

  /**
   * Constructor.
   *
   * @param values ClosableIterator containing Triples (Objects are values).
   * @throws Exception
   */
  public ValueList(ClosableIterator values) throws Exception {
    if (values == null) {
      throw new IllegalArgumentException("'values' are null");
    }
    this.values = values;
    setup();
  }

  /**
   * Initilizes and sets up components.
   *
   * @throws Exception
   */
  private void setup() throws Exception {

    //instantiate
    model = new DefaultListModel();

    //initialize
    setModel(model);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    removeAll();
    setValueRenderer();
    populate();
  }

  /**
   * Returns the selected property from the list.
   * @throws IllegalStateException
   * @return ObjectNode
   */
  public ObjectNode getSelected() throws IllegalStateException {
    try {
      return (ObjectNode) getSelectedValue();
    } catch (ClassCastException castException) {
      throw new IllegalStateException("List should only contain ObjectNodes.");
    }
  }

  /**
   * Adds all properties from the Iterator.
   * @throws Exception
   */
  private void populate() throws Exception {
    try {
      Triple current = null;
      while (values.hasNext()) {
        current = (Triple) values.next();
        if (!model.contains(current.getObject())) {
          model.addElement(current.getObject());
        }
      }
    } catch (ClassCastException castException) {
      throw new IllegalStateException("Values should contain Triples.");
    }
  }

  /**
   * Sets a Renderer that can Render Values (ObjectNodes).
   */
  private void setValueRenderer() {
    this.setCellRenderer(new NodeListRenderer());
  }

}
