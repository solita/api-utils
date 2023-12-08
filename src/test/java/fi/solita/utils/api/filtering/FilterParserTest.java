package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.functional.Collections;

public class FilterParserTest {
    
    private static final PropertyName FOO = PropertyName.of("foo");

    private List<List<Filter>> someFilter(String value) {
        return Collections.<List<Filter>>newList(newList(new Filter(FilterType.EQUAL, FOO, Literal.of(value))));
    }
    
    @Test
    public void parsesIntegers() {
        assertEquals(someFilter("1"), FilterParser.parse("foo=1"));
        assertEquals(someFilter("-1"), FilterParser.parse("foo=-1"));
    }

    @Test
    public void parsesDecimals() {
        assertEquals(someFilter("1.1"), FilterParser.parse("foo=1.1"));
        assertEquals(someFilter("-1.1"), FilterParser.parse("foo=-1.1"));
    }
    
    @Test
    public void parsesBooleans() {
        assertEquals(someFilter("true"), FilterParser.parse("foo=true"));
        assertEquals(someFilter("false"), FilterParser.parse("foo=false"));
    }
    
    @Test
    public void parsesStrings() {
        assertEquals(someFilter("''"), FilterParser.parse("foo=''"));
        assertEquals(someFilter("'a'"), FilterParser.parse("foo='a'"));
        assertEquals(someFilter("'''a''b'''"), FilterParser.parse("foo='''a''b'''"));
        assertEquals(someFilter("';'"), FilterParser.parse("foo=';'"));
        assertEquals(someFilter("'+'"), FilterParser.parse("foo='+'"));
    }
    
    @Test
    public void parsesTimes() {
        DateTime now = DateTime.now();
        assertEquals(someFilter(now.toString(Serializers.DATETIME_FORMAT_NO_MILLIS)), FilterParser.parse("foo=" + now.toString(Serializers.DATETIME_FORMAT_NO_MILLIS)));
    }
    
    @Test
    public void parsesDuration() {
        assertEquals(someFilter("P0Y0M0DT0H0M1S"), FilterParser.parse("foo=P0Y0M0DT0H0M1S"));
    }
    
    @Test
    public void parsesPolygon() {
        assertEquals(Collections.<List<Filter>>newList(newList(new Filter(FilterType.INTERSECTS, FOO, Literal.of("POLYGON((30 10,40 40,20 40,10 20,30 10))")))), FilterParser.parse("INTERSECTS(foo,POLYGON((30 10,40 40,20 40,10 20,30 10)))"));
    }
    
    @Test
    public void parsesMultiple() {
        assertEquals(Collections.<List<Filter>>newList(newList(new Filter(FilterType.EQUAL, FOO, Literal.of("1")), new Filter(FilterType.EQUAL, FOO, Literal.of("2")))), FilterParser.parse("foo=1 AND foo=2"));
        assertEquals(Collections.<List<Filter>>newList(newList(new Filter(FilterType.EQUAL, FOO, Literal.of("1")), new Filter(FilterType.EQUAL, FOO, Literal.of("2")), new Filter(FilterType.EQUAL, FOO, Literal.of("3")))), FilterParser.parse("foo=1 AND foo=2 AND foo=3"));
        assertEquals(Collections.<List<Filter>>newList(newList(new Filter(FilterType.EQUAL, FOO, Literal.of("1")), new Filter(FilterType.EQUAL, FOO, Literal.of("2")), new Filter(FilterType.EQUAL, FOO, Literal.of("3")), new Filter(FilterType.EQUAL, FOO, Literal.of("4")), new Filter(FilterType.EQUAL, FOO, Literal.of("5")))), FilterParser.parse("foo=1 AND foo=2 AND foo=3 AND foo=4 AND foo=5"));
    }
    
    @Test
    public void parseEQUAL() {
        assertFalse(FilterParser.parse("foo=1").isEmpty());
    }
    
    @Test
    public void parseNOTEQUAL() {
        assertFalse(FilterParser.parse("foo<>1").isEmpty());
    }
    
    @Test
    public void parseLT() {
        assertFalse(FilterParser.parse("foo<1").isEmpty());
    }
    
    @Test
    public void parseGT() {
        assertFalse(FilterParser.parse("foo>1").isEmpty());
    }
    
    @Test
    public void parseLTE() {
        assertFalse(FilterParser.parse("foo<=1").isEmpty());
    }
    
    @Test
    public void parseGTE() {
        assertFalse(FilterParser.parse("foo>=1").isEmpty());
    }
    
    @Test
    public void parseBETWEEN() {
        assertFalse(FilterParser.parse("foo BETWEEN 1 AND 2").isEmpty());
    }
    
    @Test
    public void parseNOTBETWEEN() {
        assertFalse(FilterParser.parse("foo NOT BETWEEN 1 AND 2").isEmpty());
    }
    
    @Test
    public void parseLIKE() {
        assertFalse(FilterParser.parse("foo LIKE 'a'").isEmpty());
    }
    
    @Test
    public void parseNOTLIKE() {
        assertFalse(FilterParser.parse("foo NOT LIKE 'a'").isEmpty());
    }
    
    @Test
    public void parseILIKE() {
        assertFalse(FilterParser.parse("foo ILIKE 'a'").isEmpty());
    }
    
    @Test
    public void parseNOTILIKE() {
        assertFalse(FilterParser.parse("foo NOT ILIKE 'a'").isEmpty());
    }
    
    @Test
    public void parseIN() {
        assertFalse(FilterParser.parse("foo IN (1,2)").isEmpty());
    }
    
    @Test
    public void parseNOTIN() {
        assertFalse(FilterParser.parse("foo NOT IN (1,2)").isEmpty());
    }
    
    @Test
    public void parseNULL() {
        assertFalse(FilterParser.parse("foo IS NULL").isEmpty());
    }
    
    @Test
    public void parseNOTNULL() {
        assertFalse(FilterParser.parse("foo IS NOT NULL").isEmpty());
    }
    
    @Test
    public void parseINTERSECTS() {
        assertFalse(FilterParser.parse("INTERSECTS(foo,POLYGON((30 10,40 40,20 40,10 20, 30 10)))").isEmpty());
    }
    
    @Test(expected = FilterParser.IllegalPointException.class)
    public void doesNotParsePolygonWithIllegalPoint() {
        FilterParser.parse("INTERSECTS(foo,POLYGON((30 10,40 40,20 40,10 20, 30 a)))");
    }
    
    @Test(expected = FilterParser.IllegalPolygonException.class)
    public void doesNotParseIllegalPolygon() {
        FilterParser.parse("INTERSECTS(foo,POLYGON(30 10,40 40,20 40,10 20, 30 10))");
    }
    
    @Test(expected = FilterParser.IllegalPolygonException.class)
    public void doesNotParseEmptyPolygon() {
        FilterParser.parse("INTERSECTS(foo,POLYGON(()))");
    }
    
    @Test(expected = FilterParser.FirstCoordinateMustEqualLastCoordinateException.class)
    public void doesNotParseOpenPolygon() {
        FilterParser.parse("INTERSECTS(foo,POLYGON((30 10,40 40,20 40,10 20)))");
    }
    
    @Test(expected = FilterParser.IllegalFilterException.class)
    public void doesNotParseIfAdditionalStuff() throws Exception {
        FilterParser.parse("foo IS NULL AND blah blah");
    }
    
    @Test
    public void parsesFunction() throws Exception {
        assertFalse(FilterParser.parse("round(foo)=1").isEmpty());
    }
    
    @Test
    public void parsesFunction0() throws Exception {
        assertFalse(FilterParser.parse("foo=f()").isEmpty());
    }
}
