package org.matsim.run.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.NetworkChangeEventsWriter;
import org.matsim.core.utils.misc.Time;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SchoolRoadsClosure {

     public Network closeSchoolRoads(Network network) {

         Collection<Link> links = null;

        //store the values of the links before
        // first network change event everything to zero
         Link link = network.getLinks().get(Id.createLinkId("5156341260014r"));
         Link link2 = network.getLinks().get(Id.createLinkId("5156341260014f"));
         links.add(link);
         links.add(link2);

         NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(830));
         networkChangeEvent.addLinks(links);
         NetworkChangeEvent.ChangeType type = NetworkChangeEvent.ChangeType.FACTOR;
         NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(type, 0.0);
         networkChangeEvent.setFlowCapacityChange(changeValue);

         NetworkChangeEvent revertNetworkChangeEvent = new NetworkChangeEvent(Time.convertHHMMInteger(930));
         networkChangeEvent.addLinks(links);
         NetworkChangeEvent.ChangeValue revertChangeValue = new NetworkChangeEvent.ChangeValue(type, 100.0);
         networkChangeEvent.setFreespeedChange(revertChangeValue);
         NetworkUtils.addNetworkChangeEvent(network, revertNetworkChangeEvent);
         ArrayList<NetworkChangeEvent> listOfNetworkChangeEvents = new ArrayList<>();
         listOfNetworkChangeEvents.add(networkChangeEvent);
         listOfNetworkChangeEvents.add(revertNetworkChangeEvent);
         NetworkChangeEventsWriter networkChangeEventsWriter = new NetworkChangeEventsWriter();
         networkChangeEventsWriter.write("/Users/gregorr/Desktop/Test/GlaMoBi/TestNetworkChangeEvent.xml", listOfNetworkChangeEvents);

        return network;
    }

}