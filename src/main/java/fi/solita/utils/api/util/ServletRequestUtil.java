package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Transformers.prepend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.util.RequestUtil.ETags;
import fi.solita.utils.api.util.RequestUtil.IllegalQueryParametersException;
import fi.solita.utils.api.util.RequestUtil.QueryParametersMustBeInAlphabeticalOrderException;
import fi.solita.utils.api.util.RequestUtil.QueryParametersMustNotBeDuplicatedException;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;

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
        return new ETags(RequestUtil.parseETags(Option.of(request.getHeader(Headers.IF_MATCH))),
                RequestUtil.parseETags(Option.of(request.getHeader(Headers.IF_NONE_MATCH))));
    }
    
    
    public static final void checkURL(Request request, Set<String> caseIgnoredParams, String... acceptedParams) throws IllegalQueryParametersException, QueryParametersMustNotBeDuplicatedException, QueryParametersMustBeInAlphabeticalOrderException {
        RequestUtil.assertAcceptHeader(newList(request.getHeaders(Headers.ACCEPT)));
        RequestUtil.assertQueryStringValid(request.getParameterMap(), newList(request.getParameterNames()), caseIgnoredParams, acceptedParams);
    }
    
    public static final String getContextPath(Request req) {
        return RequestUtil.getContextPath(req.getContextPath(), Option.of(req.getHeader(Headers.X_FORWARDED_PREFIX)));
    }
    
    public static final URI getRequestURI(Request req) {
        Option<String> qs = req.getQueryString() == null || req.getQueryString().trim().length() == 0 ? Option.<String>None() : Some(req.getQueryString());
        Option<String> forwardedProto = Option.of(req.getHeader(Headers.X_FORWARDED_PROTO));
        Option<String> forwardedHost = Option.of(req.getHeader(Headers.X_FORWARDED_HOST));
        Option<String> forwardedPrefix = Option.of(req.getHeader(Headers.X_FORWARDED_PREFIX));
        String url = req.getRequestURL().toString();
        for (String proto: forwardedProto) {
            url = url.replaceFirst("[^:]+://", proto + "://");
        }
        for (String host: forwardedHost) {
            url = url.replaceFirst("://[^:/]*", "://" + host);
        }
        for (String prefix: forwardedPrefix) {
            url = url.replaceFirst("^[^:/]+://[^/]+", "$0" + prefix);
        }
        return URI.create(url + qs.map(prepend("?")).getOrElse(""));
    }
    
    public static final String getContextRelativePath(Request req) {
        return RequestUtil.getContextRelativePath(req.getServletPath(), Option.of(req.getPathInfo()));
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
    
    public static final Either<Option<String>,SerializationFormat> resolveFormat(Request request) throws NotFoundException {
        for (String extension: RequestUtil.resolveExtension(getContextRelativePath(request))) {
            for (SerializationFormat ext: SerializationFormat.valueOfExtension(extension)) {
                return Either.right(ext);
            }
            return Either.left(Some(extension));
        }
        return Either.left(Option.<String>None());
    }
    
    public static final byte[] uncompressIfNeeded(Request req, byte[] data) throws IOException {
        if (Option.of(req.getHeader(Headers.CONTENT_ENCODING)).getOrElse("").contains("gzip")) {
            return IOUtils.toByteArray(new GZIPInputStream(new ByteArrayInputStream(data)));
        }
        return data;
    }
}
