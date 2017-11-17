package fi.solita.utils.api.format.geojson;
import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.functional.Functional;
import fi.solita.utils.functional.Option;

@JsonSerializeAsBean
public class MultiLineString extends GeometryObject {
    public final Iterable<?> coordinates;
    
    public static final MultiLineStringWithBBox withBBox(Iterable<?> murtoviivat, Object bbox) {
        return new MultiLineStringWithBBox(murtoviivat, bbox);
    }
    
    public MultiLineString(Iterable<?> murtoviivat) {
        super("MultiLineString", Option.<Crs>None());
        this.coordinates = murtoviivat;
    }
    
    @JsonSerializeAsBean
    public static final class MultiLineStringWithBBox extends MultiLineString {
        public final Object bbox;
        
        public MultiLineStringWithBBox(Iterable<?> murtoviivat, Object bbox) {
            super(murtoviivat);
            this.bbox = bbox;
        }
    }
    
    @Override
    public boolean isEmpty() {
        return Functional.isEmpty(coordinates);
    }
}