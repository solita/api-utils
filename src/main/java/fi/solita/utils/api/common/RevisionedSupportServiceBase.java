package fi.solita.utils.api.common;

import static fi.solita.utils.api.ResponseUtil.redirectToRevision;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.Duration;

import fi.solita.utils.api.ResponseUtil;
import fi.solita.utils.api.types.Revision;
import fi.solita.utils.functional.Option;

public abstract class RevisionedSupportServiceBase extends SupportServiceBase {
    protected Duration revisionsRedirectCached;
    
    public RevisionedSupportServiceBase(Duration revisionsRedirectCached) {
        this.revisionsRedirectCached = revisionsRedirectCached;
    }
    
    protected abstract Revision getCurrentRevision();
    
    public void redirectToCurrentRevision(HttpServletRequest req, HttpServletResponse res) {
        ResponseUtil.cacheFor(revisionsRedirectCached, res);
        redirectToRevision(getCurrentRevision().revision, req, res);
    }
    
    public Option<Boolean> checkRevision(Revision revision, HttpServletRequest request, HttpServletResponse response) {
        long currentRevision = getCurrentRevision().revision;
        if (currentRevision != revision.revision) {
            ResponseUtil.redirectToAnotherRevision(currentRevision, request, response);
            return None();
        }
        return Some(true);
    }
    
    public Option<RevisionedRequestData> checkRevisionAndUrlAndResolveFormat(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        long currentRevision = getCurrentRevision().revision;
        if (currentRevision != revision.revision) {
            ResponseUtil.redirectToAnotherRevision(currentRevision, request, response);
            return None();
        } else {
            RequestData data = checkUrlAndResolveFormat(request, response, acceptedParams);
            return Some(new RevisionedRequestData(data.format, data.etags, revision));
        }
    }

    public Option<Boolean> checkRevisionAndUrl(Revision revision, HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        long currentRevision = getCurrentRevision().revision;
        if (currentRevision != revision.revision) {
            ResponseUtil.redirectToAnotherRevision(currentRevision, request, response);
            return None();
        } else {
            checkUrl(request, acceptedParams);
            return Some(true);
        }
    }
}
