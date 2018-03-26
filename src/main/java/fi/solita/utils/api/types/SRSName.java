package fi.solita.utils.api.types;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;

import fi.solita.utils.api.Documentation;

@Documentation(name_en = "SRSName", description = "Vastauksen koordinaattien muoto", description_en = "Response coordinate system")
public final class SRSName {
    public static final SRSName EPSG3067 = new SRSName("epsg:3067", "urn:ogc:def:crs:EPSG::3067", false);
    public static final SRSName EPSG4326 = new SRSName("epsg:4326", "urn:ogc:def:crs:EPSG::4326", true);
    public static final SRSName CRS84    = new SRSName("crs:84"   , "urn:ogc:def:crs:OGC:1.3:CRS84", false);
    
    public static final List<SRSName> validValues = newList(EPSG3067, EPSG4326, CRS84);
    
    public final String value;
    public final String longValue;
    public final boolean axisReversed;
    
    public SRSName(String value, String longValue, boolean axisReversed) {
        this.value = value;
        this.longValue = longValue;
        this.axisReversed = axisReversed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((longValue == null) ? 0 : longValue.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SRSName other = (SRSName) obj;
        if (longValue == null) {
            if (other.longValue != null)
                return false;
        } else if (!longValue.equals(other.longValue))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return value;
    }
}
