package fi.solita.utils.api;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Functional.min;
import static fi.solita.utils.functional.FunctionalS.range;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import fi.solita.utils.functional.Tuple4;

public class WMTS {

    public static final String WMTS_TEMPLATE;
    public static final String LAYER_TEMPLATE;
    public static final String LAYER_URL_TEMPLATE;
    public static final int MIN_LAYER_ID;
    public static final String TILE_MATRIX_SET = "ETRS-TM35FIN";
    static {
        try {
            WMTS_TEMPLATE = IOUtils.toString(WMTS.class.getResource("/wmts_template.xml"));
            LAYER_TEMPLATE = IOUtils.toString(WMTS.class.getResource("/layer_template.xml"));
            
            Matcher ma = Pattern.compile("template=\"([^\"]+)\"").matcher(LAYER_TEMPLATE);
            ma.find();
            LAYER_URL_TEMPLATE = ma.group(1);
            
            List<Integer> layerIds = newList();
            Matcher m = Pattern.compile("<TileMatrix>\\s*<ows:Identifier>([^<]+)</ows:Identifier>").matcher(WMTS_TEMPLATE);
            while (m.find()) {
                layerIds.add(Integer.parseInt(m.group(1).trim()));
            }
            MIN_LAYER_ID = min(layerIds).get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static List<String> luoWmtsSeedUrlit(Iterable<Tuple4<String, String, String, String>> wmtsLayers) {
        List<String> ret = newList();
        
        for (int taso: newList(WMTS.MIN_LAYER_ID)) {
            BigDecimal tasonPikseleita = jhsTasonPikseleita(taso);
            int tileja = JHS_180_PIXSIZE_EXTENT[0][1].divide(tasonPikseleita).intValueExact();
            
            for (Tuple4<String, String, String, String> layer: wmtsLayers) {
                for (int i: range(0, tileja)) {
                    for (int j: range(0, tileja)) {
                        ret.add(
                            WMTS.LAYER_URL_TEMPLATE
                                .replace("{{name}}", layer._1)
                                .replace("{{time}}", SwaggerSupport.intervalNow())
                                .replace("{{path}}", layer._2)
                                .replace("{{qsPreTime}}", "")
                                .replace("{{qsPostTime}}", layer._4)
                                .replace("{{url}}", "latest/")
                                .replace("{TileMatrixSet}", WMTS.TILE_MATRIX_SET)
                                .replace("{TileMatrix}", Integer.toString(taso))
                                .replace("{TileRow}", Integer.toString(i))
                                .replace("{TileCol}", Integer.toString(j))
                                .replace("&amp;", "&"));
                    }
                }
            }
        }
        
        return ret;
    }
    
    public static final BigDecimal jhsTasonPikseleita(int taso) {
        return JHS_180_PIXSIZE_EXTENT[taso][1];
    }
    
    public static final BigDecimal[][] JHS_180_PIXSIZE_EXTENT = {
            /* 0 */ {BigDecimal.valueOf(8192), BigDecimal.valueOf(2097152)},
            /* 1 */ {BigDecimal.valueOf(4096), BigDecimal.valueOf(1048576)},
            /* 2 */ {BigDecimal.valueOf(2048), BigDecimal.valueOf(524288)},
            /* 3 */ {BigDecimal.valueOf(1024), BigDecimal.valueOf(262144)},
            /* 4 */ {BigDecimal.valueOf(512), BigDecimal.valueOf(131072)},
            /* 5 */ {BigDecimal.valueOf(256), BigDecimal.valueOf(65536)},
            /* 6 */ {BigDecimal.valueOf(128), BigDecimal.valueOf(32768)},
            /* 7 */ {BigDecimal.valueOf(64), BigDecimal.valueOf(16384)},
            /* 8 */ {BigDecimal.valueOf(32), BigDecimal.valueOf(8192)},
            /* 9 */ {BigDecimal.valueOf(16), BigDecimal.valueOf(4096)},
            /* 10 */ {BigDecimal.valueOf(8), BigDecimal.valueOf(2048)},
            /* 11 */ {BigDecimal.valueOf(4), BigDecimal.valueOf(1024)},
            /* 12 */ {BigDecimal.valueOf(2), BigDecimal.valueOf(512)},
            /* 13 */ {BigDecimal.valueOf(1), BigDecimal.valueOf(256)},
            /* 14 */ {BigDecimal.valueOf(0.5), BigDecimal.valueOf(128)},
            /* 15 */ {BigDecimal.valueOf(0.25), BigDecimal.valueOf(64)},
            /* 16 */ {BigDecimal.valueOf(0.125), BigDecimal.valueOf(32)},
            /* 17 */ {BigDecimal.valueOf(0.0625), BigDecimal.valueOf(16)}
        };

    
    public static String luoWmtsKuvaus(String title, String requestURI, Iterable<Tuple4<String, String, String, String>> wmtsLayers) {
        StringBuilder layers = new StringBuilder();
        for (Tuple4<String, String, String, String> layer: wmtsLayers) {
            layers.append(WMTS.LAYER_TEMPLATE
                        .replace("{{name}}", layer._1)
                        .replace("{{time}}", SwaggerSupport.intervalNow())
                        .replace("{{path}}", layer._2)
                        .replace("{{qsPreTime}}", "")
                        .replace("{{qsPostTime}}", layer._4)
                        .replace("{{matrixset1}}", "ETRS-TM35FIN")
                        .replace("{{matrixset2}}", "MERCATOR")
                        .replace("{{url}}", requestURI.replace("wmts.xml", "").replaceAll("[?].*", "")));
        }
        return WMTS.WMTS_TEMPLATE
                .replace("{{title}}", title)
                .replace("{{layers}}", layers.toString())
                .replace("{{url}}", requestURI.replace("wmts.xml", "").replaceAll("[?].*", ""));
    }
}
