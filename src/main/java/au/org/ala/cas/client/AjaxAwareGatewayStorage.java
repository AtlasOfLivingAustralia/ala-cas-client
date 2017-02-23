package au.org.ala.cas.client;

import org.jasig.cas.client.authentication.DefaultGatewayResolverImpl;
import org.jasig.cas.client.authentication.GatewayResolver;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * The AjaxAwareGatewayStorage class delegates to a CAS DefaultGatewayResolverImpl with one difference:
 * if the request is determined to be an ajax request, the next call to hasGatewayedAlready() will return false as the
 * browser will not be follow the redirect to CAS due to domain origin restrictions.
 * Note that the mechanism used to detect the ajax request relies on the default jquery behaviour of setting the
 * X-Requested-With header.
 */
public class AjaxAwareGatewayStorage implements GatewayResolver {

    public static final String CONST_CAS_GATEWAY = "_const_cas_gateway_";

    private final DefaultGatewayResolverImpl delegate = new DefaultGatewayResolverImpl();

    public boolean hasGatewayedAlready(final HttpServletRequest request,
                                       final String serviceUrl) {
        return delegate.hasGatewayedAlready(request, serviceUrl);
    }

    public String storeGatewayInformation(final HttpServletRequest request,
                                          final String serviceUrl) {
        if (!isAjax(request)) {
            return delegate.storeGatewayInformation(request, serviceUrl);
        }
        return serviceUrl;
    }

    /**
     * Checks the X-Requested-With header for XMLHttpRequest (which is set by jquery).
     * @param request the request to check.
     * @return true if the request was initiated via ajax.
     */
    private boolean isAjax(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

}
