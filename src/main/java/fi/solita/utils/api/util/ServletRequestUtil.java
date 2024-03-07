package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Transformers.prepend;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
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
import javax.servlet.http.HttpServletRequest;

public abstract class ServletRequestUtil {
    public static final ETags getETags(HttpServletRequest request) {
        return new ETags(RequestUtil.parseETags(Option.of(request.getHeader("If-Match"))),
                RequestUtil.parseETags(Option.of(request.getHeader("If-None-Match"))));
    }
    
    
    public static final void checkURL(HttpServletRequest request, Set<String> caseIgnoredParams, String... acceptedParams) throws IllegalQueryParametersException, QueryParametersMustNotBeDuplicatedException, QueryParametersMustBeInAlphabeticalOrderException {
        RequestUtil.assertAcceptHeader(newList(request.getHeaders("Accept")));
        RequestUtil.assertQueryStringValid(request.getParameterMap(), newList(request.getParameterNames()), caseIgnoredParams, acceptedParams);
    }
    
    public static final String getContextPath(HttpServletRequest req) {
        return RequestUtil.getContextPath(req.getContextPath(), Option.of(req.getHeader("X-Forwarded-Prefix")));
    }
    
    public static final URI getRequestURI(HttpServletRequest req) {
        Option<String> qs = req.getQueryString() == null || req.getQueryString().trim().length() == 0 ? Option.<String>None() : Some(req.getQueryString());
        Option<String> forwardedProto = Option.of(req.getHeader("X-Forwarded-Proto"));
        Option<String> forwardedHost = Option.of(req.getHeader("X-Forwarded-Host"));
        Option<String> forwardedPrefix = Option.of(req.getHeader("X-Forwarded-Prefix"));
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
    
    public static final String getContextRelativePath(HttpServletRequest req) {
        return RequestUtil.getContextRelativePath(req.getServletPath(), Option.of(req.getPathInfo()));
    }
    
    public static final String getAPIVersionRelativePath(HttpServletRequest req) {
        return RequestUtil.getAPIVersionRelativePath(getContextRelativePath(req));
    }
    
    public static final String getAPIVersionRelativePathWithoutRevision(HttpServletRequest req) {
        return RequestUtil.getAPIVersionRelativePathWithoutRevision(getAPIVersionRelativePath(req));
    }
    
    public static final String getApiVersionBasePath(HttpServletRequest req) {
        return RequestUtil.getApiVersionBasePath(getContextPath(req), getContextRelativePath(req));
    }
    
    public static final Either<Option<String>,SerializationFormat> resolveFormat(HttpServletRequest request) throws NotFoundException {
        for (String extension: RequestUtil.resolveExtension(getContextRelativePath(request))) {
            for (SerializationFormat ext: SerializationFormat.valueOfExtension(extension)) {
                return Either.right(ext);
            }
            return Either.left(Some(extension));
        }
        return Either.left(Option.<String>None());
    }
    
    public static final byte[] uncompressIfNeeded(HttpServletRequest req, byte[] data) throws IOException {
        if (Option.of(req.getHeader("Content-Encoding")).getOrElse("").contains("gzip")) {
            return IOUtils.toByteArray(new GZIPInputStream(new ByteArrayInputStream(data)));
        }
        return data;
    }
}
