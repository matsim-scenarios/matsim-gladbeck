package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
        name = "gladbeck-emissions",
        description = "Calculates emission events from existing MATSim network, vehicles, and events.",
        mixinStandardHelpOptions = true
)
public class GladbeckEmissionsRunner implements MATSimAppCommand {

    @CommandLine.Option(names = "--run-dir", description = "Path to the MATSim run directory", required = true)
    private Path runDirectory;

    @CommandLine.Option(names = "--output", description = "Path for emission events output", required = true)
    private Path outputFile;

    @CommandLine.Mixin
    private ShpOptions shp;

    @Override
    public Integer call() throws Exception {
        // 1. Load existing config and scenario
        Path configPath = ApplicationUtils.matchInput("config.xml", runDirectory);
        Config config = ConfigUtils.loadConfig(configPath.toString());
        config.vehicles().setVehiclesFile("/Users/gregorr/Volumes/math-cluster/matsim-gladbeck/v3.0/baseCaseContinued/output/3pctWithUnselectedPlans/output_gladbeck-v3.0-3pct.output_vehicles.xml.gz");
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // 2. Prepare vehicle types for HBEFA emissions
        GladbeckDashboardsRunner.prepareVehicleTypesForEmissionAnalysis(scenario);

        //network adjustments for HBEFA
        //set track or footway manually to path as it is not included in HBEFA mapping
        for (Link link : scenario.getNetwork().getLinks().values()) {
            String type = (String) link.getAttributes().getAttribute("type");
            if (type != null && type.equals("track") || type != null && type.equals("footway")) {
                link.getAttributes().putAttribute("type", "path");
            }
        }

        HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
        roadTypeMapping.addHbefaMappings(scenario.getNetwork());

        // 3. Setup Emissions module
        EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        eConfig.setWritingEmissionsEvents(true);
        GladbeckDashboardsRunner.setEmissionsConfigs(config);

        EmissionModule emissionModule = new EmissionModule(scenario, EventsUtils.createEventsManager());

        // 4. Setup emission events writer
        EventWriterXML emissionWriter = new EventWriterXML(outputFile.toString());
        emissionModule.getEmissionEventsManager().addHandler(emissionWriter);

        // 5. Process events
        List<String> eventsFiles = List.of(ApplicationUtils.matchInput("output_events.xml.gz", runDirectory).toString());
        for (String eventsFile : eventsFiles) {
            EventsUtils.readEvents(emissionModule.getEmissionEventsManager(), eventsFile);
        }

        // 6. Close writer and write summary
        emissionWriter.closeFile();
        emissionModule.writeEmissionInformation();

        System.out.println("Emission events written to: " + outputFile);
        return 0;
    }

    public static void main(String[] args) {
        new GladbeckEmissionsRunner().execute(args);
    }
}