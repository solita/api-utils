package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import org.springframework.http.MediaType;

import fi.solita.utils.functional.Option;

public enum SerializationFormat {
    JSON(MediaType.APPLICATION_JSON_VALUE),
    GEOJSON("application/vnd.geo+json;charset=UTF-8"),
    JSONL("application/x-ndjson"),
    HTML(MediaType.TEXT_HTML_VALUE + ";charset=UTF-8"),
    CSV("text/csv;charset=UTF-8"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8"),
    PNG(MediaType.IMAGE_PNG_VALUE),
    XML(MediaType.APPLICATION_XML_VALUE),
    GML("application/gml+xml"),
    COUNT(MediaType.TEXT_PLAIN_VALUE)
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
        }
        return None();
    }
}