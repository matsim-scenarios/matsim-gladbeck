package org.matsim.analysis;

import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.CommandSpec;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.avro.XYTData;
import org.matsim.application.options.InputOptions;
import org.matsim.application.options.OutputOptions;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.analysis.time.TimeBinMap;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.Pollutant;
import org.matsim.contrib.emissions.analysis.EmissionsByVehicleTypeEventHandler;
import org.matsim.contrib.emissions.analysis.EmissionsOnLinkEventHandler;
import org.matsim.contrib.emissions.analysis.FastEmissionGridAnalyzer;
import org.matsim.contrib.emissions.analysis.Raster;
import org.matsim.contrib.emissions.events.ColdEmissionEvent;
import org.matsim.contrib.emissions.events.ColdEmissionEventHandler;
import org.matsim.contrib.emissions.events.WarmEmissionEvent;
import org.matsim.contrib.emissions.events.WarmEmissionEventHandler;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.filter.NetworkFilterManager;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.*;

import static org.matsim.analysis.GladbeckDashboardsRunner.prepareVehicleTypesForEmissionAnalysis;
import static org.matsim.analysis.GladbeckDashboardsRunner.setEmissionsConfigs;

@CommandLine.Command(
        name = "air-pollution", description = "General air pollution analysis.",
        mixinStandardHelpOptions = true, showDefaultValues = true
)
@CommandSpec(requireRunDirectory = true,
        produces = {
                "emissions_total.csv", "emissions_per_link.csv",
                "emissions_per_link_per_m.csv",
                "emissions_grid_per_hour.%s",
                "emissions_vehicle_info.csv",
                "emissions_grid_per_day.%s",
                "emissions_per_vehicle_type.csv",
                "emissions_per_network_mode.csv"
        }
)
public class EmissionsPerVehicle implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(EmissionsPerVehicle.class);

    @CommandLine.Mixin
    private final InputOptions input = InputOptions.ofCommand(EmissionsPerVehicle.class);
    @CommandLine.Mixin
    private final OutputOptions output = OutputOptions.ofCommand(EmissionsPerVehicle.class);

    @CommandLine.Mixin
    private final ShpOptions shp = new ShpOptions();

    @CommandLine.Mixin
    private SampleOptions sample;

    @CommandLine.Option(names = "--grid-size", description = "Grid size in meter", defaultValue = "100")
    private double gridSize;

    public static void main(String[] args) {
        new EmissionsPerVehicle().execute(args);
    }

    @Override
    public Integer call() throws Exception {

        Config config = prepareConfig();
        config.vehicles().setVehiclesFile("/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/projects/output/v3.0/base-case-continued/output_gladbeck-v3.0-3pct.output_vehicles.xml.gz");
        setEmissionsConfigs(config);
        config.network().setChangeEventsInputFile("/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/projects/output/v3.0/base-case-continued/output_gladbeck-v3.0-3pct.output_networkChangeEvents.xml.gz");


        //EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        prepareVehicleTypesForEmissionAnalysis(scenario);

        //set track or footway manually to path as it is not included in HBEFA mapping
        for (Link link : scenario.getNetwork().getLinks().values()) {
            String type = (String) link.getAttributes().getAttribute("type");
            if (type != null && type.equals("track") || type != null && type.equals("footway")) {
                link.getAttributes().putAttribute("type", "path");
            }
        }

        HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
        roadTypeMapping.addHbefaMappings(scenario.getNetwork());


        Network filteredNetwork;
        if (shp.isDefined()) {
            ShpOptions.Index index = shp.createIndex(ProjectionUtils.getCRS(scenario.getNetwork()), "_");

            NetworkFilterManager manager = new NetworkFilterManager(scenario.getNetwork(), config.network());
            manager.addLinkFilter(l -> index.contains(l.getCoord()));

            filteredNetwork = manager.applyFilters();
        } else
            filteredNetwork = scenario.getNetwork();

        EventsManager eventsManager = EventsUtils.createEventsManager();
        AbstractModule module = new AbstractModule() {
            @Override
            public void install() {
                bind(Scenario.class).toInstance(scenario);
                bind(EventsManager.class).toInstance(eventsManager);
                bind(EmissionModule.class);
            }
        };


        com.google.inject.Injector injector = Injector.createInjector(config, module);

        // Emissions module will be installed to the event handler
        injector.getInstance(EmissionModule.class);

        //String eventsFile = ApplicationUtils.matchInput("events", input.getRunDirectory()).toString();
        String eventsFile = "/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/projects/output/v3.0/klima-taler-1EUR/output_gladbeck-v3.0-3pct.output_events.xml.gz";


        EmissionsOnLinkEventHandler emissionsEventHandler = new EmissionsOnLinkEventHandler(3600, 86400);
        EmissionsByVehicleTypeEventHandler emissionsByVehicleType = new EmissionsByVehicleTypeEventHandler(scenario.getVehicles(), filteredNetwork);
        EmissionsByVehicleHandler emissionsPerVehicle = new EmissionsByVehicleHandler();

        eventsManager.addHandler(emissionsPerVehicle);
        eventsManager.addHandler(emissionsEventHandler);
        eventsManager.addHandler(emissionsByVehicleType);

        eventsManager.initProcessing();
        MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
        matsimEventsReader.readFile(eventsFile);

        log.info("Done reading the events file.");
        log.info("Finish processing...");
        eventsManager.finishProcessing();



        //writeOutput(filteredNetwork, emissionsEventHandler);

        writeTotal(filteredNetwork, emissionsEventHandler);
        writeEmissionsByNetworkMode(emissionsByVehicleType);
        writeEmissionsPerVehicle(emissionsPerVehicle);


        return 0;
    }

    private Config prepareConfig() {
        Config config = ConfigUtils.loadConfig(ApplicationUtils.matchInput("config.xml", input.getRunDirectory()).toAbsolutePath().toString());

        config.vehicles().setVehiclesFile(ApplicationUtils.matchInput("vehicles", input.getRunDirectory()).toAbsolutePath().toString());
        config.network().setInputFile(ApplicationUtils.matchInput("network", input.getRunDirectory()).toAbsolutePath().toString());
        config.transit().setTransitScheduleFile(ApplicationUtils.matchInput("transitSchedule", input.getRunDirectory()).toAbsolutePath().toString());
        config.transit().setVehiclesFile(ApplicationUtils.matchInput("transitVehicles", input.getRunDirectory()).toAbsolutePath().toString());
        config.plans().setInputFile(null);
        config.facilities().setInputFile(null);
        config.eventsManager().setNumberOfThreads(null);
        config.eventsManager().setEstimatedNumberOfEvents(null);
        config.global().setNumberOfThreads(1);

        return config;
    }

    private void writeEmissionsByNetworkMode(EmissionsByVehicleTypeEventHandler emissionsByVehicleType) throws IOException {

        log.info("Writing emissions by vehicle type...");
        Map<String, Object2DoubleMap<Pollutant>> pollutants = emissionsByVehicleType.getByNetworkMode();

        CSVPrinter emissionsCSV = new CSVPrinter(Files.newBufferedWriter(Path.of("/Users/gregorr/Documents/work/stuff/analysisGlaMoBi/forTrainWork/test/emissions_per_network_mode.csv")), CSVFormat.DEFAULT);

        emissionsCSV.print("vehicleType");
        emissionsCSV.print("pollutant");
        emissionsCSV.print("value");
        emissionsCSV.println();

        for (Map.Entry<String, Object2DoubleMap<Pollutant>> entry : pollutants.entrySet()) {
            String vehicleTypeId = entry.getKey();
            Object2DoubleMap<Pollutant> emissionMap = entry.getValue();

            for (Pollutant pollutant : Pollutant.values()) {
                double emissionValue = emissionMap.getDouble(pollutant);
                emissionsCSV.print(vehicleTypeId);
                emissionsCSV.print(pollutant);
                emissionsCSV.print(emissionValue * sample.getUpscaleFactor());
                emissionsCSV.println();
            }
        }

        emissionsCSV.close();
    }

    private void writeEmissionsByVehicleType(EmissionsByVehicleTypeEventHandler emissionsByVehicleType) throws IOException {

        log.info("Writing emissions by vehicle type...");
        Map<Id<VehicleType>, Object2DoubleMap<Pollutant>> pollutants = emissionsByVehicleType.getByVehicleType();

        CSVPrinter emissionsCSV = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_vehicle_type.csv")), CSVFormat.DEFAULT);

        emissionsCSV.print("vehicleType");
        emissionsCSV.print("pollutant");
        emissionsCSV.print("value");
        emissionsCSV.println();

        for (Map.Entry<Id<VehicleType>, Object2DoubleMap<Pollutant>> entry : pollutants.entrySet()) {
            Id<VehicleType> vehicleTypeId = entry.getKey();
            Object2DoubleMap<Pollutant> emissionMap = entry.getValue();

            for (Pollutant pollutant : Pollutant.values()) {
                double emissionValue = emissionMap.getDouble(pollutant);
                emissionsCSV.print(vehicleTypeId);
                emissionsCSV.print(pollutant);
                emissionsCSV.print(emissionValue * sample.getUpscaleFactor());
                emissionsCSV.println();
            }
        }

        emissionsCSV.close();
    }


    private void writeOutput(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) throws IOException {

        log.info("Emission analysis completed.");

        log.info("Writing output...");

        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMaximumFractionDigits(4);
        nf.setGroupingUsed(false);

        CSVPrinter absolute = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_link.csv")), CSVFormat.DEFAULT);
        CSVPrinter perMeter = new CSVPrinter(Files.newBufferedWriter(output.getPath("emissions_per_link_per_m.csv")), CSVFormat.DEFAULT);

        absolute.print("linkId");
        perMeter.print("linkId");

        for (Pollutant pollutant : Pollutant.values()) {
            absolute.print(pollutant);
            perMeter.print(pollutant + " [g/m]");
        }

        absolute.println();
        perMeter.println();

        Map<Id<Link>, Map<Pollutant, Double>> link2pollutants = emissionsEventHandler.getLink2pollutants();

        for (Id<Link> linkId : link2pollutants.keySet()) {

            // Link might be filtered
            if (!network.getLinks().containsKey(linkId))
                continue;

            absolute.print(linkId);
            perMeter.print(linkId);

            for (Pollutant pollutant : Pollutant.values()) {
                double emissionValue = 0.;
                if (link2pollutants.get(linkId).get(pollutant) != null) {
                    emissionValue = link2pollutants.get(linkId).get(pollutant);
                }
                absolute.print(nf.format(emissionValue * sample.getUpscaleFactor()));

                Link link = network.getLinks().get(linkId);
                double emissionPerM = emissionValue / link.getLength();
                perMeter.print(nf.format(emissionPerM * sample.getUpscaleFactor()));
            }

            absolute.println();
            perMeter.println();
        }

        absolute.close();
        perMeter.close();
    }

    /**
     * Total emissions table.
     */
    private void writeTotal(Network network, EmissionsOnLinkEventHandler emissionsEventHandler) {

        Object2DoubleMap<Pollutant> sum = new Object2DoubleLinkedOpenHashMap<>();

        DecimalFormat simple = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        simple.setMaximumFractionDigits(2);
        simple.setMaximumIntegerDigits(5);

        DecimalFormat scientific = new DecimalFormat("0.###E0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        for (Map.Entry<Id<Link>, Map<Pollutant, Double>> e : emissionsEventHandler.getLink2pollutants().entrySet()) {

            if (!network.getLinks().containsKey(e.getKey()))
                continue;
            for (Map.Entry<Pollutant, Double> p : e.getValue().entrySet()) {
                sum.mergeDouble(p.getKey(), p.getValue(), Double::sum);
            }
        }



        try (CSVPrinter total = new CSVPrinter(Files.newBufferedWriter(Path.of("/Users/gregorr/Documents/work/stuff/analysisGlaMoBi/forTrainWork/test/emissions_total.csv")), CSVFormat.DEFAULT)) {

            total.printRecord("Pollutant", "kg");
            for (Pollutant p : Pollutant.values()) {
                double val = (sum.getDouble(p) * sample.getUpscaleFactor()) / 1000;
                total.printRecord(p, val < 100_000 && val > 100 ? simple.format(val) : scientific.format(val));
            }

        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }



    private static class EmissionsByVehicleHandler implements ColdEmissionEventHandler, WarmEmissionEventHandler {

        private final Map<Id<Vehicle>, Object2DoubleOpenHashMap<Pollutant>> vehicleEmissions = new HashMap<>();

        @Override
        public void handleEvent(WarmEmissionEvent event) {
            handle(event.getVehicleId(), event.getWarmEmissions());
        }

        @Override
        public void handleEvent(ColdEmissionEvent event) {
            handle(event.getVehicleId(), event.getColdEmissions());
        }

        private void handle(Id<Vehicle> vehicleId, Map<Pollutant, Double> emissions) {
            vehicleEmissions.computeIfAbsent(vehicleId, v -> new Object2DoubleOpenHashMap<>());
            for (Map.Entry<Pollutant, Double> e : emissions.entrySet()) {
                vehicleEmissions.get(vehicleId).addTo(e.getKey(), e.getValue());
            }
        }

        @Override
        public void reset(int iteration) {
            vehicleEmissions.clear();
        }

        public Map<Id<Vehicle>, Object2DoubleOpenHashMap<Pollutant>> getVehicleEmissions() {
            return vehicleEmissions;
        }
    }


    /**
     * Writes emissions per vehicle to a CSV file.
     */
    private void writeEmissionsPerVehicle(EmissionsByVehicleHandler emissionsByVehicleHandler) {
        log.info("Writing emissions per vehicle...");

        try (CSVPrinter printer = new CSVPrinter(
                Files.newBufferedWriter(Path.of("/Users/gregorr/Documents/work/stuff/analysisGlaMoBi/forTrainWork/test/emissions_per_vehicle.csv")),
                CSVFormat.DEFAULT.withHeader("vehicleId", "pollutant", "grams"))) {

            for (Map.Entry<Id<Vehicle>, Object2DoubleOpenHashMap<Pollutant>> vehicleEntry :
                    emissionsByVehicleHandler.getVehicleEmissions().entrySet()) {

                Id<Vehicle> vehicleId = vehicleEntry.getKey();
                Object2DoubleOpenHashMap<Pollutant> pollutantMap = vehicleEntry.getValue();

                for (Pollutant pollutant : Pollutant.values()) {
                    double value = pollutantMap.getDouble(pollutant) * sample.getUpscaleFactor();
                    printer.printRecord(vehicleId.toString(), pollutant.toString(), value);
                }
            }

        } catch (IOException e) {
            throw new UncheckedIOException("Error writing emissions_per_vehicle.csv", e);
        }

        log.info("Finished writing emissions_per_vehicle.csv");
    }



}
