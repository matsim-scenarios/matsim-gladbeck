package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.population.PopulationUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * This handler will give all migrants in the population (persons with attribute {@code "migrant": true}) a massive negative score when using bikes.
 * Migrant thus will not use the bike, when this handler is used.
 */
public class MigrantBicycleChoiceHandler implements PersonDepartureEventHandler, AfterMobsimListener {
    private List<PersonScoreEvent> migrantDepartures;
    private List<Id<Person>> migrants;

    public MigrantBicycleChoiceHandler(List<Id<Person>> listOfMigrants) {
        migrants = listOfMigrants;
        migrantDepartures = new LinkedList<>();
    }

    public MigrantBicycleChoiceHandler(Population population) {
        migrantDepartures = new LinkedList<>();
        migrants = new LinkedList<>();
        for(Person p : population.getPersons().values()){
            if(p.getAttributes().getAsMap().containsKey("migrant") && p.getAttributes().getAttribute("migrant").equals(true)) migrants.add(p.getId());
        }
    }

    public MigrantBicycleChoiceHandler(String populationPath) {
        migrantDepartures = new LinkedList<>();
        migrants = new LinkedList<>();
        Population population = PopulationUtils.readPopulation(populationPath);
        for(Person p : population.getPersons().values()){
            if(p.getAttributes().getAsMap().containsKey("migrant") && p.getAttributes().getAttribute("migrant").equals(true)) migrants.add(p.getId());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if(personDepartureEvent.getLegMode().equals("bicycle") && migrants.contains(personDepartureEvent.getPersonId())) {
            migrantDepartures.add(new PersonScoreEvent(personDepartureEvent.getTime(), personDepartureEvent.getPersonId(), -10000.0, "punishment_for_cycling"));
        }
        // TODO TransportMode.bike
    }

    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        for(PersonScoreEvent personScoreEvent : migrantDepartures) {
            afterMobsimEvent.getServices().getEvents().processEvent(personScoreEvent);
        }
    }

    @Override
    public void reset(int iteration) {
        PersonDepartureEventHandler.super.reset(iteration);
    }
}
