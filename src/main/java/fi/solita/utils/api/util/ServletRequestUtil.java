package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.it;
import static fi.solita.utils.functional.Collections.newList;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.util.RequestUtil.ETags;
import fi.solita.utils.api.util.RequestUtil.IllegalQueryParametersException;
import fi.solita.utils.api.util.RequestUtil.QueryParametersMustBeInAlphabeticalOrderException;
import fi.solita.utils.api.util.RequestUtil.QueryParametersMustNotBeDuplicatedException;
import fi.solita.utils.functional.Either;
import java.util.Optional;

public abstract class ServletRequestUtil {
    public static interface Request {
        public Object getHttpServletRequest();
        public String getHeader(String name);
        public Enumeration<String> getHeaders(String name); 
        public Map<String, String[]> getParameterMap();
        public String getServletPath();
        public String getPathInfo();
        public String getContextPath();
        public String getQueryString();
        public StringBuffer getRequestURL();
        public Enumeration<String> getParameterNames();
    }
    
    public static final ETags getETags(Request request) {
        return new ETags(RequestUtil.parseETags(Optional.ofNullable(request.getHeader(Headers.IF_MATCH))),
                RequestUtil.parseETags(Optional.ofNullable(request.getHeader(Headers.IF_NONE_MATCH))));
    }
    
    
    public static final void checkURL(Request request, Set<String> caseIgnoredParams, String... acceptedParams) throws IllegalQueryParametersException, QueryParametersMustNotBeDuplicatedException, QueryParametersMustBeInAlphabeticalOrderException {
        RequestUtil.assertAcceptHeader(newList(request.getHeaders(Headers.ACCEPT)));
        RequestUtil.assertQueryStringValid(request.getParameterMap(), newList(request.getParameterNames()), caseIgnoredParams, acceptedParams);
    }
    
    public static final String getContextPath(Request req) {
        return RequestUtil.getContextPath(req.getContextPath(), Optional.ofNullable(req.getHeader(Headers.X_FORWARDED_PREFIX)));
    }
    
    public static final URI getRequestURI(Request req) {
        Optional<String> qs = req.getQueryString() == null || req.getQueryString().trim().length() == 0 ? Optional.empty() : Optional.of(req.getQueryString());
        Optional<String> forwardedProto = Optional.ofNullable(req.getHeader(Headers.X_FORWARDED_PROTO));
        Optional<String> forwardedHost = Optional.ofNullable(req.getHeader(Headers.X_FORWARDED_HOST));
        Optional<String> forwardedPrefix = Optional.ofNullable(req.getHeader(Headers.X_FORWARDED_PREFIX));
        String url = req.getRequestURL().toString();
        for (String proto: it(forwardedProto)) {
            url = url.replaceFirst("[^:]+://", proto + "://");
        }
        for (String host: it(forwardedHost)) {
            url = url.replaceFirst("://[^:/]*", "://" + host);
        }
        for (String prefix: it(forwardedPrefix)) {
            url = url.replaceFirst("^[^:/]+://[^/]+", "$0" + prefix);
        }
        return URI.create(url + qs.map(x -> "?" + x).orElse(""));
    }
    
    public static final String getContextRelativePath(Request req) {
        return RequestUtil.getContextRelativePath(req.getServletPath(), Optional.ofNullable(req.getPathInfo()));
    }
    
    public static final String getAPIVersionRelativePath(Request req) {
        return RequestUtil.getAPIVersionRelativePath(getContextRelativePath(req));
    }
    
    public static final String getAPIVersionRelativePathWithoutRevision(Request req) {
        return RequestUtil.getAPIVersionRelativePathWithoutRevision(getAPIVersionRelativePath(req));
    }
    
    public static final String getApiVersionBasePath(Request req) {
        return RequestUtil.getApiVersionBasePath(getContextPath(req), getContextRelativePath(req));
    }
    
    public static final Either<Optional<String>,SerializationFormat> resolveFormat(Request request) throws NotFoundException {
        for (String extension: it(RequestUtil.resolveExtension(getContextRelativePath(request)))) {
            for (SerializationFormat ext: it(SerializationFormat.valueOfExtension(extension))) {
                return Either.right(ext);
            }
            return Either.left(Optional.of(extension));
        }
        return Either.left(Optional.empty());
    }
    
    public static final byte[] uncompressIfNeeded(Request req, byte[] data) throws IOException {
        if (Optional.ofNullable(req.getHeader(Headers.CONTENT_ENCODING)).orElse("").contains("gzip")) {
            GZIPInputStream is = new GZIPInputStream(new ByteArrayInputStream(data));
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] d = new byte[4096];
            while ((nRead = is.read(d, 0, d.length)) != -1) {
                buffer.write(d, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
        return data;
    }
}
