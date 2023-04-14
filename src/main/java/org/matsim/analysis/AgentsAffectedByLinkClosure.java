package org.matsim.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import static org.matsim.application.ApplicationUtils.globFile;

public class AgentsAffectedByLinkClosure implements MATSimAppCommand {

    @CommandLine.Option(names = "--directory", description = "path to matsim output directory", required = true)
    private Path directory;

    public static void main (String args []) {
        new AgentsAffectedByLinkClosure().execute(args);
    }

    @Override
    public Integer call() throws Exception {

        Path eventsPath = globFile(directory, "*output_events.*");
        List<Id<Vehicle>> agentsThatDroveOverLink = new ArrayList<>();
        List<Id<Link>> listOfLinks = new ArrayList<>();


        //TODO change to shape File approach
        listOfLinks.add(Id.createLinkId("5156341260014r"));
        listOfLinks.add(Id.createLinkId("5156341260014f"));
        listOfLinks.add(Id.createLinkId("380432140001r"));
        listOfLinks.add(Id.createLinkId("380432140001f"));
        listOfLinks.add(Id.createLinkId("381870670005f"));
        listOfLinks.add(Id.createLinkId("381870670005r"));
        listOfLinks.add(Id.createLinkId("353353090002f"));
        listOfLinks.add(Id.createLinkId("353353090002r"));

        //  werner von siemens schule gladbeck
        listOfLinks.add(Id.createLinkId("358770500002f"));
        listOfLinks.add(Id.createLinkId("358770500002r"));
        listOfLinks.add(Id.createLinkId("358770510002r"));
        listOfLinks.add(Id.createLinkId("358770510002r"));
        listOfLinks.add(Id.createLinkId("358770510002f"));
        listOfLinks.add(Id.createLinkId("1157881300007f"));
        listOfLinks.add(Id.createLinkId("1157881300007r"));
        listOfLinks.add(Id.createLinkId("1157881300007r"));
        listOfLinks.add(Id.createLinkId("1157881300007r"));
        listOfLinks.add(Id.createLinkId("481471120002f"));
        listOfLinks.add(Id.createLinkId("481471120002r"));

        EventsManager manager = EventsUtils.createEventsManager();
        manager.addHandler(new AgentsThatDroveOverLink(agentsThatDroveOverLink, listOfLinks));
        manager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(manager);
        matsimEventsReader.readFile(eventsPath.toString());
        manager.finishProcessing();
        writeResults(directory, agentsThatDroveOverLink);
        return null;
    }

    private static class AgentsThatDroveOverLink implements LinkEnterEventHandler {

        private final List<Id<Vehicle>> agentsThatDroveOverLink;
        private final List<Id<Link>> listOfLinks;

        AgentsThatDroveOverLink(List agentsThatDroveOverLink, List listOfLinks) {
            this.agentsThatDroveOverLink = agentsThatDroveOverLink;
            this.listOfLinks = listOfLinks;
        }


        @Override
        public void handleEvent(LinkEnterEvent linkEnterEvent) {
            if (listOfLinks.contains(linkEnterEvent.getLinkId()) && !agentsThatDroveOverLink.contains(linkEnterEvent.getVehicleId())) {
                if (!linkEnterEvent.getVehicleId().toString().contains("transit") && !linkEnterEvent.getVehicleId().toString().contains("bike")) {
                    agentsThatDroveOverLink.add(linkEnterEvent.getVehicleId());
                }
            }
        }
    }

    private static void writeResults(Path outputFolder, List<Id<Vehicle>> listOfIds) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter(outputFolder.toString() + "/agentsAffectedByLinkModification.tsv");

        writer.write("Id");
        writer.newLine();
        for (int i = 0; i < listOfIds.size(); i++) {
            // split String to get person Id from vehicle Id
            String[] splitString = listOfIds.get(i).toString().split("_");
            writer.write( splitString[0]);
            // writer.write(listOfIds.get(i).toString());
            writer.newLine();
        }
        writer.close();
    }

}
