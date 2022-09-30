/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import playground.vsp.scoring.IncomeDependentUtilityOfMoneyPersonScoringParameters;
import java.util.Random;

public class AssignIncome {

    // TODO why not matsim random, standardized somewhere
    // this is a copy from the matsim-kelheim scenario
    // https://github.com/matsim-scenarios/matsim-kelheim/blob/master/src/main/java/org/matsim/run/prepare/PreparePopulation.java

    private static final Logger log = LogManager.getLogger(AssignIncome.class);

    public static void assignIncomeToPersonSubpopulationAccordingToSNZData(Population population){
        final Random rnd = new Random(1234);

        log.info("start assigning income to persons according to SNZ Plans");

        population.getPersons().values().stream()
                .filter(person ->  {
                    String subpopulation = PopulationUtils.getSubpopulation(person);
                    return subpopulation != null && subpopulation.equals("person"); //only assign income to person subpopulation (not to freight etc.)
                })
                //don't overwrite income attribute (input plans may have income attributes already)
                .filter(person -> person.getAttributes().getAttribute(IncomeDependentUtilityOfMoneyPersonScoringParameters.PERSONAL_INCOME_ATTRIBUTE_NAME) == null)
                .forEach(person -> {
                    String incomeGroupString = (String) person.getAttributes().getAttribute("MiD:hheink_gr2");
                    String householdSizeString = (String) person.getAttributes().getAttribute("MiD:hhgr_gr");
                    int incomeGroup = 0;
                    double householdSize = 1;
                    if (incomeGroupString != null && householdSizeString != null) {
                        incomeGroup = Integer.parseInt(incomeGroupString);
                        householdSize = Double.parseDouble(householdSizeString);
                    }
                    double income = 0;
                    switch (incomeGroup) {
                        case 1:
                            income = 500 / householdSize;
                            break;
                        case 2:
                            income = (rnd.nextInt(400) + 500) / householdSize;
                            break;
                        case 3:
                            income = (rnd.nextInt(600) + 900) / householdSize;
                            break;
                        case 4:
                            income = (rnd.nextInt(500) + 1500) / householdSize;
                            break;
                        case 5:
                            income = (rnd.nextInt(1000) + 2000) / householdSize;
                            break;
                        case 6:
                            income = (rnd.nextInt(1000) + 3000) / householdSize;
                            break;
                        case 7:
                            income = (rnd.nextInt(1000) + 4000) / householdSize;
                            break;
                        case 8:
                            income = (rnd.nextInt(1000) + 5000) / householdSize;
                            break;
                        case 9:
                            income = (rnd.nextInt(1000) + 6000) / householdSize;
                            break;
                        case 10:
                            income = (Math.abs(rnd.nextGaussian()) * 1000 + 7000) / householdSize;
                            break;
                        default:
                            income = 2364; // Average monthly household income per Capita (2021). See comments below for details
                            break;
                        // Average Gross household income: 4734 Euro
                        // Average household size: 83.1M persons /41.5M households = 2.0 persons / household
                        // Average household income per capita: 4734/2.0 = 2364 Euro
                        // Source (Access date: 21 Sep. 2021):
                        // https://www.destatis.de/EN/Themes/Society-Environment/Income-Consumption-Living-Conditions/Income-Receipts-Expenditure/_node.html
                        // https://www.destatis.de/EN/Themes/Society-Environment/Population/Households-Families/_node.html
                        // https://www.destatis.de/EN/Themes/Society-Environment/Population/Current-Population/_node.html;jsessionid=E0D7A060D654B31C3045AAB1E884CA75.live711
                    }
                    PersonUtils.setIncome(person, income);

                });

        log.info("finished");
    }

}
