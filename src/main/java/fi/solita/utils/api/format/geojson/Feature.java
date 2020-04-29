package fi.solita.utils.api.format.geojson;

import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Option.Some;

import com.fasterxml.jackson.annotation.JsonValue;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;

@JsonSerializeAsBean
public class Feature extends FeatureObject {
    public final Option<? extends GeometryObject> geometry;
    public final Object properties;
    
    public static final FeatureWithBBox withBBox(GeometryObject geometry, Object properties, Option<Crs> crs, Object bbox) {
        return new FeatureWithBBox(Some(geometry), properties, crs, bbox);
    }
    
    public Feature(GeometryObject geometry, Object properties, Option<Crs> crs) {
        this(geometry == null ? Option.<GeometryObject>None() : Some(geometry), properties, crs);
    }
    
    public Feature(Option<? extends GeometryObject> geometry, Object properties, Option<Crs> crs) {
        super("Feature", crs);
        this.geometry = geometry;
        this.properties = properties;
    }
    
    public Feature(String key, Object value) {
        this(Option.<GeometryObject>None(), newMap(Pair.of(key, value)), Option.<Crs>None());
    }
    
    @JsonSerializeAsBean
    public static final class FeatureWithBBox extends Feature {
        public final Object bbox;
        private FeatureWithBBox(Option<GeometryObject> geometry, Object properties, Option<Crs> crs, Object bbox) {
            super(geometry, properties, crs);
            this.bbox = bbox;
        }
    }
    
    public static final class RawFeature extends FeatureObject {
        private final Object data;

        public RawFeature(Object data) {
            super(null, Option.<Crs>None());
            this.data = data;
        }
        
        @JsonValue
        public Object value() {
            return data;
        }
    }
}