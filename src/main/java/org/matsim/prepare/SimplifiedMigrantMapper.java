package org.matsim.prepare;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

public class SimplifiedMigrantMapper {
    private static final Logger logger = LoggerFactory.getLogger(SimplifiedMigrantMapper.class);

    private final Population population;
    private final HomeMultipleLocationFilter filter;
    private final Map<Id<Person>, String> personRegion = new HashMap<>();
    private final Map<String, List<Id<Person>>> regionPersons = new HashMap<>();
    private final Map<String, Integer> districtTotals = new HashMap<>();
    private final Map<String, Integer> districtMigrantCounts = new HashMap<>();
    private int migrantCount = 0;

    public SimplifiedMigrantMapper(Population population, String shpPath, String diAttr, String crs, Integer sampleLimit) {
        this.population = population;
        ShpOptions shp = new ShpOptions(Path.of(shpPath), crs, StandardCharsets.UTF_8);
        this.filter = new HomeMultipleLocationFilter(shp, crs, diAttr, population);

        collectDistrictData();
        assignDeterministicMigrants(sampleLimit);
        logDistrictStats();
    }

    private void collectDistrictData() {
        for (Person p : population.getPersons().values()) {
            if (!"person".equals(p.getAttributes().getAttribute("subpopulation"))) continue;
            String region = filter.getCategoryKeyOfPerson(p);
            if (region == null) continue;

            personRegion.put(p.getId(), region);
            regionPersons.computeIfAbsent(region, k -> new ArrayList<>()).add(p.getId());
            districtTotals.merge(region, 1, Integer::sum);
        }
    }

    private void assignDeterministicMigrants(Integer sampleLimit) {
        Random rand = MatsimRandom.getRandom();

        for (var entry : regionPersons.entrySet()) {
            String region = entry.getKey();
            List<Id<Person>> ids = entry.getValue();
            int totalInRegion = ids.size();

            double prob = computeRegionProbability(region);
            int expectedMigrants = (int) Math.round(prob * totalInRegion);

            Collections.shuffle(ids, rand);

            for (int i = 0; i < ids.size(); i++) {
                if (sampleLimit != null && migrantCount >= sampleLimit) break;

                Person p = population.getPersons().get(ids.get(i));
                boolean isMigrant = i < expectedMigrants;
                p.getAttributes().putAttribute(GladbeckUtils.MIGRANT, isMigrant);

                if (isMigrant) {
                    migrantCount++;
                    districtMigrantCounts.merge(region, 1, Integer::sum);
                }
            }
            if (sampleLimit != null && migrantCount >= sampleLimit) break;
        }
    }

    private void logDistrictStats() {
        logger.info("✅ Total migrants assigned: {}", migrantCount);
        for (String region : districtTotals.keySet()) {
            int total = districtTotals.getOrDefault(region, 0);
            int actual = districtMigrantCounts.getOrDefault(region, 0);
            double expectedShare = 100.0 * computeRegionProbability(region);
            double actualShare = total > 0 ? 100.0 * actual / total : 0.0;

            logger.info("District '{}': total={}, migrants={}, expected share={}%, actual share={}%",
                    region, total, actual,
                    String.format("%.2f", expectedShare),
                    String.format("%.2f", actualShare)
            );
        }
    }

    private double computeRegionProbability(String region) {
        return switch (region) {
            case "Übergangsheim" -> 1.0;
            case "Mitte I (West)" -> 0.2246;
            case "Mitte II (Ost)" -> 0.1460;
            case "Zweckel" -> 0.1327;
            case "Alt-Rentfort" -> 0.0530;
            case "Rentfort-Nord" -> 0.1515;
            case "Schultendorf" -> 0.1101;
            case "Ellinghorst" -> 0.1048;
            case "Butendorf" -> 0.2180;
            case "Brauck" -> 0.2921;
            case "Rosenhügel" -> 0.2061;
            default -> 0.0;
        };
    }

    public static void main(String[] args) {
        String plansFile = "/Users/gregorr/Documents/work/respos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/input/v3.0/gladbeck-v3.0-plansGladbeck3pct.xml.gz";
        String shapefile = "/Users/gregorr/Documents/work/respos/shared-svn/projects/GlaMoBi/data/shp-files/gladbeck_stadtbezirke_osm_25832/gladbeck_stadtbezirke_osm_25832.shp";
        String attribute = "Name";
        String crs = "EPSG:25832";
        String outputFile = "output/migrants-mapped.xml.gz";
        Integer sampleLimit = null;  // or use an integer limit

        Population population = PopulationUtils.readPopulation(plansFile);

        SimplifiedMigrantMapper mapper = new SimplifiedMigrantMapper(
                population, shapefile, attribute, crs, sampleLimit);

        PopulationUtils.writePopulation(population, outputFile);

        System.out.println("Migrants assigned: " + mapper.migrantCount);
    }
}
