package au.org.ala.cas.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class AuthenticationCookieUtils {
    
    private final static Logger logger = LoggerFactory.getLogger(AuthenticationCookieUtils.class);

    public static final String DEFAULT_ALA_AUTH_COOKIE_NAME = "ALA-Auth";
    public static final String ALA_AUTH_COOKIE_NAME_PROPERTY = "ala.auth.cookie.name";
    public static final String ALA_AUTH_COOKIE_NAME_ENV = "ALA_AUTH_COOKIE_NAME";

    public static final String ALA_AUTH_COOKIE;

    static {
        ALA_AUTH_COOKIE = firstNotNull(System.getProperty(ALA_AUTH_COOKIE_NAME_PROPERTY), System.getenv(ALA_AUTH_COOKIE_NAME_ENV), DEFAULT_ALA_AUTH_COOKIE_NAME);
    }

    private static String firstNotNull(String... strings) {
        for (String string : strings) {
            if (string != null && !string.trim().equals("")) {
                return string;
            }
        }
        return "";
    }

    @Deprecated
    public static boolean isUserLoggedIn(HttpServletRequest request) {
      return cookieExists(request, ALA_AUTH_COOKIE);
    }

    @Deprecated
    public static String getUserName(HttpServletRequest request) {
        return getCookieValue(request, ALA_AUTH_COOKIE);
    }
    
    public static boolean cookieExists(HttpServletRequest request, String name) {
        return getCookieValue(request, name) != null;
    }
    
    public static String getCookieValue(HttpServletRequest request, String name) {
        String value = null;
        Cookie cookie = getCookie(request, name);
        if (cookie != null) {
            value = cookie.getValue();
        }
        return value;
    }
    
    public static Cookie getCookie(HttpServletRequest request, String name) {

        if (request == null) {
            logger.warn("getCookie(): Request is null!");
            return null;
        }

        Cookie cookie = null;
        Cookie cookies[] = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (c.getName().equals(name)) {
                    cookie = c;
                    break;
                }
            }
        }
        
        if (cookie == null) {
            logger.trace("Cookie {} not found", name);
        } else {
            logger.trace("Cookie {} found", name);
        }
        
        return cookie;
    }
}
