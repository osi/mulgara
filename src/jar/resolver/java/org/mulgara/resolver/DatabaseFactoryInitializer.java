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

package org.mulgara.resolver;

// Java 2 standard packages
import java.net.URI;
import java.util.Set;
import java.io.File;

// Third party packages
import org.apache.log4j.Logger;  // Apache Log4J

// Local packages
import org.mulgara.resolver.spi.*;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.stringpool.StringPool;

/**
 * The database initialiser used to provide configuration information to
 * {@link NodePool} and {@link StringPool} instances.
 *
 * The {@link #close} method can be called after initialization to prevent
 * access to the interface.
 *
 * @created 2004-04-26
 * @author <a href="http://www.pisoftware.com/andrae">Andrae Muys</a>
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:23 $
 * @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@PIsoftware.com">Plugged In Software</a>
 * @copyright &copy;2004 <a href="http://www.tucanatech.com/">Tucana
 *   Technology, Inc</a>
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
class DatabaseFactoryInitializer extends DatabaseInitializer
  implements FactoryInitializer
{
  /** The databases toplevel directory */
  private File directory;

  /** The unique {@link URI} naming this database. */
  private final URI databaseURI;

  /** The set of alternative hostnames for the current host. */
  private final Set hostnameAliases;

  /**
   * Sole constructor.
   *
   * @param directory  the persistence directory to offer this component; if
   *   <code>null</code>, no persistence directory will be offered
   */
  DatabaseFactoryInitializer(
      URI databaseURI, Set hostnameAliases, File directory
  ) {
    this.databaseURI = databaseURI;
    this.hostnameAliases = hostnameAliases;
    this.directory = directory;
  }

  //
  // Methods implementing FactoryInitializer
  //

  public URI getDatabaseURI() {
    checkState();
    return databaseURI;
  }

  public Set getHostnameAliases() {
    checkState();
    return hostnameAliases;
  }

  public File getDirectory() throws InitializerException
  {
    checkState();

    if (directory != null) {
      // Ensure that the directory exists
      if (!directory.isDirectory()) {
        if (!directory.mkdirs()) {
          throw new InitializerException("Couldn't create " + directory);
        }
      }
      assert directory.isDirectory();
    }

    return directory;
  }
}
