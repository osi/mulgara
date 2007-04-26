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

package org.mulgara.util;

// Java 2 standard packages
import java.io.File;
import org.apache.log4j.*;

/**
 * General file utility methods.
 *
 * @created 2003-11-27
 *
 * @author <a href="http://staff.pisoftware.com/raboczi">Simon Raboczi</a>
 *
 * @version $Revision: 1.9 $
 *
 * @modified $Date: 2005/01/05 04:59:29 $ by $Author: newmana $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @copyright &copy;2003
 *   <a href="http://www.pisoftware.com/">Plugged In Software Pty Ltd</a>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public abstract class FileUtil {

  /** Logger.  */
  private static Logger logger = Logger.getLogger(FileUtil.class.getName());

  /**
   * Recursively delete a file or directory.
   *
   * This method is not transactional.  If it fails, it may have partially
   * deleted the contents of a file.
   *
   * @param directory the directory to delete, which must exist
   * @return whether the directory was successfully deleted
   */
  public static boolean deleteDirectory(File directory) {
    File[] files = directory.listFiles();
    if (files != null) {
      for (int i = 0; i < files.length; ++i) {
        if (files[i].isFile()) {
          if (!files[i].delete()) {
            logger.warn("Failed to delete " + files[i]);
          }
        } else {
          deleteDirectory(files[i]);
        }
      }
    }

    return directory.delete();
  }
}
