package org.matsim.prepare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.prepare.population.ExtractHomeCoordinates;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

        // Re-calculates home coordinates
        calibratedPopulation.getPersons().values().forEach(PrepareOpenPopulation::setHomeCoordinate);

        PopulationUtils.writePopulation(calibratedPopulation, output.toString());

        return 0;
    }

    /**
     * Copy coordinates from the source person to target person
     */
    private void prepare(Person targetPerson, Person sourcePerson) {

        // Activities with type "other" were added synthetically, these coordinates are not copied
        List<Activity> activities = TripStructureUtils.getActivities(sourcePerson.getSelectedPlan(), TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

        Optional<Activity> homeAct = activities.stream().filter(act -> act.getType().startsWith("home")).findFirst();

        for (Plan plan : targetPerson.getPlans()) {

            List<Activity> targetActs = TripStructureUtils.getActivities(plan, TripStructureUtils.StageActivityHandling.ExcludeStageActivities);

            int toActivityCounter = 0;
            for (int fromActivityCounter = 0; toActivityCounter < targetActs.size(); fromActivityCounter++) {

                Activity toActivity = targetActs.get(toActivityCounter);
                if (fromActivityCounter >= activities.size()) {

                    if (homeAct.isEmpty() || !toActivity.getType().startsWith("home")) {
                        throw new IllegalStateException("No home activity found as last activity");
                    }

                    toActivity.setCoord(homeAct.get().getCoord());
                    toActivityCounter++;
                    continue;
                }

                Activity fromActivity = activities.get(fromActivityCounter);

                // If both other, counters can just be incremented
                if (fromActivity.getType().startsWith("other") && toActivity.getType().startsWith("other")) {
                    toActivityCounter++;
                    continue;
                }

                // Skip other and the second part of the actual activity (which are both not present in the target)
                if (fromActivity.getType().startsWith("other")) {
                    fromActivityCounter++;
                    continue;
                }

                if (toActivity.getType().startsWith("other")) {
                    Activity secondOccurrenceOfLastFromActivity = targetActs.get(toActivityCounter + 1);
                    Activity previousActivity = activities.get(fromActivityCounter - 1);

                    secondOccurrenceOfLastFromActivity.setCoord(previousActivity.getCoord());

                    // Same from activity is used again
                    fromActivityCounter -= 1;
                    toActivityCounter += 2;
                    continue;
                }

                toActivity.setCoord(fromActivity.getCoord());
                toActivityCounter++;
            }

            TripStructureUtils.getLegs(plan).forEach(leg -> leg.setRoute(null));
        }
    }

    /**
     * Can be removed in new MATSim version.
     * @see ExtractHomeCoordinates
     */
    private static void setHomeCoordinate(Person person) {

        outer:
        for (Plan plan : person.getPlans()) {
            for (PlanElement planElement : plan.getPlanElements()) {
                if (planElement instanceof Activity) {
                    String actType = ((Activity) planElement).getType();
                    if (actType.startsWith("home")) {
                        Coord homeCoord = ((Activity) planElement).getCoord();

                        person.getAttributes().putAttribute("home_x", homeCoord.getX());
                        person.getAttributes().putAttribute("home_y", homeCoord.getY());

                        break outer;
                    }
                }
            }

        }

    }

}
