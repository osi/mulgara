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

package org.mulgara.client.drools;

import org.mulgara.itql.ItqlInterpreterBean;
import org.mulgara.itql.ItqlInterpreterException;


/**
 * Rule object for initializing a Drools configuration.
 *
 * @created 2004-07-09
 *
 * @author Paul Gearon
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:57:32 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2001-2004
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class Bootstrap {

  /** The current database session. */
  private ItqlInterpreterBean itql;

  /** The name of the inference model. */
  private String inferenceModel;

  /** The name of the base model. */
  private String sourceModel;


  /**
   * Default constructor.  This seems to be used when loading the drl configuration.
   */
  public Bootstrap() {
    System.out.println("Bootstrap: Default constructor used.");
  }


  /**
   * Main constructor.  This is called in the bootstrap rule of a Drools configuration.
   *
   * @param itql The ItqlInterpreterBean to use.
   * @param inferenceModel The name of the inference model.
   * @param sourceModel The name of the source model.
   */
  public Bootstrap(ItqlInterpreterBean itql, String inferenceModel, String sourceModel) throws ItqlInterpreterException {
    this.itql = itql;
    this.inferenceModel = inferenceModel;
    this.sourceModel = sourceModel;

    // initialize aliases in the bean
    itql.executeUpdate("alias <http://purl.org/dc/elements/1.1/> as dcns ;");
    itql.executeUpdate("alias <http://www.w3.org/1999/02/22-rdf-syntax-ns#> as rdfns ;");
    itql.executeUpdate("alias <http://www.w3.org/2000/01/rdf-schema#> as rdfsns ;");
    itql.executeUpdate("alias <http://mulgara.org/mulgara#> as mulgarans ;");
  }


  /**
   * Retrieves the database session to be used for this Drools invocation.
   *
   * @return The database session for use with inferencing.
   */
  public ItqlInterpreterBean getItqlInterpreterBean() {
    return itql;
  }


  /**
   * Retrieves the name of the inference model.
   *
   * @return The name of the inference model.
   */
  public String getInferenceModel() {
    return inferenceModel;
  }


  /**
   * Retrieves the name of the model containing the base facts.
   *
   * @return The name of the base facts model.
   */
  public String getSourceModel() {
    return sourceModel;
  }


}

