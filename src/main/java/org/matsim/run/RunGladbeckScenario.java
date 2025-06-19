package org.matsim.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.analysis.personMoney.PersonMoneyEventsAnalysisModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.application.MATSimApplication;
import org.matsim.application.analysis.HomeLocationFilter;
import org.matsim.application.analysis.noise.NoiseAnalysis;
import org.matsim.application.options.SampleOptions;
import org.matsim.application.options.ShpOptions;
import org.matsim.application.prepare.population.DownSamplePopulation;
import org.matsim.application.prepare.population.ExtractHomeCoordinates;
import org.matsim.application.prepare.population.FixSubtourModes;
import org.matsim.application.prepare.population.XYToLinks;
import org.matsim.contrib.vsp.pt.fare.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.prepare.AssignPersonAttributes;
import org.matsim.prepare.BicyclePolicies;
import org.matsim.prepare.PrepareOpenPopulation;
import org.matsim.run.policies.KlimaTaler;
import org.matsim.run.policies.ReduceSpeed;
import org.matsim.run.policies.SchoolRoadsClosure;
import org.matsim.run.policies.freePt.PtFareModuleWithFreePt;
import org.matsim.utils.gis.shp2matsim.ShpGeometryUtils;
import picocli.CommandLine;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

@CommandLine.Command(header = ":: Gladbeck Scenario ::", version = RunGladbeckScenario.VERSION)
@MATSimApplication.Prepare({DownSamplePopulation.class, FixSubtourModes.class, XYToLinks.class, ExtractHomeCoordinates.class, BicyclePolicies.class, PrepareOpenPopulation.class})
@MATSimApplication.Analysis({NoiseAnalysis.class})
public class RunGladbeckScenario extends MATSimApplication {

    public static final String VERSION = "v3.0";

    private static final Logger log = LogManager.getLogger(RunGladbeckScenario.class);

    @CommandLine.Mixin
    SampleOptions sample = new SampleOptions(10, 1);

    @CommandLine.Option(names = "--schoolClosure", description = "Measures to ban car on certain links")
    private Set<SchoolRoadsClosure.SchoolClosure> schoolClosure = new HashSet<>();
    @CommandLine.Option(names = "--tempo30Zone", defaultValue = "false", description = "measures to reduce car speed to 30 km/h in a zone")
    boolean slowSpeedZone;
    @CommandLine.Option(names = "--tempo30Streets", defaultValue = "false", description = "measures to reduce car speed to 30 km/h on links definded by a shape file")
    boolean slowSpeedOnDefinedLinks;
    @CommandLine.Option(names = "--simplePtFlat", defaultValue = "false", description = "measures to allow everyone to have free pt")
    boolean scenarioWidePtFlat;
    @CommandLine.Option(names = "--ptFlat", defaultValue = "0", description = "measures to allow people in Gladbeck to have free pt, if set to zero no agent will have free pt")
    int ptFlat;
    @CommandLine.Option(names = "--cityWidePtFlat", defaultValue = "false", description = "measures to allow every resident in Gladbeck to have free pt")
    boolean cityWidePtFlat;
    @CommandLine.Option(names = "--cyclingCourse", defaultValue = "false", description = "measures to increase the ")
    boolean cyclingCourse;
    @CommandLine.Option(names = "--klimaTaler", defaultValue = "0.0", description = "amount of money to give to a person to use pt, walk and bike")
    double klimaTalerMoneyAmount;
    @CommandLine.Mixin
    private ShpOptions shp;
    @CommandLine.Option(names = {"--policy", "--p"})
    private Set<BicyclePolicies.Policy> policies = new HashSet<>();
    @CommandLine.Option(names = {"--bicycle-freespeed", "--bf"})
    private double bicycleFreedspeed = 6.82; // taken from vehicles file in metropole-ruhr-scenario https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.0.mode-vehicles.xml

    //added this for the test
    public RunGladbeckScenario(@Nullable Config config) {
        super(config);
    }

    public RunGladbeckScenario() {
        super(String.format("./scenarios/gladbeck-%s/input/gladbeck-%s-3pct.config.xml", VERSION, VERSION));
    }

    public static void main(String[] args) {
        MATSimApplication.run(RunGladbeckScenario.class, args);
    }

    @Override
    protected Config prepareConfig(Config config) {

        //every config option from the rvr project
        ScenarioUtils.prepareConfig(config, sample, false);

        //bike policies
        if (!policies.isEmpty() && !shp.isDefined()) {
            throw new RuntimeException("A geo filter is required to apply policy changes to the network. Please add a path to a shape file by using the --shp option");
        }

        //scenario wide pt flat
        if (scenarioWidePtFlat) {
            modifyConfigForScenarioWideFreePt(config);
        }

        // this is needed for the school closure case
        config.network().setTimeVariantNetwork(true);
        //these are the network change events for the Gladbeck scenario, they are generated by the network cut out from the base rvr scenario
        config.network().setChangeEventsInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/gladbeck/glamobi/input/v3.0/gladbeck-v3.0-networkChangeEventsGladbeck.xml.gz");

        return config;
    }


    @Override
    protected void prepareScenario(Scenario scenario) {

        //scnario preparation from the rvr project
        ScenarioUtils.prepareScenario(scenario);

        if (slowSpeedZone) {
            ReduceSpeed.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shp.getShapeFile().toString())));
        }

        if (slowSpeedOnDefinedLinks) {
            ReduceSpeed.implementPushMeasuresByModifyingNetworkInArea(scenario.getNetwork(), ShpGeometryUtils.loadPreparedGeometries(IOUtils.resolveFileOrResource(shp.getShapeFile().toString())));
        }

        if (!schoolClosure.isEmpty()) {
            new SchoolRoadsClosure().closeSchoolLinks(schoolClosure, scenario.getNetwork(), 800, 1700);
        }

        if (cyclingCourse) {
            log.info("adding different citizenship's to the agents");
            AssignPersonAttributes.assigningDifferentCitizenship(scenario, shp);
        }

        if (!policies.isEmpty()) {
            //changing the network for the policy
            BicyclePolicies.applyPolicyChanges(scenario.getNetwork(), shp.getGeometry(), policies, bicycleFreedspeed);
            //delete routes from plans and linkId and facility id from activity
            for (var person : scenario.getPopulation().getPersons().values()) {
                var plan = person.getSelectedPlan();
                person.getPlans().clear();
                PopulationUtils.resetRoutes(plan);
                for (var element : plan.getPlanElements()) {
                    if (element instanceof Activity act) {
                        act.setFacilityId(null);
                        act.setLinkId(null);
                    }
                }
                person.addPlan(plan);
            }
        }
    }

    @Override
    protected void prepareControler(Controler controler) {

        super.prepareControler(controler);
        ScenarioUtils.prepareControler(controler);

        if (klimaTalerMoneyAmount != 0.0) {
            log.info("add Klima taler with money amount: " + klimaTalerMoneyAmount);
            KlimaTaler klimaTaler = new KlimaTaler(controler.getScenario().getNetwork(), klimaTalerMoneyAmount);
            addKlimaTaler(controler, klimaTaler, controler.getConfig(), klimaTalerMoneyAmount);
        }

        if (ptFlat != 0 || cityWidePtFlat) {
            addFreePt(controler);

        }
        //set the vsp defaults checking level to abort, so that we can catch errors in the config
        controler.getConfig().vspExperimental().setVspDefaultsCheckingLevel(VspExperimentalConfigGroup.VspDefaultsCheckingLevel.abort);

    }

    /**
     * This method adds the free public transport to the controler.
     * It creates a list of agents that will have free public transport
     * If cityWidePtFlat is true, all agents living in Gladbeck will have free public transport.
     * If ptFlat is set to a value greater than 0, that many random agents living in Gladbeck will have free public transport.
     * @param controler the controller to add the free public transport
     */
    private void addFreePt(Controler controler) {
        List<Id<Person>> agentsLivingInGladbeck = new ArrayList<>();
        List<Id<Person>> agentsWithPtFlat = new ArrayList<>();
        HomeLocationFilter homeLocationFilter = new HomeLocationFilter(shp, controler.getScenario().getConfig().global().getCoordinateSystem(), controler.getScenario().getPopulation());

        for (Person person : controler.getScenario().getPopulation().getPersons().values()) {
            if (homeLocationFilter.test(controler.getScenario().getPopulation().getPersons().get(person.getId()))) {
                agentsLivingInGladbeck.add(person.getId());
            }
        }

        if (cityWidePtFlat) {
            agentsWithPtFlat.addAll(agentsLivingInGladbeck);
        } else {
            for (int ii = 0; ii < ptFlat; ii++) {
                Random generator = MatsimRandom.getRandom();
                Object[] values = agentsLivingInGladbeck.toArray();
                var randomPerson = (Id<Person>) values[generator.nextInt(values.length)];
                agentsWithPtFlat.add(randomPerson);
                agentsLivingInGladbeck.remove(randomPerson);
            }
        }
        log.info("adding pt flat." + agentsWithPtFlat.size() + " agents will pay no pt cost");
        try {
            writeOutAgents(agentsWithPtFlat);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        controler.addOverridingModule(new PtFareModuleWithFreePt(agentsWithPtFlat));
    }


    /**
     * This method adds the klima taler to the controler.
     * Public as it is used in the test
     * It sets the monetary distance rate for walk and pt based on the klima taler money amount.*
     * @param controler        the controler to add the klima taler to
     * @param klimaTaler       the klima taler to add
     * @param config           the config to use for the klima taler
     * @param klimaTalerMoneyAmount the amount of money to give to a person to use pt, walk and bike
     */
    public static void addKlimaTaler(Controler controler, KlimaTaler klimaTaler, Config config, double klimaTalerMoneyAmount ) {

        //use the monetary distance rate to calculate the money amount for the klima taler for walk and pt
        config.scoring().getModes().get(TransportMode.walk).setMonetaryDistanceRate(0.176 * (klimaTalerMoneyAmount/5000));
        config.scoring().getModes().get(TransportMode.pt).setMonetaryDistanceRate(0.076 * (klimaTalerMoneyAmount/5000));

        //bike is network routed therefore we need a custom event handler
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addEventHandlerBinding().toInstance(klimaTaler);
                addControlerListenerBinding().toInstance(klimaTaler);
                new PersonMoneyEventsAnalysisModule();
            }
        });
    }

    /**
     * This method writes out the ids of the agents that have free public transport.
     * The ids are written to a file called agentsWithFreePt.tsv.
     * @param listOfIds the list of ids of the agents that have free public transport
     * @throws IOException if there is an error writing to the file
     */
    private static void writeOutAgents(List<Id<Person>> listOfIds) throws IOException {
        BufferedWriter writer = IOUtils.getBufferedWriter("agentsWithFreePt.tsv");
        writer.write("Id");
        writer.newLine();
        for (Id<Person> listOfId : listOfIds) {
            writer.write(listOfId.toString());
            writer.newLine();
        }
        writer.close();
    }

    /**
     * This method modifies the config to allow free public transport for all agents.
     * It removes all fare parameters from the config and adds a new fare config group with free pt.
     * The fare is set to zero for all distance classes.
     * @param config the config to modify
     */
    private static void modifyConfigForScenarioWideFreePt(Config config) {
        //remove all fare parameters from the config
        config.removeModule(PtFareConfigGroup.MODULE_NAME);
        //add a new fare config group with free pt
        PtFareConfigGroup ptFareConfigGroup = ConfigUtils.addOrGetModule(config, PtFareConfigGroup.class);
        //only add distance based fare parameters that are free
        DistanceBasedPtFareParams freeDistanceBasedPt = new DistanceBasedPtFareParams();
        freeDistanceBasedPt.setTransactionPartner("freePt");
        freeDistanceBasedPt.setDescription("freePt");
        //freeDistanceBasedPt.setFareZoneShp("./nrwArea/dvg2bld_nw.shp");
        DistanceBasedPtFareParams.DistanceClassLinearFareFunctionParams eezyFareFunction = freeDistanceBasedPt.getOrCreateDistanceClassFareParams(Double.POSITIVE_INFINITY);
        eezyFareFunction.setFareIntercept(0.0);
        eezyFareFunction.setFareSlope(0.0);
        freeDistanceBasedPt.setOrder(1);
        ptFareConfigGroup.addParameterSet(freeDistanceBasedPt);
        //use upper bounds
        ptFareConfigGroup.setApplyUpperBound(true);
        ptFareConfigGroup.setUpperBoundFactor(0.0);
    }


}


