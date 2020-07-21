package fi.solita.utils.api.format;

import static fi.solita.utils.functional.Collections.newList;
import static fi.solita.utils.functional.Collections.newMap;
import static fi.solita.utils.functional.FunctionalM.find;
import static fi.solita.utils.functional.Transformers.prepend;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.ParserConfigurationException;

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
import org.geotools.xml.Parser;
import org.joda.time.Duration;
import org.opengis.feature.simple.SimpleFeature;
import org.picocontainer.MutablePicoContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import ar.com.hjg.pngj.FilterType;
import fi.solita.utils.api.util.RequestUtil;
import fi.solita.utils.functional.Option;
import fi.solita.utils.functional.Pair;
import it.geosolutions.imageio.plugins.png.PNGWriter;

public class PngConversionService {
    
    public static final int tileSize = 256;
    
    public static final float COMPRESSION_QUALITY = 1.0F;
    
    private static final Logger logger = LoggerFactory.getLogger(PngConversionService.class);
    
    private final byte[] empty256;
    
    private final Map<String,Style> defaultStyles = newMap();
    
    private final URI baseURI;

    public PngConversionService(URI baseURI) {
        this.baseURI = baseURI;
        try {
            for (StyledLayer layer: createStyles(getSld())) {
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
    
    public byte[] render(HttpServletRequest req, ReferencedEnvelope paikka, String layerName) {
        URI uri = baseURI.resolve(req.getContextPath() + RequestUtil.getContextRelativePath(req).replaceFirst(".png", ".geojson") + Option.of(req.getQueryString()).map(prepend("?")).getOrElse(""));
        return render(uri, paikka, layerName, Option.of(req.getHeader(RequestUtil.API_KEY)));
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

    public byte[] render(int imageWidth, int imageHeight, URI uri, ReferencedEnvelope paikka, String layerName, Option<String> apikey) throws IOException {
        Style layerStyle = find(layerName, defaultStyles).get();
        logger.debug("Fetching geojson...");
        FeatureJSON io = new FeatureJSON();
        
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
        
        Reader reader = new InputStreamReader(in, Charset.forName("UTF-8"));
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
        final List<Exception> errors = newList();
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
    
    private static StyledLayer[] createStyles(final URL url) throws IOException, SAXException, ParserConfigurationException, URISyntaxException {
        if (url == null) {
            return new StyledLayer[0];
        }
        URI imagePath = url.toURI().resolve("r/img/");
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
