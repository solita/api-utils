package fi.solita.utils.api.request;

import static fi.solita.utils.api.util.ResponseUtil.redirectToRevision;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.base.http.HttpSerializers.InvalidValueException;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.api.util.ResponseUtil.Response;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

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
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        redirectToRevision(getCurrentRevision().revision, req, res);
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
        String[] parts = durationOrPeriod.split("/");
        
        if (parts.length == 1) {
            // create an interval either starting from or ending to current time.
            Pair<Either<Duration, Period>, Boolean> dp = parse(parts[0]);
            for (Duration d: dp.left().left) {
                redirectToCurrentRevisionAndInterval(req, res, adjustTime(intervalForRedirect(DateTime.now().withZone(DateTimeZone.UTC), d, dp.right())), newSet("duration"));
            }
            for (Period p: dp.left().right) {
                redirectToCurrentRevisionAndInterval(req, res, adjustTime(intervalForRedirect(DateTime.now().withZone(DateTimeZone.UTC), p, dp.right())), newSet("duration"));
            }
        } else if (parts.length == 2) {
            // create an interval where start and end are separately related to current time.
            Pair<Either<Duration, Period>, Boolean> start = parse(parts[0]);
            Pair<Either<Duration, Period>, Boolean> end = parse(parts[1]);
            Interval interval;
            try {
                interval = intervalForRedirect(DateTime.now().withZone(DateTimeZone.UTC), start, end);
            } catch (RuntimeException e) {
                throw new InvalidValueException("duration", durationOrPeriod);
            }
            redirectToCurrentRevisionAndInterval(req, res, adjustTime(interval), newSet("duration"));
        } else {
            throw new InvalidValueException("duration", durationOrPeriod);
        }
    }
    
    protected Option<Void> checkRevisions(Revision currentRevision, Revision revision, Request request, Response response) {
        if (!withinTolerance(currentRevision, revision)) {
            ResponseUtil.redirectToAnotherRevision(currentRevision.revision, request, response);
            return None();
        }
        return Some(null);
    }
    
    protected Option<Void> checkRevision(Revision revision, Request request, Response response) {
        Revision currentRevision = getCurrentRevision();
        return checkRevisions(currentRevision, revision, request, response);
    }
    
    protected Option<Void> checkRevisionAndUrl(Revision revision, Request request, Response response, String... acceptedParams) {
        for (@SuppressWarnings("unused") Void v: checkRevision(revision, request, response)) {
            checkUrl(request, acceptedParams);
            return Some(null);
        }
        return None();
    }
    
    /**
     * @throws NotFoundException for unidentified format
     */
    protected Option<RevisionedRequestData> checkRevisionAndUrlAndResolveFormat(Revision revision, Request request, Response response, String... acceptedParams) throws NotFoundException {
        for (@SuppressWarnings("unused") Void v: checkRevision(revision, request, response)) {
            checkUrl(request, acceptedParams);
            RequestData data = NotFoundException.assertFound(resolveFormat(request, response));
            return Some(new RevisionedRequestData(data.format, data.etags, revision));
        }
        return None();
    }
}
