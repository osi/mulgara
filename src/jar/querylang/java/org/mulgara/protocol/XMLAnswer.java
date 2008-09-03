package org.mulgara.protocol;

import java.net.URI;

/**
 * Represents an Answer that can be emitted as XML.
 *
 * @created Jul 9, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.topazproject.org/">The Topaz Project</a>
 * @licence <a href="{@docRoot}/../../LICENCE.txt">Open Software License v3.0</a>
 */
public interface XMLAnswer {

  /**
   * Explicitly adds a namespace to be used in the document.
   * @param name The name of the namespace to use.
   * @param nsValue The URI of the namespace.
   */
  public abstract void addNamespace(String name, URI nsValue);

  /**
   * Remove all previously added namespaces.
   */
  public abstract void clearNamespaces();

  /**
   * Sets whether or not to used pretty printing when creating the XML. On by default.
   * @param prettyPrint <code>true</code> to turn pretty printing on. <code>false</code> to turn it off.
   */
  public abstract void setPrettyPrint(boolean prettyPrint);

}