package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.prepare.GladbeckUtils;
import org.matsim.prepare.MigrantBicycleChoiceHandler;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.testcases.MatsimTestUtils;
import playground.vsp.openberlinscenario.cemdap.output.ActivityTypes;
import scala.util.parsing.combinator.testing.Str;

import java.io.IOException;
import java.util.*;

import static org.matsim.run.RunGladbeckScenario.addCyclingMigrants;
import static org.matsim.run.RunGladbeckScenario.addKlimaTaler;

public class TestMigrationScoring {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runKlimaTalerBikeTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/MigrantTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        createMigrantAgent(scenario.getPopulation());

        Controler controler = new Controler(scenario);

        MigrantBicycleChoiceHandler myMigrantChoiceHandler = new MigrantBicycleChoiceHandler(scenario.getPopulation());
        addCyclingMigrants(controler, myMigrantChoiceHandler);
        MigrantTestListener handler = new MigrantTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });
        controler.run();


        List<String> listOfIds = List.of("6", "7", "8", "9", "10");
        Assert.assertEquals("wrong amout of agents",10, handler.migrantEvents.size());
        for (PersonScoreEvent event: handler.migrantEvents) {
            if (!listOfIds.contains(event.getPersonId().toString())) {
               Assert.fail(event.getPersonId().toString() + " Should not be included!");
            }

        }

    }
    @Test
    public void testModeChoice() throws IOException {

        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(60);
        config.controler().setOutputDirectory("output/MigrantTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.changeMode().setModes(new String[]{TransportMode.car, "bicycle"});

        StrategyConfigGroup.StrategySettings myNewStraSetting = new StrategyConfigGroup.StrategySettings();
        myNewStraSetting.setStrategyName("ChangeTripMode");
        myNewStraSetting.setWeight(1.0);
        myNewStraSetting.setDisableAfter(40);

        config.strategy().clearStrategySettings();
        config.strategy().addStrategySettings(myNewStraSetting);

        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        createMigrantAgent(scenario.getPopulation());

        Controler controler = new Controler(scenario);

        MigrantBicycleChoiceHandler myMigrantChoiceHandler = new MigrantBicycleChoiceHandler(scenario.getPopulation());
        //addCyclingMigrants(controler, myMigrantChoiceHandler);
        MigrantTestListener handler = new MigrantTestListener();
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(handler);
            }
        });
        controler.run();
    }


    final void createMigrantAgent(Population population) {
       for (Person person: population.getPersons().values()) {
           person.getAttributes().putAttribute(GladbeckUtils.MIGRANT, true);
       }
    }

    class MigrantTestListener implements PersonScoreEventHandler {

        Set<PersonScoreEvent> migrantEvents = new HashSet<>();

        @Override
        public void handleEvent(PersonScoreEvent event) {
            System.out.println("MigrantTestListener: " + event);

            if (event.getKind().equals("punishment_for_cycling"))  {
                migrantEvents.add(event);
            }
        }

        @Override
        public void reset(int iteration) {
            PersonScoreEventHandler.super.reset(iteration);
            this.migrantEvents.clear();
        }

    }
}
