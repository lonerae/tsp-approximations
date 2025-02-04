package gr.lonerae;

import gr.lonerae.graphics.Plotter;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;
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

    private static final int MAXIMUM_PORTALS_PER_NODE_SIDE = 4;

    private static Map<String, Integer> initializeInstances() {
        Map<String, Integer> solvedInstances = new HashMap<>();
//        solvedInstances.put("a280", 2579);
        solvedInstances.put("berlin52", 7542);
//        solvedInstances.put("ch130", 6110);
//        solvedInstances.put("ch150", 6528);
//        solvedInstances.put("eil101", 629);
//        solvedInstances.put("kroA100", 21282);
//        solvedInstances.put("kroC100", 20749);
//        solvedInstances.put("kroD100", 21294);
//        solvedInstances.put("lin105", 14379);
//        solvedInstances.put("pr76", 108159);
        return solvedInstances;
    }

    public static void main(String[] args) {
        Map<String, Integer> solvedInstances = initializeInstances();
        double avgPerfomance = 0.0;

        for (String key : solvedInstances.keySet()) {
            LOGGER.log(Level.INFO, "********************");
            LOGGER.log(Level.INFO, "INSTANCE: {}", key);

            List<Point> points = FileUtils.readInstance(key);
            List<Point> original = new ArrayList<>();
            for (Point point : points) {
                original.add(new Point(point.getX(), point.getY()));
            }
            ArrayList<Integer> opt = FileUtils.readOptTour(key);

            PreProcessingPhase preProcessingPhaseInstance;
            preProcessingPhaseInstance = new PreProcessingPhase(points);
            double[] dimensions = preProcessingPhaseInstance.preprocess();

            Solution res = null;
            for (int portalsPerNodeSide = 1; portalsPerNodeSide <= MAXIMUM_PORTALS_PER_NODE_SIDE; portalsPerNodeSide++) {
                LOGGER.log(Level.INFO, "PORTALS: {}", portalsPerNodeSide);

                Solution currentRes = getInstanceSolution(preProcessingPhaseInstance, portalsPerNodeSide, points);
                if (isNull(res) || res.getLength() > currentRes.getLength()) res = currentRes;
            }
            if (!isNull(res)) {
                double optimal = solvedInstances.get(key);
                LOGGER.log(Level.INFO, "OPTIMAL: {}", optimal);
                double performance = -1 * CommonUtils.round((res.getLength() - optimal) * 100 / optimal);
                avgPerfomance += performance;
                LOGGER.log(Level.INFO, "PERFORMANCE: {}%", performance);
                if (solvedInstances.size() == 1) {
                    plotGraphs(key, res, dimensions, original, opt);
                }
            }
        }
        if (solvedInstances.size() > 1) {
            LOGGER.log(Level.INFO, "********************");
            LOGGER.log(Level.INFO, "AVG PERFORMANCE: {}%", CommonUtils.round(avgPerfomance / solvedInstances.size()));
        }
    }

    private static Solution getInstanceSolution(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points) {
        QuadtreePhase quadtreePhaseInstance;
        PathFindingPhase pathFindingPhase;
        quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide(), points.size());
        return pathFindingPhase.iterateNodes(quadtreePhaseInstance);
    }

    private static void plotGraphs(String key, Solution res, double[] dimensions, List<Point> original, ArrayList<Integer> opt) {
        Plotter.PlotSolution plotSolution = new Plotter.PlotSolution(res.getSequence(), dimensions);
        plotSolution.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        plotSolution.setVisible(true);
        plotSolution.setTitle(key);

        Plotter.PlotOptimal plotOptimal = new Plotter.PlotOptimal(original, opt, dimensions);
        plotOptimal.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        plotOptimal.setVisible(true);
        plotOptimal.setTitle(key + " optimal");
    }
}