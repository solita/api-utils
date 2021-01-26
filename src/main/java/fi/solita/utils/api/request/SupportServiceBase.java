package fi.solita.utils.api.request;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Option.Some;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public class SupportServiceBase {

    public void redirectToCurrentTime(HttpServletRequest req, HttpServletResponse res) {
        DateTime now = RequestUtil.currentTime();
        redirectToCurrentInterval(req, res, new Interval(now, now), Collections.<String>emptySet());
    }
    
    public void redirectToCurrentInterval(HttpServletRequest req, HttpServletResponse res, String durationOrPeriod) {
        Duration d;
        try {
            d = Duration.parse(durationOrPeriod);
        } catch (Exception e) {
            redirectToCurrentInterval(req, res, Period.parse(durationOrPeriod), newSet("duration"));
            return;
        }
        redirectToCurrentInterval(req, res, d, newSet("duration"));
    }
    
    public void redirectToCurrentInterval(HttpServletRequest req, HttpServletResponse res, Duration duration, Set<String> queryParamsToExclude) {
        DateTime now = RequestUtil.currentTime();
        redirectToCurrentInterval(req, res, new Interval(now, now.plus(duration)), queryParamsToExclude);
    }
    
    public void redirectToCurrentInterval(HttpServletRequest req, HttpServletResponse res, Period period, Set<String> queryParamsToExclude) {
        DateTime now = RequestUtil.currentTime();
        redirectToCurrentInterval(req, res, new Interval(now, now.plus(period)), queryParamsToExclude);
    }
    
    public void redirectToCurrentInterval(HttpServletRequest req, HttpServletResponse res, Interval interval, Set<String> queryParamsToExclude) {
        ResponseUtil.redirect307(RequestUtil.getContextRelativePath(req), req, res, newMap(Pair.of("time", RequestUtil.interval2stringRestrictedToInfinity(interval))), queryParamsToExclude);
    }

    public Option<RequestData> resolveFormat(HttpServletRequest request, HttpServletResponse response) {
        SerializationFormat format = RequestUtil.resolveFormat(request);
        response.setContentType(format.mediaType);
        return Some(new RequestData(format, RequestUtil.getETags(request)));
    }

    public RequestData checkUrlAndResolveFormat(HttpServletRequest request, HttpServletResponse response, String... acceptedParams) {
        checkUrl(request, acceptedParams);
        return resolveFormat(request, response).get();
    }
    
    public void checkUrl(HttpServletRequest request, String... acceptedParams) {
        RequestUtil.checkURL(request, newArray(String.class, cons("time", cons("presentation", cons("profile", cons("srsName", acceptedParams))))));
    }
}
