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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

import com.google.common.collect.Ordering;

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
    
    public static Class<?> memberTypeUnwrappingOption(AccessibleObject member) {
        return memberTypeUnwrappingOption(ClassUtils.getGenericType(member));
    }
    
    public static Class<?> memberTypeUnwrappingOptionAndEither(AccessibleObject member) {
        return memberTypeUnwrappingOptionAndEither(ClassUtils.getGenericType(member));
    }
    
    public static Class<?> memberTypeUnwrappingOptionAndEitherAndIterables(AccessibleObject member) {
        return memberTypeUnwrappingOptionAndEitherAndIterables(ClassUtils.getGenericType(member));
    }
    
    public static <T> Class<?> actualTypeUnwrappingOptionAndEither(final MetaNamedMember<T, ?> member) {
        return memberTypeUnwrappingOptionAndEither(member.getMember());
    }

    public static <T> Class<?> actualTypeUnwrappingOptionAndEitherAndIterables(final MetaNamedMember<T, ?> member) {
        return memberTypeUnwrappingOptionAndEitherAndIterables(member.getMember());
    }
    
    public static Class<?> memberTypeUnwrappingOption(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c)) {
            return memberTypeUnwrappingOption(ClassUtils.getFirstTypeArgument(type).getOrElse(type));
        }
        return c;
    }
    
    public static Class<?> memberTypeUnwrappingOptionAndEither(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c) || Either.class.isAssignableFrom(c)) {
            return memberTypeUnwrappingOptionAndEither(ClassUtils.getFirstTypeArgument(type).getOrElse(type));
        }
        return c;
    }
    
    public static Class<?> memberTypeUnwrappingOptionAndEitherAndIterables(Type type) {
        Class<?> c = ClassUtils.typeClass(type);
        if (Option.class.isAssignableFrom(c) || Either.class.isAssignableFrom(c) || Iterable.class.isAssignableFrom(c)) {
            return memberTypeUnwrappingOptionAndEitherAndIterables(ClassUtils.getFirstTypeArgument(type).getOrElse(type));
        }
        return c;
    }
    
    static PropertyName propertyNameFromMember(MetaNamedMember<?,?> member) {
        if (member instanceof FunctionCallMember) {
            return ((FunctionCallMember<?>) member).propertyName;
        } else {
            return PropertyName.of(memberName(member));
        }
    }
    
    public static Class<?> typeClass(Type type) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType)type).getActualTypeArguments();
            return args.length == 0 ? typeClass(((ParameterizedType)type).getRawType()) : typeClass(args[0]);
        } else if (type instanceof Class) {
            return (Class<?>) type;
        } else {
            throw new IllegalArgumentException("Could not handle Type: " + type.getClass());
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
        return (Class<T>)(member.getMember() instanceof Field ? ((Field)member.getMember()).getType() : ((Method)member.getMember()).getReturnType());
    }

    public static String ownerType(MetaNamedMember<?, ?> member) {
        return member.getMember() instanceof Field ? ((Field)member.getMember()).getDeclaringClass().getName() : ((Method)member.getMember()).getDeclaringClass().getName();
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Option<Builder<T>> findBuilderFor(Iterable<Builder<?>> builders, Class<T> clazz) {
        if (clazz.isPrimitive() || clazz.getName().startsWith("java.")) {
            return None();
        }
        for (Builder<?> b: builders) {
            if (b.resultType().equals(clazz)) {
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
