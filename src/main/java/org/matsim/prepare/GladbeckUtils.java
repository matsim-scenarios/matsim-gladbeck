package org.matsim.prepare;

import org.matsim.api.core.v01.population.Person;


public class GladbeckUtils {
    private GladbeckUtils() {
    } // do not instantiate

    public static void setPersonToDifferentCitizenship(Person person) {
        person.getAttributes().putAttribute("citizenship", "diffrent");
    }

    public static double getShareOfDiffrentCitizenship() {
        // this number  is derived from:  respos/shared-svn/projects/GlaMoBi/data/sozio-demographischen_Daten/2023-18-04_Auswertung_Staatsangehoerigkeiten.xlsx
        return 0.19;
    }

    /**
     * Taken from the ruhr utils class for scenario specific attributes and settings. //gr 02.25
     */
    public static final String ONE_HOUR_P_COST = "oneHourPCost";
    public static final String EXTRA_HOUR_P_COST = "extraHourPCost";
    public static final String MAX_DAILY_P_COST = "maxDailyPCost";
    public static final String MAX_P_TIME = "maxPTime";
    public static final String P_FINE = "pFine";
    public static final String RES_P_COSTS = "resPCosts";
    public static final String ZONE_NAME = "zoneName";
    public static final String ZONE_GROUP = "zoneGroup";


}