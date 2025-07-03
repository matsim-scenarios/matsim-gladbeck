package org.matsim.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.ApplicationUtils;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.options.ShpOptions;
import org.matsim.contrib.emissions.HbefaRoadTypeMapping;
import org.matsim.contrib.emissions.HbefaVehicleCategory;
import org.matsim.contrib.emissions.OsmHbefaMapping;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.dashboard.*;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class GladbeckDashboardsRunner implements MATSimAppCommand {

    @CommandLine.Mixin
    private ShpOptions shp;

    @CommandLine.Option(names = "--path", description = "Path to the run you want to create a " +
            "dashboard for", required = true)
    private Path runDirectory;

    @Override
    public Integer call() throws Exception {
        //need to rename network change events

        if (Files.exists(Path.of(runDirectory + "/output_gladbeck-v3.0-3pct.output_change_events.xml.gz"))) {
            Files.move(Path.of(runDirectory + "/output_gladbeck-v3.0-3pct.output_change_events.xml.gz"), Path.of(runDirectory + "/output_gladbeck-v3.0-3pct.output_networkChangeEvents.xml.gz"), StandardCopyOption.REPLACE_EXISTING);
        } else {
            System.out.println("Source file does not exist");
        }
        Path configPath = ApplicationUtils.matchInput("config.xml", runDirectory);
        Config config = ConfigUtils.loadConfig(configPath.toString());
        config.network().setChangeEventsInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/input/v3.0/gladbeck-v3.0-networkChangeEventsGladbeck3pct.xml.gz");
        SimWrapper sw = SimWrapper.create(config);
        SimWrapperConfigGroup simwrapperCfg = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

        if (shp.isDefined()) {
            simwrapperCfg.defaultParams().shp = shp.getShapeFile().toString();
        }

        simwrapperCfg.defaultDashboards = SimWrapperConfigGroup.Mode.enabled;
        sw.addDashboard(new OverviewDashboard());
        sw.addDashboard(new TripDashboard("src/main/resources/gladbeck_mode_share.csv", null, null));
        sw.addDashboard(new TrafficDashboard());
        sw.addDashboard(new TrafficCountsDashboard());
        sw.addDashboard(new StuckAgentDashboard());
        sw.addDashboard(new EmissionsDashboard("EPSG:25832"));
        sw.addDashboard(new NoiseDashboard("EPSG:25832"));

        setEmissionsConfigs(config);
        ConfigUtils.writeConfig(config, configPath.toString());

        Config dummyConfig = ConfigUtils.createConfig();

        String networkPath = ApplicationUtils.matchInput("output_network.xml.gz", runDirectory).toString();
        String vehiclesPath = ApplicationUtils.matchInput("output_vehicles.xml.gz", runDirectory).toString();
        String transitVehiclesPath = ApplicationUtils.matchInput("output_transitVehicles.xml.gz", runDirectory).toString();

        dummyConfig.network().setInputFile(networkPath);
        dummyConfig.vehicles().setVehiclesFile(vehiclesPath);
        dummyConfig.transit().setVehiclesFile(transitVehiclesPath);
        dummyConfig.global().setCoordinateSystem(config.global().getCoordinateSystem());

        Scenario scenario = ScenarioUtils.loadScenario(dummyConfig);

        //set track or footway manually to path as it is not included in HBEFA mapping
        for (Link link : scenario.getNetwork().getLinks().values()) {
            String type = (String) link.getAttributes().getAttribute("type");
            if (type != null && type.equals("track") || type != null && type.equals("footway")) {
                link.getAttributes().putAttribute("type", "path");
            }
        }

        HbefaRoadTypeMapping roadTypeMapping = OsmHbefaMapping.build();
        roadTypeMapping.addHbefaMappings(scenario.getNetwork());

        prepareVehicleTypesForEmissionAnalysis(scenario);

        //overwrite outputs with adapted files
        NetworkUtils.writeNetwork(scenario.getNetwork(), networkPath);
        new MatsimVehicleWriter(scenario.getVehicles()).writeFile(vehiclesPath);
        new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(transitVehiclesPath);

        sw.generate(runDirectory);
        sw.run(runDirectory);

        return 0;
    }

    public static void main(String[] args) {
        new GladbeckDashboardsRunner().execute(args);
    }


    /**
     * Prepare vehicle types with necessary HBEFA information for emission analysis.
     */
    public static void prepareVehicleTypesForEmissionAnalysis(Scenario scenario) {
        final String AVERAGE = "average";

        for (VehicleType type : scenario.getVehicles().getVehicleTypes().values()) {
            EngineInformation engineInformation = type.getEngineInformation();

            //only set engine information if none are present
            if (engineInformation.getAttributes().isEmpty()) {
                switch (type.getId().toString()) {
                    case TransportMode.car -> {
                        VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.PASSENGER_CAR.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }
                    case TransportMode.ride -> {
                        //ignore ride, the mode routed on network, but then teleported
                        VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }

                    case TransportMode.bike -> {
                        //ignore bikes
                        VehicleUtils.setHbefaVehicleCategory(engineInformation, HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }

                    case "truck18t" -> {
                        VehicleUtils.setHbefaVehicleCategory(engineInformation,HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }
                    case "truck26t" -> {
                        VehicleUtils.setHbefaVehicleCategory(engineInformation,HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }
                    case "truck40t" -> {
                        VehicleUtils.setHbefaVehicleCategory(engineInformation,HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }
                    case "truck8t" -> {
                        VehicleUtils.setHbefaVehicleCategory(engineInformation,HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString());
                        VehicleUtils.setHbefaTechnology(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaSizeClass(engineInformation, AVERAGE);
                        VehicleUtils.setHbefaEmissionsConcept(engineInformation, AVERAGE);
                    }

                    default ->
                            throw new IllegalArgumentException("does not know how to handle vehicleType " + type.getId().toString());
                }
            }
        }

        //ignore all pt veh types
        scenario.getTransitVehicles()
                .getVehicleTypes()
                .values().forEach(type -> VehicleUtils.setHbefaVehicleCategory(type.getEngineInformation(), HbefaVehicleCategory.NON_HBEFA_VEHICLE.toString()));
    }

    public static void setEmissionsConfigs(Config config) {
        final String HBEFA_2020_PATH = "https://svn.vsp.tu-berlin.de/repos/public-svn/3507bb3997e5657ab9da76dbedbb13c9b5991d3e/0e73947443d68f95202b71a156b337f7f71604ae/";
        final String HBEFA_FILE_COLD_DETAILED = HBEFA_2020_PATH + "82t7b02rc0rji2kmsahfwp933u2rfjlkhfpi2u9r20.enc";
        final String HBEFA_FILE_WARM_DETAILED = HBEFA_2020_PATH + "944637571c833ddcf1d0dfcccb59838509f397e6.enc";
        final String HBEFA_FILE_COLD_AVERAGE = HBEFA_2020_PATH + "r9230ru2n209r30u2fn0c9rn20n2rujkhkjhoewt84202.enc";
        final String HBEFA_FILE_WARM_AVERAGE = HBEFA_2020_PATH + "7eff8f308633df1b8ac4d06d05180dd0c5fdf577.enc";

        EmissionsConfigGroup eConfig = ConfigUtils.addOrGetModule(config, EmissionsConfigGroup.class);
        eConfig.setDetailedColdEmissionFactorsFile(HBEFA_FILE_COLD_DETAILED);
        eConfig.setDetailedWarmEmissionFactorsFile(HBEFA_FILE_WARM_DETAILED);
        eConfig.setAverageColdEmissionFactorsFile(HBEFA_FILE_COLD_AVERAGE);
        eConfig.setAverageWarmEmissionFactorsFile(HBEFA_FILE_WARM_AVERAGE);
        eConfig.setHbefaTableConsistencyCheckingLevel(EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent);
        eConfig.setDetailedVsAverageLookupBehavior(EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable);
    }


}
