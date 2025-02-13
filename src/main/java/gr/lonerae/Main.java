package gr.lonerae;

import gr.lonerae.graphics.Plotter;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.FileUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public class Main {
    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, Integer> initializeInstances() {
        Map<String, Integer> solvedInstances = new HashMap<>();
        solvedInstances.put("a280", 2579);
        solvedInstances.put("berlin52", 7542);
        solvedInstances.put("ch130", 6110);
        solvedInstances.put("ch150", 6528);
        solvedInstances.put("eil101", 629);
        solvedInstances.put("kroA100", 21282);
        solvedInstances.put("kroC100", 20749);
        solvedInstances.put("kroD100", 21294);
        solvedInstances.put("lin105", 14379);
        solvedInstances.put("pr76", 108159);
        return solvedInstances;
    }

    private static List<Double> initializeEValues() {
        List<Double> eValues = new ArrayList<>();
        eValues.add(1.0);
//        eValues.add(0.5);
//        eValues.add(0.2);
//        eValues.add(0.1);
//        eValues.add(0.01);
        return eValues;
    }

    private static List<Integer> initializePortalsPerNodeSide() {
        List<Integer> portalsPerNodeSide = new ArrayList<>();
        portalsPerNodeSide.add(1);
        portalsPerNodeSide.add(2);
//        portalsPerNodeSide.add(3);
//        portalsPerNodeSide.add(4);
//        portalsPerNodeSide.add(5);
        return portalsPerNodeSide;
    }

    public static void main(String[] args) {
        Map<String, Integer> solvedInstances = initializeInstances();
        List<Double> eValues = initializeEValues();
        List<Integer> portalsPerNodeSide = initializePortalsPerNodeSide();
        double avgPerfomance = 0.0;

        for (Map.Entry<String, Integer> entry : solvedInstances.entrySet()) {
            avgPerfomance = solveInstance(entry, eValues, portalsPerNodeSide, solvedInstances, avgPerfomance);
        }
        if (solvedInstances.size() > 1) {
            LOGGER.log(Level.INFO, "********************");
            LOGGER.log(Level.INFO, "AVG PERFORMANCE: {}%", CommonUtils.round(avgPerfomance / solvedInstances.size()));
        }
    }

    private static double solveInstance(Map.Entry<String, Integer> entry, List<Double> eValues, List<Integer> portalsPerNodeSide, Map<String, Integer> solvedInstances, double avgPerfomance) {
        LOGGER.log(Level.INFO, "********************");
        LOGGER.log(Level.INFO, "INSTANCE: {}", entry.getKey());

        List<Point> points = FileUtils.readInstance(entry.getKey());
        List<Point> original = new ArrayList<>();
        for (Point point : points) {
            original.add(new Point(point.getX(), point.getY()));
        }
        ArrayList<Integer> opt = FileUtils.readOptTour(entry.getKey());

        Solution res = null;
        for (double e : eValues) {
            res = solveInstanceForChosenE(entry, portalsPerNodeSide, solvedInstances, e, points, res, original, opt);
        }
        if (!isNull(res)) {
            double optimal = solvedInstances.get(entry.getKey());
            LOGGER.log(Level.INFO, "OPTIMAL: {}", optimal);
            double performance = -1 * CommonUtils.round((res.getLength() - optimal) * 100 / optimal);
            avgPerfomance += performance;
            LOGGER.log(Level.INFO, "PERFORMANCE: {}%", performance);

        }
        return avgPerfomance;
    }

    private static Solution solveInstanceForChosenE(
            Map.Entry<String, Integer> entry, List<Integer> portalsPerNodeSide, Map<String, Integer> solvedInstances,
            double e, List<Point> points, Solution res, List<Point> original, ArrayList<Integer> opt
    ) {
        double[] dimensions;
        LOGGER.log(Level.INFO, "E: {}", e);
        List<Point> pointsCopy = new ArrayList<>();
        for (Point point : points) {
            pointsCopy.add(new Point(point.getX(), point.getY()));
        }
        PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(pointsCopy, e);
        dimensions = preProcessingPhaseInstance.preprocess();

        for (int portals : portalsPerNodeSide) {
            res = solveInstanceForChosenEAndPortalsPerSide(entry, solvedInstances, e, points, res, original, opt, portals, preProcessingPhaseInstance, dimensions);
        }
        return res;
    }

    private static Solution solveInstanceForChosenEAndPortalsPerSide(
            Map.Entry<String, Integer> entry, Map<String, Integer> solvedInstances, double e,
            List<Point> points, Solution res, List<Point> original, ArrayList<Integer> opt,
            int portals, PreProcessingPhase preProcessingPhaseInstance, double[] dimensions
    ) {
        LOGGER.log(Level.INFO, "PORTALS: {}", portals);
        Solution currentRes = Arora.getInstanceSolution(preProcessingPhaseInstance, portals, points, preProcessingPhaseInstance.getClusters(), true);
        if (isNull(res) || res.getLength() > currentRes.getLength()) res = currentRes;
        if (solvedInstances.size() == 1) {
            plotGraphs(entry.getKey(), res, dimensions, original, opt, e, portals);
        }
        return res;
    }

    private static void plotGraphs(String key, Solution res, double[] dimensions, List<Point> original, ArrayList<Integer> opt, Double e, Integer portalsPerNodeSide) {
        Plotter.PlotSolution plotSolution = new Plotter.PlotSolution(res.getSequence(), dimensions);
        plotSolution.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        plotSolution.setVisible(true);
        plotSolution.setTitle("instance: " + key + ", e: " + e + ", portals: " + portalsPerNodeSide);

//        Plotter.PlotOptimal plotOptimal = new Plotter.PlotOptimal(original, opt, dimensions);
//        plotOptimal.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//        plotOptimal.setVisible(true);
//        plotOptimal.setTitle(key + " optimal");
    }
}