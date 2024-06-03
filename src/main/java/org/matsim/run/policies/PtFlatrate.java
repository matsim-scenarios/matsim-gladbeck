package org.matsim.run.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PtFlatrate implements PersonDepartureEventHandler, AfterMobsimListener {

    private final List<Id<Person>> personsEligibleForPtFlatrate;
    private final List<Id<Person>> currentIterationForPtFlatrate = new ArrayList<>();
    private  final double ptDailyMonetaryConstant;

    public PtFlatrate(List<Id<Person>> personsEligibleForPtFlatrate, double ptDailyMonetaryConstant) {
        this.personsEligibleForPtFlatrate = personsEligibleForPtFlatrate;
        this.ptDailyMonetaryConstant = ptDailyMonetaryConstant;
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if (currentIterationForPtFlatrate.contains(personDepartureEvent.getPersonId())
                && personDepartureEvent.getLegMode().equals(TransportMode.pt)) {
            currentIterationForPtFlatrate.add(personDepartureEvent.getPersonId());
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        for (Id person: currentIterationForPtFlatrate) {
            // multiplied by -1 to throw positiv person money event
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, (-1) *   ptDailyMonetaryConstant, "ptFlat",null, null ));
        }
    }

    @Override
    public void reset(int iteration) {
        currentIterationForPtFlatrate.clear();
        currentIterationForPtFlatrate.addAll(personsEligibleForPtFlatrate);
    }
}
