package fi.solita.utils.api.format.geojson;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public final class MultiPolygon extends GeometryObject {
    public final Iterable<?> coordinates;
    
    public MultiPolygon(Iterable<?> polygonit) {
        super("MultiPolygon", Option.<Crs>None());
        this.coordinates = polygonit;
    }

    @Override
    public boolean isEmpty() {
        return Functional.isEmpty(coordinates);
    }
}