package org.matsim.prepare;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

public class ReducePopulation {

    public static void main (String[] args) {
        Population population = PopulationUtils.readPopulation("/Users/gregorr/Desktop/Test/pop_reduced.xml");
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.loadScenario(config);
        int counter = 0;

        for (Person person: population.getPersons().values()) {
            scenario.getPopulation().addPerson(person);
            counter++;
            if (counter ==10) break;
        }
        PopulationUtils.writePopulation(scenario.getPopulation(),"/Users/gregorr/Desktop/Test/test_pop.xml");
    }
}
