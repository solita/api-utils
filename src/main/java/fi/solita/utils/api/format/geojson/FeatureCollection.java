package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public class FeatureCollection extends FeatureObject {
    public final Iterable<? extends FeatureObject> features;

    public static final FeatureCollectionWithBBox withBBox(Iterable<? extends FeatureObject> features, Option<Crs> crs, Object paikka) {
        return new FeatureCollectionWithBBox(features, crs, paikka);
    }
    
    public FeatureCollection(Iterable<? extends FeatureObject> features, Option<Crs> crs) {
        super("FeatureCollection", crs);
        this.features = features;
    }
    
    @JsonSerializeAsBean
    public static final class FeatureCollectionWithBBox extends FeatureCollection {
        public final Object bbox;
        
        private FeatureCollectionWithBBox(Iterable<? extends FeatureObject> features, Option<Crs> crs, Object bbox) {
            super(features, crs);
            this.bbox = bbox;
        }
        
    }
}
