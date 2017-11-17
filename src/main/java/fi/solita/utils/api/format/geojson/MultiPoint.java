package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public final class MultiPoint extends GeometryObject {
    public final Iterable<?> coordinates;
    
    public MultiPoint(Iterable<?> koordinaatit) {
        super("MultiPoint", Option.<Crs>None());
        this.coordinates = koordinaatit;
    }
    
    @Override
    public boolean isEmpty() {
        return Functional.isEmpty(coordinates);
    }
}