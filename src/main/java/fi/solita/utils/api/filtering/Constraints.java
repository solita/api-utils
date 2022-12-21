package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.not;

import java.util.List;
import java.util.Map;
import java.util.Set;

import fi.solita.utils.api.filtering.Constraints_.Or_;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicates;
import fi.solita.utils.functional.Tuple;
import fi.solita.utils.meta.MetaNamedMember;

public class Constraints<T> {
    private List<Or<T>> or;
    
    public List<Or<T>> or() {
        return or;
    }
    
    public boolean isEmpty() {
        return or.isEmpty();
    }
    
    public static final <T> Constraints<T> empty() { return new Constraints<T>(Collections.<Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>>>emptyList()); }
    
    public Constraints(List<Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>>> filters) {
        this.or = newList(map(Or_.<T>$(), filter(not(Predicates.<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>,Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>>>mapEmpty()), filters)));
    }
    
    public static class Or<T> {
        private Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> filters;
        
        public boolean isEmpty() {
            return filters.isEmpty();
        }
        
        public Or(Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> filters) {
            this.filters = filters;
        }
        
        public <V> Iterable<V> equal(MetaNamedMember<? super T,V> candidate) {
            return single(FilterType.EQUAL, candidate);
        }
        
        public <V> Iterable<V> notEqual(MetaNamedMember<? super T,V> candidate) {
            return single(FilterType.NOT_EQUAL, candidate);
        }
        
        public <V> Iterable<Set<V>> in(MetaNamedMember<? super T,V> candidate) {
            return multi(FilterType.IN, candidate);
        }
        
        public <V> Iterable<Set<V>> notIn(MetaNamedMember<? super T,V> candidate) {
            return multi(FilterType.NOT_IN, candidate);
        }
        
        public <V> Iterable<V> greaterThan(MetaNamedMember<? super T,V> candidate) {
            return single(FilterType.GT, candidate);
        }
        
        public <V> Iterable<V> greaterThanOrEqual(MetaNamedMember<? super T,V> candidate) {
            return single(FilterType.GTE, candidate);
        }
        
        public <V> Iterable<V> lessThan(MetaNamedMember<? super T,V> candidate) {
            return single(FilterType.LT, candidate);
        }
        
        public <V> Iterable<V> lessThanOrEqual(MetaNamedMember<? super T,V> candidate) {
            return single(FilterType.LTE, candidate);
        }
        
        public <V> Iterable<Pair<V,V>> between(MetaNamedMember<? super T,V> candidate) {
            return pair(FilterType.BETWEEN, candidate);
        }
        
        public <V> Iterable<Pair<V,V>> notBetween(MetaNamedMember<? super T,V> candidate) {
            return pair(FilterType.NOT_BETWEEN, candidate);
        }
        
        public <V> Iterable<Void> isNull(MetaNamedMember<? super T,V> candidate) {
            return empty(FilterType.NULL, candidate);
        }
        
        public <V> Iterable<Void> isNotNull(MetaNamedMember<? super T,V> candidate) {
            return empty(FilterType.NOT_NULL, candidate);
        }
        
        public <V> Iterable<String> like(MetaNamedMember<? super T,V> candidate) {
            return string(FilterType.LIKE, candidate);
        }
        
        public <V> Iterable<String> notLike(MetaNamedMember<? super T,V> candidate) {
            return string(FilterType.NOT_LIKE, candidate);
        }
        
        public <V> Iterable<String> ilike(MetaNamedMember<? super T,V> candidate) {
            return string(FilterType.ILIKE, candidate);
        }
        
        public <V> Iterable<String> notILike(MetaNamedMember<? super T,V> candidate) {
            return string(FilterType.NOT_ILIKE, candidate);
        }
        
        @SuppressWarnings("unchecked")
        private <V> Iterable<V> single(FilterType pattern, final MetaNamedMember<? super T,V> candidate) {
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
        
        private <V> Iterable<String> string(FilterType pattern, final MetaNamedMember<? super T,V> candidate) {
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
        
        private <V> Iterable<Pair<V,V>> pair(FilterType pattern, final MetaNamedMember<? super T,V> candidate) {
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
        private <V> Iterable<Set<V>> multi(FilterType pattern, final MetaNamedMember<? super T,V> candidate) {
            return flatMap(new Apply<Pair<MetaNamedMember<T, Object>, ?>, Option<Set<V>>>() {
                @Override
                public Option<Set<V>> apply(Pair<MetaNamedMember<T, Object>, ?> filter) {
                    if (candidate.equals(filter.left())) {
                        return Some(newSet((List<V>)filter.right()));
                    }
                    return None();
                }
            }, flatten(find(pattern, filters)));
        }
        
        private <V> Iterable<Void> empty(FilterType pattern, final MetaNamedMember<? super T,V> candidate) {
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
}