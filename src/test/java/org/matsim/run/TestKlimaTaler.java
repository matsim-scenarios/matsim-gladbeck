package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
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
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class TestKlimaTaler {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runKlimaTalerBikeTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/KlimaTalerBikeTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk), scenario.getNetwork());
        Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("10"));
        scenario.getPopulation().getPersons().clear();
        scenario.getPopulation().addPerson(person);
        Controler controler = new Controler(scenario);
        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator);
        KlimaTalerTestListener handler = new KlimaTalerTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });
        controler.run();


        PersonMoneyEvent event = handler.klimaTalerBikeMoneyEvents.iterator().next();
        Assert.assertEquals("wrong person", "10", event.getPersonId().toString() );
        Assert.assertEquals("wrong amount", 316.79999999999995 , event.getAmount(), 0. );
    }


    @Test
    public final void runKlimaTalerPtTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/KlimaTalerPtTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        KlimaTaler klimaTaler = new KlimaTaler(config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk), scenario.getNetwork());
        addKlimaTaler(controler, klimaTaler);
        KlimaTalerTestListener handler = new KlimaTalerTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });

        controler.run();

        //Assert that money amount is correct
        PersonMoneyEvent event = handler.klimaTalerPtMoneyEvents.iterator().next();
        Assert.assertEquals("wrong person", "1", event.getPersonId().toString() );
        Assert.assertEquals("wrong amount", 45.751999999999995 , event.getAmount(), 0. );
    }

    @Test
    public final void runKlimaTalerWalkTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("chessboard"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/KlimaTalerWalkTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.walk);
        for (Link l : scenario.getNetwork().getLinks().values()) {
            l.setAllowedModes(modes);
        }
        Population population = scenario.getPopulation();
        createWalkingAgent(population);
        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk), scenario.getNetwork());
        Controler controler = new Controler(scenario);
        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator);
        KlimaTalerTestListener handler = new KlimaTalerTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });
        controler.run();
        //Assert that money amount is correct
        PersonMoneyEvent event = handler.klimaTalerWalkMoneyEvents.iterator().next();
        Assert.assertEquals("wrong person", "walkingAgent", event.getPersonId().toString() );
        Assert.assertEquals("wrong amount", 4.576 , event.getAmount(), 0. );

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
        //Assert that money amount is correct
    }

    class KlimaTalerTestListener implements PersonMoneyEventHandler {

        Set<PersonMoneyEvent> klimaTalerBikeMoneyEvents = new HashSet<>();
        Set<PersonMoneyEvent> klimaTalerPtMoneyEvents = new HashSet<>();
        Set<PersonMoneyEvent> klimaTalerWalkMoneyEvents = new HashSet<>();

        @Override
        public void handleEvent(PersonMoneyEvent event) {
            System.out.println("hallo");
            System.out.println(event.getPurpose());
            System.out.println(event.getAmount());
            if (event.getPurpose().equals("klimaTalerForBike") && event.getAmount() > 0.) this.klimaTalerBikeMoneyEvents.add(event);
            else if (event.getPurpose().equals("klimaTalerForPt") && event.getAmount() > 0.) this.klimaTalerPtMoneyEvents.add(event);
            else if (event.getPurpose().equals("klimaTalerForWalk") && event.getAmount() > 0.) this.klimaTalerWalkMoneyEvents.add(event);
            System.out.println(klimaTalerBikeMoneyEvents.size());
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
