package org.matsim.run.policies;

import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ReduceSpeed {

    public static void implementPushMeasuresByModifyingNetworkInArea(Network network, List<PreparedGeometry> geometries) {
        Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.car)) //filter car links
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries)) //spatial filter
                .filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))//we won't change motorways and motorway_links
                .filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("trunk"))
                .filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("primary"))
                .collect(Collectors.toSet());


        carLinksInArea.forEach(link -> {
            //apply 'tempo 30' to all roads but primary and motorways
            link.setFreespeed(link.getFreespeed() * 0.6); //27 km/h is used in the net for 30 km/h streets
        });
    }

    public static void implementSpeedReductionOnPreDefinedLinks (Network network, List<PreparedGeometry> shpWithPreDefinedStreets) {
        Set<? extends Link> carLinks = network.getLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.car)) //filter car links
                .filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("residential"))
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), shpWithPreDefinedStreets)) //spatial filter
                .collect(Collectors.toSet());

        carLinks.forEach(link -> {
            //apply 'tempo 30' to all roads witin the shape file
            link.setFreespeed(link.getFreespeed() * 0.6); //27 km/h is used in the net for 30 km/h streets
        });
    }

}
