package org.matsim.prepare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

/**
 * Creates a boolean-map for the population, that shows whether this person is a migrant and puts it into the population.
 * Used in GlaMoBi-Project, containing migrant data for Gladbeck.
 */
public class MigrantMapper {
    private int migrants;
    private Population pop;
    private final HomeMultipleLocationFilter filter;

    private final Map<Id<Person>, Double> migrantProbabilityMap;

    //TODO DEBUG
    Map<String, Integer> district_amounts = new HashMap<>();
    Map<String, Integer> district_migrant_amounts = new HashMap<>();

    /**
     * An activity location.
     */
    private class Location{
        String name;
        Coord coord;
        double migrantProbability;
        Location(String name, Coord coord, double migrantProbability){
            this.name = name;
            this.coord = coord;
            this.migrantProbability = migrantProbability;
        }
    }

    /**
     * Computes the probability of every Person to be a migrant and saves it as a map in {@code migrantProbabilityMap}.
     * @param populationPath Path to the population file
     * @param cityDistrictSHPPath Path to the shapefile containing the districts of the city
     * @param districtIdentifierName Identifying attribute of the polygons of the shapefile (e.g. Name)
     * @param CRS Coordinate Reference System (e.g. EPSG:25832)
     */
    public MigrantMapper(String populationPath, String cityDistrictSHPPath, String districtIdentifierName, String CRS){ // TODO custom filepaths
        pop = PopulationUtils.readPopulation(populationPath);
        ShpOptions shp = new ShpOptions(Path.of(cityDistrictSHPPath), CRS, StandardCharsets.UTF_8);
        ShpOptions.Index index = shp.createIndex(CRS, districtIdentifierName);
        filter = new HomeMultipleLocationFilter(shp, CRS, districtIdentifierName, pop);
        migrantProbabilityMap = new HashMap<>();

        for(Person p : pop.getPersons().values()){
            migrantProbabilityMap.put(p.getId(), computeRefugeeProbability(p));
        }

        assignMigrantMapToPopulation();
    }

    /**
     * @return the probability of the given person to be a migrant
     */
    double computeRefugeeProbability(Person p){
        int age = (int) p.getAttributes().getAttribute("age");
        double income = (double) p.getAttributes().getAttribute("income");
        String gender = (String) p.getAttributes().getAttribute("sex");
        String region = filter.getCategoryKeyOfPerson(p);

        //TODO DEBUG
        district_amounts.putIfAbsent(region, 0);
        district_amounts.put(region, district_amounts.get(region)+1);
        //DEBUG END

        //TODO Initalizer values, in case that the given person-data is
        double ageProbability = 0.;     //P(M|A,G) Wahrscheinlhckeit, dass p ein Migrant ist unter Beruecksichtigung des Alters und des Geschlechts
        double incomeProbability = 0.;  //P(M|€) Wahrscheinlichkeit, dass p ein Migrant ist unter Beruecksichtigung des Einkommens
        double regionProbability = 0.1885;  //P(M|V) Wahrscheinlichkeit, dass p ein Migrant ist unter Beruecksichtigung des Wohnorts TODO
        double destProbability = 0.;    //P(M|Z) Wahrscheinlichkeit, dass p ein Migrant ist unter Beruecksichtigung der Zielaktivitaeten TODO

        //Age-Gender Probability
        if(gender.equals("m")){
            if(age < 18){
                ageProbability = 0.2;
            } else if (age < 29){
                ageProbability = 0.2688;
            } else if (age < 45){
                ageProbability = 0.2278;
            } else if (age < 65){
                ageProbability = 0.126;
            } else{
                ageProbability = 0.062;
            }
        } else if(gender.equals("w")){
            if(age < 18){
                ageProbability = 0.1505;
            } else if (age < 29){
                ageProbability = 0.0672;
            } else if (age < 45){
                ageProbability = 0.1122;
            } else if (age < 65){
                ageProbability = 0.063;
            } else{
                ageProbability = 0.0729;
            }
        }

        //Income Probability
        if (income == 0.) {
            incomeProbability = 0.4109;
        } else if (income < 500){
            incomeProbability = 0.4736;
        } else if (income < 1000){
            incomeProbability = 0.3583;
        } else if (income < 1500){
            incomeProbability = 0.2535;
        } else if (income < 2000){
            incomeProbability = 0.2477;
        } else if (income < 2500){
            incomeProbability = 0.2353;
        } else if (income < 3000){
            incomeProbability = 0.2274;
        } else if (income < 3500){
            incomeProbability = 0.2044;
        } else { // income >= 3500
            incomeProbability = 0.1939;
        }

        //RegionProbability
        if(region != null){
            regionProbability = switch (region) {
                case "Übergangsheim" -> 10; //TODO
                case "Mitte I" -> 0.2246;
                case "Mitte II" -> 0.1460;
                case "Zweckel" -> 0.1327;
                case "Alt-Rentford" -> 0.0530;
                case "Rentford-Nord" -> 0.1515;
                case "Schultendorf" -> 0.1101;
                case "Ellinghorst" -> 0.1048;
                case "Butendorf" -> 0.2180;
                case "Brauck" -> 0.2921;
                case "Rosenhügel" -> 0.2061;
                default -> regionProbability;
            };
        }

        //DestinationProbability
        destProbability = checkIfActivitiesAreRelevant(p);

        //Probability summary
        //TODO Create an actual stochastic procedure
        if(destProbability != -1) return (incomeProbability + ageProbability + 3*regionProbability + destProbability) / 6;
        return (incomeProbability + ageProbability + 3*regionProbability) / 5;
    }

    /**
     * CHecks if any of the agents activities is next to an important social facility.
     * @return The probability of this agent being a migrant using activities as hints
     */
    private double checkIfActivitiesAreRelevant(Person p){
        //TODO Make this more useful
        Location[] locations = new Location[]{
                new Location("Büro für Interkulturelle Arbeit", new Coord(361436.57,5712850.89), 0.5),
                new Location("Jugendmigrationsdienst", new Coord(367983.90,5708550.07), 0.5),
                new Location("Amt Für Soziales Und Wohnen", new Coord(360574.17,5715060.27), 0.5),
                new Location("Jobcenter", new Coord(360610.97,5715105.85), 0.5)
        };

        List<Double> probabilities = new LinkedList<>();

        for(PlanElement a : p.getSelectedPlan().getPlanElements()){
            if(a instanceof Activity){
                for(Location l : locations){
                    if(NetworkUtils.getEuclideanDistance(((Activity) a).getCoord(), l.coord) < 50){
                        probabilities.add(l.migrantProbability);
                    }
                }
            }
        }

        if(probabilities.isEmpty()) return -1;

        double probability = 0.;
        for(double prob : probabilities){
            probability += prob;
        }
        return probability/probabilities.size();
    }

    /**
     * Adds a boolean-attribute "isMigrant" to every Person of the given population.
     * NOTE: This is a non-deterministic method. It uses the computed probabilities.
     * @return the amount of migrants in this population
     */
    private void assignMigrantMapToPopulation(){
        int totalMigrants = 0;
        Random rand = new Random();
        for(var e : migrantProbabilityMap.entrySet()){
            double r = e.getValue();
            boolean isMigrant = rand.nextInt(1000) < r*1000;
            pop.getPersons().get(e.getKey()).getAttributes().putAttribute("isMigrant", isMigrant);
            if (isMigrant) totalMigrants++;
            //TODO DEBUG
            if (isMigrant){
                district_migrant_amounts.putIfAbsent(filter.getCategoryKeyOfPerson(pop.getPersons().get(e.getKey())), 0);
                district_migrant_amounts.put(filter.getCategoryKeyOfPerson(pop.getPersons().get(e.getKey())), district_migrant_amounts.get(filter.getCategoryKeyOfPerson(pop.getPersons().get(e.getKey())))+1);
            }
            //DEBUG END<
        }
        this.migrants = totalMigrants;
    }

    public Map<Id<Person>, Double> getMigrantProbabilityMap(){
        return migrantProbabilityMap;
    }

    public Population getPopulation(){
        return pop;
    }

    public int getMigrantAmount(){
        return migrants;
    }

    public static void main(String[] args) {
        MigrantMapper detecter = new MigrantMapper(
                "../shared-svn/projects/GlaMoBi/matsim-input-files/gladbeck-v1.3-10pct.plans-cleaned.xml.gz",
                "../stuff/gladbeck_stadtbezirke_osm_25832.shp",
                "Name",
                "EPSG:25832"
                );
        System.out.println(detecter.getMigrantAmount());
        System.out.println(detecter.district_amounts);
        System.out.println(detecter.district_migrant_amounts);

        //TODO MATSIM Random

        //DEBUG

        /*Ergebnisse 1
        Mitte I: 182 ~> 1820 vgl. 2670 (
        Mitte II: 143 ~> 1430 vgl. 1122
        Zweckel: 155 ~> 1550 vgl. 1471
        Alt-Rentford: 66 ~> 660 vgl. 230
        Rentford-Nord: 117 ~> 1170 vgl. 1175
        Schultendorf: 28 ~> 280 vgl. 257
        Ellinghorst: 41 ~> 410 vgl. 311

        Ergebnisse 2
        Mitte I: 203/1033=19,65%    vgl. 22,20%   diff. (-2,55%)
        Mitte II: 100/658=15,20%    vgl. 14,60%   diff. (+0,60%)
        Zweckel: 149/944=15,78%     vgl. 13,27%   diff. (+2,51%)
        Alt-Rentford: 71/395=17,97% vgl. 5,30%    diff. (+12,67%)
        Schultendorf: 38/214=17,76% vgl. 11,02%   diff. (+6,74)
        Ellinghorst: 30/273=10,99%  vgl. 10,48%   diff. (+0,51%)
        Butendorf: 208/894=23,38%   vgl. 21,80%   diff. (+1,58%)
        Brauck: 257/1014=25,35%     vgl. 29,21%   diff. (-3,86%)
        Rosenhügel: 94/462=20,35%   vgl. 20,61%   diff. (-0,26%)

        TOTAL: 6521 ~> 65210        vgl. 78565    diff. (-17,00%)
        TOTAL_M: 1266 ~> 12660      vgl. 14806    diff. (-14,50%)

        TOTAL_%: 18,8%              vgl. 18,85%
        */

    }
}
