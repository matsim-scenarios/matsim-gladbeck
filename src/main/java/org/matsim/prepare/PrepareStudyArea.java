package org.matsim.prepare;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.MultiPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.gis.PolygonFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PrepareStudyArea {

    private static final Path studyAreaShp =
            Paths.get("/Users/gregorr/Documents/work/respos/shared-svn/projects/GlaMoBi/data/shp-files/shapeFilesUntersuchungsgebiet/dvg2krs_gladbeck-bottrop-gelsenkirchen/merged.shp");
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PrepareStudyArea.class);

    public static void main (String args []) {


        Collection<SimpleFeature> features = ShapeFileReader.getAllFeatures(studyAreaShp.toString());

        for (SimpleFeature feature: features) {
            MultiPolygon geometry = (MultiPolygon) feature.getDefaultGeometry();
            Coordinate[] geometry2 = geometry.getBoundary().getCoordinates();
            ArrayList<Double> xCoords = new ArrayList<>();
            ArrayList<Double> yCoords = new ArrayList<>();

            for (Coordinate coordinate : geometry2) {
                xCoords.add(coordinate.getX());
                yCoords.add(coordinate.getY());
            }

            Collections.sort(xCoords);
            Collections.sort(yCoords);
            Coord coordBottomLeft = new Coord(xCoords.get(0), yCoords.get(0));
            Coord coordTopLeft = new Coord(xCoords.get(0), yCoords.get(yCoords.size()-1));
            Coord coordBottomRight = new Coord(xCoords.get(xCoords.size()-1), yCoords.get(0));
            Coord coordTopRight = new Coord(xCoords.get(xCoords.size()-1), yCoords.get(yCoords.size()-1));
            log.info("Writing shape file...");

            PolygonFeatureFactory.Builder featureFactoryBuilder = new PolygonFeatureFactory.Builder();
            featureFactoryBuilder.setName("StudyArea");
            PolygonFeatureFactory featureFactory = featureFactoryBuilder.create();
            Collection<SimpleFeature> featuresToWriteOut = new ArrayList<>();
            Map<String, Object> attributeValues = new HashMap<>();
            attributeValues.put("ID", "studyArea");

            Coordinate[] coordinates = new Coordinate[4];
            coordinates[0] =  new Coordinate(coordBottomLeft.getX(), coordBottomRight.getY());
            coordinates[1] =  new Coordinate(coordBottomRight.getX(), coordBottomRight.getY());
            coordinates[2] =  new Coordinate(coordTopRight.getX(), coordTopRight.getY());
            coordinates[3] =  new Coordinate(coordTopLeft.getX(), coordTopLeft.getY());

            SimpleFeature feature3 = featureFactory.createPolygon(coordinates, attributeValues, Integer.toString(1));
            featuresToWriteOut.add(feature3);

            ShapeFileWriter.writeGeometries(featuresToWriteOut, "/Users/gregorr/Desktop/Test_ScenarioCutOut/test.shp");
            log.info("Writing shape file... Done.");

        }


    }

}
