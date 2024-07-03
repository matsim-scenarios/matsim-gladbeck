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
        // TODO
    }

}
