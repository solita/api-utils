package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;

@JsonSerializeAsBean
public abstract class FeatureObject extends GeoJSONObject {
    public FeatureObject(String type, Optional<Crs> crs) {
        super(type, crs);
    }    
}
