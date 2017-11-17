package fi.solita.utils.api.base;

import static fi.solita.utils.functional.Collections.emptySet;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.map;

import java.util.Collection;
import java.util.List;
import java.util.SortedMap;

import org.eclipse.persistence.jaxb.metadata.MetadataSource;

import fi.solita.utils.api.Filtering;
import fi.solita.utils.api.Includes;
import fi.solita.utils.api.MemberUtil;
import fi.solita.utils.api.types.Filters;
import fi.solita.utils.functional.FunctionalM;
import fi.solita.utils.meta.MetaNamedMember;


/**
 * Base class for API version.
 * Extend a new class for each new API version, overriding fields as necessary.
 */
public abstract class VersionBase {
    public static class XmlNameGenerator extends fi.solita.utils.api.base.XmlNameGenerator {
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
    
    
    
    public final Filtering filtering = new Filtering(httpModule);
    
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filterRows(Includes<T> includes, Filters filters, SortedMap<K,V> ts) {
        return filtering.filterData(includes.includes, includes.geometryMembers, filters, ts);
    }
    public <T> Iterable<T> filterRows(Includes<T> includes, Filters filters, Iterable<T> ts) {
        return filtering.filterData(includes.includes, includes.geometryMembers, filters, ts);
    }
    
    public <K,T> SortedMap<K,Iterable<T>> filterColumns(Includes<T> includes, SortedMap<K,? extends Iterable<T>> ts) {
        return FunctionalM.map(MemberUtil.<T>withPropertiesF(includes), ts);
    }
    public final <T> Iterable<T> filterColumns(Includes<T> includes, Iterable<T> ts) {
        return map(MemberUtil.<T>withPropertiesF(includes), ts);
    }
    
    public final <K,T> SortedMap<K,Iterable<T>>  filter(Includes<T> includes, Filters filters, SortedMap<K,? extends Iterable<T>> ts) {
        return filterColumns(includes, filterRows(includes, filters, ts));
    }
    public final <T> List<T> filter(Includes<T> includes, Filters filters, List<T> ts) {
        return newList(filterColumns(includes, filterRows(includes, filters, ts)));
    }
    public final <T> Iterable<T> filter(Includes<T> includes, Filters filters, Iterable<T> ts) {
        return filterColumns(includes, filterRows(includes, filters, ts));
    }
    public final <T> T filter(Includes<T> includes, T t) {
        return MemberUtil.<T>withPropertiesF(includes).apply(t);
    }
    
    
    
    @Deprecated
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, SortedMap<K,V> ts) {
        return filtering.filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <K,T,V extends Iterable<T>> SortedMap<K,V> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, SortedMap<K,V> ts) {
        return filtering.filterData(includes, newList(geometry, geometry2), filters, ts);
    }
    @Deprecated
    public <T> Iterable<T> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, Filters filters, Iterable<T> ts) {
        return filtering.filterData(includes, newList(geometry), filters, ts);
    }
    @Deprecated
    public <T> Iterable<T> filter(Iterable<MetaNamedMember<T,?>> includes, MetaNamedMember<T, ?> geometry, MetaNamedMember<T, ?> geometry2, Filters filters, Iterable<T> ts) {
        return filtering.filterData(includes, newList(geometry, geometry2), filters, ts);
    }
}
