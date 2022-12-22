package fi.solita.utils.api.filtering;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableLinkedMap;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Collections.newMutableMap;
import static fi.solita.utils.functional.Collections.newMutableSortedMap;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.forall;
import static fi.solita.utils.functional.Functional.headOption;
import static fi.solita.utils.functional.Functional.isEmpty;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.FunctionalM.groupBy;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.greaterThan;
import static fi.solita.utils.functional.Predicates.greaterThanOrEqualTo;
import static fi.solita.utils.functional.Predicates.lessThan;
import static fi.solita.utils.functional.Predicates.lessThanOrEqualTo;
import static fi.solita.utils.functional.Predicates.not;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
import org.joda.time.Period;
import org.springframework.core.convert.ConverterNotFoundException;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.base.http.HttpModule;
import fi.solita.utils.api.functions.FunctionCallMember;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.functions.FunctionProvider_;
import fi.solita.utils.api.resolving.ResolvableMember;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.api.util.Assert;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.MemberUtil.UnknownPropertyNameException;
import fi.solita.utils.api.util.MemberUtil_;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Match;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Predicate;
import fi.solita.utils.functional.Predicates;
import fi.solita.utils.functional.Tuple3;
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
    
    @SuppressWarnings({ "unchecked" })
    public <T> Constraints<T> with(Filters filters, Includes<T> includes) {
        if (filters == null) {
            return Constraints.empty();
        }
        
        Set<FilterType> spatialFilters = newSet(map(Filter_.pattern, flatten(filters.spatialFilters())));
        
        List<Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>>> or = newMutableList();
        for (List<Filter> and: filters.or) {
            Map<FilterType,List<Pair<MetaNamedMember<T,Object>,List<Object>>>> c = newMutableMap();
            for (Map.Entry<FilterType, List<Filter>> f: groupBy(Filter_.pattern, and).entrySet()) {
                List<Pair<MetaNamedMember<T, Object>, List<Object>>> lst = Collections.<Pair<MetaNamedMember<T,Object>,List<Object>>>newMutableList();
                c.put(f.getKey(), lst);
                // leave out function calls, since the functions aren't found in the database anyway
                for (Filter filter: filter(not(Filter_.property.andThen(PropertyName_.isFunctionCall)), f.getValue())) {
                    MetaNamedMember<T,Object> member;
                    try {
                        member = (MetaNamedMember<T, Object>) Assert.singleton(
                            newSet(filter(MemberUtil_.memberName.andThen(equalTo(filter.property.toProperty(fp).getValue())),
                                includes.includesFromRowFiltering)));
                    } catch (UnknownPropertyNameException e) {
                        throw new Filtering.FilterPropertyNotFoundException(filter.property, e);
                    }
                    if (!spatialFilters.contains(filter.pattern)) {
                        // leave spatial filters out from constraints for now
                        Class<?> targetType = resolveTargetType(filter, member);
                        lst.add(Pair.of(member, newList(map(Filtering_.convert().ap(this, targetType), filter.literals))));
                    }
                }
            }
            or.add(c);
        }
        
        return new Constraints<T>(or);
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <T> MetaNamedMember<T, ?> unwrapFunctionCallMember(MetaNamedMember<T, ?> member) {
        if (member instanceof FunctionCallMember) {
            return ((FunctionCallMember) member).original;
        }
        return member;
    }
    
    private static final Set<? extends Class<?>> DURATION_COMPATIBLE = newSet(DateTime.class, Interval.class);
    private static final Pattern DURATION_PATTERN = Pattern.compile("PT.*");
    private static final Pattern PERIOD_PATTERN = Pattern.compile("P[^T].*");
    
    @SuppressWarnings({ "unchecked", "static-access" })
    <T> T convert(Class<?> targetType, Literal value) {
        if (value != null && value.getValue().isLeft() && !value.isStringLiteral() && fp.isFunctionCall(value.getValue().left.get())) {
            return (T) fp.apply(value.getValue().left.get(), fp.toArgument(value.getValue().left.get()));
        } else if (value != null && value.getValue().isRight()) {
            Tuple3<Literal, Character, Literal> val = value.getValue().right.get();
            return (T) fp.applyFunction(Character.toString(val._2),
                                        Option.<String>None(),
                                        Pair.of(convert(adjustTargetType(val._1, targetType), val._1),
                                                convert(adjustTargetType(val._3, targetType), val._3)));
        }
        try {
            return (T)httpModule.convert(value == null ? value : value.getValue().left.get(), targetType);
        } catch (ConverterNotFoundException e) {
            if (targetType.isAnnotationPresent(JsonSerializeAsBean.class)) {
                // wasn't even ment to be convertable -> client error
                throw new CannotFilterByStructureException();
            }
            throw e; // something's wrong -> server error
        }
    }

    private static final Class<? extends Object> adjustTargetType(Literal literal, Class<?> targetType) {
        if (DURATION_COMPATIBLE.contains(targetType) && !literal.isStringLiteral() && literal.getValue().isLeft()) {
            return DURATION_PATTERN.matcher(literal.getValue().left.get()).matches() ? Duration.class : 
                     PERIOD_PATTERN.matcher(literal.getValue().left.get()).matches() ? Period.class :
                                                                                       targetType;
        }
        return targetType;
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
    
    public static class CannotFilterByStructureException extends RuntimeException {
    }
    
    public <K,T> Map<K,T> filterDataSingle(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, Map<K,T> ts) {
        if (filters == null) {
            return ts;
        }
        Map<K, T> ret = newMutableLinkedMap();
        for (Map.Entry<K,T> e: ts.entrySet()) {
            if (!isEmpty(filterData(includes, geometryMembers, filters, e.getValue()))) {
                ret.put(e.getKey(), e.getValue());
            }
        }
        return ret;
    }
    
    public <K,T,V extends Iterable<T>> Map<K,V> filterData(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, Map<K,V> ts) {
        if (filters == null) {
            return ts;
        }
        Map<K, V> ret = newMutableLinkedMap();
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
        SortedMap<K, V> ret = newMutableSortedMap(ts.comparator());
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
    public final <T> Option<Predicate<T>> buildPredicate(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters) {
        Option<Predicate<T>> predicate = None();
        for (List<Filter> and: filters.or) {
          Predicate<T> pred = Predicate.<T>of(Function.<T,Boolean>constant(true));
          for (Filter filter: and) {
            if (filter.pattern == FilterType.INTERSECTS) {
                if (MemberUtil.toMembers(resolvableMemberProvider, fp, false, geometryMembers, filter.property).isEmpty()) {
                    throw new SpatialFilteringRequiresGeometryPropertyException(filter.property, newSet(map(MemberUtil_.memberName, geometryMembers)));
                }
                // filtering is skipped here. Assuming it's been already done in the DB
            } else {
                MetaNamedMember<? super T,?> member;
                try {
                    member = Assert.singleton(
                        newSet(map(Filtering_.<T>unwrapFunctionCallMember(),
                          filter(MemberUtil_.memberName.andThen(equalTo(filter.property.toProperty(fp).getValue())),
                              includes))));
                } catch (UnknownPropertyNameException e) {
                    throw new FilterPropertyNotFoundException(filter.property, e);
                }
                
                if (member instanceof ResolvableMember) {
                    throw new CannotFilterByResolvableException(member.getName());
                }
                Class<?> targetType = resolveTargetType(filter, member);
                if (filter.property.isFunctionCall()) {
                    member = new FunctionCallMember(filter.property, member).applied(FunctionProvider_.apply.ap(fp, filter.property.getValue()));
                }
                
                if (filter.pattern == FilterType.EQUAL) {
                    pred = pred.and(equal(member, Assert.singleton(filter.literals), targetType));
                } else if (filter.pattern == FilterType.NOT_EQUAL) {
                    pred = pred.and(notEqual(member, Assert.singleton(filter.literals), targetType));
                } else if (filter.pattern == FilterType.LT) {
                    pred = pred.and(lt((MetaNamedMember<T,? extends Comparable>)member, Assert.singleton(filter.literals), targetType));
                } else if (filter.pattern == FilterType.GT) {
                    pred = pred.and(gt((MetaNamedMember<T,? extends Comparable>)member, Assert.singleton(filter.literals), targetType));
                } else if (filter.pattern == FilterType.LTE) {
                    pred = pred.and(lte((MetaNamedMember<T,? extends Comparable>)member, Assert.singleton(filter.literals), targetType));
                } else if (filter.pattern == FilterType.GTE) {
                    pred = pred.and(gte((MetaNamedMember<T,? extends Comparable>)member, Assert.singleton(filter.literals), targetType));
                } else if (filter.pattern == FilterType.BETWEEN) {
                    Pair<Literal, Literal> vals = Match.pair(filter.literals).success.get();
                    pred = pred.and(between((MetaNamedMember<T,? extends Comparable>)member, vals.left(), vals.right(), targetType));
                } else if (filter.pattern == FilterType.NOT_BETWEEN) {
                    Pair<Literal, Literal> vals = Match.pair(filter.literals).success.get();
                    pred = pred.and(notBetween((MetaNamedMember<T,? extends Comparable>)member, vals.left(), vals.right(), targetType));
                
                } else if (filter.pattern == FilterType.LIKE) {
                    pred = pred.and(like((MetaNamedMember<T,String>)member, Assert.singleton(filter.literals).getValue().left.get()));
                } else if (filter.pattern == FilterType.NOT_LIKE) {
                    pred = pred.and(notLike((MetaNamedMember<T,String>)member, Assert.singleton(filter.literals).getValue().left.get()));
                } else if (filter.pattern == FilterType.ILIKE) {
                    pred = pred.and(ilike((MetaNamedMember<T,String>)member, Assert.singleton(filter.literals).getValue().left.get()));
                } else if (filter.pattern == FilterType.NOT_ILIKE) {
                    pred = pred.and(notILike((MetaNamedMember<T,String>)member, Assert.singleton(filter.literals).getValue().left.get()));
                    
                } else if (filter.pattern == FilterType.IN) {
                    pred = pred.and(in(member, filter.literals, targetType));
                } else if (filter.pattern == FilterType.NOT_IN) {
                    pred = pred.and(notIn(member, filter.literals, targetType));
                } else if (filter.pattern == FilterType.NULL) {
                    if (!filter.literals.isEmpty()) {
                        throw new IllegalArgumentException("Should be empty");
                    }
                    pred = pred.and(isNull(member));
                } else if (filter.pattern == FilterType.NOT_NULL) {
                    if (!filter.literals.isEmpty()) {
                        throw new IllegalArgumentException("Should be empty");
                    }
                    pred = pred.and(isNotNull(member));
                } else {
                    throw new UnsupportedOperationException("Not implemented: " + filter.pattern);
                }
            }
          }
          if (predicate.isDefined()) {
              predicate = Some(predicate.get().or(pred));
          } else {
              predicate = Some(pred);
          }
        }
        return predicate;
    }
    
    public <T> Iterable<T> filterData(Iterable<MetaNamedMember<T, ?>> includes, Iterable<? extends MetaNamedMember<T, ?>> geometryMembers, Filters filters, Iterable<T> ts) {
        if (filters == null) {
            return ts;
        }
        
        return filter(buildPredicate(includes, geometryMembers, filters).getOrElse(Predicate.<T>of(Function.<T,Boolean>constant(true))), ts);
    }

    private <T> Class<?> resolveTargetType(Filter filter, MetaNamedMember<? super T, ?> member) {
        if (filter.property.isFunctionCall()) {
            return fp.changesResultType(filter.property.getValue()).getOrElse(MemberUtil.actualTypeUnwrappingOptionAndEitherAndIterables(member));
        } else {
            return MemberUtil.actualTypeUnwrappingOptionAndEitherAndIterables(member);
        }
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
        final Function1<T, Boolean> unwrappedPred = Filtering_.<T>unwrapOption().andThen(not(Predicates.<T>isNull()).and(pred));
        return new Predicate<T>() {
            @SuppressWarnings("unchecked")
            @Override
            public boolean accept(T candidate) {
                if (candidate instanceof Iterable) {
                    switch (matchAction) {
                        case All: return forall(unwrappedPred, (Iterable<T>)candidate);
                        case Any: return exists(unwrappedPred, (Iterable<T>)candidate);
                        case One: return 1 == size(filter(unwrappedPred, (Iterable<T>)candidate));
                    }
                    throw new IllegalStateException("Shouldn't be here!");
                } else {
                    return pred.apply(candidate);
                }
            }
        };
    }
    
    @SuppressWarnings("unchecked")
    public <T,V> Predicate<T> equal(MetaNamedMember<T,V> member, Literal value, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, equalTo((V)convert(targetType, value))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V> Predicate<T> notEqual(MetaNamedMember<T,V> member, Literal value, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, not(equalTo((V)convert(targetType, value)))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V extends Comparable<V>> Predicate<T> lt(MetaNamedMember<T,V> member, Literal value, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, lessThan((V)convert(targetType, value))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V extends Comparable<V>> Predicate<T> gt(MetaNamedMember<T,V> member, Literal value, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, greaterThan((V)convert(targetType, value))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V extends Comparable<V>> Predicate<T> gte(MetaNamedMember<T,V> member, Literal value, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, greaterThanOrEqualTo((V)convert(targetType, value))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V extends Comparable<V>> Predicate<T> lte(MetaNamedMember<T,V> member, Literal value, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, lessThanOrEqualTo((V)convert(targetType, value))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V extends Comparable<V>> Predicate<T> between(MetaNamedMember<T,V> member, Literal value1, Literal value2, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, Predicates.between((V)convert(targetType, value1), (V)convert(targetType, value2))))));
    }
    
    @SuppressWarnings("unchecked")
    public <T,V extends Comparable<V>> Predicate<T> notBetween(MetaNamedMember<T,V> member, Literal value1, Literal value2, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, not(Predicates.between((V)convert(targetType, value1), (V)convert(targetType, value2)))))));
    }
    
    public <T> Predicate<T> like(MetaNamedMember<T,String> member, String likePattern) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, Filtering_.matches.apply(Function.__, likePattern.replace("%", ".*"))))));
    }
    
    public <T> Predicate<T> notLike(MetaNamedMember<T,String> member, String likePattern) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, not(Filtering_.matches.apply(Function.__, likePattern.replace("%", ".*")))))));
    }
    
    public <T> Predicate<T> ilike(MetaNamedMember<T,String> member, String likePattern) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, Filtering_.matches.apply(Function.__, "(?i)" + likePattern.replace("%", ".*"))))));
    }
    
    public <T> Predicate<T> notILike(MetaNamedMember<T,String> member, String likePattern) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<String>unwrapOption()).andThen(not(Predicates.<String>isNull()).and(doFilter(MatchAction.Any, not(Filtering_.matches.apply(Function.__, "(?i)" + likePattern.replace("%", ".*")))))));
    }
    
    public <T,V> Predicate<T> in(MetaNamedMember<T,V> member, List<Literal> values, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, Filtering_.contains.ap(newSet(map(Filtering_.<V>convert().ap(this, targetType), values)))))));
    }
    
    public <T,V> Predicate<T> notIn(MetaNamedMember<T,V> member, List<Literal> values, Class<?> targetType) {
        return Predicate.of(Function.of(member).andThen(Filtering_.<V>unwrapOption()).andThen(not(Predicates.<V>isNull()).and(doFilter(MatchAction.Any, not(Filtering_.contains.ap(newSet(map(Filtering_.<V>convert().ap(this, targetType), values))))))));
    }
    
    public <T,V> Predicate<T> isNull(MetaNamedMember<T,?> member) {
        return Predicate.of(Function.of(member).andThen(Filtering_.isNullOrEmpty));
    }
    
    public <T> Predicate<T> isNotNull(MetaNamedMember<T,?> member) {
        return Predicate.of(Function.of(member).andThen(not(Filtering_.isNullOrEmpty)));
    }
    
    @SuppressWarnings("unchecked")
    static boolean isNullOrEmpty(Object x) {
        if (x == null) {
            return true;
        }
        if (x instanceof Option<?> && !((Option<?>)x).isDefined()) {
            return true;
        }
        if (x instanceof Iterable<?>) {
            Option<?> first = headOption((Iterable<?>)x);
            if (!first.isDefined()) {
                return true;
            } else {
                if (first.get() instanceof Iterable) {
                    return isNullOrEmpty(flatten((Iterable<Iterable<?>>)x));
                }
            }
        }
        return false;
    }
    
    static boolean matches(String str, String regex) {
        return str.matches(regex);
    }
    
    static boolean contains(Set<?> set, Object value) {
        return set.contains(value);
    }
}
