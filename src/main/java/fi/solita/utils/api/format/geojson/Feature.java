package fi.solita.utils.api.format.geojson;

import static fi.solita.utils.functional.Collections.newMap;


import com.fasterxml.jackson.annotation.JsonValue;

import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;
import fi.solita.utils.functional.Pair;

@JsonSerializeAsBean
public class Feature extends FeatureObject {
    public final Optional<? extends GeometryObject> geometry;
    public final Object properties;
    
    public static final FeatureWithBBox withBBox(GeometryObject geometry, Object properties, Optional<Crs> crs, Object bbox) {
        return new FeatureWithBBox(Optional.of(geometry), properties, crs, bbox);
    }
    
    public Feature(GeometryObject geometry, Object properties, Optional<Crs> crs) {
        this(geometry == null ? null : Optional.of(geometry), properties, crs);
    }
    
    public Feature(Optional<? extends GeometryObject> geometry, Object properties, Optional<Crs> crs) {
        super("Feature", crs);
        this.geometry = geometry;
        this.properties = properties;
    }
    
    public Feature(String key, Object value) {
        this(Optional.empty(), newMap(Pair.of(key, value)), Optional.empty());
    }
    
    @JsonSerializeAsBean
    public static final class FeatureWithBBox extends Feature {
        public final Object bbox;
        private FeatureWithBBox(Optional<GeometryObject> geometry, Object properties, Optional<Crs> crs, Object bbox) {
            super(geometry, properties, crs);
            this.bbox = bbox;
        }
    }
    
    public static final class RawFeature extends FeatureObject {
        private final Object data;

        public RawFeature(Object data) {
            super(null, Optional.empty());
            this.data = data;
        }
        
        @JsonValue
        public Object value() {
            return data;
        }
    }
}