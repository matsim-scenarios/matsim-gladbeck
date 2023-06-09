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
        //ArrayList<Link, Double > oldValues = new HashMap<>();
        ArrayList<NetworkChangeEvent> listOfNetworkChangeEvents = new ArrayList<>();

        // store links with old values
        for (Link l: network.getLinks().values()) {
            if (linkList.contains(l.getId())) {
                links.add(l);
                //oldValues.put(l, l.getCapacity());
            }
        }

        // IDEE: Abfrage für Links mit permlanes>1 ... bei diesen dann nur permlanes=permlanes-1 (oder optional permlanes>1 AND "primary road" oder so ähnlich...)


        NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(startTime));
        // add all the links
        networkChangeEvent.addLinks(links);
        NetworkChangeEvent.ChangeType type = NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS;
        NetworkChangeEvent.ChangeType relative = NetworkChangeEvent.ChangeType.FACTOR;
        NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(type, 0.0);
        NetworkChangeEvent.ChangeValue changeValueRelative = new NetworkChangeEvent.ChangeValue(relative, 0.001);
        networkChangeEvent.setFlowCapacityChange(changeValue);
        networkChangeEvent.setFreespeedChange(changeValueRelative);

        for(Link l: links) {
            NetworkChangeEvent reverseNetworkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(endTime));
            reverseNetworkChangeEvent.addLink(l);
            NetworkChangeEvent.ChangeValue reverseChangeValueCapacity = new NetworkChangeEvent.ChangeValue(type, l.getCapacity());
            NetworkChangeEvent.ChangeValue reverseChangeValueSpeed = new NetworkChangeEvent.ChangeValue(relative, 1000.0);
            reverseNetworkChangeEvent.setFlowCapacityChange(reverseChangeValueCapacity);
            listOfNetworkChangeEvents.add(reverseNetworkChangeEvent);
        }

        //adding list of change events to the network
        listOfNetworkChangeEvents.add(networkChangeEvent);

        for (int ii = 0; ii< listOfNetworkChangeEvents.size(); ii++) {
            NetworkUtils.addNetworkChangeEvent(network, listOfNetworkChangeEvents.get(ii));
        }
        //NetworkChangeEventsWriter networkChangeEventsWriter = new NetworkChangeEventsWriter();
        //networkChangeEventsWriter.write( "testNetworkChangeEvent.xml", listOfNetworkChangeEvents);
    }



}