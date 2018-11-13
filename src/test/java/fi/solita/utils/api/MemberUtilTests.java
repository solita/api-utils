package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.size;
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.List;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import fi.solita.utils.api.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Transformer;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaFieldProperty;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtilTests {

    private static final List<MetaNamedMember<FooDto, ?>> noGeometries = Collections.<MetaNamedMember<FooDto, ?>>emptyList();
    private static final SerializationFormat someFormat = SerializationFormat.JSON;

    public static class External {
    }
    
    private static ResolvableMemberProvider externalProvider = new ResolvableMemberProvider() {
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
    public void resolveIncludes_returnsAllForNullPropertyNames() {
        List<MetaFieldProperty<FooDto, String>> members = newList(MemberUtilTests_.FooDto_.bar);
        assertEquals(members, MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, null, members, new Builder[] {}, noGeometries).includes);
    }
    
    @Test
    public void resolveIncludes_returnsNothingForEmptyPropertyNames() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, Collections.<String>emptyList(), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries).includes));
    }
    
    @Test
    public void resolveIncludes_returnsNothingForSingletonEmptyStringPropertyName() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList(""), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries).includes));
    }
    
    @Test
    public void resolveIncludes_excludesGivenExclusionFromAll() {
        List<MetaFieldProperty<FooDto,?>> input = newList(MemberUtilTests_.FooDto_.bar, MemberUtilTests_.FooDto_.someId);
        List<MetaFieldProperty<FooDto,String>> output = newList(MemberUtilTests_.FooDto_.bar);
        assertEquals(output, MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList("-someId"), input, new Builder[] {}, noGeometries).includes);
    }
    
    @Test(expected = MemberUtil.RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicates() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList("bar","bar"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test(expected = MemberUtil.RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicateResolvables() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList("bar.a","bar.a"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test(expected = MemberUtil.RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicateExclusions() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList("-bar","-bar"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test
    public void resolveIncludes_resolvesField() {
        assertEquals(1, size(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList("bar"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries)));
    }
    
    @Test(expected = UnknownPropertyNameException.class)
    public void resolveIncludes_failsForNonIdentifierNonExistent() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, someFormat, newList("nonexistent"), newList(MemberUtilTests_.FooDto_.bar), new Builder[] {}, noGeometries)));
    }
    
    @Test
    public void resolveIncludes_resolvesNonExistentIdentifiersAsExternal() {
        assertEquals(1, size(MemberUtil.resolveIncludes(externalProvider, someFormat, newList("someId.someUnknownField"), newList(MemberUtilTests_.FooDto_.someId), new Builder[] {}, noGeometries)));
    }
    
    @Test
    public void resolveIncludes_resolvedAreGrouped() {
        List<MetaFieldProperty<FooDto,?>> input = newList(MemberUtilTests_.FooDto_.bar, MemberUtilTests_.FooDto_.someId);
        assertEquals(2, size(MemberUtil.resolveIncludes(externalProvider, someFormat, newList("someId.someUnknownField","bar","someId.someOtherUnknownField"), input, new Builder[] {}, noGeometries)));
    }
}
