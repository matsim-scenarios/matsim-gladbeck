package org.matsim.run.policies.freePt;

import cadyts.calibrators.filebased.Agent;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.vsp.pt.fare.ChainedPtFareCalculator;
import org.matsim.contrib.vsp.pt.fare.PtFareCalculator;
import org.matsim.contrib.vsp.pt.fare.PtFareConfigGroup;
import org.matsim.contrib.vsp.pt.fare.PtFareHandler;
import org.matsim.core.api.experimental.events.AgentWaitingForPtEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.StageActivityTypeIdentifier;
import org.matsim.pt.PtConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChainedPtFareHandlerWithFreePt implements PtFareHandler {
    private final EventsManager events;
    private final ChainedPtFareCalculator fareCalculator;
    private final List<Id<Person>> personsEligibleForPtFlatrate;

    private final Map<Id<Person>, Coord> personDepartureCoordMap = new HashMap<>();
    private final Map<Id<Person>, Coord> personArrivalCoordMap = new HashMap<>();

    @Inject
    public ChainedPtFareHandlerWithFreePt(
            EventsManager events,
            ChainedPtFareCalculator fareCalculator,
            List<Id<Person>> personsEligibleForPtFlatrate) {
        this.events = events;
        this.fareCalculator = fareCalculator;
        this.personsEligibleForPtFlatrate = personsEligibleForPtFlatrate;
    }


    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(PtConstants.TRANSIT_ACTIVITY_TYPE)) {
            personDepartureCoordMap.computeIfAbsent(event.getPersonId(), c -> event.getCoord()); // The departure place is fixed to the place of
            // first pt interaction an agent has in the whole leg
            personArrivalCoordMap.put(event.getPersonId(), event.getCoord()); // The arrival stop will keep updating until the agent start a real
            // activity (i.e. finish the leg)
        }

        if (StageActivityTypeIdentifier.isStageActivity(event.getActType())) {
            return;
        }

        Id<Person> personId = event.getPersonId();
        if (!personDepartureCoordMap.containsKey(personId)) {
            return;
        }

        Coord from = personDepartureCoordMap.get(personId);
        Coord to = personArrivalCoordMap.get(personId);

        PtFareCalculator.FareResult fare = fareCalculator.calculateFare(from, to).orElseThrow();

        // If the person is eligible for the flatrate, we do not charge them
        if (!personsEligibleForPtFlatrate.contains(personId)) {
            events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare.fare(), PtFareConfigGroup.PT_FARE,
                    fare.transactionPartner(), event.getPersonId().toString()));
        }

        personDepartureCoordMap.remove(personId);
        personArrivalCoordMap.remove(personId);
    }

    @Override
    public void handleEvent(AgentWaitingForPtEvent event) {
        //TODO
    }

    @Override
    public void reset(int iteration) {
        personArrivalCoordMap.clear();
        personDepartureCoordMap.clear();
    }
}