package org.geotools.data.efeature.query;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.query.conditions.eobjects.structuralfeatures.EObjectAttributeValueCondition;
import org.opengis.filter.expression.Literal;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 *
 * @source $URL$
 */
public class EGeometryValueBBox extends EObjectAttributeValueCondition {

    public EGeometryValueBBox(EAttribute eAttribute, Literal geometry, boolean swapped)
            throws EFeatureEncoderException {
        super(eAttribute, SpatialConditionEncoder.bbox(eAttribute.getEAttributeType(), geometry, swapped));
    }

    public EGeometryValueBBox(EAttribute eAttribute, Object geometry, boolean swapped)
            throws EFeatureEncoderException {
        super(eAttribute, SpatialConditionEncoder.bbox(eAttribute.getEAttributeType(), geometry, swapped));
    }
    
    public EGeometryValueBBox(EAttribute eAttribute, Geometry geometry, boolean swapped)
            throws EFeatureEncoderException {
        super(eAttribute, SpatialConditionEncoder.bbox(eAttribute.getEAttributeType(), geometry, swapped));
    }

}
