package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.size;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import fi.solita.utils.api.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Transformer;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtilTests {

    public static class External {
    }
    
    public static class FooDto {
        public String bar;
        public External someId; 
        public String getBar() {
            return bar;
        }
        public final String someOtherMethod() {
            return null;
        }
    }
    
    @SuppressWarnings("unused")
    private static class BarDto {
        public boolean foo;
        public boolean isFoo() {
            return foo;
        }
    }
    
    @Test
    public void allMethods_returnsAllMethods() throws Exception {
        Collection<? extends MetaNamedMember<? super FooDto,?>> methods = MemberUtil.allMethods(FooDto.class);
        
        assertEquals(2, methods.size());
        assertEquals(newSet("getBar", "someOtherMethod"), newSet(map(new Transformer<MetaNamedMember<?,?>, String>() {
            @Override
            public String transform(MetaNamedMember<?, ?> source) {
                return source.getName();
            }
        }, methods)));
    }
    
    @Test
    public void beanGetters_doesNotReturnNonBeanNamed() throws Exception {
        Collection<? extends MetaNamedMember<? super FooDto,?>> methods = MemberUtil.beanGetters(FooDto.class);
        
        assertEquals(1, methods.size());
        assertEquals("bar", head(methods).getName());
    }
    
    @Test
    public void beanGetters_returnsIs() throws Exception {
        Collection<? extends MetaNamedMember<? super BarDto,?>> methods = MemberUtil.beanGetters(BarDto.class);
        
        assertEquals(1, methods.size());
        assertEquals("foo", head(methods).getName());
    }
    
    @Test
    public void resolveIncludes_resolvesField() {
        assertEquals(1, size(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, SerializationFormat.JSON, newList("bar"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, Collections.<MetaNamedMember<FooDto, ?>>emptyList())));
    }
    
    @Test(expected = UnknownPropertyNameException.class)
    public void resolveIncludes_failsForNonIdentifierNonExistent() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, SerializationFormat.JSON, newList("nonexistent"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, Collections.<MetaNamedMember<FooDto, ?>>emptyList())));
    }
    
    @Test
    public void resolveIncludes_resolvesNonExistentIdentifiersAsExternal() {
        ResolvableMemberProvider provider = new ResolvableMemberProvider() {
            @Override
            public boolean isResolvable(MetaNamedMember<?, ?> member) {
                return true;
            }
            
            @Override
            public Type resolveType(MetaNamedMember<?, ?> member) {
                return Type.ExternalUnknown;
            }

            @Override
            public void mutateResolvable(HttpServletRequest request, SortedSet<String> propertyNames, Object apply) {
            }
        };
        assertEquals(1, size(MemberUtil.resolveIncludes(provider, SerializationFormat.JSON, newList("someId.someUnknownField"), newList(MemberUtilTests_.FooDto_.someId), new Builder[] {}, Collections.<MetaNamedMember<FooDto,?>>emptyList())));
    }
}
