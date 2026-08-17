package fi.solita.utils.api.format.geojson;
import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;

@JsonSerializeAsBean
public final class LineString extends GeometryObject {
    public final Object coordinates;
    
    public LineString(Object murtoviiva) {
        super("LineString", Optional.empty());
        this.coordinates = murtoviiva;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
}