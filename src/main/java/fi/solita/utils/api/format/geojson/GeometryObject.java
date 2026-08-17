package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;

@JsonSerializeAsBean
public abstract class GeometryObject extends GeoJSONObject {
    public GeometryObject(String type, Optional<Crs> crs) {
        super(type, crs);
    }   
    
    public abstract boolean isEmpty();
}