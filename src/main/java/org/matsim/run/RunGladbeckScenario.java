package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.emissions.AirPollutionByVehicleCategory;
import org.matsim.application.analysis.emissions.AirPollutionSpatialAggregation;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.ExtractHomeCoordinates;
import org.matsim.application.prepare.population.FixSubtourModes;
import org.matsim.application.prepare.population.XYToLinks;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.prepare.ScenarioCutOut;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.run.policies.ReduceSpeed;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(header = ":: Gladbeck Scenario ::", version = RunGladbeckScenario.VERSION)
@MATSimApplication.Prepare({ScenarioCutOut.class, DownSamplePopulation.class, FixSubtourModes.class, XYToLinks.class, ExtractHomeCoordinates.class})
@MATSimApplication.Analysis({NoiseAnalysis.class, AirPollutionByVehicleCategory.class, AirPollutionSpatialAggregation.class})
public class RunGladbeckScenario extends RunMetropoleRuhrScenario {

	public static final String VERSION = "v1.0";

	private static final Logger log = LogManager.getLogger(RunGladbeckScenario.class);

	@CommandLine.Option(names = "--schoolClosure", defaultValue = "false", description = "measures to ban car on certain links")
	boolean schoolClosure;

	@CommandLine.Option(names = "--tempo30Zone", defaultValue = "false", description = "measures to reduce car speed to 30 km/h")
	boolean tempo30Zone;

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Option(names = "--simplePtFlat", defaultValue = "false", description = "measures to allow everyone to have free pt")
	private boolean simplePtFlat;

	public RunGladbeckScenario() {
		super(String.format("./scenarios/gladbeck-v1.0/input/gladbeck-%s-10pct.config.xml", VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunGladbeckScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		// Always switch off intermodal
		this.intermodal = false;

		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

		if (simplePtFlat) {
			config.planCalcScore().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0.0);
		}

		// this is for the school closure case
		config.network().setTimeVariantNetwork(true);
		return super.prepareConfig(config);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		if (tempo30Zone) {
			ReduceSpeed.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shp.getShapeFile().toString())));
		}

		if (schoolClosure) {
			List<Id<Link>> listOfSchoolLinks = new ArrayList<>();
			// Mosaikschule
			listOfSchoolLinks.add(Id.createLinkId("5156341260014r"));
			listOfSchoolLinks.add(Id.createLinkId("5156341260014f"));
			listOfSchoolLinks.add(Id.createLinkId("380432140001r"));
			listOfSchoolLinks.add(Id.createLinkId("380432140001f"));
			listOfSchoolLinks.add(Id.createLinkId("381870670005f"));
			listOfSchoolLinks.add(Id.createLinkId("381870670005r"));
			listOfSchoolLinks.add(Id.createLinkId("353353090002f"));
			listOfSchoolLinks.add(Id.createLinkId("353353090002r"));

			//  werner von siemens schule gladbeck
			listOfSchoolLinks.add(Id.createLinkId("358770500002f"));
			listOfSchoolLinks.add(Id.createLinkId("358770500002r"));
			listOfSchoolLinks.add(Id.createLinkId("358770510002r"));
			listOfSchoolLinks.add(Id.createLinkId("358770510002r"));
			listOfSchoolLinks.add(Id.createLinkId("358770510002f"));
			listOfSchoolLinks.add(Id.createLinkId("1157881300007f"));
			listOfSchoolLinks.add(Id.createLinkId("1157881300007r"));
			listOfSchoolLinks.add(Id.createLinkId("1157881300007r"));
			listOfSchoolLinks.add(Id.createLinkId("1157881300007r"));
			listOfSchoolLinks.add(Id.createLinkId("481471120002f"));
			listOfSchoolLinks.add(Id.createLinkId("481471120002r"));
			new SchoolRoadsClosure().closeSchoolLinks(listOfSchoolLinks, scenario.getNetwork(), 800, 1700);
		}
	}

	@Override
	protected void prepareControler(Controler controler) {
		//controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		super.prepareControler(controler);
	}
}
