package fi.solita.utils.api.types;

import static fi.solita.utils.api.types.Filters.EQUAL;
import static fi.solita.utils.api.types.Filters.INTERSECTS;
import static fi.solita.utils.functional.Option.Some;
import static org.junit.Assert.*;

import org.joda.time.DateTime;
import org.junit.Test;

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.Filters.Filter;
import fi.solita.utils.functional.Option;

public class FiltersTest {
    
    private Option<Filters> someFilter(String value) {
        return Some(new Filters(new Filter(EQUAL, "foo", value)));
    }
    
    @Test
    public void parsesIntegers() {
        assertEquals(someFilter("1"), Filters.parse("foo=1"));
        assertEquals(someFilter("-1"), Filters.parse("foo=-1"));
    }

    @Test
    public void parsesDecimals() {
        assertEquals(someFilter("1.1"), Filters.parse("foo=1.1"));
        assertEquals(someFilter("-1.1"), Filters.parse("foo=-1.1"));
    }
    
    @Test
    public void parsesBooleans() {
        assertEquals(someFilter("true"), Filters.parse("foo=true"));
        assertEquals(someFilter("false"), Filters.parse("foo=false"));
    }
    
    @Test
    public void parsesStrings() {
        assertEquals(someFilter(""), Filters.parse("foo=''"));
        assertEquals(someFilter("a"), Filters.parse("foo='a'"));
        assertEquals(someFilter("'a'b'"), Filters.parse("foo='''a''b'''"));
        assertEquals(someFilter(";"), Filters.parse("foo=';'"));
    }
    
    @Test
    public void parsesTimes() {
        assertEquals(someFilter(DateTime.now().toString(Serializers.DATETIME_FORMAT)), Filters.parse("foo=" + DateTime.now().toString(Serializers.DATETIME_FORMAT)));
    }
    
    @Test
    public void parsesDuration() {
        assertEquals(someFilter("P0Y0M0DT0H0M1S"), Filters.parse("foo=P0Y0M0DT0H0M1S"));
    }
    
    @Test
    public void parsesPolygon() {
        assertEquals(Some(new Filters(new Filter(INTERSECTS, "foo", "POLYGON((30 10,40 40,20 40,10 20,30 10))"))), Filters.parse("INTERSECTS(foo,POLYGON((30 10,40 40,20 40,10 20,30 10)))"));
    }
    
    @Test
    public void parsesMultiple() {
        assertEquals(Some(new Filters(new Filter(EQUAL, "foo", "1"), new Filter(EQUAL, "foo", "2"))), Filters.parse("foo=1 AND foo=2"));
        assertEquals(Some(new Filters(new Filter(EQUAL, "foo", "1"), new Filter(EQUAL, "foo", "2"), new Filter(EQUAL, "foo", "3"))), Filters.parse("foo=1 AND foo=2 AND foo=3"));
        assertEquals(Some(new Filters(new Filter(EQUAL, "foo", "1"), new Filter(EQUAL, "foo", "2"), new Filter(EQUAL, "foo", "3"), new Filter(EQUAL, "foo", "4"), new Filter(EQUAL, "foo", "5"))), Filters.parse("foo=1 AND foo=2 AND foo=3 AND foo=4 AND foo=5"));
    }
    
    @Test
    public void parseEQUAL() {
        assertTrue(Filters.parse("foo=1").isDefined());
    }
    
    @Test
    public void parseNOTEQUAL() {
        assertTrue(Filters.parse("foo<>1").isDefined());
    }
    
    @Test
    public void parseLT() {
        assertTrue(Filters.parse("foo<1").isDefined());
    }
    
    @Test
    public void parseGT() {
        assertTrue(Filters.parse("foo>1").isDefined());
    }
    
    @Test
    public void parseLTE() {
        assertTrue(Filters.parse("foo<=1").isDefined());
    }
    
    @Test
    public void parseGTE() {
        assertTrue(Filters.parse("foo>=1").isDefined());
    }
    
    @Test
    public void parseBETWEEN() {
        assertTrue(Filters.parse("foo BETWEEN 1 AND 2").isDefined());
    }
    
    @Test
    public void parseNOTBETWEEN() {
        assertTrue(Filters.parse("foo NOT BETWEEN 1 AND 2").isDefined());
    }
    
    @Test
    public void parseLIKE() {
        assertTrue(Filters.parse("foo LIKE 'a'").isDefined());
    }
    
    @Test
    public void parseNOTLIKE() {
        assertTrue(Filters.parse("foo NOT LIKE 'a'").isDefined());
    }
    
    @Test
    public void parseILIKE() {
        assertTrue(Filters.parse("foo ILIKE 'a'").isDefined());
    }
    
    @Test
    public void parseNOTILIKE() {
        assertTrue(Filters.parse("foo NOT ILIKE 'a'").isDefined());
    }
    
    @Test
    public void parseIN() {
        assertTrue(Filters.parse("foo IN (1,2)").isDefined());
    }
    
    @Test
    public void parseNOTIN() {
        assertTrue(Filters.parse("foo NOT IN (1,2)").isDefined());
    }
    
    @Test
    public void parseNULL() {
        assertTrue(Filters.parse("foo IS NULL").isDefined());
    }
    
    @Test
    public void parseNOTNULL() {
        assertTrue(Filters.parse("foo IS NOT NULL").isDefined());
    }
    
    @Test
    public void parseINTERSECTS() {
        assertTrue(Filters.parse("INTERSECTS(foo,POLYGON((30 10,40 40,20 40,10 20, 30 10)))").isDefined());
    }
}
