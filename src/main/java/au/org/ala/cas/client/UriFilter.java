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
import java.util.List;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.jasig.cas.client.authentication.AuthenticationFilter;

import au.org.ala.cas.util.AuthenticationCookieUtils;
import au.org.ala.cas.util.PatternMatchingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Meta filter that provides filtering based on URI patterns that require authentication (and thus redirection) to the CAS server.
 * <p>
 * There are 3 possible filter configurations and these filtering criteria are applied in the following order,
 * </p>
 * <table border="1">
 *     <caption>UriFilter configuration order</caption>
 *     <tr><th>Criterion</th><th>context-param</th><th>Required</th><th>Description</th></tr>
 *     <tr><td>URI exclusion</td><td>uriExclusionFilterPattern</td><td>No</td><td>URIs that should not be subject to CAS authentication</td></tr>
 *     <tr><td>URI inclusion</td><td>uriFilterPattern</td><td>No</td><td>URIs that are to be subject to CAS authentication</td></tr>
 *     <tr><td>Only if logged in</td><td>authenticateOnlyIfLoggedInFilterPattern</td><td>No</td><td>URIs that should be subject to CAS authentication only if logged in (indicated by the presence of the ALA-Auth cookie)</td></tr>
 * </table>
 * <p>
 * The list of URI patterns is specified as a comma delimited list of regular expressions in a <code>&lt;context-param&gt;</code>.
 * <p>
 * So if a request's path matches one of the URI patterns then the filter specified by the <code>filterClass</code> &lt;init-param&gt; is invoked.
 * <p>
 * The <code>contextPath</code> from the <code>ServletContext</code> is prefixed to each URI pattern defined for each filter.
 * <p>
 * An example of usage is shown in the following web.xml fragment,
 * </p>
 * <pre>
     ...

     &lt;context-param&gt;
         &lt;param-name&gt;uriFilterPattern&lt;/param-name&gt;
         &lt;param-value&gt;/occurrences/\d+&lt;/param-value&gt;
     &lt;/context-param&gt;

     &lt;!-- CAS Authentication Service filters --&gt;
     &lt;filter&gt;
         &lt;filter-name&gt;CAS Authentication Filter&lt;/filter-name&gt;
         &lt;filter-class&gt;au.org.ala.cas.client.UriFilter&lt;/filter-class&gt;
         &lt;init-param&gt;
             &lt;param-name&gt;filterClass&lt;/param-name&gt;
             &lt;param-value&gt;org.jasig.cas.client.authentication.AuthenticationFilter&lt;/param-value&gt;
         &lt;/init-param&gt;
         &lt;init-param&gt;
             &lt;param-name&gt;casServerLoginUrl&lt;/param-name&gt;
             &lt;param-value&gt;https://auth.ala.org.au/cas/login&lt;/param-value&gt;
         &lt;/init-param&gt;
         &lt;init-param&gt;
             &lt;param-name&gt;gateway&lt;/param-name&gt;
             &lt;param-value&gt;true&lt;/param-value&gt;
         &lt;/init-param&gt;
     &lt;/filter&gt;
     ...
 * </pre>
 * @author peter flemming
 */
public class UriFilter implements Filter {

    private final static Logger logger = LoggerFactory.getLogger(UriFilter.class);

    public static final String URI_FILTER_PATTERN = "uriFilterPattern";
    public static final String URI_EXCLUSION_FILTER_PATTERN = "uriExclusionFilterPattern";
    public static final String AUTHENTICATE_ONLY_IF_LOGGED_IN_FILTER_PATTERN = "authenticateOnlyIfLoggedInFilterPattern";

    private Filter filter;
    private String contextPath;
    private List<Pattern> uriInclusionPatterns;
    private List<Pattern> authOnlyIfLoggedInPatterns;
    private List<Pattern> uriExclusionPatterns;
    /** Stores whether or not CAS has been disabled allows for web.xml to exist for the filter but no config  */
    private boolean disabled =false;

    public void init(FilterConfig filterConfig) throws ServletException {
        //check to see if CAS is enabled.
        String disableCAS = filterConfig.getInitParameter("disableCAS");
        if(disableCAS != null && disableCAS.equals("true")){
            logger.info("CAS is disabled.");
            disabled=true;
        } else{
            //
            // Get contextPath parameter
            //
            this.contextPath = filterConfig.getServletContext().getInitParameter("contextPath");
            if (this.contextPath == null) {
                this.contextPath = filterConfig.getServletContext().getContextPath();
                logger.debug("Using ServletContext contextPath: {}", this.contextPath);
            } else {
                logger.warn("Overriding ServletContext contextPath: {} with ServletContext init-param value: {}", filterConfig.getServletContext().getContextPath(), this.contextPath);
            }

            //
            // Get URI inclusion filter patterns
            //
            String includedUrlPattern = filterConfig.getServletContext().getInitParameter(URI_FILTER_PATTERN);
            if (includedUrlPattern == null) {
                includedUrlPattern = "";
            }
            logger.debug("Included URI Pattern = '" + includedUrlPattern + "'");
            this.uriInclusionPatterns = PatternMatchingUtils.getPatternList(contextPath, includedUrlPattern);
    
            //
            // Get URI exclusion filter patterns
            //
            String excludedUrlPattern = filterConfig.getServletContext().getInitParameter(URI_EXCLUSION_FILTER_PATTERN);
            if (excludedUrlPattern == null) {
                excludedUrlPattern = "";
            }
            logger.debug("Excluded URI Pattern = '{}'", excludedUrlPattern);
            this.uriExclusionPatterns = PatternMatchingUtils.getPatternList(contextPath, excludedUrlPattern);
    
            //
            // Get Authenticate Only if Logged in filter patterns
            //
            String authOnlyIfLoggedInPattern = filterConfig.getServletContext().getInitParameter(AUTHENTICATE_ONLY_IF_LOGGED_IN_FILTER_PATTERN);
            if (authOnlyIfLoggedInPattern == null) {
                authOnlyIfLoggedInPattern = "";
            }
            logger.debug("Authenticate Only if Logged in Pattern = '{}'", authOnlyIfLoggedInPattern);
            this.authOnlyIfLoggedInPatterns = PatternMatchingUtils.getPatternList(contextPath, authOnlyIfLoggedInPattern);
    
            //
            // Get target filter class name
            //
            String className = filterConfig.getInitParameter("filterClass");
            try {
                Class<?> c = Class.forName(className);
                filter = (Filter) c.newInstance();
            } catch (Exception e) {
                logger.error("Could not instantiate a new Filter with class {}", className, e);
            }
            filter.init(filterConfig);
        }
    }

    /* (non-Javadoc)
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest, javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        if(!disabled){
            String requestUri = ((HttpServletRequest) request).getRequestURI();
            if (filter instanceof AuthenticationFilter) {
                logger.debug("Request Uri = '{}'", requestUri);
            }

            if (PatternMatchingUtils.matches(requestUri, uriExclusionPatterns)) {
                if (filter instanceof AuthenticationFilter) {
                    logger.debug("Ignoring URI because it matches {}", URI_EXCLUSION_FILTER_PATTERN);
                } else {
                    logger.debug("No action taken as matches {} for {}", URI_EXCLUSION_FILTER_PATTERN, requestUri);
                }
                chain.doFilter(request, response);
            } else if (PatternMatchingUtils.matches(requestUri, uriInclusionPatterns)) {
                if (filter instanceof AuthenticationFilter) {
                    logger.debug("Forwarding URI '{}' to CAS authentication filters because it matches {}", requestUri, URI_FILTER_PATTERN);
                } else {
                    logger.debug("Forwarding URI '{}' to {} filter because it matches {}", requestUri, filter.getClass().getName(), URI_FILTER_PATTERN);
                }
                filter.doFilter(request, response, chain);
            } else if (PatternMatchingUtils.matches(requestUri, authOnlyIfLoggedInPatterns) &&
                        AuthenticationCookieUtils.isUserLoggedIn((HttpServletRequest) request)) {
                if (filter instanceof AuthenticationFilter) {
                    logger.debug("Forwarding URI '{}' to CAS authentication filters because it matches {}  and ALA-Auth cookie exists", requestUri, AUTHENTICATE_ONLY_IF_LOGGED_IN_FILTER_PATTERN);
                } else {
                    logger.debug("Forwarding URI '{}' to {} filter because it matches {} and ALA-Auth cookie exists", requestUri, filter.getClass().getName(), AUTHENTICATE_ONLY_IF_LOGGED_IN_FILTER_PATTERN);
                }
                filter.doFilter(request, response, chain);
            } else {
                logger.debug("No action taken - no matching pattern found for {}", requestUri);
                chain.doFilter(request, response);
            }
        } else{
            //CAS disabled so send it down the chain to perform the remaining filters
            chain.doFilter(request, response);
        }
    }

    public void destroy() {
        filter.destroy();
    }
}
