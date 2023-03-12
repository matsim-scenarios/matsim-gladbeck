package org.matsim.run;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.application.MATSimApplication;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.testcases.MatsimTestUtils;
import java.util.HashSet;
import java.util.Set;


public class Test {

    private static final String inputNetworkFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/metropole-ruhr/metropole-ruhr-v1.0/input/metropole-ruhr-v1.0.network_resolutionHigh-with-pt.xml.gz";

    @Rule
    public MatsimTestUtils testUtils = new MatsimTestUtils();

    @Ignore
    @org.junit.Test
    public void testPtFlat() {
        MATSimApplication.execute(Test.TestApplication.class, "--output="  + "withPtFlat", "--ptFlat=true", "--download-input", "--1pct", "--config:network.inputNetworkFile=" + inputNetworkFile);
        PtFlatTestListener handler = new PtFlatTestListener();
        EventsManager eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(handler);
        EventsUtils.readEvents(eventsManager,"withPtFlat/metropole-ruhr-v1.0-1pct.output_events.xml.gz");
        PersonMoneyEvent event = handler.ptFlatEvents.iterator().next();

        for (PersonMoneyEvent personMoneyEvent: handler.ptFlatEvents) {
           if (personMoneyEvent.getPersonId().equals("1002535")) {
                Assert.fail("Agent "+personMoneyEvent.getPersonId()+ "not allowed to have the pt Flat");
           }

           if (personMoneyEvent.getPersonId().equals("1002172")) {
               Assert.assertEquals("Wrong amount of money for ptFlat", 2.3191,personMoneyEvent.getAmount(),0.01);
           }
            if (personMoneyEvent.getPersonId().equals("1002816")) {
                Assert.assertEquals("Wrong amount of money for ptFlat", 2.3191,personMoneyEvent.getAmount(),0.01);
            }
        }
        Assert.assertEquals("wrong amount of ptFlat payments",6,handler.ptFlatEvents.size());
    }

    public static class TestApplication extends RunGladbeckScenario {

        @Override
        public Config prepareConfig(Config config) {
            Config preparedConfig = super.prepareConfig(config);
            preparedConfig.controler().setLastIteration(0);
            preparedConfig.plans().setInputFile("/Users/gregorr/Desktop/Test/test_pop.xml");
            //adjusting strategy setting of config so agents try out different modes
            for (StrategyConfigGroup.StrategySettings setting: preparedConfig.strategy().getStrategySettings()) {
                if (setting.getStrategyName().equals("ChangeSingleTripMode")) {
                    setting.setWeight(1.0);
                }
                else setting.setWeight(0.0);
            }
            return preparedConfig;
        }

        @Override
        protected void prepareScenario(Scenario scenario) {
            super.prepareScenario(scenario);
        }

        @Override
        protected void prepareControler(Controler controler) {
            super.prepareControler(controler);
            PtFlatTestListener handler = new PtFlatTestListener();
            controler.addOverridingModule(new AbstractModule() {
                @Override
                public void install() {
                    addEventHandlerBinding().toInstance(handler);
                }
            });
        }

    }

     private static class PtFlatTestListener implements PersonMoneyEventHandler {
        Set<PersonMoneyEvent> ptFlatEvents = new HashSet<>();

        @Override
        public void handleEvent(PersonMoneyEvent event) {
            if (event.getPurpose().equals("ptFlat") && event.getAmount() > 0.) this.ptFlatEvents.add(event);
        }

        @Override
        public void reset(int iteration) {
            PersonMoneyEventHandler.super.reset(iteration);
            this.ptFlatEvents.clear();
        }
    }
}
