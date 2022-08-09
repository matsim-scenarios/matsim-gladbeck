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

import java.util.HashMap;
import java.util.Map;

public class PtFlatrate implements PersonDepartureEventHandler, AfterMobsimListener {

    private final Map<Id<Person>, Integer> personsEligibleForPtFiltrate;
    private final Map<Id<Person>, Integer> currentIterationForPtFlatrate = new HashMap<>();
    private final double ptConstant;
    private  final double ptDailyMonetaryConstant;

    public PtFlatrate(Map<Id<Person>, Integer> personsEligibleForPtFiltrate, double ptConstant, double ptDailyMonetaryConstant) {
        this.personsEligibleForPtFiltrate = personsEligibleForPtFiltrate;
        this.ptConstant = ptConstant;
        this.ptDailyMonetaryConstant = ptDailyMonetaryConstant;
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if (currentIterationForPtFlatrate.containsKey(personDepartureEvent.getPersonId())
                && personDepartureEvent.getLegMode().equals(TransportMode.pt)) {
            int numberOfPtTrips =  currentIterationForPtFlatrate.get(personDepartureEvent.getPersonId());
            numberOfPtTrips++;
            currentIterationForPtFlatrate.replace(personDepartureEvent.getPersonId(),numberOfPtTrips);
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        for (Map.Entry<Id<Person>, Integer> idDoubleEntry : currentIterationForPtFlatrate.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            Integer numberOfPtTrips = idDoubleEntry.getValue();
            double ptFlat = numberOfPtTrips * ptConstant + ptDailyMonetaryConstant;
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, ptFlat, "ptFlat",null, null ));
        }
    }

    @Override
    public void reset(int iteration) {
        currentIterationForPtFlatrate.clear();
        currentIterationForPtFlatrate.putAll(personsEligibleForPtFiltrate);
    }
}
