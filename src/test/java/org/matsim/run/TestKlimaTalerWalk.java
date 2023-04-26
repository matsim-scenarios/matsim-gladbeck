package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
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


public class TestKlimaTalerWalk {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runKlimaTalerChessboardTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("chessboard"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/KlimaTalerTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.walk);
        for (Link l: scenario.getNetwork().getLinks().values()) {
            l.setAllowedModes(modes);
        }
        Population population = scenario.getPopulation();
        createWalkingAgent(population);
        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(config.plansCalcRoute().getBeelineDistanceFactors().get(TransportMode.walk),scenario.getNetwork());
        Controler controler = new Controler(scenario);
        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator);
        controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
        controler.run();
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

    public static void addKlimaTaler(Controler controler, KlimaTaler klimaTaler) {
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(klimaTaler);
                addControlerListenerBinding().toInstance(klimaTaler);
            }
        });
    }
}
