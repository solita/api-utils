package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.newArray;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Collections.newSet;
import static fi.solita.utils.functional.Collections.newSortedSet;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.distinct;
import static fi.solita.utils.functional.Functional.exists;
import static fi.solita.utils.functional.Functional.filter;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.forall;
import static fi.solita.utils.functional.Functional.group;
import static fi.solita.utils.functional.Functional.head;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.remove;
import static fi.solita.utils.functional.Functional.size;
import static fi.solita.utils.functional.Functional.sort;
import static fi.solita.utils.functional.Functional.subtract;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.FunctionalM.groupBy;
import static fi.solita.utils.functional.Predicates.equalTo;
import static fi.solita.utils.functional.Predicates.greaterThan;
import static fi.solita.utils.functional.Predicates.not;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMember;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider_;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.types.PropertyName_;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.MemberUtil;
import fi.solita.utils.api.util.MemberUtil_;
import fi.solita.utils.api.util.RedundantPropertiesException;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Transformers;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;

public class Includes<T> implements Iterable<MetaNamedMember<T,?>> {

    public static class InvalidResolvableExclusionException extends RuntimeException {
        public final MetaNamedMember<?,?> member;
    
        public InvalidResolvableExclusionException(MetaNamedMember<?,?> member) {
            super(member.getName());
            this.member = member;
        }
    }

    public final List<MetaNamedMember<T, ?>> includesFromColumnFiltering;
    public final List<MetaNamedMember<T, ?>> includesFromRowFiltering;
    
    public final List<MetaNamedMember<T, ?>> geometryMembers;
    public final List<MetaNamedMember<T, ?>> allRootMembers;
    public final Builder<?>[] builders;
    public final boolean includesEverything;
    
    public final List<MetaNamedMember<T, ?>> includes() {
        return newList(distinct(concat(includesFromColumnFiltering, includesFromRowFiltering)));
    }

    public static final <T> Includes<T> none() {
        return new Includes<T>(Collections.<MetaNamedMember<? super T, ?>>emptyList(), Collections.<MetaNamedMember<? super T, ?>>emptyList(), Collections.<MetaNamedMember<? super T, ?>>emptyList(), false, emptyList());
    }
    
    public static final <T> Includes<T> all(Collection<? extends MetaNamedMember<? super T,?>> includes, Builder<?>[] builders) {
        return new Includes<T>(Includes.withNestedMembers(includes, Include.All, builders), Collections.<MetaNamedMember<? super T, ?>>emptyList(), Collections.<MetaNamedMember<? super T, ?>>emptyList(), true, includes);
    }
    
    @SuppressWarnings("unchecked")
    public Includes(Iterable<? extends MetaNamedMember<? super T,?>> includesColumn, Iterable<? extends MetaNamedMember<? super T,?>> includesRow, Iterable<? extends MetaNamedMember<? super T, ?>> geometryMembers, boolean includesEverything, Iterable<? extends MetaNamedMember<? super T, ?>> allRootMembers, Builder<?>... builders) {
        this.includesFromColumnFiltering = newList((Iterable<MetaNamedMember<T, ?>>) includesColumn);
        this.includesFromRowFiltering = newList((Iterable<MetaNamedMember<T, ?>>) includesRow);
        this.geometryMembers = newList((Iterable<MetaNamedMember<T, ?>>) geometryMembers);
        this.builders = builders;
        this.includesEverything = includesEverything;
        this.allRootMembers = (List<MetaNamedMember<T, ?>>) newList(allRootMembers);
    }
    
    @Override
    public Iterator<MetaNamedMember<T, ?>> iterator() {
        return includes().iterator();
    }
    
    /**
     * Convert to Includes of a subtype
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <SUB extends T> Includes<SUB> cast(Builder<?>[] subtypeBuilders) {
        return new Includes(includesFromColumnFiltering, includesFromRowFiltering, geometryMembers, false, allRootMembers, subtypeBuilders);
    }
    
    @Deprecated
    public static final <T> Includes<T> resolveIncludes(ResolvableMemberProvider<?> provider, FunctionProvider fp, SerializationFormat format, Iterable<PropertyName> propertyNames, final Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Iterable<? extends MetaNamedMember<? super T,?>> geometries, boolean onlyExact) {
        return resolveIncludes(provider, fp, format, Option.of(propertyNames), members, builders, geometries, onlyExact);
    }

    /**
     * 
     * @param members All members of T, which would be possible to use for filtering the result.
     * @param builders Builders defining what are considered as "nested properties". That is, these define the objects in the hierarchy whose members can be filtered with <i>propertyName</i>
     * @param propertyNames Filtering given by the API user. Empty if not given.
     * @param geometries Geometry members. Subset of <i>members</i>.
     */
    @SuppressWarnings("unchecked")
    public static final <T> Includes<T> resolveIncludes(ResolvableMemberProvider<?> provider, FunctionProvider fp, SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, final Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Iterable<? extends MetaNamedMember<? super T,?>> geometries, boolean onlyExact) {
        List<MetaNamedMember<? super T, ?>> ret = null;
        boolean includesEverything = false;
        
        if (propertyNames.isDefined() && newList(propertyNames.get()).size() > newSet(map(PropertyName_.toProperty.apply(Function.__, fp), propertyNames.get())).size()) {
            throw new RedundantPropertiesException(newSortedSet(flatten(filter(Transformers.size.andThen(greaterThan(1l)), group(sort(map(PropertyName_.toProperty.apply(Function.__, fp), propertyNames.get())))))));
        }
        
        if (propertyNames.isDefined() && (Functional.isEmpty(propertyNames.get()) ||
                                          size(propertyNames.get()) == 1 && head(propertyNames.get()).isEmpty(fp))) {
            // propertyNames is empty or contains only the empty string
            ret = emptyList();
        } else if (!propertyNames.isDefined() || forall(PropertyName_.isExclusion, propertyNames.get())) {
            // if no propertyNames given, or given but contains only exclusions:
            
            // For PNG exclude everything (geometry included back in the end)
            // For HTML and spreadsheet formats, exclude parent of nested members.
            // For others, include everything.
            switch (format) {
                case PNG:
                    ret = emptyList();
                    break;
                case CSV:
                case XLSX:
                case HTML:
                case CHART:
                    ret = Includes.withNestedMembers(members, Includes.Include.OnlyLeaf, builders);
                    break;
                case GEOJSON:
                case GML:
                case JSON:
                case JSONL:
                case XML:
                case COUNT:
                case MVT:
                    ret = Includes.withNestedMembers(members, Includes.Include.All, builders);
                    break;
            }
            if (!propertyNames.isDefined()) {
                includesEverything = true;
            }
        } else {
            ret = newList(flatMap(MemberUtil_.<T>toMembers().ap(provider, fp, onlyExact, Includes.withNestedMembers(members, Includes.Include.All, builders)), filter(not(PropertyName_.isExclusion), ((Option<Iterable<PropertyName>>)propertyNames).getOrElse(Collections.<PropertyName>emptyList()))));
        }
        
        // Include geometries for png/geojson/gml/mvt even if not explicitly requested
        switch (format) {
            case PNG:
            case GEOJSON:
            case GML:
            case MVT:
                for (MetaNamedMember<? super T,?> geometry: geometries) {
                    if (!exists(MemberUtil_.memberName.andThen(equalTo((CharSequence)geometry.getName())), ret)) {
                        ret = newList(cons(geometry, ret));
                    }
                }
                break;
            case JSON:
            case JSONL:
            case HTML:
            case CHART:
            case CSV:
            case XLSX:
            case XML:
            case COUNT:
        }
        
        // Exclusions. Also excludes geometries if explicitly excluded.
        List<MetaNamedMember<? super T, ?>> toRemove = newList(flatMap(MemberUtil_.<T>toMembers().ap(provider, fp, false, Includes.withNestedMembers(members, Includes.Include.All, builders)), map(PropertyName_.omitExclusion, filter(PropertyName_.isExclusion, ((Option<Iterable<PropertyName>>)propertyNames).getOrElse(Collections.<PropertyName>emptyList())))));
        ret = newList(subtract(ret, toRemove));
        if (toRemove != null) {
            for (MetaNamedMember<?,?> m: toRemove) {
                if (ResolvableMemberProvider.isResolvableMember(m)) {
                    throw new Includes.InvalidResolvableExclusionException(m);
                }
            }
        }
        
        final Map<String, List<MetaNamedMember<? super T, ?>>> resolvable = groupBy(MemberUtil_.memberName, filter(ResolvableMemberProvider_.isResolvableMember, ret));
    
        ret = newList(distinct(map(new Apply<MetaNamedMember<? super T,?>, MetaNamedMember<? super T,?>>() {
            @Override
            public MetaNamedMember<? super T, ?> apply(MetaNamedMember<? super T, ?> t) {
                Option<List<MetaNamedMember<? super T, ?>>> x = find(t.getName(), resolvable);
                if (x.isDefined()) {
                    return ResolvableMember.combineAll(x.get());
                } else {
                    return t;
                }
            }
        }, ret)));
        
        return new Includes<T>(ret, ret, geometries, includesEverything, members, builders);
    }
    
    public enum Include {
        All,
        OnlyLeaf,
        NoBuildable
    }

    /**
     * Returns "nested properties" hierarchically. That is, those for which there is a builder in <i>builders</i>.
     * For example, if:
     * <p>Foo { A a, B b, C c } and B { A a } and C { A a }
     * <p>then
     * <code>withNestedMembers([foo.a, foo.b, foo.c], Builder<A>, Builder<B>)</code>
     * <p>would return [a, a.b, a.b.a, b, b.a, c]
     */
    @SuppressWarnings("unchecked")
    public static <T> List<MetaNamedMember<? super T, ?>> withNestedMembers(Collection<? extends MetaNamedMember<? super T, ?>> members, Includes.Include include, Builder<?>... builders) {
        List<MetaNamedMember<? super T, ?>> ret = newMutableList();
        for (MetaNamedMember<? super T, ?> member: members) {
            Type actualType = ClassUtils.getGenericType(member.getMember());
            ret.add(member);
            for (Builder<?> builder: MemberUtil.findBuilderFor(newList(builders), MemberUtil.actualTypeUnwrappingOptionAndEitherAndIterables(member))) {
                if (include == Includes.Include.NoBuildable) {
                    ret.remove(member);
                }
                for (MetaNamedMember<?, ?> nestedMember: withNestedMembers((Collection<? extends MetaNamedMember<T, ?>>) builder.getMembers(), include, newArray(Builder.class, remove(builder, builders)))) {
                    Class<?> actualNestedType = MemberUtil.memberClass(nestedMember);
                    boolean flatten = Iterable.class.isAssignableFrom(ClassUtils.typeClass(actualType)) && Iterable.class.isAssignableFrom(actualNestedType);
                    NestedMember<? super T,?> mem = NestedMember.unchecked(member, nestedMember, flatten);
                    if (Iterable.class.isAssignableFrom(ClassUtils.typeClass(actualType)) && Iterable.class.isAssignableFrom(ClassUtils.typeClass(ClassUtils.getFirstTypeArgument(actualType).getOrElse(void.class)))) {
                        // parent returns iterable of iterable -> flatten
                        mem = mem.modifyParent((Function1<Object,Object>)(Object)Transformers.flatten());
                    }
                    ret.add(mem);
                    if (include == Includes.Include.OnlyLeaf) {
                        ret.remove(member);
                    }
                }
            }
        }
        return ret;
    }
}
