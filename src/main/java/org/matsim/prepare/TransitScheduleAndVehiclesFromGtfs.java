/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import com.conveyal.gtfs.GTFSFeed;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.gtfs.GtfsConverter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.utils.CreatePseudoNetwork;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ikaddoura
 */
@Deprecated
public class TransitScheduleAndVehiclesFromGtfs {

    private static final Logger log = Logger.getLogger(TransitScheduleAndVehiclesFromGtfs.class);

    public Scenario run(String gtfsZipFile, String dateAsString, CoordinateTransformation ct, String prefix) {

        final LocalDate date = LocalDate.parse(dateAsString);

        log.info("GTFS zip file: " + gtfsZipFile);

        // Convert GTFS to matsim transit schedule
        Scenario scenario = createScenarioFromGtfsFile(gtfsZipFile, date, ct);

        //Create a network around the schedule
        new CreatePseudoNetwork(scenario.getTransitSchedule(), scenario.getNetwork(), "pt_" + prefix + "_").createNetwork();

        //Create simple transit vehicles with a pcu of 0
        createTransitVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles(), prefix);
//        
//        new CreateVehiclesForSchedule(scenario.getTransitSchedule(), scenario.getTransitVehicles()).run();
//        scenario.getTransitVehicles().getVehicleTypes().forEach((id, type) -> type.setPcuEquivalents(0));

        // correct network
        scenario.getNetwork().getLinks().values().stream()
                .filter(this::hasImplausibleLength).forEach(implausibleLink -> {
            log.warn("Link length is " + implausibleLink.getLength() + ". Adjust link length for link " + implausibleLink.getId());
            implausibleLink.setLength(1.234);
        });

        // correct schedule
        List<Id<TransitStopFacility>> wrongStopIDs = new ArrayList<>();
        List<Id<TransitLine>> linesWithWrongStopIDs = new ArrayList<>();

        scenario.getTransitSchedule().getFacilities().values().stream()
                .filter(this::hasImplausibleCoordinate)
                .forEach(implausibleStop -> {
                    log.warn("Transit stop coordinate is " + implausibleStop.getCoord().toString() + ". Adding stop " + implausibleStop.getId() + " / " + implausibleStop.getName() + " to the list of wrong stops...");
                    wrongStopIDs.add(implausibleStop.getId());
                });

        // get lines for these stops
        for (Id<TransitStopFacility> id : wrongStopIDs) {
            for (TransitLine line : scenario.getTransitSchedule().getTransitLines().values()) {
                for (TransitRoute route : line.getRoutes().values()) {
                    for (TransitRouteStop stop : route.getStops()) {
                        if (stop.getStopFacility().getId().toString().equals(id.toString())) {
                            linesWithWrongStopIDs.add(line.getId());
                        }
                    }
                }
            }
        }

        //TransitSchedule tS = makeTransitScheduleModifiable(scenario.getTransitSchedule());

        // remove stops
        for (Id<TransitStopFacility> id : wrongStopIDs) {
            log.warn("Removing stop Id " + id);
            var facility = scenario.getTransitSchedule().getFacilities().get(id);
            scenario.getTransitSchedule().removeStopFacility(facility);
        }

        // remove lines
        for (Id<TransitLine> id : linesWithWrongStopIDs) {
            log.warn("Removing transit line " + id);
            var line = scenario.getTransitSchedule().getTransitLines().get(id);
            scenario.getTransitSchedule().removeTransitLine(line);
        }

        return scenario;
    }

    private Scenario createScenarioFromGtfsFile(String fromFile, LocalDate date, CoordinateTransformation transformation) {

        GTFSFeed feed = GTFSFeed.fromFile(fromFile);
        feed.feedInfo.values().stream().findFirst().ifPresent((feedInfo) -> {
            System.out.println("Feed start date: " + feedInfo.feed_start_date);
            System.out.println("Feed end date: " + feedInfo.feed_end_date);
        });
        System.out.println("Parsed trips: " + feed.trips.size());
        System.out.println("Parsed routes: " + feed.routes.size());
        System.out.println("Parsed stops: " + feed.stops.size());
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        GtfsConverter converter = new GtfsConverter(feed, scenario, transformation, false);
        converter.setDate(date);
        converter.convert();
        System.out.println("Converted stops: " + scenario.getTransitSchedule().getFacilities().size());

        return scenario;
    }

    private boolean hasImplausibleCoordinate(TransitStopFacility stop) {
        return !(stop.getCoord().getX() > Double.NEGATIVE_INFINITY) || !(stop.getCoord().getX() < Double.POSITIVE_INFINITY) ||
                !(stop.getCoord().getY() > Double.NEGATIVE_INFINITY) || !(stop.getCoord().getY() < Double.POSITIVE_INFINITY);
    }

    private boolean hasImplausibleLength(Link link) {
        return !(link.getLength() > 0) || !(link.getLength() < Double.POSITIVE_INFINITY);
    }
    
    private static void createTransitVehiclesForSchedule(final TransitSchedule schedule, final Vehicles vehicles, final String idPrefix) {
		VehiclesFactory vehFactotry = vehicles.getFactory();
		
		VehicleType trainVehicleType = vehFactotry.createVehicleType(Id.create(idPrefix + "-train", VehicleType.class));
		trainVehicleType.getCapacity().setSeats( 500 );
		trainVehicleType.getCapacity().setStandingRoom( 0 );
		trainVehicleType.setPcuEquivalents(0.);
		vehicles.addVehicleType(trainVehicleType);
		
		VehicleType subwayVehicleType = vehFactotry.createVehicleType(Id.create(idPrefix + "-subway", VehicleType.class));
		subwayVehicleType.getCapacity().setSeats( 500 );
		subwayVehicleType.getCapacity().setStandingRoom( 0 );
		subwayVehicleType.setPcuEquivalents(0.);
		vehicles.addVehicleType(subwayVehicleType);
		
		VehicleType tramVehicleType = vehFactotry.createVehicleType(Id.create(idPrefix + "-tram", VehicleType.class));
		tramVehicleType.getCapacity().setSeats( 200 );
		tramVehicleType.getCapacity().setStandingRoom( 0 );
		tramVehicleType.setPcuEquivalents(0.);
		vehicles.addVehicleType(tramVehicleType);
		
		VehicleType busVehicleType = vehFactotry.createVehicleType(Id.create(idPrefix + "-bus", VehicleType.class));
		busVehicleType.getCapacity().setSeats( 100 );
		busVehicleType.getCapacity().setStandingRoom( 0 );
		busVehicleType.setPcuEquivalents(0.);
		vehicles.addVehicleType(busVehicleType);

		long vehId = 0;
		for (TransitLine line : schedule.getTransitLines().values()) {

			for (TransitRoute route : line.getRoutes().values()) {
				
				VehicleType vehicleType;
				if (route.getTransportMode().equals("rail")) {
					vehicleType = trainVehicleType;
				} else if (route.getTransportMode().equals("bus")) {
					vehicleType = busVehicleType;
				} else if (route.getTransportMode().equals("tram")) {
					vehicleType = tramVehicleType;
				} else if (route.getTransportMode().equals("subway")) {
					vehicleType = subwayVehicleType;
				} else {
					throw new RuntimeException(route.getTransportMode());
				}
				
				for (Departure departure : route.getDepartures().values()) {
					
					Vehicle veh = vehFactotry.createVehicle(Id.create("pt_" + idPrefix + Long.toString(vehId++), Vehicle.class), vehicleType);
					vehicles.addVehicle(veh);
					departure.setVehicleId(veh.getId());
				}
				
				for (TransitRouteStop stop : route.getStops()) {
					// make sure transit vehicles follow the schedule!
					stop.setAwaitDepartureTime(true);
				}
			}
		}
	}
}

