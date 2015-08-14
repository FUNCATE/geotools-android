/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.data.solr;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Encapsulates how documents of a solr index are mapped to feature types.
 *
 * @see {@link org.geotools.data.solr.FieldLayerMapper}
 * @see {@link org.geotools.data.solr.SingleLayerMapper}
 */
public interface SolrLayerMapper {

    public static enum Type {
        FIELD {
            @Override
            public SolrLayerMapper createMapper(Map<String,Serializable> params) throws IOException {
                if (!params.containsKey(SolrDataStoreFactory.FIELD.key)) {
                    throw new IllegalArgumentException(format(
                        "Layer mapper '%s' requires '%s' key", FIELD.name(), SolrDataStoreFactory.FIELD.key));
                }
                return new FieldLayerMapper((String) SolrDataStoreFactory.FIELD.lookUp(params));
            }
        },
        SINGLE {
            @Override
            public SolrLayerMapper createMapper(Map<String,Serializable> params) {
                return new SingleLayerMapper();
            }
        };

        public abstract SolrLayerMapper createMapper(Map<String,Serializable> params) throws IOException;
    }

    /**
     * Creates the list of type names provided by the mapping.
     */
    List<String> createTypeNames(HttpSolrServer solr) throws Exception;

    /**
     * Prepares the filter query used to inspect solr for the purposes of deriving the schema.
     * <p>
     * This method can return <code>null</code>.
     * </p>
     */
    String prepareFilterQueryForSchema();

    /**
     * Prepares the filter query used to load documents from solr.
     * <p>
     * This method can return <code>null</code>.
     * </p>
     */
    String prepareFilterQuery(SimpleFeatureType featureType);
}
