package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public final class Point extends GeometryObject {
    public final Object coordinates;
    
    public Point(Object koordinaatti) {
        super("Point", Option.<Crs>None());
        this.coordinates = koordinaatti;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
}