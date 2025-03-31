package org.matsim.run;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

@Ignore
public class GladbeckIntegrationTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runExamplePopulationTest()  {

        MATSimApplication.execute(TestApplication.class);
    }


    private static Population createTestPopulation(Scenario scenario) {

        Network network = NetworkUtils.readNetwork(scenario.getConfig().network().getInputFile());
        Population population = PopulationUtils.createPopulation(scenario.getConfig());
        Person carPerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("carPerson"));
        Person bikePerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("bikePerson"));
        Person walkPerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("walkPerson"));

        walkPerson.getAttributes().putAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME, 1.0);
        carPerson.getAttributes().putAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME, 1.0);
        bikePerson.getAttributes().putAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME, 1.0);

        PopulationFactory factory = population.getFactory();
        Leg carLeg = factory.createLeg(TransportMode.car);
        Leg bikeLeg = factory.createLeg(TransportMode.bike);
        Leg walkLeg = factory.createLeg(TransportMode.walk);
        Link originLink = network.getLinks().get(Id.createLinkId("3242224840001r"));
        Link destinationLink = network.getLinks().get(Id.createLinkId("254602040001r"));
        Plan planForCar = factory.createPlan();
        Plan planForBike = factory.createPlan();
        Plan planForWalk = factory.createPlan();

        Activity originActivity = factory.createActivityFromCoord("home_600", originLink.getCoord());

        Activity destinationActivity = factory.createActivityFromCoord("leisure_600", destinationLink.getCoord());
        originActivity.setEndTime(3600.);

        planForCar.addActivity(originActivity);
        planForCar.addLeg(carLeg);
        planForCar.addActivity(destinationActivity);
        carPerson.addPlan(planForCar);

        planForBike.addActivity(originActivity);
        planForBike.addLeg(bikeLeg);
        planForBike.addActivity(destinationActivity);
        bikePerson.addPlan(planForBike);

        planForWalk.addActivity(originActivity);
        planForWalk.addLeg(walkLeg);
        planForWalk.addActivity(destinationActivity);
        walkPerson.addPlan(planForWalk);

        PopulationUtils.putSubpopulation(walkPerson, "person");
        PopulationUtils.putSubpopulation(bikePerson, "person");
        PopulationUtils.putSubpopulation(carPerson, "person");

        scenario.getPopulation().addPerson(walkPerson);
        scenario.getPopulation().addPerson(bikePerson);
        scenario.getPopulation().addPerson(carPerson);

        return population;
    }

    public static class TestApplication extends RunGladbeckScenario {

        @Override
        protected Config prepareConfig(Config config) {
            config = super.prepareConfig(config);
            config.controler().setLastIteration(1);
            config.plans().setInputFile(null);
            return config;
        }

        @Override
        protected void prepareScenario(Scenario scenario) {
            super.prepareScenario(scenario);
            scenario.getPopulation().getPersons().clear();
            createTestPopulation(scenario);
        }

        @Override
        protected void prepareControler(Controler controler) {
            super.prepareControler(controler);
        }

    }


}
