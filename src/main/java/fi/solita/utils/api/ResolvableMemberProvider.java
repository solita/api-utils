package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSortedMap;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.foreach;
import static fi.solita.utils.functional.Functional.map;

import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;

import javax.servlet.http.HttpServletRequest;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.functional.Option;
import fi.solita.utils.meta.MetaNamedMember;

public abstract class ResolvableMemberProvider {
    
    public enum Type { Internal, ExternalKnown, ExternalUnknown, Unknown }
    
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
        public void mutateResolvable(HttpServletRequest request, SortedSet<String> propertyNames, Object apply) {
            throw new UnsupportedOperationException();
        }
    };

    public abstract boolean isResolvable(MetaNamedMember<?,?> member);
    
    public abstract Type resolveType(MetaNamedMember<?,?> member);
    
    /**
     * Aaargh, mutable, but would need Lenses here to update a single value...
     */
    public abstract void mutateResolvable(HttpServletRequest request, SortedSet<String> propertyNames, Object object);
    
    public <K,T> SortedMap<K,Iterable<T>> mutateResolvables(HttpServletRequest request, Includes<T> includes, SortedMap<K,? extends Iterable<T>> ts) {
        SortedMap<K, Iterable<T>> ret = newSortedMap(ts.comparator());
        for (Map.Entry<K,? extends Iterable<T>> e: ts.entrySet()) {
            ret.put(e.getKey(), newList(map(ResolvableMemberProvider_.<T>mutateResolvables2().ap(this, request, includes), e.getValue())));
        }
        return ret;
    }
    public <T> Iterable<T> mutateResolvables(HttpServletRequest request, Includes<T> includes, Iterable<T> ts) {
        return map(ResolvableMemberProvider_.<T>mutateResolvables2().ap(this, request, includes), ts);
    }
    
    static boolean isResolvableMember(MetaNamedMember<?, ?> member) {
        return member instanceof ResolvableMember;
    }
    
    @SuppressWarnings("unchecked")
    public <T> T mutateResolvables(HttpServletRequest request, Includes<T> includes, T t) {
        for (MetaNamedMember<T,Object> member: (Iterable<MetaNamedMember<T,Object>>)(Object)filter(ResolvableMemberProvider_.isResolvableMember, includes)) {
            SortedSet<String> propertyNames = ((ResolvableMember<?>) member).getResolvablePropertyNames();
            foreach(ResolvableMemberProvider_.mutateResolvable.ap(this, request, propertyNames), unwrapResolvable(((ResolvableMember<T>)member).original, t));
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    static Iterable<Object> unwrapResolvable(MetaNamedMember<?,?> member, Object resolvable) {
        if (member instanceof NestedMember) {
            Object res = ((NestedMember<Object,?>) member).parent.apply(resolvable);
            return flatMap(ResolvableMemberProvider_.unwrapResolvable.ap(((NestedMember<?,?>) member).child), Collection.class.isAssignableFrom(MemberUtil.memberClass(((NestedMember<?,?>) member).parent)) || Option.class.isAssignableFrom(MemberUtil.memberClass(((NestedMember<?,?>) member).parent)) ? (Iterable<?>)res : newList(res));
        }
        Object ret = ((MetaNamedMember<Object,Object>)member).apply(resolvable);
        return (Iterable<Object>) (Collection.class.isAssignableFrom(MemberUtil.memberClass(member)) || Option.class.isAssignableFrom(MemberUtil.memberClass(member)) ? ret : newList(ret));
    }
}
