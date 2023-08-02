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

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVParser;
import com.opencsv.validators.LineValidatorAggregator;
import com.opencsv.validators.RowValidatorAggregator;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opencsv.CSVReader.*;
import static com.opencsv.ICSVWriter.*;

public class LinksInGladbeck {

    public static void main(String[] args) {

        String gladbeckShape = "../../shared-svn/projects/GlaMoBi/data/shp-files/Gladbeck.shp";

        List<PreparedGeometry> gladbeckGeoms = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(gladbeckShape));
        Network gladbeckNetwork = NetworkUtils.readNetwork("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/input/gladbeck-v1.0-10pct.network.xml.gz");
        writeLinksInShpCSV(gladbeckNetwork, gladbeckGeoms, "../../shared-svn/projects/GlaMoBi/networkLinksWithinGladbeckWithAttributes.tsv");
        //writeTempo30Zones(gladbeckNetwork, gladbeckGeoms,"../../shared-svn/projects/GlaMoBi/tempo30WithinGladbeck.tsv");
    }

    static void writeLinksInShpCSV(Network network, List<PreparedGeometry> preparedGeometryList, String outputFilePath){
        if(! outputFilePath.endsWith(".tsv")) throw new IllegalArgumentException("output file path should end with .tsv");

        Set<String[]> linkIds = network.getLinks().values().stream()
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), preparedGeometryList))
                .map(link -> new String[]{link.getId().toString()+","+link.getAttributes().getAttribute("smoothness").toString()
                +","+link.getAttributes().getAttribute("surface")})
                .collect(Collectors.toSet());

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath), '\t', NO_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
            writer.writeNext(new String[]{"linkId"});
            writer.writeAll(linkIds);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void writeTempo30Zones(Network network, List<PreparedGeometry> geometries, String outputFilePath) {
        Set<String[]> linkIds = new HashSet<>();

        Set<? extends Link> carLinksInArea = network.getLinks().values().stream()
                .filter(link -> link.getAllowedModes().contains(TransportMode.car)) //filter car links
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), geometries)) //spatial filter
                .filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("motorway"))//we won't change motorways and motorway_links
                .filter(link -> !((String) link.getAttributes().getAttribute("type")).contains("trunk"))
                .collect(Collectors.toSet());


        carLinksInArea.forEach(link -> {

            //TODO Check what free speed for 30km/h streets is used
            if (!link.getAttributes().getAttribute("type").equals("primary")) {
                //apply 'tempo 30' to all roads but primary and motorways
                if (link.getFreespeed() > 7.5)  {
                    link.setFreespeed(7.5); //27 km/h is used in the net for 30 km/h
                   linkIds.add(new String[] {link.getId().toString()});
                }
            }
        });

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath), '\t', NO_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
            writer.writeNext(new String[]{"linkId"});
            writer.writeAll((Iterable<String[]>) linkIds.stream().toList());
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}