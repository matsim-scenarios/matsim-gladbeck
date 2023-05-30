package org.matsim.prepare;

import org.matsim.application.prepare.population.FixSubtourModes;

import java.io.FileOutputStream;

public class FixSubTourModeChoice {

    public static void main(String args[]) {
        var fix = new FixSubtourModes();
        fix.execute("--input", "../../shared-svn/projects/GlaMoBi/matsim-input-files/gladbeck-v1.1-10pct.plans.xml.gz", "--output", "../../shared-svn/projects/GlaMoBi/matsim-input-files/gladbeck-v1.1-10pct.plansFixedSubTourModeChoice.xml.gz",
                "--all-plans", "--mass-conservation", "--coord-dist", "50");
    }
}
