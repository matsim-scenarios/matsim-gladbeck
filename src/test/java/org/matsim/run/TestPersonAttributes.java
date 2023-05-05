package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.locationtech.jts.util.Assert;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.prepare.AssignPersonAttributes;
import org.matsim.testcases.MatsimTestUtils;

public class TestPersonAttributes {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runTestPersonAttributes() {
        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("equil-mixedTraffic"));
        Config config = ConfigUtils.loadConfig(inputPath + "config-with-mode-vehicles.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/TestPersonAttributes/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Scenario scenario = ScenarioUtils.loadScenario(config);
        AssignPersonAttributes.assigningDifferentCitizenship(scenario, null);

        scenario.getPopulation().getPersons();
        int count = 0;
        for (Person p: scenario.getPopulation().getPersons().values()) {
            if( p.getAttributes().getAttribute("citizenship") != null) {
                count++;
            }
        }
        int correctNr = (int) Math.floor(scenario.getPopulation().getPersons().size() * 0.19);
        Assert.equals(correctNr, count);
    }
}
