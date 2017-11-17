package fi.solita.utils.api.common;

import static fi.solita.utils.api.ResponseUtil.redirect307;
import static fi.solita.utils.api.ResponseUtil.redirectToRevision;
import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.FunctionalA.cons;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Duration;

import fi.solita.utils.api.RequestUtil;
import fi.solita.utils.api.RequestUtil.ETags;
import fi.solita.utils.api.ResponseUtil;
import fi.solita.utils.api.SwaggerSupport;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public abstract class SupportServiceBase {
    protected Duration revisionsRedirectCached;
    
    public SupportServiceBase(Duration revisionsRedirectCached) {
        this.revisionsRedirectCached = revisionsRedirectCached;
    }
    
    protected abstract Revision getCurrentRevision();
    
    public void redirectToCurrentRevision(HttpServletRequest req, HttpServletResponse res) {
        RequestUtil.cacheFor(revisionsRedirectCached, res);
        redirectToRevision(getCurrentRevision().revision, req, res);
    }
    
    public void redirectToCurrentTime(HttpServletRequest req, HttpServletResponse res) {
        RequestUtil.cacheFor(revisionsRedirectCached, res);
        redirect307(RequestUtil.getContextRelativePath(req), req, res, newMap(Pair.of("time", SwaggerSupport.intervalNow())));
    }
    
    public Option<Pair<SerializationFormat, ETags>> resolveFormat(HttpServletRequest request, HttpServletResponse response) {
        SerializationFormat format = RequestUtil.resolveFormat(request);
        response.setContentType(format.mediaType);
        return Some(Pair.of(format, RequestUtil.getETags(request)));
    }
    
    public Pair<SerializationFormat, ETags> checkUrlAndResolveFormat(HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        RequestUtil.checkURL(request, newArray(String.class, cons("profile", acceptedParams)));
        return resolveFormat(request, response).get();
    }
    
    public Option<Pair<SerializationFormat,ETags>> checkRevisionAndUrlAndResolveFormat(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        long currentRevision = getCurrentRevision().revision;
        if (currentRevision != revision.revision) {
            ResponseUtil.redirectToAnotherRevision(currentRevision, request, response);
            return None();
        } else {
            return Some(checkUrlAndResolveFormat(request, response, acceptedParams));
        }
    }

    public Option<Boolean> checkRevisionAndUrl(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        long currentRevision = getCurrentRevision().revision;
        if (currentRevision != revision.revision) {
            ResponseUtil.redirectToAnotherRevision(currentRevision, request, response);
            return None();
        } else {
            RequestUtil.checkURL(request, newArray(String.class, cons("profile", acceptedParams)));
            return Some(true);
        }
    }
}
