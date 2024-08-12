package org.matsim.run;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import com.google.common.collect.Sets;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.util.Assert;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.application.options.SampleOptions;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleModule;
import org.matsim.contrib.vsp.scenario.SnzActivities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.*;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.extensions.pt.PtExtensionsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsConfigGroup;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesConfigGroup;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.prepare.RuhrUtils;
import org.matsim.simwrapper.SimWrapperConfigGroup;
import org.matsim.simwrapper.SimWrapperModule;
import org.matsim.vehicles.VehicleType;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType.accessEgressModeToLinkPlusTimeConstant;

/**
 * This class contains common functionality for scenario setup. It was mostly taken from {@link RunMetropoleRuhrScenario}.
 */
public final class ScenarioUtils {

	private static final Logger log = LogManager.getLogger(ScenarioUtils.class);

	private ScenarioUtils() {}

	/**
	 * Prepare the config for commercial traffic.
	 */
	public static void prepareCommercialTrafficConfig(Config config) {

		// TODO: these are not correct yet, vehicle Types must be updated
		Set<String> modes = Set.of("freight", "truck8t", "truck18t", "truck26t", "truck40t");

		modes.forEach(mode -> {
			ScoringConfigGroup.ModeParams thisModeParams = new ScoringConfigGroup.ModeParams(mode);
			config.scoring().addModeParams(thisModeParams);
		});

		Set<String> qsimModes = new HashSet<>(config.qsim().getMainModes());
		config.qsim().setMainModes(Sets.union(qsimModes, modes));

		Set<String> networkModes = new HashSet<>(config.routing().getNetworkModes());
		config.routing().setNetworkModes(Sets.union(networkModes, modes));

		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_start").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_end").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("service").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("pickup").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("delivery").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("commercial_return").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("start").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("end").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_start").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_end").setTypicalDuration(30 * 60));
		config.scoring().addActivityParams(new ScoringConfigGroup.ActivityParams("freight_return").setTypicalDuration(30 * 60));

		for (String subpopulation : List.of("LTL_trips", "commercialPersonTraffic", "commercialPersonTraffic_service", "longDistanceFreight",
			"FTL_trip", "FTL_kv_trip", "goodsTraffic")) {
			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta)
					.setWeight(0.85)
					.setSubpopulation(subpopulation)
			);

			config.replanning().addStrategySettings(
				new ReplanningConfigGroup.StrategySettings()
					.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute)
					.setWeight(0.1)
					.setSubpopulation(subpopulation)
			);
		}
	}


	public static Config prepareConfig(Config config, SampleOptions sample, boolean intermodal) {
		// avoid unmaterialized config group exceptions in general for PtExtensionsConfigGroup, IntermodalTripFareCompensatorsConfigGroup
		// avoid unmaterialized config group exceptions in tests for PtIntermodalRoutingModesConfigGroup, SwissRailRaptorConfigGroup
		PtExtensionsConfigGroup ptExtensionsConfigGroup = ConfigUtils.addOrGetModule(config, PtExtensionsConfigGroup.class);
		IntermodalTripFareCompensatorsConfigGroup intermodalTripFareCompensatorsConfigGroup = ConfigUtils.addOrGetModule(config, IntermodalTripFareCompensatorsConfigGroup.class);
		PtIntermodalRoutingModesConfigGroup ptIntermodalRoutingModesConfigGroup = ConfigUtils.addOrGetModule(config, PtIntermodalRoutingModesConfigGroup.class);
		SwissRailRaptorConfigGroup swissRailRaptorConfigGroup = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);

		// because vsp default reasons
		config.facilities().setFacilitiesSource(FacilitiesConfigGroup.FacilitiesSource.onePerActivityLinkInPlansFile);


		// someone wished to have an easy option to remove all intermodal functionality, so remove it from config or switch off
		if (!intermodal) {

			log.info("Disabling intermodal config...");

			// remove config options
			SubtourModeChoiceConfigGroup subtourModeChoice = config.subtourModeChoice();

			// intermodal pt should not be a chain-based mode, otherwise those would have to be modified too
			subtourModeChoice.setModes(
					Arrays.stream(subtourModeChoice.getModes())
					.filter(s -> !s.equals("pt_intermodal_allowed"))
					.toArray(String[]::new)
			);


			ChangeModeConfigGroup changeModeConfigGroup = config.changeMode();
			changeModeConfigGroup.setModes(
					Arrays.stream(changeModeConfigGroup.getModes())
							.filter(s -> !s.equals("pt_intermodal_allowed"))
							.toArray(String[]::new)
			);


			swissRailRaptorConfigGroup.setUseIntermodalAccessEgress(false);
			List<SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet> intermodalAccessEgressParameterSets =
					swissRailRaptorConfigGroup.getIntermodalAccessEgressParameterSets();
			intermodalAccessEgressParameterSets.clear();

			PtExtensionsConfigGroup.IntermodalAccessEgressModeUtilityRandomization[] intermodalAccessEgressModeUtilityRandomizationArray =
					ptExtensionsConfigGroup.getIntermodalAccessEgressModeUtilityRandomizations().
							toArray(new PtExtensionsConfigGroup.IntermodalAccessEgressModeUtilityRandomization[0]);
			for (PtExtensionsConfigGroup.IntermodalAccessEgressModeUtilityRandomization intermodalAccessEgressModeUtilityRandomization : intermodalAccessEgressModeUtilityRandomizationArray) {
				intermodalTripFareCompensatorsConfigGroup.removeParameterSet(intermodalAccessEgressModeUtilityRandomization);
			}

			IntermodalTripFareCompensatorConfigGroup[] intermodalTripFareCompensatorConfigGroupArray =
					intermodalTripFareCompensatorsConfigGroup.getIntermodalTripFareCompensatorConfigGroups().
							toArray(new IntermodalTripFareCompensatorConfigGroup[0]);
			for (IntermodalTripFareCompensatorConfigGroup intermodalTripFareCompensatorConfigGroup : intermodalTripFareCompensatorConfigGroupArray) {
				intermodalTripFareCompensatorsConfigGroup.removeParameterSet(intermodalTripFareCompensatorConfigGroup);
			}

			PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet[] ptIntermodalRoutingModeParameterArrays =
					ptIntermodalRoutingModesConfigGroup.getPtIntermodalRoutingModeParameterSets().
							toArray(new PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet[0]);
			for (PtIntermodalRoutingModesConfigGroup.PtIntermodalRoutingModeParameterSet ptIntermodalRoutingModeParameterArray : ptIntermodalRoutingModeParameterArrays) {
				ptIntermodalRoutingModesConfigGroup.removeParameterSet(ptIntermodalRoutingModeParameterArray);
			}
		}

		OutputDirectoryLogging.catchLogEntries();

		// bike contrib is needed for bike highways and elevation routing
		BicycleConfigGroup bikeConfigGroup = ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
		bikeConfigGroup.setBicycleMode(TransportMode.bike);

		// this is needed for the parking cost money events
		ParkingCostConfigGroup parkingCostConfigGroup = ConfigUtils.addOrGetModule(config, ParkingCostConfigGroup.class);
		parkingCostConfigGroup.setFirstHourParkingCostLinkAttributeName(RuhrUtils.ONE_HOUR_P_COST);
		parkingCostConfigGroup.setExtraHourParkingCostLinkAttributeName(RuhrUtils.EXTRA_HOUR_P_COST);
		parkingCostConfigGroup.setMaxDailyParkingCostLinkAttributeName(RuhrUtils.MAX_DAILY_P_COST);
		parkingCostConfigGroup.setMaxParkingDurationAttributeName(RuhrUtils.MAX_P_TIME);
		parkingCostConfigGroup.setParkingPenaltyAttributeName(RuhrUtils.P_FINE);
		parkingCostConfigGroup.setResidentialParkingFeeAttributeName(RuhrUtils.RES_P_COSTS);

		log.info("using accessEgressModeToLinkPlusTimeConstant");
		// we do this to model parking search traffic, as on some links car agents have additional travel time
		config.routing().setAccessEgressType(accessEgressModeToLinkPlusTimeConstant);
		config.qsim().setUsingTravelTimeCheckInTeleportation(true);
		config.qsim().setUsePersonIdForMissingVehicleId(false);

		SimWrapperConfigGroup simWrapperConfigGroup = ConfigUtils.addOrGetModule(config, SimWrapperConfigGroup.class);

		// adjust if sample size specific parameters
		if (sample.isSet()) {
			config.controller().setRunId(sample.adjustName(config.controller().getRunId()));
			config.controller().setOutputDirectory(sample.adjustName(config.controller().getOutputDirectory()));
			config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));

			config.qsim().setFlowCapFactor(sample.getSample());
			config.qsim().setStorageCapFactor(sample.getSample());

			simWrapperConfigGroup.sampleSize = sample.getSample();

			config.counts().setCountsScaleFactor(sample.getSample());
		}

		// snz activity types that are always the same, Differentiated by typical duration
		SnzActivities.addScoringParams(config);

		//ride scoring params

		// alpha can be calibrated
		double alpha = 2.0;
		//gamma must stay one
		double gamma = 0.0;
		double monetaryDistanceRateRide =  config.scoring().getOrCreateModeParams(TransportMode.car).getMonetaryDistanceRate() * alpha;

		double marginalUtilityOfTravllingRide = config.scoring().getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfTraveling(); //marginal disutility of passenger
		marginalUtilityOfTravllingRide += alpha * (-config.scoring().getPerforming_utils_hr() + config.scoring().getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfTraveling()); // Zeitverbrauch des Fahrers
		//marginalUtilityOfTravllingRide += gamma * -config.scoring().getPerforming_utils_hr(); //we prefer not using this term

		double tmp = (alpha + gamma) * -(config.scoring().getPerforming_utils_hr()) + config.scoring().getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfTraveling() * (1.0 + alpha) ;

		Assert.isTrue(tmp==marginalUtilityOfTravllingRide);

		double marginalUtilityOfDistanceRide = (alpha + 1.0) * config.scoring().getOrCreateModeParams(TransportMode.car).getMarginalUtilityOfDistance();
		config.scoring().getOrCreateModeParams(TransportMode.ride).setMonetaryDistanceRate(monetaryDistanceRateRide);
		config.scoring().getOrCreateModeParams(TransportMode.ride).setMarginalUtilityOfDistance(marginalUtilityOfDistanceRide);
		config.scoring().getOrCreateModeParams(TransportMode.ride).setMarginalUtilityOfTraveling(marginalUtilityOfTravllingRide);

		prepareCommercialTrafficConfig(config);

		return config;
	}


	public static void prepareScenario(Scenario scenario) {

		VehicleType bike = scenario.getVehicles().getVehicleTypes().get(Id.create("bike", VehicleType.class));
		bike.setNetworkMode(TransportMode.bike);
	}

	public static void prepareControler(Controler controler) {

		if (!controler.getConfig().transit().isUsingTransitInMobsim()) {
			log.error("Public transit will be teleported and not simulated in the mobsim! "
					+ "This will have a significant effect on pt-related parameters (travel times, modal split, and so on). "
					+ "Should only be used for testing or car-focused studies with fixed modal split.");
			throw new IllegalArgumentException("Pt is teleported, wich is not supported");
		}

		controler.addOverridingModule(new SimWrapperModule());

		// allow for separate pt routing modes (pure walk+pt, bike+walk+pt, car+walk+pt, ...)
		controler.addOverridingModule(new PtIntermodalRoutingModesModule());
		// throw additional score or money events if pt is combined with bike or car in the same trip
		controler.addOverridingModule(new IntermodalTripFareCompensatorsModule());

		// additional analysis output
		//controler.addOverridingModule(new LinkPaxVolumesAnalysisModule());
		controler.addOverridingModule(new PtStop2StopAnalysisModule());

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {

				// use the (congested) car travel time for the teleported ride mode
				addTravelTimeBinding(TransportMode.ride).to(networkTravelTime());
				addTravelDisutilityFactoryBinding(TransportMode.ride).to(carTravelDisutilityFactoryKey());
				addTravelTimeBinding(TransportMode.bike).to(networkTravelTime());
				addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);

				// calculate access/egress leg generalized cost correctly for intermodal pt routing
				bind(RaptorIntermodalAccessEgress.class).to(EnhancedRaptorIntermodalAccessEgress.class);
				// separate pure walk+pt from intermodal pt in mode stats etc.
				bind(AnalysisMainModeIdentifier.class).to(IntermodalPtAnalysisModeIdentifier.class);

				// for income dependent scoring --> this works with the bicycle contrib as we don´t use the scoring in the bicycle contrib
				bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);

			}
		});


		log.info("Adding money event analysis");

		//analyse PersonMoneyEvents
		controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());

		//this is needed for  the parking cost
		controler.addOverridingModule(new ParkingCostModule());
		// bicycle contrib
		controler.addOverridingModule(new BicycleModule());
	}

}
