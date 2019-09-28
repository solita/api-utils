package fi.solita.utils.api;

import static fi.solita.utils.api.MemberUtil.excluding;
import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.concat;
import static fi.solita.utils.functional.Functional.cons;
import static fi.solita.utils.functional.Functional.flatten;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Option.Some;

import java.util.Collection;
import java.util.SortedMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geotools.geometry.jts.ReferencedEnvelope;

import fi.solita.utils.api.RequestUtil.UnavailableContentTypeException;
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
import fi.solita.utils.api.types.SRSName;
import fi.solita.utils.functional.Apply;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Function;
import fi.solita.utils.functional.Function3;
import fi.solita.utils.functional.Option;
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
        GeojsonResolver geojsonResolver) {
        this.json = json;
        this.geoJson = geoJson;
        this.jsonlines = jsonlines;
        this.html = html;
        this.csv = csv;
        this.excel = excel;
        this.png = png;
        this.geojsonResolver = geojsonResolver;
    }
    
    protected String title2fileName(HtmlTitle title) {
        return title.plainTextTitle.toLowerCase().replace(' ', '_').replace('ä',  'a').replace('ö', 'o').replace('å', 'a');
    }
    
    protected String title2layerName(HtmlTitle title) {
        return title.plainTextTitle.toLowerCase().replace(' ', '_').replace('ä',  'a').replace('ö', 'o').replace('å', 'a');
    }
    
    public abstract ReferencedEnvelope bounds2envelope(BOUNDS place);
    
    public <DTO, KEY, SPATIAL> byte[] stdSpatialBoundedMap(
            HttpServletRequest req,
            HttpServletResponse res,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<SortedMap<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            MetaNamedMember<? super DTO, KEY> key,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, ? extends GeometryObject> toGeojson) {
        return stdSpatialBoundedMap(req, res, bbox, srsName, format, includes, data, dataTransformer, title, key, excluding(geometryLens), Function.of(geometryLens).andThen(toGeojson), Feature_.$);
    }
    
    public <DTO, KEY, SPATIAL> byte[] stdSpatialBoundedMap(
            HttpServletRequest req,
            HttpServletResponse res,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<SortedMap<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> excluding,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Function3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        byte[] response;
        switch (format) {
        case JSON:
            response = json.serialize(map(dataTransformer, data.get()));
            break;
        case GEOJSON:
            SortedMap<KEY, Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            excluding,
                            Function.constant(Option.<Crs>None()),
                            flatten(map(dataTransformer, d).values()))), resolvables),
                    Some(Crs.of(srsName))));
            break;
        case JSONL:
            response = jsonlines.serialize(map(dataTransformer, data.get()));
            break;
        case HTML:
            response = html.serialize(req, title, map(dataTransformer, data.get()), includes);
            break;
        case CSV:
            response = csv.serialize(res, title2fileName(title), map(dataTransformer, data.get()), includes);
            break;
        case XLSX:
            response = excel.serialize(res, title2fileName(title), map(dataTransformer, data.get()), includes);
            break;
        case PNG:
            response = png.render(req, bounds2envelope(bbox), title2layerName(title));
            break;
        case GML:
        case XML:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }

    public <DTO, KEY, SPATIAL> byte[] stdSpatialBoundedMap(
            HttpServletRequest req,
            HttpServletResponse res,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<SortedMap<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            MetaNamedMember<? super DTO, KEY> key,
            Apply<? super DTO, ? super DTO> excluding,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Function3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        byte[] response;
        switch (format) {
        case JSON:
            response = json.serialize(map(dataTransformer, data.get()));
            break;
        case GEOJSON:
            SortedMap<KEY, Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            excluding,
                            Function.constant(Option.<Crs>None()),
                            flatten(map(dataTransformer, data.get()).values()))), resolvables),
                    Some(Crs.of(srsName))));
            break;
        case JSONL:
            response = jsonlines.serialize(map(dataTransformer, data.get()));
            break;
        case HTML:
            response = html.serializeWithKey(req, title, map(dataTransformer, data.get()), includes, key);
            break;
        case CSV:
            response = csv.serializeWithKey(res, title2fileName(title), map(dataTransformer, data.get()), includes, key);
            break;
        case XLSX:
            response = excel.serializeWithKey(res, title2fileName(title), map(dataTransformer, data.get()), includes, key);
            break;
        case PNG:
            response = png.render(req, bounds2envelope(bbox), title2layerName(title));
            break;
        case GML:
        case XML:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO, KEY, SPATIAL> byte[] stdSpatialBoundedCollection(
            HttpServletRequest req,
            HttpServletResponse res,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, ? extends GeometryObject> toGeojson) {
        return stdSpatialBoundedCollection(req, res, bbox, srsName, format, includes, data, dataTransformer, title, excluding(geometryLens), Function.of(geometryLens).andThen(toGeojson), Feature_.$);
    }
    
    public <DTO, KEY, SPATIAL> byte[] stdSpatialBoundedCollection(
            HttpServletRequest req,
            HttpServletResponse res,
            BOUNDS bbox,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> excluding,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Function3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        byte[] response;
        switch (format) {
        case JSON:
            response = json.serialize(map(dataTransformer, data.get()));
            break;
        case GEOJSON:
            Iterable<DTO> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
            response = geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            excluding,
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, data.get()))), resolvables),
                    Some(Crs.of(srsName))));
            break;
        case JSONL:
            response = jsonlines.serialize(map(dataTransformer, data.get()));
            break;
        case HTML:
            response = html.serialize(req, title, newList(map(dataTransformer, data.get())), includes);
            break;
        case CSV:
            response = csv.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
            break;
        case XLSX:
            response = excel.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
            break;
        case PNG:
            response = png.render(req, bounds2envelope(bbox), title2layerName(title));
            break;
        case GML:
        case XML:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO,KEY,SPATIAL> byte[] stdSpatialCollection(
            HttpServletRequest req,
            HttpServletResponse res,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, ? extends GeometryObject> toGeojson) {
        return stdSpatialCollection(req, res, srsName, format, includes, data, dataTransformer, title, excluding(geometryLens), Function.of(geometryLens).andThen(toGeojson), Feature_.$);
    }

    public <DTO,KEY,SPATIAL> byte[] stdSpatialCollection(
            HttpServletRequest req,
            HttpServletResponse res,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> excluding,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Function3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(map(dataTransformer, data.get()));
                break;
            case GEOJSON:
                Iterable<DTO> d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
                response = geoJson.serialize(new FeatureCollection(
                    concat(map(toFeature, map(
                            toGeojson,
                            excluding,
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, data.get()))), resolvables),
                    Some(Crs.of(srsName))));
                break;
            case JSONL:
                response = jsonlines.serialize(map(dataTransformer, data.get()));
                break;
            case HTML:
                response = html.serialize(req, title, newList(map(dataTransformer, data.get())), includes);
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case PNG:
            case GML:
            case XML:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO,KEY,SPATIAL> byte[] stdSpatialSingle(
            HttpServletRequest req,
            HttpServletResponse res,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Lens<? super DTO, SPATIAL> geometryLens,
            Apply<? super SPATIAL, ? extends GeometryObject> toGeojson) {
        return stdSpatialSingle(req, res, srsName, format, includes, data, dataTransformer, title, excluding(geometryLens), Function.of(geometryLens).andThen(toGeojson), Feature_.$);
    }
    
    public <DTO,KEY,SPATIAL> byte[] stdSpatialSingle(
            HttpServletRequest req,
            HttpServletResponse res,
            SRSName srsName,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            Apply<? super DTO, ? super DTO> excluding,
            Apply<? super DTO, ? extends SPATIAL> toGeojson,
            Function3<SPATIAL, Object, Option<Crs>, Feature> toFeature) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(dataTransformer.apply(data.get()));
                break;
            case GEOJSON:
                DTO d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(Some(d), includes);
                DTO d2 = dataTransformer.apply(data.get());
                Feature feature = toFeature.apply(
                        toGeojson.apply(d2),
                        excluding.apply(d2),
                    Some(Crs.of(srsName)));
                
                response = geoJson.serialize(resolvables.isEmpty() ? feature : new FeatureCollection(cons(feature, resolvables), Some(Crs.of(srsName))));
                break;
            case JSONL:
                response = jsonlines.serialize(newList(dataTransformer.apply(data.get())));
                break;
            case HTML:
                response = html.serialize(req, title, dataTransformer.apply(data.get()), includes);
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), dataTransformer.apply(data.get()), includes);
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), dataTransformer.apply(data.get()), includes);
                break;
            case PNG:
            case GML:
            case XML:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <KEY,DTO> byte[] stdMap(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<SortedMap<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        byte[] response;
        switch (format) {
        case JSON:
            response = json.serialize(map(dataTransformer, data.get()));
            break;
        case GEOJSON:
            SortedMap<KEY,Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = geoJson.serialize(new FeatureCollection(
                        concat(map(Feature_.$1, map(
                                Function.constant(Option.<GeometryObject>None()), 
                                Function.id(),
                                Function.constant(Option.<Crs>None()),
                                flatten(map(dataTransformer, data.get()).values()))), resolvables),
                        Option.<Crs>None()));
            break;
        case JSONL:
            response = jsonlines.serialize(map(dataTransformer, data.get()));
            break;
        case HTML:
            response = html.serialize(req, title, map(dataTransformer, data.get()), includes);
            break;
        case CSV:
            response = csv.serialize(res, title2fileName(title), map(dataTransformer, data.get()), includes);
            break;
        case XLSX:
            response = excel.serialize(res, title2fileName(title), map(dataTransformer, data.get()), includes);
            break;
        case PNG:
        case GML:
        case XML:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO, KEY> byte[] stdMap(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<SortedMap<KEY, Iterable<DTO>>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title,
            MetaNamedMember<? super DTO, KEY> key) {
        byte[] response;
        switch (format) {
        case JSON:
            response = json.serialize(map(dataTransformer, data.get()));
            break;
        case GEOJSON:
            SortedMap<KEY,Iterable<DTO>> d = data.get();
            Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(flatten(d.values()), includes);
            response = geoJson.serialize(new FeatureCollection(
                        concat(map(Feature_.$1, map(
                                Function.constant(Option.<GeometryObject>None()), 
                                Function.id(),
                                Function.constant(Option.<Crs>None()),
                                flatten(map(dataTransformer, data.get()).values()))), resolvables),
                        Option.<Crs>None()));
            break;
        case JSONL:
            response = jsonlines.serialize(map(dataTransformer, data.get()));
            break;
        case HTML:
            response = html.serializeWithKey(req, title, map(dataTransformer, data.get()), includes, key);
            break;
        case CSV:
            response = csv.serializeWithKey(res, title2fileName(title), map(dataTransformer, data.get()), includes, key);
            break;
        case XLSX:
            response = excel.serializeWithKey(res, title2fileName(title), map(dataTransformer, data.get()), includes, key);
            break;
        case PNG:
        case GML:
        case XML:
            throw new UnavailableContentTypeException();
        default:
            throw new IllegalStateException();
        }
        return response;
    }

    public <DTO> byte[] stdCollection(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(map(dataTransformer, data.get()));
                break;
            case GEOJSON:
                Iterable<DTO> d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(d, includes);
                response = geoJson.serialize(new FeatureCollection(
                    concat(map(Feature_.$1, map(
                            Function.constant(Option.<GeometryObject>None()),
                            Function.id(),
                            Function.constant(Option.<Crs>None()),
                            map(dataTransformer, data.get()))), resolvables),
                    Option.<Crs>None()));
                break;
            case JSONL:
                response = jsonlines.serialize(map(dataTransformer, data.get()));
                break;
            case HTML:
                response = html.serialize(req, title, newList(map(dataTransformer, data.get())), includes);
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case PNG:
            case GML:
            case XML:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO> byte[] stdSingle(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            Includes<DTO> includes,
            ApplyZero<DTO> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(dataTransformer.apply(data.get()));
                break;
            case GEOJSON:
                DTO d = data.get();
                Collection<FeatureObject> resolvables = geojsonResolver.getResolvedFeatures(Some(d), includes);
                Feature feature = new Feature(
                        Option.<GeometryObject>None(),
                        dataTransformer.apply(data.get()),
                        Option.<Crs>None());
                response = geoJson.serialize(resolvables.isEmpty() ? feature : new FeatureCollection(cons(feature, resolvables), Option.<Crs>None()));
                break;
            case JSONL:
                response = jsonlines.serialize(newList(dataTransformer.apply(data.get())));
                break;
            case HTML:
                response = html.serialize(req, title, dataTransformer.apply(data.get()), includes);
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), dataTransformer.apply(data.get()), includes);
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), dataTransformer.apply(data.get()), includes);
                break;
            case PNG:
            case GML:
            case XML:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <E extends Enum<E>> byte[] stdTypes(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            Iterable<E> data,
            HtmlTitle title) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(data);
                break;
            case GEOJSON:
                response = geoJson.serialize(new FeatureCollection(
                    map(Feature_.$2, map(
                            Function.constant("typeName"),
                            Function.id(),
                            data)),
                    Option.<Crs>None()));
                break;
            case JSONL:
                response = jsonlines.serialize(data);
                break;
            case HTML:
                response = html.serialize(req, title, data);
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), data);
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), data);
                break;
            case PNG:
            case GML:
            case XML:
                throw new UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO> byte[] stdStatic(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            Iterable<? extends MetaNamedMember<DTO, ?>> includes,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(map(dataTransformer, data.get()));
                break;
            case JSONL:
                response = jsonlines.serialize(map(dataTransformer, data.get()));
                break;
            case HTML:
                response = html.serialize(req, title, newList(map(dataTransformer, data.get())), includes);
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), newList(map(dataTransformer, data.get())), includes);
                break;
            case PNG:
            case XML:
            case GEOJSON: 
            case GML:
                throw new RequestUtil.UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
    
    public <DTO> byte[] stdStatic(
            HttpServletRequest req,
            HttpServletResponse res,
            SerializationFormat format,
            ApplyZero<? extends Iterable<DTO>> data,
            Apply<DTO,DTO> dataTransformer,
            HtmlTitle title) {
        byte[] response;
        switch (format) {
            case JSON:
                response = json.serialize(map(dataTransformer, data.get()));
                break;
            case JSONL:
                response = jsonlines.serialize(map(dataTransformer, data.get()));
                break;
            case HTML:
                response = html.serialize(req, title, map(dataTransformer, data.get()));
                break;
            case CSV:
                response = csv.serialize(res, title2fileName(title), map(dataTransformer, data.get()));
                break;
            case XLSX:
                response = excel.serialize(res, title2fileName(title), map(dataTransformer, data.get()));
                break;
            case PNG:
            case XML:
            case GEOJSON: 
            case GML:
                throw new RequestUtil.UnavailableContentTypeException();
            default:
                throw new IllegalStateException();
        }
        return response;
    }
}
