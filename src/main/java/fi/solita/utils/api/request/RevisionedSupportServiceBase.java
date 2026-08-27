package fi.solita.utils.api.request;

import static fi.solita.utils.api.util.ResponseUtil.redirectToRevision;
import static fi.solita.utils.functional.Collections.it;

import java.util.Map;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.base.http.HttpSerializers.InvalidValueException;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.api.util.ResponseUtil.Response;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.functional.Collections;
import java.util.Optional;

public abstract class RevisionedSupportServiceBase extends SupportServiceBase implements RevisionProvider {
    protected final Duration revisionsRedirectCached;
    private final int revisionCheckTolerance;
    
    public RevisionedSupportServiceBase(Duration revisionsRedirectCached, int revisionCheckTolerance) {
        this.revisionsRedirectCached = revisionsRedirectCached;
        this.revisionCheckTolerance = revisionCheckTolerance;
    }
    
    public abstract Revision getCurrentRevision();
    
    public abstract Set<Revision> getValidRevisions();
    
    public boolean withinTolerance(Revision revision1, Revision revision2) {
        return Math.abs(revision1.revision - revision2.revision) <= revisionCheckTolerance;
    }
    
    public void redirectToCurrentRevision(Request req, Response res) {
        redirectToCurrentRevision(req, res, Collections.<String,String>emptyMap(), Collections.<String>emptySet());
    }
    
    public void redirectToCurrentRevision(Request req, Response res, Map<String,String> additionalUnescapedQueryParams, Set<String> queryParamsToExclude) {
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        redirectToRevision(getCurrentRevision().revision, req, res, additionalUnescapedQueryParams, queryParamsToExclude);
    }
    
    public void redirectToCurrentRevisionAndTime(Request req, Response res) {
        DateTime now = currentTime();
        redirectToCurrentRevisionAndInterval(req, res, new Interval(now, now), Collections.<String>emptySet());
    }

    public void redirectToCurrentRevisionAndDateTime(Request req, Response res, DateTime dateTime, Set<String> queryParamsToExclude) {
        if (dateTime == null) {
            dateTime = currentTime();
        }
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        ResponseUtil.redirectToRevisionAndDateTime(req, res, getCurrentRevision().revision, dateTime, queryParamsToExclude);
    }
    
    public void redirectToCurrentRevisionAndInterval(Request req, Response res, Interval interval, Set<String> queryParamsToExclude) {
        if (interval == null) {
            DateTime now = currentTime();
            interval = new Interval(now, now);
        }
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        ResponseUtil.redirectToRevisionAndInterval(req, res, getCurrentRevision().revision, interval, queryParamsToExclude);
    }
    
    public void redirectToCurrentRevisionAndInterval(Request req, Response res, String durationOrPeriod) throws InvalidValueException {
        redirectToCurrentInterval(RevisionedSupportServiceBase_.redirectToCurrentRevisionAndInterval.ap(this), req, res, durationOrPeriod);
    }
    
    protected boolean checkRevisions(Revision currentRevision, Revision revision, Request request, Response response) {
        if (!withinTolerance(currentRevision, revision)) {
            ResponseUtil.redirectToAnotherRevision(currentRevision.revision, request, response);
            return false;
        }
        return true;
    }
    
    protected boolean checkRevision(Revision revision, Request request, Response response) {
        Revision currentRevision = getCurrentRevision();
        return checkRevisions(currentRevision, revision, request, response);
    }
    
    protected boolean checkRevisionAndUrl(Revision revision, Request request, Response response, String... acceptedParams) {
        if (checkRevision(revision, request, response)) {
            checkUrl(request, acceptedParams);
            return true;
        }
        return false;
    }
    
    /**
     * @throws NotFoundException for unidentified format
     */
    protected Optional<RevisionedRequestData> checkRevisionAndUrlAndResolveFormat(Revision revision, Request request, Response response, String... acceptedParams) throws NotFoundException {
        if (checkRevision(revision, request, response)) {
            checkUrl(request, acceptedParams);
            RequestData data = NotFoundException.assertFound(resolveFormat(request, response));
            return Optional.of(new RevisionedRequestData(data.format, data.etags, revision));
        }
        return Optional.empty();
    }
}
