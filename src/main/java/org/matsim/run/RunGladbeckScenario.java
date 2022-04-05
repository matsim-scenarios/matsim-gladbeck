package org.matsim.run;

import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.prepare.AssignIncome;
import org.matsim.prepare.CreateQuickGladbeckScenario;
import org.matsim.prepare.ShapeFileUtils;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;

import java.util.HashSet;
import java.util.Set;

public class RunGladbeckScenario {

    private static final Logger log = Logger.getLogger(RunGladbeckScenario.class);

    public static void main(String args []) {
        MATSimApplication.run(RunGladbeckScenario.RunGladbeckApplication.class, args);
    }

    public static class RunGladbeckApplication extends RunMetropoleRuhrScenario {

        @Override
        public Config prepareConfig(Config config) {

            var preparedConfig = super.prepareConfig(config);
            log.info("changing config");
            preparedConfig.controler().setLastIteration(0);
            preparedConfig.network().setInputFile("/Users/gregorr/Desktop/TestGlaMoBi/output_metropole-ruhr-v1.0-10pct/metropole-ruhr-v1.0-10pct.output_network.xml.gz");
            preparedConfig.plans().setInputFile("/Users/gregorr/Desktop/TestGlaMoBi/output_metropole-ruhr-v1.0-10pct/metropole-ruhr-v1.0-10pct.output_plans.xml.gz");
            return preparedConfig;
        }

        @Override
        protected void prepareScenario(Scenario scenario) {
            super.prepareScenario(scenario);
            AssignIncome.assignIncomeToPersonSubpopulationAccordingToGermanyAverage(scenario.getPopulation());
        }

        @Override
        protected void prepareControler(Controler controler) {
            super.prepareControler(controler);
            //use income-dependent marginal utility of money for scoring
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    bind(ScoringParametersForPerson.class).to(IncomeDependentUtilityOfMoneyPersonScoringParameters.class).in(Singleton.class);
                }
            });
        }
    }

}
