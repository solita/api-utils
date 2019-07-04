package fi.solita.utils.api.common;

import static fi.solita.utils.api.ResponseUtil.redirectToRevision;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Duration;

import fi.solita.utils.api.ResponseUtil;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.functional.Option;

public abstract class RevisionedSupportServiceBase extends SupportServiceBase {
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
    
    public void redirectToCurrentRevision(HttpServletRequest req, HttpServletResponse res) {
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        redirectToRevision(getCurrentRevision().revision, req, res);
    }
    
    public Option<Boolean> checkRevision(Revision revision, HttpServletRequest request, HttpServletResponse response) {
        Revision currentRevision = getCurrentRevision();
        if (!withinTolerance(currentRevision, revision)) {
            ResponseUtil.redirectToAnotherRevision(currentRevision.revision, request, response);
            return None();
        }
        return Some(true);
    }
    
    public Option<RevisionedRequestData> checkRevisionAndUrlAndResolveFormat(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        Revision currentRevision = getCurrentRevision();
        if (!withinTolerance(currentRevision, revision)) {
            ResponseUtil.redirectToAnotherRevision(currentRevision.revision, request, response);
            return None();
        } else {
            RequestData data = checkUrlAndResolveFormat(request, response, acceptedParams);
            return Some(new RevisionedRequestData(data.format, data.etags, revision));
        }
    }

    public Option<Boolean> checkRevisionAndUrl(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        Revision currentRevision = getCurrentRevision();
        if (!withinTolerance(currentRevision, revision)) {
            ResponseUtil.redirectToAnotherRevision(currentRevision.revision, request, response);
            return None();
        } else {
            checkUrl(request, acceptedParams);
            return Some(true);
        }
    }
}
