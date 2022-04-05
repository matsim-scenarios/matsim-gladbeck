package org.matsim.run.strategy;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MobilityBudgetEventHandler implements PersonDepartureEventHandler, AfterMobsimListener {

    Logger log = Logger.getLogger(MobilityBudgetEventHandler.class);
    private final Map<Id<Person>, Double> person2MobilityBudget;
    private final Map<Id<Person>, Double> currentIterationMobilityBudget = new HashMap<>();

    MobilityBudgetEventHandler(Map<Id<Person>, Double> personsEligibleForMobilityBudget2MoneyValue) {
        this.person2MobilityBudget = personsEligibleForMobilityBudget2MoneyValue;
    }

    @Override
    public void reset(int iteration) {
        currentIterationMobilityBudget.clear();
        currentIterationMobilityBudget.putAll(person2MobilityBudget);
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        Id<Person> personId = personDepartureEvent.getPersonId();
        if (this.currentIterationMobilityBudget.containsKey(personId) && personDepartureEvent.getLegMode().equals(TransportMode.pt)) {
            // cost pt
            // monetaryConstant + constant * marginalUtilityOfMoney
            // the values from the config are different depending on the sample size
            this.currentIterationMobilityBudget.replace(personId,0.0);
        }
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent event) {
        double totalSumMobilityBudget = 0.;
        for (Map.Entry<Id<Person>, Double> idDoubleEntry : currentIterationMobilityBudget.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            Double mobilityBudget = idDoubleEntry.getValue();
            event.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, mobilityBudget, "mobilityBudget", null, null));
            totalSumMobilityBudget = totalSumMobilityBudget + mobilityBudget;
        }
        log.info("This iteration the totalSumMobilityBudget paid to the Agents was:" + totalSumMobilityBudget);
    }

    public static Map<Id<Person>, Double> getPersonsEligibleForMobilityBudget2FixedValue(Scenario scenario, Double value) {
        Map<Id<Person>, Double> persons2Budget = new HashMap<>();
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Id personId = person.getId();
            if(!personId.toString().contains("commercial")) {
                Plan plan = person.getSelectedPlan();
                //TripStructureUtil get Legs
                List<String> transportModeList = new ArrayList<>();
                List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
                for (TripStructureUtils.Trip trip: trips) {
                    List<Leg> listLegs = trip.getLegsOnly();
                    for (Leg leg: listLegs) {
                        transportModeList.add(leg.getMode());
                    }
                }
                if (transportModeList.contains(TransportMode.car)) {
                    persons2Budget.put(personId, value);
                }
            }
        }
        return persons2Budget;
    }

}
