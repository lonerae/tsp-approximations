package gr.lonerae.graphics;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProgressBar {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String ZERO = "[----]";
    private static final String QUARTER = "[■---]";
    private static final String HALF = "[■■--]";
    private static final String HALF_QUARTER = "[■■■-]";
    private static final String FULL = "[■■■■]";
    private static final String PROGRESS_BAR = "{} ({}%)";

    private ProgressBar() {
    }

    public static void showProgress(int currentLoop, int loops) {
        int progressPercent = -1;
        String currentProgressBar = null;
        if (currentLoop == 1) {
            progressPercent = 0;
            currentProgressBar = ZERO;
        } else if (currentLoop == loops / 4) {
            progressPercent = 25;
            currentProgressBar = QUARTER;
        } else if (currentLoop == loops / 2) {
            progressPercent = 50;
            currentProgressBar = HALF;
        } else if (currentLoop == 3 * loops / 4) {
            progressPercent = 75;
            currentProgressBar = HALF_QUARTER;
        } else if (currentLoop == loops - 1) {
            progressPercent = 100;
            currentProgressBar = FULL;
        }
        if (progressPercent < 0) return;
        LOGGER.log(Level.INFO, PROGRESS_BAR, currentProgressBar, progressPercent);
    }
}
