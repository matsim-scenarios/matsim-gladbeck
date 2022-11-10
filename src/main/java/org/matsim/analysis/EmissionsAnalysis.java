package org.matsim.analysis;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.emissions.AirPollutionByVehicleCategory;

public class EmissionsAnalysis {

    public static void main (String[] args) throws Exception {
        new AirPollutionByVehicleCategory().execute(args);
    }
}
