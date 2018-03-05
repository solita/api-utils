package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMapOfSize;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.FunctionalA.concat;
import static fi.solita.utils.functional.FunctionalC.drop;
import static fi.solita.utils.functional.FunctionalC.span;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.util.UriUtils;

import fi.solita.utils.api.RequestUtil.ETags;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class ResponseUtil {
    
    private static final DateTime started = DateTime.now();
    
    public static void respond(HttpServletResponse response, int status, String responseText, Pair<String,String>... headers) throws IOException {
        response.setStatus(status);
        for (Pair<String,String> header: headers) {
            response.setHeader(header.left, header.right);
        }
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.getWriter().write(responseText);
    }

    public static void respondWithEternalCaching(HttpServletResponse response, byte[] data, ETags etags) {
        respond(response, data, etags, true);
    }
    
    public static void respond(HttpServletResponse response, byte[] data, ETags etags) {
        respond(response, data, etags, false);
    }
    
    @SuppressWarnings("unchecked")
    private static void respond(HttpServletResponse response, byte[] data, ETags etags, boolean cacheOKForInfinity) {
        try {
            String etag = calculateETag(data);
            if (etags.ifMatch.isDefined() && !etags.ifMatch.get().contains(etag)) {
                // "14.24 If-Match: If none of the entity tags match ... MUST return a 412"
                respondError(response, HttpStatus.PRECONDITION_FAILED);
            } else if (etags.ifNoneMatch.isDefined() && (etags.ifNoneMatch.get().contains("*") || etags.ifNoneMatch.get().contains(etag))) {
                // "14.26 If-None-Match: If any of the entity tags match ... or if "*" is given and any current entity exists for that resource ... the server SHOULD respond with a 304"
                respondError(response, HttpStatus.NOT_MODIFIED, Pair.of(HttpHeaders.ETAG, etag));
            } else {
                if (cacheOKForInfinity) {
                    cacheForInfinity(response);
                }
                response.setStatus(HttpStatus.OK.value());
                response.setHeader(HttpHeaders.ETAG, etag);
                response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                response.setHeader(HttpHeaders.CONTENT_LENGTH, Integer.toString(data.length));
                
                ServletOutputStream os = response.getOutputStream();
                try {
                    os.write(data);
                } finally {
                    os.close();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static String calculateETag(byte[] data) {
        return "W/\"" + DigestUtils.md5Hex(data) + "\"";
    }
    
    public static final void cacheForInfinity(HttpServletResponse response) {
        cacheFor(Duration.standardDays(360), response);
    }

    public static final void setLastModifiedToAppStartupTime(HttpServletResponse response) {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        response.setDateHeader(HttpHeaders.DATE, now.getMillis());
        response.setDateHeader(HttpHeaders.LAST_MODIFIED, started.getMillis());
    }

    /**
     * No-op if cache-control header is already set
     */
    public static final void cacheFor(Duration age, HttpServletResponse response) {
        if (age.isLongerThan(Duration.ZERO) && !response.containsHeader(HttpHeaders.CACHE_CONTROL)) {
            DateTime now = DateTime.now(DateTimeZone.UTC);
            response.setDateHeader(HttpHeaders.DATE, now.getMillis());
            response.setDateHeader(HttpHeaders.EXPIRES, now.plus(age).getMillis());
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + age.getStandardSeconds());
            response.setHeader(HttpHeaders.PRAGMA, "cache");
        }
    }
   
    public static void respondError(HttpServletResponse response, HttpStatus status, Pair<String,String>... headers) throws IOException {
        for (Pair<String,String> header: headers) {
            response.setHeader(header.left, header.right);
        }
        respondError(response, status.value(), status.getReasonPhrase());
    }
    
    public static void respondError(HttpServletResponse response, int status, String errorMsg) throws IOException {
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.sendError(status, errorMsg);
    }

    public static void respondLatest(Class<?> latestVersion, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String version = RequestUtil.resolvePath(latestVersion);
        String path = RequestUtil.getAPIVersionRelativePath(request);
        redirect307(version + path, request, response);
    }
    
    public static void redirectToRevision(long revision, HttpServletRequest request, HttpServletResponse response) {
        Pair<String,String> path = span(not(equalTo('/')), drop(1, RequestUtil.getContextRelativePath(request)));
        // path == <Whole API path>
        // path.left == <API version>
        // path.right == <remaining API path, if any?>
        redirect307(path.left + "/" + revision + path.right, request, response);
    }
    
    public static void redirectToAnotherRevision(long revision, HttpServletRequest request, HttpServletResponse response) {
        Pair<String,String> path = span(not(equalTo('/')), drop(1, RequestUtil.getContextRelativePath(request)));
        Pair<String, String> revisionAndRemainingPath = span(not(equalTo('/')), drop(1, path.right));
        // path == <Whole API path>
        // path.left == <API version>
        // path.right == 
        // revisionAndRemainingPath.left == <old revision>
        // revisionAndRemainingPath.right == <remaining API path, if any?>
        redirect307(path.left + "/" + revision + revisionAndRemainingPath.right, request, response);
    }
    
    public static void redirect307(String contextRelativePath, HttpServletRequest request, HttpServletResponse response) {
        redirect307(contextRelativePath, request, response, Collections.<String,String>emptyMap());
    }
    
    @SuppressWarnings("unchecked")
    public static void redirect307(String contextRelativePath, HttpServletRequest request, HttpServletResponse response, Map<String,String> additionalUnescapedQueryParams) {
        String path = request.getContextPath() +
                      (contextRelativePath.startsWith("/") ? contextRelativePath : "/" + contextRelativePath);
        Iterable<String> params = map(Transformers.join("=").andThen(ResponseUtil_.encodeUrlQueryString), additionalUnescapedQueryParams.entrySet());
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            params = sort(concat(request.getQueryString().split("&"), params));
        }
        String query = mkString("&", params);
        String uri = response.encodeRedirectURL(encodeUrlPath(path) + (query.isEmpty() ? "" : "?" + query));
        try {
            respond(response, HttpServletResponse.SC_TEMPORARY_REDIRECT, "<a href='" + uri + "'>" + path + "</a>", Pair.of("Location", uri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    static String encodeUrlPath(String component) {
        try {
            return UriUtils.encodePath(component, "UTF-8").replace("'", "%27");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
    
    static String encodeUrlQueryString(String component) {
        try {
            return UriUtils.encodeQuery(component, "UTF-8").replace("'", "%27");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
