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
    private Map<Pattern,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> filters;
    
    public static final <T> Constraints<T> empty() { return new Constraints<T>(Collections.<Pattern,List<Pair<MetaNamedMember<T,Object>,List<Object>>>>emptyMap()); }
    
    public Constraints(Map<Pattern,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> filters) {
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
    
    public <V> Iterable<V> greaterThan(MetaNamedMember<? super T,V> candidate) {
        return equal(Filters.GT, candidate);
    }
    
    public <V> Iterable<V> greaterThanOrEqual(MetaNamedMember<? super T,V> candidate) {
        return equal(Filters.GTE, candidate);
    }
    
    public <V> Iterable<V> lessThan(MetaNamedMember<? super T,V> candidate) {
        return equal(Filters.LT, candidate);
    }
    
    public <V> Iterable<V> lessThanOrEqual(MetaNamedMember<? super T,V> candidate) {
        return equal(Filters.LTE, candidate);
    }
    
    @SuppressWarnings("unchecked")
    private <V> Iterable<V> equal(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
        return flatMap(new Apply<Pair<MetaNamedMember<T, Object>, List<Object>>, Option<V>>() {
            @Override
            public Option<V> apply(Pair<MetaNamedMember<T, Object>, List<Object>> filter) {
                if (candidate.equals(filter.left())) {
                    return Some((V)Assert.singleton(filter.right()));
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