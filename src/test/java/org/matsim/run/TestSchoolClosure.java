package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestSchoolClosure {

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void testSchoolClosure() {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("chessboard"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());
        config.controler().setRunId("testSchool");
        config.controler().setLastIteration(0);
        config.network().setTimeVariantNetwork(true);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        List<Id<Link>> linksToClose = new ArrayList<>();
        linksToClose.add(Id.createLinkId("15"));
        linksToClose.add(Id.createLinkId("12"));
        new SchoolRoadsClosure().closeSchoolLinks(linksToClose, scenario.getNetwork(),630, 1330);
        Controler controler = new Controler(scenario);
        controler.run();

        // load output
        Population population = PopulationUtils.readPopulation(testUtils.getOutputDirectory() + config.controler().getRunId() + ".output_plans.xml.gz");

        var personWithFreeRoad = population.getPersons().get(Id.createPersonId("4"));
        var personWithoutFreeRoad =  population.getPersons().get(Id.createPersonId("1"));
        assertTrue(personWithFreeRoad.getSelectedPlan().getScore() > personWithoutFreeRoad.getSelectedPlan().getScore());
    }


}
