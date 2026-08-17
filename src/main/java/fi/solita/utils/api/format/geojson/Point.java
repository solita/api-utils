package fi.solita.utils.api.format.geojson;

import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;

@JsonSerializeAsBean
public final class Point extends GeometryObject {
    public final Object coordinates;
    
    public Point(Object koordinaatti) {
        super("Point", Optional.empty());
        this.coordinates = koordinaatti;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
}