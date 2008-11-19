/*
 * Copyright 2008 Fedora Commons, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mulgara.webquery;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.mulgara.util.functional.Pair;

/**
 * Represents the parameters used in an HTTP request.
 *
 * @created Aug 5, 2008
 * @author Paul Gearon
 * @copyright &copy; 2008 <a href="http://www.fedora-commons.org/">Fedora Commons</a>
 */
public class QueryParams {

  /** The encoding to use for a URL */
  static final String ENCODING = "UTF-8";

  /** The parameter names and values. */
  private Map<String,String> params = new LinkedHashMap<String,String>();

  /**
   * Create an empty set of parameters.
   */
  public QueryParams() {
  }

  /**
   * Create a set of parameters from a single name/value pair.
   * @param name The name of the parameter.
   * @param value The value for the parameter.
   */
  public QueryParams(String name, Object value) {
    params.put(name, value.toString());
  }

  /**
   * Create a set of parameters from an array of name/value pairs.
   * @param pairs An array of name/value pairs.
   */
  public QueryParams(Pair<String,String>... pairs) {
    for (Pair<String,String> p: pairs) params.put(p.first(), p.second());
  }

  /**
   * Create a set of parameters from a collection of name/value pairs.
   * @param pairs A collection of name/value pairs.
   */
  public QueryParams(Collection<Pair<String,String>> pairs) {
    for (Pair<String,String> p: pairs) params.put(p.first(), p.second());
  }

  /**
   * Adds a name/value to the parameters.
   * @param pair The name/value pair to be added.
   * @return This parameters object.
   */
  public QueryParams add(Pair<String,String> pair) {
    params.put(pair.first(), pair.second());
    return this;
  }

  /**
   * Adds a name/value to the parameters.
   * @param name The name of the parameter to be added.
   * @param value The valueof the parameter to be added.
   * @return This parameters object.
   */
  public QueryParams add(String name, String value) {
    params.put(name, value);
    return this;
  }

  /**
   * Adds an array of name/values to the parameters.
   * @param pairs The name/value array to be added.
   * @return This parameters object.
   */
  public QueryParams addAll(Pair<String,String>... pairs) {
    for (Pair<String,String> p: pairs) params.put(p.first(), p.second());
    return this;
  }

  /**
   * Adds a collection of name/values to the parameters.
   * @param pairs The name/value collection to be added.
   * @return This parameters object.
   */
  public QueryParams addAll(Collection<Pair<String,String>> pairs) {
    for (Pair<String,String> p: pairs) params.put(p.first(), p.second());
    return this;
  }

  /**
   * Converts this set of parameters to the query portion of a URL.
   * @return a query to be added to a URL.
   */
  public String toString() {
    try {
      boolean first = true;
      StringBuilder b = new StringBuilder();
      for (Map.Entry<String,String> e: params.entrySet()) {
        if (first) first = false;
        else b.append("&amp;");
        b.append(URLEncoder.encode(e.getKey(), ENCODING));
        b.append("=").append(URLEncoder.encode(e.getValue(), ENCODING));
      }
      return b.toString();
    } catch (UnsupportedEncodingException e) {
      throw new IllegalArgumentException("Unable to encode with " + ENCODING + ": " + e.getMessage());
    }
  }
}
