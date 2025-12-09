package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.emptyList;
import static fi.solita.utils.functional.Collections.emptySet;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.distinct;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalM.mapValue;
import static fi.solita.utils.functional.Option.Some;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.persistence.jaxb.metadata.MetadataSource;

import fi.solita.utils.api.Includes;
import fi.solita.utils.api.base.csv.CsvModule;
import fi.solita.utils.api.base.csv.CsvSerializers;
import fi.solita.utils.api.base.excel.ExcelModule;
import fi.solita.utils.api.base.excel.ExcelSerializers;
import fi.solita.utils.api.base.html.HtmlModule;
import fi.solita.utils.api.base.html.HtmlSerializers;
import fi.solita.utils.api.base.http.HttpModule;
import fi.solita.utils.api.base.http.HttpSerializers;
import fi.solita.utils.api.base.json.JsonModule;
import fi.solita.utils.api.base.json.JsonSerializers;
import fi.solita.utils.api.base.xml.XmlMetadataSource;
import fi.solita.utils.api.base.xml.XmlModule;
import fi.solita.utils.api.base.xml.XmlRegistry;
import fi.solita.utils.api.base.xml.XmlSerializers;
import fi.solita.utils.api.filtering.Filter_;
import fi.solita.utils.api.filtering.Filtering;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.ClassUtils;
import fi.solita.utils.api.util.ModificationUtils;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.FunctionalM;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Transformer;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;


/**
 * Base class for API version.
 * Extend a new class for each new API version, overriding fields as necessary.
 */
public abstract class VersionBase<REQ> {
    // package containing the version implementation
    public String getBasePackage() {
        return getClass().getPackage().getName();
    }
    
    public String getVersion() {
        try {
            return (String) getClass().getField("VERSION").get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    public static class XmlNameGenerator extends fi.solita.utils.api.base.xml.XmlNameGenerator {
    }
    
    public Serializers serializers() {
        return new Serializers();
    }
    
    protected HttpSerializers httpSerializers() { return new HttpSerializers(serializers()); }
    protected JsonSerializers jsonSerializers() { return new JsonSerializers(serializers()); }
    protected abstract HtmlSerializers htmlSerializers();
    protected CsvSerializers csvSerializers() { return new CsvSerializers(serializers()); }
    protected ExcelSerializers excelSerializers() { return new ExcelSerializers(serializers()); }
    protected XmlSerializers xmlSerializers() { return new XmlSerializers(serializers()); }
    
    public final HttpModule httpModule = new HttpModule(httpSerializers().converters());
    public final JsonModule jsonModule = new JsonModule(jsonSerializers().serializers(), jsonSerializers().keySerializers(), jsonSerializers().deserializers(), jsonSerializers().rawTypes());
    public final HtmlModule htmlModule = new HtmlModule(htmlSerializers().serializers());
    public final CsvModule csvModule = new CsvModule(csvSerializers().serializers());
    public final ExcelModule excelModule = new ExcelModule(excelSerializers().serializers());
    public final XmlModule xmlModule = new XmlModule() {
        @Override
        public MetadataSource getMetadataSource() {
            return new XmlMetadataSource(XmlNameGenerator.class, xmlSerializers().adapters());
        }
        @Override
        public Collection<Package> getAdditionalPackages() {
            return emptySet();
        }
        @Override
        public Class<?> getXmlRegistry() {
            return XmlRegistry.class;
        }
    };
    
    @SuppressWarnings("unchecked")
    protected ResolvableMemberProvider<REQ> resolvableMemberProvider() {
        return (ResolvableMemberProvider<REQ>) ResolvableMemberProvider.NONE;
    }
    
    /**
     * @param option
     */
    protected FunctionProvider functionProvider(Option<REQ> option) {
        return new FunctionProvider();
    }
    
    public Filtering filtering(REQ req) {
        return new Filtering(httpModule, jsonModule, resolvableMemberProvider(), functionProvider(Some(req)));
    }
    
    public <K,T> Map<K,T> filterRowsSingle(REQ req, Includes<T> includes, Filters filters, Map<K,T> ts) {
        return filtering(req).filterDataSingle(includes.includesFromRowFiltering, includes.geometryMembers, filters, ts);
    }
    public <K,T,V extends Iterable<T>> Map<K,V> filterRows(REQ req, Includes<T> includes, Filters filters, Map<K,V> ts) {
        return filtering(req).filterData(includes.includesFromRowFiltering, includes.geometryMembers, filters, ts);
    }
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filterRows(REQ req, Includes<T> includes, Filters filters, SortedMap<K,V> ts) {
        return filtering(req).filterData(includes.includesFromRowFiltering, includes.geometryMembers, filters, ts);
    }
    public <T> Iterable<T> filterRows(REQ req, Includes<T> includes, Filters filters, Iterable<T> ts) {
        return filtering(req).filterData(includes.includesFromRowFiltering, includes.geometryMembers, filters, ts);
    }
    
    public <K,T> Map<K,T> filterColumnsSingle(final Includes<T> includes, Map<K,T> ts) {
        return includes.includesEverything ? ts : mapValue(new Transformer<T,T>() {
            @Override
            public T transform(T source) {
                return ModificationUtils.withPropertiesF(includes, functionProvider(Option.<REQ>None())).apply(source);
            }
        }, ts);
    }
    @SuppressWarnings("unchecked")
    public <K,T> Map<K,Iterable<T>> filterColumns(final Includes<T> includes, Map<K,? extends Iterable<T>> ts) {
        return includes.includesEverything ? (Map<K, Iterable<T>>)ts : mapValue(new Transformer<Iterable<T>,Iterable<T>>() {
            @Override
            public Iterable<T> transform(Iterable<T> source) {
                return map(ModificationUtils.<T>withPropertiesF(includes, functionProvider(Option.<REQ>None())), source);
            }
        }, (Map<K,Iterable<T>>)ts);
    }
    @SuppressWarnings("unchecked")
    public <K,T> SortedMap<K,Iterable<T>> filterColumns(Includes<T> includes, SortedMap<K,? extends Iterable<T>> ts) {
        return includes.includesEverything ? (SortedMap<K, Iterable<T>>)ts : FunctionalM.map(ModificationUtils.<T>withPropertiesF(includes, functionProvider(Option.<REQ>None())), ts);
    }
    public final <T> Iterable<T> filterColumns(Includes<T> includes, Iterable<T> ts) {
        return includes.includesEverything ? ts : map(ModificationUtils.<T>withPropertiesF(includes, functionProvider(Option.<REQ>None())), ts);
    }
    
    @SuppressWarnings("unchecked")
    @Deprecated
    public <K,T> Map<K,Iterable<T>> filter(REQ req, Includes<T> includes, Filters filters, Map<K,? extends Iterable<T>> ts) {
        return includes.includesEverything ? (Map<K, Iterable<T>>)ts : resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters, ts)));
    }
    @SuppressWarnings("unchecked")
    @Deprecated
    public <K,T> SortedMap<K,Iterable<T>> filter(REQ req, Includes<T> includes, Filters filters, SortedMap<K,? extends Iterable<T>> ts) {
        return includes.includesEverything ? (SortedMap<K, Iterable<T>>)ts : resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters, ts)));
    }
    @Deprecated
    public <T> List<T> filter(REQ req, Includes<T> includes, Filters filters, List<T> ts) {
        return includes.includesEverything ? ts : ClassUtils.toList(resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters, ts))));
    }
    @Deprecated
    public <T> Iterable<T> filter(REQ req, Includes<T> includes, Filters filters, Iterable<T> ts) {
        return includes.includesEverything ? ts : resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters, ts)));
    }
    
    @SuppressWarnings("unchecked")
    public <K,T> Map<K,Iterable<T>> filter(REQ req, Includes<T> includes, Option<Filters> filters, Map<K,? extends Iterable<T>> ts) {
        return includes.includesEverything ? (Map<K, Iterable<T>>)ts : resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters.getOrElse(Filters.EMPTY), ts)));
    }
    @SuppressWarnings("unchecked")
    public <K,T> SortedMap<K,Iterable<T>> filter(REQ req, Includes<T> includes, Option<Filters> filters, SortedMap<K,? extends Iterable<T>> ts) {
        return includes.includesEverything ? (SortedMap<K, Iterable<T>>)ts : resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters.getOrElse(Filters.EMPTY), ts)));
    }
    public <T> List<T> filter(REQ req, Includes<T> includes, Option<Filters> filters, List<T> ts) {
        return includes.includesEverything ? ts : ClassUtils.toList(resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters.getOrElse(Filters.EMPTY), ts))));
    }
    public <T> Iterable<T> filter(REQ req, Includes<T> includes, Option<Filters> filters, Iterable<T> ts) {
        return includes.includesEverything ? ts : resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(req, includes, filters.getOrElse(Filters.EMPTY), ts)));
    }
    
    public final <T> T filter(REQ req, Includes<T> includes, T t) {
        return includes.includesEverything ? t : resolvableMemberProvider().mutateResolvables(req, includes, ModificationUtils.<T>withPropertiesF(includes, functionProvider(Option.<REQ>None())).apply(t));
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters) {
        return resolveIncludes(format, propertyNames, members, builders, filters, emptyList());
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, MetaNamedMember<? super T,?> geometry) {
        return resolveIncludes(format, propertyNames, members, builders, filters, newList(geometry));
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, Iterable<? extends MetaNamedMember<? super T,?>> geometries) {
        Includes<T> includesFromPropertyNames =
                  Includes.resolveIncludes(resolvableMemberProvider(), functionProvider(Option.<REQ>None()), format, propertyNames,                                    members, builders, geometries, false);
        Includes<T> includesFromFilters = filters == null || filters.or.isEmpty()
                ? Includes.<T>none()
                : Includes.resolveIncludes(resolvableMemberProvider(), functionProvider(Option.<REQ>None()), format, Some(distinct(map(Filter_.property, flatten(filters.or)))), members, builders, geometries, true);
        
        return new Includes<T>(includesFromPropertyNames.includes(), includesFromFilters.includes(), distinct(concat(includesFromPropertyNames.geometryMembers, includesFromFilters.geometryMembers)), includesFromPropertyNames.includesEverything || includesFromFilters.includesEverything, members, builders);
    }
    
    @Deprecated
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters) {
        return resolveIncludes(format, Option.of(propertyNames), members, builders, filters, Collections.<MetaNamedMember<? super T,?>>emptyList());
    }
    
    @Deprecated
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, MetaNamedMember<? super T,?> geometry) {
        return resolveIncludes(format, Option.of(propertyNames), members, builders, filters, Collections.<MetaNamedMember<? super T,?>>newList(geometry));
    }

    @Deprecated
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, MetaNamedMember<? super T,?> geometry, MetaNamedMember<? super T,?> geometry2) {
        return resolveIncludes(format, Option.of(propertyNames), members, builders, filters, Collections.<MetaNamedMember<? super T,?>>newList(geometry, geometry2));
    }
    
    @Deprecated
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, MetaNamedMember<? super T,?> geometry, MetaNamedMember<? super T,?> geometry2, @SuppressWarnings("unchecked") MetaNamedMember<? super T,?>... geometry3) {
        return resolveIncludes(format, Option.of(propertyNames), members, builders, filters, Collections.<MetaNamedMember<? super T,?>>newList(cons(geometry, cons(geometry2, geometry3))));
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Option<Filters> filters) {
        return resolveIncludes(format, propertyNames, members, builders, filters.getOrElse(Filters.EMPTY), Collections.<MetaNamedMember<? super T,?>>emptyList());
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Option<Filters> filters, MetaNamedMember<? super T,?> geometry) {
        return resolveIncludes(format, propertyNames, members, builders, filters.getOrElse(Filters.EMPTY), Collections.<MetaNamedMember<? super T,?>>newList(geometry));
    }

    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Option<Filters> filters, MetaNamedMember<? super T,?> geometry, MetaNamedMember<? super T,?> geometry2) {
        return resolveIncludes(format, propertyNames, members, builders, filters.getOrElse(Filters.EMPTY), Collections.<MetaNamedMember<? super T,?>>newList(geometry, geometry2));
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Option<? extends Iterable<PropertyName>> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Option<Filters> filters, MetaNamedMember<? super T,?> geometry, MetaNamedMember<? super T,?> geometry2, @SuppressWarnings("unchecked") MetaNamedMember<? super T,?>... geometry3) {
        return resolveIncludes(format, propertyNames, members, builders, filters.getOrElse(Filters.EMPTY), Collections.<MetaNamedMember<? super T,?>>newList(cons(geometry, cons(geometry2, geometry3))));
    }
    
    
    
    @Deprecated
    public <K,T,V extends Iterable<T>> Map<K,V> filter(REQ req, Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, Map<K,V> ts) {
        return filtering(req).filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filter(REQ req, Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, SortedMap<K,V> ts) {
        return filtering(req).filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> Map<K,V> filter(REQ req, Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, Map<K,V> ts) {
        return filtering(req).filterData(includes, newList(geometry, geometry2), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filter(REQ req, Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, SortedMap<K,V> ts) {
        return filtering(req).filterData(includes, newList(geometry, geometry2), filters, ts);
    }
    @Deprecated
    public <T> Iterable<T> filter(REQ req, Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, Iterable<T> ts) {
        return filtering(req).filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <T> Iterable<T> filter(REQ req, Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, Iterable<T> ts) {
        return filtering(req).filterData(includes, newList(geometry, geometry2), filters, ts);
    }
}
