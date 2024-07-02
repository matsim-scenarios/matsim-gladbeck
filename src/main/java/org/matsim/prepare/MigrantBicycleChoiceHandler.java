package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.PersonScoreEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
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
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class MigrantBicycleChoiceHandler implements PersonDepartureEventHandler, AfterMobsimListener {
    private List<PersonScoreEvent> migrantDepartures;
    private Population population;
    private List<Id<Person>> migrants;

    public MigrantBicycleChoiceHandler(Population population) {
        migrantDepartures = new LinkedList<>();
        this.population = population;
    }


    public MigrantBicycleChoiceHandler(List<Id<Person>> listOfMigrants) {
        migrants = listOfMigrants;
        migrantDepartures = new LinkedList<>();
    }

    public MigrantBicycleChoiceHandler(String populationPath) {
        migrantDepartures = new LinkedList<>();
        this.population = PopulationUtils.readPopulation(populationPath);
    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
      /*  // Do not do anything, if this Person is not a migrant
        if( !population.getPersons().get(personDepartureEvent.getPersonId()).getAttributes().getAsMap().containsKey("subpopulation")
                || !population.getPersons().get(personDepartureEvent.getPersonId()).getAttributes().getAttribute("subpopulation").equals("migrant") ){
            return;
        }
*/
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



/*    public static void main(String[] args) {
        //MigrantBicycleChoiceHandler choiceHandler = new MigrantBicycleChoiceHandler();

    }*/

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
