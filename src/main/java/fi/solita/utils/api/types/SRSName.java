package fi.solita.utils.api.types;

import static fi.solita.utils.functional.Collections.newList;

import java.util.List;
import java.util.Objects;

import fi.solita.utils.api.Documentation;

@Documentation(name_en = "SRSName", description = "Vastauksen koordinaattien muoto", description_en = "Response coordinate system")
public final class SRSName {
    public static final SRSName EPSG3067 = new SRSName("epsg:3067", "urn:ogc:def:crs:EPSG::3067", false, false);
    public static final SRSName EPSG4326 = new SRSName("epsg:4326", "urn:ogc:def:crs:EPSG::4326", true, true);
    public static final SRSName EPSG3857 = new SRSName("epsg:3857", "urn:ogc:def:crs:EPSG::3857", false, false);
    public static final SRSName CRS84    = new SRSName("crs:84"   , "urn:ogc:def:crs:OGC:1.3:CRS84", false, true);
    
    public static final List<SRSName> validValues = newList(EPSG3067, EPSG4326, EPSG3857, CRS84);
    
    public final String value;
    public final String longValue;
    public final boolean axisReversed;
    public final boolean usesDegrees;
    
    public SRSName(String value, String longValue, boolean axisReversed, boolean usesDegrees) {
        this.value = value;
        this.longValue = longValue;
        this.axisReversed = axisReversed;
        this.usesDegrees = usesDegrees;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SRSName srsName = (SRSName) o;
        return axisReversed == srsName.axisReversed && usesDegrees == srsName.usesDegrees && Objects.equals(value, srsName.value) && Objects.equals(longValue, srsName.longValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, longValue, axisReversed, usesDegrees);
    }

    @Override
    public String toString() {
        return value;
    }
}
