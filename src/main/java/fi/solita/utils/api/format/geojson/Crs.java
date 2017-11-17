package fi.solita.utils.api.format.geojson;


import fi.solita.utils.api.JsonSerializeAsBean;
import fi.solita.utils.api.types.SRSName;

@JsonSerializeAsBean
public abstract class Crs {
    public static final Crs epsg3067 = Crs.of(SRSName.DEFAULT);
    
    public final String type;
    public final Object properties;
    
    public static final Crs of(SRSName srsName) {
        return new NamedCrs(srsName.longValue);
    }
    
    private Crs(String type, Object properties) {
        this.type = type;
        this.properties = properties;
    }
    
    @JsonSerializeAsBean
    public static final class NamedCrs extends Crs {
        
        @JsonSerializeAsBean
        static class Properties {
            public final String name;
            public Properties(String name) {
                this.name = name;
            }
        }
        public NamedCrs(String urn) {
            super("name", new Properties(urn));
        }
    }
}