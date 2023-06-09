package org.matsim.prepare;

import com.opencsv.CSVWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.opencsv.ICSVWriter.*;

public class LinksInShp {

    private final String shpSource;
    private final String networkSource;
    private String linksInAreaCsv = null;

    public LinksInShp(String networkFile, String shpFile){
        this.shpSource = shpFile;
        this.networkSource = networkFile;
    }

    public LinksInShp(String networkFile, String shpFile, String outputCsv){
        shpSource = shpFile;
        networkSource = networkFile;
        linksInAreaCsv = outputCsv;
    }

    private static final Logger log = LogManager.getLogger(LinksInShp.class);
/*
    @CommandLine.Parameters(paramLabel = "INPUT-NETWORK", description = "Input network file", arity = "1")
    private String networkSource;

    @CommandLine.Parameters(paramLabel = "INPUT-SHP", description = "Input shp file", arity = "2")
    private String shpSource;

    @CommandLine.Option(names = "--writeToCsv", description = "Optional output to csv file path")
    private String person;

    @CommandLine.Option(names = "--mode", description = "Optional filter by allowed transport mode ")
    private String type;
*/

    public static void main(String[] args) {
      String shpSource = "C:/Users/djp/Desktop/TUB/VSP/glamobi/gladbeck_schulstr_reduced/gladbeck_schulstr_reduced.shp";
      String networkSource = "C:/Users/djp/Desktop/TUB/VSP/glamobi/bc-continued/baseCaseContinued.output_network.xml.gz";
      String linksInAreaCsv = "C:/Users/djp/Desktop/TUB/VSP/glamobi/gladbeck_schulstr_shp/gladbeck_schulstr_links_car-test-2.csv";
//      linksInAreaCsv = null;

//    public List<Id<Link>> getLinkIds(){

        List<PreparedGeometry> shpArea = ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shpSource));
        Network network = NetworkUtils.readNetwork(networkSource);
        log.info(shpArea.size()+" shapes found.");

        List<Id<Link>> linkIds = network.getLinks().values().stream()
//                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getCoord(), shpArea))
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getFromNode().getCoord(), shpArea))
                .filter(link -> ShpGeometryUtils.isCoordInPreparedGeometries(link.getToNode().getCoord(), shpArea))
                .filter(link -> link.getAllowedModes().contains("car"))
                .map(link -> link.getId())
                .collect(Collectors.toList());

        if(linksInAreaCsv!=null) writeLinksInShpCSV(linkIds, linksInAreaCsv.toString());

        log.info("Done.");

//        return linkIds;
    }

    static void writeLinksInShpCSV(List<Id<Link>> linkIds, String outputFilePath){
        if(! outputFilePath.endsWith(".csv")) throw new IllegalArgumentException("output file path should end with .csv");

        Set<String[]> setLinkIds = linkIds.stream()
                .map(linkId -> new String[]{linkId.toString()})
                .collect(Collectors.toSet());

        log.info("Writing linkIds to csv output file");

        try {
            CSVWriter writer = new CSVWriter(new FileWriter(outputFilePath), '\t', NO_QUOTE_CHARACTER, DEFAULT_ESCAPE_CHARACTER, DEFAULT_LINE_END);
            writer.writeNext(new String[]{"linkId"});
            writer.writeAll(setLinkIds);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.info(linkIds.size()+" links written to "+outputFilePath);
    }

}
