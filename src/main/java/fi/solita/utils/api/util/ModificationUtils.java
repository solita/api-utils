package fi.solita.utils.api.util;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableListOfSize;
import static fi.solita.utils.functional.Collections.newMutableMapOfSize;
import static fi.solita.utils.functional.Collections.newMutableSetOfSize;
import static fi.solita.utils.functional.Collections.newMutableSortedMap;
import static fi.solita.utils.functional.Collections.newMutableSortedSet;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.not;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvingInterval;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.functional.lens.Setter;
import fi.solita.utils.meta.MetaField;

public class ModificationUtils {
    
    
    private static final Logger logger = LoggerFactory.getLogger(ModificationUtils.class);
    
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
                    t = setter.set(t, null);
                }
                return t;
            }
        };
    }

    public static final <T> Function1<T,T> withPropertiesF(Includes<T> includes, FunctionProvider fp) {
        return includes.includesEverything ? Function.<T>id() : ModificationUtils_.<T>withProperties_topLevel().ap(newList(map(MemberUtil_.propertyNameFromMember, includes.includesFromColumnFiltering)), fp, Arrays.asList(includes.builders));
    }

    static final <T> T withProperties_topLevel(Collection<PropertyName> propertyNames, FunctionProvider fp, Iterable<Builder<?>> builders, T t) {
        if (t == null) {
            return t;
        }
        Assert.defined(MemberUtil.findBuilderFor(builders, t.getClass()), "No Builder found for the type of the root object: " + t.getClass().getName() + ". You have a bug?");
        return ModificationUtils.withProperties(propertyNames, builders, fp, t);
    }

    @SuppressWarnings("unchecked")
    static final <T> T withProperties(Collection<PropertyName> propertyNames, Iterable<Builder<?>> builders, FunctionProvider fp, T t) {
        logger.debug("Including properties {} in {}", propertyNames, t);
        for (Builder<T> builder: MemberUtil.findBuilderFor(builders, (Class<T>)t.getClass())) {
            logger.debug("Found Builder for {}", t.getClass());
            for (Apply<? super T, Object> member: (Iterable<Apply<? super T, Object>>)builder.getMembers()) {
                logger.debug("Handling member {}", member);
                List<PropertyName> subs = newList(filter(PropertyName_.startsWith.apply(Function.__, fp, MemberUtil.memberName(member)), propertyNames));
                String memberName = MemberUtil.memberName(member);
                logger.debug("Relevant properties: {}", propertyNames);
                if (!subs.isEmpty()) {
                    Object value = member.apply(t);
                    logger.debug("Got value: {}", value);
                    if (value != null) {
                        List<PropertyName> subProps = newList(filter(not(PropertyName_.isEmpty.apply(Function.__, fp)), Functional.map(PropertyName_.stripPrefix.apply(Function.__, fp, memberName), subs)));
                        logger.debug("Relevant properties for nested: {}", subProps);
                        
                        Object nested;
                        if (subProps.isEmpty()) {
                            nested = Assert.singleton(subs).applyFunction(fp, value);
                        } else {
                            nested = withProperties(subProps, builders, fp, value);
                        }
                        logger.debug("Builder.with({},{})", member, nested);
                        
                        builder = builder.with(member, nested);
                    }
                }
            }
            if (t instanceof ResolvingInterval) {
                logger.debug("Keeping ResolvingInterval property");
                MetaField<T,Interval> resolvingInterval = ((ResolvingInterval<T>) t).resolvingInterval();
                builder = builder.with(resolvingInterval, resolvingInterval.apply(t));
            }
            logger.debug("Building new object");
            return builder.buildAllowIncomplete();
        }
        
        if (t instanceof SortedSet) {
            logger.debug("Object is a SortedSet: {}", t.getClass());
            SortedSet<Object> ret = newMutableSortedSet(((SortedSet<Object>) t).comparator());
            for (Object o: (SortedSet<Object>)t) {
                ret.add(withProperties(propertyNames, builders, fp, o));
            }
            return (T) ret;
        } else if (t instanceof Set) {
            logger.debug("Object is a Set: {}", t.getClass());
            Set<Object> ret = newMutableSetOfSize(((Set<?>) t).size());
            for (Object o: (Set<Object>)t) {
                ret.add(withProperties(propertyNames, builders, fp, o));
            }
            return (T) ret;
        } else if (t instanceof List || t instanceof Collection) {
            logger.debug("Object is a List/Collection: {}", t.getClass());
            List<Object> ret = newMutableListOfSize(((Collection<?>) t).size());
            for (Object o: (List<Object>)t) {
                ret.add(withProperties(propertyNames, builders, fp, o));
            }
            return (T) ret;
        } else if (t instanceof SortedMap) {
            logger.debug("Object is a SortedMap: {}", t.getClass());
            SortedMap<Object,Object> ret = newMutableSortedMap(((SortedMap<Object,Object>) t).comparator());
            for (SortedMap.Entry<Object, Object> o: ((SortedMap<Object,Object>)t).entrySet()) {
                ret.put(o.getKey(), withProperties(propertyNames, builders, fp, o.getValue()));
            }
            if (((SortedMap<?,?>) t).size() != ret.size()) {
                throw new IllegalStateException("Something wrong");
            }
            return (T) ret;
        } else if (t instanceof Map) {
            logger.debug("Object is a Map: {}", t.getClass());
            Map<Object,Object> ret = newMutableMapOfSize(((Map<?,?>) t).size());
            for (Map.Entry<Object, Object> o: ((Map<Object,Object>)t).entrySet()) {
                ret.put(o.getKey(), withProperties(propertyNames, builders, fp, o.getValue()));
            }
            if (((Map<?,?>) t).size() != ret.size()) {
                throw new IllegalStateException("Something wrong");
            }
            return (T) ret;
        } else if (t instanceof Option) {
            logger.debug("Object is an Option: {}", t.getClass());
            return (T)((Option<T>) t).map(ModificationUtils_.withProperties().ap(propertyNames, builders, fp));
        }
        
        logger.debug("No builder found for {}", t.getClass());
        return t;
    }

}
