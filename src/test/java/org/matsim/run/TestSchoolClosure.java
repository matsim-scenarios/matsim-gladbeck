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
import org.matsim.testcases.MatsimTestUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TestSchoolClosure {

    private static final String inputNetworkFile = "Downloads/network.xml";
    private static final String configFile = "Downloads/config.xml";
    private static final String plansFile = "Downloads/plans4.xml";

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Test
    public void testSchoolClosure() {

        Config config = ConfigUtils.loadConfig(configFile);
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setOutputDirectory(testUtils.getOutputDirectory());
        config.controler().setRunId("testSchool");
        config.plans().setInputFile(plansFile);
        config.network().setInputFile(inputNetworkFile);
        config.controler().setLastIteration(0);
        config.network().setTimeVariantNetwork(true);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        List<Id<Link>> linksToClose = new ArrayList<>();
        linksToClose.add(Id.createLinkId("15"));
        linksToClose.add(Id.createLinkId("12"));
        closeSchoolLinks(linksToClose, scenario.getNetwork(),630, 1330);
        Controler controler = new Controler(scenario);
        controler.run();

        // load output
        Population population = PopulationUtils.readPopulation(testUtils.getOutputDirectory() + config.controler().getRunId() + ".output_plans.xml.gz");

        var personWithFreeRoad = population.getPersons().get(Id.createPersonId("4"));
        var personWithoutFreeRoad =  population.getPersons().get(Id.createPersonId("1"));

        assertTrue(personWithFreeRoad.getSelectedPlan().getScore() > personWithoutFreeRoad.getSelectedPlan().getScore());

    }


    public void closeSchoolLinks(List<Id<Link>> linkList, Network network, int startTime, int endTime) {

        Collection<Link> links = new ArrayList<>();
        HashMap<Link, Double > oldValues = new HashMap<>();
        ArrayList<NetworkChangeEvent> listOfNetworkChangeEvents = new ArrayList<>();

        for (Link l: network.getLinks().values()) {
            if (linkList.contains(l.getId())) {
                links.add(l);
                oldValues.put(l, l.getCapacity());
            }
        }

        NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(startTime));
        networkChangeEvent.addLinks(oldValues.keySet());
        NetworkChangeEvent.ChangeType type = NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS;
        NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(type, 0.0);
        networkChangeEvent.setFlowCapacityChange(changeValue);

        for(Link l: links) {
            NetworkChangeEvent reverseNetworkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(1330));
            reverseNetworkChangeEvent.addLink(l);
            NetworkChangeEvent.ChangeValue reverseChangeValue = new NetworkChangeEvent.ChangeValue(type, l.getCapacity());
            reverseNetworkChangeEvent.setFlowCapacityChange(reverseChangeValue);
            listOfNetworkChangeEvents.add(reverseNetworkChangeEvent);
        }

        //revert changes
        NetworkChangeEvent reverseNetworkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(endTime));
        reverseNetworkChangeEvent.addLinks(links);
        NetworkChangeEvent.ChangeValue reverseChangeValue = new NetworkChangeEvent.ChangeValue(type, 1000.0);
        reverseNetworkChangeEvent.setFlowCapacityChange(reverseChangeValue);

        //adding list of change events to the network
        listOfNetworkChangeEvents.add(networkChangeEvent);

        for (int ii = 0; ii< listOfNetworkChangeEvents.size(); ii++) {
            NetworkUtils.addNetworkChangeEvent(network, listOfNetworkChangeEvents.get(ii));
        }

        NetworkChangeEventsWriter networkChangeEventsWriter = new NetworkChangeEventsWriter();
        networkChangeEventsWriter.write(testUtils.getOutputDirectory()+ "testNetworkChangeEvent.xml", listOfNetworkChangeEvents);
    }
}
