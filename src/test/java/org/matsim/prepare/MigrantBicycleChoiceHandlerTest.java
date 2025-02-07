package org.matsim.prepare;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;

public class MigrantBicycleChoiceHandlerTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runTestMigrantBicycleChoiceHandler() {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));

        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("10"));
        person.getAttributes().putAttribute("migrant", "true");

        scenario.getPopulation().getPersons().clear();
        scenario.getPopulation().addPerson(person);
        Controler controler = new Controler(scenario);

        List<Id<Person>> migrants = new ArrayList<>();
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

        Assert.assertTrue("Score of migrant was not changed! It should be negative but it is:" + person.getSelectedPlan().getScore(), person.getSelectedPlan().getScore() < 0);
    }
}