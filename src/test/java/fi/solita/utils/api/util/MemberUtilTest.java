package fi.solita.utils.api.util;

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

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.functions.FunctionCallMember;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.functions.FunctionProvider.UnknownFunctionException;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider.Type;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.util.MemberUtilTest.FooDto;
import fi.solita.utils.api.util.MemberUtilTest_.FooDto_;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Transformer;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaFieldProperty;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtilTest {

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
        public void mutateResolvable(HttpServletRequest request, SortedSet<PropertyName> propertyNames, Object apply) {
        }
    };
    
    public static class FooDto {
        public static final List<? extends MetaNamedMember<FooDto,?>> FIELDS = Tuple.asList(FooDto_.$Fields());
        public static final Builder<FooDto> BUILDER = Builder.of(FooDto_.$Fields(), FooDto_.$);
        public static final Builder<?>[] BUILDERS = { BUILDER };
        
        public String bar;
        public External someId; 
        public Double baz;
        
        public FooDto(String bar, External someId, Double baz) {
            this.bar = bar;
            this.someId = someId;
            this.baz = baz;
        }
        
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
        List<MetaFieldProperty<FooDto, String>> members = newList(MemberUtilTest_.FooDto_.bar);
        assertEquals(members, MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, null, members, new Builder[] {}, noGeometries).includes);
    }
    
    @Test
    public void resolveIncludes_returnsNothingForEmptyPropertyNames() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, Collections.<PropertyName>emptyList(), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries).includes));
    }
    
    @Test
    public void resolveIncludes_returnsNothingForSingletonEmptyStringPropertyName() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("")), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries).includes));
    }
    
    @Test
    public void resolveIncludes_excludesGivenExclusionFromAll() {
        List<MetaFieldProperty<FooDto,?>> input = newList(MemberUtilTest_.FooDto_.bar, MemberUtilTest_.FooDto_.someId);
        List<MetaFieldProperty<FooDto,String>> output = newList(MemberUtilTest_.FooDto_.bar);
        assertEquals(output, MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("-someId")), input, new Builder[] {}, noGeometries).includes);
    }
    
    @Test(expected = MemberUtil.RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicates() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("bar"),new PropertyName("bar")), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test(expected = MemberUtil.RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicateResolvables() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("bar.a"),new PropertyName("bar.a")), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test(expected = MemberUtil.RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicateExclusions() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("-bar"),new PropertyName("-bar")), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test
    public void resolveIncludes_resolvesField() {
        assertEquals(1, size(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("bar")), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries)));
    }
    
    @Test(expected = UnknownPropertyNameException.class)
    public void resolveIncludes_failsForNonIdentifierNonExistent() {
        assertTrue(isEmpty(MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(new PropertyName("nonexistent")), newList(MemberUtilTest_.FooDto_.bar), new Builder[] {}, noGeometries)));
    }
    
    @Test
    public void resolveIncludes_resolvesNonExistentIdentifiersAsExternal() {
        assertEquals(1, size(MemberUtil.resolveIncludes(externalProvider, FunctionProvider.NONE, someFormat, newList(new PropertyName("someId.someUnknownField")), newList(MemberUtilTest_.FooDto_.someId), new Builder[] {}, noGeometries)));
    }
    
    @Test
    public void resolveIncludes_resolvedAreGrouped() {
        List<MetaFieldProperty<FooDto,?>> input = newList(MemberUtilTest_.FooDto_.bar, MemberUtilTest_.FooDto_.someId);
        assertEquals(2, size(MemberUtil.resolveIncludes(externalProvider, FunctionProvider.NONE, someFormat, newList(new PropertyName("someId.someUnknownField"),new PropertyName("bar"),new PropertyName("someId.someOtherUnknownField")), input, new Builder[] {}, noGeometries)));
    }
    
    
    @Test
    public void resolveIncludes_handlesRound() {
        Includes<FooDto> includes = MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, new FunctionProvider(), someFormat, newList(new PropertyName("round(baz)")), newList(MemberUtilTest_.FooDto_.baz), new Builder[] {}, noGeometries);
        assertTrue(Assert.singleton(includes.includes) instanceof FunctionCallMember);
    }
    
    @Test(expected = FunctionProvider.UnknownFunctionException.class)
    public void resolveIncludes_failsForUnknownFunction() {
        MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, new FunctionProvider(), someFormat, newList(new PropertyName("unknown_function(baz)")), newList(MemberUtilTest_.FooDto_.baz), new Builder[] {}, noGeometries);
    }
    
    @Test
    public void withPropertiesF_appliesFunction() {
        Includes<FooDto> includes = MemberUtil.resolveIncludes(ResolvableMemberProvider.NONE, new FunctionProvider(), someFormat, newList(new PropertyName("round(baz)")), FooDto.FIELDS, FooDto.BUILDERS, noGeometries);
        Function1<FooDto, FooDto> mapper = MemberUtil.withPropertiesF(includes, new FunctionProvider());
        FooDto foo = new FooDto("", new External(), 3.14);
        assertEquals(Double.valueOf(3.0), mapper.apply(foo).baz);
    }
}
