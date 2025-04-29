package gr.lonerae;

import com.google.common.collect.Sets;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.utils.PointUtils;
import gr.lonerae.utils.SegmentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

import static java.util.Objects.isNull;

public class Gottlieb {

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
        points.sort(Comparator.comparingDouble(Point::getX));
        // for all d, p_i satisfying d ≤ p_i = s and k ∈ [k], we ﬁx entries V(p_i, d, k) = Arora(p_1, p_i, s, d, k)
        int counter = 0;
        int pauseIndex = -1;
        List<Point> copy = new ArrayList<>();
        while (counter < points.size() && points.get(counter).getX() <= start.getX()) {
            if (!points.get(counter).equals(start)) copy.add(new Point(points.get(counter)));
            counter++;
        }
        pauseIndex = counter;
        for (int j = 0; j < copy.size(); j++) {
            for (int k = 0; k < copy.size(); k++) {
                aroraSubRoutine(start, points.get(j), copy, k);
            }
        }
        // for each p_i > s in increasing order, calculate the entries for all d satisfying s < d ≤ p_i and all k1 ∈ [k]
        // V (p_i, d, k1) = min{V(p_j, d2, k2) + |d2 − c| + Arora(p_j+1, p_i, c, d, k1 − k2) : p_j ∈ P_i, d ∈ P_j, c ∈ P_i \ P_j, k2 < k1}
        while (counter < points.size()) {
            for (int i = pauseIndex; i < points.size(); i++) {
                for (int d = pauseIndex; d < i; d++) {
                    for (int k1 = 0; k1 < i-d; k1++) {
                        GottliebKey lead = new GottliebKey(points.get(i), points.get(d), k1);
                        System.out.println(lead);
                        //v(points.get(i), points.get(d), k1)
                        double best = -1;
                        for (int j = 0; j < i; j++) {
                            for (int d2 = 0; d2 < j; d2++) {
                                for (int k2 = 0; k2 < k1; k2++) {
                                    GottliebKey key = new GottliebKey(points.get(j), points.get(d2), k2);
                                    System.out.println("     " + key);
                                    for (int c = j+1; c < i+1; c++) {
                                        System.out.println("          " + c);
                                        List<Point> sub = points.subList(j+1, i+1);
                                        if (k1-k2 > sub.size()) continue;
                                        Solution s = v.get(key);
                                        Solution partial = aroraSubRoutine(points.get(c), points.get(d), sub, k1-k2);
                                        if (!isNull(s) && !isNull(partial)) {
                                            s.setLength(v.get(key).getLength() +
                                                    SegmentUtils.euclideanDistance(points.get(d2), points.get(c)) +
                                                    partial.getLength());
                                            v.put(lead, s);
                                        }
                                    }

                                }
                            }
                        }
                    }
                }
            }
            counter++;
        }
    }

    private static Solution aroraSubRoutine(Point start, Point end, List<Point> points, int townNumber) {
        List<Point> copy = new ArrayList<>();
        for (Point point : points) {
            copy.add(new Point(point));
        }
        copy.remove(end);
        Solution finalSolution = null;
        for (Set<Point> combination : Sets.combinations(new HashSet<>(copy), townNumber)) {
            Solution solution;
            if (!combination.isEmpty()) {
                List<Point> combinationAsList = new ArrayList<>();
                for (Point point : combination) {
                    combinationAsList.add(new Point(point));
                }
                PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(combinationAsList, 0.01);
                preProcessingPhaseInstance.preprocess();
                solution = Arora.calculateWithEndpoints(preProcessingPhaseInstance, 1, combinationAsList, preProcessingPhaseInstance.getClusters(), false, start, end, points.size());
            } else solution = new Solution(SegmentUtils.euclideanDistance(start, end), new ArrayList<>(List.of(start, end)), 0, 0.0, false, new double[2]);
            GottliebKey key = new GottliebKey(start, end, townNumber);
            if (!isNull(v.get(key))) {
                if (!isNull(solution) && solution.getLength() < v.get(key).getLength()) {
                    finalSolution = solution;
                    v.replace(key, solution);
                }
            } else if (!isNull(solution)) {
                finalSolution = solution;
                v.put(new GottliebKey(start, end, townNumber), solution);
            }

            copy = new ArrayList<>();
            for (Point point : points) {
                copy.add(new Point(point));
            }
            copy.remove(end);
        }
        return finalSolution;
    }
}
