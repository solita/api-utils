package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.emptySet;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;

import java.util.Map;

import org.junit.Test;

import fi.solita.utils.api.util.RequestUtil.QueryParameterValuesMustBeInLowercaseException;
import fi.solita.utils.functional.Pair;

public class RequestUtilTest {

    private static final Map<String,String[]> paramMap = emptyMap();
    
    @Test(expected = RequestUtil.IllegalQueryParametersException.class)
    public void tuntemattomatParametritEiKelpaa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("foo"), emptySet(), "bar");
    }
    
    @Test(expected = RequestUtil.QueryParameterValuesMustBeInLowercaseException.class)
    public void parametrienPitaaOllaLowercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("foo", new String[]{"Bar"})), newList("foo"), emptySet(), "foo");
    }
    
    @Test
    public void parametriSaaOllaUppercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("time", new String[]{"ZT"})), newList("time"), newSet("time"), "time");
    }
    
    @Test(expected = QueryParameterValuesMustBeInLowercaseException.class)
    public void parametriEiSaaOllaUppercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("time", new String[]{"ZT"})), newList("time"), emptySet(), "time");
    }
    
    @Test(expected = RequestUtil.QueryParametersMustNotBeDuplicatedException.class)
    public void duplikaattiparametritEiKelpaa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("foo", "foo"), emptySet(), "foo");
    }
    
    @Test
    public void parametritOltavaAakkosjarjestyksessaYhdessaTaiKahdessaOsassa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("c", "d"), emptySet(), "a", "c", "d");
        RequestUtil.assertQueryStringValid(paramMap, newList("c", "d", "a"), emptySet(), "a", "c", "d");
        RequestUtil.assertQueryStringValid(paramMap, newList("c", "d", "a", "b"), emptySet(), "a", "b", "c", "d");
        
        try {
            RequestUtil.assertQueryStringValid(paramMap, newList("b", "a"), emptySet(), "a", "b");
        } catch (RequestUtil.QueryParametersMustBeInAlphabeticalOrderException e) {
            // ok
        }
        
        try {
            RequestUtil.assertQueryStringValid(paramMap, newList("d", "b", "c", "a"), emptySet(), "a", "b", "c", "d");
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
