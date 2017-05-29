package au.org.ala.cas.util;

import org.apache.log4j.Logger;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.util.CommonUtils;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

/**
 * Helper methods that simplify getting key User Principal attributes from a Web Request
 */
public class AuthenticationUtils {

    private final static Logger logger = Logger.getLogger(AuthenticationUtils.class);

    // Attribute keys for pulling out values from the AttributePrincipal
    private static final String ATTR_USER_ID = "userid";
    private static final String ATTR_EMAIL_ADDRESS = "email";
    private static final String ATTR_FIRST_NAME = "firstname";
    private static final String ATTR_LAST_NAME = "lastname";
    private static final String ATTR_ROLES = "authority";

    /**
     * @param request Needs to be a {@link au.org.ala.cas.client.AlaHttpServletRequestWrapperFilter}
     * @return The numeric user id of the currently authenticated user, or null if not authenticated
     */
    public static String getUserId(final HttpServletRequest request) {
        return getPrincipalAttribute(request, ATTR_USER_ID);
    }

    /**
     * @param request Needs to be a {@link au.org.ala.cas.client.AlaHttpServletRequestWrapperFilter}
     * @return The email address of the currently authenticated user, or null if not authenticated
     */
    public static String getEmailAddress(final HttpServletRequest request) {
        String email = getPrincipalAttribute(request, ATTR_EMAIL_ADDRESS);
        if (email == null) {
            logger.debug(String.format("Unable to retrieve email from User Principal. Looking in ALA cookie."));
            email = AuthenticationCookieUtils.getUserName(request);
        }
        return email;
    }

    /**
     * @param request Needs to be a {@link au.org.ala.cas.client.AlaHttpServletRequestWrapperFilter}
     * @return The users display name (suitable for display in user interfaces), or null if not authenticated
     */
    public static String getDisplayName(final HttpServletRequest request) {
        String firstname = getPrincipalAttribute(request, ATTR_FIRST_NAME);
        String lastname = getPrincipalAttribute(request, ATTR_LAST_NAME);
        String displayName = null;
        if (CommonUtils.isNotBlank(firstname) && CommonUtils.isNotBlank(lastname)) {
            displayName = String.format("%s %s", firstname, lastname);
        } else if (CommonUtils.isNotBlank(firstname) || CommonUtils.isNotBlank(lastname)) {
            displayName = String.format("%s", CommonUtils.isNotBlank(firstname) ? firstname : lastname);
        }
        return displayName;
    }

    /**
     * Tests to see if the currently authenticated user has the specified role
     *
     * @param request Needs to be a {@link au.org.ala.cas.client.AlaHttpServletRequestWrapperFilter}
     * @param role    The name of the role to test. E.g. ALA_ADMIN
     * @return True if the user has the specified role. False if not, or not authenticated
     */
    public static boolean isUserInRole(final HttpServletRequest request, final String role) {

        if (CommonUtils.isBlank(role)) {
            return false;
        }

        String roles = getPrincipalAttribute(request, ATTR_ROLES);
        if (CommonUtils.isNotBlank(roles)) {
            for (String roleValue : roles.split(",")) {
                if (role.equalsIgnoreCase(roleValue.trim())) {
                    return true;
                }
            }
        } else {
            return false;
        }

        return false;
    }

    /**
     * Helper method that extracts the value of a specified attribute value from a {@link org.jasig.cas.client.authentication.AttributePrincipal}
     *
     * @param request      Needs to be a {@link au.org.ala.cas.client.AlaHttpServletRequestWrapperFilter}
     * @param attributeKey The name of the attribute to retrieve
     * @return The value of the specified attribute, or null
     */
    private static String getPrincipalAttribute(final HttpServletRequest request, final String attributeKey) {

        if (request == null) {
            logger.debug(String.format("Request is null! (Looking for attribute %s)", attributeKey));
            return null;
        }

        Principal principal = request.getUserPrincipal();
        if (principal != null && principal instanceof AttributePrincipal) {
            AttributePrincipal attrPrincipal = (AttributePrincipal) principal;
            Object attrValue = attrPrincipal.getAttributes().get(attributeKey);
            logger.debug(String.format("getPrincipalAttribute(%s) = %s", attributeKey, attrValue == null ? "null" : attrValue.toString()));
            return attrValue == null ? null : attrValue.toString();
        }

        return null;
    }

    public static boolean isUserLoggedIn(HttpServletRequest request) {
        //as described in org.jasig.cas.client.authentication.AuthenticationFilter
        return request != null && request.getSession(false) != null &&
                request.getSession(false).getAttribute("_const_cas_assertion_") != null;
    }
}
