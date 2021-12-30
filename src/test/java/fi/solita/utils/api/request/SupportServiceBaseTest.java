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
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Pair;

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
    public void relatePeriod() {
        DateTime someDateTime = DateTime.now();
        Period somePeriod = Period.days(42);
        
        assertEquals(someDateTime.plus(somePeriod), SupportServiceBase.relate(someDateTime, false, somePeriod));
        assertEquals(someDateTime.minus(somePeriod), SupportServiceBase.relate(someDateTime, true, somePeriod));
    }
    
    @Test
    public void parse() {
        Duration someDuration = Duration.standardDays(1);
        Period somePeriod = Period.days(42);
        
        assertEquals(Pair.of(Either.left(someDuration), false), SupportServiceBase.parse(      someDuration.toString()));
        assertEquals(Pair.of(Either.left(someDuration), true) , SupportServiceBase.parse("-" + someDuration.toString()));
        
        assertEquals(Pair.of(Either.right(somePeriod), false), SupportServiceBase.parse(      somePeriod.toString()));
        assertEquals(Pair.of(Either.right(somePeriod), true) , SupportServiceBase.parse("-" + somePeriod.toString()));
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
        support.redirectToCurrentInterval(new MockHttpServletRequest(), new MockHttpServletResponse(), someDuration.toString());
        support.redirectToCurrentInterval(new MockHttpServletRequest(), new MockHttpServletResponse(), someDuration + "/" + someDuration);
    }
    
    @Test(expected = InvalidValueException.class)
    public void redirectToCurrentInterval_multipartFailsForTooManyParts() {
        Duration someDuration = Duration.standardDays(1);
        support.redirectToCurrentInterval(new MockHttpServletRequest(), new MockHttpServletResponse(), someDuration + "/" + someDuration + "/" + someDuration);
    }
    
    @Test
    public void intervalForRedirectDuration() {
        DateTime someDateTime = DateTime.now();
        Duration someDuration = Duration.standardDays(1);
        
        assertEquals(new Interval(someDateTime, someDateTime.plus(someDuration)), SupportServiceBase.intervalForRedirect(someDateTime, someDuration, false));
        assertEquals(new Interval(someDateTime.minus(someDuration), someDateTime), SupportServiceBase.intervalForRedirect(someDateTime, someDuration, true));
    }
    
    @Test
    public void intervalForRedirectPeriod() {
        DateTime someDateTime = DateTime.now();
        Period somePeriod = Period.days(42);
        
        assertEquals(new Interval(someDateTime, someDateTime.plus(somePeriod)), SupportServiceBase.intervalForRedirect(someDateTime, somePeriod, false));
        assertEquals(new Interval(someDateTime.minus(somePeriod), someDateTime), SupportServiceBase.intervalForRedirect(someDateTime, somePeriod, true));
    }
    
    @Test
    public void intervalForRedirect() {
        DateTime someDateTime = DateTime.now();
        Duration someDuration = Duration.standardDays(1);
        Period somePeriod = Period.days(42);
        
        assertEquals(new Interval(someDateTime.plus(someDuration), someDateTime.plus(somePeriod)), SupportServiceBase.intervalForRedirect(someDateTime, Pair.of(Either.<Duration,Period>left(someDuration), false), Pair.of(Either.<Duration,Period>right(somePeriod), false)));
        assertEquals(new Interval(someDateTime.minus(somePeriod), someDateTime.minus(someDuration)), SupportServiceBase.intervalForRedirect(someDateTime, Pair.of(Either.<Duration,Period>right(somePeriod), true), Pair.of(Either.<Duration,Period>left(someDuration), true)));
    }
}
