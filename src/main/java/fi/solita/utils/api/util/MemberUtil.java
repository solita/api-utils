package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.collect.Ordering;

import fi.solita.utils.api.NestedMember;
import fi.solita.utils.api.functions.FunctionCallMember;
import fi.solita.utils.api.functions.FunctionCallMember_;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMember;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider_;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtil {
    
    public static class UnknownPropertyNameException extends RuntimeException {
        public final PropertyName propertyName;
    
        public UnknownPropertyNameException(PropertyName propertyName) {
            super(propertyName.toString());
            this.propertyName = propertyName;
        }
    }
    
    public static Class<?> memberTypeUnwrappingOptionAndEither(AccessibleObject member) {
        Class<?> type = member instanceof Field ? ((Field)member).getType() : ((Method)member).getReturnType();
        if (Option.class.isAssignableFrom(type)) {
            return ClassUtils.typeClass(member instanceof Field ? ((Field)member).getGenericType() : ((Method)member).getGenericReturnType());
        } else if (Either.class.isAssignableFrom(type)) {
            return ClassUtils.typeClass(member instanceof Field ? ((Field)member).getGenericType() : ((Method)member).getGenericReturnType());
        }
        return type;
    }
    
    public static <T> Class<?> actualTypeUnwrappingOptionAndEither(final MetaNamedMember<T, ?> member) {
        if (member instanceof NestedMember) {
            Class<?> par = memberTypeUnwrappingOptionAndEither(((NestedMember<?,?>) member).parent.getMember());
            if (Iterable.class.isAssignableFrom(par) && !Option.class.isAssignableFrom(par)) {
                return par;
            }
        }
        return MemberUtil.memberTypeUnwrappingOptionAndEither(member.getMember());
    }
    
    public static Class<?> memberTypeUnwrappingOptionAndEitherAndIterables(AccessibleObject member) {
        Class<?> type = member instanceof Field ? ((Field)member).getType() : ((Method)member).getReturnType();
        if (Option.class.isAssignableFrom(type)) {
            return ClassUtils.typeClass(member instanceof Field ? ((Field)member).getGenericType() : ((Method)member).getGenericReturnType());
        } else if (Either.class.isAssignableFrom(type)) {
            return ClassUtils.typeClass(member instanceof Field ? ((Field)member).getGenericType() : ((Method)member).getGenericReturnType());
        } else if (Iterable.class.isAssignableFrom(type)) {
            return ClassUtils.typeClass(member instanceof Field ? ((Field)member).getGenericType() : ((Method)member).getGenericReturnType());
        }
        return type;
    }
    
    public static <T> Class<?> actualTypeUnwrappingOptionAndEitherAndIterables(final MetaNamedMember<T, ?> member) {
        return MemberUtil.memberTypeUnwrappingOptionAndEitherAndIterables(member.getMember());
    }
    
    static PropertyName propertyNameFromMember(MetaNamedMember<?,?> member) {
        if (member instanceof FunctionCallMember) {
            return ((FunctionCallMember<?>) member).propertyName;
        } else {
            return new PropertyName(memberName(member));
        }
    }
    
    static boolean isEmpty(String s) {
        return s.isEmpty();
    }

    public static final <T> List<? extends MetaNamedMember<? super T,?>> toMembers(ResolvableMemberProvider resolvableMemberProvider, FunctionProvider fp, Iterable<? extends MetaNamedMember<? super T,?>> fields, PropertyName propertyName) throws UnknownPropertyNameException {
        List<? extends MetaNamedMember<? super T, ?>> ret = newList(filter(MemberUtil_.memberName.andThen(PropertyName_.isPrefixOf.ap(propertyName, fp)), fields));
        if (ret.isEmpty()) {
            // Exactly the requested property was not found. Check if the property was resolvable, for example a reference to an external API
            Iterable<? extends MetaNamedMember<? super T, ?>> allResolvableMembers = filter(ResolvableMemberProvider_.isResolvable.ap(resolvableMemberProvider), fields);
            Iterable<? extends MetaNamedMember<? super T, ?>> potentialPrefixes = filter(MemberUtil_.memberName.andThen(PropertyName_.startsWith.ap(propertyName, fp)), allResolvableMembers);
            for (MetaNamedMember<? super T, ?> prefix: sort(Compare.by(MemberUtil_.memberName.andThen(MemberUtil_.stringLength)), potentialPrefixes)) {
                return newList(new ResolvableMember<T>(prefix, newSortedSet(Ordering.<PropertyName>natural(), newList(propertyName.stripPrefix(fp, memberName(prefix)))), resolvableMemberProvider.resolveType(prefix)));
            }
            throw new MemberUtil.UnknownPropertyNameException(propertyName);
        }
        
        if (fp.isFunctionCall(propertyName)) {
            return newList(map(FunctionCallMember_.<T>$().ap(propertyName), ret));
        }
        
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> memberClass(MetaNamedMember<?, T> member) {
        return (Class<T>)(member.getMember() instanceof Field ? ((Field)member.getMember()).getType() : ((Method)member.getMember()).getReturnType());
    }
    
    @SuppressWarnings("unchecked")
    public
    static <T> Class<T> memberClassUnwrappingGeneric(MetaNamedMember<?, T> member) {
        return (Class<T>) ClassUtils.typeClass(member.getMember() instanceof Field ? ((Field)member.getMember()).getGenericType() : ((Method)member.getMember()).getGenericReturnType());
    }
    
    static String ownerType(MetaNamedMember<?, ?> member) {
        return member.getMember() instanceof Field ? ((Field)member.getMember()).getDeclaringClass().getName() : ((Method)member.getMember()).getDeclaringClass().getName();
    }
    
    @SuppressWarnings("unchecked")
    public
    static <T> Option<Builder<T>> findBuilderFor(Iterable<Builder<?>> builders, Class<T> clazz) {
        for (Builder<?> b: builders) {
            if (b.resultType().equals(clazz)) {
                return Some((Builder<T>)b);
            }
        }
        return None();
    }

    public static String memberName(Apply<?, ?> member) {
        return ((MetaNamedMember<?,?>)member).getName();
    }
    
    public static String memberNameWithDot(Apply<?, ?> member) {
        return memberName(member) + ".";
    }
    
    public static int stringLength(CharSequence str) {
        return str.length();
    }
}
