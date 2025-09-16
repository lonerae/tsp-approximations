package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.phases.arora.helper.Untangler;
import gr.lonerae.phases.gottlieb.RotationPhase;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.ICallback;
import gr.lonerae.utils.PointUtils;
import gr.lonerae.utils.SegmentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

import static java.util.Objects.isNull;

public class Gottlieb {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<Point, Point> corresponder = new HashMap<>();

    @Data
    @AllArgsConstructor
    private static class AroraKey {
        private Point windowStart;
        private Point windowEnd;
        private List<Point> subList;
        private int k;
    }
    private static Map<AroraKey, Solution> fastAroraSolutions = new HashMap<>();

    @Data
    @AllArgsConstructor
    private static class GottliebKey {
        private Point start;
        private Point end;
        private int k;

        @Override
        public String toString() {
            return "GottliebKey{" +
                    "start=" + start +
                    ", end=" + end +
                    ", k=" + k +
                    '}';
        }
    }
    private static Map<GottliebKey, Solution> orienteeringSolutions = new HashMap<>();

    private Gottlieb() {
    }

    public static Solution calculate(Point start, Point end, List<Point> points, double timeLimit) {
        RotationPhase rotationPhase = new RotationPhase(start, end, points);
        rotationPhase.rotateOnEndpointAxis();
        // Sort by ascending x
        points.sort(Comparator.comparingDouble(Point::getX));
        for (Point point : points) {
            corresponder.put(new Point(point), new Point(point.getOriginal()));
        }
        // Base of v
        GottliebKey base = new GottliebKey(start, start, 1);
        orienteeringSolutions.put(base, new Solution(0, new ArrayList<>(List.of(start)), 0, 0, true, null));
        iteratePointsBeforeStart(start, points);
        multiThreadedTSP(points, points.indexOf(start));
        iteratePointsAfterStart(start, points);
        return manipulateFinalSolution(end, points, timeLimit);
    }

    static class CallbackTask implements Callable<Void> {
        private final Runnable task;
        private final ICallback callback;

        public CallbackTask(Runnable task, ICallback callback) {
            this.task = task;
            this.callback = callback;
        }

        public Void call() {
            task.run();
            callback.complete();
            return null;
        }
    }

    private static int prog = 0;
    private static void multiThreadedTSP(List<Point> points, int index) {
        for (int i = index + 1; i < points.size(); i++) {
            List<Callable<Void>> taskList = new ArrayList<>();
            int count = (points.size() - index - 1);
            for (int j = index + 20; j < points.size(); j++) {
                int finalI = i;
                int finalJ = j;
                CallbackTask task = new CallbackTask(
                        () -> {
                            Map<Integer, Solution> s = aroraSubRoutine(new Point(points.get(finalI)), new Point(points.get(finalJ)), CommonUtils.deepCopy(points.subList(Math.min(finalI, finalJ), Math.max(finalI, finalJ) + 1)), points.size());
                            for (Map.Entry<Integer, Solution> entry : s.entrySet()) {
                                AroraKey key = new AroraKey(new Point(points.get(finalI)), new Point(points.get(finalJ)), new ArrayList<>(), entry.getKey());
                                fastAroraSolutions.put(key, entry.getValue());
                            }

                        },
                        () -> {
                            prog += 1;
                            String rounded = String.format("%.2f", (double) prog / count * 100);
                            System.out.print(finalI + ": " + rounded + "%\r");
                        }
                );
                taskList.add(task);
            }
            ExecutorService executor = Executors.newFixedThreadPool(count);
            try {
                //start the threads and wait for them to finish
                executor.invokeAll(taskList);
            } catch (InterruptedException ie) {
                System.out.println("OOPS");
            } finally {
                prog = 0;
                System.out.println("");
                executor.close();
            }
        }
    }

    private static void iteratePointsBeforeStart(Point start, List<Point> points) {
        List<Point> initialWindow = points.subList(0, points.indexOf(start) + 1);
        int startIndex = points.indexOf(start);
        for (int i = 0; i < startIndex; i++) {
            List<Point> initialWindowCopy = new ArrayList<>();
            for (Point point : initialWindow) initialWindowCopy.add(new Point(point));
            Map<Integer, Solution> solutions = aroraSubRoutine(new Point(start), new Point(points.get(i)), initialWindowCopy, initialWindow.size());
            for (Map.Entry<Integer, Solution> entry : solutions.entrySet()) {
                GottliebKey gottliebKey = new GottliebKey(start, points.get(i), entry.getKey());
                orienteeringSolutions.put(gottliebKey, entry.getValue());
            }
        }
    }

    private static void iteratePointsAfterStart(Point start, List<Point> points) {
        GottliebKey base;
        int index = points.indexOf(start);
        for (int i = index + 1; i < points.size(); i++) {
            LOGGER.log(Level.INFO, "WINDOW OF P({})", i);
            // Dummy entry
            base = new GottliebKey(points.get(i), start, 1);
            orienteeringSolutions.put(base, new Solution(0, new ArrayList<>(List.of(start)), 0, 0, true, null));
            calculateForAllDestinationsInWindow(start, points, index, i);
        }
    }

    private static void calculateForAllDestinationsInWindow(Point start, List<Point> points, int index, int i) {
        for (int d1 = index + 1; d1 <= i; d1++) {
            LOGGER.log(Level.INFO, "    DESTINATION P({})", d1);
            // 2-town-paths include only the start and the current town
            GottliebKey goal = new GottliebKey(points.get(i), points.get(d1), 2);
            orienteeringSolutions.put(goal, new Solution(SegmentUtils.euclideanDistance(start, points.get(d1)), new ArrayList<>(List.of(start, points.get(d1))), 0, 0, true, null));
            calculateForAllNumberOfTowns(points, index, i, d1);
        }
    }

    private static void calculateForAllNumberOfTowns(List<Point> points, int index, int i, int d1) {
        GottliebKey goal;
        for (int k1 = 3; k1 <= i + 1; k1++) {
            LOGGER.log(Level.INFO, "        NUMBER OF TOWNS: {}", k1);
            goal = new GottliebKey(points.get(i), points.get(d1), k1);
            Solution temp = orienteeringSolutions.get(new GottliebKey(points.get(i-1), points.get(d1), k1));
            if (!isNull(temp)) {
                orienteeringSolutions.put(goal, new Solution(temp.getLength(), new ArrayList<>(temp.getSequence()), 0, 0, true, null));
                continue;
            }
            double best = Double.MAX_VALUE;
            int bestScore = Integer.MIN_VALUE;
            ArrayList<Point> bestSequence = new ArrayList<>();
            for (int j = index; j <= i; j++) {
                if (d1 < j+1) continue;
                for (int d2 = 0; d2 <= j; d2++) {
                    for (int c = j+1; c <= i; c++) {
                        List<Point> subList = CommonUtils.deepCopy(points.subList(j+1, i+1));
                        Map<Integer, Solution> aroraSolutions = new HashMap<>();
                        if (fastAroraSolutions.containsKey(new AroraKey(new Point(points.get(c)), new Point(points.get(d1)), new ArrayList<>(), k1))) {
                            for (int tt = 2; tt <= k1; tt++) {
                                AroraKey key = new AroraKey(new Point(points.get(c)), new Point(points.get(d1)), new ArrayList<>(), tt);
                                aroraSolutions.put(tt, fastAroraSolutions.get(new AroraKey(new Point(points.get(c)), new Point(points.get(d1)), new ArrayList<>(), tt)));
                            }
                        } else {
                            aroraSolutions = aroraSubRoutine(new Point(points.get(c)), new Point(points.get(d1)), subList, k1);
                            for (int tt = 2; tt <= k1; tt++) {
                                AroraKey key = new AroraKey(new Point(points.get(c)), new Point(points.get(d1)), new ArrayList<>(), tt);
                                fastAroraSolutions.put(key, aroraSolutions.get(tt));
                            }
                        }
                        for (int k2 = 1; k2 <= Math.min(k1 - 1, j + 1); k2++) {
                            if (i - j < k1 - k2 || Math.abs(d1 - c) + 1 < k1 - k2 || isNull(aroraSolutions.get(k1 - k2))) continue;
                            GottliebKey tempKey = new GottliebKey(points.get(j), points.get(d2), k2);
                            if (orienteeringSolutions.containsKey(tempKey)) {
                                Solution tempSolution = orienteeringSolutions.get(tempKey);
                                Solution aroraSolution = aroraSolutions.get(k1 - k2);
                                ArrayList<Point> tempSequence = new ArrayList<>(tempSolution.getSequence());
                                tempSequence.addAll(new ArrayList<>(aroraSolution.getSequence()));
                                tempSequence = new ArrayList<>(new LinkedHashSet<>(tempSequence));
                                int y = PointUtils.getTotalPlurality(tempSequence.stream().map(corresponder::get).toList());
                                double x = tempSolution.getLength() + SegmentUtils.euclideanDistance(points.get(d2), points.get(c)) + aroraSolution.getLength();
                                if ((y > bestScore || (y == bestScore && x < best)) && (tempSequence.getLast().equals(points.get(d1)))) {
                                    best = x;
                                    bestScore = y;
                                    bestSequence = new ArrayList<>(tempSequence);
                                }
                            }
                        }
                    }
                }
            }
            // At least one solution found
            if (best < Double.MAX_VALUE) {
                orienteeringSolutions.put(goal, new Solution(best, new ArrayList<>(bestSequence), 0, 0, true, null));
            }
        }
    }

    private static Map<Integer, Solution> aroraSubRoutine(Point start, Point end, List<Point> points, int townNumber) {
        List<Point> copy = new ArrayList<>();
        for (Point point : points) {
            copy.add(new Point(point));
        }
        PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(copy, 1);
        preProcessingPhaseInstance.preprocess();
        return Arora.calculateWithEndpoints(preProcessingPhaseInstance, 1, copy, preProcessingPhaseInstance.getClusters(), false, start, end, townNumber);
    }

    private static Solution manipulateFinalSolution(Point end, List<Point> points, double timeLimit) {
        List<Solution> possibleSolutions = orienteeringSolutions.entrySet().stream().filter(x -> x.getKey().getEnd().equals(end)).map(Map.Entry::getValue).toList();
        Solution finalSolution = null;
        for (Solution tempSolution : possibleSolutions) {
            if (tempSolution.getLength() > timeLimit) continue;
            if (isNull(finalSolution) || PointUtils.getTotalPlurality(tempSolution.getSequence().stream().map(corresponder::get).toList()) > PointUtils.getTotalPlurality(finalSolution.getSequence().stream().map(corresponder::get).toList())) {
                finalSolution = tempSolution;
            }
        }
        if (isNull(finalSolution)) return null;
        ArrayList<Point> actualSequence = new ArrayList<>(finalSolution.getSequence().stream().map(corresponder::get).toList());
        Untangler.untangle(actualSequence);
        double actualBest = 0;
        Point previousTown = null;
        for (Point town : actualSequence) {
            if (isNull(previousTown)) {
                previousTown = town;
                continue;
            }
            actualBest += SegmentUtils.euclideanDistance(previousTown, town);
            previousTown = town;
        }
        double xDimension = actualSequence.stream().max(Comparator.comparingDouble(Point::getX)).get().getX() - actualSequence.stream().min(Comparator.comparingDouble(Point::getX)).get().getX();
        double yDimension = actualSequence.stream().max(Comparator.comparingDouble(Point::getY)).get().getY() - actualSequence.stream().min(Comparator.comparingDouble(Point::getY)).get().getY();
        return new Solution(actualBest, actualSequence, 0, 0, true, new double[]{xDimension, yDimension});
    }
}
