package fi.solita.utils.api.request;

import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import fi.solita.utils.api.base.http.HttpSerializers.InvalidValueException;
import fi.solita.utils.api.util.JakartaRequest;
import fi.solita.utils.api.util.JakartaResponse;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Tuple;

public class SupportServiceBaseTest {
    
    private SupportServiceBase support = new SupportServiceBase();

    @Test
    public void relateDuration() {
        DateTime someDateTime = DateTime.now();
        Duration someDuration = Duration.standardDays(1);
        
        assertEquals(someDateTime.plus(someDuration), SupportServiceBase.relate(someDateTime, false, someDuration));
        assertEquals(someDateTime.minus(someDuration), SupportServiceBase.relate(someDateTime, true, someDuration));
    }
    
    @Test
    public void relatePeriod_inaccurate() {
        DateTime someDateTime = DateTime.now();
        Period somePeriod = Period.days(42);
        
        assertEquals(someDateTime.plus(somePeriod).plusDays(1).withTimeAtStartOfDay(), SupportServiceBase.relate(someDateTime, false, false, somePeriod));
        assertEquals(someDateTime.minus(somePeriod).withTimeAtStartOfDay(), SupportServiceBase.relate(someDateTime, true, false, somePeriod));
    }
    
    @Test
    public void relatePeriod_accurate() {
        DateTime someDateTime = DateTime.now();
        Period somePeriod = Period.minutes(42);
        
        assertEquals(someDateTime.plus(somePeriod), SupportServiceBase.relate(someDateTime, false, true, somePeriod));
        assertEquals(someDateTime.minus(somePeriod), SupportServiceBase.relate(someDateTime, true, true, somePeriod));
    }
    
    @Test
    public void parse() {
        Duration someDuration = Duration.standardDays(1);
        Period somePeriodInaccurate = Period.days(42);
        Period somePeriodAccurate = Period.hours(42);
        
        assertEquals(Tuple.of(Either.left(someDuration), false, true), SupportServiceBase.parse(      someDuration.toString()));
        assertEquals(Tuple.of(Either.left(someDuration), true, true) , SupportServiceBase.parse("-" + someDuration.toString()));
        
        assertEquals(Tuple.of(Either.right(somePeriodInaccurate), false, false), SupportServiceBase.parse(      somePeriodInaccurate.toString()));
        assertEquals(Tuple.of(Either.right(somePeriodInaccurate), true, false) , SupportServiceBase.parse("-" + somePeriodInaccurate.toString()));
        
        assertEquals(Tuple.of(Either.right(somePeriodAccurate), false, true), SupportServiceBase.parse(      somePeriodAccurate.toString()));
        assertEquals(Tuple.of(Either.right(somePeriodAccurate), true, true) , SupportServiceBase.parse("-" + somePeriodAccurate.toString()));
    }
    
    @Test(expected = InvalidValueException.class)
    public void parseInvalid() {
        SupportServiceBase.parse("foo");
    }
    
    @Test(expected = InvalidValueException.class)
    public void parseEmpty() {
        SupportServiceBase.parse("");
    }
    
    @Test
    public void redirectToCurrentInterval_multipart() {
        Duration someDuration = Duration.standardDays(1);
        support.redirectToCurrentInterval(JakartaRequest.of(new MockHttpServletRequest()), JakartaResponse.of(new MockHttpServletResponse()), someDuration.toString());
        support.redirectToCurrentInterval(JakartaRequest.of(new MockHttpServletRequest()), JakartaResponse.of(new MockHttpServletResponse()), someDuration + "/" + someDuration);
    }
    
    @Test(expected = InvalidValueException.class)
    public void redirectToCurrentInterval_multipartFailsForTooManyParts() {
        Duration someDuration = Duration.standardDays(1);
        support.redirectToCurrentInterval(JakartaRequest.of(new MockHttpServletRequest()), JakartaResponse.of(new MockHttpServletResponse()), someDuration + "/" + someDuration + "/" + someDuration);
    }
    
    @Test
    public void intervalForRedirectDuration() {
        DateTime someDateTime = DateTime.now();
        Duration someDuration = Duration.standardDays(1);
        
        assertEquals(new Interval(someDateTime, someDateTime.plus(someDuration)), SupportServiceBase.intervalForRedirect(someDateTime, someDuration, false));
        assertEquals(new Interval(someDateTime.minus(someDuration), someDateTime), SupportServiceBase.intervalForRedirect(someDateTime, someDuration, true));
    }
    
    @Test
    public void intervalForRedirectPeriod_inaccurate() {
        DateTime someDateTime = DateTime.now();
        Period somePeriod = Period.days(42);
        
        assertEquals(new Interval(someDateTime, someDateTime.plus(somePeriod).plusDays(1).withTimeAtStartOfDay()), SupportServiceBase.intervalForRedirect(someDateTime, somePeriod, false, false));
        assertEquals(new Interval(someDateTime.minus(somePeriod).withTimeAtStartOfDay(), someDateTime), SupportServiceBase.intervalForRedirect(someDateTime, somePeriod, true, false));
    }
    
    @Test
    public void intervalForRedirectPeriod_accurate() {
        DateTime someDateTime = DateTime.now();
        Period somePeriod = Period.minutes(42);
        
        assertEquals(new Interval(someDateTime, someDateTime.plus(somePeriod)), SupportServiceBase.intervalForRedirect(someDateTime, somePeriod, false, true));
        assertEquals(new Interval(someDateTime.minus(somePeriod), someDateTime), SupportServiceBase.intervalForRedirect(someDateTime, somePeriod, true, true));
    }
    
    @Test
    public void intervalForRedirect() {
        DateTime someDateTime = DateTime.now();
        Duration someDuration = Duration.standardHours(1);
        Period somePeriod = Period.hours(1);
        
        assertEquals(new Interval(someDateTime.plus(someDuration), someDateTime.plus(somePeriod)), SupportServiceBase.intervalForRedirect(someDateTime, Pair.of(Either.<Duration,Period>left(someDuration), false, true), Pair.of(Either.<Duration,Period>right(somePeriod), false, true)));
        assertEquals(new Interval(someDateTime.minus(somePeriod), someDateTime.minus(someDuration)), SupportServiceBase.intervalForRedirect(someDateTime, Pair.of(Either.<Duration,Period>right(somePeriod), true, true), Pair.of(Either.<Duration,Period>left(someDuration), true, true)));
    }
}
