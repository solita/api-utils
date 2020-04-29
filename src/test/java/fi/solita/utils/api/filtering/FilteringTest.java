package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static org.junit.Assert.*;

import org.junit.Test;

import fi.solita.utils.api.base.Serializers;
import fi.solita.utils.api.base.http.HttpModule;
import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.filtering.Filtering;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.functional.Option;
import fi.solita.utils.meta.MetaNamedMember;

public class FilteringTest {
    
    public static class Data {
        String required = "1";
        Option<String> defined = Some("1");
        Option<String> undefined = None();

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
        assertEquals(newList(data), newList(filtering.equal(FilteringTest_.Data_.required, "1", newList(data))));
        assertEquals(emptyList()  , newList(filtering.equal(FilteringTest_.Data_.required, "2", newList(data))));
    }
    
    @Test
    public void equalDefined() {
        assertEquals(newList(data), newList(filtering.equal(FilteringTest_.Data_.defined, "1", newList(data))));
        assertEquals(emptyList()  , newList(filtering.equal(FilteringTest_.Data_.defined, "2", newList(data))));
    }
    
    @Test
    public void equalUndefined() {
        assertEquals(emptyList(), newList(filtering.equal(FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.equal(FilteringTest_.Data_.undefined, "1", newList(data))));
    }
    
    
    
    @Test
    public void notEqual() {
        assertEquals(newList(data), newList(filtering.notEqual(FilteringTest_.Data_.required, "2", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notEqual(FilteringTest_.Data_.required, "1", newList(data))));
    }
    
    @Test
    public void notEqualDefined() {
        assertEquals(newList(data), newList(filtering.notEqual(FilteringTest_.Data_.defined, "2", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notEqual(FilteringTest_.Data_.defined, "1", newList(data))));
    }
    
    @Test
    public void notEqualUndefined() {
        assertEquals(emptyList(), newList(filtering.notEqual(FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.notEqual(FilteringTest_.Data_.undefined, "2", newList(data))));
    }
    
    
    
    @Test
    public void lt() {
        assertEquals(newList(data), newList(filtering.lt(FilteringTest_.Data_.required, "2", newList(data))));
        assertEquals(emptyList()  , newList(filtering.lt(FilteringTest_.Data_.required, "1", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ltDefined() {
        assertEquals(newList(data), newList(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "2", newList(data))));
        assertEquals(emptyList()  , newList(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "1", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ltUndefined() {
        assertEquals(emptyList(), newList(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.lt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2", newList(data))));
    }
    
    
    
    @Test
    public void lte() {
        assertEquals(newList(data), newList(filtering.lte(FilteringTest_.Data_.required, "1", newList(data))));
        assertEquals(emptyList()  , newList(filtering.lte(FilteringTest_.Data_.required, "0", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void lteDefined() {
        assertEquals(newList(data), newList(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "1", newList(data))));
        assertEquals(emptyList()  , newList(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "0", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void lteUndefined() {
        assertEquals(emptyList(), newList(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.lte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1", newList(data))));
    }
    
    
    
    @Test
    public void gt() {
        assertEquals(newList(data), newList(filtering.gt(FilteringTest_.Data_.required, "0", newList(data))));
        assertEquals(emptyList()  , newList(filtering.gt(FilteringTest_.Data_.required, "1", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gtDefined() {
        assertEquals(newList(data), newList(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "0", newList(data))));
        assertEquals(emptyList()  , newList(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "1", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gtUndefined() {
        assertEquals(emptyList(), newList(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.gt((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "0", newList(data))));
    }
    

    
    @Test
    public void gte() {
        assertEquals(newList(data), newList(filtering.gte(FilteringTest_.Data_.required, "1", newList(data))));
        assertEquals(emptyList()  , newList(filtering.gte(FilteringTest_.Data_.required, "2", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gteDefined() {
        assertEquals(newList(data), newList(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "1", newList(data))));
        assertEquals(emptyList()  , newList(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "2", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void gteUndefined() {
        assertEquals(emptyList(), newList(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.gte((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1", newList(data))));
    }
    
    
    
    @Test
    public void between() {
        assertEquals(newList(data), newList(filtering.between(FilteringTest_.Data_.required, "1", "2", newList(data))));
        assertEquals(emptyList()  , newList(filtering.between(FilteringTest_.Data_.required, "2", "3", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void betweenDefined() {
        assertEquals(newList(data), newList(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "1", "2", newList(data))));
        assertEquals(emptyList()  , newList(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "2", "3", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void betweenUndefined() {
        assertEquals(emptyList(), newList(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, null, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.between((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1", "2", newList(data))));
    }
    
    
    
    @Test
    public void notBetween() {
        assertEquals(newList(data), newList(filtering.notBetween(FilteringTest_.Data_.required, "2", "3", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notBetween(FilteringTest_.Data_.required, "1", "2", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notBetweenDefined() {
        assertEquals(newList(data), newList(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "2", "3", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.defined, "1", "2", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notBetweenUndefined() {
        assertEquals(emptyList(), newList(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, null, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.notBetween((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2", "3", newList(data))));
    }
    
    
    
    @Test
    public void like() {
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "1"  , newList(data))));
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "%1" , newList(data))));
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "1%" , newList(data))));
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "%1%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.like(FilteringTest_.Data_.required, "%2%", newList(data))));
    }
    
    @Test
    public void likeDefined() {
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "1"  , newList(data))));
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "%1" , newList(data))));
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "1%" , newList(data))));
        assertEquals(newList(data), newList(filtering.like(FilteringTest_.Data_.required, "%1%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.like(FilteringTest_.Data_.required, "%2%", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void likeUndefined() {
        assertEquals(emptyList(), newList(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1"  , newList(data))));
        assertEquals(emptyList(), newList(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1" , newList(data))));
        assertEquals(emptyList(), newList(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1%" , newList(data))));
        assertEquals(emptyList(), newList(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%", newList(data))));
        assertEquals(emptyList(), newList(filtering.like((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%", newList(data))));
    }
    
    
    
    @Test
    public void notLike() {
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "2"  , newList(data))));
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "%2" , newList(data))));
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "2%" , newList(data))));
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "%2%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notLike(FilteringTest_.Data_.required, "%1%", newList(data))));
    }
    
    @Test
    public void notLikeDefined() {
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "2"  , newList(data))));
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "%2" , newList(data))));
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "2%" , newList(data))));
        assertEquals(newList(data), newList(filtering.notLike(FilteringTest_.Data_.required, "%2%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notLike(FilteringTest_.Data_.required, "%1%", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notLikeUndefined() {
        assertEquals(emptyList(), newList(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2"  , newList(data))));
        assertEquals(emptyList(), newList(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2" , newList(data))));
        assertEquals(emptyList(), newList(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2%" , newList(data))));
        assertEquals(emptyList(), newList(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%", newList(data))));
        assertEquals(emptyList(), newList(filtering.notLike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%", newList(data))));
    }
    
    
    
    @Test
    public void ilike() {
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "1"  , newList(data))));
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "%1" , newList(data))));
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "1%" , newList(data))));
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "%1%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.ilike(FilteringTest_.Data_.required, "%2%", newList(data))));
    }
    
    @Test
    public void ilikeDefined() {
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "1"  , newList(data))));
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "%1" , newList(data))));
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "1%" , newList(data))));
        assertEquals(newList(data), newList(filtering.ilike(FilteringTest_.Data_.required, "%1%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.ilike(FilteringTest_.Data_.required, "%2%", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void ilikeUndefined() {
        assertEquals(emptyList(), newList(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1"  , newList(data))));
        assertEquals(emptyList(), newList(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1" , newList(data))));
        assertEquals(emptyList(), newList(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "1%" , newList(data))));
        assertEquals(emptyList(), newList(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%", newList(data))));
        assertEquals(emptyList(), newList(filtering.ilike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%", newList(data))));
    }
    
    
    
    @Test
    public void notIlike() {
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "2"  , newList(data))));
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "%2" , newList(data))));
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "2%" , newList(data))));
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "%2%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notILike(FilteringTest_.Data_.required, "%1%", newList(data))));
    }
    
    @Test
    public void notILikeDefined() {
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "2"  , newList(data))));
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "%2" , newList(data))));
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "2%" , newList(data))));
        assertEquals(newList(data), newList(filtering.notILike(FilteringTest_.Data_.required, "%2%", newList(data))));
        assertEquals(emptyList()  , newList(filtering.notILike(FilteringTest_.Data_.required, "%1%", newList(data))));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void notILikeUndefined() {
        assertEquals(emptyList(), newList(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2"  , newList(data))));
        assertEquals(emptyList(), newList(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2" , newList(data))));
        assertEquals(emptyList(), newList(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "2%" , newList(data))));
        assertEquals(emptyList(), newList(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%2%", newList(data))));
        assertEquals(emptyList(), newList(filtering.notILike((MetaNamedMember<Data,String>)(Object)FilteringTest_.Data_.undefined, "%1%", newList(data))));
    }
    
    
    
    @Test
    public void in() {
        assertEquals(newList(data), newList(filtering.in(FilteringTest_.Data_.required, newList("1"), newList(data))));
        assertEquals(emptyList()  , newList(filtering.in(FilteringTest_.Data_.required, newList("2"), newList(data))));
    }
    
    @Test
    public void inDefined() {
        assertEquals(newList(data), newList(filtering.in(FilteringTest_.Data_.defined, newList("1"), newList(data))));
        assertEquals(emptyList()  , newList(filtering.in(FilteringTest_.Data_.defined, newList("2"), newList(data))));
    }
    
    @Test
    public void inUndefined() {
        assertEquals(emptyList(), newList(filtering.in(FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.in(FilteringTest_.Data_.undefined, newList("1"), newList(data))));
    }
    
    
    @Test
    public void notIn() {
        assertEquals(newList(data), newList(filtering.notIn(FilteringTest_.Data_.required, newList("2"), newList(data))));
        assertEquals(emptyList()  , newList(filtering.notIn(FilteringTest_.Data_.required, newList("1"), newList(data))));
    }
    
    @Test
    public void notInDefined() {
        assertEquals(newList(data), newList(filtering.notIn(FilteringTest_.Data_.defined, newList("2"), newList(data))));
        assertEquals(emptyList()  , newList(filtering.notIn(FilteringTest_.Data_.defined, newList("1"), newList(data))));
    }
    
    @Test
    public void notInUndefined() {
        assertEquals(emptyList(), newList(filtering.notIn(FilteringTest_.Data_.undefined, null, newList(data))));
        assertEquals(emptyList(), newList(filtering.notIn(FilteringTest_.Data_.undefined, newList("2"), newList(data))));
    }
}
