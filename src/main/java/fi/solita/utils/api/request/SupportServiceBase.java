package fi.solita.utils.api.request;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.FunctionalC.tail;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.base.http.HttpSerializers.InvalidValueException;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.api.util.ResponseUtil.Response;
import fi.solita.utils.api.util.ServletRequestUtil;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function4;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Tuple3;

public class SupportServiceBase {
    
    /**
     * @return Current time. Second precision by default.
     */
    public DateTime currentTime() {
        return DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0);
    }
    
    /**
     * @return currentTime() as ISO String.
     */
    public final String now() {
        return RequestUtil.instant2string(currentTime());
    }
    
    /**
     * @return currentTime() as ISO interval.
     */
    public final String intervalNow() {
        String n = now();
        return n + "/" + n;
    }
    
    public DateTime adjustTime(DateTime time) {
        return time;
    }
    
    public Interval adjustTime(Interval time) {
        return time;
    }

    public void redirectToCurrentTime(Request req, Response res) {
        DateTime now = currentTime();
        redirectToInterval(req, res, new Interval(now, now), Collections.<String>emptySet());
    }
    
    public void redirectToCurrentInterval(Request req, Response res, String durationOrPeriod) throws InvalidValueException {
        redirectToCurrentInterval(SupportServiceBase_.redirectToInterval, req, res, durationOrPeriod);
    }
    
    public void redirectToCurrentInterval(Function4<Request,Response,Interval,Set<String>,Void> f, Request req, Response res, String durationOrPeriod) throws InvalidValueException {
        String[] parts = durationOrPeriod.split("/");
        
        if (parts.length == 1) {
            // create an interval either starting from or ending to current time.
            Tuple3<Either<Duration, Period>, Boolean, Boolean> dp = parse(parts[0]);
            for (Duration d: dp._1.left) {
                f.apply(req, res, adjustTime(intervalForRedirect(DateTime.now(), d, dp._2)), newSet("duration"));
            }
            for (Period p: dp._1.right) {
                f.apply(req, res, adjustTime(intervalForRedirect(DateTime.now(), p, dp._2, dp._3)), newSet("duration"));
            }
        } else if (parts.length == 2) {
            // create an interval where start and end are separately related to current time.
            Tuple3<Either<Duration, Period>, Boolean, Boolean> start = parse(parts[0]);
            Tuple3<Either<Duration, Period>, Boolean, Boolean> end = parse(parts[1]);
            Interval interval;
            try {
                interval = intervalForRedirect(DateTime.now(), start, end);
            } catch (RuntimeException e) {
                throw new InvalidValueException("duration", durationOrPeriod);
            }
            f.apply(req, res, adjustTime(interval), newSet("duration"));
        } else {
            throw new InvalidValueException("duration", durationOrPeriod);
        }
    }
    
    static DateTime relate(DateTime now, boolean negate, Duration d) {
        return negate ? now.minus(d) : now.plus(d);
    }
    
    static DateTime relate(DateTime now, boolean negate, boolean containsMoreAccurateThanDays, Period p) {
        return negate ? (containsMoreAccurateThanDays ? now.minus(p) : now.minus(p).withTimeAtStartOfDay())
                      : (containsMoreAccurateThanDays ? now.plus(p) : now.plus(p).withTimeAtStartOfDay());
    }
    
    static Tuple3<Either<Duration,Period>,Boolean,Boolean> parse(String durationOrPeriod) throws InvalidValueException {
        boolean negate = false;
        if (durationOrPeriod.startsWith("-")) {
            negate = true;
            durationOrPeriod = tail(durationOrPeriod);
        }
        boolean containsMoreAccurateThanDays = durationOrPeriod.contains("T");
        try {
            return Pair.of(Either.<Duration,Period>left(Duration.parse(durationOrPeriod)), negate, containsMoreAccurateThanDays);
        } catch (Exception e) {
            try {
                return Pair.of(Either.<Duration,Period>right(Period.parse(durationOrPeriod)), negate, containsMoreAccurateThanDays);
            } catch (Exception e1) {
                throw new InvalidValueException("duration", durationOrPeriod);
            }
        }
    }
    
    static Interval intervalForRedirect(DateTime now, Duration duration, boolean negate) {
        return negate ? new Interval(relate(now, true, duration), now) : new Interval(now, relate(now, false, duration));
    }
    
    static Interval intervalForRedirect(DateTime now, Period period, boolean negate, boolean containsMoreAccurateThanDays) {
        return negate ? new Interval(relate(now, true, containsMoreAccurateThanDays, period), now) : new Interval(now, relate(now, false, containsMoreAccurateThanDays, period));
    }
    
    static Interval intervalForRedirect(DateTime now, Tuple3<Either<Duration, Period>, Boolean, Boolean> start, Tuple3<Either<Duration, Period>, Boolean, Boolean> end) {
        DateTime start_ = start._1.fold(SupportServiceBase_.relate.ap(now, start._2), SupportServiceBase_.relate1.ap(now, start._2, start._3));
        DateTime end_   = end  ._1.fold(SupportServiceBase_.relate.ap(now, end  ._2), SupportServiceBase_.relate1.ap(now, end  ._2, start._3));
        return new Interval(start_, end_);
    }
    
    public static void redirectToInterval(Request req, Response res, Interval interval, Set<String> queryParamsToExclude) {
        ResponseUtil.redirect307(ServletRequestUtil.getContextRelativePath(req), req, res, newMap(Pair.of("time", RequestUtil.interval2stringRestrictedToInfinity(interval))), queryParamsToExclude);
    }

    protected Option<RequestData> resolveFormat(Request request, Response response) {
        Either<Option<String>, SerializationFormat> format = ServletRequestUtil.resolveFormat(request);
        for (SerializationFormat f: format.right) {
            response.setContentType(f.mediaType);
            return Some(new RequestData(f, ServletRequestUtil.getETags(request)));
        }
        return None();
    }
    
    private static final Set<String> defaultCaseIgnoredParams = newSet("propertyName", "cql_filter", "time");
    private static final String[] defaultKnownParameters = { "time", "presentation", "profile", "srsName" };
    
    protected Set<String> getCaseIgnoredParams() {
        return defaultCaseIgnoredParams;
    }
    
    public void checkUrl(Request request, String... acceptedParams) {
        ServletRequestUtil.checkURL(request, getCaseIgnoredParams(), acceptedParams.length == 0 ? defaultKnownParameters : newArray(String.class, concat(defaultKnownParameters, acceptedParams)));
    }
    
    /**
     * @throws NotFoundException for unidentified format
     */
    protected Option<RequestData> checkUrlAndResolveFormat(Request request, Response response, String... acceptedParams) throws NotFoundException {
        checkUrl(request, acceptedParams);
        Option<RequestData> ret = resolveFormat(request, response);
        NotFoundException.assertFound(ret);
        return ret;
    }
}
