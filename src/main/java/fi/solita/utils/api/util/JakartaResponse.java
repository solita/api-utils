package fi.solita.utils.api.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

import jakarta.servlet.http.HttpServletResponse;

import fi.solita.utils.api.util.ResponseUtil.Response;

public abstract class JakartaResponse {

    public static Response of(HttpServletResponse resp) {
        return new Response() {
            @Override
            public void setStatus(int sc) {
                resp.setStatus(sc);
            }

            @Override
            public void setHeader(String name, String value) {
                resp.setHeader(name, value);
            }
            
            @Override
            public void addHeader(String name, String value) {
                resp.addHeader(name, value);
            }

            @Override
            public void setDateHeader(String name, long date) {
                resp.setDateHeader(name, date);
            }

            @Override
            public boolean containsHeader(String name) {
                return resp.containsHeader(name);
            }

            @Override
            public PrintWriter getWriter() throws IOException {
                return resp.getWriter();
            }

            @Override
            public OutputStream getOutputStream() throws IOException {
                return resp.getOutputStream();
            }

            @Override
            public String encodeRedirectURL(String url) {
                return resp.encodeRedirectURL(url);
            }

            @Override
            public void sendError(int sc, String msg) throws IOException {
                resp.sendError(sc, msg);
            }
            
            @Override
            public void setContentType(String type) {
                resp.setContentType(type);
            }
        };
    }
}
