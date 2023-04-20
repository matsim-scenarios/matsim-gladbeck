package org.matsim.run.policies;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;


import java.util.HashMap;
import java.util.Map;

public class KlimaTaler implements PersonDepartureEventHandler, PersonArrivalEventHandler, AfterMobsimListener, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkLeaveEventHandler {

    private final Map<Id<Person>, Double> distanceTravelledPt = new HashMap<>();
    private final Map<Id<Person>, Double> distanceTravelledWalk = new HashMap<>();
    private final Map<Id<Person>, Double> distanceTravelledBike = new HashMap<>();
    private Map<Id<Vehicle>, Id<Person>> vehicles2Persons = new HashMap<>();


    private final double beelineDistanceFactor;
    private final Network network;

    private Map<Id<Person>, Coord> agentDepartureLocations = new HashMap<>();

    public KlimaTaler(double modeSpecificBeelineDistanceFactor, Network network) {
        this.beelineDistanceFactor = modeSpecificBeelineDistanceFactor;
        this.network = network;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(TransportMode.walk)) {
            Id<Link> linkId = event.getLinkId();
            Coord endcoord = network.getLinks().get(linkId).getCoord();
            Coord startCoord = this.agentDepartureLocations.remove(event.getPersonId());
            double beelineDistance = CoordUtils.calcEuclideanDistance(startCoord, endcoord);
            double distance = beelineDistance * beelineDistanceFactor;

            if (!distanceTravelledWalk.containsKey(event.getPersonId())) {
                distanceTravelledWalk.put(event.getPersonId(), distance);
            } else {
                distance = distanceTravelledWalk.get(event.getPersonId()) + distance;
                distanceTravelledWalk.replace(event.getPersonId(), distance);
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (event.getLegMode().equals(TransportMode.walk)) {
            Id<Link> linkId = event.getLinkId();
            Coord coord = network.getLinks().get(linkId).getCoord();
            this.agentDepartureLocations.put(event.getPersonId(), coord);
        }
        if (event.getLegMode().equals(TransportMode.bike)) {
            distanceTravelledBike.put(event.getPersonId(), 0.0);
        }
    }

    @Override
    public void reset(int iteration) {
        this.agentDepartureLocations.clear();
        this.vehicles2Persons.clear();
        distanceTravelledWalk.clear();
    }


    @Override
    public void notifyAfterMobsim(AfterMobsimEvent afterMobsimEvent) {
        for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledWalk.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            double emissionsSaved = idDoubleEntry.getValue()* 0.176;
            double klimaTaler = emissionsSaved/5000* 10.0;
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForWalk", null, null));
        }

        for (Map.Entry<Id<Person>, Double> idDoubleEntry : distanceTravelledBike.entrySet()) {
            Id<Person> person = idDoubleEntry.getKey();
            double emissionsSaved = idDoubleEntry.getValue()* 0.176;
            double klimaTaler = emissionsSaved/5000 * 10.0;
            System.out.println("BikeDistance" + idDoubleEntry.getValue());
            afterMobsimEvent.getServices().getEvents().processEvent(new PersonMoneyEvent(Time.MIDNIGHT, person, klimaTaler, "klimaTalerForBike", null, null));
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent personEntersVehicleEvent) {
        //if (distanceTravelledBike.containsKey(personEntersVehicleEvent.getPersonId())) {
            System.out.println("Hallo");
            System.out.println(personEntersVehicleEvent.getPersonId());
            vehicles2Persons.put(personEntersVehicleEvent.getVehicleId(), personEntersVehicleEvent.getPersonId());
        //}
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent personLeavesVehicleEvent) {
        if (vehicles2Persons.containsKey(personLeavesVehicleEvent.getVehicleId())) {
            vehicles2Persons.remove(personLeavesVehicleEvent.getVehicleId());
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        if (vehicles2Persons.containsKey(linkLeaveEvent.getVehicleId())) {
            Id<Person> personId = vehicles2Persons.get(linkLeaveEvent.getVehicleId());
            if (distanceTravelledBike.containsKey(personId)) {
                double linkLength = network.getLinks().get(linkLeaveEvent.getLinkId()).getLength();
                double distanceTravelled = distanceTravelledBike.get(personId) + linkLength;
                distanceTravelledBike.replace(personId, distanceTravelled);
            }
        }

    }
}
