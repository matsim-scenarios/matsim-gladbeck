package org.matsim.analysis;

import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.noise.NoiseAnalysis;

public class NoiseAnalysisGladbeck {

    public static void main (String args []) {
        new NoiseAnalysis().execute(args);
    }

}
