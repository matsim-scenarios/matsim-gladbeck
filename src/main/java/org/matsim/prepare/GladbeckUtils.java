package org.matsim.prepare;

import org.matsim.api.core.v01.population.Person;

public class GladbeckUtils{
    private GladbeckUtils(){} // do not instantiate
    public static void setPersonToDifferentCitizenship(Person person) {
        person.getAttributes().putAttribute("citizenship", "diffrent");
    }
}