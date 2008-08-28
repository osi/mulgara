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

package org.mulgara.util;

import java.io.File;
import java.io.IOException;

/**
 * Utility for describing a classpath, and extracting elements if needed.
 *
 * @created Aug 28, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ClasspathDesc {

  /** The identifier for the system classpath property. */
  private static final String JAVA_CLASS_PATH = "java.class.path";


  /**
   * @return The system classpath.
   */
  public static String getPath() {
    return System.getProperty(JAVA_CLASS_PATH);
  }


  /**
   * @return The elements of the classpath as an array of strings.
   */
  public static String[] getPaths() {
    return System.getProperty(JAVA_CLASS_PATH).split(File.pathSeparator);
  }


  /**
   * Looks in the classpath for a file that matches the <value>expected</value> parameter.
   * @param expected This will be part of a filename to search for.
   * @return The first filename in the classpath that contains <value>expected</value>.
   *         <code>null</code> if not found.
   */
  public static String getPath(String expected) {
    for (String path: getPaths()) if (path.contains(expected)) return path;
    return null;
  }

  /**
   * Creates a copy of a file from the classpath in a temporary directory.
   * @param expected Part of the name of the file being looked for in the classpath.
   * @return The path of the temporary file.
   * @throws IOException If there was a problem reading or writing the file.
   */
  public static String createTempCopy(String expected) throws IOException {
    String path = getPath(expected);
    if (path == null) return null;
    File dir = TempDir.getTempDir();
    return FileUtil.copyFile(path, new File(dir,path));
  }

}
