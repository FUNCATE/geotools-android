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
import java.util.Iterator;
import java.util.List;

import org.geotools.data.FeatureReader;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.spatial.BBOX;

import com.vividsolutions.jts.geom.Envelope;

/**
 * This strategy addresses a bug in most MapServer implementations where a filter is required in order for all the features to 
 * be returned.  So if the Filter is Filter.NONE or Query.ALL then a BBox Filter is constructed that is the entire layer.
 * 
 * @author Jesse
 * @since 1.1.0
 *
 *
 *
 *
 * @source $URL$
 */
public class MapServerWFSStrategy extends StrictWFSStrategy implements WFSStrategy {

    private Integer compliance;

    public MapServerWFSStrategy( WFS_1_0_0_DataStore store ) {
        this(store, null);
    }
    
    public MapServerWFSStrategy(WFS_1_0_0_DataStore store, Integer filterCompliance) {
        super(store);
        compliance = filterCompliance;
    }

    protected  FeatureReader<SimpleFeatureType, SimpleFeature> createFeatureReader(Transaction transaction, Query query) throws IOException {
        return new MapServerWFSFeatureReader(transaction, query, compliance == null ? COMPLIANCE_LEVEL : compliance);
    }
    
    protected class MapServerWFSFeatureReader extends StrictFeatureReader{

        public MapServerWFSFeatureReader( Transaction transaction, Query query, Integer level ) throws IOException {
            super(transaction, query, level);
        }
        
        protected void init( Transaction transaction, Query query, Integer level ) throws IOException {
            Filter filter = query.getFilter();
            Query query2;
            if( filter == Filter.INCLUDE ){
                FilterFactory fac=CommonFactoryFinder.getFilterFactory(null);
                try {
                    SimpleFeatureType schema = store.getSchema(query.getTypeName());
                    String attName = schema.getGeometryDescriptor().getLocalName();
                    
                    List<FeatureSetDescription> fts = store.capabilities.getFeatureTypes(); // FeatureSetDescription
                    Iterator<FeatureSetDescription> i = fts.iterator();
                    String desiredType = query.getTypeName().substring(query.getTypeName()
                            .indexOf(":") + 1);
                    
                    Envelope bbox = null;
                    while (i.hasNext()) {
                        FeatureSetDescription fsd = (FeatureSetDescription) i.next();
                        String fsdName = (fsd.getName() == null) ? null
                                 : fsd.getName().substring(fsd.getName().indexOf(":") + 1);

                        if (desiredType.equals(fsdName)) {
                            bbox = fsd.getLatLongBoundingBox();
                            
                        }
                    }

                    if( bbox==null ){
                        query2=query;
                    }else{
                        BBOX newFilter = fac.bbox(attName, bbox.getMinX(), bbox.getMinY(), 
                                bbox.getMaxX(), bbox.getMaxY(), "EPSG:4326");
                        
                        query2 = new Query(query);
                        query2.setFilter(newFilter);
                    }
                } catch (IllegalFilterException e) {
                    query2=query;
                }
            }else
                query2=query;
            super.init(transaction, query2, level);
        }

        
    }

}
