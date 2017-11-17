package fi.solita.utils.api.format.geojson;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public final class Polygon extends GeometryObject {
    public final Object coordinates;
    
    public Polygon(Object polygoni) {
        super("Polygon", Option.<Crs>None());
        this.coordinates = polygoni;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
}