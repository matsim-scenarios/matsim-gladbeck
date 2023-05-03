package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.testcases.MatsimTestUtils;

public class GladbeckIntegrationTest {
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void runExamplePopulationTest() {
        Config config = ConfigUtils.loadConfig("scenarios/gladbeck-v1.0/input/gladbeck-v1.0-10pct.config.xml");
        config.controler().setLastIteration(1);
        //config.global().setNumberOfThreads(1);
        //config.qsim().setNumberOfThreads(1);
        config.controler()
                .setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        MATSimApplication.execute(RunGladbeckScenario.class,
                "run", "--10pct");
    }
}