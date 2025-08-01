package fi.solita.utils.api;

import static fi.solita.utils.api.util.ModificationUtils.excluding;
import static fi.solita.utils.functional.Collections.emptyMap;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.FunctionalM.mapValue;
import static fi.solita.utils.functional.FunctionalM.mapValues;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.util.Collection;
import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;

import fi.solita.utils.api.format.ChartConversionService;
import fi.solita.utils.api.format.CountConversionService;
import fi.solita.utils.api.format.CsvConversionService;
import fi.solita.utils.api.format.ExcelConversionService;
import fi.solita.utils.api.format.HtmlConversionService;
import fi.solita.utils.api.format.HtmlConversionService.HtmlTitle;
import fi.solita.utils.api.format.JsonConversionService;
import fi.solita.utils.api.format.JsonLinesConversionService;
import fi.solita.utils.api.format.PngConversionService;
import fi.solita.utils.api.format.SerializationFormat;
import fi.solita.utils.api.format.geojson.Crs;
import fi.solita.utils.api.format.geojson.Feature;
import fi.solita.utils.api.format.geojson.FeatureCollection;
import fi.solita.utils.api.format.geojson.FeatureObject;
import fi.solita.utils.api.format.geojson.Feature_;
import fi.solita.utils.api.format.geojson.GeometryObject;
import fi.solita.utils.api.resolving.GeojsonResolver;
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.api.util.ServletRequestUtil.Request;
import fi.solita.utils.api.util.UnavailableContentTypeException;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.Apply3;
import fi.solita.utils.functional.ApplyBi;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function1;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.lens.Lens;
import fi.solita.utils.meta.MetaNamedMember;

/**
 * One "standard" way to serialize stuff. Feel free to use this, or make your own.
 */
public abstract class StdSerialization<BOUNDS> {
    
    public final JsonConversionService json;
    public final JsonConversionService geoJson;
    public final JsonLinesConversionService jsonlines;
    public final HtmlConversionService html;
    public final CsvConversionService csv;
    public final ExcelConversionService excel;
    public final PngConversionService png;
    public final CountConversionService count;
    public final ChartConversionService chart;
    //public final XmlConversionService xml;
    
    private final GeojsonResolver geojsonResolver;
    
    public StdSerialization(
        JsonConversionService json,
        JsonConversionService geoJson,
        JsonLinesConversionService jsonlines,
        HtmlConversionService html,
        CsvConversionService csv,
        ExcelConversionService excel,
        PngConversionService png,
        GeojsonResolver geojsonResolver,
        CountConversionService count,
        ChartConversionService chart) {
        this.json = json;
        this.geoJson = geoJson;
        this.jsonlines = jsonlines;
        this.html = html;
        this.csv = csv;
        this.excel = excel;
        this.png = png;
        this.geojsonResolver = geojsonResolver;
        this.count = count;
        this.chart = chart;
    }
    
    protected String title2fileName(HtmlTitle title) {
        return title.plainTextTitle.toLowerCase().replace(' ', '_').replace('ä',  'a').replace('ö', 'o').replace('å', 'a');
    }
    
    protected String title2layerName(HtmlTitle title) {
        return title.plainTextTitle.toLowerCase().replace(' ', '_').replace('ä',  'a').replace('ö', 'o').replace('å', 'a');
    }
    
    public abstract ReferencedEnvelope bounds2envelope(BOUNDS place);
    
    public static <DTO, SPATIAL> Function1<DTO, Option<GeometryObject>> geojsonFromDto(Apply<DTO, SPATIAL> geometryGetter, final Apply<? super SPATIAL, Option<GeometryObject>> toGeojson) {
        return Function.of(geometryGetter).andThen(new Apply<SPATIAL, Option<GeometryObject>>() {
            @Override
            public Option<GeometryObject> apply(SPATIAL t) {
                return t == null ? None() : toGeojson.apply(t);
            }
        });
    }
    
    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialBoundedMap(
            Request req,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<Map<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            MetaNamedMember<? super DTO, KEY> key,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, Option<GeometryObject>> toGeojson) {
        return stdSpatialBoundedMap(req, bbox, srsName, format, includes, data, dataTransformer, title, key, excluding(geometryLens), geojsonFromDto(geometryLens, toGeojson), Feature_.$1);
    }
    
    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialBoundedMap(
            Request req,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<Map<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
        case JSON:
            response = Pair.of(json.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case GEOJSON:
            Map<KEY, Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            geojsonPropertyTransformer,
                            Function.constant(Option.<Crs>None()),
                            flatten(mapValues(dataTransformer, d).values()))), resolvables),
                    Some(Crs.of(srsName)))), emptyMap());
            break;
        case JSONL:
            response = Pair.of(jsonlines.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case HTML:
            response = Pair.of(html.serialize(req, title, mapValues(dataTransformer, data.get()), includes), emptyMap());
            break;
        case CSV:
            response = csv.serialize(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering);
            break;
        case XLSX:
            response = excel.serialize(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering);
            break;
        case PNG:
            response = Pair.of(png.render(getRequestUri(req), getRequestApiKey(req), bounds2envelope(bbox), title2layerName(title)), emptyMap());
            break;
        case COUNT:
            response = Pair.of(count.serialize(data.get()), emptyMap());
            break;
        case CHART:
            response = Pair.of(chart.serialize(req, title, mapValues(dataTransformer, data.get()), includes), emptyMap());
            break;
        case PDF:
        case GML:
        case XML:
        case MVT:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    protected abstract String getRequestUri(Request req);
    
    protected abstract Option<String> getRequestApiKey(Request req);

    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialBoundedMap(
            Request req,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<Map<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            MetaNamedMember<? super DTO, KEY> key,
            Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
        case JSON:
            response = Pair.of(json.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case GEOJSON:
            Map<KEY, Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            geojsonPropertyTransformer,
                            Function.constant(Option.<Crs>None()),
                            flatten(mapValues(dataTransformer, d).values()))), resolvables),
                    Some(Crs.of(srsName)))), emptyMap());
            break;
        case JSONL:
            response = Pair.of(jsonlines.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case HTML:
            response = Pair.of(html.serializeWithKey(req, title, mapValues(dataTransformer, data.get()), includes, key), emptyMap());
            break;
        case CSV:
            response = csv.serializeWithKey(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering, key);
            break;
        case XLSX:
            response = excel.serializeWithKey(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering, key);
            break;
        case PNG:
            response = Pair.of(png.render(getRequestUri(req), getRequestApiKey(req), bounds2envelope(bbox), title2layerName(title)), emptyMap());
            break;
        case COUNT:
            response = Pair.of(count.serialize(data.get()), emptyMap());
            break;
        case CHART:
            response = Pair.of(chart.serialize(req, title, mapValues(dataTransformer, data.get()), includes), emptyMap());
            break;
        case PDF:
        case GML:
        case XML:
        case MVT:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialBoundedMapSingle(
        Request req,
        BOUNDS bbox,
        SRSName srsName,
        SerializationFormat format,
        Includes<DTO> includes,
        ApplyZero<Map<KEY, DTO>> data,
        Apply<DTO,DTO> dataTransformer,
        HtmlTitle title,
        Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
        Apply<? super DTO, ? extends SPATIAL> toGeojson,
        Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
    Pair<byte[],Map<String,String>> response;
    switch (format) {
    case JSON:
        response = Pair.of(json.serialize(mapValue(dataTransformer, data.get())), emptyMap());
        break;
    case GEOJSON:
        Map<KEY, DTO> d = data.get();
        Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d.values(), includes);
        response = Pair.of(geoJson.serialize(new FeatureCollection(
                concat(map(toFeature, map(
                        toGeojson,
                        geojsonPropertyTransformer,
                        Function.constant(Option.<Crs>None()),
                        mapValue(dataTransformer, d).values())), resolvables),
                Some(Crs.of(srsName)))), emptyMap());
        break;
    case JSONL:
        response = Pair.of(jsonlines.serialize(mapValue(dataTransformer, data.get())), emptyMap());
        break;
    case HTML:
        response = Pair.of(html.serializeSingle(req, title, mapValue(dataTransformer, data.get()), includes), emptyMap());
        break;
    case CSV:
        response = csv.serializeSingle(title2fileName(title), mapValue(dataTransformer, data.get()), includes.includesFromColumnFiltering);
        break;
    case XLSX:
        response = excel.serializeSingle(title2fileName(title), mapValue(dataTransformer, data.get()), includes.includesFromColumnFiltering);
        break;
    case PNG:
        response = Pair.of(png.render(getRequestUri(req), getRequestApiKey(req), bounds2envelope(bbox), title2layerName(title)), emptyMap());
        break;
    case COUNT:
        response = Pair.of(count.serialize(data.get()), emptyMap());
        break;
    case CHART:
        response = Pair.of(chart.serializeSingle(req, title, mapValue(dataTransformer, data.get()), includes), emptyMap());
        break;
    case PDF:
    case GML:
    case XML:
    case MVT:
        throw new UnavailableContentTypeException();
    default:
        throw new IllegalStateException();
    }
    return response;
}
    
    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialMapSingle(
            Request req,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<Map<KEY, DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
        case JSON:
            response = Pair.of(json.serialize(mapValue(dataTransformer, data.get())), emptyMap());
            break;
        case GEOJSON:
            Map<KEY, DTO> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d.values(), includes);
            response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            geojsonPropertyTransformer,
                            Function.constant(Option.<Crs>None()),
                            mapValue(dataTransformer, d).values())), resolvables),
                    Some(Crs.of(srsName)))), emptyMap());
            break;
        case JSONL:
            response = Pair.of(jsonlines.serialize(mapValue(dataTransformer, data.get())), emptyMap());
            break;
        case HTML:
            response = Pair.of(html.serializeSingle(req, title, mapValue(dataTransformer, data.get()), includes), emptyMap());
            break;
        case CSV:
            response = csv.serializeSingle(title2fileName(title), mapValue(dataTransformer, data.get()), includes.includesFromColumnFiltering);
            break;
        case XLSX:
            response = excel.serializeSingle(title2fileName(title), mapValue(dataTransformer, data.get()), includes.includesFromColumnFiltering);
            break;
        case COUNT:
            response = Pair.of(count.serialize(data.get()), emptyMap());
            break;
        case CHART:
            response = Pair.of(chart.serializeSingle(req, title, mapValue(dataTransformer, data.get()), includes), emptyMap());
            break;
        case PDF:
        case PNG:
        case GML:
        case XML:
        case MVT:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
    return response;
}
    
    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialBoundedCollection(
            Request req,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, Option<GeometryObject>> toGeojson) {
        return stdSpatialBoundedCollection(req, bbox, srsName, format, includes, data, dataTransformer, title, excluding(geometryLens), geojsonFromDto(geometryLens, toGeojson), Feature_.$1);
    }
    
    public <DTO, KEY, SPATIAL> Pair<byte[],Map<String,String>> stdSpatialBoundedCollection(
            Request req,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
        case JSON:
            response = Pair.of(json.serialize(map(dataTransformer, data.get())), emptyMap());
            break;
        case GEOJSON:
            Iterable<DTO> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
            response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            geojsonPropertyTransformer,
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, d))), resolvables),
                    Some(Crs.of(srsName)))), emptyMap());
            break;
        case JSONL:
            response = Pair.of(jsonlines.serialize(map(dataTransformer, data.get())), emptyMap());
            break;
        case HTML:
            response = Pair.of(html.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
            break;
        case CSV:
            response = csv.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes.includesFromColumnFiltering);
            break;
        case XLSX:
            response = excel.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes.includesFromColumnFiltering);
            break;
        case PNG:
            response = Pair.of(png.render(getRequestUri(req), getRequestApiKey(req), bounds2envelope(bbox), title2layerName(title)), emptyMap());
            break;
        case COUNT:
            response = Pair.of(count.serialize(data.get()), emptyMap());
            break;
        case CHART:
            response = Pair.of(chart.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
            break;
        case PDF:
        case GML:
        case XML:
        case MVT:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO,KEY,SPATIAL> Pair<byte[],Map<String,String>> stdSpatialCollection(
            Request req,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, Option<GeometryObject>> toGeojson) {
        return stdSpatialCollection(req, srsName, format, includes, data, dataTransformer, title, excluding(geometryLens), geojsonFromDto(geometryLens, toGeojson), Feature_.$1);
    }

    public <DTO,KEY,SPATIAL> Pair<byte[],Map<String,String>> stdSpatialCollection(
            Request req,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case GEOJSON:
                Iterable<DTO> d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
                response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            geojsonPropertyTransformer,
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, d))), resolvables),
                    Some(Crs.of(srsName)))), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes.includesFromColumnFiltering);
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes.includesFromColumnFiltering);
                break;
            case COUNT:
                response = Pair.of(count.serialize(data.get()), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
                break;
            case PDF:
            case PNG:
            case GML:
            case XML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO,KEY,SPATIAL> Pair<byte[],Map<String,String>> stdSpatialSingle(
            Request req,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, Option<GeometryObject>> toGeojson) {
        return stdSpatialSingle(req, srsName, format, includes, data, dataTransformer, title, excluding(geometryLens), geojsonFromDto(geometryLens, toGeojson), Feature_.$1);
    }
    
    public <DTO,KEY,SPATIAL> Pair<byte[],Map<String,String>> stdSpatialSingle(
            Request req,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> geojsonPropertyTransformer,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Apply3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        return stdSpatialSingle(req, srsName, format, includes, data, dataTransformer, title, new Apply<DTO, FeatureObject>() {
            @Override
            public FeatureObject apply(DTO d) {
                return toFeature.apply(
                        toGeojson.apply(d),
                        geojsonPropertyTransformer.apply(d),
                    Some(Crs.of(srsName)));
            }
        });
    }
    
    public <DTO,KEY,SPATIAL> Pair<byte[],Map<String,String>> stdSpatialSingle(
            Request req,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<DTO, FeatureObject> toFeatures) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(dataTransformer.apply(data.get())), emptyMap());
                break;
            case GEOJSON:
                DTO d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(Some(d), includes);
                DTO d2 = dataTransformer.apply(d);
                FeatureObject feature = toFeatures.apply(d2);
                
                if (!resolvables.isEmpty()) {
                    if (feature instanceof FeatureCollection) {
                        feature = new FeatureCollection(concat(((FeatureCollection)feature).features, resolvables), Some(Crs.of(srsName)));
                    } else {
                        feature = new FeatureCollection(cons(feature, resolvables), Some(Crs.of(srsName)));
                    }
                }
                response = Pair.of(geoJson.serialize(feature), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(newList(dataTransformer.apply(data.get()))), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, dataTransformer.apply(data.get()), includes), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), dataTransformer.apply(data.get()), includes.includesFromColumnFiltering);
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), dataTransformer.apply(data.get()), includes.includesFromColumnFiltering);
                break;
            case COUNT:
                response = Pair.of(count.serialize(data.get()), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, dataTransformer.apply(data.get()), includes), emptyMap());
                break;
            case PDF:
            case PNG:
            case GML:
            case XML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <KEY,DTO> Pair<byte[],Map<String,String>> stdMap(
            Request req,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<Map<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
        case JSON:
            response = Pair.of(json.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case GEOJSON:
            Map<KEY,Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = Pair.of(geoJson.serialize(new FeatureCollection(
                        concat(Functional.<Option<? extends GeometryObject>, Object, Option<Crs>,Feature>map(Feature_.$1, map(
                                Function.constant(Option.<GeometryObject>None()), 
                                Function.id(),
                                Function.constant(Option.<Crs>None()),
                                flatten(mapValues(dataTransformer, d).values()))), resolvables),
                        Option.<Crs>None())), emptyMap());
            break;
        case JSONL:
            response = Pair.of(jsonlines.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case HTML:
            response = Pair.of(html.serialize(req, title, mapValues(dataTransformer, data.get()), includes), emptyMap());
            break;
        case CSV:
            response = csv.serialize(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering);
            break;
        case XLSX:
            response = excel.serialize(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering);
            break;
        case COUNT:
            response = Pair.of(count.serialize(data.get()), emptyMap());
            break;
        case CHART:
            response = Pair.of(chart.serialize(req, title, mapValues(dataTransformer, data.get()), includes), emptyMap());
            break;
        case PDF:
        case PNG:
        case GML:
        case XML:
        case MVT:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO, KEY> Pair<byte[],Map<String,String>> stdMap(
            Request req,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<Map<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            MetaNamedMember<? super DTO, KEY> key) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
        case JSON:
            response = Pair.of(json.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case GEOJSON:
            Map<KEY,Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = Pair.of(geoJson.serialize(new FeatureCollection(
                        concat(Functional.<Option<? extends GeometryObject>, Object, Option<Crs>,Feature>map(Feature_.$1, map(
                                Function.constant(Option.<GeometryObject>None()), 
                                Function.id(),
                                Function.constant(Option.<Crs>None()),
                                flatten(mapValues(dataTransformer, d).values()))), resolvables),
                        Option.<Crs>None())), emptyMap());
            break;
        case JSONL:
            response = Pair.of(jsonlines.serialize(mapValues(dataTransformer, data.get())), emptyMap());
            break;
        case HTML:
            response = Pair.of(html.serializeWithKey(req, title, mapValues(dataTransformer, data.get()), includes, key), emptyMap());
            break;
        case CSV:
            response = csv.serializeWithKey(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering, key);
            break;
        case XLSX:
            response = excel.serializeWithKey(title2fileName(title), mapValues(dataTransformer, data.get()), includes.includesFromColumnFiltering, key);
            break;
        case COUNT:
            response = Pair.of(count.serialize(data.get()), emptyMap());
            break;
        case CHART:
            response = Pair.of(chart.serialize(req, title, mapValues(dataTransformer, data.get()), includes), emptyMap());
            break;
        case PDF:
        case PNG:
        case GML:
        case XML:
        case MVT:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }

    public <DTO> Pair<byte[],Map<String,String>> stdCollection(
            Request req,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case GEOJSON:
                Iterable<DTO> d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
                response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(Functional.<Option<? extends GeometryObject>, Object, Option<Crs>,Feature>map(Feature_.$1, map(
                            Function.constant(Option.<GeometryObject>None()),
                            Function.id(),
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, d))), resolvables),
                    Option.<Crs>None())), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes.includesFromColumnFiltering);
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes.includesFromColumnFiltering);
                break;
            case COUNT:
                response = Pair.of(count.serialize(data.get()), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
                break;
            case PDF:
            case PNG:
            case GML:
            case XML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO> Pair<byte[],Map<String,String>> stdSingle(
            Request req,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(dataTransformer.apply(data.get())), emptyMap());
                break;
            case GEOJSON:
                DTO d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(Some(d), includes);
                Feature feature = new Feature(
                        Option.<GeometryObject>None(),
                        dataTransformer.apply(d),
                        Option.<Crs>None());
                response = Pair.of(geoJson.serialize(resolvables.isEmpty() ? feature : new FeatureCollection(cons(feature, resolvables), Option.<Crs>None())), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(newList(dataTransformer.apply(data.get()))), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, dataTransformer.apply(data.get()), includes), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), dataTransformer.apply(data.get()), includes.includesFromColumnFiltering);
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), dataTransformer.apply(data.get()), includes.includesFromColumnFiltering);
                break;
            case COUNT:
                response = Pair.of(count.serialize(data.get()), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, dataTransformer.apply(data.get()), includes), emptyMap());
                break;
            case PDF:
            case PNG:
            case GML:
            case XML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <E extends Enum<E>> Pair<byte[],Map<String,String>> stdTypes(
            Request req,
            SerializationFormat format,
            Iterable<E> data,
            HtmlTitle title) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(data), emptyMap());
                break;
            case GEOJSON:
                response = Pair.of(geoJson.serialize(new FeatureCollection(
                    map((ApplyBi<String,Object,Feature>)Feature_.$2, map(
                            Function.constant("typeName"),
                            Function.id(),
                            data)),
                    Option.<Crs>None())), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(data), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, data), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), data);
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), data);
                break;
            case COUNT:
                response = Pair.of(count.serialize(data), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, data), emptyMap());
                break;
            case PDF:
            case PNG:
            case GML:
            case XML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO> Pair<byte[],Map<String,String>> stdStatic(
            Request req,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case GEOJSON:
                Iterable<DTO> d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
                response = Pair.of(geoJson.serialize(new FeatureCollection(
                    concat(Functional.<Option<? extends GeometryObject>, Object, Option<Crs>,Feature>map(Feature_.$1, map(
                            Function.constant(Option.<GeometryObject>None()),
                            Function.id(),
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, d))), resolvables),
                    Option.<Crs>None())), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case COUNT:
                response = Pair.of(count.serialize(data.get()), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, newList(map(dataTransformer, data.get())), includes), emptyMap());
                break;
            case PDF:
            case PNG:
            case XML:
            case GML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO> Pair<byte[],Map<String,String>> stdStatic(
            Request req,
            SerializationFormat format,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        Pair<byte[],Map<String,String>> response;
        switch (format) {
            case JSON:
                response = Pair.of(json.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case GEOJSON:
                Iterable<DTO> d = map(dataTransformer, data.get());
                response = Pair.of(geoJson.serialize(new FeatureCollection(
                    Functional.<Option<? extends GeometryObject>, Object, Option<Crs>,Feature>map(Feature_.$1, map(
                            Function.constant(Option.<GeometryObject>None()),
                            Function.id(),
                            Function.constant(Option.<Crs>None()),
                            d)),
                    Option.<Crs>None())), emptyMap());
                break;
            case JSONL:
                response = Pair.of(jsonlines.serialize(map(dataTransformer, data.get())), emptyMap());
                break;
            case HTML:
                response = Pair.of(html.serialize(req, title, map(dataTransformer, data.get())), emptyMap());
                break;
            case CSV:
                response = csv.serialize(title2fileName(title), map(dataTransformer, data.get()));
                break;
            case XLSX:
                response = excel.serialize(title2fileName(title), map(dataTransformer, data.get()));
                break;
            case COUNT:
                response = Pair.of(count.serialize(data.get()), emptyMap());
                break;
            case CHART:
                response = Pair.of(chart.serialize(req, title, map(dataTransformer, data.get())), emptyMap());
                break;
            case PDF:
            case PNG:
            case XML:
            case GML:
            case MVT:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    /**
     * @param req  
     */
    public Pair<byte[],Map<String,String>> stdPassThrough(
            Request req,
            ApplyZero<byte[]> data) {
        return Pair.of(data.get(), emptyMap());
    }
}
