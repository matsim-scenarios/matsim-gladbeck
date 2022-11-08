package org.matsim.analysis;

import org.matsim.application.MATSimAppCommand;

public class NoiseAnalysis implements MATSimAppCommand {

    public static void main (String args []) {
        new NoiseAnalysis().execute(args);
    }

    @Override
    public Integer call() throws Exception {
        return null;
    }
}
