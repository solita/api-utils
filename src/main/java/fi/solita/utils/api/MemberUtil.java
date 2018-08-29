package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newListOfSize;
import static fi.solita.utils.functional.Collections.newMapOfSize;
import static fi.solita.utils.functional.Collections.newSetOfSize;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static fi.solita.utils.functional.Function.__;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.fold;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.Functional.span;
import static fi.solita.utils.functional.FunctionalA.filter;
import static fi.solita.utils.functional.FunctionalM.groupBy;
import static fi.solita.utils.functional.FunctionalM.mapValue;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.not;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Ordering;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Compare;
import fi.solita.utils.functional.Either;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.functional.lens.Setter;
import fi.solita.utils.meta.MetaNamedMember;

public class MemberUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(MemberUtil.class);
    
    public static class RedundantResolvablePropertiesException extends RuntimeException {
        public final SortedSet<String> propertyNames;
    
        public RedundantResolvablePropertiesException(SortedSet<String> propertyNames) {
            super(mkString(",", propertyNames));
            this.propertyNames = propertyNames;
        }
    }

    public static class UnknownPropertyNameException extends RuntimeException {
        public final String propertyName;
    
        public UnknownPropertyNameException(String propertyName) {
            super(propertyName);
            this.propertyName = propertyName;
        }
    }
    
    public static final <T> Collection<? extends MetaNamedMember<? super T,?>> allMethods(Class<T> clazz) {
        try {
            return newList(map(MemberUtil_.<T>md(), filter(MemberUtil_.include, Introspector.getBeanInfo(clazz).getMethodDescriptors())));
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
    
    static boolean include(MethodDescriptor md) {
        return !md.getMethod().getDeclaringClass().equals(Object.class);
    }
    
    static <T> MetaNamedMember<T,?> md(final MethodDescriptor md) {
        return new MetaNamedMember<T, Object>() {
            @Override
            public Object apply(T t) {
                try {
                    return md.getMethod().invoke(t);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AccessibleObject getMember() {
                return md.getMethod();
            }

            @Override
            public String getName() {
                return md.getName();
            }
        };
    }
    
    public static final <T> Collection<? extends MetaNamedMember<? super T,?>> beanGetters(Class<T> clazz) {
        try {
            return newList(map(MemberUtil_.<T>pd(), filter(MemberUtil_.include1, Introspector.getBeanInfo(clazz).getPropertyDescriptors())));
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
    
    static boolean include(PropertyDescriptor pd) {
        return !pd.getReadMethod().getDeclaringClass().equals(Object.class);
    }
    
    static <T> MetaNamedMember<T,?> pd(final PropertyDescriptor pd) {
        return new MetaNamedMember<T, Object>() {
            @Override
            public Object apply(T t) {
                try {
                    return pd.getReadMethod().invoke(t);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public AccessibleObject getMember() {
                return pd.getReadMethod();
            }

            @Override
            public String getName() {
                return pd.getName();
            }
        };
    }
    
    /**
     * 
     * @param members All members of T, which would be possible to use for filtering the result.
     * @param builders Builders defining what are considered as "nested properties". That is, these define the objects in the hierarchy whose members can be filtered with <i>propertyName</i>
     * @param propertyNames Filtering given by the API user. Empty if not given.
     * @param geometries Geometry members. Subset of <i>members</i>.
     */
    @SuppressWarnings("unchecked")
    public static final <T> Includes<T> resolveIncludes(ResolvableMemberProvider provider, SerializationFormat format, Iterable<String> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Iterable<? extends MetaNamedMember<? super T,?>> geometries) {
        List<MetaNamedMember<? super T, ?>> ret;
        boolean includesEverything = false;
        if (propertyNames == null) {
            // For PNG exclude everything (geometry included back in the end)
            // For HTML and spreadsheet formats, exclude parent of nested members.
            // For others, include everything.
            switch (format) {
                case PNG:
                    members = emptyList();
                    break;
                case CSV:
                case XLSX:
                case HTML:
                    members = withNestedMembers(members, Include.OnlyLeaf, builders);
                    break;
                case GEOJSON:
                case GML:
                case JSON:
                case XML:
                    members = withNestedMembers(members, Include.All, builders);
                    break;
            }
            
            ret = (List<MetaNamedMember<? super T, ?>>) members;
            includesEverything = true;
        } else if (size(propertyNames) == 1 && head(propertyNames).isEmpty()) {
            ret = emptyList();
        } else {
            members = withNestedMembers(members, Include.All, builders);
            ret = newList(flatMap(MemberUtil_.<T>toMembers().ap(provider, members), propertyNames));
        }
        
        // Always include geometries for png/geojson/gml
        switch (format) {
            case PNG:
            case GEOJSON:
            case GML:
                for (MetaNamedMember<? super T,?> geometry: geometries) {
                    if (!exists(MemberUtil_.memberName.andThen(equalTo((CharSequence)geometry.getName())), ret)) {
                        ret = newList(cons(geometry, ret));
                    }
                }
                break;
            case JSON:
            case HTML:
            case CSV:
            case XLSX:
            case XML:
        }
        
        Pair<Iterable<MetaNamedMember<? super T,?>>,Iterable<MetaNamedMember<? super T,?>>> resolvableAndOthers = span(ResolvableMemberProvider_.isResolvableMember, ret);
        Map<CharSequence, List<MetaNamedMember<? super T, ?>>> resolvable = groupBy(MemberUtil_.memberName, resolvableAndOthers.left());
        Collection<ResolvableMember<T>> combinedResolvables = mapValue(MemberUtil_.<T>combineResolvableMembers(), resolvable).values();
        
        return new Includes<T>(newList(concat(resolvableAndOthers.right(), combinedResolvables)), geometries, includesEverything, builders);
    }
    
    @SuppressWarnings("unchecked")
    static <T> ResolvableMember<T> combineResolvableMembers(List<MetaNamedMember<? super T, ?>> xs) {
        return fold(ResolvableMember_.<T>combine(), (List<ResolvableMember<T>>)(Object)xs).get();
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
    
    public enum Include {
        All,
        OnlyLeaf,
        NoBuildable
    }
    
    /**
     * Returns "nested properties" hierarchically. That is, those for which there is a builder in <i>builders</i>.
     * For example, if:
     * <p>Foo { A a, B b, C c } and B { A a } and C { A a }
     * <p>then
     * <code>withNestedMembers([foo.a, foo.b, foo.c], Builder<A>, Builder<B>)</code>
     * <p>would return [a, a.b, a.b.a, b, b.a, c]
     */
    @SuppressWarnings("unchecked")
    public static <T> List<MetaNamedMember<? super T, ?>> withNestedMembers(Collection<? extends MetaNamedMember<? super T, ?>> members, Include include, Builder<?>... builders) {
        List<MetaNamedMember<? super T, ?>> ret = newList();
        for (MetaNamedMember<? super T, ?> member: members) {
            ret.add(member);
            for (Builder<?> builder: findBuilderFor(newList(builders), memberClassUnwrappingGeneric(member))) {
                if (include == Include.NoBuildable) {
                    ret.remove(member);
                }
                for (MetaNamedMember<?, ?> nestedMember: withNestedMembers((Collection<? extends MetaNamedMember<T, ?>>) builder.getMembers(), include, builders)) {
                    ret.add(NestedMember.unchecked(member, nestedMember));
                    if (include == Include.OnlyLeaf) {
                        ret.remove(member);
                    }
                }
            }
        }
        return ret;
    }

    public static final <T> Function1<T,T> withPropertiesF(Includes<T> includes) {
        return includes.includesEverything ? Function.<T>id() : MemberUtil_.<T>withProperties_topLevel().ap(newList(Functional.map(MemberUtil_.memberNameWithDot, includes)), Arrays.asList(includes.builders));
    }
    
    static boolean startsWith(String s, String prefix) {
        return s.startsWith(prefix);
    }
    
    static boolean isEmpty(String s) {
        return s.isEmpty();
    }

    static final <T> T withProperties_topLevel(Collection<String> propertyNames, Iterable<Builder<?>> builders, T t) {
        Assert.defined(findBuilderFor(builders, t.getClass()), "No Builder found for the type of the root object: " + t.getClass().getName() + ". You have a bug?");
        return withProperties(propertyNames, builders, t);
    }
    
    @SuppressWarnings("unchecked")
    static final <T> T withProperties(Collection<String> propertyNames, Iterable<Builder<?>> builders, T t) {
        logger.debug("Including properties {} in {}", propertyNames, t);
        for (Builder<T> builder: findBuilderFor(builders, (Class<T>)t.getClass())) {
            logger.debug("Found Builder for {}", t.getClass());
            for (Apply<? super T, Object> member: (Iterable<Apply<? super T, Object>>)builder.getMembers()) {
                logger.debug("Handling member {}", member);
                List<String> subs = newList(filter(MemberUtil_.startsWith.apply(Function.__, memberNameWithDot(member)), propertyNames));
                logger.debug("Relevant properties: {}", propertyNames);
                if (!subs.isEmpty()) {
                    Object value = member.apply(t);
                    logger.debug("Got value: {}", value);
                    if (value != null) {
                        List<String> subProps = newList(filter(not(MemberUtil_.isEmpty), Functional.map(MemberUtil_.removeFirstPart, subs)));
                        logger.debug("Relevant properties for nested: {}", subProps);
                        
                        Object nested = subProps.isEmpty() ? value : withProperties(subProps, builders, value);
                        logger.debug("Builder.with({},{})", member, nested);
                        builder = builder.with(member, nested);
                    }
                }
            }
            logger.debug("Building new object");
            return builder.buildAllowIncomplete();
        }
        
        if (t instanceof SortedSet) {
            logger.debug("Object is a SortedSet: {}", t.getClass());
            SortedSet<Object> ret = newSortedSet(((SortedSet<Object>) t).comparator());
            for (Object o: (SortedSet<Object>)t) {
                ret.add(withProperties(propertyNames, builders, o));
            }
            if (((SortedSet<?>) t).size() != ret.size()) {
                throw new IllegalStateException("Something wrong");
            }
            return (T) ret;
        } else if (t instanceof Set) {
            logger.debug("Object is a Set: {}", t.getClass());
            Set<Object> ret = newSetOfSize(((Set<?>) t).size());
            for (Object o: (Set<Object>)t) {
                ret.add(withProperties(propertyNames, builders, o));
            }
            if (((Set<?>) t).size() != ret.size()) {
                throw new IllegalStateException("Something wrong");
            }
            return (T) ret;
        } else if (t instanceof List || t instanceof Collection) {
            logger.debug("Object is a List/Collection: {}", t.getClass());
            List<Object> ret = newListOfSize(((Collection<?>) t).size());
            for (Object o: (List<Object>)t) {
                ret.add(withProperties(propertyNames, builders, o));
            }
            return (T) ret;
        } else if (t instanceof Map) {
            logger.debug("Object is a Map: {}", t.getClass());
            Map<Object,Object> ret = newMapOfSize(((Map<?,?>) t).size());
            for (Map.Entry<Object, Object> o: ((Map<Object,Object>)t).entrySet()) {
                ret.put(o.getKey(), withProperties(propertyNames, builders, o.getValue()));
            }
            if (((Map<?,?>) t).size() != ret.size()) {
                throw new IllegalStateException("Something wrong");
            }
            return (T) ret;
        }
        
        logger.debug("No builder found for {}", t.getClass());
        return t;
    }
    
    /**
     * @see MemberUtil#excluding(Iterable)
     */
    public static final <T> Apply<T,T> excluding(Setter<T, ?> setter) {
        return excluding(newList(setter));
    }

    /**
     * @see MemberUtil#excluding(Iterable)
     */
    public static final <T> Apply<T,T> excluding(Setter<T, ?> setter1, Setter<T, ?> setter2) {
        return excluding(newList(setter1, setter2));
    }

    /**
     * Palauttaa funktion, joka nullaa (please forgive me!) annettujen Lensien osoittamat kentät.
     */
    public static final <T> Apply<T,T> excluding(final Iterable<? extends Setter<T, ?>> setters) {
        return new Apply<T, T>() {
            @Override
            public T apply(T t) {
                for (Setter<T,?> setter: setters) {
                    logger.debug("Nulling {} in {}", setter, t);
                    t = setter.set(t, null);
                }
                return t;
            }
        };
    }
    
    static final <T> List<? extends MetaNamedMember<? super T,?>> toMembers(ResolvableMemberProvider resolvableMemberProvider, Iterable<? extends MetaNamedMember<? super T,?>> fields, String propertyName) throws UnknownPropertyNameException {
        List<? extends MetaNamedMember<? super T, ?>> ret = newList(filter(MemberUtil_.memberNameWithDot.andThen(MemberUtil_.startsWith.apply(__, propertyName + ".")), fields));
        if (ret.isEmpty()) {
            // Exactly the requested property was not found. Check if the property was resolvable, for example a reference to an external API
            Iterable<? extends MetaNamedMember<? super T, ?>> allResolvableMembers = filter(ResolvableMemberProvider_.isResolvable.ap(resolvableMemberProvider), fields);
            Iterable<? extends MetaNamedMember<? super T, ?>> potentialPrefixes = filter(MemberUtil_.memberNameWithDot.andThen(MemberUtil_.startsWith.ap(propertyName)), allResolvableMembers);
            for (MetaNamedMember<? super T, ?> prefix: sort(Compare.by(MemberUtil_.memberName.andThen(MemberUtil_.stringLength)), potentialPrefixes)) {
                return newList(new ResolvableMember<T>(prefix, newSortedSet(Ordering.<String>natural(), newList(propertyName.replaceFirst(Pattern.quote(prefix.getName() + "."), ""))), resolvableMemberProvider.resolveType(prefix)));
            }
            throw new MemberUtil.UnknownPropertyNameException(propertyName);
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    static <T> Class<T> memberClass(MetaNamedMember<?, T> member) {
        return (Class<T>)(member.getMember() instanceof Field ? ((Field)member.getMember()).getType() : ((Method)member.getMember()).getReturnType());
    }
    
    @SuppressWarnings("unchecked")
    static <T> Class<T> memberClassUnwrappingGeneric(MetaNamedMember<?, T> member) {
        return (Class<T>) ClassUtils.typeClass(member.getMember() instanceof Field ? ((Field)member.getMember()).getGenericType() : ((Method)member.getMember()).getGenericReturnType());
    }
    
    static String ownerType(MetaNamedMember<?, ?> member) {
        return member.getMember() instanceof Field ? ((Field)member.getMember()).getDeclaringClass().getName() : ((Method)member.getMember()).getDeclaringClass().getName();
    }
    
    static String removeFirstPart(String str) {
        int i = str.indexOf('.');
        return i == -1 ? "" : str.substring(i+1);
    }

    @SuppressWarnings("unchecked")
    static <T> Option<Builder<T>> findBuilderFor(Iterable<Builder<?>> builders, Class<T> clazz) {
        for (Builder<?> b: builders) {
            if (b.resultType().equals(clazz)) {
                return Some((Builder<T>)b);
            }
        }
        return None();
    }

    public static CharSequence memberName(Apply<?, ?> member) {
        return ((MetaNamedMember<?,?>)member).getName();
    }
    
    public static String memberNameWithDot(Apply<?, ?> member) {
        return memberName(member) + ".";
    }
    
    public static int stringLength(CharSequence str) {
        return str.length();
    }
}
