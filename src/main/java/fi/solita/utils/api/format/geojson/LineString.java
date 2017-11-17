package fi.solita.utils.api.format.geojson;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public final class LineString extends GeometryObject {
    public final Object coordinates;
    
    public LineString(Object murtoviiva) {
        super("LineString", Option.<Crs>None());
        this.coordinates = murtoviiva;
    }
    
    @Override
    public boolean isEmpty() {
        return false;
    }
}