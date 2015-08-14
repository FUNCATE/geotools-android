package org.geotools.data.ogr;

import static org.geotools.data.ogr.bridj.OgrLibrary.OGR_G_DestroyGeometry;

import java.io.IOException;

import org.bridj.Pointer;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 *
 * @source $URL: https://svn.osgeo.org/geotools/trunk/modules/unsupported/ogr/src/test/java/org/geotools/data/ogr/GeometryMapperTest.java $
 */
public class GeometryMapperTest extends TestCaseSupport {
    
	GeometryFactory gf = new GeometryFactory();

    @Override
    protected void setUp() throws Exception {
        GdalInit.init();
    }

    public void testLine() throws Exception {
        checkRoundTrip("LINESTRING(0 0, 10 10)");
    }

    public void testPolygon() throws Exception {
        checkRoundTrip("POLYGON((0 0, 0 10, 10 10, 10 0, 0 0))");
    }
    
    public void testPoint() throws Exception {
        checkRoundTrip("POINT(0 0)");
    }
    
    void checkRoundTrip(String geometryWkt) throws Exception {
        checkRoundTrip(geometryWkt, new GeometryMapper.WKB(gf));
        checkRoundTrip(geometryWkt, new GeometryMapper.WKT(gf));
    }

    void checkRoundTrip(String geometryWkt, GeometryMapper mapper) throws Exception {
        Geometry geometry = new WKTReader().read(geometryWkt);

        // to ogr and back
        Pointer ogrGeometry = mapper.parseGTGeometry(geometry);
        Geometry remapped = mapper.parseOgrGeometry(ogrGeometry);
        OGR_G_DestroyGeometry(ogrGeometry);

        assertEquals(geometry, remapped);
    }

   

}
