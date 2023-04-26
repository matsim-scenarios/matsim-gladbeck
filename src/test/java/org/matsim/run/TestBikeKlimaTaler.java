package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.Bicycles;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;


public class TestBikeKlimaTaler {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runKlimaTalerEquillTest() throws IOException {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/KlimaTalerBikeTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        KlimaTaler teleportedModeTravelDistanceEvaluator = new KlimaTaler(1.0,scenario.getNetwork());

        Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("10"));
        scenario.getPopulation().getPersons().clear();
        scenario.getPopulation().addPerson(person);

        Controler controler = new Controler(scenario);
        addKlimaTaler(controler, teleportedModeTravelDistanceEvaluator);
        controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
        controler.run();
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
