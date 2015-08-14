/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2004-2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.data.wfs.v1_0_0;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.NoSuchElementException;

import junit.framework.TestCase;

import org.geotools.feature.IllegalAttributeException;
import org.xml.sax.SAXException;

/**
 *  summary sentence.
 * <p>
 * Paragraph ...
 * </p><p>
 * Responsibilities:
 * <ul>
 * <li>
 * <li>
 * </ul>
 * </p><p>
 * Example:<pre><code>
 * GeoServer x = new GeoServer( ... );
 * TODO code example
 * </code></pre>
 * </p>
 * @author dzwiers
 * @since 0.6.0
 *
 *
 *
 * @source $URL$
 */
public class MapServerOnlineTest extends TestCase {

    private URL url = null;
    
    public MapServerOnlineTest() throws MalformedURLException{
        url = new URL("http://map.ns.ec.gc.ca/MapServer/mapserv.exe?map=/mapserver/services/envdat/config.map&service=WFS&version=1.0.0&request=GetCapabilities");
    }
    
    // POST is buggy ... and returns HTML, doesn't seem to recognize the request
    public void testFeatureType() throws NoSuchElementException, IOException, SAXException{
        WFSDataStoreReadTest.doFeatureType(url,true,false,0);
    }
    public void testFeatureReader() throws NoSuchElementException, IOException, IllegalAttributeException, SAXException{
        WFSDataStoreReadTest.doFeatureReader(url,true,false,0);
    }
    public void testFeatureReaderWithFilter() throws NoSuchElementException, IllegalAttributeException, IOException, SAXException{
        WFSDataStoreReadTest.doFeatureReaderWithQuery(url,true,false,0);
    }
}
