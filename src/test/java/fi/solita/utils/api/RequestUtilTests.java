package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;

import java.util.Map;

import org.junit.Test;

import fi.solita.utils.api.RequestUtil;
import fi.solita.utils.functional.Pair;

public class RequestUtilTests {

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
    
    @Test(expected = RequestUtil.QueryParameterMustBeInAlphabeticalOrderException.class)
    public void propertyNameOltavaAakkosjarjestyksessa() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("propertyName", new String[] {"b","a"})), newList("propertyName"), "propertyName");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustBeInAlphabeticalOrderException.class)
    public void propertyNameOltavaAakkosjarjestyksessa2() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("propertyName", new String[] {"b,a"})), newList("propertyName"), "propertyName");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustBeInAlphabeticalOrderException.class)
    public void typeNamesOltavaAakkosjarjestyksessa() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("typeNames", new String[] {"b","a"})), newList("typeNames"), "typeNames");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustBeInAlphabeticalOrderException.class)
    public void typeNamesOltavaAakkosjarjestyksessa2() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("typeNames", new String[] {"b,a"})), newList("typeNames"), "typeNames");
    }
    
    @Test
    public void muuEiTarvitseOllaAakkosjarjestyksessa() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("muu", new String[] {"b","a"})), newList("muu"), "muu");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustNotContainDuplicatesException.class)
    public void propertyNameEiSaaSisaltaaDuplikaatteja() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("propertyName", new String[] {"a","a"})), newList("propertyName"), "propertyName");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustNotContainDuplicatesException.class)
    public void propertyNameEiSaaSisaltaaDuplikaatteja2() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("propertyName", new String[] {"a,a"})), newList("propertyName"), "propertyName");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustNotContainDuplicatesException.class)
    public void propertyNameEiSaaSisaltaaDuplikaatteja3() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("propertyName", new String[] {"a,a "})), newList("propertyName"), "propertyName");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustNotContainDuplicatesException.class)
    public void typeNamesEiSaaSisaltaaDuplikaatteja() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("typeNames", new String[] {"a","a"})), newList("typeNames"), "typeNames");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustNotContainDuplicatesException.class)
    public void typeNamesEiSaaSisaltaaDuplikaatteja2() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("typeNames", new String[] {"a,a"})), newList("typeNames"), "typeNames");
    }
    
    @Test(expected = RequestUtil.QueryParameterMustNotContainDuplicatesException.class)
    public void typeNamesEiSaaSisaltaaDuplikaatteja3() {
        RequestUtil.assertQueryStringValid(newMap(Pair.of("typeNames", new String[] {"a,a "})), newList("typeNames"), "typeNames");
    }
}
