package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import picocli.CommandLine;

import java.nio.file.Path;

@CommandLine.Command(
        name = "open-population",
        description = "Prepare open population"
)
public class PrepareOpenPopulation implements MATSimAppCommand {

    private static final Logger log = LogManager.getLogger(PrepareOpenPopulation.class);

    @CommandLine.Option(names = "--open-population", description = "Path to population using open data", required = true)
    private Path openPopulationPath;

    @CommandLine.Option(names = "--calibrated-population", description = "Path to calibrated population", required = true)
    private Path calibratedPopulationPath;

    @CommandLine.Option(names = "--output", description = "Path to output population", required = true)
    private Path output;

    public static void main(String[] args) {
        new PrepareOpenPopulation().execute(args);
    }

    @Override
    public Integer call() throws Exception {

        // TODO read open population
        // replace coordinates from

        Population openPopulation = PopulationUtils.readPopulation(openPopulationPath.toString());
        Population calibratedPopulation = PopulationUtils.readPopulation(calibratedPopulationPath.toString());

        for (Person person : calibratedPopulation.getPersons().values()) {

            Person openPerson = openPopulation.getPersons().get(person.getId());

            if (openPerson == null) {
                throw new RuntimeException("Person " + person.getId() + " not found in open population");
            }

            // Copy coordinates from the open person to target person
            prepare(person, openPerson);
        }

        PopulationUtils.writePopulation(calibratedPopulation, output.toString());

        return 0;
    }

    /**
     * Copy coordinates from the source person to target person
     */
    private void prepare(Person targetPerson, Person sourcePerson) {


        // TODO
    }

}
