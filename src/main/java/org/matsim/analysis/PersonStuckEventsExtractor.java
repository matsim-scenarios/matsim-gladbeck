package org.matsim.analysis;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Extract all PersonStuckEvents from a MATSim events file
 * and write them to a CSV.
 */
@CommandLine.Command(
        name = "person-stuck-events",
        description = "Extract PersonStuckEvents from a MATSim events file and write to CSV."
)
public class PersonStuckEventsExtractor implements MATSimAppCommand {

    @CommandLine.Option(names = "--events-file", description = "Path to the MATSim events file", required = true)
    private String eventsFile;

    @CommandLine.Option(names = "--output-csv", description = "Path for the output CSV file", required = true)
    private String outputCsvFile;

    public static void main(String[] args) {
        new PersonStuckEventsExtractor().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        var eventsManager = EventsUtils.createEventsManager();
        var handler = new PersonStuckEventCollector();
        eventsManager.addHandler(handler);

        new MatsimEventsReader(eventsManager).readFile(eventsFile);

        writeCsv(handler.getEvents(), outputCsvFile);

        System.out.println("✅ Found " + handler.getEvents().size() + " PersonStuckEvents.");
        return 0;
    }

    private static void writeCsv(List<PersonStuckEvent> events, String outputCsvFile) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputCsvFile))) {
            writer.write("time,person,link,legMode\n");
            for (PersonStuckEvent e : events) {
                writer.write(String.join(",",
                        String.valueOf(e.getTime()),
                        e.getPersonId().toString(),
                        e.getLinkId().toString(),
                        e.getLegMode()
                ));
                writer.newLine();
            }
        }
    }

    /**
     * Collects all PersonStuckEvents from the stream.
     */
    private static class PersonStuckEventCollector implements PersonStuckEventHandler {
        private final List<PersonStuckEvent> events = new ArrayList<>();

        @Override
        public void handleEvent(PersonStuckEvent event) {
            events.add(event);
        }

        public List<PersonStuckEvent> getEvents() {
            return events;
        }
    }
}