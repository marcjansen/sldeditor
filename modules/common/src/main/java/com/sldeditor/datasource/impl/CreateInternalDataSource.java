/*
 * SLD Editor - The Open Source Java SLD Editor
 *
 * Copyright (C) 2016, SCISYS UK Limited
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.sldeditor.datasource.impl;

import java.util.List;

import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.NamedLayerImpl;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.opengis.feature.simple.SimpleFeatureType;

import com.sldeditor.common.DataSourceFieldInterface;
import com.sldeditor.common.SLDDataInterface;
import com.sldeditor.common.output.SLDWriterInterface;
import com.sldeditor.common.output.impl.SLDWriterFactory;
import com.sldeditor.datasource.SLDEditorFileInterface;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;

/**
 * The Class CreateInternalDataSource.
 *
 * @author Robert Ward (SCISYS)
 */
public class CreateInternalDataSource implements CreateDataSourceInterface {

    /** The data source info. */
    private DataSourceInfo dsInfo = new DataSourceInfo();

    /** The Constant INTERNAL_SCHEMA_NAME. */
    private static final String INTERNAL_SCHEMA_NAME = "MEMORY";

    /** The Constant DEFAULT_GEOMETRY_FIELD_NAME. */
    private static final String DEFAULT_GEOMETRY_FIELD_NAME = "geom";

    /** The sld writer. */
    private SLDWriterInterface sldWriter = null;

    /**
     * Creates the.
     *
     * @param editorFile the editor file
     * @return the data source info
     */
    @Override
    public DataSourceInfo connect(SLDEditorFileInterface editorFile)
    {
        dsInfo.reset();

        if(editorFile != null)
        {
            StyledLayerDescriptor sld = editorFile.getSLD();
            SLDDataInterface sldData = editorFile.getSLDData();

            determineGeometryType(sld);

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();

            //set the name
            String typeName = INTERNAL_SCHEMA_NAME;
            dsInfo.setTypeName(typeName);
            b.setName( typeName );

            String namespace = null;
            b.setNamespaceURI(namespace);

            //add a geometry property
            b.setCRS( DefaultGeographicCRS.WGS84 ); // set crs first

            if(sldWriter == null)
            {
                sldWriter = SLDWriterFactory.createSLDWriter(null);
            }

            List<DataSourceFieldInterface> fieldList = sldData.getFieldList();

            setGeometryField(b, DEFAULT_GEOMETRY_FIELD_NAME);

            if((fieldList == null) || fieldList.isEmpty())
            {
                fieldList = ExtractAttributes.addDefaultFields(b, sldWriter.encodeSLD(sld));
            }
            else
            {
                addFields(b, fieldList);
            }

            // Store the fields
            sldData.setFieldList(fieldList);

            // Build the feature type
            SimpleFeatureType schema = b.buildFeatureType();
            dsInfo.setSchema(schema);

            CreateSampleData sampleData = new CreateSampleData();
            sampleData.create(schema);
            MemoryDataStore dataStore = sampleData.getDataStore();

            dsInfo.setDataStore(dataStore);
        }
        return dsInfo;
    }

    /**
     * Sets the geometry field.
     *
     * @param b the feature type builder
     * @param geometryFieldName the geometry field name
     */
    private void setGeometryField(SimpleFeatureTypeBuilder b, String geometryFieldName)
    {
        switch(dsInfo.getGeometryType())
        {
        case POLYGON:
            b.add( geometryFieldName, MultiPolygon.class );
            break;
        case LINE:
            b.add( geometryFieldName, LineString.class );
            break;
        case POINT:
        default:
            b.add( geometryFieldName, Point.class );
            break;
        }
        b.setDefaultGeometry( geometryFieldName );
    }

    /**
     * Adds the fields.
     *
     * @param b the feature type builder
     * @param fieldList the field list
     */
    private void addFields(SimpleFeatureTypeBuilder b,
            List<DataSourceFieldInterface> fieldList) {

        for(DataSourceFieldInterface field : fieldList)
        {
            if(field.getFieldType() == Geometry.class)
            {
                setGeometryField(b, field.getName());
            }
            else
            {
                b.add(field.getName(), field.getFieldType());
            }
        }
    }

    /**
     * Determine geometry type.
     *
     * @param sld the sld
     */
    private void determineGeometryType(StyledLayerDescriptor sld)
    {
        if(sld == null)
        {
            return;
        }

        List<StyledLayer> styledLayerList = sld.layers();
        int pointCount = 0;
        int lineCount = 0;
        int polygonCount = 0;

        for(StyledLayer styledLayer : styledLayerList)
        {
            if(styledLayer instanceof NamedLayerImpl)
            {
                NamedLayerImpl namedLayerImpl = (NamedLayerImpl)styledLayer;

                for(Style style : namedLayerImpl.styles())
                {
                    for(FeatureTypeStyle fts : style.featureTypeStyles())
                    {
                        for(Rule rule : fts.rules())
                        {
                            for(org.opengis.style.Symbolizer symbolizer : rule.symbolizers())
                            {
                                if(symbolizer instanceof PointSymbolizer)
                                {
                                    pointCount ++;
                                }
                                else if(symbolizer instanceof LineSymbolizer)
                                {
                                    lineCount ++;
                                }
                                else if(symbolizer instanceof PolygonSymbolizer)
                                {
                                    polygonCount ++;
                                }
                            }
                        }
                    }
                }
            }
        }

        if(pointCount > 0)
        {
            dsInfo.setGeometryType(GeometryTypeEnum.POINT);
        }
        else if(lineCount > 0)
        {
            dsInfo.setGeometryType(GeometryTypeEnum.LINE);
        }
        else if(polygonCount > 0)
        {
            dsInfo.setGeometryType(GeometryTypeEnum.POLYGON);
        }
    }
}
