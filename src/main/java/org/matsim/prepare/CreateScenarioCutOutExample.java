package org.matsim.prepare;

import org.matsim.application.prepare.population.CleanPopulation;
import org.matsim.application.prepare.scenario.CreateScenarioCutOut;

public class CreateScenarioCutOutExample {

    public static void main(String[] args) {
        //this was created by using PR3788
        String[] inputScenario = new String[]{
                "--shp-crs",
                "EPSG:25832",
                "--shp",
                "/Users/gregorr/Documents/work/respos/shared-svn/projects/GlaMoBi/data/shp-files/Gladbeck.shp",
                "--population",
                "/Users/gregorr/Volumes/math-cluster/matsim-metropole-ruhr/calibration-2.0/calibration-2.0-3pct-new/runs/007/007.output_plans.xml.gz",
                "--network",
                "/Users/gregorr/Volumes/math-cluster/matsim-metropole-ruhr/calibration-2.0/calibration-2.0-3pct-new/runs/007/007.output_network.xml.gz",
                "--facilities",
                "/Users/gregorr/Volumes/math-cluster/matsim-metropole-ruhr/calibration-2.0/calibration-2.0-3pct-new/runs/007/007.output_facilities.xml.gz",
                "--events",
                "/Users/gregorr/Volumes/math-cluster/matsim-metropole-ruhr/calibration-2.0/calibration-2.0-3pct-new/runs/007/007.output_events.xml.gz",
                "--output-network",
                "gladbeck-v3.0-networkGladbeck.xml.gz",
                "--output-network-change-events",
                "gladbeck-v3.0-networkChangeEventsGladbeck.xml.gz",
                "--output-population",
                "gladbeck-v3.0-populationGladbeck.xml.gz",
                "--output-facilities",
                "gladbeck-v3.0-facilitiesGladbeck.xml.gz",
                "--input-crs",
                "EPSG:25832",
                "--network-modes",
                "bike,car",
                "--clean-modes",
                "freight,truck8t,truck18t,truck26t,truck40t,ride",
                "--keep-capacities"
        };

        if (args.length == 0) {
            args = inputScenario;
        }
        new CreateScenarioCutOut().execute(args);
    }
}
