package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.emptySet;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.distinct;
import static fi.solita.utils.functional.Functional.map;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import javax.servlet.http.HttpServletRequest;

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
import fi.solita.utils.api.base.xml.XmlSerializers;
import fi.solita.utils.api.filtering.Filtering;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.functions.FunctionProvider;
import fi.solita.utils.api.resolving.ResolvableMemberProvider;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.filtering.Filter_;
import fi.solita.utils.api.util.ModificationUtils;
import fi.solita.utils.functional.Collections;
import fi.solita.utils.functional.FunctionalM;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.Transformer;
import fi.solita.utils.functional.lens.Builder;
import fi.solita.utils.meta.MetaNamedMember;


/**
 * Base class for API version.
 * Extend a new class for each new API version, overriding fields as necessary.
 */
public abstract class VersionBase {
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
    
    @javax.xml.bind.annotation.XmlRegistry
    public static class XmlRegistry {
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
    
    public ResolvableMemberProvider resolvableMemberProvider() {
        return ResolvableMemberProvider.NONE;
    }
    
    public Filtering filtering() {
        return new Filtering(httpModule, resolvableMemberProvider(), functionProvider());
    }
    
    public <K,T,V extends Iterable<T>> Map<K,V> filterRows(Includes<T> includes, Filters filters, Map<K,V> ts) {
        return filtering().filterData(includes.includes, includes.geometryMembers, filters, ts);
    }
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filterRows(Includes<T> includes, Filters filters, SortedMap<K,V> ts) {
        return filtering().filterData(includes.includes, includes.geometryMembers, filters, ts);
    }
    public <T> Iterable<T> filterRows(Includes<T> includes, Filters filters, Iterable<T> ts) {
        return filtering().filterData(includes.includes, includes.geometryMembers, filters, ts);
    }
    
    public <K,T> Map<K,Iterable<T>> filterColumns(final Includes<T> includes, Map<K,? extends Iterable<T>> ts) {
        return FunctionalM.map(new Transformer<Map.Entry<K,? extends Iterable<T>>,Map.Entry<K,Iterable<T>>>() {
            @Override
            public Map.Entry<K, Iterable<T>> transform(Map.Entry<K, ? extends Iterable<T>> source) {
                return Pair.of(source.getKey(), map(ModificationUtils.<T>withPropertiesF(includes, functionProvider()), source.getValue()));
            }
        }, ts);
    }
    public <K,T> SortedMap<K,Iterable<T>> filterColumns(Includes<T> includes, SortedMap<K,? extends Iterable<T>> ts) {
        return FunctionalM.map(ModificationUtils.<T>withPropertiesF(includes, functionProvider()), ts);
    }
    public final <T> Iterable<T> filterColumns(Includes<T> includes, Iterable<T> ts) {
        return map(ModificationUtils.<T>withPropertiesF(includes, functionProvider()), ts);
    }
    
    public <K,T> Map<K,Iterable<T>> filter(HttpServletRequest req, Includes<T> includes, Filters filters, Map<K,? extends Iterable<T>> ts) {
        return resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(includes, filters, ts)));
    }
    public <K,T> SortedMap<K,Iterable<T>> filter(HttpServletRequest req, Includes<T> includes, Filters filters, SortedMap<K,? extends Iterable<T>> ts) {
        return resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(includes, filters, ts)));
    }
    public <T> List<T> filter(HttpServletRequest req, Includes<T> includes, Filters filters, List<T> ts) {
        return newList(resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(includes, filters, ts))));
    }
    public <T> Iterable<T> filter(HttpServletRequest req, Includes<T> includes, Filters filters, Iterable<T> ts) {
        return resolvableMemberProvider().mutateResolvables(req, includes, filterColumns(includes, filterRows(includes, filters, ts)));
    }
    
    public final <T> T filter(HttpServletRequest req, Includes<T> includes, T t) {
        return resolvableMemberProvider().mutateResolvables(req, includes, ModificationUtils.<T>withPropertiesF(includes, functionProvider()).apply(t));
    }
    
    public FunctionProvider functionProvider() {
        return new FunctionProvider();
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, Iterable<? extends MetaNamedMember<? super T,?>> geometries) {
        Includes<T> includesFromPropertyNames = Includes.resolveIncludes(resolvableMemberProvider(), functionProvider(), format, propertyNames, members, builders, geometries);
        Includes<T> includesFromFilters = filters == null ? Includes.<T>none() : Includes.resolveIncludes(resolvableMemberProvider(), functionProvider(), format, distinct(map(Filter_.property, filters.filters)), members, builders, geometries);
        return new Includes<T>(distinct(concat(includesFromPropertyNames.includes, includesFromFilters.includes)), distinct(concat(includesFromPropertyNames.geometryMembers, includesFromFilters.geometryMembers)), includesFromPropertyNames.includesEverything || includesFromFilters.includesEverything, builders);
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters) {
        return resolveIncludes(format, propertyNames, members, builders, filters, Collections.<MetaNamedMember<? super T,?>>emptyList());
    }
    
    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, MetaNamedMember<? super T,?> geometry) {
        return resolveIncludes(format, propertyNames, members, builders, filters, Collections.<MetaNamedMember<? super T,?>>newList(geometry));
    }

    public <T> Includes<T> resolveIncludes(SerializationFormat format, Iterable<PropertyName> propertyNames, Collection<? extends MetaNamedMember<? super T,?>> members, Builder<?>[] builders, Filters filters, MetaNamedMember<? super T,?> geometry, MetaNamedMember<? super T,?> geometry2) {
        return resolveIncludes(format, propertyNames, members, builders, filters, Collections.<MetaNamedMember<? super T,?>>newList(geometry, geometry2));
    }
    
    
    
    @Deprecated
    public <K,T,V extends Iterable<T>> Map<K,V> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, Map<K,V> ts) {
        return filtering().filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, SortedMap<K,V> ts) {
        return filtering().filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> Map<K,V> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, Map<K,V> ts) {
        return filtering().filterData(includes, newList(geometry, geometry2), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, SortedMap<K,V> ts) {
        return filtering().filterData(includes, newList(geometry, geometry2), filters, ts);
    }
    @Deprecated
    public <T> Iterable<T> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, Iterable<T> ts) {
        return filtering().filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <T> Iterable<T> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, Iterable<T> ts) {
        return filtering().filterData(includes, newList(geometry, geometry2), filters, ts);
    }
}
