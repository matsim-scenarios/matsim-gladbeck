package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.emissions.AirPollutionByVehicleCategory;
import org.matsim.application.analysis.emissions.AirPollutionSpatialAggregation;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.ExtractHomeCoordinates;
import org.matsim.application.prepare.population.FixSubtourModes;
import org.matsim.application.prepare.population.XYToLinks;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.prepare.ScenarioCutOut;
import org.matsim.run.policies.PtFlatrate;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.run.policies.Tempo30Zone;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.List;

@CommandLine.Command(header = ":: Gladbeck Scenario ::", version = RunGladbeckScenario.VERSION)
@MATSimApplication.Prepare({ScenarioCutOut.class, DownSamplePopulation.class, FixSubtourModes.class, XYToLinks.class, ExtractHomeCoordinates.class})
@MATSimApplication.Analysis({NoiseAnalysis.class, AirPollutionByVehicleCategory.class, AirPollutionSpatialAggregation.class})
public class RunGladbeckScenario extends RunMetropoleRuhrScenario {

	public static final String VERSION = "v1.0";

	private static final Logger log = LogManager.getLogger(RunGladbeckScenario.class);

	@CommandLine.Option(names = "--ptFlat", defaultValue = "false", description = "measures to allow car users to have free pt")
	private boolean ptFlat;

	@CommandLine.Option(names = "--schoolClosure", defaultValue = "false", description = "measures to ban car on certain links")
	boolean schoolClosure;

	@CommandLine.Option(names = "--tempo30Zone", defaultValue = "false", description = "measures to reduce car speed to 30 km/h")
	boolean tempo30Zone;

	static HashMap<Id<Person>, Integer> personsEligibleForPtFlat = new HashMap<>();

	public RunGladbeckScenario() {
		super(String.format("./scenarios/gladbeck-v1.0/input/gladbeck-%s-25pct.config.xml", VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunGladbeckScenario.class, args);
	}

	@Override
	protected Config prepareConfig(Config config) {
		// Always switch off intermodal
		this.intermodal = false;
		return super.prepareConfig(config);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		if (ptFlat) {
			for (Person p : scenario.getPopulation().getPersons().values()) {
				Plan plan = p.getSelectedPlan();
				List<Leg> legs = TripStructureUtils.getLegs(plan);
				//only car users are allowed to enjoy the pt flatrate
				for (Leg leg : legs) {
					if (leg.getMode().equals(TransportMode.car)) {
						personsEligibleForPtFlat.put(p.getId(), 0);
					}
				}
			}
			log.info("Number of Agents eligible for pt flat: " + personsEligibleForPtFlat.size());
		}

		if (tempo30Zone) {
			Tempo30Zone.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource("/Users/gregorr/Desktop/Test_ScenarioCutOut/test.shp")));
		}

		if (schoolClosure) {
			new SchoolRoadsClosure().closeSchoolRoads(scenario.getNetwork());
		}
	}

	@Override
	protected void prepareControler(Controler controler) {
		super.prepareControler(controler);
		//use income-dependent marginal utility of money for scoring
		log.info("Using income dependent scoring");
		if (ptFlat) {
			log.info("using pt flat");
			double ptConstant = controler.getConfig().planCalcScore().getScoringParameters("person").getModes().get(TransportMode.pt).getConstant();
			log.info("pt constant is: " + ptConstant);
			double ptDailyMonetaryConstant = controler.getConfig().planCalcScore().getScoringParameters("person").getModes().get(TransportMode.pt).getDailyMonetaryConstant();
			log.info("daily monetary constant is: " + ptDailyMonetaryConstant);
			PtFlatrate ptFlatrate = new PtFlatrate(personsEligibleForPtFlat, -1 * ptConstant, -1 * ptDailyMonetaryConstant);
			controler.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					addEventHandlerBinding().toInstance(ptFlatrate);
					addControlerListenerBinding().toInstance(ptFlatrate);
					install(new PersonMoneyEventsAnalysisModule());
				}
			});
		}
	}
}
