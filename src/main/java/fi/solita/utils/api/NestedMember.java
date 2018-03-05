package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Predicates.not;

import java.lang.reflect.AccessibleObject;

import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Predicates;
import fi.solita.utils.meta.MetaNamedMember;

public class NestedMember<S,T> implements MetaNamedMember<S,T> {
    public final MetaNamedMember<S,?> parent;
    public final MetaNamedMember<?,T> child;

    public static final <S,U,T> MetaNamedMember<S,T> of(MetaNamedMember<S, U> parent, MetaNamedMember<? super U,T> child) {
        return new NestedMember<S,T>(parent, child);
    }

    public static final <S,U,T> MetaNamedMember<S,T> ofIt(MetaNamedMember<S, ? extends Iterable<U>> parent, MetaNamedMember<? super U,T> child) {
        return new NestedMember<S,T>(parent, child);
    }

    public static final <S,T> MetaNamedMember<S,T> unchecked(MetaNamedMember<S, ?> parent, MetaNamedMember<?,T> child) {
        return new NestedMember<S,T>(parent, child);
    }

    private NestedMember(MetaNamedMember<S, ?> parent, MetaNamedMember<?,T> child) {
        this.parent = parent;
        this.child = child;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(S t) {
        // Hmm, I somehow feel bad about this...
        Object u = parent.apply(t);
        if (u == null) {
            return null;
        } else if (u instanceof Option) {
            if (((Option<?>) u).isDefined()) {
                return (T)Option.of(((MetaNamedMember<Object,T>)child).apply(((Option<?>)u).get()));
            } else {
                return (T)None();
            }
        } else if (u instanceof Iterable) {
            // not sure if this is smart...
            return (T) filter(not(Predicates.isNull()), map((MetaNamedMember<Object,T>)child, (Iterable<Object>)u));
        } else {
            return ((MetaNamedMember<Object,T>)child).apply(u);
        }
    }

    @Override
    public AccessibleObject getMember() {
        return child.getMember();
    }

    @Override
    public String getName() {
        return parent.getName() + "." + child.getName();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((child == null) ? 0 : child.hashCode());
        result = prime * result + ((parent == null) ? 0 : parent.hashCode());
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
        NestedMember<?,?> other = (NestedMember<?,?>) obj;
        if (child == null) {
            if (other.child != null)
                return false;
        } else if (!child.equals(other.child))
            return false;
        if (parent == null) {
            if (other.parent != null)
                return false;
        } else if (!parent.equals(other.parent))
            return false;
        return true;
    }

    
}