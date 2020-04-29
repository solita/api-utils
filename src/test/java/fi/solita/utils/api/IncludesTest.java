package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.size;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import fi.solita.utils.api.IncludesTest_.FooDto_;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.functions.FunctionCallMember;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.util.ModificationUtils;
import fi.solita.utils.api.util.RedundantPropertiesException;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaFieldProperty;
import fi.solita.utils.meta.MetaNamedMember;

public class IncludesTest {

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
    public void resolveIncludes_returnsAllForNullPropertyNames() {
        List<MetaFieldProperty<FooDto, String>> members = newList(FooDto_.bar);
        assertEquals(members, Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, null, members, new Builder[] {}, noGeometries).includes);
    }
    
    @Test
    public void resolveIncludes_returnsNothingForEmptyPropertyNames() {
        assertTrue(isEmpty(Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, Collections.<PropertyName>emptyList(), newList(FooDto_.bar), new Builder[] {}, noGeometries).includes));
    }
    
    @Test
    public void resolveIncludes_returnsNothingForSingletonEmptyStringPropertyName() {
        assertTrue(isEmpty(Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("")), newList(FooDto_.bar), new Builder[] {}, noGeometries).includes));
    }
    
    @Test
    public void resolveIncludes_excludesGivenExclusionFromAll() {
        List<MetaFieldProperty<FooDto,?>> input = newList(FooDto_.bar, FooDto_.someId);
        List<MetaFieldProperty<FooDto,String>> output = newList(FooDto_.bar);
        assertEquals(output, Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("-someId")), input, new Builder[] {}, noGeometries).includes);
    }
    
    @Test(expected = RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicates() {
        Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("bar"),PropertyName.of("bar")), newList(FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test(expected = RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicateResolvables() {
        Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("bar.a"),PropertyName.of("bar.a")), newList(FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test(expected = RedundantPropertiesException.class)
    public void resolveIncludes_failsForDuplicateExclusions() {
        Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("-bar"),PropertyName.of("-bar")), newList(FooDto_.bar), new Builder[] {}, noGeometries);
    }
    
    @Test
    public void resolveIncludes_resolvesField() {
        assertEquals(1, size(Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("bar")), newList(FooDto_.bar), new Builder[] {}, noGeometries)));
    }
    
    @Test(expected = UnknownPropertyNameException.class)
    public void resolveIncludes_failsForNonIdentifierNonExistent() {
        assertTrue(isEmpty(Includes.resolveIncludes(ResolvableMemberProvider.NONE, FunctionProvider.NONE, someFormat, newList(PropertyName.of("nonexistent")), newList(FooDto_.bar), new Builder[] {}, noGeometries)));
    }
    
    @Test
    public void resolveIncludes_resolvesNonExistentIdentifiersAsExternal() {
        assertEquals(1, size(Includes.resolveIncludes(externalProvider, FunctionProvider.NONE, someFormat, newList(PropertyName.of("someId.someUnknownField")), newList(FooDto_.someId), new Builder[] {}, noGeometries)));
    }
    
    @Test
    public void resolveIncludes_resolvedAreGrouped() {
        List<MetaFieldProperty<FooDto,?>> input = newList(FooDto_.bar, FooDto_.someId);
        assertEquals(2, size(Includes.resolveIncludes(externalProvider, FunctionProvider.NONE, someFormat, newList(PropertyName.of("someId.someUnknownField"),PropertyName.of("bar"),PropertyName.of("someId.someOtherUnknownField")), input, new Builder[] {}, noGeometries)));
    }
    
    
    @Test
    public void resolveIncludes_handlesRound() {
        Includes<FooDto> includes = Includes.resolveIncludes(ResolvableMemberProvider.NONE, new FunctionProvider(), someFormat, newList(PropertyName.of("round(baz)")), newList(FooDto_.baz), new Builder[] {}, noGeometries);
        assertTrue(Assert.singleton(includes.includes) instanceof FunctionCallMember);
    }
    
    @Test(expected = FunctionProvider.UnknownFunctionException.class)
    public void resolveIncludes_failsForUnknownFunction() {
        Includes.resolveIncludes(ResolvableMemberProvider.NONE, new FunctionProvider(), someFormat, newList(PropertyName.of("unknown_function(baz)")), newList(FooDto_.baz), new Builder[] {}, noGeometries);
    }
    
    @Test
    public void withPropertiesF_appliesFunction() {
        Includes<FooDto> includes = Includes.resolveIncludes(ResolvableMemberProvider.NONE, new FunctionProvider(), someFormat, newList(PropertyName.of("round(baz)")), FooDto.FIELDS, FooDto.BUILDERS, noGeometries);
        Function1<FooDto, FooDto> mapper = ModificationUtils.withPropertiesF(includes, new FunctionProvider());
        FooDto foo = new FooDto("", new External(), 3.14);
        assertEquals(Double.valueOf(3.0), mapper.apply(foo).baz);
    }
}
