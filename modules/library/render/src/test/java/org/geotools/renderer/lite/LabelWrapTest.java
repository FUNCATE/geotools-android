package org.geotools.renderer.lite;

import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;

import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

import junit.framework.TestCase;

import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.style.FontCache;
import org.geotools.styling.Style;
import org.geotools.test.TestData;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * 
 *
 * @source $URL$
 */
public class LabelWrapTest extends TestCase {

    private static final long TIME = 10000;
    SimpleFeatureSource fs;
    ReferencedEnvelope bounds;

    @Override
    protected void setUp() throws Exception {
        // register a cross platform test
        FontCache.getDefaultInstance().registerFont(
                Font.createFont(Font.TRUETYPE_FONT, TestData.getResource(this, "Vera.ttf")
                        .openStream()));

        bounds = new ReferencedEnvelope(0, 10, 0, 10, null);
        
        // System.setProperty("org.geotools.test.interactive", "true");
        
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.add("geom", Point.class);
        builder.add("label", String.class);
        builder.setName("labelWrap");
        SimpleFeatureType type = builder.buildFeatureType();
        
        GeometryFactory gf = new GeometryFactory();
        SimpleFeature f1 = SimpleFeatureBuilder.build(type, new Object[]{gf.createPoint(new Coordinate(5, 8)), "A long label, no newlines"}, null);
        SimpleFeature f2 = SimpleFeatureBuilder
                .build(type, new Object[] { gf.createPoint(new Coordinate(5, 5)),
                        "A long label\nwith newlines" }, null);
        SimpleFeature f3 = SimpleFeatureBuilder.build(type,
                new Object[] { gf.createPoint(new Coordinate(5, 2)),
                        "A long label with (parenthesis)" }, null);
        
        MemoryDataStore data = new MemoryDataStore();
        data.addFeature(f1);
        data.addFeature(f2);
        data.addFeature(f3);
        fs = data.getFeatureSource("labelWrap");
        
    }
    
    public void testNoAutoWrap() throws Exception {
        Style style = RendererBaseTest.loadStyle(this, "textWrapDisabled.sld");
        BufferedImage image = renderLabels(fs, style, "Label wrap disabled");
        String refPath = "./src/test/resources/org/geotools/renderer/lite/test-data/textWrapDisabled.png";
        ImageAssert.assertEquals(new File(refPath), image, 1200);

    }
    
    public void testAutoWrap() throws Exception {
        Style style = RendererBaseTest.loadStyle(this, "textWrapEnabled.sld");
        BufferedImage image = renderLabels(fs, style, "Label wrap enabled");
        String refPath = "./src/test/resources/org/geotools/renderer/lite/test-data/textWrapEnabled.png";
        ImageAssert.assertEquals(new File(refPath), image, 1200);

    }

    private BufferedImage renderLabels(SimpleFeatureSource fs, Style style, String title)
            throws Exception {
        MapContent mc = new MapContent();
        mc.getViewport().setCoordinateReferenceSystem(DefaultGeographicCRS.WGS84);
        mc.addLayer(new FeatureLayer(fs, style));
        
        StreamingRenderer renderer = new StreamingRenderer();
        renderer.setJava2DHints(new RenderingHints(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON));
        renderer.setMapContent(mc);
        
        return RendererBaseTest.showRender(title, renderer, TIME, bounds);
    }
    
    
}
