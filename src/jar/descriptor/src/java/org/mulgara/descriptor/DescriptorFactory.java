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

package org.mulgara.descriptor;

import java.lang.ref.*;
import java.net.*;
import java.util.*;

import org.apache.log4j.*;
import org.mulgara.itql.ItqlInterpreterBean;

/**
 * Factory that control access and creation of descriptors.
 *
 * @created 2002-03-15
 *
 * @author Keith Ahern
 *
 * @version $Revision: 1.8 $
 *
 * @modified $Date: 2005/01/05 04:58:11 $
 *
 * @maintenanceAuthor $Author: newmana $
 *
 * @company <A href="mailto:info@PIsoftware.com">Plugged In Software</A>
 *
 * @copyright &copy; 2001-2003 <A href="http://www.PIsoftware.com/">Plugged In
 *      Software Pty Ltd</A>
 *
 * @licence <a href="{@docRoot}/../../LICENCE">Mozilla Public License v1.1</a>
 */
public class DescriptorFactory {

  /**
   * Description of the Field
   */
  private static Logger log = Logger.getLogger(DescriptorFactory.class);

  /**
   * Description of the Field
   */
  private static DescriptorFactory descriptorFactory = null;

  /**
   * Description of the Field
   */
  private static Map busyPool = new HashMap();

  /**
   * Description of the Field
   */
  private static Map freePool = new HashMap();

  /**
   * Description of the Field
   */
  private ItqlInterpreterBean bean = null;

  /**
   * CONSTRUCTOR DescriptorFactory TO DO
   */
  public DescriptorFactory() {
    this(null);
  }

  /**
   * CONSTRUCTOR DescriptorFactory TO DO
   *
   * @param bean PARAMETER TO DO
   */
  public DescriptorFactory(ItqlInterpreterBean bean) {

    this.bean = bean;
  }

  /**
   * Gets the Instance attribute of the DescriptorFactory class
   *
   * @return The Instance value
   */
  public static DescriptorFactory getInstance() {

    if (descriptorFactory == null) {

      synchronized (DescriptorFactory.class) {

        descriptorFactory = new DescriptorFactory();
      }
    }

    return descriptorFactory;
  }

  /**
   * Call with this getInstance first to set the bean to use when creating
   * descriptors TODO very ugly must fix
   *
   * @param bean PARAMETER TO DO
   * @return The Instance value
   */
  public static DescriptorFactory getInstance(ItqlInterpreterBean bean) {

    if (descriptorFactory == null) {

      synchronized (DescriptorFactory.class) {

        descriptorFactory = new DescriptorFactory(bean);
      }
    }

    return descriptorFactory;
  }

  /**
       * Return the InterpreterBean used by the factory to pass to descriptors. bean
   * will be null if factory was constructed without one.
   *
   * @return bean see comment
   */
  public ItqlInterpreterBean getItqlInterpreterBean() {

    return bean;
  }

  /**
   * Gets the Descriptor attribute of the DescriptorFactory object
   *
   * @param url PARAMETER TO DO
   * @return The Descriptor value
   * @throws DescriptorException EXCEPTION TO DO
   */
  public Descriptor getDescriptor(URL url) throws DescriptorException {

    Descriptor descriptor;

    // get a descriptor from the pool
    descriptor = getFromFreePool(url);

    if (descriptor == null) {

      log.info("creating descriptor! " + url);
      descriptor = new Descriptor(url, bean);
    }
    else if (log.isDebugEnabled()) {

      log.debug("reusing descriptor! " + url);
    }

    // put in busy pool
    putInBusyPool(descriptor);

    return descriptor;
  }

  /**
   * Releases a descriptor
   *
   * @param descriptor PARAMETER TO DO
   * @throws DescriptorException EXCEPTION TO DO
   */
  public void releaseDescriptor(Descriptor descriptor) throws
      DescriptorException {

    putInFreePool(getFromBusyPool(descriptor));
  }

  /**
   * Clears the cache of descriptors. this is normally done if a descriptor has
   * been changed externally and the cached versions are now invalid
   *
   * @throws DescriptorException EXCEPTION TO DO
   */
  public void clearDescriptorCache() throws DescriptorException {

    log.info("Clearing Descriptor Cache");

    synchronized (busyPool) {

      //resetDescriptorSet(busyPool.keySet(), busyPool);
      //busyPool.clear();
      Set urls = busyPool.keySet();
      Set set = null;
      URL url = null;

      for (Iterator i = urls.iterator(); i.hasNext(); ) {

        // get next URL
        url = (URL) i.next();

        // get the set of these descriptors
        set = (Set) busyPool.get(url);

        if (set.size() != 0) {

          log.warn("Clearing descriptor Cache but there are still " +
              " active descriptors in busy pool for " + url + " these " +
              " have been ignored");
        }
      }
    }

    synchronized (freePool) {

      resetDescriptorSet(freePool.keySet(), freePool);
      freePool.clear();
    }
  }

  /**
       * Gets a descriptor from the busy pool, the same descriptor is returned - its
   * not in ANY pool when returned
   *
   * @param descriptor PARAMETER TO DO
   * @return The FromBusyPool value
   * @throws DescriptorException EXCEPTION TO DO
   */
  private Descriptor getFromBusyPool(Descriptor descriptor) throws
      DescriptorException {

    URL url = descriptor.getURL();

    synchronized (busyPool) {

      Set busyDes = (Set) busyPool.get(url);

      if ( (busyDes == null) || (busyDes.size() == 0)) {

        throw new DescriptorException(
            "tried to remove descriptor from empty busy pool " +
            " and its not there! URL: " + url + " Pool Set: " + busyDes);
      }

      if (!busyDes.remove(descriptor)) {

        throw new DescriptorException(
            "tried to remove descriptor from busy pool " +
            " and its not there! URL: " + url);
      }
    }

    return descriptor;
  }

  /**
   * gets a descriptor from the free pool or null if there isn't one
   *
   * @param url PARAMETER TO DO
   * @return The FromFreePool value
   */
  private Descriptor getFromFreePool(URL url) {

    Descriptor descriptor = null;
    SoftReference ref = null;

    synchronized (freePool) {

      Set freeDes = (Set) freePool.get(url);

      if ( (freeDes != null) && (freeDes.size() > 0)) {

        // get first available non null descriptor
        // debug
        if (log.isDebugEnabled()) {

          log.debug("Looking in free pool for : " + url);
        }

        // iterate thru set of soft ref'ed descriptors..
        for (Iterator i = freeDes.iterator(); i.hasNext(); ) {

          //descriptor = (Descriptor)(((SoftReference)i.next()).get());
          ref = (SoftReference) i.next();
          descriptor = (Descriptor) ref.get();

          // got a non garbage collected descriptor
          if (descriptor != null) {

            // debug
            if (log.isDebugEnabled()) {

              log.debug("Got Free descriptor : " + url);
            }

            // debug
            if (log.isDebugEnabled()) {

              log.debug("free pool size b4 removal : " + freeDes.size());
            }

            // remove it from the free list
            freeDes.remove(ref);

            // debug
            if (log.isDebugEnabled()) {

              log.debug("free pool size after removal : " + freeDes.size());
            }

            // return it
            return descriptor;
          }
          else if (log.isDebugEnabled()) {

            log.debug("free pool descriptor gc'ed: " + url);
          }
        }

        // debug
        if (log.isDebugEnabled()) {

          log.debug("Free pool soft referenced descriptors gc'ed for  : " +
              url);
        }

        // if we got here then all soft referenced descriptors have been garbage
        // collected
        return null;
      }
      else {

        // debug
        if (log.isDebugEnabled()) {

          log.debug("Nothing in free pool for : " + url);
        }

        // nothing in pool
        return null;
      }
    }
  }

  /**
   * puts a descriptor in a pool
   *
   * @param descriptor PARAMETER TO DO
   */
  private void putInFreePool(Descriptor descriptor) {

    // URL of descriptor - key to map of Sets of descriptors
    URL url = descriptor.getURL();

    synchronized (freePool) {

      // get the set for this URL
      Set set = (Set) freePool.get(url);

      // create set if needed
      if (set == null) {

        set = new HashSet();
        freePool.put(url, set);
      }

      // add this descriptor to the set
      set.add(new SoftReference(descriptor));

      // debug
      if (log.isDebugEnabled()) {

        log.debug("Free Pool size: " + set.size() + " for url: " + url);
      }
    }
  }

  /**
   * puts a descriptor in a persistent pool
   *
   * @param descriptor PARAMETER TO DO
   */
  private void putInBusyPool(Descriptor descriptor) {

    // URL of descriptor - key to map of Sets of descriptors
    URL url = descriptor.getURL();

    synchronized (busyPool) {

      // get the set for this URL
      Set set = (Set) busyPool.get(url);

      // create set if needed
      if (set == null) {

        set = new HashSet();
        busyPool.put(url, set);
      }

      // debug
      if (log.isDebugEnabled()) {

        log.debug("Busy Pool size: " + set.size());
      }

      // add this descriptor to the set
      set.add(descriptor);
    }
  }

  /**
   * resets descriptors in a set
   *
   * @param descriptors PARAMETER TO DO
   * @param pool PARAMETER TO DO
   * @throws DescriptorException EXCEPTION TO DO
   */
  private void resetDescriptorSet(Set descriptors, Map pool) throws
      DescriptorException {

    // reset every descriptor
    URL url = null;
    Descriptor des = null;
    Set set = null;

    synchronized (pool) {

      for (Iterator i = descriptors.iterator(); i.hasNext(); ) {

        url = (URL) i.next();

        // get the set of these descriptors
        set = (Set) pool.get(url);

        // go thru each and reset
        for (Iterator pi = set.iterator(); pi.hasNext(); ) {

          Object obj = pi.next();

          if (obj instanceof SoftReference) {

            des = (Descriptor) ( (SoftReference) obj).get();
          }
          else if (obj instanceof Descriptor) {

            des = (Descriptor) obj;
          }
          else {

            throw new DescriptorException("Found wacky object " + obj +
                " in Set, should be SoftReference or Descriptor");
          }

          if (des != null) {

            des.resetSettings();
          }
          else {

            // remove this from the set, its not used any more
            set.remove(obj);
          }
        }
      }
    }
  }
}