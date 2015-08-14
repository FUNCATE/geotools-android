/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2007-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverageio.gdal.erdasimg;

import it.geosolutions.imageio.plugins.erdasimg.ErdasImgImageReaderSpi;

import java.util.Collections;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.coverageio.gdal.BaseGDALGridFormat;
import org.geotools.data.DataSourceException;
import org.geotools.factory.Hints;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.parameter.ParameterGroup;
import org.opengis.coverage.grid.Format;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.parameter.GeneralParameterDescriptor;


/**
 * An implementation a {@link Format} for the Erdas Imagine format.
 *
 * @author Daniele Romagnoli, GeoSolutions
 * @author Simone Giannecchini (simboss), GeoSolutions
 * @since 2.5.x
 *
 *
 * @source $URL$
 */
public final class ErdasImgFormat extends BaseGDALGridFormat implements Format {
    /**
     * Logger.
     */
    private final static Logger LOGGER = org.geotools.util.logging.Logging.getLogger(
            "org.geotools.coverageio.gdal.erdasimg");

    /**
     * Creates an instance and sets the metadata.
     */
    public ErdasImgFormat() {
        super(new ErdasImgImageReaderSpi());

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Creating a new ErdasImgFormat.");
        }

        setInfo();
    }

    /**
     * Sets the metadata information.
     */
    protected void setInfo() {
        final HashMap<String, String> info = new HashMap<String, String>();
        info.put("name", "ERDASImg");
        info.put("description", "Erdas Imagine Coverage Format");
        info.put("vendor", "Geotools");
        info.put("docURL", ""); // TODO: set something
        info.put("version", "1.0");
        mInfo = Collections.unmodifiableMap(info);

        // writing parameters
        writeParameters = null;

        // reading parameters
        readParameters = new ParameterGroup(new DefaultParameterDescriptorGroup(mInfo,
                    new GeneralParameterDescriptor[] {
                        READ_GRIDGEOMETRY2D, USE_JAI_IMAGEREAD, USE_MULTITHREADING,
                        SUGGESTED_TILE_SIZE
                    }));
    }

    /**
     * @see org.geotools.data.coverage.grid.AbstractGridFormat#getReader(Object, Hints)
     */
    public ErdasImgReader getReader(Object source, Hints hints) {
        try {
            return new ErdasImgReader(source, hints);
        } catch (MismatchedDimensionException e) {
            final RuntimeException re = new RuntimeException();
            re.initCause(e);
            throw re;
        } catch (DataSourceException e) {
            final RuntimeException re = new RuntimeException();
            re.initCause(e);
            throw re;
        }
    }
}
