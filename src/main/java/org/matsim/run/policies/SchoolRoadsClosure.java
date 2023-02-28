package org.matsim.run.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.misc.Time;

import java.util.*;

public class SchoolRoadsClosure {
    public void closeSchoolLinks(List<Id<Link>> linkList, Network network, int startTime, int endTime) {

        Collection<Link> links = new ArrayList<>();
        HashMap<Link, Double > oldValues = new HashMap<>();
        ArrayList<NetworkChangeEvent> listOfNetworkChangeEvents = new ArrayList<>();

        for (Link l: network.getLinks().values()) {
            if (linkList.contains(l.getId())) {
                links.add(l);
                oldValues.put(l, l.getCapacity());
            }
        }

        NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(startTime));
        networkChangeEvent.addLinks(oldValues.keySet());
        NetworkChangeEvent.ChangeType type = NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS;
        NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(type, 0.0);
        networkChangeEvent.setFlowCapacityChange(changeValue);

        for(Link l: links) {
            NetworkChangeEvent reverseNetworkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(endTime));
            reverseNetworkChangeEvent.addLink(l);
            NetworkChangeEvent.ChangeValue reverseChangeValue = new NetworkChangeEvent.ChangeValue(type, l.getCapacity());
            reverseNetworkChangeEvent.setFlowCapacityChange(reverseChangeValue);
            listOfNetworkChangeEvents.add(reverseNetworkChangeEvent);
        }

        //adding list of change events to the network
        listOfNetworkChangeEvents.add(networkChangeEvent);

        for (int ii = 0; ii< listOfNetworkChangeEvents.size(); ii++) {
            NetworkUtils.addNetworkChangeEvent(network, listOfNetworkChangeEvents.get(ii));
        }

        //NetworkChangeEventsWriter networkChangeEventsWriter = new NetworkChangeEventsWriter();
       //networkChangeEventsWriter.write(testUtils.getOutputDirectory()+ "testNetworkChangeEvent.xml", listOfNetworkChangeEvents);
    }



}