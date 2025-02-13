package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.last;
import static org.junit.Assert.*;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import fi.solita.utils.api.functions.FunctionCallMember;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMember;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.MemberUtilTest_.FooDto_;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtilTest {
    
    private static ResolvableMemberProvider<?> externalProvider = new ResolvableMemberProvider<Object>() {
        @Override
        public boolean isResolvable(MetaNamedMember<?, ?> member) {
            return true;
        }
        
        @Override
        public Type resolveType(MetaNamedMember<?, ?> member) {
            return Type.ExternalUnknown;
        }

        @Override
        public void mutateResolvable(Object request, Set<PropertyName> propertyNames, Object apply) {
        }
    };
    
    public static class FooDto {
        public static final List<? extends MetaNamedMember<FooDto,?>> FIELDS = Tuple.asList(FooDto_.$Fields());
        public static final Builder<FooDto> BUILDER = Builder.of(FooDto_.$Fields(), FooDto_.$);
        public static final Builder<?>[] BUILDERS = { BUILDER };
        
        public String bar;
        
        public Option<Set<String>> baz;
        
        public FooDto(String bar, Option<Set<String>> baz) {
            this.bar = bar;
            this.baz = baz;
        }
    }

    @Test
    public void toMembers_resolvableMemberIsResolved() {
        List<? extends MetaNamedMember<? super MemberUtilTest.FooDto,?>> members = MemberUtil.toMembers(externalProvider, FunctionProvider.NONE, false, FooDto.FIELDS, PropertyName.of("bar.baz"));
        MetaNamedMember<? super FooDto, ?> member = Assert.singleton(members);
        Assert.True(member instanceof ResolvableMember);
        assertEquals(newList(PropertyName.of("baz")), newList(((ResolvableMember<?>)member).getResolvablePropertyNames()));
    }
    
    @Test
    public void toMembers_functionCallMemberIsResolved() {
        List<? extends MetaNamedMember<? super MemberUtilTest.FooDto,?>> members = MemberUtil.toMembers(externalProvider, new FunctionProvider(), false, FooDto.FIELDS, PropertyName.of("round(bar)"));
        MetaNamedMember<? super FooDto, ?> member = Assert.singleton(members);
        Assert.True(member instanceof FunctionCallMember);
        assertEquals(PropertyName.of("round(bar)"), ((FunctionCallMember<?>)member).propertyName);
    }
    
    @Test
    public void actualTypeUnwrappingOptionAndEitherAndIterables() {
        List<MetaNamedMember<MemberUtilTest.FooDto,?>> members = (List<MetaNamedMember<FooDto, ?>>) MemberUtil.toMembers(externalProvider, new FunctionProvider(), false, FooDto.FIELDS, PropertyName.of("baz"));
        Class<?> ret = MemberUtil.actualTypeUnwrappingOptionAndEitherAndIterables(head(members));
        assertEquals(String.class, ret);
    }
}
