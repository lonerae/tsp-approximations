package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Segment;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.phases.arora.helper.Untangler;
import gr.lonerae.phases.gottlieb.RotationPhase;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.SegmentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static java.util.Objects.isNull;

public class Gottlieb {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final HashMap<Point, Point> corresponder = new HashMap<>();

    @Data
    @AllArgsConstructor
    private static class AroraKey {
        private Point windowStart;
        private Point windowEnd;
        private Point start;
        private Point end;
        private int visitedTowns;

        @Override
        public String toString() {
            return "AroraKey{" +
                    "windowStart=" + windowStart +
                    ", windowEnd=" + windowEnd +
                    ", start=" + start +
                    ", end=" + end +
                    ", visitedTowns=" + visitedTowns +
                    '}';
        }
    }

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

    public static Solution calculate(Point start, Point end, List<Point> points) {
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
        iteratePointsAfterStart(start, points);
        return manipulateFinalSolution(end, points);
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
            double best = Double.MAX_VALUE;
            ArrayList<Point> bestSequence = new ArrayList<>();
            for (int j = index; j <= i; j++) {
                if (d1 < j+1) continue;
                for (int d2 = 0; d2 <= j; d2++) {
                    for (int c = j+1; c <= i; c++) {
                        List<Point> subList = CommonUtils.deepCopy(points.subList(j+1, i+1));
                        Map<Integer, Solution> aroraSolutions = aroraSubRoutine(new Point(points.get(c)), new Point(points.get(d1)), subList, k1);
                        for (int k2 = 1; k2 <= Math.min(k1 - 1, j + 1); k2++) {
                            if (i - j < k1 - k2 || Math.abs(d1 - c) + 1 < k1 - k2 || isNull(aroraSolutions.get(k1 - k2))) continue;
                            GottliebKey tempKey = new GottliebKey(points.get(j), points.get(d2), k2);
                            if (orienteeringSolutions.containsKey(tempKey)) {
                                Solution tempSolution = orienteeringSolutions.get(tempKey);
                                Solution aroraSolution = aroraSolutions.get(k1 - k2);
                                double x = tempSolution.getLength() + SegmentUtils.euclideanDistance(points.get(d2), points.get(c)) + aroraSolution.getLength();
                                if (x < best && (aroraSolution.getSequence().getLast().equals(points.get(d1)))) {
                                    best = x;
                                    bestSequence = tempSolution.getSequence();
                                    bestSequence.addAll(aroraSolution.getSequence());
                                    bestSequence = new ArrayList<>(new LinkedHashSet<>(bestSequence));
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
        return Arora.calculateWithEndpoints(preProcessingPhaseInstance, 1, copy, preProcessingPhaseInstance.getClusters(), false, start, end, points.size());
    }

    private static Solution manipulateFinalSolution(Point end, List<Point> points) {
        GottliebKey finalKey = new GottliebKey(end, end, points.size());
        Solution finalSolution = orienteeringSolutions.get(finalKey);
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
