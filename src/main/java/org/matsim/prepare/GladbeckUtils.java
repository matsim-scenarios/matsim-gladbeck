package org.matsim.prepare;

import org.matsim.api.core.v01.population.Person;



public class GladbeckUtils{
    private GladbeckUtils(){} // do not instantiate


    public static void setPersonToDifferentCitizenship(Person person) {
        person.getAttributes().putAttribute("citizenship", "diffrent");
    }

    public static double getShareOfDiffrentCitizenship() {
        // this number  is derived from:  respos/shared-svn/projects/GlaMoBi/data/sozio-demographischen_Daten/2023-18-04_Auswertung_Staatsangehoerigkeiten.xlsx
        return 0.19;
    }


}