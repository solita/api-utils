package fi.solita.utils.api.resolving;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableLinkedMap;
import static fi.solita.utils.functional.Collections.newMutableSortedMap;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.foreach;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.isNull;
import static fi.solita.utils.functional.Predicates.not;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.joda.time.Duration;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.NestedMember;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class ResolvableMemberProvider<REQ> {
    
    public enum Type {
        Internal,
        /* External API, but whose revision is included in the revision number of the current API */
        ExternalKnown,
        ExternalUnknown,
        Unknown
    }
    
    private static final ExecutorService pool = Executors.newFixedThreadPool(5);
    
    public static final class CannotResolveAsFormatException extends RuntimeException {
        public final SerializationFormat format;

        public CannotResolveAsFormatException(SerializationFormat format) {
            this.format = format;
        }
    }
    
    public static final ResolvableMemberProvider<?> NONE = new ResolvableMemberProvider<Object>() {
        @Override
        public boolean isResolvable(MetaNamedMember<?, ?> member) {
            return false;
        }

        @Override
        public Type resolveType(MetaNamedMember<?, ?> member) {
            return Type.Unknown;
        }
        
        @Override
        public void mutateResolvable(Object request, Set<PropertyName> propertyNames, Object apply) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract boolean isResolvable(MetaNamedMember<?,?> member);
    
    public abstract Type resolveType(MetaNamedMember<?,?> member);
    
    /**
     * Aaargh, mutable, but would need Lenses here to update a single value...
     */
    public abstract void mutateResolvable(REQ request, Set<PropertyName> propertyNames, Object object);
    
    public <K,T> Map<K,T> mutateResolvablesSingle(REQ request, Includes<T> includes, Map<K,T> ts) {
        if (includes.includesEverything || !exists(ResolvableMemberProvider_.isResolvableMember, includes)) {
            return ts;
        }
        Map<K,T> ret = newMutableLinkedMap();
        for (Map.Entry<K,T> e: ts.entrySet()) {
            ret.put(e.getKey(), mutateResolvables(request, includes, e.getValue()));
        }
        return ret;
    }
    
    @SuppressWarnings("unchecked")
    public <K,T> Map<K,Iterable<T>> mutateResolvables(REQ request, Includes<T> includes, Map<K,? extends Iterable<T>> ts) {
        if (includes.includesEverything || !exists(ResolvableMemberProvider_.isResolvableMember, includes)) {
            return (Map<K, Iterable<T>>) ts;
        }
        Map<K, Iterable<T>> ret = newMutableLinkedMap();
        for (Map.Entry<K,? extends Iterable<T>> e: ts.entrySet()) {
            ret.put(e.getKey(), newList(map(ResolvableMemberProvider_.<REQ,T>mutateResolvables3().ap(this, request, includes), e.getValue())));
        }
        return ret;
    }
    
    @SuppressWarnings("unchecked")
    public <K,T> SortedMap<K,Iterable<T>> mutateResolvables(REQ request, Includes<T> includes, SortedMap<K,? extends Iterable<T>> ts) {
        if (includes.includesEverything || !exists(ResolvableMemberProvider_.isResolvableMember, includes)) {
            return (SortedMap<K, Iterable<T>>) ts;
        }
        SortedMap<K, Iterable<T>> ret = newMutableSortedMap(ts.comparator());
        for (Map.Entry<K,? extends Iterable<T>> e: ts.entrySet()) {
            ret.put(e.getKey(), newList(map(ResolvableMemberProvider_.<REQ,T>mutateResolvables3().ap(this, request, includes), e.getValue())));
        }
        return ret;
    }
    
    public <T> Iterable<T> mutateResolvables(REQ request, Includes<T> includes, Iterable<T> ts) {
        if (includes.includesEverything || !exists(ResolvableMemberProvider_.isResolvableMember, includes)) {
            return ts;
        }
        return map(ResolvableMemberProvider_.<REQ,T>mutateResolvables3().ap(this, request, includes), ts);
    }
    
    public static boolean isResolvableMember(MetaNamedMember<?, ?> member) {
        return member instanceof ResolvableMember;
    }
    
    protected Duration getTimeout() {
        return Duration.standardSeconds(30);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T mutateResolvables(final REQ request, Includes<T> includes, T t) {
        if (includes.includesEverything) {
            return t;
        }
        
        for (MetaNamedMember<T,Object> member: (Iterable<MetaNamedMember<T,Object>>)(Object)filter(ResolvableMemberProvider_.isResolvableMember, includes)) {
            final Set<PropertyName> propertyNames = ((ResolvableMember<?>) member).getResolvablePropertyNames();
            try {
                List<Future<Void>> futures = pool.invokeAll(newList(map(new Apply<Object, Callable<Void>>() {
                    @Override
                    public Callable<Void> apply(final Object x) {
                        return new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                mutateResolvable(request, propertyNames, x);
                                return null;
                            }
                        };
                    }
                    // might be null in some edge-cases due to propertyName-filtering
                }, filter(not(isNull()), unwrapResolvable(((ResolvableMember<T>)member).original, t)))), getTimeout().getMillis(), TimeUnit.MILLISECONDS);
                foreach(new Apply<Future<Void>, Void>() {
                    @Override
                    public Void apply(Future<Void> t) {
                        try {
                            return t.get();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }, futures);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    public static Iterable<Object> unwrapResolvable(MetaNamedMember<?,?> member, Object resolvable) {
        if (member instanceof NestedMember) {
            Object res = ((NestedMember<Object,?>) member).parent.apply(resolvable);
            return flatMap(ResolvableMemberProvider_.unwrapResolvable.ap(((NestedMember<?,?>) member).child), Collection.class.isAssignableFrom(MemberUtil.memberClass(((NestedMember<?,?>) member).parent)) || Option.class.isAssignableFrom(MemberUtil.memberClass(((NestedMember<?,?>) member).parent)) ? (Iterable<Object>)res : newList(res));
        }
        Object ret = ((MetaNamedMember<Object,Object>)member).apply(resolvable);
        Class<?> memberClass = MemberUtil.memberClass(member);
        return (Iterable<Object>) (Collection.class.isAssignableFrom(memberClass) || Option.class.isAssignableFrom(memberClass) ? map(ResolvableMemberProvider_.hackTuple, (Iterable<Object>)ret) : newList(hackTuple(ret)));
    }
    
    // if a tuple, assume the first element is the resolvable one. Sigh...
    @SuppressWarnings("unchecked")
    public static Object hackTuple(Object resolvable) {
        return resolvable instanceof Tuple ? ((Tuple._1<Object>)resolvable).get_1() : resolvable; 
    }
}
