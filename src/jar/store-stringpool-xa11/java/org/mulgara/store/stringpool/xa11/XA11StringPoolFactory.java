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


package org.mulgara.store.stringpool.xa11;

// Java 2 standard packages
import java.io.File;
import java.io.IOException;

// Third-party packages
import org.apache.log4j.*;

// Local packages
import org.mulgara.resolver.spi.FactoryInitializer;
import org.mulgara.resolver.spi.InitializerException;
import org.mulgara.store.nodepool.NodePool;
import org.mulgara.store.nodepool.NodePoolException;
import org.mulgara.store.nodepool.NodePoolFactory;
import org.mulgara.store.stringpool.StringPool;
import org.mulgara.store.stringpool.StringPoolException;
import org.mulgara.store.stringpool.StringPoolFactory;

/**
 * A {@link StringPoolFactory} that constructs {@link XA11StringPoolImpl} instances.
 *
 * @created Aug 11, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public class XA11StringPoolFactory implements StringPoolFactory, NodePoolFactory {

  /** Logger. */
  private static final Logger logger = Logger.getLogger(XA11StringPoolFactory.class.getName());

  /** The singleton instance of this class. */
  private static StringPoolFactory stringPoolFactory = null;

  /** The base file names. */
  private final String[] baseNames;

  /**
   * This constructor is only for internal use.  Use the {@link #newInstance} method
   * to obtain instances.
   */
  private XA11StringPoolFactory(String[] baseNames) {
    this.baseNames = baseNames;
  }

  /**
   * Obtain the singleton instance of this factory.
   *
   * @param factoryInitializer  not used
   */
  static public StringPoolFactory newInstance(FactoryInitializer factoryInitializer) throws InitializerException {

    if (stringPoolFactory == null) {
      // Lazily initialize the singleton instance
      File[] directories = factoryInitializer.getDirectories();
      String[] paths = new String[directories.length];
      for (int f = 0; f < directories.length; f++) paths[f] = directories[f].toString() + File.separatorChar + "xa11";
      stringPoolFactory = new XA11StringPoolFactory(paths);
    }

    return stringPoolFactory;
  }

  //
  // Methods implementing StringPoolFactory
  //

  /**
   * {@inheritDoc StringPoolFactory}
   */
  public StringPool newStringPool() throws StringPoolException
  {
    try {
      XA11StringPoolImpl xaStringPoolImpl = new XA11StringPoolImpl(baseNames);
      return xaStringPoolImpl;
    } catch (IOException e) {
      logger.error("Couldn't construct string pool", e);
      throw new StringPoolException("Couldn't construct string pool", e);
    }
  }

  public NodePool newNodePool() throws NodePoolException {
    return null;
  }
}
