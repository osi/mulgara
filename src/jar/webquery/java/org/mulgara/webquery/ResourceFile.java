package org.mulgara.webquery;

import java.io.IOException;
import java.io.InputStream;

public abstract class ResourceFile {

  /** The location of resource files. */
  static final String RESOURCES = "resources";
  
  /** The path of the resource file to load. */
  protected String resourceFile;

  /**
   * Create the ResourceFile.
   * @param resourceFile The path to the resource file.
   */
  ResourceFile(String resourceFile) {
    this.resourceFile = RESOURCES + resourceFile;
  }

  /**
   * Get the data from the resource file as a stream.
   * @return An InputStream for accessing the resource file.
   */
  protected InputStream getStream() throws IOException {
    InputStream in = getClass().getClassLoader().getResourceAsStream(resourceFile);
    if (in == null) throw new IOException("Unable to load resource: " + resourceFile);
    return in;
  }
}