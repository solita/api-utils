package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.emptySet;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;

import java.util.Map;

import org.junit.Test;

import fi.solita.utils.api.util.RequestUtil.QueryParameterValuesMustBeInLowercaseException;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Pair;

public class RequestUtilTest {

    private static final Map<String,String[]> paramMap = emptyMap();
    
    @Test(expected = RequestUtil.IllegalQueryParametersException.class)
    public void tuntemattomatParametritEiKelpaa() {
        RequestUtil.assertQueryStringValid(paramMap, Collections.<String>newList("foo"), Collections.<String>emptySet(), "bar");
    }
    
    @Test(expected = RequestUtil.QueryParameterValuesMustBeInLowercaseException.class)
    public void parametrienPitaaOllaLowercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("foo", new String[]{"Bar"})), Collections.<String>newList("foo"), Collections.<String>emptySet(), "foo");
    }
    
    @Test
    public void parametriSaaOllaUppercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("time", new String[]{"ZT"})), Collections.<String>newList("time"), Collections.<String>newSet("time"), "time");
    }
    
    @Test(expected = QueryParameterValuesMustBeInLowercaseException.class)
    public void parametriEiSaaOllaUppercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("time", new String[]{"ZT"})), Collections.<String>newList("time"), Collections.<String>emptySet(), "time");
    }
    
    @Test(expected = RequestUtil.QueryParametersMustNotBeDuplicatedException.class)
    public void duplikaattiparametritEiKelpaa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("foo", "foo"), Collections.<String>emptySet(), "foo");
    }
    
    @Test
    public void parametritOltavaAakkosjarjestyksessaYhdessaTaiKahdessaOsassa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("c", "d"), Collections.<String>emptySet(), "a", "c", "d");
        RequestUtil.assertQueryStringValid(paramMap, newList("c", "d", "a"), Collections.<String>emptySet(), "a", "c", "d");
        RequestUtil.assertQueryStringValid(paramMap, newList("c", "d", "a", "b"), Collections.<String>emptySet(), "a", "b", "c", "d");
        
        try {
            RequestUtil.assertQueryStringValid(paramMap, newList("b", "a"), Collections.<String>emptySet(), "a", "b");
        } catch (RequestUtil.QueryParametersMustBeInAlphabeticalOrderException e) {
            // ok
        }
        
        try {
            RequestUtil.assertQueryStringValid(paramMap, newList("d", "b", "c", "a"), Collections.<String>emptySet(), "a", "b", "c", "d");
        } catch (RequestUtil.QueryParametersMustBeInAlphabeticalOrderException e) {
            // ok
        }
    }

    @Test(expected = RequestUtil.EventStreamNotAccepted.class)
    public void eventStreamNotAccepted() {
        RequestUtil.assertAcceptHeader(newList("text/event-stream"));
    }

    @Test(expected = RequestUtil.EventStreamNotAccepted.class)
    public void eventStreamWithCharsetNotAccepted() {
        RequestUtil.assertAcceptHeader(newList("text/event-stream;chartset=foo"));
    }

    @Test(expected = RequestUtil.EventStreamNotAccepted.class)
    public void eventStreamsNotAccepted() {
        RequestUtil.assertAcceptHeader(newList("text/plain", "text/event-stream"));
    }
}
