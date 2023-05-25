package org.matsim.run;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.facilities.Facility;

public class NearestLinkChooser implements MultimodalLinkChooser {
    @Override
    public Link decideOnLink(Facility facility, Network network) {

        if (facility.getCoord() == null) {
            throw new RuntimeException("We need coordinates to choose links.");
        }

        return NetworkUtils.getNearestLink(network, facility.getCoord());
    }
}

