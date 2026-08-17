package fi.solita.utils.api.format.geojson;
import fi.solita.utils.api.JsonSerializeAsBean;
import java.util.Optional;

@JsonSerializeAsBean
public final class Polygon extends GeometryObject {
    public final Object coordinates;
    
    public Polygon(Object polygoni) {
        super("Polygon", Optional.empty());
        this.coordinates = polygoni;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
}