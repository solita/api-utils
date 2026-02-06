package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import fi.solita.utils.functional.Option;

public enum SerializationFormat {
    JSON("application/json"),
    GEOJSON("application/vnd.geo+json;charset=UTF-8"),
    JSONL("application/x-ndjson"),
    HTML("text/html;charset=UTF-8"),
    CSV("text/csv;charset=UTF-8"),
    TSV("text/tab-separated-values;charset=UTF-8"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8"),
    PNG("image/png"),
    XML("application/xml"),
    GML("application/gml+xml"),
    COUNT("text/plain"),
    MVT("application/vnd.mapbox-vector-tile"),
    CHART("text/html;charset=UTF-8"),
    PDF("application/pdf")
    ;
    
    public final String mediaType;
    private SerializationFormat(String mediaType) {
        this.mediaType = mediaType;
    }
    
    public static Option<SerializationFormat> valueOfExtension(String extension) {
        if (extension.equals("json")) {
            return Some(SerializationFormat.JSON);
        } else if (extension.equals("geojson")) {
            return Some(SerializationFormat.GEOJSON);
        } else if (extension.equals("jsonl")) {
            return Some(SerializationFormat.JSONL);
        } else if (extension.equals("html")) {
            return Some(SerializationFormat.HTML);
        } else if (extension.equals("csv")) {
            return Some(SerializationFormat.CSV);
        } else if (extension.equals("tsv")) {
            return Some(SerializationFormat.TSV);
        } else if (extension.equals("xlsx")) {
            return Some(SerializationFormat.XLSX);
        } else if (extension.equals("png")) {
            return Some(SerializationFormat.PNG);
        } else if (extension.equals("xml")) {
            return Some(SerializationFormat.XML);
        } else if (extension.equals("gml")) {
            return Some(SerializationFormat.GML);
        } else if (extension.equals("count")) {
            return Some(SerializationFormat.COUNT);
        } else if (extension.equals("mvt")) {
            return Some(SerializationFormat.MVT);
        } else if (extension.equals("chart")) {
            return Some(SerializationFormat.CHART);
        } else if (extension.equals("pdf")) {
            return Some(SerializationFormat.PDF);
        }
        return None();
    }
}