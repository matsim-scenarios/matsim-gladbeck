package org.matsim.prepare;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.application.MATSimAppCommand;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import picocli.CommandLine;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

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
		var filteredNetwork = network.getLinks().values().stream()
				.filter(link -> geoFilter.covers(MGC.coord2Point(link.getFromNode().getCoord())) || geoFilter.covers(MGC.coord2Point(link.getToNode().getCoord())))
				.collect(NetworkUtils.getCollector());

		if (policies.contains(Policy.CycleStreets)) applyCycleStreets(filteredNetwork);
		if (policies.contains(Policy.SuperFast)) applySuperFast(filteredNetwork);
		if (policies.contains(Policy.CyclewayEverywhere)) applyCyclewayEverywhere(filteredNetwork, bicycleFreedspeed);
		if (policies.contains(Policy.SuperSmooth)) applySuperSmooth(filteredNetwork);

		var mergedNetwork = network.getLinks().values().stream()
				.map(link -> filteredNetwork.getLinks().containsKey(link.getId()) ? filteredNetwork.getLinks().get(link.getId()) : link)
				.collect(NetworkUtils.getCollector());

		NetworkUtils.writeNetwork(mergedNetwork, outputNetwork);

		return 0;
	}

	/**
	 * Give all links asphalt surface, as well as a cycleway. This yields the highest scores and the highest speeds.
	 */
	private static void applySuperSmooth(Network network) {

		for (var link : network.getLinks().values()) {
			link.getAttributes().putAttribute("cycleway", "yes");
			link.getAttributes().putAttribute("surface", "asphalt");
		}
	}

	/**
	 * Change infrastructure speed factor on all streets to double the speed of bicycles
	 */
	private static void applySuperFast(Network network) {

		for (var link : network.getLinks().values()) {
			link.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRASTRUCTURE_SPEED_FACTOR, 1.0);
		}
	}

	/**
	 * add a bicycle link next to each link in case it in not a bike only link anyway.
	 */
	private static void applyCyclewayEverywhere(Network network, double bicycleFreedspeed) {

		var allowedModes = Set.of(TransportMode.bike);
		var cycleways = network.getLinks().values().stream()
				.filter(link -> link.getAllowedModes().size() == 1 && link.getAllowedModes().contains(TransportMode.bike))
				.map(link -> {
					var id = link.getId().toString() + "_cyev";
					var newLink = network.getFactory().createLink(Id.createLinkId(id), link.getFromNode(), link.getToNode());
					newLink.getAttributes().putAttribute("type", "cycleway");
					newLink.getAttributes().putAttribute("cycleway", "yes");
					newLink.getAttributes().putAttribute("surface", "asphalt");
					newLink.getAttributes().putAttribute("smoothness", "excellent");

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

		var allowedModes = Set.of(TransportMode.bike);
		network.getLinks().values().stream()
				.filter(link -> majorRoadTypes.contains((String)link.getAttributes().getAttribute("type")))
				.forEach(link -> link.setAllowedModes(allowedModes));
	}

	enum Policy {
		SuperSmooth, CyclewayEverywhere, SuperFast, CycleStreets
	}
}