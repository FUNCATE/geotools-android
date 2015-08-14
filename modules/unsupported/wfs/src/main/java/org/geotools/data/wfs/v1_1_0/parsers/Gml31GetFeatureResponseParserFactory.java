/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.wfs.v1_1_0.parsers;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.util.logging.Logger;

import net.opengis.wfs.GetFeatureType;

import org.eclipse.emf.ecore.EObject;
import org.geotools.data.wfs.protocol.wfs.WFSOperationType;
import org.geotools.data.wfs.protocol.wfs.WFSResponse;
import org.geotools.data.wfs.protocol.wfs.WFSResponseParser;
import org.geotools.data.wfs.protocol.wfs.WFSResponseParserFactory;
import org.geotools.data.wfs.v1_1_0.WFS_1_1_0_DataStore;
import org.geotools.util.logging.Logging;

/**
 * A WFS response parser factory for GetFeature requests in {@code text/xml; subtype=gml/3.1.1}
 * output format.
 * 
 * @author Gabriel Roldan (OpenGeo)
 * @version $Id$
 * @since 2.6
 *
 *
 *
 * @source $URL$
 *         http://gtsvn.refractions.net/trunk/modules/plugin/wfs/src/main/java/org/geotools/data
 *         /wfs/v1_1_0/parsers/Gml31GetFeatureResponseParserFactory.java $
 */
@SuppressWarnings("nls")
public class Gml31GetFeatureResponseParserFactory extends GmlAbstractGetFeatureResponseParserFactory {

    private static final String SUPPORTED_OUTPUT_FORMAT1 = "text/xml; subtype=gml/3.1.1";
    private static final String SUPPORTED_OUTPUT_FORMAT2 = "GML3";
    private static final String SUPPORTED_OUTPUT_FORMAT3 = "text/xml; subType=gml/3.1.1/profiles/gmlsf/1.0.0/0";

    protected boolean isSupportedOutputFormat(String outputFormat) {
        boolean matches = SUPPORTED_OUTPUT_FORMAT1.equals(outputFormat)
                || SUPPORTED_OUTPUT_FORMAT2.equals(outputFormat)
                || SUPPORTED_OUTPUT_FORMAT3.equals(outputFormat);
        return matches;
    }
}
