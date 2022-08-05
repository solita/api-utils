package fi.solita.utils.api.request;

import static fi.solita.utils.api.util.ResponseUtil.redirectToRevision;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Duration;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.functional.Option;

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
    
    public void redirectToCurrentRevision(HttpServletRequest req, HttpServletResponse res) {
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        redirectToRevision(getCurrentRevision().revision, req, res);
    }
    
    protected Option<Void> checkRevisions(Revision currentRevision, Revision revision, HttpServletRequest request, HttpServletResponse response) {
        if (!withinTolerance(currentRevision, revision)) {
            ResponseUtil.redirectToAnotherRevision(currentRevision.revision, request, response);
            return None();
        }
        return Some(null);
    }
    
    protected Option<Void> checkRevision(Revision revision, HttpServletRequest request, HttpServletResponse response) {
        Revision currentRevision = getCurrentRevision();
        return checkRevisions(currentRevision, revision, request, response);
    }
    
    protected Option<Void> checkRevisionAndUrl(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        for (@SuppressWarnings("unused") Void v: checkRevision(revision, request, response)) {
            checkUrl(request, acceptedParams);
            return Some(null);
        }
        return None();
    }
    
    /**
     * @throws NotFoundException for unidentified format
     */
    protected Option<RevisionedRequestData> checkRevisionAndUrlAndResolveFormat(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) throws NotFoundException {
        for (@SuppressWarnings("unused") Void v: checkRevision(revision, request, response)) {
            checkUrl(request, acceptedParams);
            RequestData data = NotFoundException.assertFound(resolveFormat(request, response)).get();
            return Some(new RevisionedRequestData(data.format, data.etags, revision));
        }
        return None();
    }
    
    /**
     * @throws NotFoundException for unidentified format
     */
    protected Option<RequestData> checkUrlAndResolveFormat(HttpServletRequest request, HttpServletResponse response, String... acceptedParams) throws NotFoundException {
        checkUrl(request, acceptedParams);
        Option<RequestData> ret = resolveFormat(request, response);
        NotFoundException.assertFound(ret);
        return ret;
    }
    
}
