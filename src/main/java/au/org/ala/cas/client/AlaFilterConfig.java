package au.org.ala.cas.client;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;

import org.apache.log4j.Logger;

/**
 * A filterconfig wrapper that allows cas.properties to be provided as
 * configuration to jasig-cas filters.
 * 
 * @author Natasha Carter (natasha.carter@csiro.au)
 * 
 */
public class AlaFilterConfig implements FilterConfig {
    private final static Logger logger = Logger
            .getLogger(AlaFilterConfig.class);
    private FilterConfig embeddedFilterConfig;
    private ServletContext embeddedServletContext;
    private Properties casProperties;


    public AlaFilterConfig(FilterConfig config) {
        this.embeddedFilterConfig = config;
        
        // load the cas.properties file
        loadProperties();
        embeddedServletContext = new
        AlaServletContext(config.getServletContext(), casProperties);
        
        
    }

    private void loadProperties() {
        /*
         * NC 2013-08-21: Config properties file is provided by A JNDI environment variable by the name configPropFile
         * 
         * Sometimes the other properties that are in the properties file can prevent the 
         * authenticator from doing its job correctly. To counteract this we look for a  
         * casProperties value in the properties file to supply a comma separated list of supported
         * properties.
         */
        
        try{
            javax.naming.Context ctx = new javax.naming.InitialContext();
            String filename =(String)ctx.lookup("java:comp/env/configPropFile");
            casProperties = new Properties();
            try {
                Properties p = new Properties();
                p.load(new java.io.FileInputStream(new File(filename)));
                //remove the properties that don't make up the cas configuration
                if(p.containsKey("casProperties")){
                    List<String> whitelist = Arrays.asList(p.getProperty("casProperties").split(","));
                    for(String item: whitelist){
                        Object value = p.getProperty(item);
                        if(value != null){
                            casProperties.put(item, p.getProperty(item));
                        }
                    }
                } else{
                    casProperties = p;
                }
            } catch (Exception e) {
                logger.warn("Unable to load config properties in "
                        + filename, e);
            }
        } catch(Exception e){
            //don't do anything obviously can't find the value
        }        
       
        if (casProperties == null) {
            casProperties = new Properties();
        }
        logger.info("The configProperties " + casProperties);
    }

    public String getFilterName() {
        return embeddedFilterConfig.getFilterName();
    }

    public ServletContext getServletContext() {
        return embeddedServletContext;
        // return embeddedFilterConfig.getServletContext();
    }

    public String getInitParameter(String name) {
        String property = casProperties.getProperty(name);
        String initParam = embeddedFilterConfig.getInitParameter(name);
        if (property == null) {
            logger.debug("No property found for " + name +
                    ", using filter init param value " + initParam + " instead.");
            return initParam;
        } else {
            logger.debug("Preferring property from properties file for " + name + " "
                    + property + " over the filter init param " + initParam);
            return property;
        }
    }

    /**
     * This class acts as a Enumerator. It will enumerate over the
     * cas.properties first and then the embedded filter properties.
     */
    public Enumeration<String> getInitParameterNames() {
        Enumeration<String> propertyKeys = (Enumeration<String>)(Enumeration<?>) casProperties.keys();
        return MultiSourceEnumeration.from(
                propertyKeys,
                embeddedFilterConfig.getInitParameterNames());

    }

    /**
     * An ala wrapper class to include properties file in the params.
     * 
     * @author car61w
     * 
     */
    private class AlaServletContext implements ServletContext {

        private ServletContext embeddedContext;
        private Properties casProperties;


        AlaServletContext(ServletContext context, Properties properties) {
            this.embeddedContext = context;
            this.casProperties = properties;

        }

        public ServletContext getContext(String uripath) {
            return embeddedContext.getContext(uripath);
        }

        public String getContextPath() {
            return embeddedContext.getContextPath();
        }

        public int getMajorVersion() {
            return embeddedContext.getMajorVersion();
        }

        public int getMinorVersion() {
            return embeddedContext.getMinorVersion();
        }

        public int getEffectiveMajorVersion() {
            return embeddedContext.getEffectiveMajorVersion();
        }

        public int getEffectiveMinorVersion() {
            return embeddedContext.getEffectiveMinorVersion();
        }

        public String getMimeType(String file) {
            return embeddedContext.getMimeType(file);
        }

        public Set<String> getResourcePaths(String path) {
            return embeddedContext.getResourcePaths(path);
        }

        public URL getResource(String path) throws MalformedURLException {
            return embeddedContext.getResource(path);
        }

        public InputStream getResourceAsStream(String path) {
            return embeddedContext.getResourceAsStream(path);
        }

        public RequestDispatcher getRequestDispatcher(String path) {
            return embeddedContext.getRequestDispatcher(path);
        }

        public RequestDispatcher getNamedDispatcher(String name) {
            return embeddedContext.getNamedDispatcher(name);
        }

        public Servlet getServlet(String name) throws ServletException {
            return embeddedContext.getServlet(name);
        }

        public Enumeration<Servlet> getServlets() {
            return embeddedContext.getServlets();
        }

        public Enumeration<String> getServletNames() {
            return embeddedContext.getServletNames();
        }

        public void log(String msg) {
            embeddedContext.log(msg);
        }

        public void log(Exception exception, String msg) {
            embeddedContext.log(exception, msg);
        }

        public void log(String message, Throwable throwable) {
            embeddedContext.log(message, throwable);

        }

        public String getRealPath(String path) {
            return embeddedContext.getRealPath(path);
        }

        public String getServerInfo() {
            return embeddedContext.getServerInfo();
        }

        public String getInitParameter(String name) {
            final String property = casProperties.getProperty(name);
            final String initParam = embeddedContext.getInitParameter(name);
            if (property == null) {
                logger.debug("No property found for " + name +
                        ", using servlet context param value " + initParam + " instead.");
                return initParam;
            } else {
                logger.debug("Preferring property from the properties file " + name
                        + " " + property + " over the servlet context param " + initParam);

                return property;
            }
        }

        public Enumeration<String> getInitParameterNames() {
            Enumeration<String> casKeys = (Enumeration<String>)(Enumeration<?>) casProperties.keys();
            return MultiSourceEnumeration.from(casKeys, embeddedContext.getInitParameterNames());
        }

        public boolean setInitParameter(String name, String value) {
            return embeddedContext.setInitParameter(name, value);
        }

        public Object getAttribute(String name) {
            return embeddedContext.getAttribute(name);
        }

        public Enumeration<String> getAttributeNames() {
            return embeddedContext.getAttributeNames();
        }

        public void setAttribute(String name, Object object) {
            embeddedContext.setAttribute(name, object);
        }

        public void removeAttribute(String name) {
            embeddedContext.removeAttribute(name);
        }

        public String getServletContextName() {
            return embeddedContext.getServletContextName();
        }

        public ServletRegistration.Dynamic addServlet(String servletName, String className) {
            return embeddedContext.addServlet(servletName, className);
        }

        public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
            return embeddedContext.addServlet(servletName, servlet);
        }

        public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
            return embeddedContext.addServlet(servletName, servletClass);
        }

        public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
            return embeddedContext.createServlet(clazz);
        }

        public ServletRegistration getServletRegistration(String servletName) {
            return embeddedContext.getServletRegistration(servletName);
        }

        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            return embeddedContext.getServletRegistrations();
        }

        public FilterRegistration.Dynamic addFilter(String filterName, String className) {
            return embeddedContext.addFilter(filterName, className);
        }

        public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
            return embeddedContext.addFilter(filterName, filter);
        }

        public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
            return embeddedContext.addFilter(filterName, filterClass);
        }

        public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
            return embeddedContext.createFilter(clazz);
        }

        public FilterRegistration getFilterRegistration(String filterName) {
            return embeddedContext.getFilterRegistration(filterName);
        }

        public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
            return embeddedContext.getFilterRegistrations();
        }

        public SessionCookieConfig getSessionCookieConfig() {
            return embeddedContext.getSessionCookieConfig();
        }

        public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
            embeddedContext.setSessionTrackingModes(sessionTrackingModes);
        }

        public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
            return embeddedContext.getDefaultSessionTrackingModes();
        }

        public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
            return embeddedContext.getEffectiveSessionTrackingModes();
        }

        public void addListener(String className) {
            embeddedContext.addListener(className);
        }

        public <T extends EventListener> void addListener(T t) {
            embeddedContext.addListener(t);
        }

        public void addListener(Class<? extends EventListener> listenerClass) {
            embeddedContext.addListener(listenerClass);
        }

        public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
            return embeddedContext.createListener(clazz);
        }

        public JspConfigDescriptor getJspConfigDescriptor() {
            return embeddedContext.getJspConfigDescriptor();
        }

        public ClassLoader getClassLoader() {
            return embeddedContext.getClassLoader();
        }

        public void declareRoles(String... roleNames) {
            embeddedContext.declareRoles(roleNames);
        }

    }

    /**
     * Enumerates of multiple source in the order specified in the sources
     * array.
     * 
     * @author Natasha Carter (natasha.carter@csiro.au)
     * 
     */
    public static class MultiSourceEnumeration<T> implements Enumeration<T> {
        Enumeration<T>[] sources;

        MultiSourceEnumeration(Enumeration<T>... sources) {
            this.sources = sources;
        }

        /**
         * @return true when at least one of the enumerators has more elements
         */
        public boolean hasMoreElements() {
            for (Enumeration<T> source : sources) {
                if (source.hasMoreElements()) {
                    return true;
                }
            }
            return false;
        }

        /**
         * @return the next element in the collection. It will go through the
         *          enumerators in the order that they are supplied.
         */
        public T nextElement() {
            for (Enumeration<T> source : sources) {
                if (source.hasMoreElements()) {
                    return source.nextElement();
                }
            }
            return null;
        }

        static <T> Enumeration<T> from(Enumeration<T>... enumerations) {
            return new MultiSourceEnumeration<T>(enumerations);
        }
    }
}
