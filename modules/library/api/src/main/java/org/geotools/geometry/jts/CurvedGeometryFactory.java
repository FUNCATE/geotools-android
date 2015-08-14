/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2004-2014, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.geometry.jts;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * A {@link GeometryFactory} with extra methods to generate {@link CurvedGeometry} instances
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CurvedGeometryFactory extends GeometryFactory {

    private static final long serialVersionUID = -298811277709598383L;

    GeometryFactory delegate;

    double tolerance;

    public CurvedGeometryFactory(double tolerance) {
        this(new GeometryFactory(), tolerance);
    }

    public CurvedGeometryFactory(GeometryFactory delegate, double tolerance) {
        this.tolerance = tolerance;
        this.delegate = delegate;
    }

    /**
     * Creates a {@link CircularString} or a {@link CircularRing} depending on whether the points
     * are forming a closed ring, or not
     * 
     * @param dimension Number of dimensions in the control point array. For the time being, any
     *        value other than 2 will cause a IllegalArgumentException
     */
    public LineString createCurvedGeometry(int dimension, double... controlPoints) {
        if (dimension != 2) {
            throw new IllegalArgumentException(
                    "Invalid dimension value, right now only 2 dimensional curves are supported");
        }
        if (controlPoints[0] == controlPoints[controlPoints.length - 2]
                && controlPoints[1] == controlPoints[controlPoints.length - 1]) {
            return new CircularRing(controlPoints, this, tolerance);
        } else {
            return new CircularString(controlPoints, this, tolerance);
        }
    }

    /**
     * Creates a {@link CircularString} or a {@link CircularRing} depending on whether the points
     * are forming a closed ring, or not
     */
    public LineString createCurvedGeometry(CoordinateSequence cs) {
        int lastCoordinate = cs.size() - 1;
        if (cs.getOrdinate(0, 0) == cs.getOrdinate(lastCoordinate, 0)
                && cs.getOrdinate(0, 1) == cs.getOrdinate(lastCoordinate, 1)) {
            return new CircularRing(cs, this, tolerance);
        } else {
            return new CircularString(cs, this, tolerance);
        }
    }

    /**
     * Creates a compound curve with the given components
     */
    public LineString createCurvedGeometry(LineString... components) {
        if (components == null) {
            // return an empty lineString?
            return createLineString(new Coordinate[] {});
        }
        return createCurvedGeometry(Arrays.asList(components));
    }

    /**
     * Creates a compound curve with the given components
     */
    public LineString createCurvedGeometry(List<LineString> components) {
        if (components.isEmpty()) {
            // return an empty lineString?
            return createLineString(new Coordinate[] {});
        }
        if (components.size() == 1) {
            return components.get(0);
        }
        LineString first = components.get(0);
        LineString last = components.get(components.size() - 1);
        if (first.getStartPoint().equals(last.getEndPoint())) {
            return new CompoundRing(components, this, tolerance);
        } else {
            return new CompoundCurve(components, this, tolerance);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
        long temp;
        temp = Double.doubleToLongBits(tolerance);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CurvedGeometryFactory other = (CurvedGeometryFactory) obj;
        if (delegate == null) {
            if (other.delegate != null)
                return false;
        } else if (!delegate.equals(other.delegate))
            return false;
        if (Double.doubleToLongBits(tolerance) != Double.doubleToLongBits(other.tolerance))
            return false;
        return true;
    }

    /**
     * Returns the linearization tolerance used to create the curved geometries
     * 
     * @return
     */
    public double getTolerance() {
        return tolerance;
    }

    @Override
    public String toString() {
        return "CurvedGeometryFactory [delegate=" + delegate + ", tolerance=" + tolerance + "]";
    }

    /* Delegate methods */


    public Geometry toGeometry(Envelope envelope) {
        return delegate.toGeometry(envelope);
    }

    public PrecisionModel getPrecisionModel() {
        return delegate.getPrecisionModel();
    }

    public Point createPoint(Coordinate coordinate) {
        return delegate.createPoint(coordinate);
    }

    public Point createPoint(CoordinateSequence coordinates) {
        return delegate.createPoint(coordinates);
    }

    public MultiLineString createMultiLineString(LineString[] lineStrings) {
        boolean curved = false;
        for (LineString ls : lineStrings) {
            if (ls instanceof CurvedGeometry<?>) {
                curved = true;
                break;
            }
        }
        if (curved) {
            return new MultiCurve(Arrays.asList(lineStrings), this, tolerance);
        } else {
            return delegate.createMultiLineString(lineStrings);
        }
    }

    public GeometryCollection createGeometryCollection(Geometry[] geometries) {
        return delegate.createGeometryCollection(geometries);
    }

    public MultiPolygon createMultiPolygon(Polygon[] polygons) {
        return delegate.createMultiPolygon(polygons);
    }

    public LinearRing createLinearRing(Coordinate[] coordinates) {
        return delegate.createLinearRing(coordinates);
    }

    public LinearRing createLinearRing(CoordinateSequence coordinates) {
        return delegate.createLinearRing(coordinates);
    }

    public MultiPoint createMultiPoint(Point[] point) {
        return delegate.createMultiPoint(point);
    }

    public MultiPoint createMultiPoint(Coordinate[] coordinates) {
        return delegate.createMultiPoint(coordinates);
    }

    public MultiPoint createMultiPoint(CoordinateSequence coordinates) {
        return delegate.createMultiPoint(coordinates);
    }

    public Polygon createPolygon(LinearRing shell, LinearRing[] holes) {
        return delegate.createPolygon(shell, holes);
    }

    public Polygon createPolygon(CoordinateSequence coordinates) {
        return delegate.createPolygon(coordinates);
    }

    public Polygon createPolygon(Coordinate[] coordinates) {
        return delegate.createPolygon(coordinates);
    }

    public Polygon createPolygon(LinearRing shell) {
        return delegate.createPolygon(shell);
    }

    public Geometry buildGeometry(Collection geomList) {
        return delegate.buildGeometry(geomList);
    }

    public LineString createLineString(Coordinate[] coordinates) {
        return delegate.createLineString(coordinates);
    }

    public LineString createLineString(CoordinateSequence coordinates) {
        return delegate.createLineString(coordinates);
    }

    public Geometry createGeometry(Geometry g) {
        return delegate.createGeometry(g);
    }

    public int getSRID() {
        return delegate.getSRID();
    }

    public CoordinateSequenceFactory getCoordinateSequenceFactory() {
        return delegate.getCoordinateSequenceFactory();
    }

}
