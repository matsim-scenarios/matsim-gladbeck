package org.matsim.prepare;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.MATSimAppCommand;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;

@CommandLine.Command(
        name = "open-population",
        description = "Prepare open population"
)
public class PrepareOpenPopulation implements MATSimAppCommand {

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

        // Activities with type "other" were added synthetically, these coordinates are not copied
        List<Activity> activities = TripStructureUtils.getActivities(sourcePerson.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

        for (Plan plan : targetPerson.getPlans()) {

            List<Activity> targetActs = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

            int toActivityCounter = 0;
            for (int fromActivityCounter = 0; fromActivityCounter < activities.size(); fromActivityCounter++) {
                Activity fromActivity = activities.get(fromActivityCounter);
                Activity toActivity = targetActs.get(toActivityCounter);

                // Skip other and the second part of the actual activity (which are both not present in the target)
                if (fromActivity.getType().startsWith("other")) {
                    fromActivityCounter++;
                    continue;
                }

                if (toActivity.getType().startsWith("other")) {
                    Activity secondOccurrenceOfLastFromActivity = targetActs.get(toActivityCounter + 1);
                    secondOccurrenceOfLastFromActivity.setCoord(activities.get(fromActivityCounter - 1).getCoord());

                    toActivityCounter += 2;
                    continue;
                }

                toActivity.setCoord(fromActivity.getCoord());
                toActivityCounter++;
            }

            TripStructureUtils.getLegs(plan).forEach(leg -> leg.setRoute(null));
        }
    }
}
