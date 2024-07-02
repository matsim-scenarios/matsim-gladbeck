package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.prepare.MigrantBicycleChoiceHandler;
import org.matsim.prepare.MigrantMapper;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;


public class TestMigrationMapper {


    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runTestMigrationMapper() {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(3);
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("10"));
        //TODO use migrant mapper class?
        //TODO I do
        person.getAttributes().putAttribute("migrant", "true");

        scenario.getPopulation().getPersons().clear();
        scenario.getPopulation().addPerson(person);
        Controler controler = new Controler(scenario);

        List migrants = new ArrayList<Id<Person>>();
        migrants.add(person.getId());

        MigrantBicycleChoiceHandler migrantBicycleChoiceHandler = new MigrantBicycleChoiceHandler(migrants);

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(migrantBicycleChoiceHandler);
                addControlerListenerBinding().toInstance(migrantBicycleChoiceHandler);
                new PersonMoneyEventsAnalysisModule();
            }
        });

        controler.run();
    }

}
