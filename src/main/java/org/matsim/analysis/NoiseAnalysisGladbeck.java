package org.matsim.analysis;

import org.matsim.application.MATSimAppCommand;
import org.matsim.application.analysis.noise.NoiseAnalysis;

public class NoiseAnalysisGladbeck {

    public static void main (String args []) {
        if ( args==null || args.length==0 ) {
            args = new String[]{
                            "--directory", "../../runs-svn/glaMobi/freePt/",
                            "--runId", "freePt_gladbeck-v1.0",
                            "--input-crs", "EPSG:25832"
            };
        }
        new NoiseAnalysis().execute(args);
    }

}
