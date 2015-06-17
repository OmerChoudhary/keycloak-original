package org.keycloak.adapters.springsecurity.facade;

import org.keycloak.adapters.HttpFacade.Cookie;
import org.keycloak.adapters.HttpFacade.Request;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Concrete Keycloak {@link Request request} implementation wrapping an {@link HttpServletRequest}.
 *
 * @author <a href="mailto:srossillo@smartling.com">Scott Rossillo</a>
 * @version $Revision: 1 $
 */
class WrappedHttpServletRequest implements Request {

    private final HttpServletRequest request;

    /**
     * Creates a new request for the given <code>HttpServletRequest</code>
     *
     * @param request the current <code>HttpServletRequest</code> (required)
     */
    public WrappedHttpServletRequest(HttpServletRequest request) {
        Assert.notNull(request, "HttpServletRequest required");
        this.request = request;
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getURI() {
        StringBuffer buf = request.getRequestURL();
        if (request.getQueryString() != null) {
            buf.append('?').append(request.getQueryString());
        }
        return buf.toString();
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
    }

    @Override
    public String getQueryParamValue(String param) {
        return request.getParameter(param);
    }

    @Override
    public Cookie getCookie(String cookieName) {

        javax.servlet.http.Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (javax.servlet.http.Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                return new Cookie(cookie.getName(), cookie.getValue(), cookie.getVersion(), cookie.getDomain(), cookie.getPath());
            }
        }

        return null;
    }

    @Override
    public String getHeader(String name) {
        return request.getHeader(name);
    }

    @Override
    public List<String> getHeaders(String name) {
        Enumeration<String> values = request.getHeaders(name);
        List<String> array = new ArrayList<String>();

        while (values.hasMoreElements()) {
            array.add(values.nextElement());
        }

        return Collections.unmodifiableList(array);
    }

    @Override
    public InputStream getInputStream() {
        try {
            return request.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Unable to get request input stream", e);
        }
    }

    @Override
    public String getRemoteAddr() {
        return request.getRemoteAddr();
    }
}
