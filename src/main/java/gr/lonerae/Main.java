package gr.lonerae;

import gr.lonerae.exception.CustomException;
import gr.lonerae.graphics.Plotter;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.FileUtils;
import gr.lonerae.utils.PointUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.isNull;

public class Main {
    public static final String K_TSP_CACHE_FILE = "cache/%s.cache";
    private static boolean isTSP;
    private static boolean isKTSP;

    private static final Properties prop = new Properties();
    private static final Logger LOGGER = LogManager.getLogger();

    private static String divisionLine;

    private static Map<String, Integer> initializeTSPInstances() {
        Map<String, Integer> solvedInstances = new HashMap<>();
//        solvedInstances.put("a280", 2579);
        solvedInstances.put("berlin52", 7542);
//        solvedInstances.put("ch130", 6110);
//        solvedInstances.put("ch150", 6528);
//        solvedInstances.put("eil101", 629);
//        solvedInstances.put("kroA100", 21282);
//        solvedInstances.put("kroC100", 20749);
//        solvedInstances.put("kroD100", 21294);
        solvedInstances.put("lin105", 14379);
//        solvedInstances.put("pr76", 108159);
//        solvedInstances.put("st70", 675);
//        solvedInstances.put("test", 0);
//        solvedInstances.put("test2", 0);
//        solvedInstances.put("set64", 0);
        return solvedInstances;
    }

    private static Map<String, Integer> initializeOrienteeringInstances() {
        Map<String, Integer> solvedInstances = new HashMap<>();
        solvedInstances.put("set64", 80);
//        solvedInstances.put("set66", 130);
//        solvedInstances.put("tsiligirides1", 85);
//        solvedInstances.put("tsiligirides2", 45);
//        solvedInstances.put("tsiligirides3", 110);
//        solvedInstances.put("test", 0);
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

    private static boolean validate(String[] args) {
        if (args.length == 0) return false;

        try {
            prop.load(Main.class.getClassLoader().getResourceAsStream("messages.properties"));
        }
        catch (IOException ex) {
            LOGGER.log(Level.ERROR, ex.getMessage());
        }

        if (args[0].equals(prop.getProperty("tsp"))) {
            isTSP = true;
        } else if (args[0].equals(prop.getProperty("ktsp"))) {
            isKTSP = true;
        } else return args[0].equals(prop.getProperty("orienteering"));
        return true;
    }

    public static void main(String[] args) {
        if (!validate(args)) return;

        divisionLine = prop.getProperty("division_line");

        Map<String, Integer> solvedInstances;
        if (isTSP || isKTSP) solvedInstances = initializeTSPInstances();
        else solvedInstances = initializeOrienteeringInstances();

        List<Double> eValues = initializeEValues();
        List<Integer> portalsPerNodeSide = initializePortalsPerNodeSide();
        double avgPerfomance = 0.0;

        for (Map.Entry<String, Integer> entry : solvedInstances.entrySet()) {
            avgPerfomance = solveInstance(entry, eValues, portalsPerNodeSide, solvedInstances, avgPerfomance);
        }
        if (solvedInstances.size() > 1) {
            LOGGER.log(Level.INFO, divisionLine);
            String averagePerformance = prop.getProperty("average_performance").formatted(CommonUtils.round(avgPerfomance / solvedInstances.size()));
            LOGGER.log(Level.INFO, averagePerformance);
        }
    }

    private static double solveInstance(Map.Entry<String, Integer> entry, List<Double> eValues, List<Integer> portalsPerNodeSide, Map<String, Integer> solvedInstances, double avgPerfomance) {
        LOGGER.log(Level.INFO, divisionLine);
        String instance = prop.getProperty("instance").formatted(entry.getKey());
        LOGGER.log(Level.INFO, instance);

        List<Point> points;
        if (isTSP || isKTSP) points = FileUtils.readTSPInstance(entry.getKey());
        else points = FileUtils.readOrienteeringInstance(entry.getKey());

        List<Point> original = new ArrayList<>();
        for (Point point : points) original.add(new Point(point.getX(), point.getY()));

        if (isTSP) {
            return solveTSP(entry, eValues, portalsPerNodeSide, solvedInstances, avgPerfomance, points, original);
        } else if (isKTSP) {
            return solveKTsp(entry, eValues, portalsPerNodeSide, avgPerfomance, points);
        } else {
            return solveOrienteering(points);
        }
    }

    private static int solveOrienteering(List<Point> points) {
        LOGGER.log(Level.INFO, divisionLine);
        Solution finalSolution = Korula.calculate(points);
        if (!isNull(finalSolution)) {
            LOGGER.log(Level.INFO, "LENGTH OF GOTTLIEB SOLUTION: {}", finalSolution.getLength());
            plotKTSPGraphs(finalSolution, 1.0, 1);
        } else {
            String message = prop.getProperty("no_solution_found_error");
            LOGGER.log(Level.INFO, message);
        }
        return -1;
    }

    private static double solveKTsp(Map.Entry<String, Integer> entry, List<Double> eValues, List<Integer> portalsPerNodeSide, double avgPerfomance, List<Point> points) {
        double[] dimensions = PointUtils.findDimensions(points);
        try(BufferedReader br = new BufferedReader(new FileReader(K_TSP_CACHE_FILE.formatted(entry.getKey())))) {
            return readKTspCacheFile(br, points, dimensions);
        } catch (IOException ex) {
            Map<Integer, Solution> res = null;
            for (double e : eValues) {
                res = solveKTSPInstanceForChosenE(portalsPerNodeSide, e, points, res);
            }
            if (!isNull(res) && !res.isEmpty()) {
                FileUtils.saveKTSPSolution(entry.getKey(), res);
            }
            return avgPerfomance;
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARN, "Interrupted!", e);
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
        }
        return -1;
    }

    private static double solveTSP(Map.Entry<String, Integer> entry, List<Double> eValues, List<Integer> portalsPerNodeSide, Map<String, Integer> solvedInstances, double avgPerfomance, List<Point> points, List<Point> original) {
        Solution res = null;
        for (double e : eValues) {
            res = solveTSPInstanceForChosenE(portalsPerNodeSide, e, points, res);
        }
        if (!isNull(res)) {
            double optimal = solvedInstances.get(entry.getKey());
            LOGGER.log(Level.INFO, "OPTIMAL: {}", optimal);
            double performance = -1 * CommonUtils.round((res.getLength() - optimal) * 100 / optimal);
            avgPerfomance += performance;
            LOGGER.log(Level.INFO, "PERFORMANCE: {}%", performance);
            LOGGER.log(Level.INFO, "BEST SOLUTION: {}%", res.getLength());
            plotTSPGraphs(entry.getKey(), res, res.getDimensions(), original, res.getE(), res.getPortalsPerSide());
        }
        return avgPerfomance;
    }

    private static int readKTspCacheFile(BufferedReader br, List<Point> points, double[] dimensions) throws IOException, InterruptedException {
        String line = br.readLine();
        while (line != null) {
            String[] segments = line.split(" ");
            if (
                    Integer.parseInt(segments[0]) == (points.size() / 4) ||
                    Integer.parseInt(segments[0]) == (points.size() / 2) ||
                    Integer.parseInt(segments[0]) == (3 * points.size() / 4) ||
                    Integer.parseInt(segments[0]) == points.size()
            ) {
                String[] pointsToDraw = segments[1].split("\\),");
                ArrayList<Point> solutionPoints = new ArrayList<>();
                for (String point : pointsToDraw) {
                    String[] coords = point.split("\\(")[1].split("\\)")[0].split(",");
                    solutionPoints.add(new Point(Double.parseDouble(coords[0]), Double.parseDouble(coords[1])));
                }
                Solution kTspSolution = new Solution(Double.parseDouble(segments[2]), solutionPoints, 1, 1, false, dimensions);
                plotKTSPGraphs(kTspSolution, kTspSolution.getE(), kTspSolution.getPortalsPerSide());
                TimeUnit.SECONDS.sleep(1);
            }
            line = br.readLine();
        }
        return 0;
    }

    private static Solution solveTSPInstanceForChosenE(List<Integer> portalsPerNodeSide, double e, List<Point> points, Solution res) {
        double[] dimensions;
        LOGGER.log(Level.INFO, "E: {}", e);
        List<Point> pointsCopy = new ArrayList<>();
        for (Point point : points) {
            pointsCopy.add(new Point(point.getX(), point.getY()));
        }
        PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(pointsCopy, e);
        dimensions = preProcessingPhaseInstance.preprocess();

        for (int portals : portalsPerNodeSide) {
            res = solveInstanceForChosenEAndPortalsPerSide(e, points, res, portals, preProcessingPhaseInstance);
            res.setDimensions(dimensions);
            if (res.isBest()) break;
        }
        return res;
    }

    private static Map<Integer, Solution> solveKTSPInstanceForChosenE(List<Integer> portalsPerNodeSide, double e, List<Point> points, Map<Integer, Solution> res) {
        double[] dimensions;
        LOGGER.log(Level.INFO, "E: {}", e);
        List<Point> pointsCopy = new ArrayList<>();
        for (Point point : points) {
            pointsCopy.add(new Point(point.getX(), point.getY()));
        }
        PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(pointsCopy, e);
        dimensions = preProcessingPhaseInstance.preprocess();

        for (int portals : portalsPerNodeSide) {
            Map<Integer, Solution> currentRes = solveInstanceForChosenEAndPortalsPerSide(points, portals, preProcessingPhaseInstance);
            if (currentRes.isEmpty()) break;
            res = updateCurrentKTspSolutions(e, res, portals, currentRes, dimensions);
        }
        return res;
    }

    private static Map<Integer, Solution> updateCurrentKTspSolutions(double e, Map<Integer, Solution> res, int portals, Map<Integer, Solution> currentRes, double[] dimensions) {
        if (isNull(res)) {
            for (Solution solution : currentRes.values()) {
                solution.setDimensions(dimensions);
                solution.setE(e);
                solution.setPortalsPerSide(portals);
            }
            res = currentRes;
        }
        else {
            for (Map.Entry<Integer, Solution> entry : currentRes.entrySet()) {
                Solution solution = entry.getValue();
                solution.setDimensions(dimensions);
                solution.setE(e);
                solution.setPortalsPerSide(portals);
                if (
                        isNull(res.get(entry.getKey())) ||
                        res.get(entry.getKey()).getLength() > solution.getLength()
                ) res.put(entry.getKey(), entry.getValue());
            }
        }
        return res;
    }

    private static Solution solveInstanceForChosenEAndPortalsPerSide(double e, List<Point> points, Solution res, int portals, PreProcessingPhase preProcessingPhaseInstance) {
        LOGGER.log(Level.INFO, "PORTALS: {}", portals);
        Solution currentRes = Arora.calculateOriginal(preProcessingPhaseInstance, portals, points, preProcessingPhaseInstance.getClusters(), true);
        if (isNull(currentRes)) {
            res.setBest(true);
            return res;
        } else {
            currentRes.setPortalsPerSide(portals);
            currentRes.setE(e);
        }
        if (isNull(res) || res.getLength() > currentRes.getLength()) res = currentRes;
        return res;
    }

    private static Map<Integer, Solution> solveInstanceForChosenEAndPortalsPerSide(List<Point> points, int portals, PreProcessingPhase preProcessingPhaseInstance) {
        LOGGER.log(Level.INFO, "PORTALS: {}", portals);
        Point start = CommonUtils.minimumPoint(points);
        Point end = CommonUtils.maximumPoint(points);
        if (isNull(start) || isNull(end)) {
            throw new CustomException("Given endpoint wasn't found in instance's points.");
        }
        return Arora.calculateWithEndpoints(preProcessingPhaseInstance, portals, points, preProcessingPhaseInstance.getClusters(), false, new Point(start), new Point(end), preProcessingPhaseInstance.getTowns().size(), false);
    }

    private static void plotKTSPGraphs(Solution res, Double e, Integer portalsPerNodeSide) {
        Plotter.PlotSolution plotSolution = new Plotter.PlotSolution(res.getSequence(), res.getDimensions(), false);
        plotSolution.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        plotSolution.setVisible(true);
        plotSolution.setTitle("k: " + res.getSequence().size() + ", e: " + e + ", portals: " + portalsPerNodeSide);
    }

    private static void plotTSPGraphs(String key, Solution res, double[] dimensions, List<Point> original, Double e, Integer portalsPerNodeSide) {
        Plotter.PlotSolution plotSolution = new Plotter.PlotSolution(res.getSequence(), dimensions, true);
        plotSolution.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        plotSolution.setVisible(true);
        plotSolution.setTitle("instance: " + key + ", e: " + e + ", portals: " + portalsPerNodeSide);

        List<Integer> optimalIndexes = FileUtils.readTSPOptTour(key);
        if (!optimalIndexes.isEmpty()) {
            ArrayList<Point> optimalSequence = new ArrayList<>();
            for (Integer index : optimalIndexes) {
                optimalSequence.add(original.get(index));
            }
            Plotter.PlotSolution plotOptimal = new Plotter.PlotSolution(optimalSequence, dimensions, true);
            plotOptimal.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            plotOptimal.setVisible(true);
            plotOptimal.setTitle(key + " optimal");
        }
    }
}