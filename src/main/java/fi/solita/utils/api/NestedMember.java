package fi.solita.utils.api;

import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Predicates.not;

import java.lang.reflect.AccessibleObject;

import fi.solita.utils.api.NestedMember;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Predicates;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.meta.MetaNamedMember;

public class NestedMember<S,T> implements MetaNamedMember<S,T> {
    public final MetaNamedMember<S,?> parent;
    public final MetaNamedMember<?,T> child;
    private final Function1<Object,Object> parentModifier;
    private final boolean flatten;

    public static final <S,U,T> NestedMember<S,T> of(MetaNamedMember<S, U> parent, MetaNamedMember<? super U,T> child) {
        return new NestedMember<S,T>(parent, child, Function.id(), false);
    }
    
    @SuppressWarnings("unchecked")
    public static final <S,U,T> NestedMember<S,Option<T>> ofOption(MetaNamedMember<S, Option<U>> parent, MetaNamedMember<? super U,T> child) {
        return new NestedMember<S,Option<T>>(parent, (MetaNamedMember<?, Option<T>>) child, Function.id(), false);
    }

    @SuppressWarnings("unchecked")
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofItFlatType(MetaNamedMember<S, ? extends Iterable<U>> parent, MetaNamedMember<? super U,? extends Iterable<T>> child) {
        return new NestedMember<S,Iterable<T>>(parent, (MetaNamedMember<?, Iterable<T>>) child, Function.id(), true);
    }
    
    public static final <S,U,T> NestedMember<S,Option<T>> ofOptionFlatType(MetaNamedMember<S, ? extends Iterable<U>> parent, MetaNamedMember<? super U,Option<T>> child) {
        return new NestedMember<S,Option<T>>(parent, child, Function.id(), true);
    }

    @SuppressWarnings("unchecked")
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofIt(MetaNamedMember<S, ? extends Iterable<U>> parent, MetaNamedMember<? super U,T> child) {
        return (NestedMember<S, Iterable<T>>) new NestedMember<S,T>(parent, child, Function.id(), false);
    }

    public static final <S,T> MetaNamedMember<S,T> unchecked(MetaNamedMember<S, ?> parent, MetaNamedMember<?,T> child) {
        return new NestedMember<S,T>(parent, child, Function.id(), false);
    }
    
    @SuppressWarnings("unchecked")
    public static final <S,U,T> NestedMember<S,Iterable<T>> ofOptionItFlatType(MetaNamedMember<S, ? extends Option<? extends Iterable<U>>> parent, MetaNamedMember<? super U,? extends Iterable<T>> child) {
        return new NestedMember<S,Iterable<T>>(parent, (MetaNamedMember<?, Iterable<T>>) child, (Function1<Object,Object>)(Object)Transformers.flatten(), true);
    }

    private NestedMember(MetaNamedMember<S, ?> parent, MetaNamedMember<?,T> child, Function1<Object,Object> parentModifier, boolean flatten) {
        this.parent = parent;
        this.child = child;
        this.parentModifier = parentModifier;
        this.flatten = flatten;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T apply(S t) {
        // Hmm, I somehow feel bad about this...
        Object u = parentModifier.apply(parent.apply(t));
        if (u == null) {
            return null;
        } else if (u instanceof Option) {
            if (flatten) {
                return (T) Functional.flatten((Iterable<? extends Iterable<? extends T>>) filter(not(Predicates.isNull()), map((MetaNamedMember<Object,T>)child, (Iterable<Object>)u)));
            } else if (((Option<?>) u).isDefined()) {
                return (T)Option.of(((MetaNamedMember<Object,T>)child).apply(((Option<?>)u).get()));
            } else {
                return (T)None();
            }
        } else if (u instanceof Iterable) {
            // not sure if this is smart...
            if (flatten) {
                return (T) Functional.flatten((Iterable<? extends Iterable<? extends T>>) filter(not(Predicates.isNull()), map((MetaNamedMember<Object,T>)child, (Iterable<Object>)u)));
            } else {
                return (T) filter(not(Predicates.isNull()), map((MetaNamedMember<Object,T>)child, (Iterable<Object>)u));
            }
        } else {
            return ((MetaNamedMember<Object,T>)child).apply(u);
        }
    }
    
    @SuppressWarnings("unchecked")
    public <U> NestedMember<S,T> modifyParent(Apply<U,U> modifier) {
        return new NestedMember<S,T>(parent, child, parentModifier.andThen((Apply<? super Object, ? extends U>) modifier), flatten);
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

    @Override
    public String toString() {
        return getName();
    }    
}
