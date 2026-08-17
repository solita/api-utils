package fi.solita.utils.api.format;




import java.util.Optional;

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
    PDF("application/pdf"),
    DWG("image/vnd.dwg")
    ;
    
    public final String mediaType;
    private SerializationFormat(String mediaType) {
        this.mediaType = mediaType;
    }
    
    public static Optional<SerializationFormat> valueOfExtension(String extension) {
        if (extension.equals("json")) {
            return Optional.of(SerializationFormat.JSON);
        } else if (extension.equals("geojson")) {
            return Optional.of(SerializationFormat.GEOJSON);
        } else if (extension.equals("jsonl")) {
            return Optional.of(SerializationFormat.JSONL);
        } else if (extension.equals("html")) {
            return Optional.of(SerializationFormat.HTML);
        } else if (extension.equals("csv")) {
            return Optional.of(SerializationFormat.CSV);
        } else if (extension.equals("tsv")) {
            return Optional.of(SerializationFormat.TSV);
        } else if (extension.equals("xlsx")) {
            return Optional.of(SerializationFormat.XLSX);
        } else if (extension.equals("png")) {
            return Optional.of(SerializationFormat.PNG);
        } else if (extension.equals("xml")) {
            return Optional.of(SerializationFormat.XML);
        } else if (extension.equals("gml")) {
            return Optional.of(SerializationFormat.GML);
        } else if (extension.equals("count")) {
            return Optional.of(SerializationFormat.COUNT);
        } else if (extension.equals("mvt")) {
            return Optional.of(SerializationFormat.MVT);
        } else if (extension.equals("chart")) {
            return Optional.of(SerializationFormat.CHART);
        } else if (extension.equals("pdf")) {
            return Optional.of(SerializationFormat.PDF);
        }
        return Optional.empty();
    }
}