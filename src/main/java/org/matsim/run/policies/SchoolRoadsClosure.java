package org.matsim.run.policies;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.core.network.NetworkUtils;

import java.util.Collection;

public class SchoolRoadsClosure {

     public Network closeSchoolRoads(Network network) {

        Collection<? extends Link> links = null;
        //store the values of the links before
        // first network change event everything to zero
        NetworkChangeEvent networkChangeEvent = new NetworkChangeEvent(18.0);
        networkChangeEvent.addLinks(links);
        NetworkChangeEvent.ChangeType type = NetworkChangeEvent.ChangeType.FACTOR;
        NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(type, 0.1);
        networkChangeEvent.setFreespeedChange(changeValue);

        //second network change event setting everything back to the normal values
        NetworkUtils.addNetworkChangeEvent(network, networkChangeEvent);



        return network;
    }

}