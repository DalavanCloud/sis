/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sis.internal.referencing;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.text.Format;
import javax.measure.Unit;
import javax.measure.quantity.Length;

import org.opengis.util.FactoryException;
import org.opengis.util.InternationalString;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.IdentifiedObject;
import org.opengis.referencing.crs.SingleCRS;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.crs.TemporalCRS;
import org.opengis.referencing.crs.VerticalCRS;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.CRSFactory;
import org.opengis.referencing.cs.CSFactory;
import org.opengis.referencing.cs.AxisDirection;
import org.opengis.referencing.cs.CartesianCS;
import org.opengis.referencing.cs.CoordinateSystem;
import org.opengis.referencing.cs.CoordinateSystemAxis;
import org.opengis.referencing.datum.PrimeMeridian;
import org.opengis.referencing.operation.Matrix;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;
import org.opengis.referencing.operation.TransformException;
import org.opengis.referencing.operation.OperationMethod;
import org.opengis.referencing.operation.SingleOperation;
import org.opengis.referencing.operation.CoordinateOperation;
import org.opengis.referencing.operation.CoordinateOperationFactory;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.metadata.citation.Citation;
import org.opengis.metadata.citation.OnLineFunction;
import org.opengis.metadata.citation.OnlineResource;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.metadata.extent.GeographicExtent;
import org.opengis.metadata.extent.VerticalExtent;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Envelope;

import org.apache.sis.geometry.Envelopes;
import org.apache.sis.geometry.AbstractEnvelope;
import org.apache.sis.geometry.DirectPosition2D;
import org.apache.sis.geometry.CoordinateFormat;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.CommonCRS;
import org.apache.sis.referencing.IdentifiedObjects;
import org.apache.sis.referencing.AbstractIdentifiedObject;
import org.apache.sis.referencing.cs.AbstractCS;
import org.apache.sis.referencing.cs.CoordinateSystems;
import org.apache.sis.referencing.crs.DefaultDerivedCRS;
import org.apache.sis.referencing.crs.DefaultTemporalCRS;
import org.apache.sis.referencing.datum.BursaWolfParameters;
import org.apache.sis.referencing.operation.transform.MathTransforms;
import org.apache.sis.referencing.operation.DefaultCoordinateOperationFactory;
import org.apache.sis.parameter.DefaultParameterDescriptor;
import org.apache.sis.parameter.Parameterized;
import org.apache.sis.io.wkt.Formatter;
import org.apache.sis.io.wkt.FormattableObject;
import org.apache.sis.internal.metadata.WKTKeywords;
import org.apache.sis.metadata.iso.extent.DefaultExtent;
import org.apache.sis.metadata.iso.extent.DefaultVerticalExtent;
import org.apache.sis.metadata.iso.extent.DefaultTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultSpatialTemporalExtent;
import org.apache.sis.metadata.iso.extent.DefaultGeographicBoundingBox;
import org.apache.sis.measure.Latitude;
import org.apache.sis.measure.Longitude;
import org.apache.sis.internal.metadata.AxisDirections;
import org.apache.sis.internal.metadata.ReferencingServices;
import org.apache.sis.internal.referencing.provider.Affine;
import org.apache.sis.internal.system.DefaultFactories;
import org.apache.sis.internal.system.Modules;
import org.apache.sis.internal.util.Constants;
import org.apache.sis.internal.util.TemporalUtilities;
import org.apache.sis.util.CharSequences;
import org.apache.sis.util.collection.Containers;
import org.apache.sis.util.resources.Vocabulary;
import org.apache.sis.util.resources.Errors;
import org.apache.sis.util.logging.Logging;
import org.apache.sis.util.Exceptions;
import org.apache.sis.util.Utilities;
import org.apache.sis.util.iso.DefaultNameSpace;

// Branch-dependent imports
import org.apache.sis.metadata.iso.citation.DefaultCitation;
import org.apache.sis.referencing.factory.GeodeticObjectFactory;
import org.apache.sis.referencing.cs.DefaultParametricCS;
import org.apache.sis.referencing.datum.DefaultParametricDatum;
import org.apache.sis.referencing.factory.InvalidGeodeticParameterException;
import org.opengis.referencing.datum.Datum;
import org.opengis.referencing.datum.DatumFactory;


/**
 * Implements the referencing services needed by the {@code "sis-metadata"} module.
 *
 * @author  Martin Desruisseaux (Geomatys)
 * @version 1.0
 * @since   0.5
 * @module
 */
public final class ServicesForMetadata extends ReferencingServices {
    /**
     * Name of an {@link OnLineFunction} code list value, used for transferring information about the EPSG database.
     */
    public static final String CONNECTION = "CONNECTION";

    /**
     * Creates a new instance. This constructor is invoked by reflection only.
     */
    public ServicesForMetadata() {
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                        SERVICES FOR ISO 19115 METADATA                        ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Creates an exception message for a spatial, vertical or temporal dimension not found.
     * The given key must be one of {@code Resources.Keys} constants.
     */
    private static String dimensionNotFound(final short resourceKey, final CoordinateReferenceSystem crs) {
        if (crs == null) {
            return Errors.format(Errors.Keys.UnspecifiedCRS);
        } else {
            return Resources.format(resourceKey, crs.getName());
        }
    }

    /**
     * Implementation of the public {@code setBounds(…, DefaultGeographicBoundingBox, …)} methods for
     * the horizontal extent. If the {@code crs} argument is null, then it is caller's responsibility
     * to ensure that the given envelope is two-dimensional.
     *
     * <p>If {@code findOpCaller} is non-null, then this method will be executed in optional mode:
     * some failures will cause this method to return {@code null} instead than throwing an exception.
     * Note that {@link TransformException} may still be thrown but not directly by this method.
     * Warning may be logged, but in such case this method presumes that public caller is the named
     * method from {@link Envelopes} — typically {@link Envelopes#findOperation(Envelope, Envelope)}.</p>
     *
     * @param  envelope       the source envelope.
     * @param  target         the target bounding box, or {@code null} for creating it automatically.
     * @param  crs            the envelope CRS, or {@code null} if unknown.
     * @param  normalizedCRS  the horizontal component of the given CRS, or null if the {@code crs} argument is null.
     * @param  findOpCaller   non-null for replacing some (not all) exceptions by {@code null} return value.
     * @return the bounding box or {@code null} on failure. Never {@code null} if {@code findOpCaller} argument is {@code null}.
     * @throws TransformException if the given envelope can not be transformed.
     */
    private static DefaultGeographicBoundingBox setGeographicExtent(Envelope envelope, DefaultGeographicBoundingBox target,
            final CoordinateReferenceSystem crs, final GeographicCRS normalizedCRS, final String findOpCaller) throws TransformException
    {
        if (normalizedCRS != null) {
            // No need to check for dimension, since GeodeticCRS can not have less than 2.
            final CoordinateSystem cs1 = crs.getCoordinateSystem();
            final CoordinateSystem cs2 = normalizedCRS.getCoordinateSystem();
            if (!Utilities.equalsIgnoreMetadata(cs2.getAxis(0), cs1.getAxis(0)) ||
                !Utilities.equalsIgnoreMetadata(cs2.getAxis(1), cs1.getAxis(1)))
            {
                final CoordinateOperation operation;
                final CoordinateOperationFactory factory = CoordinateOperations.factory();
                try {
                    operation = factory.createOperation(crs, normalizedCRS);
                } catch (FactoryException e) {
                    if (findOpCaller != null) {
                        // See javadoc for the assumption that optional mode is used by Envelopes.findOperation(…).
                        Logging.recoverableException(Logging.getLogger(Modules.REFERENCING), Envelopes.class, findOpCaller, e);
                        return null;
                    }
                    throw new TransformException(Resources.format(Resources.Keys.CanNotTransformEnvelopeToGeodetic), e);
                }
                envelope = Envelopes.transform(operation, envelope);
            }
        }
        /*
         * At this point, the envelope should use (longitude, latitude) coordinates in degrees.
         * The envelope may cross the anti-meridian if the envelope implementation is an Apache SIS one.
         * For other implementations, the longitude range may be conservatively expanded to [-180 … 180]°.
         */
        double westBoundLongitude, eastBoundLongitude;
        double southBoundLatitude, northBoundLatitude;
        if (envelope instanceof AbstractEnvelope) {
            final AbstractEnvelope ae = (AbstractEnvelope) envelope;
            westBoundLongitude = ae.getLower(0);
            eastBoundLongitude = ae.getUpper(0);            // Cross anti-meridian if eastBoundLongitude < westBoundLongitude.
            southBoundLatitude = ae.getLower(1);
            northBoundLatitude = ae.getUpper(1);
        } else {
            westBoundLongitude = envelope.getMinimum(0);
            eastBoundLongitude = envelope.getMaximum(0);    // Expanded to [-180 … 180]° if it was crossing the anti-meridian.
            southBoundLatitude = envelope.getMinimum(1);
            northBoundLatitude = envelope.getMaximum(1);
        }
        /*
         * The envelope transformation at the beginning of this method intentionally avoided to apply datum shift.
         * This implies that the prime meridian has not been changed and may be something else than Greenwich.
         * We need to take it in account manually.
         *
         * Note that there is no need to normalize the longitudes back to the [-180 … +180]° range after the rotation, or
         * to verify if the longitude span is 360°. Those verifications will be done automatically by target.setBounds(…).
         */
        if (normalizedCRS != null) {
            final double rotation = CRS.getGreenwichLongitude(normalizedCRS);
            westBoundLongitude += rotation;
            eastBoundLongitude += rotation;
        }
        /*
         * In the particular case where this method is invoked (indirectly) for Envelopes.findOperation(…) purposes,
         * replace NaN values by the whole world.  We do that only for Envelopes.findOperation(…) since we know that
         * the geographic bounding box will be used for choosing a CRS, and a conservative approach is to select the
         * CRS valid in the widest area. If this method is invoked for other usages, then we keep NaN values because
         * we don't know the context (union, intersection, something else?).
         */
        if (findOpCaller != null) {
            if (Double.isNaN(southBoundLatitude)) southBoundLatitude = Latitude.MIN_VALUE;
            if (Double.isNaN(northBoundLatitude)) northBoundLatitude = Latitude.MAX_VALUE;
            if (Double.isNaN(eastBoundLongitude) || Double.isNaN(westBoundLongitude)) {
                // Conservatively set the two bounds because may be spanning the anti-meridian.
                eastBoundLongitude = Longitude.MIN_VALUE;
                westBoundLongitude = Longitude.MAX_VALUE;
            }
        }
        if (target == null) {
            target = new DefaultGeographicBoundingBox();
        }
        target.setBounds(westBoundLongitude, eastBoundLongitude, southBoundLatitude, northBoundLatitude);
        target.setInclusion(Boolean.TRUE);
        return target;
    }

    /**
     * Implementation of the public {@code setBounds} methods for the vertical extent.
     * If the {@code crs} argument is null, then it is caller's responsibility to ensure
     * that the given envelope is one-dimensional.
     *
     * @param  envelope     the source envelope.
     * @param  target       the target vertical extent.
     * @param  crs          the envelope CRS, or {@code null} if unknown.
     * @param  verticalCRS  the vertical component of the given CRS, or null if the {@code crs} argument is null.
     */
    private static void setVerticalExtent(final Envelope envelope, final DefaultVerticalExtent target,
            final CoordinateReferenceSystem crs, final VerticalCRS verticalCRS)
    {
        final int dim;
        if (verticalCRS == null) {
            dim = 0;
        } else {
            dim = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), verticalCRS.getCoordinateSystem());
            assert dim >= 0 : crs; // Should not fail since 'verticalCRS' has been extracted from 'crs' by the caller.
        }
        target.setMinimumValue(envelope.getMinimum(dim));
        target.setMaximumValue(envelope.getMaximum(dim));
        target.setVerticalCRS(verticalCRS);
    }

    /**
     * Implementation of the public {@code setBounds} methods for the temporal extent.
     *
     * @param  envelope     the source envelope.
     * @param  target       the target temporal extent.
     * @param  crs          the envelope CRS (mandatory, can not be {@code null}).
     * @param  temporalCRS  the temporal component of the given CRS (mandatory).
     * @throws UnsupportedOperationException if no implementation of {@code TemporalFactory} has been found
     *         on the classpath.
     */
    private static void setTemporalExtent(final Envelope envelope, final DefaultTemporalExtent target,
            final CoordinateReferenceSystem crs, final TemporalCRS temporalCRS)
    {
        final int dim = AxisDirections.indexOfColinear(crs.getCoordinateSystem(), temporalCRS.getCoordinateSystem());
        assert dim >= 0 : crs; // Should not fail since 'temporalCRS' has been extracted from 'crs' by the caller.
        final DefaultTemporalCRS converter = DefaultTemporalCRS.castOrCopy(temporalCRS);
        target.setBounds(TemporalUtilities.toDate(converter.toInstant(envelope.getMinimum(dim))),
                         TemporalUtilities.toDate(converter.toInstant(envelope.getMaximum(dim))));
    }

    /**
     * Sets a geographic bounding box from the specified envelope.
     * If the envelope has no CRS, then (<var>longitude</var>, <var>latitude</var>) axis order is assumed.
     * If the envelope CRS is not geographic, then the envelope will be transformed to a geographic CRS.
     * If {@code findOpCaller} is {@code true}, then some failures will cause this method to return {@code null}
     * instead than throwing an exception, and warning may be logged with assumption that caller is the named
     * method from {@link Envelopes} — typically {@link Envelopes#findOperation(Envelope, Envelope)}.
     *
     * @param  envelope      the source envelope.
     * @param  target        the target bounding box, or {@code null} for creating it automatically.
     * @param  findOpCaller  non-null for replacing some (not all) exceptions by {@code null} return value.
     * @return the bounding box or {@code null} on failure. Never {@code null} if {@code findOpCaller} argument is {@code null}.
     * @throws TransformException if the given envelope can not be transformed.
     */
    @Override
    public DefaultGeographicBoundingBox setBounds(final Envelope envelope, final DefaultGeographicBoundingBox target,
            final String findOpCaller) throws TransformException
    {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs);
        if (normalizedCRS == null) {
            if (crs != null) {
                normalizedCRS = CommonCRS.defaultGeographic();
            } else if (envelope.getDimension() != 2) {
                if (findOpCaller != null) return null;
                throw new TransformException(dimensionNotFound(Resources.Keys.MissingHorizontalDimension_1, crs));
            }
        }
        return setGeographicExtent(envelope, target, crs, normalizedCRS, findOpCaller);
    }

    /**
     * Sets a vertical extent with the value inferred from the given envelope.
     * Only the vertical ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target vertical extent where to store envelope information.
     * @throws TransformException if no vertical component can be extracted from the given envelope.
     */
    @Override
    public void setBounds(final Envelope envelope, final DefaultVerticalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        if (verticalCRS == null && envelope.getDimension() != 1) {
            throw new TransformException(dimensionNotFound(Resources.Keys.MissingVerticalDimension_1, crs));
        }
        setVerticalExtent(envelope, target, crs, verticalCRS);
    }

    /**
     * Sets a temporal extent with the value inferred from the given envelope.
     * Only the vertical ordinates are extracted; all other ordinates are ignored.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target temporal extent where to store envelope information.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     */
    @Override
    public void setBounds(final Envelope envelope, final DefaultTemporalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
        if (temporalCRS == null) { // Mandatory for the conversion from numbers to dates.
            throw new TransformException(dimensionNotFound(Resources.Keys.MissingTemporalDimension_1, crs));
        }
        setTemporalExtent(envelope, target, crs, temporalCRS);
    }

    /**
     * Sets the geographic, vertical and temporal extents with the values inferred from the given envelope.
     * If the given {@code target} has more geographic or vertical extents than needed (0 or 1), then the
     * extraneous extents are removed.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target spatiotemporal extent where to store envelope information.
     * @throws TransformException if no temporal component can be extracted from the given envelope.
     */
    @Override
    public void setBounds(final Envelope envelope, final DefaultSpatialTemporalExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final SingleCRS horizontalCRS = CRS.getHorizontalComponent(crs);
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
        if (horizontalCRS == null && verticalCRS == null && temporalCRS == null) {
            throw new TransformException(dimensionNotFound(Resources.Keys.MissingSpatioTemporalDimension_1, crs));
        }
        /*
         * Try to set the geographic bounding box first, because this operation may fail with a
         * TransformException while the other operations (vertical and temporal) should not fail.
         * So doing the geographic part first help us to get a "all or nothing" behavior.
         */
        DefaultGeographicBoundingBox box = null;
        boolean useExistingBox = (horizontalCRS != null);
        final Collection<GeographicExtent> spatialExtents = target.getSpatialExtent();
        final Iterator<GeographicExtent> it = spatialExtents.iterator();
        while (it.hasNext()) {
            final GeographicExtent extent = it.next();
            if (extent instanceof GeographicBoundingBox) {
                if (useExistingBox && (extent instanceof DefaultGeographicBoundingBox)) {
                    box = (DefaultGeographicBoundingBox) extent;
                    useExistingBox = false;
                } else {
                    it.remove();
                }
            }
        }
        if (horizontalCRS != null) {
            if (box == null) {
                box = new DefaultGeographicBoundingBox();
                spatialExtents.add(box);
            }
            GeographicCRS normalizedCRS = ReferencingUtilities.toNormalizedGeographicCRS(crs);
            if (normalizedCRS == null) {
                normalizedCRS = CommonCRS.defaultGeographic();
            }
            setGeographicExtent(envelope, box, crs, normalizedCRS, null);
        }
        /*
         * Other dimensions (vertical and temporal).
         */
        if (verticalCRS != null) {
            VerticalExtent e = target.getVerticalExtent();
            if (!(e instanceof DefaultVerticalExtent)) {
                e = new DefaultVerticalExtent();
                target.setVerticalExtent(e);
            }
            setVerticalExtent(envelope, (DefaultVerticalExtent) e, crs, verticalCRS);
        } else {
            target.setVerticalExtent(null);
        }
        if (temporalCRS != null) {
            setTemporalExtent(envelope, target, crs, temporalCRS);
        } else {
            target.setExtent(null);
        }
    }

    /**
     * Initializes a horizontal, vertical and temporal extent with the values inferred from the given envelope.
     *
     * @param  envelope  the source envelope.
     * @param  target    the target extent where to store envelope information.
     * @throws TransformException if a coordinate transformation was required and failed.
     * @throws UnsupportedOperationException if this method requires an Apache SIS module
     *         which has been found on the classpath.
     */
    @Override
    public void addElements(final Envelope envelope, final DefaultExtent target) throws TransformException {
        final CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        final SingleCRS horizontalCRS = CRS.getHorizontalComponent(crs);
        final VerticalCRS verticalCRS = CRS.getVerticalComponent(crs, true);
        final TemporalCRS temporalCRS = CRS.getTemporalComponent(crs);
        if (horizontalCRS == null && verticalCRS == null && temporalCRS == null) {
            throw new TransformException(dimensionNotFound(Resources.Keys.MissingSpatioTemporalDimension_1, crs));
        }
        if (horizontalCRS != null) {
            target.getGeographicElements().add(setBounds(envelope, null, null));
        }
        if (verticalCRS != null) {
            final DefaultVerticalExtent extent = new DefaultVerticalExtent();
            setVerticalExtent(envelope, extent, crs, verticalCRS);
            target.getVerticalElements().add(extent);
        }
        if (temporalCRS != null) {
            final DefaultTemporalExtent extent = new DefaultTemporalExtent();
            setTemporalExtent(envelope, extent, crs, temporalCRS);
            target.getTemporalElements().add(extent);
        }
    }

    /**
     * Creates a two-dimensional geographic position associated to the default geographic CRS.
     * Axis order is (longitude, latitude).
     *
     * @param  λ  the longitude value.
     * @param  φ  the latitude value.
     * @return the direct position for the given geographic coordinate.
     *
     * @since 0.8
     */
    @Override
    public DirectPosition geographic(final double λ, final double φ) {
        return new DirectPosition2D(CommonCRS.defaultGeographic(), λ, φ);
    }

    /**
     * Returns an identifier for the given object, giving precedence to EPSG identifier if available.
     * The returned string should be of the form {@code "AUTHORITY:CODE"} if possible (no guarantees).
     *
     * @param  object  the object for which to get an identifier.
     * @return an identifier for the given object, with preference given to EPSG codes.
     * @throws FactoryException if an error occurred while searching for the EPSG code.
     *
     * @since 1.0
     */
    @Override
    public String getPreferredIdentifier(final IdentifiedObject object) throws FactoryException {
        final Integer code = IdentifiedObjects.lookupEPSG(object);
        if (code != null) {
            return Constants.EPSG + DefaultNameSpace.DEFAULT_SEPARATOR + code;
        }
        /*
         * If above code did not found an EPSG code, discard EPSG codes that
         * we may find in the loop below because they are probably invalid.
         */
        for (final ReferenceIdentifier id : object.getIdentifiers()) {
            if (!Constants.EPSG.equalsIgnoreCase(id.getCodeSpace())) {
                return IdentifiedObjects.toString(id);
            }
        }
        return IdentifiedObjects.getSimpleNameOrIdentifier(object);
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                          SERVICES FOR WKT FORMATTING                          ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a fully implemented parameter descriptor.
     *
     * @param  parameter  a partially implemented parameter descriptor, or {@code null}.
     * @return a fully implemented parameter descriptor, or {@code null} if the given argument was null.
     */
    @Override
    public ParameterDescriptor<?> toImplementation(final ParameterDescriptor<?> parameter) {
        return DefaultParameterDescriptor.castOrCopy(parameter);
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance.
     *
     * @param  object  the object to wrap.
     * @return the given object converted to a {@code FormattableObject} instance.
     */
    @Override
    public FormattableObject toFormattableObject(final IdentifiedObject object) {
        return AbstractIdentifiedObject.castOrCopy(object);
    }

    /**
     * Converts the given object in a {@code FormattableObject} instance. Callers should verify that the given
     * object is not already an instance of {@code FormattableObject} before to invoke this method. This method
     * returns {@code null} if it can not convert the object.
     *
     * @param  object    the object to wrap.
     * @param  internal  {@code true} if the formatting convention is {@code Convention.INTERNAL}.
     * @return the given object converted to a {@code FormattableObject} instance, or {@code null}.
     *
     * @since 0.6
     */
    @Override
    public FormattableObject toFormattableObject(final MathTransform object, boolean internal) {
        Matrix matrix;
        final ParameterValueGroup parameters;
        if (internal && (matrix = MathTransforms.getMatrix(object)) != null) {
            parameters = Affine.parameters(matrix);
        } else if (object instanceof Parameterized) {
            parameters = ((Parameterized) object).getParameterValues();
        } else {
            matrix = MathTransforms.getMatrix(object);
            if (matrix == null) {
                return null;
            }
            parameters = Affine.parameters(matrix);
        }
        return new FormattableObject() {
            @Override
            protected String formatTo(final Formatter formatter) {
                WKTUtilities.appendParamMT(parameters, formatter);
                return WKTKeywords.Param_MT;
            }
        };
    }




    ///////////////////////////////////////////////////////////////////////////////////////
    ////                                                                               ////
    ////                           SERVICES FOR WKT PARSING                            ////
    ////                                                                               ////
    ///////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns a coordinate reference system for heights above the mean seal level.
     *
     * @return the "Mean Seal Level (MSL) height" coordinate reference system.
     *
     * @since 0.6
     */
    @Override
    public VerticalCRS getMSLH() {
        return CommonCRS.Vertical.MEAN_SEA_LEVEL.crs();
    }

    /**
     * Returns the Greenwich prime meridian.
     *
     * @return the Greenwich prime meridian.
     *
     * @since 0.6
     */
    @Override
    public PrimeMeridian getGreenwich() {
        return CommonCRS.WGS84.primeMeridian();
    }

    /**
     * Returns the coordinate system of a geocentric CRS using axes in the given unit of measurement.
     *
     * @param  linearUnit  the unit of measurement for the geocentric CRS axes.
     * @return the coordinate system for a geocentric CRS with axes using the given unit of measurement.
     *
     * @since 0.6
     */
    @Override
    public CartesianCS getGeocentricCS(final Unit<Length> linearUnit) {
        return Legacy.standard(linearUnit);
    }

    /**
     * Converts a geocentric coordinate system from the legacy WKT 1 to the current ISO 19111 standard.
     * This method replaces the (Other, East, North) directions by (Geocentric X, Geocentric Y, Geocentric Z).
     *
     * @param  cs  the geocentric coordinate system to upgrade.
     * @return the upgraded coordinate system, or {@code cs} if there is no change to apply.
     *
     * @since 0.6
     */
    @Override
    public CartesianCS upgradeGeocentricCS(final CartesianCS cs) {
        return Legacy.forGeocentricCRS(cs, false);
    }

    /**
     * Creates a coordinate system of unknown type. This method is used during parsing of WKT version 1,
     * since that legacy format did not specified any information about the coordinate system in use.
     * This method should not need to be invoked for parsing WKT version 2.
     *
     * @param  properties  the coordinate system name, and optionally other properties.
     * @param  axes        the axes of the unknown coordinate system.
     * @return an "abstract" coordinate system using the given axes.
     *
     * @since 0.6
     */
    @Override
    public CoordinateSystem createAbstractCS(final Map<String,?> properties, final CoordinateSystemAxis[] axes) {
        return new AbstractCS(properties, axes);
    }

    /**
     * Creates a parametric CS. This method requires the SIS factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the coordinate system name, and optionally other properties.
     * @param  axis        the axis of the parametric coordinate system.
     * @param  factory     the factory to use for creating the coordinate system.
     * @return a parametric coordinate system using the given axes.
     * @throws FactoryException if the parametric object creation failed.
     *
     * @since 0.7
     */
    @Override
    public CoordinateSystem createParametricCS(final Map<String,?> properties, final CoordinateSystemAxis axis,
            CSFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = DefaultFactories.forBuildin(CSFactory.class, GeodeticObjectFactory.class);
        }
        return ((GeodeticObjectFactory) factory).createParametricCS(properties, axis);
    }

    /**
     * Creates a parametric datum. This method requires the SIS factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the datum name, and optionally other properties.
     * @param  factory     the factory to use for creating the datum.
     * @return a parametric datum using the given name.
     * @throws FactoryException if the parametric object creation failed.
     *
     * @since 0.7
     */
    @Override
    public Datum createParametricDatum(final Map<String,?> properties, DatumFactory factory)
            throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = DefaultFactories.forBuildin(DatumFactory.class, GeodeticObjectFactory.class);
        }
        return ((GeodeticObjectFactory) factory).createParametricDatum(properties);
    }

    /**
     * Creates a parametric CRS. This method requires the SIS factory
     * since parametric CRS were not available in GeoAPI 3.0.
     *
     * @param  properties  the coordinate reference system name, and optionally other properties.
     * @param  datum       the parametric datum.
     * @param  cs          the parametric coordinate system.
     * @param  factory     the factory to use for creating the coordinate reference system.
     * @return a parametric coordinate system using the given axes.
     * @throws FactoryException if the parametric object creation failed.
     *
     * @since 0.7
     */
    @Override
    public SingleCRS createParametricCRS(final Map<String,?> properties, final Datum datum,
            final CoordinateSystem cs, CRSFactory factory) throws FactoryException
    {
        if (!(factory instanceof GeodeticObjectFactory)) {
            factory = DefaultFactories.forBuildin(CRSFactory.class, GeodeticObjectFactory.class);
        }
        try {
            return ((GeodeticObjectFactory) factory).createParametricCRS(properties,
                    (DefaultParametricDatum) datum, (DefaultParametricCS) cs);
        } catch (ClassCastException e) {
            throw new InvalidGeodeticParameterException(e.toString(), e);
        }
    }

    /**
     * Creates a derived CRS from the information found in a WKT 1 {@code FITTED_CS} element.
     * This coordinate system can not be easily constructed from the information provided by the WKT 1 format.
     * Note that this method is needed only for WKT 1 parsing, since WKT provides enough information for using
     * the standard factories.
     *
     * @param  properties     the properties to be given to the {@code DerivedCRS} and {@code Conversion} objects.
     * @param  baseCRS        coordinate reference system to base the derived CRS on.
     * @param  method         the coordinate operation method (mandatory in all cases).
     * @param  baseToDerived  transform from positions in the base CRS to positions in this target CRS.
     * @param  derivedCS      the coordinate system for the derived CRS.
     * @return the newly created derived CRS, potentially implementing an additional CRS interface.
     *
     * @since 0.6
     */
    @Override
    public DerivedCRS createDerivedCRS(final Map<String,?>    properties,
                                       final SingleCRS        baseCRS,
                                       final OperationMethod  method,
                                       final MathTransform    baseToDerived,
                                       final CoordinateSystem derivedCS)
    {
        return DefaultDerivedCRS.create(properties, baseCRS, null, method, baseToDerived, derivedCS);
    }

    /**
     * Creates a format for {@link DirectPosition} instances.
     *
     * @param  locale    the locale for the new {@code Format}, or {@code null} for {@code Locale.ROOT}.
     * @param  timezone  the timezone, or {@code null} for UTC.
     * @return a {@link org.apache.sis.geometry.CoordinateFormat}.
     *
     * @since 0.8
     */
    @Override
    public Format createCoordinateFormat(final Locale locale, final TimeZone timezone) {
        return new CoordinateFormat(locale, timezone);
    }

    /**
     * Returns an axis direction from a pole along a meridian.
     * The given meridian is usually, but not necessarily, relative to the Greenwich meridian.
     *
     * @param  baseDirection  the base direction, which must be {@link AxisDirection#NORTH} or {@link AxisDirection#SOUTH}.
     * @param  meridian       the meridian in degrees, relative to a unspecified (usually Greenwich) prime meridian.
     *         Meridians in the East hemisphere are positive and meridians in the West hemisphere are negative.
     * @return the axis direction along the given meridian.
     *
     * @since 0.6
     */
    @Override
    public AxisDirection directionAlongMeridian(final AxisDirection baseDirection, final double meridian) {
        return CoordinateSystems.directionAlongMeridian(baseDirection, meridian);
    }

    /**
     * Creates the {@code TOWGS84} element during parsing of a WKT version 1.
     *
     * @param  values  the 7 Bursa-Wolf parameter values.
     * @return the {@link BursaWolfParameters}.
     *
     * @since 0.6
     */
    @Override
    public Object createToWGS84(final double[] values) {
        final BursaWolfParameters info = new BursaWolfParameters(CommonCRS.WGS84.datum(), null);
        info.setValues(values);
        return info;
    }

    /**
     * Creates a single operation from the given properties.
     * This method is provided here because not yet available in GeoAPI interfaces.
     *
     * @param  properties        the properties to be given to the identified object.
     * @param  sourceCRS         the source CRS.
     * @param  targetCRS         the target CRS.
     * @param  interpolationCRS  the CRS of additional coordinates needed for the operation, or {@code null} if none.
     * @param  method            the coordinate operation method (mandatory in all cases).
     * @param  factory           the factory to use.
     * @return the coordinate operation created from the given arguments.
     * @throws FactoryException if the object creation failed.
     *
     * @since 0.6
     */
    @Override
    public SingleOperation createSingleOperation(
            final Map<String,?>              properties,
            final CoordinateReferenceSystem  sourceCRS,
            final CoordinateReferenceSystem  targetCRS,
            final CoordinateReferenceSystem  interpolationCRS,
            final OperationMethod            method,
            final CoordinateOperationFactory factory) throws FactoryException
    {
        final DefaultCoordinateOperationFactory df;
        if (factory instanceof DefaultCoordinateOperationFactory) {
            df = (DefaultCoordinateOperationFactory) factory;
        } else {
            df = CoordinateOperations.factory();
        }
        return df.createSingleOperation(properties, sourceCRS, targetCRS, interpolationCRS, method, null);
    }

    /**
     * Returns the coordinate operation factory to use for the given properties and math transform factory.
     * If the given properties are empty and the {@code mtFactory} is the system default, then this method
     * returns the system default {@code CoordinateOperationFactory} instead of creating a new one.
     *
     * <p>It is okay to set all parameters to {@code null} in order to get the system default factory.</p>
     *
     * @param  properties  the default properties.
     * @param  mtFactory   the math transform factory to use.
     * @param  crsFactory  the factory to use if the operation factory needs to create CRS for intermediate steps.
     * @param  csFactory   the factory to use if the operation factory needs to create CS for intermediate steps.
     * @return the coordinate operation factory to use.
     *
     * @since 0.7
     */
    @Override
    public CoordinateOperationFactory getCoordinateOperationFactory(Map<String,?> properties,
            final MathTransformFactory mtFactory, final CRSFactory crsFactory, final CSFactory csFactory)
    {
        if (Containers.isNullOrEmpty(properties)) {
            if (DefaultFactories.isDefaultInstance(MathTransformFactory.class, mtFactory) &&
                DefaultFactories.isDefaultInstance(CRSFactory.class, crsFactory) &&
                DefaultFactories.isDefaultInstance(CSFactory.class, csFactory))
            {
                return CoordinateOperations.factory();
            }
            properties = Collections.emptyMap();
        }
        final HashMap<String,Object> p = new HashMap<>(properties);
        p.putIfAbsent(CRS_FACTORY, crsFactory);
        p.putIfAbsent(CS_FACTORY,  csFactory);
        properties = p;
        return new DefaultCoordinateOperationFactory(properties, mtFactory);
    }

    /**
     * Returns the properties of the given object.
     *
     * @param  object  the object from which to get the properties.
     * @param  keepId  {@code true} for preserving the identifiers, {@code false} for discarding them.
     * @return the properties of the given object.
     *
     * @since 0.6
     */
    @Override
    public Map<String,?> getProperties(final IdentifiedObject object, final boolean keepId) {
        return IdentifiedObjects.getProperties(object, keepId ? CharSequences.EMPTY_ARRAY
                : new String[] {IdentifiedObject.IDENTIFIERS_KEY});
    }

    /**
     * Returns {@code true} if the {@linkplain AbstractIdentifiedObject#getName() primary name} or an aliases
     * of the given object matches the given name.
     *
     * @param  object  the object for which to check the name or alias.
     * @param  name    the name to compare with the object name or aliases.
     * @return {@code true} if the primary name of at least one alias matches the specified {@code name}.
     *
     * @since 0.6
     */
    @Override
    public boolean isHeuristicMatchForName(final IdentifiedObject object, final String name) {
        return IdentifiedObjects.isHeuristicMatchForName(object, name);
    }

    /**
     * Returns the coordinate operation method for the given classification.
     * This method checks if the given {@code opFactory} is a SIS implementation
     * before to fallback on a slower fallback.
     *
     * @param  opFactory  The coordinate operation factory to use if it is a SIS implementation.
     * @param  mtFactory  The math transform factory to use as a fallback.
     * @param  identifier The name or identifier of the operation method to search.
     * @return The coordinate operation method for the given name or identifier.
     * @throws FactoryException if an error occurred which searching for the given method.
     *
     * @since 0.6
     */
    @Override
    public OperationMethod getOperationMethod(final CoordinateOperationFactory opFactory,
            final MathTransformFactory mtFactory, final String identifier) throws FactoryException
    {
        if (opFactory instanceof DefaultCoordinateOperationFactory) {
            ((DefaultCoordinateOperationFactory) opFactory).getOperationMethod(identifier);
        }
        return super.getOperationMethod(opFactory, mtFactory, identifier);
    }

    /**
     * Returns information about the Apache SIS configuration.
     * See super-class for a list of keys.
     *
     * @param  key     a key identifying the information to return.
     * @param  locale  language to use if possible.
     * @return the information, or {@code null} if none.
     */
    @Override
    public String getInformation(final String key, final Locale locale) {
        switch (key) {
            /*
             * Get the version of the EPSG database and the version of the database software.
             * This operation can be relatively costly as it may open a JDBC connection.
             */
            case Constants.EPSG: {
                final Citation authority;
                try {
                    authority = CRS.getAuthorityFactory(Constants.EPSG).getAuthority();
                } catch (FactoryException e) {
                    final String msg = Exceptions.getLocalizedMessage(e, locale);
                    return (msg != null) ? msg : e.toString();
                }
                if (authority instanceof DefaultCitation) {
                    final OnLineFunction f = OnLineFunction.valueOf(CONNECTION);
                    for (final OnlineResource res : ((DefaultCitation) authority).getOnlineResources()) {
                        if (f.equals(res.getFunction())) {
                            final InternationalString i18n = res.getDescription();
                            if (i18n != null) return i18n.toString(locale);
                        }
                    }
                    final InternationalString i18n = authority.getTitle();
                    if (i18n != null) return i18n.toString(locale);
                }
                return Vocabulary.getResources(locale).getString(Vocabulary.Keys.Untitled);
            }
            // More cases may be added in future SIS versions.
        }
        return super.getInformation(key, locale);
    }
}
