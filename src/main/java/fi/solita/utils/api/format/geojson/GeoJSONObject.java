package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;

@JsonSerializeAsBean
public abstract class GeoJSONObject {
    public final String type;
    public final Crs crs;
    
    public GeoJSONObject(String type, Optional<Crs> crs) {
        this.type = type;
        this.crs = crs.orElse(null); // to make Jackson leave the property out when null
    }
}