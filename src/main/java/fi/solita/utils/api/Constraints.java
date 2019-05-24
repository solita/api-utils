package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptySet;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import fi.solita.utils.api.types.Filters;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.meta.MetaNamedMember;

public class Constraints<T> {
    private Map<Pattern,List<Pair<MetaNamedMember<T,Object>,?>>> filters;
    
    public static final <T> Constraints<T> empty() { return new Constraints<T>(Collections.<Pattern,List<Pair<MetaNamedMember<T,Object>,?>>>emptyMap()); }
    
    public Constraints(Map<Pattern,List<Pair<MetaNamedMember<T,Object>,?>>> filters) {
        this.filters = filters;
    }
    
    public <V> Iterable<V> equal(MetaNamedMember<? super T,V> candidate) {
        return equal(Filters.EQUAL, candidate);
    }
    
    public <V> Iterable<V> notEqual(MetaNamedMember<? super T,V> candidate) {
        return equal(Filters.NOT_EQUAL, candidate);
    }
    
    public <V> Iterable<Set<V>> in(MetaNamedMember<? super T,V> candidate) {
        return in(Filters.IN, candidate);
    }
    
    public <V> Iterable<Set<V>> notIn(MetaNamedMember<? super T,V> candidate) {
        return in(Filters.NOT_IN, candidate);
    }
    
    @SuppressWarnings("unchecked")
    private <V> Iterable<V> equal(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
        return flatMap(new Apply<Pair<MetaNamedMember<T, Object>, ?>, Option<V>>() {
            @Override
            public Option<V> apply(Pair<MetaNamedMember<T, Object>, ?> filter) {
                if (candidate.equals(filter.left())) {
                    return Some((V)filter.right());
                }
                return None();
            }
        }, flatten(find(pattern, filters)));
    }
    
    @SuppressWarnings("unchecked")
    private <V> Iterable<Set<V>> in(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
        return map(new Apply<Pair<MetaNamedMember<T, Object>, ?>, Set<V>>() {
            @Override
            public Set<V> apply(Pair<MetaNamedMember<T, Object>, ?> filter) {
                if (candidate.equals(filter.left())) {
                    return newSet((List<V>)filter.right());
                }
                return emptySet();
            }
        }, flatten(find(pattern, filters)));
    }
}