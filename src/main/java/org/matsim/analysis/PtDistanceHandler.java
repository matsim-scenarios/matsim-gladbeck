package org.matsim.analysis;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.PtConstants;

import java.util.HashMap;
import java.util.Map;

public class PtDistanceHandler implements ActivityStartEventHandler {
    @Inject
    private EventsManager events;

    private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
    private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();
    private final Map<Id<Person>, Double> personPtDistance = new HashMap<>();



    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
            personDepartureCoordMap.computeIfAbsent(event.getPersonId(), c -> event.getCoord()); // The departure place is fixed to the place of first pt interaction an agent has in the whole leg
            personArrivalCoordMap.put(event.getPersonId(), event.getCoord()); // The arrival stop will keep updating until the agent start a real activity (i.e. finish the leg)
        }

        if (!StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
            Id<Person> personId = event.getPersonId();
            if (personDepartureCoordMap.containsKey(personId)) {
                double distance = CoordUtils.calcEuclideanDistance(personDepartureCoordMap.get(personId), personArrivalCoordMap.get(personId));
                if (personPtDistance.containsKey(event.getPersonId())) {
                    personPtDistance.put(personId, distance);
                }
                if (!personPtDistance.containsKey(event.getPersonId())) {
                    distance = personPtDistance.get(personId) +distance;
                    personPtDistance.replace(personId, distance);
                }

                personDepartureCoordMap.remove(personId);
                personArrivalCoordMap.remove(personId);
            }
        }
    }

    @Override
    public void reset(int iteration) {
        personArrivalCoordMap.clear();
        personDepartureCoordMap.clear();
        personPtDistance.clear();
    }


}
