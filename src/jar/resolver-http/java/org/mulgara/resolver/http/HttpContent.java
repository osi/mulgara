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

package org.mulgara.resolver.http;

// Java 2 standard packages
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.*;

// Java 2 enterprise packages
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

//Third party packages
import org.apache.commons.httpclient.*;  // Apache HTTP Client
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.methods.*;
import org.apache.log4j.Logger;          // Apache Log4J

//Local packages
import org.mulgara.content.Content;
import org.mulgara.content.NotModifiedException;

/**
 * Wrapper around a {@link URL}to make it satisfy the {@link Content}
 * interface.
 * 
 * @created 2004-09-23
 * @author Mark Ludlow
 * @version $Revision: 1.8 $
 * @modified $Date: 2005/01/05 04:58:45 $
 * @maintenanceAuthor $Author: newmana $
 * @company <a href="mailto:info@tucanatech.com">Tucana Technology </a>
 * @copyright &copy; 2004 <a href="http://www.tucanatech.com/">Tucana Technology
 *            Inc </a>
 * @licence <a href=" {@docRoot}/../../LICENCE">Mozilla Public License v1.1
 *          </a>
 */
public class HttpContent implements Content {

  /** Logger. */
  private final static Logger logger =
    Logger.getLogger(HttpContent.class.getName());

  /** The URI version of the URL */
  private URI httpUri;

  /**
   * A map containing any format-specific blank node mappings from previous
   * parses of this file.
   */
  private Map blankNodeMap = new HashMap();

  /**
   * Connection host <code>host</code>
   */
  private String host;

  /**
   * <code>port</code> to make connection to
   */
  private int port;

  /**
   * Schema for connection <code>schema</code>
   */
  private String schema;

  /**
   * A container for HTTP attributes that may persist from request to request
   */
  private HttpState state = new HttpState();

  /**
   * Http connection
   */
  private HttpConnection connection = null;

  /**
   * To obtain the http headers only
   */
  private static final int HEAD = 1;

  /**
   * To obtain the response body
   */
  private static final int GET = 2;

  /**
   * Max. number of redirects
   */
  private static final int MAX_NO_REDIRECTS = 10;

  public HttpContent(URI uri) throws URISyntaxException, MalformedURLException {
    this(uri.toURL());
  }


  /**
   * Constructor.
   * 
   * @param url The URL this object will be representing 
   * the content of
   */
  public HttpContent(URL url) throws URISyntaxException {

    // Validate "url" parameter
    if (url == null) {

      throw new IllegalArgumentException("Null \"url\" parameter");
    }

    initialiseSettings(url);
  }

  /**
   * Initialise the basic settings for a connection
   * 
   * @param url
   *          location of source
   * @throws URISyntaxException
   *           invalid URI
   */
  private void initialiseSettings(URL url) throws URISyntaxException {

    // Convert the URL to a Uri
    httpUri = new URI(url.toExternalForm());

    // obtain basic details for connections
    host = httpUri.getHost();
    port = httpUri.getPort();
    schema = httpUri.getScheme();

  }

  /**
   * Retrieves the node map used to ensure that blank nodes are consistent.
   * 
   * @return The node map used to ensure that blank nodes are consistent
   */
  public Map getBlankNodeMap() {

    return blankNodeMap;
  }

  /**
   * Obtain the approrpriate connection method
   * 
   * @param methodType
   *          can be HEAD or GET
   * @return HttpMethodBase method
   */
  private HttpMethod getConnectionMethod(int methodType) {

    if (methodType != GET && methodType != HEAD) {
      throw new IllegalArgumentException(
          "Invalid method base supplied for connection");
    }

    Protocol protocol = Protocol.getProtocol(schema);

    connection = new HttpConnection(host, port, protocol);

    String proxyHost = System.getProperty("tucana.httpcontent.proxyHost");

    if (proxyHost != null && proxyHost.length() > 0) {
      connection.setProxyHost(proxyHost);
    }

    String proxyPort = System.getProperty("tucana.httpcontent.proxyPort");
    if (proxyPort != null && proxyPort.length() > 0) {
      connection.setProxyPort(Integer.parseInt(proxyPort));
    }

    // default timeout to 30 seconds
    connection.setConnectionTimeout(Integer.parseInt(System.getProperty(
        "tucana.httpcontent.timeout", "30000")));

    String proxyUserName = System
        .getProperty("tucana.httpcontent.proxyUserName");
    if (proxyUserName != null) {
      state.setCredentials(System.getProperty("tucana.httpcontent.proxyRealm"),
          System.getProperty("tucana.httpcontent.proxyRealmHost"),
          new UsernamePasswordCredentials(proxyUserName, System
              .getProperty("tucana.httpcontent.proxyPassword")));
    }

    HttpMethod method = null;
    if (methodType == HEAD) {
      method = new HeadMethod(httpUri.toString());
    }
    else {
      method = new GetMethod(httpUri.toString());
    }

    if (connection.isProxied() && connection.isSecure()) {
      method = new ConnectMethod(method);
    }

    // manually follow redirects due to the
    // strictness of http client implementation

    method.setFollowRedirects(false);

    return method;
  }

  private final Map lastModifiedMap = new WeakHashMap();
  private final Map eTagMap = new WeakHashMap();

  /**
   * Obtain a valid connection and follow redirects if necessary.
   * 
   * @param methodType
   *          request the headders (HEAD) or body (GET)
   * @return valid connection method. Can be null.
   * @throws NotModifiedException  if the content validates against the cache
   * @throws IOException  if there's difficulty communicating with the web site
   */
  private HttpMethod establishConnection(int methodType)
    throws IOException, NotModifiedException
  {
    if (logger.isDebugEnabled()) {
      logger.debug("Establishing connection");
    }

    HttpMethod method = this.getConnectionMethod(methodType);
    Header header = null;

    if (method != null) {
      /*
      // Add cache validation headers to the request
      if (lastModifiedMap.containsKey(httpUri)) {
        String lastModified = (String) lastModifiedMap.get(httpUri);
        assert lastModified != null;
        method.addRequestHeader("If-Modified-Since", lastModified);
      }

      if (eTagMap.containsKey(httpUri)) {
        String eTag = (String) eTagMap.get(httpUri);
        assert eTag != null;
        method.addRequestHeader("If-None-Match", eTag);
      }
      */
     
      // Make the request
      if (logger.isDebugEnabled()) {
        logger.debug("Executing HTTP request");
      }
      method.execute(state, connection);
      if (logger.isDebugEnabled()) {
        logger.debug("Executed HTTP request, response code " +
                     method.getStatusCode());
      }

      // Interpret the response header
      if (method.getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
        // cache has been validated
        throw new NotModifiedException(httpUri);
      }
      else if (!isValidStatusCode(method.getStatusCode())) {
        throw new UnknownHostException("Unable to obtain connection to "
            + httpUri + ". Returned status code " + method.getStatusCode());
      }
      else {
        // has a redirection been issued
        int numberOfRedirection = 0;
        while (isRedirected(method.getStatusCode())
            && numberOfRedirection <= MAX_NO_REDIRECTS) {
          
          // release the existing connection
          method.releaseConnection();
          
          //attempt to follow the redirects
          numberOfRedirection++;

          // obtain the new location
          header = method.getResponseHeader("location");
          if (header != null) {
            try {
              initialiseSettings(new URL(header.getValue()));
              if (logger.isInfoEnabled()) {
                logger.info("Redirecting to " + header.getValue());
              }

              // attempt a new connection to this location
              method = this.getConnectionMethod(methodType);
              method.execute(state, connection);
              if (!isValidStatusCode(method.getStatusCode())) {
                throw new UnknownHostException(
                    "Unable to obtain connection to " + " the redirected site "
                        + httpUri + ". Returned status code "
                        + method.getStatusCode());
              }
            }
            catch (URISyntaxException ex) {
              throw new IOException("Unable to follow redirection to "
                  + header.getValue() + " Not a valid URI");
            }
          }
          else {
            throw new IOException("Unable to obtain redirecting detaild from "
                + httpUri);
          }
        }
      }
    }
    else {
      if (logger.isDebugEnabled()) {
        logger.debug("Establish connection returned a null method");
      }
    }

    // Update metadata about the cached document
    Header lastModifiedHeader = method.getResponseHeader("Last-Modified");
    if (lastModifiedHeader != null) {
      logger.debug(lastModifiedHeader.toString());
      assert lastModifiedHeader.getValues().length == 1;
      assert lastModifiedHeader.getValues()[0].getName() != null;
      assert lastModifiedHeader.getValues()[0].getName() instanceof String;
      lastModifiedMap.put(httpUri, lastModifiedHeader.getValues()[0]
                                                     .getName());
    }

    Header eTagHeader = method.getResponseHeader("Etag");
    if (eTagHeader != null) {
      logger.debug(eTagHeader.toString());
      assert eTagHeader.getValues().length == 1;
      assert eTagHeader.getValues()[0].getName() != null;
      assert eTagHeader.getValues()[0].getName() instanceof String;
      eTagMap.put(httpUri, eTagHeader.getValues()[0].getName());
    }

    return method;
  }

  /**
   * {@inheritDoc}
   *
   * This particular implementation tries to read the content type directly
   * from the HTTP <code>Content-Type</code> header.
   */
  public MimeType getContentType() throws NotModifiedException {

    MimeType mimeType = null;
    HeadMethod method = null;
    String contentType = null;

    try {

      // obtain connection and retrieve the headers
      method = (HeadMethod) establishConnection(HEAD);
      Header header = method.getResponseHeader("Content-Type");
      if (header != null) {
        contentType = header.getValue();
        mimeType = new MimeType(contentType);
        if (logger.isInfoEnabled()) {
          logger.info("Obtain content type " + mimeType + "  from " + httpUri);
        }
      }
    }
    catch (MimeTypeParseException e) {
      logger.warn("Unable to parse " + contentType + " as a content type for "
          + httpUri);
    }
    catch (IOException e) {
      logger.info("Unable to obtain content type for " + httpUri);
    }
    catch (java.lang.IllegalStateException e) {
      logger.info("Unable to obtain content type for " + httpUri);
    }
    finally {
      if (method != null) {
        method.releaseConnection();
      }
      if (connection != null) {
        connection.close();
      }
    }
    return mimeType;
  }

  /**
   * Retrieves the URI for the actual content.
   * 
   * @return The URI for the actual content
   */
  public URI getURI() {

    return httpUri;
  }

  /**
   * Creates an input stream to the resource whose content we are representing.
   * 
   * @return An input stream to the resource whose content we are representing
   * @throws IOException
   */
  public InputStream newInputStream() throws IOException, NotModifiedException {

    if (logger.isDebugEnabled()) {
      logger.debug("Getting new input stream for " + httpUri);
    }

    // Create an input stream by opening the URL's input stream
    GetMethod method = null;
    InputStream inputStream = null;

    // obtain connection and retrieve the headers
    method = (GetMethod) establishConnection(GET);
    inputStream = method.getResponseBodyAsStream();
    if (inputStream == null) {
      throw new IOException("Unable to obtain inputstream from " + httpUri);
    }
    if (logger.isDebugEnabled()) {
      logger.debug("Got new input stream for " + httpUri);
    }
    return inputStream;
  }

  /**
   * @throws IOException always (not implemented)
   */
  public OutputStream newOutputStream() throws IOException {
    throw new IOException("Output of HTTP content not implemented");
  }

  private boolean isValidStatusCode(int status) {
    return (status == HttpStatus.SC_OK || isRedirected(status));
  }

  private boolean isRedirected(int status) {
    return (status == HttpStatus.SC_TEMPORARY_REDIRECT
        || status == HttpStatus.SC_MOVED_TEMPORARILY
        || status == HttpStatus.SC_MOVED_PERMANENTLY || status == HttpStatus.SC_SEE_OTHER);
  }

}
