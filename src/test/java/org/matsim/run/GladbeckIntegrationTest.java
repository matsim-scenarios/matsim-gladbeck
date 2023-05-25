package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimApplication;
import org.matsim.application.options.SampleOptions;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

public class GladbeckIntegrationTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runExamplePopulationTest() {
        Config config = ConfigUtils.loadConfig("scenarios/gladbeck-v1.0/input/gladbeck-v1.0-10pct.config.xml");
        config.controler().setLastIteration(1);
        SampleOptions sample = new SampleOptions(1);
        config.controler().setOutputDirectory(sample.adjustName(config.controler().getOutputDirectory()));
        config.controler().setRunId(sample.adjustName(config.controler().getRunId()));
        //config.plans().setInputFile(sample.adjustName(config.plans().getInputFile()));
        config.qsim().setFlowCapFactor(sample.getSize() / 100.0);
        config.qsim().setStorageCapFactor(sample.getSize() / 100.0);
        PopulationUtils.sampleDown(PopulationUtils.readPopulation(config.plans().getInputFile()), 0.01);
       /* PopulationUtils.writePopulation(PopulationUtils.readPopulation(config.plans().getInputFile()),"reducedPop.xml.gz");
        config.plans().setInputFile("reducedPop.xml.gz");*/
        config.controler()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
        MATSimApplication.execute(RunGladbeckScenario.class, config,
                "run");
    }
}