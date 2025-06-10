package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.matsim.run.RunGladbeckScenario.addKlimaTaler;

public class TestKlimaTaler {

    @RegisterExtension
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runKlimaTalerBikeTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output/KlimaTalerBikeTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("10"));
        scenario.getPopulation().getPersons().clear();
        scenario.getPopulation().addPerson(person);
        Controler controler = new Controler(scenario);

        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(scenario.getNetwork(), 10.0);

        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator, config, 10.0);
        KlimaTalerTestListener handler = new KlimaTalerTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });
        controler.run();

        PersonMoneyEvent event = handler.klimaTalerBikeMoneyEvents.iterator().next();
        assertEquals("10", event.getPersonId().toString(), "wrong person");
        assertEquals(31.679999999999996, event.getAmount(), 0., "wrong amount");
    }


    @Test
    public final void runKlimaTalerPtTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output/KlimaTalerPtTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);


        KlimaTaler klimaTaler = new KlimaTaler(scenario.getNetwork(), 10.0);
        addKlimaTaler(controler, klimaTaler, config, 10.0);


        controler.run();

        Scenario testForReading = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(testForReading).readFile(config.controller().getOutputDirectory() + "output_plans.xml.gz");
        //Score without klima Taler is 27.468448990195423
        Assert.isTrue(testForReading.getPopulation().getPersons().get(Id.createPersonId("1")).getSelectedPlan().getScore() > 27.468448990195423);
    }

    @Test
    public final void runKlimaTalerWalkTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("chessboard"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controller().setLastIteration(0);
        config.controller().setOutputDirectory("output/KlimaTalerWalkTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        Population population = scenario.getPopulation();
        createWalkingAgent(population);
        Controler controler = new Controler(scenario);

        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(scenario.getNetwork(), 10.0);
        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator, config, 10.0);

        controler.run();

        Scenario testForReading = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new PopulationReader(testForReading).readFile(config.controller().getOutputDirectory() + "output_plans.xml.gz");

        //Score without klima Taler is 29.460006520575348
        Assert.isTrue(testForReading.getPopulation().getPersons().get(Id.createPersonId("walkingAgent")).getSelectedPlan().getScore() > 29.460006520575348);

    }


    final void createWalkingAgent(Population population) {
        population.getPersons().clear();
        PopulationFactory factory = population.getFactory();
        Leg walkLeg = factory.createLeg(TransportMode.walk);
        Person walkingAgent = factory.createPerson(Id.createPersonId("walkingAgent"));
        Plan plan1 = factory.createPlan();
        Activity home = factory.createActivityFromLinkId(ActivityTypes.HOME, Id.createLinkId("80"));
        home.setEndTime(9000);
        plan1.addActivity(home);
        plan1.addLeg(walkLeg);
        Activity education = factory.createActivityFromLinkId(ActivityTypes.WORK, Id.createLinkId("81"));
        education.setEndTime(1800);
        plan1.addActivity(education);
        walkingAgent.addPlan(plan1);
        population.addPerson(walkingAgent);
    }

    class KlimaTalerTestListener implements PersonMoneyEventHandler {

        Set<PersonMoneyEvent> klimaTalerBikeMoneyEvents = new HashSet<>();
        Set<PersonMoneyEvent> klimaTalerPtMoneyEvents = new HashSet<>();
        Set<PersonMoneyEvent> klimaTalerWalkMoneyEvents = new HashSet<>();

        @Override
        public void handleEvent(PersonMoneyEvent event) {
            if (event.getPurpose().equals("klimaTalerForBike") && event.getAmount() > 0.)
                this.klimaTalerBikeMoneyEvents.add(event);
            else if (event.getPurpose().equals("klimaTalerForPt") && event.getAmount() > 0.)
                this.klimaTalerPtMoneyEvents.add(event);
            else if (event.getPurpose().equals("klimaTalerForWalk") && event.getAmount() > 0.)
                this.klimaTalerWalkMoneyEvents.add(event);
        }

        @Override
        public void reset(int iteration) {
            PersonMoneyEventHandler.super.reset(iteration);
            this.klimaTalerPtMoneyEvents.clear();
            this.klimaTalerWalkMoneyEvents.clear();
            this.klimaTalerBikeMoneyEvents.clear();
        }
    }
}
