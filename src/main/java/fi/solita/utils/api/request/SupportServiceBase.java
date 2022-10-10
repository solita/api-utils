package fi.solita.utils.api.request;

import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.FunctionalC.tail;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.Period;

import fi.solita.utils.api.NotFoundException;
import fi.solita.utils.api.base.http.HttpSerializers.InvalidValueException;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.api.util.ResponseUtil;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public class SupportServiceBase {
    
    /**
     * @return Current time. Previous UTC midnight by default.
     */
    public DateTime currentTime() {
        return LocalDate.now(DateTimeZone.UTC).toDateTimeAtStartOfDay(DateTimeZone.UTC);
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

    public void redirectToCurrentTime(HttpServletRequest req, HttpServletResponse res) {
        DateTime now = currentTime();
        redirectToInterval(req, res, new Interval(now, now), Collections.<String>emptySet());
    }
    
    public void redirectToCurrentInterval(HttpServletRequest req, HttpServletResponse res, String durationOrPeriod) throws InvalidValueException {
        String[] parts = durationOrPeriod.split("/");
        
        if (parts.length == 1) {
            // create an interval either starting from or ending to current time.
            Pair<Either<Duration, Period>, Boolean> dp = parse(parts[0]);
            for (Duration d: dp.left().left) {
                redirectToInterval(req, res, intervalForRedirect(currentTime(), d, dp.right()), newSet("duration"));
            }
            for (Period p: dp.left().right) {
                redirectToInterval(req, res, intervalForRedirect(currentTime(), p, dp.right()), newSet("duration"));
            }
        } else if (parts.length == 2) {
            // create an interval where start and end are separately related to current time.
            Pair<Either<Duration, Period>, Boolean> start = parse(parts[0]);
            Pair<Either<Duration, Period>, Boolean> end = parse(parts[1]);
            Interval interval;
            try {
                interval = intervalForRedirect(currentTime(), start, end);
            } catch (RuntimeException e) {
                throw new InvalidValueException("duration", durationOrPeriod);
            }
            redirectToInterval(req, res, interval, newSet("duration"));
        } else {
            throw new InvalidValueException("duration", durationOrPeriod);
        }
    }
    
    static DateTime relate(DateTime now, boolean negate, Duration d) {
        return negate ? now.minus(d) : now.plus(d);
    }
    
    static DateTime relate(DateTime now, boolean negate, Period p) {
        return negate ? now.minus(p) : now.plus(p);
    }
    
    static Pair<Either<Duration,Period>,Boolean> parse(String durationOrPeriod) throws InvalidValueException {
        boolean negate = false;
        if (durationOrPeriod.startsWith("-")) {
            negate = true;
            durationOrPeriod = tail(durationOrPeriod);
        }
        try {
            return Pair.of(Either.<Duration,Period>left(Duration.parse(durationOrPeriod)), negate);
        } catch (Exception e) {
            try {
                return Pair.of(Either.<Duration,Period>right(Period.parse(durationOrPeriod)), negate);
            } catch (Exception e1) {
                throw new InvalidValueException("duration", durationOrPeriod);
            }
        }
    }
    
    static Interval intervalForRedirect(DateTime now, Duration duration, boolean negate) {
        return negate ? new Interval(now.minus(duration), now) : new Interval(now, now.plus(duration));
    }
    
    static Interval intervalForRedirect(DateTime now, Period period, boolean negate) {
        return negate ? new Interval(now.minus(period), now) : new Interval(now, now.plus(period));
    }
    
    static Interval intervalForRedirect(DateTime now, Pair<Either<Duration, Period>, Boolean> start, Pair<Either<Duration, Period>, Boolean> end) {
        DateTime start_ = start.left().fold(SupportServiceBase_.relate.ap(now, start.right()), SupportServiceBase_.relate1.ap(now, start.right()));
        DateTime end_   = end  .left().fold(SupportServiceBase_.relate.ap(now, end  .right()), SupportServiceBase_.relate1.ap(now, end  .right()));
        return new Interval(start_, end_);
    }
    
    public static void redirectToInterval(HttpServletRequest req, HttpServletResponse res, Interval interval, Set<String> queryParamsToExclude) {
        ResponseUtil.redirect307(RequestUtil.getContextRelativePath(req), req, res, newMap(Pair.of("time", RequestUtil.interval2stringRestrictedToInfinity(interval))), queryParamsToExclude);
    }

    protected Option<RequestData> resolveFormat(HttpServletRequest request, HttpServletResponse response) {
        Either<Option<String>, SerializationFormat> format = RequestUtil.resolveFormat(request);
        for (SerializationFormat f: format.right) {
            response.setContentType(f.mediaType);
            return Some(new RequestData(f, RequestUtil.getETags(request)));
        }
        return None();
    }

    protected void checkUrl(HttpServletRequest request, String... acceptedParams) {
        RequestUtil.checkURL(request, newArray(String.class, cons("time", cons("presentation", cons("profile", cons("srsName", acceptedParams))))));
    }
}
