package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.HomeLocationFilter;
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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.prepare.AssignPersonAttributes;
import org.matsim.prepare.BicyclePolicies;
import org.matsim.prepare.PrepareOpenPopulation;
import org.matsim.prepare.ScenarioCutOut;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.run.policies.PtFlatrate;
import org.matsim.run.policies.ReduceSpeed;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;
import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@CommandLine.Command(header = ":: Gladbeck Scenario ::", version = RunGladbeckScenario.VERSION)
@MATSimApplication.Prepare({ScenarioCutOut.class, DownSamplePopulation.class, FixSubtourModes.class, XYToLinks.class, ExtractHomeCoordinates.class, BicyclePolicies.class, PrepareOpenPopulation.class})
@MATSimApplication.Analysis({NoiseAnalysis.class, AirPollutionByVehicleCategory.class, AirPollutionSpatialAggregation.class})
public class RunGladbeckScenario extends RunMetropoleRuhrScenario {

	public static final String VERSION = "v2.0";

	private static final Logger log = LogManager.getLogger(RunGladbeckScenario.class);

	@CommandLine.Option(names = "--schoolClosure", defaultValue = "false", description = "measures to ban car on certain links")
	boolean schoolClosure;

	@CommandLine.Option(names = "--tempo30Zone", defaultValue = "false", description = "measures to reduce car speed to 30 km/h in a zone")
	boolean slowSpeedZone;

	@CommandLine.Option(names = "--tempo30Streets", defaultValue = "false", description = "measures to reduce car speed to 30 km/h on links definded by a shape file")
	boolean slowSpeedOnDefinedLinks;

	@CommandLine.Mixin
	private ShpOptions shp;

	@CommandLine.Option(names = "--simplePtFlat", defaultValue = "false", description = "measures to allow everyone to have free pt")
	boolean scenarioWidePtFlat;

	@CommandLine.Option(names = "--ptFlat", defaultValue = "0", description = "measures to allow people in Gladbeck to have free pt, if set to zero no agent will have free pt")
	int ptFlat;

	@CommandLine.Option(names = "--cityWidePtFlat", defaultValue = "false", description = "measures to allow every resident in Gladbeck to have free pt")
	boolean cityWidePtFlat;

	@CommandLine.Option(names = "--cyclingCourse", defaultValue = "false", description = "measures to increase the ")
	boolean cyclingCourse;

	@CommandLine.Option(names = "--klimaTaler", defaultValue = "0.0", description = "amount of money to give to a person to use pt, walk and bike")
	double klimaTalerMoneyAmount;

    @CommandLine.Option(names = {"--policy", "--p"})
    private Set<BicyclePolicies.Policy> policies = new HashSet<>();

    @CommandLine.Option(names = {"--bicycle-freespeed", "--bf"})
    private double bicycleFreedspeed = 6.82; // taken from vehicles file in metropole-ruhr-scenario https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.0.mode-vehicles.xml

    //added this for the test
    public RunGladbeckScenario(@Nullable Config config) {
        super(config);
    }

	public RunGladbeckScenario() {
		super(String.format("./scenarios/gladbeck-v2.0/input/gladbeck-%s-10pct.config.xml", VERSION));
	}

	public static void main(String[] args) {
		MATSimApplication.run(RunGladbeckScenario.class, args);
	}

    @Override
    protected Config prepareConfig(Config config) {

        if (!policies.isEmpty() && !shp.isDefined()) {
            throw new RuntimeException("A geo filter is required to apply policy changes to the network. Please add a path to a shape file by using the --shp option");
        }

        // Always switch off intermodal
        this.intermodal = false;

		// so we don´t use the rvr accessEgressModeToLinkPlusTimeConstant
		config.plansCalcRoute().setAccessEgressType(PlansCalcRouteConfigGroup.AccessEgressType.accessEgressModeToLink);

		if (scenarioWidePtFlat) {
			config.planCalcScore().getModes().get(TransportMode.pt).setDailyMonetaryConstant(0.0);
		}

		// this is needed for the school closure case
		config.network().setTimeVariantNetwork(true);
		return super.prepareConfig(config);
	}

	@Override
	protected void prepareScenario(Scenario scenario) {
		super.prepareScenario(scenario);

		if (slowSpeedZone) {
			ReduceSpeed.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shp.getShapeFile().toString())));
		}

		if (slowSpeedOnDefinedLinks) {
			ReduceSpeed.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shp.getShapeFile().toString())));
		}

		if (schoolClosure) {
			List<Id<Link>> listOfSchoolLinks = new ArrayList<>();

			// street in front of Mosaikschule
			listOfSchoolLinks.add(Id.createLinkId("353353080004r"));
			listOfSchoolLinks.add(Id.createLinkId("353353080004f"));



			new SchoolRoadsClosure().closeSchoolLinks(listOfSchoolLinks, scenario.getNetwork(), 800, 1700);
		}

        if (cyclingCourse) {
            log.info("adding different citizenship's to the agents");
            AssignPersonAttributes.assigningDifferentCitizenship(scenario, shp);
        }

        if (!policies.isEmpty()) {
			//changing the network for the policy
            BicyclePolicies.applyPolicyChanges(scenario.getNetwork(), shp.getGeometry(), policies, bicycleFreedspeed);
            //delete routes from plans and linkId and facility id from activity
			for (var person : scenario.getPopulation().getPersons().values()) {
				var plan = person.getSelectedPlan();
				person.getPlans().clear();
				PopulationUtils.resetRoutes(plan);
				for (var element : plan.getPlanElements()) {
					if (element instanceof Activity act) {
						act.setFacilityId(null);
						act.setLinkId(null);
					}
				}
				person.addPlan(plan);
			}
        }
    }

	@Override
	protected void prepareControler(Controler controler) {

		//controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);
		if (klimaTalerMoneyAmount != 0.0) {
			log.info("add Klima taler with money amount: " + klimaTalerMoneyAmount);
			KlimaTaler klimaTaler = new KlimaTaler(controler.getScenario().getConfig().plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk), controler.getScenario().getNetwork(), klimaTalerMoneyAmount);
			addKlimaTaler(controler, klimaTaler);
		}

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(MultimodalLinkChooser.class).to(NearestLinkChooser.class);
            }
        });


		if (ptFlat !=0 || cityWidePtFlat) {

            List<Id<Person>> agentsLivingInGladbeck = new ArrayList<>();
			List<Id<Person>> agentsWithPtFlat = new ArrayList<>();
			HomeLocationFilter homeLocationFilter = new HomeLocationFilter(shp, controler.getScenario().getConfig().global().getCoordinateSystem(), controler.getScenario().getPopulation());

			for (Person person: controler.getScenario().getPopulation().getPersons().values())  {
				if (homeLocationFilter.test(controler.getScenario().getPopulation().getPersons().get(person.getId()))) {
					agentsLivingInGladbeck.add(person.getId());
				}
			}


			if (cityWidePtFlat) {
				agentsWithPtFlat.addAll(agentsLivingInGladbeck);
			} else {
				for (int ii= 0; ii < ptFlat; ii++) {
					Random generator = MatsimRandom.getRandom();
					Object[] values = agentsLivingInGladbeck.toArray();
					var randomPerson = (Id<Person>) values[generator.nextInt(values.length)];
					agentsWithPtFlat.add(randomPerson);
					agentsLivingInGladbeck.remove(randomPerson);
				}
			}
			log.info("adding pt flat." + agentsWithPtFlat.size() +" agents will pay no pt cost");
            try {
                writeOutAgents(agentsWithPtFlat);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            addPtFlat(controler, new PtFlatrate(agentsWithPtFlat, controler.getConfig().planCalcScore().getModes().get(TransportMode.pt).getDailyMonetaryConstant()));

		}
		super.prepareControler(controler);
	}

	public static void addKlimaTaler(Controler controler, KlimaTaler klimaTaler) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(klimaTaler);
				addControlerListenerBinding().toInstance(klimaTaler);
				new PersonMoneyEventsAnalysisModule();
			}
		});
	}

	public static void addPtFlat(Controler controler, PtFlatrate ptFlatrate) {
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(ptFlatrate);
				addControlerListenerBinding().toInstance(ptFlatrate);
				new PersonMoneyEventsAnalysisModule();
			}
		});
	}

	private static void writeOutAgents(List<Id<Person>> listOfIds) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter("agentsWithFreePt.tsv");

		writer.write("Id");
		writer.newLine();
        for (Id<Person> listOfId : listOfIds) {
            writer.write(listOfId.toString());
            writer.newLine();
        }
		writer.close();
	}
}
