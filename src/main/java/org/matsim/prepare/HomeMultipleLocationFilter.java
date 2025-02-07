package org.matsim.prepare;


import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.options.ShpOptions;

import java.util.*;

/**
 * Same use as {@link org.matsim.application.analysis.HomeLocationFilter} but this class also differs between different polygons in the shapefile.
 */
public class HomeMultipleLocationFilter {
    private Map<String, Set<Id<Person>>> personMapping = new HashMap<>();
    private ShpOptions.Index index;

    /**
     *
     * @param analysisAreaShapeFile .shp-file
     * @param inputCRS Coordinate Reference System as String (e.g. "EPSG:25832")
     * @param columnToMap polygon-attribute that should be used as key of the population mapping.
     *                    If two different polygons have the same value in this attribute, the population inside those
     *                    polygons will be mapped as one category.
     * @param population
     */
    public HomeMultipleLocationFilter(ShpOptions analysisAreaShapeFile, String inputCRS, String columnToMap, Population population){
        //Create an index, searching for the given attribute
        index = analysisAreaShapeFile.createIndex(inputCRS, columnToMap);

        //Map the population
        for(Person p : population.getPersons().values()){
            List<PlanElement> planElements = p.getSelectedPlan().getPlanElements(); // Get the selected plan of Person
            for(PlanElement el : planElements){
                if (el instanceof Activity && ((Activity)el).getType().startsWith("home")) { //Find his home activity
                    Coord coord = ((Activity)el).getCoord();
                    if(index.contains(coord)){ //Check if home lies inside our shapefile
                        personMapping.putIfAbsent(index.query(coord), new HashSet<>()); //Init Set if null
                        personMapping.get(index.query(coord)).add(p.getId()); //Add person to this mapping-category
                        break;
                    }
                }
            }
        }
    }

    /**
     *
     * @param analysisAreaShapeFile .shp-file
     * @param inputCRS Coordinate Reference System as String (e.g. "EPSG:25832")
     * @param columnToMap polygon-attribute that should be used as key of the population mapping.
     *                    If two different polygons have the same value in this attribute, the population inside those
     *                    polygons will be mapped as one category.
     * @param priorityList A List of sets, that contains String with keys. First set has highest priority, second set has second-highest priority, ...
     *                     Elements that are in no set at all have the lowest priority. Only important if agents can be in mutliple shapes (e.g. if they are overlapping)
     * @param population
     */
    public HomeMultipleLocationFilter(ShpOptions analysisAreaShapeFile, String inputCRS, String columnToMap, List<Set<String>> priorityList, Population population){
        //TODO
        //Create an index, searching for the given attribute
        index = analysisAreaShapeFile.createIndex(inputCRS, columnToMap);

        //Map the population
        for(Person p : population.getPersons().values()){
            List<PlanElement> planElements = p.getSelectedPlan().getPlanElements(); // Get the selected plan of Person
            for(PlanElement el : planElements){
                if (el instanceof Activity && ((Activity)el).getType().startsWith("home")) { //Find his home activity
                    Coord coord = ((Activity)el).getCoord();
                    if(index.contains(coord)){ //Check if home lies inside our shapefile
                        personMapping.putIfAbsent(index.query(coord), new HashSet<>()); //Init Set if null
                        personMapping.get(index.query(coord)).add(p.getId()); //Add person to this mapping-category
                        break;
                    }
                }
            }
        }
    }


    /**
     * Checks if the given person lives in the specific polygon-category of the shapefile given by the {@code key}.
     * @param p Person to check
     * @param key Will search in all polygons, that have this {@code key} value in the {@code columnToMap} attribute.
     */
    public boolean checkIfPersonInPolygon(Person p, String key){
        return personMapping.get(key).contains(p.getId());
    }

    /**
     * Gets the category-key in which this person lives.
     * @param p Person to check
     * @return String of category key. Returns {@code null} if person does not live in any category
     */
    public String getCategoryKeyOfPerson(Person p){
        //TODO Fix for person that are in multiple overlapping shapefiles
        for(var e : personMapping.entrySet()){
            if(e.getValue().contains(p.getId())) return e.getKey();
        }
        return null;
    }

    public int occurrencesOfKey(String key){
        return personMapping.get(key).size();
    }
}
