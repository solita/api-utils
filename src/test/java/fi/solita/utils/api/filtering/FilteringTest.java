package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.http.HttpModule;
import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Option;
import fi.solita.utils.meta.MetaNamedMember;

public class FilteringTest {
    
    public static class Data {
        String required = "1";
        Option<String> defined = Some("1");
        Option<String> undefined = None();
        List<String> nonemptylist = newList("1");
        List<String> emptylist = emptyList();
        List<List<String>> listofnonemptylist = Arrays.asList(newList("1"));
        List<List<String>> listofemptylist = Arrays.asList(Collections.<String>emptyList());

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((defined == null) ? 0 : defined.hashCode());
            result = prime * result + ((required == null) ? 0 : required.hashCode());
            result = prime * result + ((undefined == null) ? 0 : undefined.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Data other = (Data) obj;
            if (defined == null) {
                if (other.defined != null)
                    return false;
            } else if (!defined.equals(other.defined))
                return false;
            if (required == null) {
                if (other.required != null)
                    return false;
            } else if (!required.equals(other.required))
                return false;
            if (undefined == null) {
                if (other.undefined != null)
                    return false;
            } else if (!undefined.equals(other.undefined))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Data [required=" + required + ", defined=" + defined + ", undefined=" + undefined + "]";
        }
    };
    public static final Data data = new Data();

    private Filtering filtering = new Filtering(new HttpModule(new HttpSerializers(new Serializers()).converters()), ResolvableMemberProvider.NONE, FunctionProvider.NONE);
    
    
    
    @Test
    public void equal() {
        assertEquals(newList(data), newList(filter(filtering.equal(FilteringTest_.Data_.required, Literal.of("1"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.equal(FilteringTest_.Data_.required, Literal.of("2"), String.class), newList(data))));
    }
    
    @Test
    public void equalDefined() {
        assertEquals(newList(data), newList(filter(filtering.equal(FilteringTest_.Data_.defined, Literal.of("1"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.equal(FilteringTest_.Data_.defined, Literal.of("2"), String.class), newList(data))));
    }
    
    @Test
    public void equalUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.equal(FilteringTest_.Data_.undefined, Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.equal(FilteringTest_.Data_.undefined, Literal.of("1"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void notEqual() {
        assertEquals(newList(data), newList(filter(filtering.notEqual(FilteringTest_.Data_.required, Literal.of("2"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notEqual(FilteringTest_.Data_.required, Literal.of("1"), String.class), newList(data))));
    }
    
    @Test
    public void notEqualDefined() {
        assertEquals(newList(data), newList(filter(filtering.notEqual(FilteringTest_.Data_.defined, Literal.of("2"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notEqual(FilteringTest_.Data_.defined, Literal.of("1"), String.class), newList(data))));
    }
    
    @Test
    public void notEqualUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.notEqual(FilteringTest_.Data_.undefined, Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notEqual(FilteringTest_.Data_.undefined, Literal.of("2"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void lt() {
        assertEquals(newList(data), newList(filter(filtering.lt(FilteringTest_.Data_.required, Literal.of("2"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.lt(FilteringTest_.Data_.required, Literal.of("1"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ltDefined() {
        assertEquals(newList(data), newList(filter(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("2"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("1"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ltUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of("2"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void lte() {
        assertEquals(newList(data), newList(filter(filtering.lte(FilteringTest_.Data_.required, Literal.of("1"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.lte(FilteringTest_.Data_.required, Literal.of("0"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void lteDefined() {
        assertEquals(newList(data), newList(filter(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("1"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("0"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void lteUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of("1"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void gt() {
        assertEquals(newList(data), newList(filter(filtering.gt(FilteringTest_.Data_.required, Literal.of("0"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.gt(FilteringTest_.Data_.required, Literal.of("1"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gtDefined() {
        assertEquals(newList(data), newList(filter(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("0"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("1"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gtUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of("0"), String.class), newList(data))));
    }
    

    
    @Test
    public void gte() {
        assertEquals(newList(data), newList(filter(filtering.gte(FilteringTest_.Data_.required, Literal.of("1"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.gte(FilteringTest_.Data_.required, Literal.of("2"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gteDefined() {
        assertEquals(newList(data), newList(filter(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("1"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("2"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gteUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of("1"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void between() {
        assertEquals(newList(data), newList(filter(filtering.between(FilteringTest_.Data_.required, Literal.of("1"), Literal.of("2"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.between(FilteringTest_.Data_.required, Literal.of("2"), Literal.of("3"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void betweenDefined() {
        assertEquals(newList(data), newList(filter(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("1"), Literal.of("2"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("2"), Literal.of("3"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void betweenUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of(null), Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of("1"), Literal.of("2"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void notBetween() {
        assertEquals(newList(data), newList(filter(filtering.notBetween(FilteringTest_.Data_.required, Literal.of("2"), Literal.of("3"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notBetween(FilteringTest_.Data_.required, Literal.of("1"), Literal.of("2"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notBetweenDefined() {
        assertEquals(newList(data), newList(filter(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("2"), Literal.of("3"), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, Literal.of("1"), Literal.of("2"), String.class), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notBetweenUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of(null), Literal.of(null), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, Literal.of("2"), Literal.of("3"), String.class), newList(data))));
    }
    
    
    
    @Test
    public void like() {
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "1"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "%1" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "1%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "%1%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.like(FilteringTest_.Data_.required, "%2%"), newList(data))));
    }
    
    @Test
    public void likeDefined() {
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "1"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "%1" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "1%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.like(FilteringTest_.Data_.required, "%1%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.like(FilteringTest_.Data_.required, "%2%"), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void likeUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1"  ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1%" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%"), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%"), newList(data))));
    }
    
    
    
    @Test
    public void notLike() {
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "2"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "%2" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "2%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "%2%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notLike(FilteringTest_.Data_.required, "%1%"), newList(data))));
    }
    
    @Test
    public void notLikeDefined() {
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "2"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "%2" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "2%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notLike(FilteringTest_.Data_.required, "%2%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notLike(FilteringTest_.Data_.required, "%1%"), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notLikeUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2"  ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2%" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%"), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%"), newList(data))));
    }
    
    
    
    @Test
    public void ilike() {
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "1"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "%1" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "1%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "%1%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.ilike(FilteringTest_.Data_.required, "%2%"), newList(data))));
    }
    
    @Test
    public void ilikeDefined() {
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "1"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "%1" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "1%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.ilike(FilteringTest_.Data_.required, "%1%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.ilike(FilteringTest_.Data_.required, "%2%"), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ilikeUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1"  ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1%" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%"), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%"), newList(data))));
    }
    
    
    
    @Test
    public void notIlike() {
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "2"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "%2" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "2%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "%2%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notILike(FilteringTest_.Data_.required, "%1%"), newList(data))));
    }
    
    @Test
    public void notILikeDefined() {
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "2"  ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "%2" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "2%" ), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.notILike(FilteringTest_.Data_.required, "%2%"), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notILike(FilteringTest_.Data_.required, "%1%"), newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notILikeUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2"  ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2%" ), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%"), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%"), newList(data))));
    }
    
    
    
    @Test
    public void in() {
        assertEquals(newList(data), newList(filter(filtering.in(FilteringTest_.Data_.required, newList(Literal.of("1")), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.in(FilteringTest_.Data_.required, newList(Literal.of("2")), String.class), newList(data))));
    }
    
    @Test
    public void inDefined() {
        assertEquals(newList(data), newList(filter(filtering.in(FilteringTest_.Data_.defined, newList(Literal.of("1")), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.in(FilteringTest_.Data_.defined, newList(Literal.of("2")), String.class), newList(data))));
    }
    
    @Test
    public void inUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.in(FilteringTest_.Data_.undefined, newList(Literal.of(null)), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.in(FilteringTest_.Data_.undefined, newList(Literal.of("1")), String.class), newList(data))));
    }
    
    
    @Test
    public void notIn() {
        assertEquals(newList(data), newList(filter(filtering.notIn(FilteringTest_.Data_.required, newList(Literal.of("2")), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notIn(FilteringTest_.Data_.required, newList(Literal.of("1")), String.class), newList(data))));
    }
    
    @Test
    public void notInDefined() {
        assertEquals(newList(data), newList(filter(filtering.notIn(FilteringTest_.Data_.defined, newList(Literal.of("2")), String.class), newList(data))));
        assertEquals(emptyList()  , newList(filter(filtering.notIn(FilteringTest_.Data_.defined, newList(Literal.of("1")), String.class), newList(data))));
    }
    
    @Test
    public void notInUndefined() {
        assertEquals(emptyList(), newList(filter(filtering.notIn(FilteringTest_.Data_.undefined, newList(Literal.of(null)), String.class), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.notIn(FilteringTest_.Data_.undefined, newList(Literal.of("2")), String.class), newList(data))));
    }
    
    
    
    @Test
    public void isNull() {
        assertEquals(emptyList(), newList(filter(filtering.isNull(FilteringTest_.Data_.defined), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.isNull(FilteringTest_.Data_.nonemptylist), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.isNull(FilteringTest_.Data_.listofnonemptylist), newList(data))));
        
        assertEquals(newList(data), newList(filter(filtering.isNull(FilteringTest_.Data_.undefined), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.isNull(FilteringTest_.Data_.emptylist), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.isNull(FilteringTest_.Data_.listofemptylist), newList(data))));
    }
    
    @Test
    public void isNotNull() {
        assertEquals(newList(data), newList(filter(filtering.isNotNull(FilteringTest_.Data_.defined), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.isNotNull(FilteringTest_.Data_.nonemptylist), newList(data))));
        assertEquals(newList(data), newList(filter(filtering.isNotNull(FilteringTest_.Data_.listofnonemptylist), newList(data))));
        
        assertEquals(emptyList(), newList(filter(filtering.isNotNull(FilteringTest_.Data_.undefined), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.isNotNull(FilteringTest_.Data_.emptylist), newList(data))));
        assertEquals(emptyList(), newList(filter(filtering.isNotNull(FilteringTest_.Data_.listofemptylist), newList(data))));
    }
}
