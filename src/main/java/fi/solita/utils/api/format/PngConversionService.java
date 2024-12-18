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
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

import org.apache.poi.util.IOUtils;
import org.geotools.feature.FeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.renderer.RenderListener;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.sld.SLDConfiguration;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xsd.Parser;
import org.joda.time.Duration;
import org.opengis.feature.simple.SimpleFeature;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ar.com.hjg.pngj.FilterType;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.ApplyZero;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import it.geosolutions.imageio.plugins.png.PNGWriter;

public class PngConversionService {
    
    public static final int tileSize = 256;
    
    public static final float COMPRESSION_QUALITY = 1.0F;
    
    private static final Logger logger = LoggerFactory.getLogger(PngConversionService.class);
    
    private final byte[] empty256;
    
    private final Map<String,Style> defaultStyles = newMutableMap();
    
    private final URI baseURI;

    public PngConversionService(String imageBasePath, URI baseURI) {
        this.baseURI = baseURI;
        try {
            for (StyledLayer layer: createStyles(imageBasePath, getSld())) {
                // use only the first style in the layer...
                defaultStyles.put(layer.getName(), ((NamedLayer)layer).getStyles()[0]);
            }
            
            this.empty256 = IOUtils.toByteArray(PngConversionService.class.getResource("/empty" + tileSize + ".png").openStream());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    protected URL getSld() {
        return getClass().getResource("/defaultStyle.sld");
    }
    
    public byte[] render(String requestURI, Option<String> apikey, ReferencedEnvelope paikka, String layerName) {
        URI uri = baseURI.resolve(requestURI.replaceFirst(".png", ".geojson"));
        return render(uri, paikka, layerName, apikey);
    }
    
    public byte[] render(URI uri, ReferencedEnvelope paikka, String layerName, Option<String> apikey) {
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

    public byte[] render(int imageWidth, int imageHeight, URI uri, ReferencedEnvelope paikka, String layerName, Option<String> apikey) throws IOException {
        Style layerStyle = find(layerName, defaultStyles).orElse(new ApplyZero<Style>() {
            @Override
            public Style get() {
                throw new RuntimeException("Couldn't find layer with name: " + layerName);
            } });
        logger.debug("Fetching geojson...");
        FeatureJSON io = new FeatureJSON();
        Reader reader = new InputStreamReader(fetchGeojson(uri, apikey), Charset.forName("UTF-8"));
        FeatureCollection<?,?> featureCollection;
        try {
            featureCollection = io.readFeatureCollection(reader);
        } finally {
            reader.close();
        }
        
        if (featureCollection.isEmpty()) {
            return empty256;
        }
        
        long started = System.nanoTime();

        logger.debug("Rendering data...");
        final List<Exception> errors = newMutableList();
        final MapContent map = new MapContent();
        BufferedImage image;
        try {
            map.addLayer(new FeatureLayer(featureCollection, layerStyle));
            map.getViewport().setBounds(paikka);
            
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
                    // no-op
                }
                @Override
                public void errorOccurred(Exception e) {
                    logger.error("Error rendering image", e);
                    renderer.stopRendering();
                    errors.add(e);
                }
            });
            
            image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
            renderer.paint(image.createGraphics(), new Rectangle(imageWidth, imageWidth), map.getViewport().getBounds());
        } finally {
            map.dispose();
        }
        
        logger.debug("Generating PNG...");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        for (Exception e: errors) {
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
        Parser parser = new Parser(new SLDConfiguration() {
            // suhteellisten urlien parsinta vaatii ilmeisesti tällaista...
            // https://osgeo-org.atlassian.net/browse/GEOS-5800
            @Override
            protected void configureContext(MutablePicoContainer container) {
                super.configureContext(container);
                DefaultResourceLocator locator = new DefaultResourceLocator();
                locator.setSourceUrl(sourceUrl);
                for (Object o: container.getComponentInstancesOfType(ResourceLocator.class)) {
                    container.unregisterComponentByInstance(o);
                }
                container.registerComponentInstance(ResourceLocator.class, locator); 
            }
        });
        
        InputStream in = url.openStream();
        try {
            StyledLayerDescriptor sld = (StyledLayerDescriptor) parser.parse(in);
            return sld.getStyledLayers();
        } finally {
            in.close();
        }
    }
}
