package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.prepare.AssignIncome;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestPtFlat {

    private static final Id<Person> personId = Id.createPersonId("test-person");
    private static final String inputNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.0.network_resolutionHigh-with-pt.xml.gz";

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void testPtFlat() {

        var outputDir = testUtils.getOutputDirectory();

        MATSimApplication.execute(TestApplication.class, "--output=" + outputDir + "withPtFlat", "--ptFlat=true", "--download-input", "--1pct", "--config:network.inputNetworkFile=" + inputNetworkFile);
        MATSimApplication.execute(TestApplication.class, "--output=" + outputDir + "withoutPtFlat", "--ptFlat=false", "--download-input", "--1pct", "--config:network.inputNetworkFile=" + inputNetworkFile);

        // load output of both runs
        var scenarioWithElevation = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenarioWithElevation).readFile(outputDir + "withPtFlat/" + TestApplication.RUN_ID + ".output_plans.xml.gz");

        var scenarioWithoutElevation = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenarioWithoutElevation).readFile(outputDir + "withoutPtFlat/" + TestApplication.RUN_ID + ".output_plans.xml.gz");

        // somehow compare the two routes
        var personWithElevation = scenarioWithElevation.getPopulation().getPersons().get(personId);
        var personWithoutElevation = scenarioWithoutElevation.getPopulation().getPersons().get(personId);

        assertTrue(personWithElevation.getSelectedPlan().getScore() < personWithoutElevation.getSelectedPlan().getScore());

    }

    public static class TestApplication extends RunGladbeckScenario {

        private static final String RUN_ID = "TestApplication";

        @Override
        public Config prepareConfig(Config config) {
            Config preparedConfig = super.prepareConfig(config);
            preparedConfig.global().setNumberOfThreads(1);
            preparedConfig.qsim().setNumberOfThreads(1);
            preparedConfig.plans().setInputFile(null);
            //need multiple iteration for modeChoice
            preparedConfig.controler().setLastIteration(1);
            preparedConfig.controler().setRunId(RUN_ID);

            //adjusting strategy setting of config so agents try out different modes
            for (StrategyConfigGroup.StrategySettings setting:    preparedConfig.strategy().getStrategySettings()) {
                if (setting.getStrategyName().equals("ChangeSingleTripMode")) {
                    setting.setWeight(1.0);
                }
            }

            return preparedConfig;
        }

        @Override
        protected void prepareScenario(Scenario scenario) {
            //super.prepareScenario(scenario);
            // Other agents are not needed for the test
            scenario.getPopulation().getPersons().clear();
            // add single person with two activities
            var factory = scenario.getPopulation().getFactory();
            var plan = factory.createPlan();
            var homeCoord = scenario.getNetwork().getLinks().get( Id.createLinkId("431735990000f")).getCoord();
            var home = factory.createActivityFromCoord("home_600.0", homeCoord);
            home.setEndTime(0);
            plan.addActivity(home);
            var leg = factory.createLeg(TransportMode.car);
            leg.setMode(TransportMode.car);
            plan.addLeg(leg);
            var otherCoord = scenario.getNetwork().getLinks().get( Id.createLinkId("7339832750094r")).getCoord();
            var other = factory.createActivityFromCoord("other_3600.0",otherCoord);
            other.setEndTime(3600);
            plan.addActivity(other);
            var person = factory.createPerson(personId);
            person.addPlan(plan);
            scenario.getPopulation().addPerson(person);
            AssignIncome.assignIncomeToPersonSubpopulationAccordingToSNZData(scenario.getPopulation());
            for (Person p: scenario.getPopulation().getPersons().values()) {
                Plan planOfAgent = p.getSelectedPlan();
                List<Leg> legs = TripStructureUtils.getLegs(planOfAgent);
                //only car users are allowed to enjoy the pt flatrate
                for (Leg leg1: legs) {
                    if (leg1.getMode().equals(TransportMode.car)) {
                        personsEligibleForPtFlat.put(p.getId(),0);
                    }
                }
                //p.getAttributes().putAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME, 100.0);
            }

            super.prepareScenario(scenario);
        }

        @Override
        protected void prepareControler(Controler controler) {
            super.prepareControler(controler);
        }
    }

}