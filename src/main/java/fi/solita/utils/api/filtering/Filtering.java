package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newLinkedMap;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Collections.newSortedMap;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.forall;
import static fi.solita.utils.functional.Functional.headOption;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.FunctionalM.groupBy;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.greaterThan;
import static fi.solita.utils.functional.Predicates.greaterThanOrEqualTo;
import static fi.solita.utils.functional.Predicates.lessThan;
import static fi.solita.utils.functional.Predicates.lessThanOrEqualTo;
import static fi.solita.utils.functional.Predicates.not;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.base.http.HttpModule;
import fi.solita.utils.api.functions.FunctionCallMember;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMember;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.util.MemberUtil_;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Match;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import fi.solita.utils.functional.Predicates;
import fi.solita.utils.meta.MetaNamedMember;

public class Filtering {
    private final HttpModule httpModule;
    final ResolvableMemberProvider resolvableMemberProvider;
    final FunctionProvider fp;
    
    public Filtering(HttpModule httpModule, ResolvableMemberProvider resolvableMemberProvider, FunctionProvider fp) {
        this.httpModule = httpModule;
        this.resolvableMemberProvider = resolvableMemberProvider;
        this.fp = fp;
    }
    
    @SuppressWarnings("unchecked")
    public <T> Constraints<T> with(Filters filters, Includes<T> includes) {
        if (filters == null) {
            return Constraints.empty();
        }
        
        Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> c = newMap();
        
        Set<FilterType> spatialFilters = newSet(map(new Apply<Filter, FilterType>() {
            @Override
            public FilterType apply(Filter t) {
                return t.pattern;
            }
        }, filters.spatialFilters()));
        
        for (Entry<FilterType, List<Filter>> f: groupBy(Filter_.pattern, filters.filters).entrySet()) {
            List<Pair<MetaNamedMember<T, Object>, List<Object>>> lst = Collections.<Pair<MetaNamedMember<T,Object>,List<Object>>>newList();
            c.put(f.getKey(), lst);
            for (Filter filter: f.getValue()) {
                MetaNamedMember<T,Object> member;
                try {
                    member = (MetaNamedMember<T, Object>) Assert.singleton(filter(MemberUtil_.memberName.andThen(equalTo(filter.property.getValue())), includes.includes));
                } catch (UnknownPropertyNameException e) {
                    throw new Filtering.FilterPropertyNotFoundException(filter.property, e);
                }
                if (!spatialFilters.contains(filter.pattern)) {
                    // leave spatial filters out from constraints for now
                    lst.add(Pair.of(member, newList(map(Filtering_.convert().ap(this, (MetaNamedMember<? super T,Object>)member), filter.values))));
                }
            }
        }
        
        return new Constraints<T>(c);
    }
    
    public static class SpatialFilteringRequiresGeometryPropertyException extends RuntimeException {
        public final PropertyName filteringProperty;
        public final Set<String> geometryProperties;

        public SpatialFilteringRequiresGeometryPropertyException(PropertyName filteringProperty, Set<String> geometryProperties) {
            this.filteringProperty = filteringProperty;
            this.geometryProperties = geometryProperties;
        }
    }
    
    public static class FilterPropertyNotFoundException extends RuntimeException {
        public final PropertyName filterProperty;

        public FilterPropertyNotFoundException(PropertyName filterProperty, Throwable cause) {
            super(filterProperty.toString(), cause);
            this.filterProperty = filterProperty;
        }
    }
    
    public static class CannotFilterByResolvableException extends RuntimeException {
        public final String filterProperty;

        public CannotFilterByResolvableException(String filterProperty) {
            super(filterProperty);
            this.filterProperty = filterProperty;
        }
    }
    
    @SuppressWarnings("unchecked")
    public <T> T convert(MetaNamedMember<?,T> m, String value) {
        return (T)httpModule.convert(value, MemberUtil.actualTypeUnwrappingOptionAndEitherAndIterables(m));
    }
    
    public <K,T,V extends Iterable<T>> Map<K,V> filterData(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, Map<K,V> ts) {
        if (filters == null) {
            return ts;
        }
        Map<K, V> ret = newLinkedMap();
        for (Map.Entry<K,V> e: ts.entrySet()) {
            // Keeps the whole key (all values) if any value satisfies filters
            if (!isEmpty(filterData(includes, geometryMembers, filters, e.getValue()))) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }
    
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filterData(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, SortedMap<K,V> ts) {
        if (filters == null) {
            return ts;
        }
        SortedMap<K, V> ret = newSortedMap(ts.comparator());
        for (Map.Entry<K,V> e: ts.entrySet()) {
            // Keeps the whole key (all values) if any value satisfies filters
            if (!isEmpty(filterData(includes, geometryMembers, filters, e.getValue()))) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }
    
    public <T> Option<T> filterData(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, T t) {
        return headOption(filterData(includes, geometryMembers, filters, newList(t)));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <T> Iterable<T> filterData(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, Iterable<T> ts) {
        if (filters == null) {
            return ts;
        }
        
        for (Filter filter: filters.filters) {
            if (filter.pattern == FilterType.INTERSECTS) {
                if (MemberUtil.toMembers(resolvableMemberProvider, fp, geometryMembers, filter.property).isEmpty()) {
                    throw new SpatialFilteringRequiresGeometryPropertyException(filter.property, newSet(map(MemberUtil_.memberName, geometryMembers)));
                }
                // filtering is skipped here. Assuming it's been already done in the DB
            } else {
                MetaNamedMember<? super T,?> member;
                try {
                    member = Assert.singleton(filter(MemberUtil_.memberName.andThen(equalTo(filter.property.getValue())), includes));
                } catch (UnknownPropertyNameException e) {
                    throw new FilterPropertyNotFoundException(filter.property, e);
                }
                
                if (member instanceof ResolvableMember) {
                    throw new CannotFilterByResolvableException(member.getName());
                }
                if (member instanceof FunctionCallMember) {
                    throw new CannotFilterByResolvableException(member.getName());
                }
                
                if (filter.pattern == FilterType.EQUAL) {
                    ts = equal(member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.NOT_EQUAL) {
                    ts = notEqual(member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.LT) {
                    ts = lt((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.GT) {
                    ts = gt((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.LTE) {
                    ts = lte((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.GTE) {
                    ts = gte((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.BETWEEN) {
                    Pair<String, String> vals = Match.pair(filter.values).success.get();
                    ts = between((MetaNamedMember<? super T,? extends Comparable>)member, vals.left(), vals.right(), ts);
                } else if (filter.pattern == FilterType.NOT_BETWEEN) {
                    Pair<String, String> vals = Match.pair(filter.values).success.get();
                    ts = notBetween((MetaNamedMember<? super T,? extends Comparable>)member, vals.left(), vals.right(), ts);
                
                } else if (filter.pattern == FilterType.LIKE) {
                    ts = like((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.NOT_LIKE) {
                    ts = notLike((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.ILIKE) {
                    ts = ilike((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == FilterType.NOT_ILIKE) {
                    ts = notILike((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                    
                } else if (filter.pattern == FilterType.IN) {
                    ts = in(member, filter.values, ts);
                } else if (filter.pattern == FilterType.NOT_IN) {
                    ts = notIn(member, filter.values, ts);
                } else if (filter.pattern == FilterType.NULL) {
                    if (!filter.values.isEmpty()) {
                        throw new IllegalArgumentException("Should be empty");
                    }
                    ts = isNull((MetaNamedMember<? super T,Option<Object>>)member, ts);
                } else if (filter.pattern == FilterType.NOT_NULL) {
                    if (!filter.values.isEmpty()) {
                        throw new IllegalArgumentException("Should be empty");
                    }
                    ts = isNotNull(member, ts);
                } else {
                    throw new UnsupportedOperationException("Not implemented: " + filter.pattern);
                }
            }
        }
        return ts;
    }
    
    @SuppressWarnings("unchecked")
    static <T> T unwrapOption(T t) {
        return t instanceof Option ? unwrapOption(((Option<T>)t).getOrElse(null)) : t;
    }
    
    public enum MatchAction {
        Any,
        All,
        One
    }
    
    static <T> Predicate<T> doFilter(final MatchAction matchAction, final Apply<T,Boolean> pred) {
        return new Predicate<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean accept(T candidate) {
                if (candidate instanceof Collection) {
                    switch (matchAction) {
                        case All: return forall(pred, (Collection<T>)candidate);
                        case Any: return exists(pred, (Collection<T>)candidate);
                        case One: return 1 == size(filter(pred, (Collection<T>)candidate));
                    }
                    throw new IllegalStateException("Shouldn't be here!");
                } else {
                    return pred.apply(candidate);
                }
            }
        };
    }
    
    public <T,V> Iterable<T> equal(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, equalTo(convert(member, value))))), ts);
    }
    
    public <T,V> Iterable<T> notEqual(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, not(equalTo(convert(member, value)))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> lt(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, lessThan(convert(member, value))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> gt(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, greaterThan(convert(member, value))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> gte(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, greaterThanOrEqualTo(convert(member, value))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> lte(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, lessThanOrEqualTo(convert(member, value))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> between(MetaNamedMember<? super T,V> member, String value1, String value2, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, Predicates.between(convert(member, value1), convert(member, value2))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> notBetween(MetaNamedMember<? super T,V> member, String value1, String value2, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, not(Predicates.between(convert(member, value1), convert(member, value2)))))), ts);
    }
    
    public <T> Iterable<T> like(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, Filtering_.matches.apply(Function.__, likePattern.replace("%", ".*"))))), ts);
    }
    
    public <T> Iterable<T> notLike(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, not(Filtering_.matches.apply(Function.__, likePattern.replace("%", ".*")))))), ts);
    }
    
    public <T> Iterable<T> ilike(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, Filtering_.matches.apply(Function.__, "(?i)" + likePattern.replace("%", ".*"))))), ts);
    }
    
    public <T> Iterable<T> notILike(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, not(Filtering_.matches.apply(Function.__, "(?i)" + likePattern.replace("%", ".*")))))), ts);
    }
    
    public <T,V> Iterable<T> in(MetaNamedMember<? super T,V> member, List<String> values, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, Filtering_.contains.ap(newSet(map(Filtering_.<V>convert().ap(this, member), values)))))), ts);
    }
    
    public <T,V> Iterable<T> notIn(MetaNamedMember<? super T,V> member, List<String> values, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, not(Filtering_.contains.ap(newSet(map(Filtering_.<V>convert().ap(this, member), values))))))), ts);
    }
    
    public <T,V> Iterable<T> isNull(MetaNamedMember<? super T,Option<V>> member, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.isNullOrEmpty), ts);
    }
    
    public <T> Iterable<T> isNotNull(MetaNamedMember<? super T,?> member, Iterable<T> ts) {
        return filter(Function.of(member).andThen(not(Filtering_.isNullOrEmpty)), ts);
    }
    
    static boolean isNullOrEmpty(Object x) {
        return x instanceof Option<?> && !((Option<?>)x).isDefined() ||
               x instanceof Collection<?> && ((Collection<?>)x).isEmpty();
    }
    
    static boolean matches(String str, String regex) {
        return str.matches(regex);
    }
    
    static boolean contains(Set<?> set, Object value) {
        return set.contains(value);
    }
}
