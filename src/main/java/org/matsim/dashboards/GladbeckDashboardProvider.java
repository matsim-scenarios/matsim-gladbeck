package org.matsim.dashboards;

import org.matsim.core.config.Config;
import org.matsim.simwrapper.Dashboard;
import org.matsim.simwrapper.DashboardProvider;
import org.matsim.simwrapper.SimWrapper;
import org.matsim.simwrapper.dashboard.EmissionsDashboard;
import org.matsim.simwrapper.dashboard.TripDashboard;

import java.util.List;

public class GladbeckDashboardProvider implements DashboardProvider {

    @Override
    public List<Dashboard> getDashboards(Config config, SimWrapper simWrapper) {
        TripDashboard trips = new TripDashboard(
                "gladbeck_mode_share.csv", null, null
        ).setAnalysisArgs("--person-filter", "subpopulation=person");


        return List.of(
                trips, new EmissionsDashboard(config.global().getCoordinateSystem())
        );
    }
}
