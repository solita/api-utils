package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newLinkedMap;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSortedMap;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.foreach;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Predicates.isNull;
import static fi.solita.utils.functional.Predicates.not;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.joda.time.Duration;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class ResolvableMemberProvider {
    
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
    
    public static final ResolvableMemberProvider NONE = new ResolvableMemberProvider() {
        @Override
        public boolean isResolvable(MetaNamedMember<?, ?> member) {
            return false;
        }

        @Override
        public Type resolveType(MetaNamedMember<?, ?> member) {
            return Type.Unknown;
        }
        
        @Override
        public void mutateResolvable(HttpServletRequest request, SortedSet<PropertyName> propertyNames, Object apply) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract boolean isResolvable(MetaNamedMember<?,?> member);
    
    public abstract Type resolveType(MetaNamedMember<?,?> member);
    
    /**
     * Aaargh, mutable, but would need Lenses here to update a single value...
     */
    public abstract void mutateResolvable(HttpServletRequest request, SortedSet<PropertyName> propertyNames, Object object);
    
    public <K,T> Map<K,Iterable<T>> mutateResolvables(HttpServletRequest request, Includes<T> includes, Map<K,? extends Iterable<T>> ts) {
        Map<K, Iterable<T>> ret = newLinkedMap();
        for (Map.Entry<K,? extends Iterable<T>> e: ts.entrySet()) {
            ret.put(e.getKey(), newList(map(ResolvableMemberProvider_.<T>mutateResolvables3().ap(this, request, includes), e.getValue())));
        }
        return ret;
    }
    
    public <K,T> SortedMap<K,Iterable<T>> mutateResolvables(HttpServletRequest request, Includes<T> includes, SortedMap<K,? extends Iterable<T>> ts) {
        SortedMap<K, Iterable<T>> ret = newSortedMap(ts.comparator());
        for (Map.Entry<K,? extends Iterable<T>> e: ts.entrySet()) {
            ret.put(e.getKey(), newList(map(ResolvableMemberProvider_.<T>mutateResolvables3().ap(this, request, includes), e.getValue())));
        }
        return ret;
    }
    public <T> Iterable<T> mutateResolvables(HttpServletRequest request, Includes<T> includes, Iterable<T> ts) {
        return map(ResolvableMemberProvider_.<T>mutateResolvables3().ap(this, request, includes), ts);
    }
    
    static boolean isResolvableMember(MetaNamedMember<?, ?> member) {
        return member instanceof ResolvableMember;
    }
    
    protected Duration getTimeout() {
        return Duration.standardSeconds(30);
    }
    
    @SuppressWarnings("unchecked")
    public <T> T mutateResolvables(final HttpServletRequest request, Includes<T> includes, T t) {
        for (MetaNamedMember<T,Object> member: (Iterable<MetaNamedMember<T,Object>>)(Object)filter(ResolvableMemberProvider_.isResolvableMember, includes)) {
            final SortedSet<PropertyName> propertyNames = ((ResolvableMember<?>) member).getResolvablePropertyNames();
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
            return flatMap(ResolvableMemberProvider_.unwrapResolvable.ap(((NestedMember<?,?>) member).child), Collection.class.isAssignableFrom(MemberUtil.memberClass(((NestedMember<?,?>) member).parent)) || Option.class.isAssignableFrom(MemberUtil.memberClass(((NestedMember<?,?>) member).parent)) ? (Iterable<?>)res : newList(res));
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
