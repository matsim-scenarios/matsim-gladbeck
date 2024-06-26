package org.matsim.prepare;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MigrantBicycleChoiceHandler implements PersonDepartureEventHandler, AfterMobsimListener {
    private List<PersonScoreEvent> migrantDepartures;
    private Population population;

    public MigrantBicycleChoiceHandler(Population population) {
        migrantDepartures = new LinkedList<>();
        this.population = population;
    }

    public MigrantBicycleChoiceHandler(String populationPath) {
        migrantDepartures = new LinkedList<>();
        this.population = PopulationUtils.readPopulation(populationPath);
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        // Do not do anything, if this Person is not a migrant
        if( !population.getPersons().get(personDepartureEvent.getPersonId()).getAttributes().getAsMap().containsKey("subpopulation")
                || !population.getPersons().get(personDepartureEvent.getPersonId()).getAttributes().getAttribute("subpopulation").equals("migrant") ){
            return;
        }

        // Add to departures of migrants
        Random rand = MatsimRandom.getRandom();
        if(personDepartureEvent.getLegMode().equals("bicycle") && rand.nextInt(1000) < 1000) new PersonMoneyEvent(personDepartureEvent.getTime(), personDepartureEvent.getPersonId(), 1000); //TODO what is kind?; set rand-value to 400
        // TODO TransportMode.bike
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        for(PersonScoreEvent e : migrantDepartures){
            // TODO Check if this will actually influence the choice
            // TODO Check what would be a useful value
            // TODO Gibt es keine direktere Möglichkeit? Kann ich das Event nicht einfach irgendwie überschreiben?
            afterMobsimEvent.getServices().getEvents().processEvent(e);
        }
        migrantDepartures.clear();
    }

    public static void main(String[] args) {
        //MigrantBicycleChoiceHandler choiceHandler = new MigrantBicycleChoiceHandler();

    }

//    @Override
//    public void notifyReplanning(ReplanningEvent replanningEvent) {
//        /*for(PersonScoreEvent e : migrantDepartures){
//            // TODO Check if this will actually influence the choice
//            // TODO Check what would be a useful value
//            // TODO Gibt es keine direktere Möglichkeit? Kann ich das Event nicht einfach irgendwie überschreiben?
//            replanningEvent.getServices().getEvents().processEvent(e);
//        }*/
//    }
}
