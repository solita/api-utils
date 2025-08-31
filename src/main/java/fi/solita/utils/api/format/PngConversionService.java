package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Collections.newMutableMap;
import static fi.solita.utils.functional.FunctionalM.find;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.poi.util.IOUtils;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayer;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.xml.styling.SLDParser;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.hjg.pngj.FilterType;
import fi.solita.utils.api.filtering.Filtering;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import it.geosolutions.imageio.plugins.png.PNGWriter;

public class PngConversionService {
    
    public static final int tileSize = 4096;
    
    public static final float COMPRESSION_QUALITY = 0.01F;
    
    private static final Logger logger = LoggerFactory.getLogger(PngConversionService.class);
    
    private static final Pattern PROPERTY_NAME_EXTRACTOR = Pattern.compile("Could not find '([^']+)' in the FeatureType");
    
    static {
        // to get rid of "Graphics2D from BufferedImage lacks BUFFERED_IMAGE hint" logging
        System.setProperty("org.apache.batik.warn_destination", "false");
    }
    
    private static final byte[] emptyTile;
    static {
        try {
            emptyTile = IOUtils.toByteArray(PngConversionService.class.getResource("/empty256.png").openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private static final ConcurrentMap<String,Map<String,Style>> allDefaultStyles = new ConcurrentHashMap<String, Map<String,Style>>();
    
    private final Map<String,Style> defaultStyles;
    
    private final URI baseURI;

    public PngConversionService(String imageBasePath, URI baseURI) {
        this.baseURI = baseURI;
        try {
            if (!allDefaultStyles.containsKey(imageBasePath)) {
                Map<String,Style> styles = newMutableMap();
                for (StyledLayer layer: createStyles(imageBasePath, getSld())) {
                    // use only the first style in the layer...
                    styles.put(layer.getName(), ((NamedLayer)layer).getStyles()[0]);
                }
                allDefaultStyles.put(imageBasePath, styles);
            }
            this.defaultStyles = allDefaultStyles.get(imageBasePath);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected URL getSld() {
        return getClass().getResource("/defaultStyle.sld");
    }
    
    public byte[] render(String requestURI, Option<String> apikey, Option<ReferencedEnvelope> paikka, String layerName) {
        URI uri = baseURI.resolve(requestURI.replaceFirst(".png", ".geojson"));
        return render(uri, paikka, layerName, apikey);
    }
    
    public byte[] render(URI uri, Option<ReferencedEnvelope> paikka, String layerName, Option<String> apikey) {
        try {
            return render(tileSize, tileSize, uri, paikka, layerName, apikey);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected InputStream fetchGeojson(URI uri, Option<String> apikey) throws IOException {
        URLConnection connection = uri.toURL().openConnection();
        for (String key: apikey) {
            connection.setRequestProperty(RequestUtil.API_KEY, key);
        }
        connection.setRequestProperty("Accept-Encoding", "gzip");
        
        Option<String> contentEncoding = Option.of(connection.getContentEncoding());
        InputStream in = connection.getInputStream();
        if (contentEncoding.isDefined() && contentEncoding.get().equals("gzip")) {
            in = new GZIPInputStream(in);
        }
        
        return in;
    }

    public byte[] render(int imageWidth, int imageHeight, URI uri, Option<ReferencedEnvelope> paikka, String layerName, Option<String> apikey) throws IOException {
        Style layerStyle = find(layerName, defaultStyles).orElse(new ApplyZero<Style>() {
            @Override
            public Style get() {
                throw new RuntimeException("Couldn't find layer with name: " + layerName);
            } });
        logger.debug("Fetching geojson...");
        String geojson = org.apache.commons.io.IOUtils.toString(fetchGeojson(uri, apikey), "UTF-8");
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        
        GeoJSONReader reader = new GeoJSONReader(geojson);
        try {
            featureCollection = reader.getFeatures();
        } finally {
            reader.close();
        }
        
        if (featureCollection.isEmpty()) {
            GeoJSONReader reader2 = new GeoJSONReader(geojson);
            try {
                SimpleFeature feature = reader2.getFeature();
                featureCollection = new ListFeatureCollection(feature.getFeatureType(), feature) ;
            } catch (Exception e) {
                logger.debug("Could not parse a Feature", e);
            } finally {
                reader2.close();
            }
        }
        
        if (featureCollection.isEmpty()) {
            return emptyTile;
        }
        
        // "GeoJSON is always WGS84" my ass!
        CoordinateReferenceSystem crs = new FeatureJSON().readCRS(geojson);
        SimpleFeatureType ft = SimpleFeatureTypeBuilder.retype(featureCollection.getSchema(), crs);
        FeatureIterator<SimpleFeature> f = featureCollection.features();
        List<SimpleFeature> feats = newMutableList();
        while (f.hasNext()) {
            feats.add(SimpleFeatureBuilder.retype(f.next(), ft));
        }
        featureCollection = new ListFeatureCollection(ft, feats);
        
        long started = System.nanoTime();

        logger.debug("Rendering data...");
        final List<Exception> errors = newMutableList();
        final MapContent map = new MapContent();
        BufferedImage image;
        try {
            map.addLayer(new FeatureLayer(featureCollection, layerStyle));
            map.getViewport().setBounds(paikka.getOrElse(featureCollection.getBounds()));
            
            final GTRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(map);
            renderer.setJava2DHints(new RenderingHints(newMap(
                Pair.of(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY),
                Pair.of(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_DEFAULT),
                Pair.of(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY),
                Pair.of(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY),
                Pair.of(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE)
            )));
            
            renderer.addRenderListener(new RenderListener() {
                @Override
                public void featureRenderer(SimpleFeature feature) {
                    logger.debug("Rendered: {}", feature);
                }
                @Override
                public void errorOccurred(Exception e) {
                    logger.error("Error rendering image", e);
                    renderer.stopRendering();
                    errors.add(e);
                }
            });
            
            image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            renderer.paint(image.createGraphics(), new Rectangle(imageWidth, imageHeight), map.getViewport().getBounds());
        } finally {
            map.dispose();
        }
        
        logger.debug("Generating PNG...");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        for (Exception e: errors) {
            if (e instanceof org.geotools.filter.IllegalFilterException) {
                Matcher m = PROPERTY_NAME_EXTRACTOR.matcher(e.getMessage());
                
                if (m.find() && m.groupCount() > 0) {
                    throw new Filtering.FilterPropertyNotFoundException(PropertyName.of(m.group(1)), e);
                }
            }
            throw new RuntimeException(e);
        }
        
        PNGWriter writer = new PNGWriter();
        if (!writer.isScanlineSupported(image)) {
            throw new IllegalStateException("Scanline not supported? Should it be?");
        }
        try {
            writer.writePNG(image, out, COMPRESSION_QUALITY, FilterType.FILTER_DEFAULT);
        } catch (Exception e1) {
            throw new RuntimeException(e1);
        }
        
        Duration duration = new Duration((System.nanoTime() - started)/1000/1000);
        
        logger.debug("Done! Returning data. Rendering took {}", duration);
        return out.toByteArray();
    }
    
    private static StyledLayer[] createStyles(String imageBasePath, final URL url) throws Exception {
        if (url == null) {
            return new StyledLayer[0];
        }
        URI imagePath = url.toURI().resolve(imageBasePath);
        final URL sourceUrl;
        try {
            sourceUrl = imagePath.toURL();
        } catch (Exception e) {
            throw new RuntimeException("Error constructing URL for styles, based on: " + url.toString(), e);
        }
        SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory(null), url);
        DefaultResourceLocator resLoc = new DefaultResourceLocator();
        resLoc.setSourceUrl(sourceUrl);
        parser.setOnLineResourceLocator(resLoc);
        StyledLayerDescriptor sld = (StyledLayerDescriptor) parser.parseSLD();
        return sld.getStyledLayers();
    }
}
