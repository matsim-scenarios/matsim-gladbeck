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
    public enum SchoolClosure {mosaikSchool, allSchools, testCase}


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

            //Ingeborg Drewitz
            listOfSchoolLinks.add(Id.createLinkId("3211239560008r"));
            listOfSchoolLinks.add(Id.createLinkId("3211239560008f"));
            listOfSchoolLinks.add(Id.createLinkId("3211239550004r"));
            listOfSchoolLinks.add(Id.createLinkId("3211239550004f"));
            listOfSchoolLinks.add(Id.createLinkId("369047460003r"));
            listOfSchoolLinks.add(Id.createLinkId("369047460003f"));
            //Pestalozzi Schule
            listOfSchoolLinks.add(Id.createLinkId("389130530005r"));
            listOfSchoolLinks.add(Id.createLinkId("389130530005f"));
            //Jordan Mai Schule
            listOfSchoolLinks.add(Id.createLinkId("1337273760011r"));
            listOfSchoolLinks.add(Id.createLinkId("1337273760011f"));
            listOfSchoolLinks.add(Id.createLinkId("1337273690005f"));
            listOfSchoolLinks.add(Id.createLinkId("1337273690005r"));
            //Heisenberg
            listOfSchoolLinks.add(Id.createLinkId("1666275060002f"));
            listOfSchoolLinks.add(Id.createLinkId("1556231900005f"));
            listOfSchoolLinks.add(Id.createLinkId("1556231740006f"));
            listOfSchoolLinks.add(Id.createLinkId("1666275080002f"));
            listOfSchoolLinks.add(Id.createLinkId("1867905930004f"));
            listOfSchoolLinks.add(Id.createLinkId("1556231720011r"));
            listOfSchoolLinks.add(Id.createLinkId("1556231720011f"));
            //Ratsgymnasium
            listOfSchoolLinks.add(Id.createLinkId("2245411250008r"));
            listOfSchoolLinks.add(Id.createLinkId("2245411250008f"));
            listOfSchoolLinks.add(Id.createLinkId("357392350003f"));
            listOfSchoolLinks.add(Id.createLinkId("357392350003r"));
            listOfSchoolLinks.add(Id.createLinkId("1718120700011r"));
            listOfSchoolLinks.add(Id.createLinkId("1718120700011f"));
            //Lamberti Schule
            listOfSchoolLinks.add(Id.createLinkId("357392390002f"));
            listOfSchoolLinks.add(Id.createLinkId("357392390002r"));
            listOfSchoolLinks.add(Id.createLinkId("511410650001f"));
            listOfSchoolLinks.add(Id.createLinkId("511410650001r"));
            listOfSchoolLinks.add(Id.createLinkId("511410650000r"));
            listOfSchoolLinks.add(Id.createLinkId("511410650000f"));
            //Wittinger Schule
            listOfSchoolLinks.add(Id.createLinkId("3254028960007f"));
            listOfSchoolLinks.add(Id.createLinkId("3254028960007r"));
            listOfSchoolLinks.add(Id.createLinkId("1781204710002f"));
            listOfSchoolLinks.add(Id.createLinkId("1781204710002r"));
            //Werner von Siemens
            listOfSchoolLinks.add(Id.createLinkId("358770500002f"));
            listOfSchoolLinks.add(Id.createLinkId("358770500002r"));
            listOfSchoolLinks.add(Id.createLinkId("358770510002r"));
            listOfSchoolLinks.add(Id.createLinkId("358770510002f"));
            //Anne Frank
            listOfSchoolLinks.add(Id.createLinkId("1157881300007r"));
            listOfSchoolLinks.add(Id.createLinkId("1157881300007f"));
            // Riesener Gymnasium
            listOfSchoolLinks.add(Id.createLinkId("1474460420007f"));
            listOfSchoolLinks.add(Id.createLinkId("6127701840009f"));
            //Walddorfschule
            listOfSchoolLinks.add(Id.createLinkId("5156341260028r"));
            listOfSchoolLinks.add(Id.createLinkId("5156341260028f"));
            //Mosaikschule
            listOfSchoolLinks.add(Id.createLinkId("5156341260014f"));
            listOfSchoolLinks.add(Id.createLinkId("5156341260014r"));
            listOfSchoolLinks.add(Id.createLinkId("381870670005f"));
            listOfSchoolLinks.add(Id.createLinkId("381870670005r"));
            //Roßheidschule
            listOfSchoolLinks.add(Id.createLinkId("1337273250028r"));
            listOfSchoolLinks.add(Id.createLinkId("1337273250028f"));
            //Südparkschule
            listOfSchoolLinks.add(Id.createLinkId("3301414430001r"));
            listOfSchoolLinks.add(Id.createLinkId("3301414430001f"));
            //SChulzentrum
            listOfSchoolLinks.add(Id.createLinkId("3301414430001f"));
            listOfSchoolLinks.add(Id.createLinkId("6774113930003r"));
            listOfSchoolLinks.add(Id.createLinkId("330026670013f"));
            listOfSchoolLinks.add(Id.createLinkId("330026670013r"));
            //Albert Schweitzer Schule
            listOfSchoolLinks.add(Id.createLinkId("115596910006f"));
            listOfSchoolLinks.add(Id.createLinkId("115596910006r"));
            listOfSchoolLinks.add(Id.createLinkId("115596910009f"));
            listOfSchoolLinks.add(Id.createLinkId("115596910009r"));


        }

        if (schoolClosure.contains(SchoolClosure.testCase)) {
            listOfSchoolLinks.add(Id.createLinkId("173"));
            listOfSchoolLinks.add(Id.createLinkId("176"));
        }


        return listOfSchoolLinks;
    }

}