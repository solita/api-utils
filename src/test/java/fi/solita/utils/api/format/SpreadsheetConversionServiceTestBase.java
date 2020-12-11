package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Map;

import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.junit.Test;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.Cells;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

public abstract class SpreadsheetConversionServiceTestBase {

    protected abstract Cells<?> serialize(Object o);
    
    @JsonSerializeAsBean
    public static class SomeDto {
        public final Option<Interval> optionInterval = Some(new Interval(DateTime.now(), DateTime.now()));
    }
    
    @JsonSerializeAsBean
    public static class NoneDto {
        public final Option<Interval> optionInterval = None();
    }
    
    @JsonSerializeAsBean
    public static class NullDto {
        public final Option<Interval> nullInterval = null;
    }
    
    @JsonSerializeAsBean
    public static class NestedSomeDto {
        public final Option<SomeDto> nested = Some(new SomeDto());
    }
    
    @JsonSerializeAsBean
    public static class NestedNoneDto {
        public final Option<NoneDto> nested = Some(new NoneDto());
    }
    
    @JsonSerializeAsBean
    public static class NestedNullDto {
        public final Option<NullDto> nested = Some(new NullDto());
    }
    
    @JsonSerializeAsBean
    public static class CollectionDto {
        public final Collection<String> collectionStrings = newList("a", "b");
    }
    
    @JsonSerializeAsBean
    public static class CollectionEmptyDto {
        public final Collection<String> collectionEmpty = newMutableList();
    }
    
    @JsonSerializeAsBean
    public static class CollectionMulticolumnDto {
        public final Collection<Interval> collectionIntervals = newList(new Interval(DateTime.now(), DateTime.now()), new Interval(DateTime.now(), DateTime.now()));
    }
    
    @JsonSerializeAsBean
    public static class CollectionSomeMulticolumnDto {
        public final Collection<Option<Interval>> collectionSomeIntervals = newList(Some(new Interval(DateTime.now(), DateTime.now())), Some(new Interval(DateTime.now(), DateTime.now())));
    }
    
    @JsonSerializeAsBean
    public static class CollectionNoneMulticolumnDto {
        public final Collection<Option<Interval>> collectionNoneIntervals = newList(Option.<Interval>None(), Option.<Interval>None());
    }
    
    @JsonSerializeAsBean
    public static class TupleMapDto {
        public final Map<String,Interval> map = newMap(Pair.of("a", new Interval(DateTime.now(), DateTime.now())));
    }
    
    @Test
    public void otsikotOikeinSomelle() {
        Cells<?> res = serialize(new SomeDto());
        assertEquals(2, res.cells.size());
        assertEquals(newList("optionInterval alku", "optionInterval loppu"), res.headers);
    }
    
    @Test
    public void otsikotOikeinNonelle() {
        Cells<?> res = serialize(new NoneDto());
        assertEquals(2, res.cells.size());
        assertEquals(newList("optionInterval alku", "optionInterval loppu"), res.headers);
    }
    
    @Test
    public void otsikotOikeinNullille() {
        Cells<?> res = serialize(new NullDto());
        assertEquals(2, res.cells.size());
        assertEquals(newList("nullInterval alku", "nullInterval loppu"), res.headers);
    }
    
    @Test
    public void otsikotOikeinNestatulleSomeDtolle() {
        Cells<?> res = serialize(new NestedSomeDto());
        assertEquals(2, res.cells.size());
        assertEquals(newList("nested optionInterval alku", "nested optionInterval loppu"), res.headers);
    }
    
    @Test
    public void otsikotOikeinNestatulleNoneDtolle() {
        Cells<?> res = serialize(new NestedNoneDto());
        assertEquals(2, res.cells.size());
        assertEquals(newList("nested optionInterval alku", "nested optionInterval loppu"), res.headers);
    }
    
    @Test
    public void otsikotOikeinNestatulleNullDtolle() {
        Cells<?> res = serialize(new NestedNullDto());
        assertEquals(2, res.cells.size());
        assertEquals(newList("nested nullInterval alku", "nested nullInterval loppu"), res.headers);
    }
    
    @Test
    public void otsikotOikeinCollectionille() {
        Cells<?> res = serialize(new CollectionDto());
        assertEquals(1, res.cells.size());
        assertEquals(newList("collectionStrings"), res.headers);
    }
    
    @Test
    public void otsikotOikeinTyhjalleCollectionille() {
        Cells<?> res = serialize(new CollectionEmptyDto());
        assertEquals(1, res.cells.size());
        assertEquals(newList("collectionEmpty"), res.headers);
    }
    
    @Test
    public void otsikotOikeinMonisarakkeistenCollectionille() {
        Cells<?> res = serialize(new CollectionMulticolumnDto());
        assertEquals(1, res.cells.size());
        assertEquals(newList("collectionIntervals"), res.headers);
    }
    
    @Test
    public void otsikotOikeinMonisarakkeistenSomeCollectionille() {
        Cells<?> res = serialize(new CollectionSomeMulticolumnDto());
        assertEquals(1, res.cells.size());
        assertEquals(newList("collectionSomeIntervals"), res.headers);
    }
    
    @Test
    public void otsikotOikeinMonisarakkeistenNoneCollectionille() {
        Cells<?> res = serialize(new CollectionNoneMulticolumnDto());
        assertEquals(1, res.cells.size());
        assertEquals(newList("collectionNoneIntervals"), res.headers);
    }
    
    @Test
    public void otsikotOikeinMapille() {
        Cells<?> res = serialize(new TupleMapDto());
        assertEquals(1, res.cells.size());
        assertEquals(newList("map"), res.headers);
    }
}
