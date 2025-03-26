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
    public void closeSchoolLinks(Set<SchoolClosure> decideOnSchools, Network network, int startTime, int endTime) {
        Collection<Link> links = new ArrayList<>();
        //ArrayList<Link, Double > oldValues = new HashMap<>();
        ArrayList<NetworkChangeEvent> listOfNetworkChangeEvents = new ArrayList<>();
        Collection<Id<Link>> linkList = wichLinks(decideOnSchools);

        // store links with old values
        for (Link l: network.getLinks().values()) {
            if (linkList.contains(l.getId())) {
                links.add(l);
                //oldValues.put(l, l.getCapacity());
            }
        }

        for (Link l: links) {
            NetworkChangeEvent networkChangeEventCapacity = new NetworkChangeEvent(Time.convertHHMMInteger(startTime));
            //NetworkChangeEvent networkChangeEventSpeed = new NetworkChangeEvent(Time.convertHHMMInteger(startTime));
            networkChangeEventCapacity.addLink(l);
            //networkChangeEventSpeed.addLink(l);
            NetworkChangeEvent.ChangeType type = NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS;
            NetworkChangeEvent.ChangeType relative = NetworkChangeEvent.ChangeType.FACTOR;
            NetworkChangeEvent.ChangeValue changeValue = new NetworkChangeEvent.ChangeValue(type, 0.0);
            NetworkChangeEvent.ChangeValue changeValueRelative = new NetworkChangeEvent.ChangeValue(relative, 0.001);
            networkChangeEventCapacity.setFlowCapacityChange(changeValue);
            networkChangeEventCapacity.setFreespeedChange(changeValueRelative);
            //networkChangeEventSpeed.setFreespeedChange(changeValueRelative);

            //adding list of change events to the network
            listOfNetworkChangeEvents.add(networkChangeEventCapacity);
            //listOfNetworkChangeEvents.add(networkChangeEventSpeed);

            NetworkChangeEvent reverseNetworkChangeEventCapacity = new NetworkChangeEvent(Time.convertHHMMInteger(endTime));
            //NetworkChangeEvent reverseNetworkChangeEventSpeed = new NetworkChangeEvent(Time.convertHHMMInteger(endTime));
            reverseNetworkChangeEventCapacity.addLink(l);
            //reverseNetworkChangeEventSpeed.addLink(l);
            NetworkChangeEvent.ChangeValue reverseChangeValueCapacity = new NetworkChangeEvent.ChangeValue(type, l.getCapacity());
            NetworkChangeEvent.ChangeValue reverseChangeValueSpeed = new NetworkChangeEvent.ChangeValue(relative, 1000.0);
            reverseNetworkChangeEventCapacity.setFlowCapacityChange(reverseChangeValueCapacity);
            reverseNetworkChangeEventCapacity.setFreespeedChange(reverseChangeValueSpeed);

            //adding list of change events to the network
            listOfNetworkChangeEvents.add(reverseNetworkChangeEventCapacity);
            //listOfNetworkChangeEvents.add(reverseNetworkChangeEventSpeed);

        }






        for (int ii = 0; ii< listOfNetworkChangeEvents.size(); ii++) {
            NetworkUtils.addNetworkChangeEvent(network, listOfNetworkChangeEvents.get(ii));
        }

        NetworkChangeEventsWriter networkChangeEventsWriter = new NetworkChangeEventsWriter();
        networkChangeEventsWriter.write( "testNetworkChangeEvent.xml", listOfNetworkChangeEvents);
    }


    /**
     * Defines wich schools are closed.
     */
    @SuppressWarnings("checkstyle:OneTopLevelClass")
    public enum SchoolClosure {mosaikSchool, allSchools}


    private static Collection<Id<Link>> wichLinks(Set<SchoolClosure> schoolClosure) {
        List<Id<Link>> listOfSchoolLinks = new ArrayList<>();
        if ((schoolClosure.contains(SchoolClosure.mosaikSchool))) {
            // street in front of Mosaikschule
            listOfSchoolLinks.add(Id.createLinkId("353353080004r"));
            listOfSchoolLinks.add(Id.createLinkId("353353080004f"));
            //more links to see an effect in the model
            listOfSchoolLinks.add(Id.createLinkId("380432140001r"));
            listOfSchoolLinks.add(Id.createLinkId("380432140001f"));
            listOfSchoolLinks.add(Id.createLinkId("5156341260014r"));
            listOfSchoolLinks.add(Id.createLinkId("5156341260014f"));
        }

        if (schoolClosure.contains(SchoolClosure.allSchools)) {
            //wilhemlsschule
            listOfSchoolLinks.add(Id.createLinkId("5156341260014f"));
            listOfSchoolLinks.add(Id.createLinkId("102406730021r"));
            listOfSchoolLinks.add(Id.createLinkId("3090871450010f"));
            listOfSchoolLinks.add(Id.createLinkId("3090871450010r"));

            //josefSchule
            listOfSchoolLinks.add(Id.createLinkId("2246773700002f"));
            listOfSchoolLinks.add(Id.createLinkId("2246773700002r"));

        }
        return listOfSchoolLinks;
    }

}

