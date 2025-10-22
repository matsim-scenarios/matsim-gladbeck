package org.matsim.analysis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.events.*;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import picocli.CommandLine;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


@CommandLine.Command(
        name = "emissions-per-vehicle-csv",
        description = "Generates a CSV file summarizing emissions per vehicle from emission events.",
        mixinStandardHelpOptions = true
)

public class EmissionsPerVehicleCSV implements MATSimAppCommand {

    @CommandLine.Option(names = "--events-file", description = "Path to the emission events file", required = true)
    private String eventsFile;
    @CommandLine.Option(names = "--output-csv", description = "Path for output CSV file", required = true)
    private String outputCsvFile;

    private static final Logger log = LogManager.getLogger(EmissionsPerVehicleCSV.class);


    public static void main(String[] args) {
       new EmissionsPerVehicleCSV().execute(args);
    }

    private static void writeCsv(Map<Id, Map<Pollutant, Double>> emissions, String outputCsvFile) {
        try (FileWriter writer = new FileWriter(outputCsvFile)) {
            Set<Pollutant> allPollutants = new TreeSet<>();
            for (Map<Pollutant, Double> map : emissions.values()) {
                allPollutants.addAll(map.keySet());
            }

            writer.append("VehicleId");
            for (Pollutant p : allPollutants) {
                writer.append(",").append(p.toString());
            }
            writer.append("\n");

            for (Map.Entry<Id, Map<Pollutant, Double>> entry : emissions.entrySet()) {
                writer.append(entry.getKey().toString());
                Map<Pollutant, Double> vehicleEmissions = entry.getValue();
                for (Pollutant p : allPollutants) {
                    Double value = vehicleEmissions.getOrDefault(p, 0.0);
                    writer.append(",").append(String.valueOf(value));
                }
                writer.append("\n");
            }

            log.info("✅ CSV successfully written to " + outputCsvFile);
            log.info("Vehicles processed: " + emissions.size());

        } catch (IOException e) {
            log.error("❌ Error writing CSV", e);
        }
    }

    @Override
    public Integer call() throws Exception {


        EventsManager events = EventsUtils.createEventsManager();

        final EmissionsPerVehicleHandler handler = new EmissionsPerVehicleHandler();
        events.addHandler(handler);

        new EmissionEventsReader(events).readFile(eventsFile);

        writeCsv(handler.getEmissionsPerVehicle(), outputCsvFile);

        return 0;
    }

    static class EmissionsPerVehicleHandler implements WarmEmissionEventHandler, ColdEmissionEventHandler {

        private final Map<Id, Map<Pollutant, Double>> emissionsPerVehicle = new HashMap<>();

        @Override
        public void handleEvent(WarmEmissionEvent event) {
            mergeEmissions(event.getVehicleId(), event.getWarmEmissions());
        }

        @Override
        public void handleEvent(ColdEmissionEvent event) {
            mergeEmissions(event.getVehicleId(), event.getColdEmissions());
        }

        private void mergeEmissions(Id vehicleId, Map<Pollutant, Double> eventEmissions) {
            emissionsPerVehicle.merge(vehicleId, new HashMap<>(eventEmissions), (existing, incoming) -> {
                for (Map.Entry<Pollutant, Double> e : incoming.entrySet()) {
                    existing.merge(e.getKey(), e.getValue(), Double::sum);
                }
                return existing;
            });
        }

        public Map<Id, Map<Pollutant, Double>> getEmissionsPerVehicle() {
            return emissionsPerVehicle;
        }

        @Override
        public void reset(int iteration) {
            emissionsPerVehicle.clear();
        }

    }
}