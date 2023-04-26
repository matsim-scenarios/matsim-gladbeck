package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.testcases.MatsimTestUtils;

import java.io.IOException;

import static org.matsim.run.TestKlimaTalerWalk.addKlimaTaler;

public class TestPtKlimaTaler {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runPtKlimaTalerTest() throws IOException {

        String inputPath = String.valueOf(ExamplesUtils.getTestScenarioURL("pt-simple-lineswitch"));
        Config config = ConfigUtils.loadConfig(inputPath + "config.xml");
        config.controler().setLastIteration(0);
        config.controler().setOutputDirectory("output/KlimaTalerPtTest/");
        config.global().setNumberOfThreads(1);
        config.qsim().setNumberOfThreads(1);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        Controler controler = new Controler(scenario);
        KlimaTaler klimaTaler = new KlimaTaler(1.0,scenario.getNetwork());
        addKlimaTaler(controler, klimaTaler);
        controler.addOverridingModule(new PersonMoneyEventsAnalysisModule());
        controler.run();
    }
}
