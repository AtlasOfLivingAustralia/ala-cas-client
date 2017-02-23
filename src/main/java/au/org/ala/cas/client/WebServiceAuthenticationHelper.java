/***************************************************************************
 * Copyright (C) 2010 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/
package au.org.ala.cas.client;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Helper class that authenticates user credentials prior to invoking a web service that requires authentication.
 * Authentication is carried out during construction and the CAS generated Ticket Granting Ticket is saved for
 * subsequent web service invocations.
 * <p>
 * Invoking a web service using the invoke() method involves obtaining a CAS service ticket which is then passed to
 * the web service provider as a URI parameter.  The web service provider application then obtains the user attributes
 * via its CAS validation filter.
 * 
 * @author peterflemming
 *
 */
public class WebServiceAuthenticationHelper {
	
	private final static Logger logger = LoggerFactory.getLogger(UriFilter.class);
	private final static String CAS_CONTEXT = "/cas/v1/tickets/";
	
	private final String casServer;
	private final String ticketGrantingTicket;
	
	/**
	 * Constructor that authenticates the user credentials and obtains a CAS Ticket Granting ticket.
	 * 
	 * @param casServer The CAS server URI
	 * @param userName	User name
	 * @param password  Password
	 */
	public WebServiceAuthenticationHelper(final String casServer, final String userName, final String password) {
		super();
		this.casServer = casServer;
		ticketGrantingTicket = getTicketGrantingTicket(casServer, userName, password);
	}

	/**
	 * Invokes a web service.  Firstly a CAS Service ticket is obtained and passed with the web service request.
	 * 
	 * @param serviceUrl Web service URI
	 * @return Web service response as a string
	 */
	public String invoke(final String serviceUrl) {
		String serviceTicket = getServiceTicket(this.casServer, this.ticketGrantingTicket, serviceUrl);
		if (serviceTicket != null) {
			return getServiceResponse(serviceUrl, serviceTicket);
		} else {
			return null;
		}
	}
	
	/**
	 * Authenticates user credentials with CAS server and obtains a Ticket Granting ticket.
	 * 
	 * @param server   CAS server URI
	 * @param username User name
	 * @param password Password
	 * @return The Ticket Granting Ticket id
	 */
	private String getTicketGrantingTicket(final String server, final String username, final String password) {
		final CloseableHttpClient client = HttpClients.createDefault();

		final HttpPost post = new HttpPost(server + CAS_CONTEXT);

		try {
            post.setEntity(new UrlEncodedFormEntity(
                    Arrays.asList(
                            new BasicNameValuePair("username", username),
                            new BasicNameValuePair("password", password))
            ));

            CloseableHttpResponse call = client.execute(post);

            final String response;
            final int statusCode;
            try {
                response = EntityUtils.toString(call.getEntity());
                statusCode = call.getStatusLine().getStatusCode();
            } finally {
                call.close();
            }

            switch (statusCode) {
                case 201: {
                    final Matcher matcher = Pattern.compile(
                            ".*action=\".*/(.*?)\".*").matcher(response);

                    if (matcher.matches())
                        return matcher.group(1);

                    logger.warn("Successful ticket granting request, but no ticket found!");
                    logger.info("Response (1k): {}", getMaxString(response));
                    break;
                }

                default:
                    logger.warn("Invalid response code ({}) from CAS server!", statusCode);
                    logger.info("Response (1k): {}", getMaxString(response));
                    break;
            }
		}

		catch (final IOException e) {
			logger.warn("Exception calling {}", post, e);
		}

		finally {
			post.reset();
		}

		return null;
	}

	/**
	 * Obtains a Service ticket for a web service invocation.
	 * 
	 * @param server CAS server URI
	 * @param ticketGrantingTicket TGT id
	 * @param service Web service URI
	 * @return Service ticket id
	 */
	private String getServiceTicket(final String server, final String ticketGrantingTicket, final String service) {
		if (ticketGrantingTicket == null)
			return null;

        final CloseableHttpClient client = HttpClients.createDefault();

		final HttpPost post = new HttpPost(server + CAS_CONTEXT + ticketGrantingTicket);

		try {
            post.setEntity(new UrlEncodedFormEntity(
                    Collections.singletonList(new BasicNameValuePair("service", service))
            ));

            final String response;
            final int statusCode;
            CloseableHttpResponse call = client.execute(post);
            try {

                response = EntityUtils.toString(call.getEntity());
                statusCode = call.getStatusLine().getStatusCode();
            } finally {
                call.close();
            }

            switch (statusCode) {
			case 200:
				return response;

			default:
				logger.warn("Invalid response code ({}) from CAS server!", statusCode);
				logger.info("Response (1k): {}", getMaxString(response));
				break;
			}
		}

		catch (final IOException e) {
			logger.warn("Exception calling {}", post, e);
		}

		finally {
			post.reset();
		}

		return null;
	}

	/**
	 * Invokes a web service request via an HTTP Get method.
	 * 
	 * @param url Web service URI
	 * @param serviceTicket CAS service ticket for web service
	 * @return Web service response as a string
	 */
	private String getServiceResponse(final String url, final String serviceTicket) {
        final CloseableHttpClient client = HttpClients.createDefault();

		final HttpGet get = new HttpGet(url + "?ticket=" + serviceTicket);

		try {
            CloseableHttpResponse call = client.execute(get);

            final String response;
            final int statusCode;
            try {
                response = EntityUtils.toString(call.getEntity());
                statusCode = call.getStatusLine().getStatusCode();
            } finally {
                call.close();
            }

            switch (statusCode) {
			case 200:
				return response;

			default:
				logger.warn("Invalid response code ({}) from web service!", statusCode);
				logger.info("Response (1k): {}",  getMaxString(response));
				break;
			}
		}

		catch (final IOException e) {
			logger.warn("Exception calling {}", get, e);
		}

		finally {
			get.reset();
		}

		return null;
	}

	/**
	 * Truncates a string to a maximum length.
	 * @param string Input string
	 * @return Truncated string
	 */
	private String getMaxString(String string) {
		return string.substring(0, Math.min(1024, string.length()));
	}

}
