/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.analysis;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class LinksInShp {

    private static final Logger log = LogManager.getLogger(LinksInShp.class);

    public static void main(String[] args) {
        String gladbeckShape = "../../shared-svn/projects/GlaMoBi/data/shp-files/Gladbeck.shp";
        List<PreparedGeometry> gladbeckGeoms = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(gladbeckShape));
        Network ruhrgebietNetwork = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.0.network_resolutionHigh-with-pt.xml.gz");
        writeLinksInToNetwork(ruhrgebietNetwork, gladbeckGeoms, "../../shared-svn/projects/GlaMoBi/networkLinksWithinGladbeck.xml.gz");
    }

    static void writeLinksInToNetwork(Network network, List<PreparedGeometry> preparedGeometryList, String outputFilePath){
        Set<Id<Link>> linkIds = new HashSet<>();
        log.info("Parsing links");
        for (Link link : network.getLinks().values()) {
            if (ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), preparedGeometryList)) {
                linkIds.add(link.getId());
            }
        }

        for (Link link: network.getLinks().values()) {
            if (linkIds.contains(link.getId())) {
                //keep link
                log.info("Keep Link "+link.getId()+"because in shp File");
            }
            else {
                network.removeLink(link.getId());
            }
        }
        NetworkUtils.writeNetwork(network, outputFilePath);
    }



}
