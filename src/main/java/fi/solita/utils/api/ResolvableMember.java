package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Predicates.not;

import java.lang.reflect.AccessibleObject;
import java.util.SortedSet;

import fi.solita.utils.api.MemberUtil.RedundantResolvablePropertiesException;
import fi.solita.utils.api.ResolvableMemberProvider.Type;
import fi.solita.utils.functional.Ordering;
import fi.solita.utils.meta.MetaNamedMember;

public class ResolvableMember<T> implements MetaNamedMember<T,Object> {
    public static final SortedSet<String> ALL_DATA = newSortedSet(Ordering.Natural(), newList(""));
    
    public final MetaNamedMember<? super T,?> original;
    private final SortedSet<String> resolvablePropertyNames;

    public final Type type;

    public ResolvableMember(MetaNamedMember<? super T,?> original, SortedSet<String> resolvablePropertyNames, Type type) {
        this.original = original;
        this.resolvablePropertyNames = resolvablePropertyNames;
        this.type = type;
    }
    
    public ResolvableMember<T> combine(ResolvableMember<T> other) {
        if (resolvablePropertyNames.equals(ALL_DATA) && exists(not(MemberUtil_.isExclusion), other.resolvablePropertyNames)) {
            throw new RedundantResolvablePropertiesException(other.resolvablePropertyNames);
        }
        if (other.resolvablePropertyNames.equals(ALL_DATA) && exists(not(MemberUtil_.isExclusion), resolvablePropertyNames)) {
            throw new RedundantResolvablePropertiesException(resolvablePropertyNames);
        }
        
        Assert.equal(original, other.original);
        SortedSet<String> newSet = newSortedSet(concat(resolvablePropertyNames, other.resolvablePropertyNames));
        Assert.equal(resolvablePropertyNames.size() + other.resolvablePropertyNames.size(), newSet.size());
        return new ResolvableMember<T>(original, newSet, type);
    }
    
    public SortedSet<String> getResolvablePropertyNames() {
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
}