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
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaNamedMember;

public class Constraints<T> {
    private Map<Pattern,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> filters;
    
    public static final <T> Constraints<T> empty() { return new Constraints<T>(Collections.<Pattern,List<Pair<MetaNamedMember<T,Object>,List<Object>>>>emptyMap()); }
    
    public boolean isEmpty() {
        return filters.isEmpty();
    }
    
    public Constraints(Map<Pattern,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> filters) {
        this.filters = filters;
    }
    
    public <V> Iterable<V> equal(MetaNamedMember<? super T,V> candidate) {
        return single(Filters.EQUAL, candidate);
    }
    
    public <V> Iterable<V> notEqual(MetaNamedMember<? super T,V> candidate) {
        return single(Filters.NOT_EQUAL, candidate);
    }
    
    public <V> Iterable<Set<V>> in(MetaNamedMember<? super T,V> candidate) {
        return multi(Filters.IN, candidate);
    }
    
    public <V> Iterable<Set<V>> notIn(MetaNamedMember<? super T,V> candidate) {
        return multi(Filters.NOT_IN, candidate);
    }
    
    public <V> Iterable<V> greaterThan(MetaNamedMember<? super T,V> candidate) {
        return single(Filters.GT, candidate);
    }
    
    public <V> Iterable<V> greaterThanOrEqual(MetaNamedMember<? super T,V> candidate) {
        return single(Filters.GTE, candidate);
    }
    
    public <V> Iterable<V> lessThan(MetaNamedMember<? super T,V> candidate) {
        return single(Filters.LT, candidate);
    }
    
    public <V> Iterable<V> lessThanOrEqual(MetaNamedMember<? super T,V> candidate) {
        return single(Filters.LTE, candidate);
    }
    
    public <V> Iterable<Pair<V,V>> between(MetaNamedMember<? super T,V> candidate) {
        return pair(Filters.BETWEEN, candidate);
    }
    
    public <V> Iterable<Pair<V,V>> notBetween(MetaNamedMember<? super T,V> candidate) {
        return pair(Filters.NOT_BETWEEN, candidate);
    }
    
    public <V> Iterable<Void> isNull(MetaNamedMember<? super T,V> candidate) {
        return empty(Filters.NULL, candidate);
    }
    
    public <V> Iterable<Void> isNotNull(MetaNamedMember<? super T,V> candidate) {
        return empty(Filters.NOT_NULL, candidate);
    }
    
    public <V> Iterable<String> like(MetaNamedMember<? super T,V> candidate) {
        return string(Filters.LIKE, candidate);
    }
    
    public <V> Iterable<String> notLike(MetaNamedMember<? super T,V> candidate) {
        return string(Filters.NOT_LIKE, candidate);
    }
    
    public <V> Iterable<String> ilike(MetaNamedMember<? super T,V> candidate) {
        return string(Filters.ILIKE, candidate);
    }
    
    public <V> Iterable<String> notILike(MetaNamedMember<? super T,V> candidate) {
        return string(Filters.NOT_ILIKE, candidate);
    }
    
    @SuppressWarnings("unchecked")
    private <V> Iterable<V> single(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
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
    
    private <V> Iterable<String> string(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
        return flatMap(new Apply<Pair<MetaNamedMember<T, Object>, List<Object>>, Option<String>>() {
            @Override
            public Option<String> apply(Pair<MetaNamedMember<T, Object>, List<Object>> filter) {
                if (candidate.equals(filter.left())) {
                    return Some((String)Assert.singleton(filter.right()));
                }
                return None();
            }
        }, flatten(find(pattern, filters)));
    }
    
    private <V> Iterable<Pair<V,V>> pair(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
        return flatMap(new Apply<Pair<MetaNamedMember<T, Object>, List<Object>>, Option<Pair<V,V>>>() {
            @SuppressWarnings("unchecked")
            @Override
            public Option<Pair<V,V>> apply(Pair<MetaNamedMember<T, Object>, List<Object>> filter) {
                if (candidate.equals(filter.left())) {
                    return Some((Pair<V,V>)Tuple.of(filter.right().toArray()));
                }
                return None();
            }
        }, flatten(find(pattern, filters)));
    }
    
    @SuppressWarnings("unchecked")
    private <V> Iterable<Set<V>> multi(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
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
    
    private <V> Iterable<Void> empty(Pattern pattern, final MetaNamedMember<? super T,V> candidate) {
        return flatMap(new Apply<Pair<MetaNamedMember<T, Object>, ?>, Option<Void>>() {
            @Override
            public Option<Void> apply(Pair<MetaNamedMember<T, Object>, ?> filter) {
                if (candidate.equals(filter.left())) {
                    return Some(null);
                }
                return None();
            }
        }, flatten(find(pattern, filters)));
    }

}