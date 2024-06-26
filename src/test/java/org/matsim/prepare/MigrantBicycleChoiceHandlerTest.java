package org.matsim.prepare;

import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import org.junit.Test;
import org.matsim.analysis.ModeChoiceCoverageControlerListener;
import org.matsim.analysis.linkpaxvolumes.LinkPaxVolumesAnalysisModule;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.analysis.pt.stop2stop.PtStop2StopAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.Bicycles;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.examples.ExamplesUtils;
import org.matsim.extensions.pt.fare.intermodalTripFareCompensator.IntermodalTripFareCompensatorsModule;
import org.matsim.extensions.pt.routing.EnhancedRaptorIntermodalAccessEgress;
import org.matsim.extensions.pt.routing.ptRoutingModes.PtIntermodalRoutingModesModule;
import org.matsim.run.IntermodalPtAnalysisModeIdentifier;
import org.matsim.run.NearestLinkChooser;
import org.matsim.run.StrategyWeightFadeout;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import playground.vsp.simpleParkingCostHandler.ParkingCostConfigGroup;
import playground.vsp.simpleParkingCostHandler.ParkingCostModule;

public class MigrantBicycleChoiceHandlerTest {

    String migrantPopulationPath = "gladbeck-v1.3-10pct.migrants-mapped.xml.gz"; // TODO Adjust path

    private void prepareConfig(Config config){}

    private void prepareScenario(Scenario scenario){}

    private void prepareControler(Controler controler){}

    @Test
    public void test2(){
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        {
            config.controler().setLastIteration(3);
            config.controler().setOutputDirectory("output/MigrantBicycleTest/");
            config.global().setNumberOfThreads(1);
            config.qsim().setNumberOfThreads(1);
            config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
            //bike contrib
            BicycleConfigGroup bikeConfigGroup = (BicycleConfigGroup)ConfigUtils.addOrGetModule(config, BicycleConfigGroup.class);
            bikeConfigGroup.setBicycleMode("bike");
            config.changeMode().setModes(new String[]{"car", "pt", "bicycle"});

            //Set the replanning strategy for migrants: All migrants will fully reroute their plan
            StrategyConfigGroup.StrategySettings migrantStrategySettings = new StrategyConfigGroup.StrategySettings(ConfigUtils.createAvailableStrategyId(config));
            migrantStrategySettings.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ChangeTripMode);
            migrantStrategySettings.setSubpopulation("migrant");
            migrantStrategySettings.setWeight(1);
            config.strategy().addStrategySettings(migrantStrategySettings);
        }

        Scenario scenario = ScenarioUtils.loadScenario(config);
        //Person person = scenario.getPopulation().getPersons().get(Id.createPersonId("10"));
        //scenario.getPopulation().getPersons().clear();
        //scenario.getPopulation().addPerson(person);
        Person migrant = scenario.getPopulation().getPersons().get(Id.createPersonId("1"));
        migrant.getAttributes().putAttribute("subpopulation", "migrant");

        Controler controler = new Controler(scenario);
        {
            controler.addOverridingModule(new AbstractModule() {
                public void install() {
                    this.addTravelTimeBinding("ride").to(this.networkTravelTime());
                    this.addTravelDisutilityFactoryBinding("ride").to(this.carTravelDisutilityFactoryKey());
                    this.addTravelTimeBinding("bike").to(this.networkTravelTime());
                    this.addControlerListenerBinding().to(ModeChoiceCoverageControlerListener.class);
                    this.addControlerListenerBinding().to(StrategyWeightFadeout.class).in(Singleton.class);
                    Multibinder<StrategyWeightFadeout.Schedule> schedules = Multibinder.newSetBinder(this.binder(), StrategyWeightFadeout.Schedule.class);
                    schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule("SubtourModeChoice", "person", 0.75, 0.85));
                    schedules.addBinding().toInstance(new StrategyWeightFadeout.Schedule("ReRoute", "person", 0.78));
                }
            });
            Bicycles.addAsOverridingModule(controler);
        }

        MigrantBicycleChoiceHandler choiceHandler = new MigrantBicycleChoiceHandler(scenario.getPopulation());

        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(choiceHandler);
                addControlerListenerBinding().toInstance(choiceHandler);
            }
        });
        controler.run();

        //Migrant should now have bike as the best plan

        System.out.println();
    }
}