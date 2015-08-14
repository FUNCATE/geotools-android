/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2005-2008, Open Source Geospatial Foundation (OSGeo)
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
package org.geotools.coverage.processing.operation;

// JAI dependencies (for javadoc)
import javax.media.jai.operator.InvertDescriptor;

// Geotools dependencies
import org.geotools.util.NumberRange;
import org.geotools.coverage.processing.OperationJAI;


/**
 * Inverts the sample values of a coverage. For source coverages with signed data types,
 * the sample values of the destination coverage are defined by the pseudocode:
 *
 * <BLOCKQUOTE><PRE>
 * dst[x][y][b] = -src[x][y][b]
 * </PRE></BLOCKQUOTE>
 *
 * <P><STRONG>Name:</STRONG>&nbsp;<CODE>"Invert"</CODE><BR>
 *    <STRONG>JAI operator:</STRONG>&nbsp;<CODE>"{@linkplain InvertDescriptor Invert}"</CODE><BR>
 *    <STRONG>Parameters:</STRONG></P>
 * <table border='3' cellpadding='6' bgcolor='F4F8FF'>
 *   <tr bgcolor='#B9DCFF'>
 *     <th>Name</th>
 *     <th>Class</th>
 *     <th>Default value</th>
 *     <th>Minimum value</th>
 *     <th>Maximum value</th>
 *   </tr>
 *   <tr>
 *     <td>{@code "Source"}</td>
 *     <td>{@link org.geotools.coverage.grid.GridCoverage2D}</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *     <td align="center">N/A</td>
 *   </tr>
 * </table>
 *
 * @since 2.2
 *
 *
 * @source $URL$
 * @version $Id$
 * @author Martin Desruisseaux (IRD)
 *
 * @see org.geotools.coverage.processing.Operations#invert
 * @see InvertDescriptor
 */
public class Invert extends OperationJAI {
    /**
     * Serial number for interoperability with different versions.
     */
    private static final long serialVersionUID = 7297641092994880308L;

    /**
     * Constructs a default {@code "Invert"} operation.
     */
    public Invert() {
        super("Invert");
    }

    /**
     * Returns the expected range of values for the resulting image.
     */
    protected NumberRange deriveRange(final NumberRange[] ranges, final Parameters parameters) {
        final NumberRange range = ranges[0];
        final double min = -range.getMaximum();
        final double max = -range.getMinimum();
        return NumberRange.create(min, max);
    }
}
