package fi.solita.utils.api.util;

import java.util.Enumeration;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import fi.solita.utils.api.util.ServletRequestUtil.Request;

public abstract class JakartaRequest {

    public static Request of(HttpServletRequest req) {
        return new Request() {
            @Override
            public Object getHttpServletRequest() {
                return req;
            }
            @Override
            public String getHeader(String name) {
                return req.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                return req.getHeaders(name);
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return req.getParameterMap();
            }

            @Override
            public String getServletPath() {
                return req.getServletPath();
            }

            @Override
            public String getPathInfo() {
                return req.getPathInfo();
            }

            @Override
            public String getContextPath() {
                return req.getContextPath();
            }

            @Override
            public String getQueryString() {
                return req.getQueryString();
            }

            @Override
            public StringBuffer getRequestURL() {
                return req.getRequestURL();
            }

            @Override
            public Enumeration<String> getParameterNames() {
                return req.getParameterNames();
            }
        };
    }
}
