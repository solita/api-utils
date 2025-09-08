package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.Collections.newMutableList;
import static fi.solita.utils.functional.Collections.newMutableMap;
import static fi.solita.utils.functional.Functional.flatMap;
import static fi.solita.utils.functional.Functional.map;
import static fi.solita.utils.functional.Functional.mkString;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Option.None;
import static fi.solita.utils.functional.Option.Some;

import java.awt.Graphics2D;
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
import org.geotools.api.feature.Property;
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
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ar.com.hjg.pngj.FilterType;
import fi.solita.utils.api.filtering.Filtering;
import fi.solita.utils.api.types.PropertyName;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import fi.solita.utils.functional.SemiGroups;
import fi.solita.utils.functional.Transformers;
import it.geosolutions.imageio.plugins.png.PNGWriter;

public class PngConversionService {
    
    public static final int tileSize = 256;
    public static final int imageSize = 4096;
    
    public static final int BUFFER_AROUND_POINTS = 20;
    
    public static final float COMPRESSION_QUALITY = 0.05F;
    
    private static final Pattern BBOX_INT = Pattern.compile("bbox=[0-9,]+");
    private static final Pattern BBOX_DEC = Pattern.compile("bbox=[0-9,.]+");
    
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
    
    private static final ObjectMapper DEFAULT_OM = new ObjectMapper();

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
    
    static final int toInt(double d) {
        return (int) Math.round(d);
    }
    
    static final double buffer(double x) {
        int numberOfDigits = String.valueOf((long)x).length();
        return 5*x/Math.pow(10, numberOfDigits-2);
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
    
    private static boolean isTile(Option<ReferencedEnvelope> bounds) {
        return bounds.isDefined() && bounds.get().getWidth() == bounds.get().getHeight();
    }
    
    public byte[] render(String pngRequestURI, Option<String> apikey, Option<ReferencedEnvelope> requestedBounds, String layerName) {
        double bufferRatioX = 1;
        double bufferRatioY = 1;
        boolean isTile = isTile(requestedBounds);
        if (isTile) {
            for (ReferencedEnvelope p: requestedBounds) {
                // add some buffer to the bbox, so that we retrieve features slightly
                // larger area than we are going to render. This way we get to render images
                // whose geometry point is in another tile, but whose graphic extends to this one.
                double originalWidth = p.getWidth();
                double originalHeight = p.getHeight();
                p.expandBy(buffer(originalWidth), buffer(originalHeight));
                Matcher matcher = BBOX_INT.matcher(pngRequestURI);
                if (matcher.find()) {
                    String bbox = "bbox=" + mkString(",", map(PngConversionService_.toInt.andThen(Transformers.toString), newList(p.getMinX(), p.getMinY(), p.getMaxX(), p.getMaxY())));
                    pngRequestURI = matcher.replaceAll(bbox);
                } else {
                    matcher = BBOX_DEC.matcher(pngRequestURI);
                    String bbox = "bbox=" + mkString(",", map(Transformers.toString, newList(p.getMinX(), p.getMinY(), p.getMaxX(), p.getMaxY())));
                    pngRequestURI = matcher.replaceAll(bbox);
                }
                if (originalWidth != 0) {
                    bufferRatioX = p.getWidth()/originalWidth;
                }
                if (originalHeight != 0) {
                    bufferRatioY = p.getHeight()/originalHeight;
                }
            }
        }
        URI uri = baseURI.resolve(pngRequestURI.replaceFirst(".png", ".geojson"));
        try {
            return render(isTile(requestedBounds) ? tileSize : imageSize,
                          isTile(requestedBounds) ? tileSize : imageSize,
                          uri,
                          requestedBounds,
                          isTile,
                          bufferRatioX,
                          bufferRatioY,
                          layerName,
                          apikey);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static Option<Pair<String,Object>> toEntry(Property p) throws JsonProcessingException {
        if (p.getName().getLocalPart().isEmpty() || p.getValue() instanceof Geometry) {
            return None();
        }
        
        return Some(Pair.of(p.getName().getLocalPart(), p.getValue()));
    }

    public byte[] render(int imageWidth, int imageHeight, URI uri, Option<ReferencedEnvelope> requestedBoundsWithBuffer, boolean isTile, double bufferRatioX, double bufferRatioY, String layerName, Option<String> apikey) throws IOException {
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
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.init(ft);
        ftb.add("json", String.class);
        ft = ftb.buildFeatureType();
        FeatureIterator<SimpleFeature> f = featureCollection.features();
        List<SimpleFeature> feats = newMutableList();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(ft);
        while (f.hasNext()) {
            SimpleFeature feature = f.next();
            fb.init(feature);
            // put all fields into custom "json" attribute so that it can be access from SLD using jsonPointer without property-not-found-errors...
            fb.set("json", DEFAULT_OM.writeValueAsString(newMap(SemiGroups.failUnequal(), flatMap(PngConversionService_.toEntry, feature.getProperties()))));
            feats.add(SimpleFeatureBuilder.retype(fb.buildFeature(null), ft));
            fb.reset();
        }
        featureCollection = new ListFeatureCollection(ft, feats);
        
        ReferencedEnvelope boundsWithBuffer = featureCollection.getBounds();
        if (!requestedBoundsWithBuffer.isDefined()) {
            // no explicit bounds given, so add some buffer around points
            if (boundsWithBuffer.getWidth() == 0) {
                boundsWithBuffer.expandBy(BUFFER_AROUND_POINTS, 0);
            }
            if (boundsWithBuffer.getHeight() == 0) {
                boundsWithBuffer.expandBy(0, BUFFER_AROUND_POINTS);
            }
        }
        boundsWithBuffer = requestedBoundsWithBuffer.getOrElse(boundsWithBuffer);
        
        long started = System.nanoTime();

        logger.debug("Rendering data...");
        final List<Exception> errors = newMutableList();
        final MapContent map = new MapContent();
        BufferedImage image;
        try {
            map.addLayer(new FeatureLayer(featureCollection, layerStyle));
            map.getViewport().setBounds(boundsWithBuffer);
            
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
            
            // render to a bit larger image to include added buffer.
            int renderWidth  = bufferRatioX == 1.0 ? imageWidth  : (int)(bufferRatioX*imageWidth);
            int renderHeight = bufferRatioY == 1.0 ? imageHeight : (int)(bufferRatioY*imageHeight);
            
            BufferedImage imageWithBuffer = new BufferedImage(renderWidth, renderHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = imageWithBuffer.createGraphics();
            Rectangle paintArea = new Rectangle(renderWidth, renderHeight);
            
            // make mapArea the same ratio as output image (square)
            double ratio = boundsWithBuffer.getWidth()/boundsWithBuffer.getHeight();
            ReferencedEnvelope mapArea = ReferencedEnvelope.create(boundsWithBuffer);
            mapArea.expandBy(ratio < 1 ? (boundsWithBuffer.getHeight()-boundsWithBuffer.getWidth())/2 : 0,
                             ratio > 1 ? (boundsWithBuffer.getWidth()-boundsWithBuffer.getHeight())/2 : 0);
            
            renderer.paint(graphics, paintArea, mapArea);
            
            // Remove added buffer. If no requestedBounds, then no need for buffering, and no need to clip the output.
            image = isTile
                ? imageWithBuffer.getSubimage((int)(renderWidth-imageWidth)/2, (int)(renderHeight-imageHeight)/2, imageWidth, imageHeight)
                : imageWithBuffer.getSubimage(0, 0, imageWidth, imageHeight);
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
