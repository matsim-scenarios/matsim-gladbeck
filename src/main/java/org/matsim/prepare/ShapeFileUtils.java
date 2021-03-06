package org.matsim.prepare;

/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */


import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.UncheckedIOException;
import org.opengis.feature.simple.SimpleFeature;

/**
 * @author ikaddoura
 */

public final class ShapeFileUtils {

    private Map<Integer, Geometry> areaGeometries;

    public ShapeFileUtils(String areaShapeFile) {
        if (areaShapeFile != null && areaShapeFile != "" && areaShapeFile != "null" ) {
            this.areaGeometries = loadShapeFile(areaShapeFile);
        }
    }

    private Map<Integer, Geometry> loadShapeFile(String shapeFile) {
        Map<Integer, Geometry> geometries = new HashMap<>();

        Collection<SimpleFeature> features = null;
        if (!shapeFile.startsWith("http")) {
            features = ShapeFileReader.getAllFeatures(shapeFile);
        } else {
            try {
                features = getAllFeatures(new URL(shapeFile));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        if (features == null) throw new RuntimeException("Aborting...");
        int featureCounter = 0;
        for (SimpleFeature feature : features) {
            geometries.put(featureCounter, (Geometry) feature.getDefaultGeometry());
            featureCounter++;
        }
        return geometries;
    }

    public boolean isCoordInArea(Coord coord) {
        return isCoordInArea(coord, areaGeometries, 0.);
    }

    public boolean isCoordInArea(Coord coord, double buffer) {
        return isCoordInArea(coord, areaGeometries, buffer);
    }

    private boolean isCoordInArea(Coord coord, Map<Integer, Geometry> areaGeometries, double buffer) {
        boolean coordInArea = false;
        for (Geometry geometry : areaGeometries.values()) {
            Point p = MGC.coord2Point(coord);

            if (buffer == 0.) {
                if (p.within(geometry)) {
                    coordInArea = true;
                    break;
                }
            } else {
                if (p.isWithinDistance(geometry, buffer)) {
                    coordInArea = true;
                    break;
                }
            }
        }
        return coordInArea;
    }

    public boolean isLineInArea(Coord coord1, Coord coord2, double buffer) {
        return isLineInArea(coord1, coord2, areaGeometries, buffer);
    }

    private boolean isLineInArea(Coord coord1, Coord coord2, Map<Integer, Geometry> areaGeometries, double buffer) {
        boolean lineInArea = false;
        for (Geometry geometry : areaGeometries.values()) {
            Coordinate p1 = MGC.coord2Coordinate(coord1);
            Coordinate p2 = MGC.coord2Coordinate(coord2);
            Coordinate[] coordinates = {p1, p2};
            CoordinateSequence points = new CoordinateArraySequence(coordinates);
            LineString line = new LineString(points, new GeometryFactory());

            if (buffer == 0.) {
                if (line.within(geometry)) {
                    lineInArea = true;
                    break;
                }
            } else {
                if (line.isWithinDistance(geometry, buffer)) {
                    lineInArea = true;
                    break;
                }
            }
        }
        return lineInArea;
    }

    static Collection<SimpleFeature> getAllFeatures(final URL url) {
        try {
            FileDataStore store = FileDataStoreFinder.getDataStore(url);
            SimpleFeatureSource featureSource = store.getFeatureSource();

            SimpleFeatureIterator it = featureSource.getFeatures().features();
            List<SimpleFeature> featureSet = new ArrayList<SimpleFeature>();
            while (it.hasNext()) {
                SimpleFeature ft = it.next();
                featureSet.add(ft);
            }
            it.close();
            store.dispose();
            return featureSet;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public Point getRandomPointInArea(Random random) {
        return getRandomPointInFeature(random, areaGeometries.get(random.nextInt(areaGeometries.size())));
    }

    private static Point getRandomPointInFeature(Random rnd, Geometry g)
    {
        Point p = null;
        double x, y;
        do {
            x = g.getEnvelopeInternal().getMinX() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxX() - g.getEnvelopeInternal().getMinX());
            y = g.getEnvelopeInternal().getMinY() + rnd.nextDouble()
                    * (g.getEnvelopeInternal().getMaxY() - g.getEnvelopeInternal().getMinY());
            p = MGC.xy2Point(x, y);
        }
        while (!g.contains(p));
        return p;
    }

}
