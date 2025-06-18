package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.FacilitiesConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.matsim.run.policies.SchoolRoadsClosure.SchoolClosure.testCase;

public class TestSchoolClosure {

    @RegisterExtension
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void testSchoolClosure() {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("chessboard"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setOutputDirectory(testUtils.getOutputDirectory());
        config.controller().setRunId("testSchool");
        config.controller().setLastIteration(0);
        config.network().setTimeVariantNetwork(true);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        createTestPopulation(scenario);

        List<Id<Link>> linksToClose = new ArrayList<>();
        linksToClose.add(Id.createLinkId("173"));
        linksToClose.add(Id.createLinkId("176"));
        new SchoolRoadsClosure().closeSchoolLinks(Collections.singleton(testCase), scenario.getNetwork(),10, 1330);
        Controler controler = new Controler(scenario);
        controler.run();

        // load output
        Population population = PopulationUtils.readPopulation(testUtils.getOutputDirectory() + config.controller().getRunId() + ".output_plans.xml.gz");

        var personWithFreeRoad = population.getPersons().get(Id.createPersonId("TestPersonWithFreeRoad"));
        var personWithoutFreeRoad =  population.getPersons().get(Id.createPersonId("TestPersonWithBlockedRoad"));
        assertTrue(personWithFreeRoad.getSelectedPlan().getScore() > personWithoutFreeRoad.getSelectedPlan().getScore());
    }


    private void createTestPopulation(Scenario scenario) {
        PopulationFactory populationFactory = scenario.getPopulation().getFactory();
        scenario.getPopulation().getPersons().clear();

        Person person = populationFactory.createPerson(Id.createPersonId("TestPersonWithFreeRoad"));
        Person person2 = populationFactory.createPerson(Id.createPersonId("TestPersonWithBlockedRoad"));


        Plan planForPersonWithFreeRoad = populationFactory.createPlan();
        Activity homeActivityForPersonWithFreeRoad = populationFactory.createActivityFromLinkId("home", Id.createLinkId("1"));
        FacilitiesUtils.wrapActivity(homeActivityForPersonWithFreeRoad);
        homeActivityForPersonWithFreeRoad.setEndTime(10);
        Activity leisureActivityForPersonWithFreeRoad = populationFactory.createActivityFromLinkId("work", Id.createLinkId("82"));
        leisureActivityForPersonWithFreeRoad.setEndTime(1000);
        FacilitiesUtils.wrapActivity(leisureActivityForPersonWithFreeRoad);

        planForPersonWithFreeRoad.addActivity(homeActivityForPersonWithFreeRoad);
        planForPersonWithFreeRoad.addLeg(populationFactory.createLeg(TransportMode.car));
        planForPersonWithFreeRoad.addActivity(leisureActivityForPersonWithFreeRoad);
        person.addPlan(planForPersonWithFreeRoad);


        Plan planForPersonWithBlockRoad = populationFactory.createPlan();
        Activity homeActivityForPersonWithBlockedRoad = populationFactory.createActivityFromLinkId("home", Id.createLinkId("9"));
        homeActivityForPersonWithBlockedRoad.setEndTime(10);
        FacilitiesUtils.wrapActivity(homeActivityForPersonWithBlockedRoad);
        Activity leisureActivityForPersonWithBlockedRoad = populationFactory.createActivityFromLinkId("work", Id.createLinkId("90"));
        leisureActivityForPersonWithBlockedRoad.setEndTime(1000);
        FacilitiesUtils.wrapActivity(leisureActivityForPersonWithBlockedRoad);

        planForPersonWithBlockRoad.addActivity(homeActivityForPersonWithBlockedRoad);
        planForPersonWithBlockRoad.addLeg(populationFactory.createLeg(TransportMode.car));
        planForPersonWithBlockRoad.addActivity(leisureActivityForPersonWithBlockedRoad);
        person2.addPlan(planForPersonWithBlockRoad);

        scenario.getPopulation().addPerson(person);
        scenario.getPopulation().addPerson(person2);
    }


}
