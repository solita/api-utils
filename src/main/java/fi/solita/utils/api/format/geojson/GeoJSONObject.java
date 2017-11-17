package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public abstract class GeoJSONObject {
    public final String type;
    public final Crs crs;
    
    public GeoJSONObject(String type, Option<Crs> crs) {
        this.type = type;
        this.crs = crs.getOrElse(null); // to make Jackson leave the property out when null
    }
}