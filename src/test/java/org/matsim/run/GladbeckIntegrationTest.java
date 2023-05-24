package org.matsim.run;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.application.MATSimApplication;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
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


    private static Population createTestPopulation(Config config) {

        Network network = NetworkUtils.readNetwork(config.network().getInputFile());

        Population population = PopulationUtils.createPopulation(config);


        Person carPerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("carPerson"));
        Person bikePerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("bikePerson"));
        Person walkPerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("walkPerson"));


        PopulationFactory factory = population.getFactory();
        Leg carLeg = factory.createLeg(TransportMode.car);
        Leg bikeLeg = factory.createLeg(TransportMode.bike);
        Leg walkLeg = factory.createLeg(TransportMode.walk);
        Link originLink = network.getLinks().get(Id.createLinkId("80"));
        Link destinationLink = network.getLinks().get(Id.createLinkId("81"));

        return population;
    }


}

