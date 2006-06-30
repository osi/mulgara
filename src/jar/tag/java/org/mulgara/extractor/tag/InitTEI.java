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

package org.mulgara.extractor.tag;

// standard java packages
import java.net.*;

import javax.servlet.jsp.*;
import javax.servlet.jsp.tagext.*;

/**
 * Validates attributes for the {@link InitTag} tag.
 *
 * @created 2001-10-15
 *
 * @author Tom Adams
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:08 $ by $Author: newmana $
 *
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 *
 * @copyright &copy;2001 <a href="http://www.pisoftware.com/">Plugged In
 *      Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class InitTEI extends TagExtraInfo {

  /**
   * Validates the run-time attribute values of the init tag.
   *
   * @param data run-time attributes of the init tag
   * @return The Valid value
   */
  public boolean isValid(TagData data) {

    // assume the tag is valid
    boolean valid = true;

    // validate the server attribute
    Object server = data.getAttribute(InitTag.ATTR_SERVER);

    if ((server != null) && (server != TagData.REQUEST_TIME_VALUE)) {

      try {

        // try to convert it to a URL
        URL serverURL = new URL((String) server);
      }
       catch (Exception e) {

        valid = false;
      }

      // try-catch
    }

    // end if
    // only continue if the server parameter validated
    if (valid) {

      // validate the model attribute
      Object model = data.getAttribute(InitTag.ATTR_MODEL);

      if ((model != null) && (model != TagData.REQUEST_TIME_VALUE)) {

        try {

          // try to convert it to a URI
          URI modelURL = new URI((String) model);
        }
         catch (Exception e) {

          valid = false;
        }

        // try-catch
      }

      // end if
    }

    // end if
    // only continue if the server and model parameters validated
    if (valid) {

      // validate the scope attribute
      Object scope = data.getAttribute(InitTag.ATTR_SCOPE);

      if ((scope != null) && (scope != TagData.REQUEST_TIME_VALUE)) {

        // check each value the attribute can take
        if (!(((String) scope).equalsIgnoreCase(InitTag.PAGE_SCOPE)) &&
            !(((String) scope).equalsIgnoreCase(InitTag.REQUEST_SCOPE)) &&
            !(((String) scope).equalsIgnoreCase(InitTag.SESSION_SCOPE)) &&
            !(((String) scope).equalsIgnoreCase(InitTag.APPLICATION_SCOPE))) {

          valid = false;
        }

        // end if
      }

      // end if
    }

    // end if
    // return the validity of the attribute value
    return valid;
  }

  // isValid()
}


// InitTEI
