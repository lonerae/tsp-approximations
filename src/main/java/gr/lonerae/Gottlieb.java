package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.phases.gottlieb.RotationPhase;
import gr.lonerae.utils.SegmentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

import static java.util.Objects.isNull;

public class Gottlieb {

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
    private static Map<AroraKey, Double> aroraSolutions = new HashMap<>();

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
    private static Map<GottliebKey, Solution> v = new HashMap<>();

    private Gottlieb() {
    }

    public static void calculate(Point start, Point end, List<Point> points) {
        RotationPhase rotationPhase = new RotationPhase(start, end, points);
        rotationPhase.rotateOnEndpointAxis();
        // Sort by ascending x
        points.sort(Comparator.comparingDouble(Point::getX));
        // Initial values of v
        List<Point> initialWindow = points.subList(0, points.indexOf(start) + 1);
        for (int i = 0; i < points.indexOf(start); i++) {
            List<Point> initialWindowCopy = new ArrayList<>();
            for (Point point : initialWindow) initialWindowCopy.add(new Point(point));
            Map<Integer, Solution> solutions = aroraSubRoutine(new Point(start), new Point(points.get(i)), initialWindowCopy, initialWindow.size());
            for (Map.Entry<Integer, Solution> entry : solutions.entrySet()) {
                GottliebKey gottliebKey = new GottliebKey(start, points.get(i), entry.getKey());
                v.put(gottliebKey, entry.getValue());
            }
        }
        // Gottlieb
        int index = points.indexOf(start);
        for (int i = index + 1; i < points.size(); i++) {
            for (int d1 = index + 1; d1 <= i; d1++) {
                // k1 = 2 karfwto
                GottliebKey goal = new GottliebKey(points.get(i), points.get(d1), 2);
                v.put(goal, new Solution(SegmentUtils.euclideanDistance(points.get(i), points.get(d1)), new ArrayList<>(List.of(points.get(i), points.get(d1))), 0, 0, true, null));
                for (int k1 = 3; k1 <= i; k1++) {
                    goal = new GottliebKey(points.get(i), points.get(d1), k1);
                    double best = Double.MAX_VALUE;
                    for (int j = index; j <= i; j++) {
                        if (d1 < j+1 || d1 > i) continue;
                        for (int d2 = 1; d2 <= j; d2++) {
                            for (int c = j+1; c <= i; c++) {
                                if (c < j+1 || c > i) continue;
                                ArrayList<Point> subList = new ArrayList<>();
                                for (int sub = j+1; sub <= i; sub++) {
                                    subList.add(new Point(points.get(sub)));
                                }
                                Map<Integer, Solution> aroraSolution = aroraSubRoutine(new Point(points.get(c)), new Point(points.get(d1)), subList, k1);
                                for (int k2 = 2; k2 <= Math.min(k1 - 1, j); k2++) {
                                    if (i - j < k1 - k2 || d1 - c + 1 < k1 - k2 || isNull(aroraSolution.get(k1 - k2))) continue;
                                    GottliebKey tempKey = new GottliebKey(points.get(j), points.get(d2), k2);
                                    Solution tempSolution = v.get(tempKey);
                                    if (!isNull(tempSolution)) {
                                        double x = tempSolution.getLength() + SegmentUtils.euclideanDistance(points.get(d2), points.get(c)) + aroraSolution.get(k1 - k2).getLength();
                                        if (x < best) best = x;
                                    }
                                }
                            }
                        }
                    }
                    if (best < Double.MAX_VALUE) {
                        v.put(goal, new Solution(best, null, 0, 0, true, null));
                    }
                }
            }
        }
        int x = 1;
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
}
