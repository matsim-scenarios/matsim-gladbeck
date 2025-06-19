package org.matsim.run.policies;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonScoreEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.prepare.GladbeckUtils;

import java.util.List;


/**
 * This handler will give all migrants in the population (persons with attribute {@code "migrant": true}) a massive negative score when using bikes.
 * Migrant thus will not use the bike, when this handler is used.
 */
public class BikePunishmentEventHandler implements PersonDepartureEventHandler {

    @Inject
    EventsManager eventsManager;

    @Inject
    Scenario scenario;


    private List<Id<Person>> migrants;

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {


        if (personDepartureEvent.getLegMode().contains(TransportMode.bike)) {

            if (scenario.getPopulation().getPersons().get(personDepartureEvent.getPersonId()).getAttributes().getAttribute(GladbeckUtils.MIGRANT).equals(true)) {
                PersonMoneyEvent personMoneyEvent = new PersonMoneyEvent(personDepartureEvent.getTime(), personDepartureEvent.getPersonId(), -100, "bike", null, null);
                eventsManager.processEvent(personMoneyEvent);
            }
        }

    }
}
