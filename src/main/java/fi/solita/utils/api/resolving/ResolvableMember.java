package fi.solita.utils.api.resolving;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.fold;
import static fi.solita.utils.functional.Predicates.not;

import java.lang.reflect.AccessibleObject;
import java.util.List;
import java.util.SortedSet;

import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.api.resolving.ResolvableMemberProvider.Type;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.RedundantPropertiesException;
import fi.solita.utils.functional.Ordering;
import fi.solita.utils.meta.MetaNamedMember;

public final class ResolvableMember<T> implements MetaNamedMember<T,Object> {
    public static final SortedSet<String> ALL_DATA = newSortedSet(Ordering.Natural(), newList(""));
    
    public final MetaNamedMember<? super T,?> original;
    private final SortedSet<PropertyName> resolvablePropertyNames;

    public final Type type;

    public ResolvableMember(MetaNamedMember<? super T,?> original, SortedSet<PropertyName> resolvablePropertyNames, Type type) {
        this.original = original;
        this.resolvablePropertyNames = resolvablePropertyNames;
        this.type = type;
    }
    
    @SuppressWarnings("unchecked")
    public static <T> ResolvableMember<T> combineAll(List<MetaNamedMember<? super T, ?>> xs) {
        return fold(ResolvableMember_.<T>combine(), (List<ResolvableMember<T>>)(Object)xs).get();
    }
    
    public ResolvableMember<T> combine(ResolvableMember<T> other) {
        if (resolvablePropertyNames.equals(ALL_DATA) && exists(not(PropertyName_.isExclusion), other.resolvablePropertyNames)) {
            throw new RedundantPropertiesException(other.resolvablePropertyNames);
        }
        if (other.resolvablePropertyNames.equals(ALL_DATA) && exists(not(PropertyName_.isExclusion), resolvablePropertyNames)) {
            throw new RedundantPropertiesException(resolvablePropertyNames);
        }
        
        Assert.equal(original, other.original);
        SortedSet<PropertyName> newSet = newSortedSet(concat(resolvablePropertyNames, other.resolvablePropertyNames));
        Assert.equal(resolvablePropertyNames.size() + other.resolvablePropertyNames.size(), newSet.size());
        return new ResolvableMember<T>(original, newSet, type);
    }
    
    public SortedSet<PropertyName> getResolvablePropertyNames() {
        return resolvablePropertyNames;
    }
    
    @Override
    public Object apply(T t) {
        return original.apply(t);
    }

    @Override
    public AccessibleObject getMember() {
        return original.getMember();
    }
    
    @Override
    public String getName() {
        return original.getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((original == null) ? 0 : original.hashCode());
        result = prime * result + ((resolvablePropertyNames == null) ? 0 : resolvablePropertyNames.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ResolvableMember<?> other = (ResolvableMember<?>) obj;
        if (original == null) {
            if (other.original != null)
                return false;
        } else if (!original.equals(other.original))
            return false;
        if (resolvablePropertyNames == null) {
            if (other.resolvablePropertyNames != null)
                return false;
        } else if (!resolvablePropertyNames.equals(other.resolvablePropertyNames))
            return false;
        if (type != other.type)
            return false;
        return true;
    }
}