package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jaxb.core.v2.TODO;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.application.prepare.network.CleanNetwork;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.MultimodalNetworkCleaner;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class add bike policies to the network. And can be run with the following parameters for example
 * --in
 * "shared-svn\projects\GlaMoBi\matsim-input-files\gladbeck-v1.0.network_resolutionHigh-with-pt-with-bike.xml.gz"
 * --on
 * "shared-svn\projects\GlaMoBi\matsim-input-files\all-push-and-pull.network.xml.gz"
 * --shp
 * "shared-svn\projects\GlaMoBi\data\shp-files\Gladbeck.shp"
 * --p
 * SuperSmooth
 * --p
 * CyclewayEverywhere
 * --p
 * SuperFast
 * --p
 * CycleStreets
 * comment to merge */
@CommandLine.Command(name = "bicycle-policies")
public class BicyclePolicies implements MATSimAppCommand {

	private static final Logger log = LogManager.getLogger(BicyclePolicies.class);

	private static final Set<String> majorRoadTypes = Set.of("motorway", "motorway_trunk", "trunk", "trunk_link", "primary", "primary_link", "secondary", "secondary_link", "tertiary", "tertiary_link");

	@CommandLine.Option(names = {"--input-network", "--in" }, required = true)
	private String inputNetwork;

	@CommandLine.Option(names = {"--output-network", "--on" }, required = true)
	private String outputNetwork;

	@CommandLine.Option(names = { "--policy", "--p"}, required = true)
	private Set<Policy> policies = new HashSet<>();

	@CommandLine.Option(names = {"--bicycle-freespeed", "--bf"})
	private double bicycleFreedspeed = 6.82; // taken from vehicles file in metropole-ruhr-scenario https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.0.mode-vehicles.xml

	@SuppressWarnings("FieldMayBeFinal")
	@CommandLine.Option(names = {"--shp"}, required = true)
	private Path shp;

	public static void main(String[] args) {
		System.exit(new CommandLine(new BicyclePolicies()).execute(args));
	}

	@Override
	public Integer call() {

		log.info(policies.toString());

		var geoFilter = ShapeFileReader.getAllFeatures(shp.toString()).stream()
				.map(f -> (Geometry)f.getDefaultGeometry())
				.findAny()
				.orElseThrow();
		var network = NetworkUtils.readNetwork(inputNetwork);

		applyPolicyChanges(network, geoFilter, policies, bicycleFreedspeed);

		NetworkUtils.writeNetwork(network, outputNetwork);
		return 0;
	}

	public static void applyPolicyChanges(Network network, Geometry geoFilter, Set<Policy> policies, double bicycleFreedspeed) {
		var filteredNetwork = network.getLinks().values().stream()
				.filter(link -> !link.getAllowedModes().contains(TransportMode.pt)) // pt links are separate, so this test is sufficient
				.filter(link -> geoFilter.covers(MGC.coord2Point(link.getFromNode().getCoord())) || geoFilter.covers(MGC.coord2Point(link.getToNode().getCoord())))
				.collect(NetworkUtils.getCollector());

		// in case one want to execute a combination of the policies the order in which those changes are applied matter
		// at the moment
		if (policies.contains(Policy.CycleStreets)) applyCycleStreets(filteredNetwork);
		if (policies.contains(Policy.CyclewayEverywhere)) applyCyclewayEverywhere(filteredNetwork, bicycleFreedspeed);
		if (policies.contains(Policy.SuperFast)) applySuperFast(filteredNetwork);
		if (policies.contains(Policy.SuperSmooth)) applySuperSmooth(filteredNetwork);
		if (policies.contains(Policy.EBikeCity)) applyEBikeCity(filteredNetwork);

		for (var link : filteredNetwork.getLinks().values()) {

			// replace links in original network with links from filtered network which we have altered.
			network.removeLink(link.getId());
			network.addLink(link);
		}

		MultimodalNetworkCleaner multimodalNetworkCleaner = new MultimodalNetworkCleaner(network);
		multimodalNetworkCleaner.run(Collections.singleton(TransportMode.car));
		multimodalNetworkCleaner.run(Collections.singleton(TransportMode.bike));
		multimodalNetworkCleaner.run(Collections.singleton(TransportMode.ride));
	}

	/**
	 * Give all links asphalt surface, as well as a cycleway. This yields the highest scores and the highest speeds.
	 */
	private static void applySuperSmooth(Network network) {

		log.info("Adding tags: cycleway:yes, surface:asphalt, smoothness:excellent to all links in the network");

		for (var link : network.getLinks().values()) {
			link.getAttributes().putAttribute("cycleway", "yes");
			link.getAttributes().putAttribute("surface", "asphalt");
			link.getAttributes().putAttribute("smoothness", "excellent");
		}
	}

	/**
	 * Change infrastructure speed factor on all streets to double the speed of bicycles
	 */
	private static void applySuperFast(Network network) {

		log.info("Adding infrastructure speed factor of 1.0 to all links in the network.");

		for (var link : network.getLinks().values()) {
			link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.0);
		}
	}

	/**
	 * Change infrastructure speed factor on all streets to double the speed of bicycles and half the capacity
	 */
	private static void applyEBikeCity(Network network) {

		log.info("Adding infrastructure speed factor of 1.0 to all links in the network.");

		for (var link : network.getLinks().values()) {
			link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.0);
			if (link.getAllowedModes().contains(TransportMode.car)) {
				link.setCapacity(link.getCapacity() * 0.5);
				if (link.getNumberOfLanes() > 2.0) {
					link.setNumberOfLanes(link.getNumberOfLanes() * 0.5);
				}
			}
		}
	}


	/**
	 * add a bicycle link next to each link in case it in not a bike only link anyway.
	 */
	private static void applyCyclewayEverywhere(Network network, double bicycleFreedspeed) {

		log.info("Adding cycle ways to all links which were not bicycle ways already.");

		var allowedModes = Set.of(TransportMode.bike);
		var cycleways = network.getLinks().values().stream()
				.filter(link -> !isBicycleOnly(link))
				.map(link -> {
					var id = link.getId().toString() + "_cyev";
					var newLink = network.getFactory().createLink(Id.createLinkId(id), link.getFromNode(), link.getToNode());
					newLink.getAttributes().putAttribute("type", "cycleway");
					newLink.getAttributes().putAttribute("cycleway", "yes");
					newLink.getAttributes().putAttribute("surface", "asphalt");
					newLink.getAttributes().putAttribute("smoothness", "excellent");
					newLink.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 0.5);
					//TODO
					// this is needed because accessEgressModeToLinkPlusTimeConstant is still active.........
					NetworkUtils.setLinkEgressTime(newLink, TransportMode.bike, 0.0);
					NetworkUtils.setLinkAccessTime(newLink, TransportMode.bike, 0.0);
					var origid = link.getAttributes().getAttribute("origid");
					origid = origid == null ? "" : origid;
					newLink.getAttributes().putAttribute("origid", origid);
					newLink.setCapacity(10000);
					newLink.setFreespeed(bicycleFreedspeed);
					newLink.setAllowedModes(allowedModes);
					return newLink;
				})
				.collect(Collectors.toSet());

		for (var link : cycleways) {
			network.addLink(link);
		}
	}

	/**
	 * Close minor roads for car traffic
	 */
	private static void applyCycleStreets(Network network) {

		log.info("Converting all minor streets to cycle only streets");

		var allowedModes = Set.of(TransportMode.bike);
		network.getLinks().values().stream()
				.filter(link -> link.getAttributes().getAttribute("type") != null)
				.filter(link -> !majorRoadTypes.contains((String) link.getAttributes().getAttribute("type")))
				.forEach(link -> link.setAllowedModes(allowedModes));
	}

	private static boolean isBicycleOnly(Link link) {
		return link.getAllowedModes().size() == 1 && link.getAllowedModes().contains(TransportMode.bike);
	}

	public enum Policy {
		SuperSmooth, CyclewayEverywhere, SuperFast, CycleStreets, EBikeCity
	}
}