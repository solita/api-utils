package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;

import java.util.Iterator;
import java.util.List;

import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class Includes<T> implements Iterable<MetaNamedMember<T,?>> {

    public final List<MetaNamedMember<T, ?>> includes;
    public final List<MetaNamedMember<T, ?>> geometryMembers;
    public final Builder<?>[] builders;
    public final boolean includesEverything;

    public static final <T> Includes<T> none() {
        return new Includes<T>(Collections.<MetaNamedMember<? super T, ?>>emptyList(), Collections.<MetaNamedMember<? super T, ?>>emptyList(), false);
    }
    
    public static final <T> Includes<T> all(Iterable<? extends MetaNamedMember<? super T,?>> includes) {
        return new Includes<T>(includes, Collections.<MetaNamedMember<? super T, ?>>emptyList(), true);
    }
    
    @SuppressWarnings("unchecked")
    public Includes(Iterable<? extends MetaNamedMember<? super T,?>> includes, Iterable<? extends MetaNamedMember<? super T, ?>> geometryMembers, boolean includesEverything, Builder<?>... builders) {
        this.includes = newList((Iterable<MetaNamedMember<T, ?>>) includes);
        this.geometryMembers = newList((Iterable<MetaNamedMember<T, ?>>) geometryMembers);
        this.builders = builders;
        this.includesEverything = includesEverything;
    }
    
    @Override
    public Iterator<MetaNamedMember<T, ?>> iterator() {
        return includes.iterator();
    }

}
