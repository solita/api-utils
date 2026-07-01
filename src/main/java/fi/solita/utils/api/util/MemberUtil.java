package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.FunctionalA.zip;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.Ordering;

import fi.solita.utils.api.DynamicMember;
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
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;
import fi.solita.utils.meta.MetaProperty;

public class MemberUtil {
    
    public static class UnknownPropertyNameException extends RuntimeException {
        public final PropertyName propertyName;
    
        public UnknownPropertyNameException(PropertyName propertyName) {
            super(propertyName.toString());
            this.propertyName = propertyName;
        }
    }
    
    public static Class<?> memberClassUnwrappingOption(AccessibleObject member) {
        return memberClassUnwrappingOption(ClassUtils.getGenericType(member));
    }
    
    public static Class<?> memberClassUnwrappingOptionAndEither(AccessibleObject member) {
        return memberClassUnwrappingOptionAndEither(ClassUtils.getGenericType(member));
    }
    
    public static Class<?> memberClassUnwrappingOptionAndEitherAndIterables(AccessibleObject member) {
        return memberClassUnwrappingOptionAndEitherAndIterables(ClassUtils.getGenericType(member));
    }
    
    public static Type memberTypeUnwrappingOptionAndEitherAndIterables(AccessibleObject member) {
        return memberTypeUnwrappingOptionAndEitherAndIterables(ClassUtils.getGenericType(member));
    }
    
    public static <T> Class<?> actualClassUnwrappingOptionAndEither(final MetaNamedMember<T, ?> member) {
        return memberClassUnwrappingOptionAndEither(member.getMember());
    }

    public static <T> Class<?> actualClassUnwrappingOptionAndEitherAndIterables(final MetaNamedMember<T, ?> member) {
        return memberClassUnwrappingOptionAndEitherAndIterables(member.getMember());
    }
    
    public static <T> Type actualTypeUnwrappingOptionAndEitherAndIterables(final MetaNamedMember<T, ?> member) {
        return memberTypeUnwrappingOptionAndEitherAndIterables(member.getMember());
    }
    
    public static Class<?> memberClassUnwrappingOption(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c)) {
            return memberClassUnwrappingOption(ClassUtils.getFirstTypeArgument(type).getOrElse(type));
        }
        return c;
    }
    
    public static Class<?> memberClassUnwrappingOptionAndEither(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c) || Either.class.isAssignableFrom(c)) {
            return memberClassUnwrappingOptionAndEither(ClassUtils.getFirstTypeArgument(type).getOrElse(type));
        }
        return c;
    }
    
    public static Class<?> memberClassUnwrappingOptionAndEitherAndIterables(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c) || Either.class.isAssignableFrom(c) || Iterable.class.isAssignableFrom(c)) {
            for (Type argument: ClassUtils.getFirstTypeArgument(type)) {
                return memberClassUnwrappingOptionAndEitherAndIterables(argument);
            }
        }
        return c;
    }
    
    public static Type memberTypeUnwrappingOptionAndEitherAndIterables(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c) || Either.class.isAssignableFrom(c) || Iterable.class.isAssignableFrom(c)) {
            for (Type argument: ClassUtils.getFirstTypeArgument(type)) {
                return memberTypeUnwrappingOptionAndEitherAndIterables(argument);
            }
        }
        return type;
    }
    
    static PropertyName propertyNameFromMember(MetaNamedMember<?,?> member) {
        if (member instanceof FunctionCallMember) {
            return ((FunctionCallMember<?>) member).propertyName;
        } else {
            return PropertyName.of(memberName(member));
        }
    }
    
    static boolean isEmpty(String s) {
        return s.isEmpty();
    }

    public static final <T> List<? extends MetaNamedMember<? super T,?>> toMembers(ResolvableMemberProvider<?> resolvableMemberProvider, FunctionProvider fp, boolean onlyExact, Iterable<? extends MetaNamedMember<? super T,?>> fields, PropertyName propertyName) throws UnknownPropertyNameException {
        List<? extends MetaNamedMember<? super T, ?>> ret = newList(filter(MemberUtil_.memberName.andThen(onlyExact ? PropertyName_.isEqualTo.ap(propertyName, fp) : PropertyName_.isPrefixOf.ap(propertyName, fp)), fields));
        if (ret.isEmpty()) {
            // Exactly the requested property was not found. Check if the property was resolvable, for example a reference to an external API
            Iterable<? extends MetaNamedMember<? super T, ?>> allResolvableMembers = filter(ResolvableMemberProvider_.isResolvable.ap(resolvableMemberProvider), fields);
            Iterable<? extends MetaNamedMember<? super T, ?>> potentialPrefixes = filter(MemberUtil_.memberName.andThen(PropertyName_.startsWith.ap(propertyName, fp)), allResolvableMembers);
            for (MetaNamedMember<? super T, ?> prefix: sort(Compare.by(MemberUtil_.memberName.andThen(MemberUtil_.stringLength)), potentialPrefixes)) {
                return newList(new ResolvableMember<T>(prefix, newSortedSet(Ordering.<PropertyName>natural(), newList(propertyName.stripPrefix(fp, memberName(prefix)))), resolvableMemberProvider.resolveType(prefix)));
            }
            throw new MemberUtil.UnknownPropertyNameException(propertyName);
        }
        
        if (propertyName.isFunctionCall()) {
            return newList(map(FunctionCallMember_.<T>$().ap(propertyName), ret));
        }
        
        return ret;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> memberClass(MetaNamedMember<?, T> member) {
        return (Class<T>)(member.getMember() instanceof Field
                ? ((Field)member.getMember()).getType()
                : member.getMember() instanceof Method
                ? ((Method)member.getMember()).getReturnType()
                : member.getMember() instanceof DynamicMember.DynamicAccessibleObject
                ? ((DynamicMember.DynamicAccessibleObject)member.getMember()).type
                : null);
    }

    public static String ownerType(MetaNamedMember<?, ?> member) {
        return member.getMember() instanceof Field
            ? ((Field)member.getMember()).getDeclaringClass().getName()
            : member.getMember() instanceof Method
            ? ((Method)member.getMember()).getDeclaringClass().getName()
            : null;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Option<Builder<T>> findBuilderFor(Iterable<Builder<?>> builders, Type type) {
        Option<Class<?>> clazz = ClassUtils.resolveClass(type);
        if (clazz.isDefined() && (clazz.get().isPrimitive() || clazz.get().getName().startsWith("java.lang.") || clazz.get().getName().startsWith("java.math."))) {
            return None();
        }
        for (Builder<?> b: builders) {
            if (b.resultType().equals(type)) {
                return Some((Builder<T>)b);
            }
        }
        return None();
    }
    
    public static <VALUES extends Tuple,T> Apply<VALUES,T> builderConstructor(final VALUES members) {
        @SuppressWarnings("unchecked")
        List<MetaProperty<T,?>> ms = (List<MetaProperty<T,?>>)(Object)newList(members.toArray());
        
        Class<T> targetClass = Assert.singleton(newSet(map(new Apply<MetaProperty<T,?>, Class<T>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Class<T> apply(MetaProperty<T,?> x) {
                return (Class<T>) x.getMember().getDeclaringClass();
            }
        }, ms)));
        
        return new Apply<VALUES, T>() {
            @SuppressWarnings("unchecked")
            @Override
            public T apply(VALUES t) {
                try {
                    T ret = targetClass.newInstance();
                    for (Pair<? extends MetaProperty<T, ?>, Object> s: zip(ms, t.toArray())) {
                        ((MetaProperty<T, Object>)s.left()).setter(ret).apply(s.right());
                    }
                    return ret;
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        };
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
