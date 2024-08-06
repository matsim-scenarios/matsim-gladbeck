package org.matsim.run;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestSchoolClosure {

    @RegisterExtension
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void testSchoolClosure() {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("chessboard"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setOutputDirectory(testUtils.getOutputDirectory());
        config.controller().setRunId("testSchool");
        config.controller().setLastIteration(0);
        config.network().setTimeVariantNetwork(true);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        List<Id<Link>> linksToClose = new ArrayList<>();
        linksToClose.add(Id.createLinkId("15"));
        linksToClose.add(Id.createLinkId("12"));
        new SchoolRoadsClosure().closeSchoolLinks(linksToClose, scenario.getNetwork(),630, 1330);
        Controler controler = new Controler(scenario);
        controler.run();

        // load output
        Population population = PopulationUtils.readPopulation(testUtils.getOutputDirectory() + config.controller().getRunId() + ".output_plans.xml.gz");

        var personWithFreeRoad = population.getPersons().get(Id.createPersonId("4"));
        var personWithoutFreeRoad =  population.getPersons().get(Id.createPersonId("1"));
        assertTrue(personWithFreeRoad.getSelectedPlan().getScore() > personWithoutFreeRoad.getSelectedPlan().getScore());
    }


}
