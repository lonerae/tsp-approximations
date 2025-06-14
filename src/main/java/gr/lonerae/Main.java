package gr.lonerae;

import gr.lonerae.graphics.Plotter;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PreProcessingPhase;
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
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

public class Main {
    private static boolean isTSP;

    private static final Logger LOGGER = LogManager.getLogger();

    private static Map<String, Integer> initializeTSPInstances() {
        Map<String, Integer> solvedInstances = new HashMap<>();
//        solvedInstances.put("a280", 2579);
//        solvedInstances.put("berlin52", 7542);
//        solvedInstances.put("ch130", 6110);
//        solvedInstances.put("ch150", 6528);
//        solvedInstances.put("eil101", 629);
//        solvedInstances.put("kroA100", 21282);
//        solvedInstances.put("kroA150", 26524);
//        solvedInstances.put("kroA200", 29368);
//        solvedInstances.put("kroC100", 20749);
//        solvedInstances.put("kroD100", 21294);
//        solvedInstances.put("kroE100", 22068);
//        solvedInstances.put("lin105", 14379);
//        solvedInstances.put("pr144", 58537);
//        solvedInstances.put("pr76", 108159);
//        solvedInstances.put("rat195", 2323);
//        solvedInstances.put("st70", 675);
        solvedInstances.put("test", 0);
//        solvedInstances.put("test2", 0);
        return solvedInstances;
    }

    private static Map<String, Integer> initializeOrienteeringInstances() {
        Map<String, Integer> solvedInstances = new HashMap<>();
//        solvedInstances.put("set64", 80);
//        solvedInstances.put("set66", 130);
//        solvedInstances.put("tsiligirides1", 85);
//        solvedInstances.put("tsiligirides2", 45);
//        solvedInstances.put("tsiligirides3", 110);
        solvedInstances.put("test", 0);
//        solvedInstances.put("test2", 0);
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
//        portalsPerNodeSide.add(2);
//        portalsPerNodeSide.add(3);
//        portalsPerNodeSide.add(4);
//        portalsPerNodeSide.add(5);
//        portalsPerNodeSide.add(6);
//        portalsPerNodeSide.add(7);
//        portalsPerNodeSide.add(8);
//        portalsPerNodeSide.add(9);
//        portalsPerNodeSide.add(10);
        return portalsPerNodeSide;
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0) return;

        Map<String, Integer> solvedInstances;
        if ("arora".equals(args[0])) isTSP = true;
        else if ("gottlieb".equals(args[0])) isTSP = false;
        else return;

        if (isTSP) solvedInstances = initializeTSPInstances();
        else solvedInstances = initializeOrienteeringInstances();

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

    private static double solveInstance(Map.Entry<String, Integer> entry, List<Double> eValues, List<Integer> portalsPerNodeSide, Map<String, Integer> solvedInstances, double avgPerfomance) throws InterruptedException {
        LOGGER.log(Level.INFO, "********************");
        LOGGER.log(Level.INFO, "INSTANCE: {}", entry.getKey());

        List<Point> points;
        if (isTSP) points = FileUtils.readTSPInstance(entry.getKey());
        else points = FileUtils.readOrienteeringInstance(entry.getKey());

        List<Point> original = new ArrayList<>();
        for (Point point : points) original.add(new Point(point.getX(), point.getY()));

        if (isTSP) {
            Map<Integer, Solution> res = null;
            for (double e : eValues) {
                res = solveInstanceForChosenE(portalsPerNodeSide, e, points, res);
            }
            if (!isNull(res) && !res.isEmpty()) {
                for (Solution kTspSolution : res.values()) {
                    plotGraphs(entry.getKey(), kTspSolution, kTspSolution.getE(), kTspSolution.getPortalsPerSide());
                    TimeUnit.SECONDS.sleep(1);
                }
            }
            return avgPerfomance;
        } else {
            Solution finalSolution = Gottlieb.calculate(points.get(2), points.get(0), points);
            LOGGER.log(Level.INFO, "***************");
            LOGGER.log(Level.INFO, "LENGTH OF GOTTLIEB SOLUTION: {}", finalSolution.getLength());
            if (!isNull(finalSolution)) {
                plotGraphs(String.valueOf(finalSolution.getSequence().size()), finalSolution, 1.0, 1);
            }
            return -1;
        }
    }

    private static Map<Integer, Solution> solveInstanceForChosenE(List<Integer> portalsPerNodeSide, double e, List<Point> points, Map<Integer, Solution> res) {
        double[] dimensions;
        LOGGER.log(Level.INFO, "E: {}", e);
        List<Point> pointsCopy = new ArrayList<>();
        for (Point point : points) {
            pointsCopy.add(new Point(point.getX(), point.getY()));
        }
        PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(pointsCopy, e);
        dimensions = preProcessingPhaseInstance.preprocess();

        for (int portals : portalsPerNodeSide) {
            Map<Integer, Solution> currentRes = solveInstanceForChosenEAndPortalsPerSide(e, points, portals, preProcessingPhaseInstance);
            if (currentRes.isEmpty()) break;
            if (isNull(res)) {
                for (Solution solution : currentRes.values()) {
                    solution.setDimensions(dimensions);
                    solution.setE(e);
                    solution.setPortalsPerSide(portals);
                }
                res = currentRes;
            }
            else {
                for (Integer key : currentRes.keySet()) {
                    Solution solution = currentRes.get(key);
                    solution.setDimensions(dimensions);
                    solution.setE(e);
                    solution.setPortalsPerSide(portals);
                    if (isNull(res.get(key)) || res.get(key).getLength() > solution.getLength()) res.put(key, solution);
                }
            }
        }
        return res;
    }

    private static Map<Integer, Solution> solveInstanceForChosenEAndPortalsPerSide(double e, List<Point> points, int portals, PreProcessingPhase preProcessingPhaseInstance) {
        LOGGER.log(Level.INFO, "PORTALS: {}", portals);
        return Arora.calculateWithEndpoints(preProcessingPhaseInstance, portals, points, preProcessingPhaseInstance.getClusters(), true, new Point(0,0), new Point(16,20), preProcessingPhaseInstance.getTowns().size());
    }

    private static void plotGraphs(String key, Solution res, Double e, Integer portalsPerNodeSide) {
        Plotter.PlotSolution plotSolution = new Plotter.PlotSolution(res.getSequence(), res.getDimensions());
        plotSolution.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        plotSolution.setVisible(true);
        plotSolution.setTitle("k: " + res.getSequence().size() + ", e: " + e + ", portals: " + portalsPerNodeSide);
    }
}