package org.matsim.analysis;

import org.matsim.core.router.AnalysisMainModeIdentifier;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import java.util.*;
import java.util.stream.Collectors;

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


public class AnalysePlansForSubtourModeChoice {

    private static final RuhrgebietIntermodalAnalysisModeIdentifier mainModeIdentifier = new RuhrgebietIntermodalAnalysisModeIdentifier();
    static final Set<String> RUHRGEBIET_CHAIN_BASED_MODES = Set.of(TransportMode.bike, TransportMode.car);
    private static final Set<String> MODES = Set.of("car", "pt", "bike", "walk");

    public static void main(String[] args) {

        //Population population = PopulationUtils.readPopulation("../../shared-svn/projects/matsim-metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.1-1pct.plans.xml.gz");
        Population population = PopulationUtils.readPopulation("/Users/gregorr/Desktop/TestGlaMoBi/output_metropole-ruhr-v1.0-10pct/metropole-ruhr-v1.0-10pct.output_plans.xml.gz");
//		List<Id<Person>> nonCarUsers = population.getPersons().values().stream()
//				.filter(person -> !isUser(person, TransportMode.car))
//				.map(person -> person.getId())
//				.collect(Collectors.toList());
//
//		nonCarUsers.forEach(nonUser -> population.removePerson(nonUser));


//		List<Id<Person>> stayHome = population.getPersons().values().stream()
//				.filter(person -> TripStructureUtils.getTrips(person.getSelectedPlan()).size() == 0)
//				.map(person -> person.getId())
//				.collect(Collectors.toList());
//		stayHome.forEach(p -> population.removePerson(p));

        Map<Integer, Integer> nrOfSubtours2NrOfAgents = new HashMap<>();
        Map<Integer, Integer> nrOfProblematicSubtours2NrOfAgents = new HashMap<>();

        int nrOfCarAgents = 0;
        int nrOfCarAgentsNotResponsiveToMobilityBudget = 0;
        int totalNrOfSubtours = 0;
        int totalNrOfProblematicSubtours = 0;
        int totalNrOfSubtoursViolatingMassConservation = 0;
        int totalNrOfOpenSubtours = 0;
        int totalNrOfSubtoursWithUnknownModes = 0;
        int nrOfAgentsWithFixedPlans = 0;
        int nrOfAgentsWithPartialProblematicPlan = 0;
        int nrOfAgentsWith0Trips = 0;

        HashSet<TripStructureUtils.Subtour> allSubtours = new HashSet<>();

        for (Person person : population.getPersons().values()) {

            Collection<TripStructureUtils.Subtour> subtours = TripStructureUtils.getSubtours(person.getSelectedPlan());
            allSubtours.addAll(subtours);

            if (TripStructureUtils.getTrips(person.getSelectedPlan()).size() == 0) nrOfAgentsWith0Trips++;

            nrOfSubtours2NrOfAgents.compute(subtours.size(), (k, v) -> v == null ? 1 : v + 1);
            totalNrOfSubtours += (subtours.size());

            int nrOfProblematicSubTours = (int) subtours.stream()
                    .filter(AnalysePlansForSubtourModeChoice::isProblematic)
                    .count();
            totalNrOfProblematicSubtours += nrOfProblematicSubTours;

            totalNrOfSubtoursViolatingMassConservation += subtours.stream()
                    .filter(subtour -> !isMassConserving(subtour))
                    .count();

            totalNrOfOpenSubtours += subtours.stream()
                    .filter(subtour -> !subtour.isClosed())
                    .count();

            totalNrOfSubtoursWithUnknownModes += subtours.stream()
                    .filter(subtour -> containsUnknownMode(subtour))
                    .count();

            List<TripStructureUtils.Subtour> carSubtours = subtours.stream()
                    .filter(AnalysePlansForSubtourModeChoice::hasCarLeg)
                    .collect(Collectors.toList());

            if (carSubtours.size() > 0) {
                nrOfCarAgents++;
                if (carSubtours.stream()
                        .filter(AnalysePlansForSubtourModeChoice::isProblematic)
                        .findAny()
                        .isPresent()) nrOfCarAgentsNotResponsiveToMobilityBudget++;
            }


            if (nrOfProblematicSubTours == subtours.size()) {
                nrOfAgentsWithFixedPlans++;
            } else if (nrOfProblematicSubTours > 0 && nrOfProblematicSubTours < subtours.size()) {
                nrOfAgentsWithPartialProblematicPlan++;
            }
            nrOfProblematicSubtours2NrOfAgents.compute(nrOfProblematicSubTours, (k, v) -> v == null ? 1 : v + 1);

        }

        System.out.println("###################");
        System.out.println("### total number of agents: " + population.getPersons().size());
        System.out.println("###################");
        System.out.println("### number of agents with 0 trips: " + nrOfAgentsWith0Trips);
        System.out.println("###################");
        System.out.println("### total number of subtours: " + totalNrOfSubtours);
        System.out.println("### number of problematic subtours: " + totalNrOfProblematicSubtours + " = " + (100 * (double) totalNrOfProblematicSubtours / ((double) totalNrOfSubtours) + "%"));
        System.out.println("### number of subtours violating mass conservation: " + totalNrOfSubtoursViolatingMassConservation + " = "
                + (100 * (double) totalNrOfSubtoursViolatingMassConservation / ((double) totalNrOfSubtours) + "%"));
        System.out.println("### number of open subtours: " + totalNrOfOpenSubtours + " = "
                + (100 * (double) totalNrOfOpenSubtours / ((double) totalNrOfSubtours) + "%"));
        System.out.println("### number of subtours containing unknown mode(s): " + totalNrOfSubtoursWithUnknownModes + " = "
                + (100 * (double) totalNrOfSubtoursWithUnknownModes / ((double) totalNrOfSubtours) + "%"));
        System.out.println("###################");
        System.out.println("###################");
        nrOfSubtours2NrOfAgents.forEach((k, v) -> System.out.println("### " + k + " subtours:\t" + v + " agents"));
        System.out.println("###################");
        nrOfProblematicSubtours2NrOfAgents.forEach((k, v) -> System.out.println("### " + k + " problematic subtours:\t" + v + " agents"));
        System.out.println("###################");
        System.out.println("### number of agents with completely problematic plans: " + nrOfAgentsWithFixedPlans + " = " + (100 * (double) nrOfAgentsWithFixedPlans / ((double) population.getPersons().size()) + "%"));
        System.out.println("### number of agents with partially problematic plans: " + nrOfAgentsWithPartialProblematicPlan + " = " + (100 * (double) nrOfAgentsWithPartialProblematicPlan / ((double) population.getPersons().size()) + "%"));
        System.out.println("###################");
        int mobileButFixed = nrOfAgentsWithFixedPlans - nrOfAgentsWith0Trips;
        System.out.println("### number of MOBILE agents with completely problematic plans: "
                + mobileButFixed + " = " + (100 * (double) mobileButFixed / ((double) population.getPersons().size() - nrOfAgentsWith0Trips) + "%"));
        System.out.println("###################");
        System.out.println("### number of car agents " + nrOfCarAgents);
        System.out.println("###################");
        List<TripStructureUtils.Subtour> subToursWithAtLeastOneCarLeg = allSubtours.stream()
                .filter(AnalysePlansForSubtourModeChoice::hasCarLeg)
                .collect(Collectors.toList());

        List<TripStructureUtils.Subtour> problematicCarSubTours = subToursWithAtLeastOneCarLeg.stream()
                .filter(AnalysePlansForSubtourModeChoice::isProblematic)
                .collect(Collectors.toList());
        System.out.println("### number of subtours with at least one car leg: " + subToursWithAtLeastOneCarLeg.size());
        System.out.println("### number of subtours with at least one car leg that are problematic: " + problematicCarSubTours.size()
                + " = " + (100 * (double) problematicCarSubTours.size() / (double) subToursWithAtLeastOneCarLeg.size()) + "%");

        System.out.println("######FINISHED#####");
    }

    private static boolean hasCarLeg(TripStructureUtils.Subtour subtour) {
        return subtour.getTrips().stream()
                .flatMap(trip -> trip.getLegsOnly().stream())
                .filter(leg -> leg.getMode().equals(TransportMode.car))
                .findAny()
                .isPresent();
    }

    /**
     * in other words: will not be mutated by ChooseRandomLegModeForSubtour
     *
     * @param subtour
     * @return
     */
    private static boolean isProblematic(TripStructureUtils.Subtour subtour) {
        return !subtour.isClosed() || !isMassConserving(subtour) || containsUnknownMode(subtour);
    }


    ///-----------------------copied from ChooseRandomLegModeForSubtour

    private static boolean containsUnknownMode(final TripStructureUtils.Subtour subtour) {
        for (TripStructureUtils.Trip trip : subtour.getTrips()) {
            if (!MODES.contains(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                return true;
            }
        }
        return false;
    }

    static boolean isMassConserving(final TripStructureUtils.Subtour subtour) {
        for (String mode : RUHRGEBIET_CHAIN_BASED_MODES) {
            if (!isMassConserving(subtour, mode)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isMassConserving(
            final TripStructureUtils.Subtour subtour,
            final String mode) {
        final Activity firstOrigin =
                findFirstOriginOfMode(
                        subtour.getTrips(),
                        mode);

        if (firstOrigin == null) {
            return true;
        }

        final Activity lastDestination =
                findLastDestinationOfMode(
                        subtour.getTrips(),
                        mode);

        return atSameLocation(firstOrigin, lastDestination);
    }


    private static boolean atSameLocation(Activity firstLegUsingMode,
                                          Activity lastLegUsingMode) {
        return firstLegUsingMode.getFacilityId() != null ?
                firstLegUsingMode.getFacilityId().equals(
                        lastLegUsingMode.getFacilityId()) :
                firstLegUsingMode.getLinkId().equals(
                        lastLegUsingMode.getLinkId());
    }

    private static Activity findLastDestinationOfMode(
            final List<TripStructureUtils.Trip> tripsToSearch,
            final String mode) {
        final List<TripStructureUtils.Trip> reversed = new ArrayList<>(tripsToSearch);
        Collections.reverse(reversed);
        for (TripStructureUtils.Trip trip : reversed) {
            if (mode.equals(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                return trip.getDestinationActivity();
            }
        }
        return null;
    }

    private static Activity findFirstOriginOfMode(
            final List<TripStructureUtils.Trip> tripsToSearch,
            final String mode) {
        for (TripStructureUtils.Trip trip : tripsToSearch) {
            if (mode.equals(mainModeIdentifier.identifyMainMode(trip.getTripElements()))) {
                return trip.getOriginActivity();
            }
        }
        return null;
    }

}

