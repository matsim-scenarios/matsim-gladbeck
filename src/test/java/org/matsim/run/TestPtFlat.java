package org.matsim.run;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.assertTrue;

//test runs too long...
@Disabled
public class TestPtFlat {

    private static final Id<Person> personId = Id.createPersonId("test-person");
    private static final String inputNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/input/gladbeck-v1.0-10pct.network.xml.gz";

    @RegisterExtension
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void testPtFlat() {

        var outputDir = testUtils.getOutputDirectory();

        MATSimApplication.execute(TestApplication.class, "--output=" + outputDir + "withPtFlat", "--ptFlat", "1", "--1pct", "--config:network.inputNetworkFile=" + inputNetworkFile,
                "--shp", "/Users/gregorr/Documents/work/respos/shared-svn/projects/GlaMoBi/data/shp-files/Gladbeck.shp",
                "--shp-crs", "EPSG:25832"
        );
        MATSimApplication.execute(TestApplication.class, "--output=" + outputDir + "withoutPtFlat", "--1pct", "--config:network.inputNetworkFile=" + inputNetworkFile,
                "--shp", "/Users/gregorr/Documents/work/respos/shared-svn/projects/GlaMoBi/data/shp-files/Gladbeck.shp",
                "--shp-crs", "EPSG:25832"
                );

        // load output of both runs
        var scenarioWithPtFlat = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenarioWithPtFlat).readFile(outputDir + "withPtFlat/" + TestApplication.RUN_ID + ".output_plans.xml.gz");
        var scenarioWithoutPtFlat = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(scenarioWithoutPtFlat).readFile(outputDir + "withoutPtFlat/" + TestApplication.RUN_ID + ".output_plans.xml.gz");
        // somehow compare the two plans
        var unaffectedAgentScenario1 = scenarioWithPtFlat.getPopulation().getPersons().get(personId);
        var unaffectedAgentScenario2 = scenarioWithoutPtFlat.getPopulation().getPersons().get(personId);

        assertTrue(unaffectedAgentScenario1.getSelectedPlan().getScore().equals(unaffectedAgentScenario2.getSelectedPlan().getScore()));

        System.out.printf(scenarioWithPtFlat.getPopulation().getPersons().toString() + "\n");
        var agentWithPtFlat = scenarioWithPtFlat.getPopulation().getPersons().get(Id.createPersonId(personId+"inside"));
        var agentWithoutPtFlat = scenarioWithoutPtFlat.getPopulation().getPersons().get(Id.createPersonId(personId+"inside"));



        assertTrue(agentWithPtFlat.getSelectedPlan().getScore() > agentWithoutPtFlat.getSelectedPlan().getScore());
    }

    public static class TestApplication extends RunGladbeckScenario {
        private static final String RUN_ID = "TestApplication";

        @Override
        public Config prepareConfig(Config config) {
            Config preparedConfig = super.prepareConfig(config);
            //preparedConfig.global().setNumberOfThreads(1);
            //preparedConfig.qsim().setNumberOfThreads(1);
            preparedConfig.plans().setInputFile(null);
            preparedConfig.controller().setLastIteration(0);
            preparedConfig.controller().setRunId(RUN_ID);
            return preparedConfig;
        }

        @Override
        protected void prepareScenario(Scenario scenario) {
            // Other agents are not needed for the test
            scenario.getPopulation().getPersons().clear();
            // add single person with two activities living outside gladbeck
            var factory = scenario.getPopulation().getFactory();
            var plan = factory.createPlan();
            var homeCoord = scenario.getNetwork().getLinks().get( Id.createLinkId("pt_65711")).getCoord();
            var home = factory.createActivityFromCoord("home_600", homeCoord);
            home.setEndTime(50400);
            plan.addActivity(home);
            var leg = factory.createLeg(TransportMode.pt);
            leg.setMode(TransportMode.pt);
            plan.addLeg(leg);
            var otherCoord = scenario.getNetwork().getLinks().get( Id.createLinkId("pt_65377")).getCoord();
            var other = factory.createActivityFromCoord("other_3600",otherCoord);
            other.setEndTime(54000);
            plan.addActivity(other);
            var person = factory.createPerson(personId);
            person.addPlan(plan);
            person.getAttributes().putAttribute("subpopulation", "person");

            PersonUtils.setIncome(person, 1);

            // add single person with two activities living inside gladbeck
            var planInsider = factory.createPlan();
            var homeCoordInsider = scenario.getNetwork().getLinks().get(Id.createLinkId("pt_65455")).getCoord();
            var homeInsider = factory.createActivityFromCoord("home_600", homeCoordInsider);
            homeInsider.setEndTime(50400);
            planInsider.addActivity(homeInsider);
            var legInsider = factory.createLeg(TransportMode.pt);
            legInsider.setMode(TransportMode.pt);
            planInsider.addLeg(legInsider);
            var otherCoordInsider = scenario.getNetwork().getLinks().get( Id.createLinkId("pt_65377")).getCoord();
            var otherInsider = factory.createActivityFromCoord("other_3600",otherCoordInsider);
            otherInsider.setEndTime(54000);
            planInsider.addActivity(otherInsider);
            var personInsider = factory.createPerson(Id.createPersonId(personId+"inside"));
            personInsider.addPlan(planInsider);
            personInsider.getAttributes().putAttribute("subpopulation", "person");
            PersonUtils.setIncome(personInsider, 1);

            scenario.getPopulation().addPerson(personInsider);
            scenario.getPopulation().addPerson(person);
            super.prepareScenario(scenario);

        }

        @Override
        protected void prepareControler(Controler controler) {
            super.prepareControler(controler);
        }
    }

}