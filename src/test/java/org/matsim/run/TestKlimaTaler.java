package org.matsim.run;

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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class TestKlimaTaler {

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
        PlanCalcScoreConfigGroup.ModeParams modeParamsBike = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.bike);
        config.planCalcScore().addModeParams(modeParamsBike);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Set<String> modes = new HashSet<>();
        modes.add(TransportMode.walk);
        modes.add(TransportMode.bike);
        modes.add(TransportMode.car);
        for (Link l: scenario.getNetwork().getLinks().values()) {
            l.setAllowedModes(modes);
        }
        Population population = scenario.getPopulation();
        createExampleParkingPopulation(population);
        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(2.0,scenario.getNetwork());
        Controler controler = new Controler(scenario);
        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator);
        controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
        controler.run();
    }



    final void createExampleParkingPopulation(Population population) {
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

        Leg bikeLeg = factory.createLeg(TransportMode.bike);
        Person bikeAgent = factory.createPerson(Id.createPersonId("bikeAgent"));
        Plan plan2 = factory.createPlan();
        home.setEndTime(9500);
        plan2.addActivity(home);
        plan2.addLeg(bikeLeg);
        education.setEndTime(1850);
        plan2.addActivity(education);
        bikeAgent.addPlan(plan2);
        population.addPerson(bikeAgent);
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
