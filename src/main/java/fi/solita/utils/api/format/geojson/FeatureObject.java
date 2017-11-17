package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public abstract class FeatureObject extends GeoJSONObject {
    public FeatureObject(String type, Option<Crs> crs) {
        super(type, crs);
    }    
}
