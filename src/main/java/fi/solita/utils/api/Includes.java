package fi.solita.utils.api;

import java.util.Iterator;

import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class Includes<T> implements Iterable<MetaNamedMember<T,?>> {

    public final Iterable<MetaNamedMember<T, ?>> includes;
    public final Iterable<MetaNamedMember<T, ?>> geometryMembers;
    public final Builder<?>[] builders;
    public final boolean includesEverything;

    @SuppressWarnings("unchecked")
    public Includes(Iterable<? extends MetaNamedMember<? super T,?>> includes, Iterable<? extends MetaNamedMember<? super T, ?>> geometryMembers, boolean includesEverything, Builder<?>... builders) {
        this.includes = (Iterable<MetaNamedMember<T, ?>>) includes;
        this.geometryMembers = (Iterable<MetaNamedMember<T, ?>>) geometryMembers;
        this.builders = builders;
        this.includesEverything = includesEverything;
    }
    
    @Override
    public Iterator<MetaNamedMember<T, ?>> iterator() {
        return includes.iterator();
    }

}
