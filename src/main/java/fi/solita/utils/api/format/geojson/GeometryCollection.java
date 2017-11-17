package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public final class GeometryCollection extends GeometryObject {
    public final Iterable<? extends GeometryObject> geometries;
    
    public GeometryCollection(Iterable<? extends GeometryObject> geometries) {
        super("GeometryCollection", Option.<Crs>None());
        this.geometries = geometries;
    }
    
    @Override
    public boolean isEmpty() {
        return Functional.isEmpty(geometries);
    }
}