package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.analysis.HomeLocationFilter;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.gbl.MatsimRandom;

public class AssignPersonAttributes {

    private static final Logger log = LogManager.getLogger(AssignPersonAttributes.class);

    public static void assigningDifferentCitizenship(Scenario scenario, ShpOptions shp) {

        for (Person p: scenario.getPopulation().getPersons().values()) {
            if (shp != null) {
                HomeLocationFilter homeLocationFilter = new HomeLocationFilter(shp, scenario.getConfig().global().getCoordinateSystem(), scenario.getPopulation());
                if (homeLocationFilter.test(p) == true) {
                    log.info("person p is resident in Gladbeck" + p);
                    double random = MatsimRandom.getRandom().nextDouble();
                    if (random <= GladbeckUtils.getShareOfDiffrentCitizenship()) {
                        log.info("set person " + p + "to different citizenship");
                        GladbeckUtils.setPersonToDifferentCitizenship(p);
                    }
                }
            } else {
                log.warn("no shape file defined assigning attribute to everyone in the population");
                double random = MatsimRandom.getRandom().nextDouble();
                if (random <= GladbeckUtils.getShareOfDiffrentCitizenship()) {
                    log.info("set person " + p + "to different citizenship");
                    GladbeckUtils.setPersonToDifferentCitizenship(p);
                }
            }


        }
    }
}
