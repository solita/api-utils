package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newLinkedMap;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Collections.newSortedMap;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.headOption;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalM.groupBy;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.greaterThan;
import static fi.solita.utils.functional.Predicates.greaterThanOrEqualTo;
import static fi.solita.utils.functional.Predicates.isDefined;
import static fi.solita.utils.functional.Predicates.lessThan;
import static fi.solita.utils.functional.Predicates.lessThanOrEqualTo;
import static fi.solita.utils.functional.Predicates.not;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import fi.solita.utils.api.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.base.HttpModule;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.Filters.Filter;
import fi.solita.utils.api.types.Filters_.Filter_;
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
    
    public Filtering(HttpModule httpModule, ResolvableMemberProvider resolvableMemberProvider) {
        this.httpModule = httpModule;
        this.resolvableMemberProvider = resolvableMemberProvider;
    }
    
    @SuppressWarnings("unchecked")
    public <T> Constraints<T> with(Filters filters, Includes<T> includes) {
        if (filters == null) {
            return Constraints.empty();
        }
        
        Map<Pattern,List<Pair<MetaNamedMember<T,Object>,?>>> c = newMap();
        
        for (Entry<Pattern, List<Filter>> f: groupBy(Filter_.pattern, filters.filters).entrySet()) {
            List<Pair<MetaNamedMember<T, Object>, ?>> lst = Collections.<Pair<MetaNamedMember<T,Object>,?>>newList();
            c.put(f.getKey(), lst);
            for (Filter filter: f.getValue()) {
                MetaNamedMember<T,Object> member;
                try {
                    member = (MetaNamedMember<T, Object>) Assert.singleton(MemberUtil.toMembers(resolvableMemberProvider, includes.includes, filter.property));
                } catch (UnknownPropertyNameException e) {
                    throw new Filtering.FilterPropertyNotFoundException(filter.property, e);
                }
                lst.add(Pair.of(member, newList(map(Filtering_.convert().ap(this, (MetaNamedMember<? super T,Object>)member), filter.values))));
            }
        }
        
        return new Constraints<T>(c);
    }
    
    public static class SpatialFilteringRequiresGeometryPropertyException extends RuntimeException {
        public final String filteringProperty;
        public final Set<CharSequence> geometryProperties;

        public SpatialFilteringRequiresGeometryPropertyException(String filteringProperty, Set<CharSequence> geometryProperties) {
            this.filteringProperty = filteringProperty;
            this.geometryProperties = geometryProperties;
        }
    }
    
    public static class FilterPropertyNotFoundException extends RuntimeException {
        public final String filterProperty;

        public FilterPropertyNotFoundException(String filterProperty, Throwable cause) {
            super(filterProperty, cause);
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
        return (T)httpModule.convert(value, MemberUtil.actualTypeUnwrappingOptionAndEither(m));
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
            if (filter.pattern == Filters.INTERSECTS) {
                if (MemberUtil.toMembers(resolvableMemberProvider, geometryMembers, filter.property).isEmpty()) {
                    throw new SpatialFilteringRequiresGeometryPropertyException(filter.property, newSet(map(MemberUtil_.memberName, geometryMembers)));
                }
                // TODO: filtering is skipped here. Assuming it's been already done in the DB
            } else {
                MetaNamedMember<? super T,?> member;
                try {
                    member = Assert.singleton(MemberUtil.toMembers(resolvableMemberProvider, includes, filter.property));
                } catch (UnknownPropertyNameException e) {
                    throw new FilterPropertyNotFoundException(filter.property, e);
                }
                
                if (member instanceof ResolvableMember) {
                    throw new CannotFilterByResolvableException(member.getName());
                }
                
                // TODO: make filters work with collections (Any) (https://github.com/geotools/geotools/wiki/support-multi-valued-attributes-in-filter-comparison-operators)
                if (filter.pattern == Filters.EQUAL) {
                    ts = equal(member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.NOT_EQUAL) {
                    ts = notEqual(member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.LT) {
                    ts = lt((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.GT) {
                    ts = gt((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.LTE) {
                    ts = lte((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.GTE) {
                    ts = gte((MetaNamedMember<? super T,? extends Comparable>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.BETWEEN) {
                    Pair<String, String> vals = Match.pair(filter.values).success.get();
                    ts = between((MetaNamedMember<? super T,? extends Comparable>)member, vals.left(), vals.right(), ts);
                } else if (filter.pattern == Filters.NOT_BETWEEN) {
                    Pair<String, String> vals = Match.pair(filter.values).success.get();
                    ts = notBetween((MetaNamedMember<? super T,? extends Comparable>)member, vals.left(), vals.right(), ts);
                
                // TODO: could these be made to work also with other than String-properties, by comparing to the chosen serialization format?
                } else if (filter.pattern == Filters.LIKE) {
                    ts = like((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.NOT_LIKE) {
                    ts = notLike((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.ILIKE) {
                    ts = ilike((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                } else if (filter.pattern == Filters.NOT_ILIKE) {
                    ts = notILike((MetaNamedMember<? super T,String>)member, Assert.singleton(filter.values), ts);
                    
                } else if (filter.pattern == Filters.IN) {
                    ts = in(member, filter.values, ts);
                } else if (filter.pattern == Filters.NOT_IN) {
                    ts = notIn(member, filter.values, ts);
                } else if (filter.pattern == Filters.NULL) {
                    if (!filter.values.isEmpty()) {
                        throw new IllegalArgumentException("Should be empty");
                    }
                    ts = isNull((MetaNamedMember<? super T,Option<Object>>)member, ts);
                } else if (filter.pattern == Filters.NOT_NULL) {
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
        return t instanceof Option ? ((Option<T>)t).getOrElse(null) : t;
    }
    
    public <T,V> Iterable<T> equal(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(equalTo(convert(member, value)))), ts);
    }
    
    public <T,V> Iterable<T> notEqual(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(Predicates.<V>isNull().or(not(equalTo(convert(member, value))))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> lt(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(lessThan(convert(member, value)))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> gt(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(greaterThan(convert(member, value)))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> gte(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(greaterThanOrEqualTo(convert(member, value)))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> lte(MetaNamedMember<? super T,V> member, String value, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(lessThanOrEqualTo(convert(member, value)))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> between(MetaNamedMember<? super T,V> member, String value1, String value2, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(Predicates.between(convert(member, value1), convert(member, value2)))), ts);
    }
    
    public <T,V extends Comparable<V>> Iterable<T> notBetween(MetaNamedMember<? super T,V> member, String value1, String value2, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(Predicates.<V>isNull().or(not(Predicates.between(convert(member, value1), convert(member, value2))))), ts);
    }
    
    public <T> Iterable<T> like(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(Filtering_.matches.apply(Function.__, likePattern.replace("%", ".*")))), ts);
    }
    
    public <T> Iterable<T> notLike(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(Predicates.<String>isNull().or(not(Filtering_.matches.apply(Function.__, likePattern.replace("%", ".*"))))), ts);
    }
    
    public <T> Iterable<T> ilike(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(Filtering_.matches.apply(Function.__, "(?i)" + likePattern.replace("%", ".*")))), ts);
    }
    
    public <T> Iterable<T> notILike(MetaNamedMember<? super T,String> member, String likePattern, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(Predicates.<String>isNull().or(not(Filtering_.matches.apply(Function.__, "(?i)" + likePattern.replace("%", ".*"))))), ts);
    }
    
    public <T,V> Iterable<T> in(MetaNamedMember<? super T,V> member, List<String> values, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(Filtering_.contains.ap(newSet(map(Filtering_.<V>convert().ap(this, member), values))))), ts);
    }
    
    public <T,V> Iterable<T> notIn(MetaNamedMember<? super T,V> member, List<String> values, Iterable<T> ts) {
        return filter(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(Predicates.<V>isNull().or(not(Filtering_.contains.ap(newSet(map(Filtering_.<V>convert().ap(this, member), values)))))), ts);
    }
    
    public <T,V> Iterable<T> isNull(MetaNamedMember<? super T,Option<V>> member, Iterable<T> ts) {
        return filter(Function.of(member).andThen(not(isDefined)), ts);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Iterable<T> isNotNull(MetaNamedMember<? super T,?> member, Iterable<T> ts) {
        return filter(Function.of(member).andThen(not(Filtering_.isOption).or((Predicate<Object>)(Object)isDefined)), ts);
    }
    
    static boolean matches(String str, String regex) {
        return str.matches(regex);
    }
    
    static boolean contains(Set<?> set, Object value) {
        return set.contains(value);
    }
    
    static boolean isOption(Object o) {
        return Option.class.isInstance(o);
    }
}
