package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.FunctionalA.filter;
import static fi.solita.utils.functional.FunctionalC.drop;
import static fi.solita.utils.functional.FunctionalC.span;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.digest.DigestUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.springframework.web.util.UriUtils;

import fi.solita.utils.api.util.RequestUtil.ETags;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Transformers;

public abstract class ResponseUtil {
    
    // must be 365 days to get 31536000. Otherwise at least safari might refuse to cache content.
    public static final Duration ETERNAL_CACHE_DURATION = Duration.standardDays(365);

    public static interface Response {
        public void setStatus(int sc);
        public void setHeader(String name, String value);
        public void addHeader(String name, String value);
        public void setDateHeader(String name, long date);
        public boolean containsHeader(String name);
        public PrintWriter getWriter() throws IOException;
        public OutputStream getOutputStream() throws IOException;
        public String encodeRedirectURL(String url);
        public void sendError(int sc, String msg) throws IOException;
        public void setContentType(String type);
    }
    
    private static final DateTime started = DateTime.now();
    
    @SafeVarargs
    public static void respond(Response response, int status, String responseText, Pair<String,String>... headers) throws IOException {
        response.setStatus(status);
        for (Pair<String,String> header: headers) {
            response.setHeader(header.left(), header.right());
        }
        setAccessControlHeaders(response);
        response.getWriter().write(responseText);
    }

    public static void setAccessControlHeaders(Response response) {
        response.setHeader(Headers.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
    }

    public static void respondOKWithEternalCaching(Response response, byte[] data, ETags etags) {
        respond(response, data, etags, 200, true);
    }
    
    public static void respondOK(Response response, byte[] data, ETags etags) {
        respond(response, data, etags, 200, false);
    }
    
    public static void respond(Response response, byte[] data, ETags etags, int status) {
        respond(response, data, etags, status, false);
    }
    
    private static void respond(Response response, byte[] data, ETags etags, int status, boolean cacheOKForInfinity) {
        try {
            String etag = calculateETag(data);
            if (etags.ifMatch.isDefined() && !etags.ifMatch.get().contains(etag)) {
                // "14.24 If-Match: If none of the entity tags match ... MUST return a 412"
                respondError(response, 412, "Precondition Failed");
            } else if (etags.ifNoneMatch.isDefined() && (etags.ifNoneMatch.get().contains("*") || etags.ifNoneMatch.get().contains(etag))) {
                // "14.26 If-None-Match: If any of the entity tags match ... or if "*" is given and any current entity exists for that resource ... the server SHOULD respond with a 304"
                respondError(response, 304, "Not Modified", Pair.of(Headers.ETAG, etag));
            } else {
                if (cacheOKForInfinity) {
                    cacheForInfinity(response);
                }
                response.setStatus(status);
                response.setHeader(Headers.ETAG, etag);
                setAccessControlHeaders(response);
                response.setHeader(Headers.CONTENT_LENGTH, Integer.toString(data.length));
                
                OutputStream os = response.getOutputStream();
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
    
    public static final void cacheForInfinity(Response response) {
        cacheFor(ETERNAL_CACHE_DURATION, response);
    }
    
    public static final void disableCaching(Response response) {
        response.setHeader(Headers.CACHE_CONTROL, "private, no-store, no-cache, must-revalidate");
    }

    public static final void setLastModifiedToAppStartupTime(Response response) {
        DateTime now = DateTime.now(DateTimeZone.UTC);
        response.setDateHeader(Headers.DATE, now.getMillis());
        response.setDateHeader(Headers.LAST_MODIFIED, started.getMillis());
    }

    /**
     * No-op if cache-control header is already set
     */
    public static final void cacheFor(Duration age, Response response) {
        if (age.isLongerThan(Duration.ZERO) && !response.containsHeader(Headers.CACHE_CONTROL)) {
            DateTime now = DateTime.now(DateTimeZone.UTC);
            response.setDateHeader(Headers.DATE, now.getMillis());
            response.setDateHeader(Headers.EXPIRES, now.plus(age).getMillis());
            response.setHeader(Headers.CACHE_CONTROL, "public, max-age=" + age.getStandardSeconds());
            response.setHeader(Headers.PRAGMA, "cache");
            response.addHeader(Headers.VARY, Headers.ACCEPT_ENCODING); // cache different compressions separately 
            response.addHeader(Headers.VARY, Headers.CACHE_CONTEXT); // this custom header can be used to split a cache by a known key 
        }
    }
   
    @SafeVarargs
    public static void respondError(Response response, int status, String errorMsg, Pair<String,String>... headers) throws IOException {
        for (Pair<String,String> header: headers) {
            response.setHeader(header.left(), header.right());
        }
        setAccessControlHeaders(response);
        response.sendError(status, errorMsg);
    }

    public static void respondLatest(Class<?> latestVersion, Request request, Response response) throws IOException {
        String version = SpringRequestUtil.resolvePath(latestVersion);
        String path = ServletRequestUtil.getAPIVersionRelativePath(request);
        redirect307(version + path, request, response);
    }
    
    public static void redirectToRevision(long revision, Request request, Response response) {
        Pair<String,String> path = span(not(equalTo('/')), drop(1, ServletRequestUtil.getContextRelativePath(request)));
        // path == <Whole API path>
        // path.left == <API version>
        // path.right == <remaining API path, if any?>
        redirect307(path.left() + "/" + revision + path.right(), request, response);
    }
    
    public static void redirectToRevisionAndDateTime(Request req, Response res, long revision, DateTime dateTime, Set<String> queryParamsToExclude) {
        Pair<String,String> path = span(not(equalTo('/')), drop(1, ServletRequestUtil.getContextRelativePath(req)));
        redirect307(path.left() + "/" + revision + path.right(), req, res, newMap(Pair.of("time", RequestUtil.instant2string(dateTime))), queryParamsToExclude);
    }

    public static void redirectToRevisionAndInterval(Request req, Response res, long revision, Interval interval, Set<String> queryParamsToExclude) {
        Pair<String,String> path = span(not(equalTo('/')), drop(1, ServletRequestUtil.getContextRelativePath(req)));
        redirect307(path.left() + "/" + revision + path.right(), req, res, newMap(Pair.of("time", RequestUtil.interval2stringRestrictedToInfinity(interval))), queryParamsToExclude);
    }
    
    public static void redirectToAnotherRevision(long revision, Request request, Response response) {
        Pair<String,String> path = span(not(equalTo('/')), drop(1, ServletRequestUtil.getContextRelativePath(request)));
        Pair<String, String> revisionAndRemainingPath = span(not(equalTo('/')), drop(1, path.right()));
        // path == <Whole API path>
        // path.left == <API version>
        // path.right == 
        // revisionAndRemainingPath.left == <old revision>
        // revisionAndRemainingPath.right == <remaining API path, if any?>
        redirect307(path.left() + "/" + revision + revisionAndRemainingPath.right(), request, response);
    }
    
    public static void redirect307(String contextRelativePath, Request request, Response response) {
        redirect307(contextRelativePath, request, response, Collections.<String,String>emptyMap());
    }
    
    public static void redirect307(String contextRelativePath, Request request, Response response, Map<String,String> additionalUnescapedQueryParams) {
        redirect307(contextRelativePath, request, response, additionalUnescapedQueryParams, Collections.<String>emptySet());
    }
    
    public static void redirect307(String contextRelativePath, Request request, Response response, Map<String,String> additionalUnescapedQueryParams, Set<String> queryParamsToExclude) {
        String path = ServletRequestUtil.getContextPath(request) +
                      (contextRelativePath.startsWith("/") ? contextRelativePath : "/" + contextRelativePath);
        Iterable<String> params = map(Transformers.join("=").andThen(ResponseUtil_.encodeUrlQueryString), additionalUnescapedQueryParams.entrySet());
        if (request.getQueryString() != null && !request.getQueryString().isEmpty()) {
            params = sort(concat(filter(ResponseUtil_.acceptParam.ap(queryParamsToExclude), request.getQueryString().split("&")), params));
        }
        String query = mkString("&", params);
        String uri = response.encodeRedirectURL(encodeUrlPath(path) + (query.isEmpty() ? "" : "?" + query));
        try {
            respond(response, 307, "<a href='" + uri + "'>" + path + "</a>", Pair.of("Location", uri));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    static boolean acceptParam(Set<String> queryParamsToExclude, String param) {
        for (String exclude: queryParamsToExclude) {
            if (param.equals(exclude) || param.startsWith(exclude + "=")) {
                return false;
            }
        }
        return true;
    }
    
    static String encodeUrlPath(String component) {
        return UriUtils.encodePath(component, "UTF-8").replace("'", "%27");
    }
    
    static String encodeUrlQueryString(String component) {
        return UriUtils.encodeQuery(component, "UTF-8").replace("'", "%27");
    }

}
