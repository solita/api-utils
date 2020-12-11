package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;

import java.util.Map;

import org.junit.Test;

import fi.solita.utils.functional.Pair;

public class RequestUtilTest {

    private static final Map<String,String[]> paramMap = emptyMap();
    
    @Test(expected = RequestUtil.IllegalQueryParametersException.class)
    public void tuntemattomatParametritEiKelpaa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("foo"), "bar");
    }
    
    @Test(expected = RequestUtil.QueryParameterValuesMustBeInLowercaseException.class)
    public void parametrienPitaaOllaLowercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("foo", new String[]{"Bar"})), newList("foo"), "foo");
    }
    
    @Test
    public void timeParametriSaaOllaUppercase() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("time", new String[]{"ZT"})), newList("time"), "time");
    }
    
    @Test(expected = RequestUtil.QueryParametersMustNotBeDuplicatedException.class)
    public void duplikaattiparametritEiKelpaa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("foo", "foo"), null, "foo");
    }
    
    @Test(expected = RequestUtil.QueryParametersMustBeInAlphabeticalOrderException.class)
    public void parametritOltavaAakkosjarjestyksessa() {
        RequestUtil.assertQueryStringValid(paramMap, newList("b", "a"), "a", "b");
    }
}
