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

package org.mulgara.webquery;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import javax.servlet.ServletOutputStream;

/**
 * Loads a resource file, and sends the results to an output stream.
 * This object closes any streams it receives, since any files written to those
 * streams are "binary" and are therefore considered complete.
 *
 * @created Aug 1, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class ResourceBinaryFile extends ResourceFile {

  /** The default buffer size is 10K. */
  private static final int BUFFER_SIZE = 10240;

  /** The buffer size to use. */
  private int bufferSize = BUFFER_SIZE;

  /**
   * Loads a binary resource file.
   * @param resourceFile The path of the resource to load.
   */
  public ResourceBinaryFile(String resourceFile) {
    super(resourceFile);
  }

  /**
   * Loads a binary resource file.
   * @param resourceFile The path of the resource to load.
   */
  public ResourceBinaryFile(String resourceFile, int bufferSize) {
    super(resourceFile);
    this.bufferSize = bufferSize;
  }

  /**
   * Sets the buffer size to use when reading/writing resource files.
   * @param bufferSize The new buffer size to use, in bytes.
   */
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  /**
   * Sends the resource to a given output.
   * @param out The output that will receive the file.
   * @return The provided OutputStream.
   */
  public OutputStream sendTo(OutputStream out) throws IOException {
    InputStream in = getStream();
    try {
      if (in == null) return out;
      byte[] buffer = new byte[bufferSize];
      int r;
      while ((r = in.read(buffer)) != -1) out.write(buffer, 0, r);
    } finally {
      in.close();
      out.close();
    }
    return out;
  }

  /**
   * Sends the resource to a given output, using text transfers.
   * @param out The output that will receive the file.
   * @return The provided ServletOutputStream.
   */
  public ServletOutputStream sendTextTo(ServletOutputStream out) throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(getStream()));
    try {
      String line;
      while ((line = reader.readLine()) != null) out.println(line);
    } finally {
      reader.close();
      out.close();
    }
    return out;
  }

}
