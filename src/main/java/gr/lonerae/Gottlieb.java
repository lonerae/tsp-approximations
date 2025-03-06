package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PreProcessingPhase;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;

public class Gottlieb {

    @Data
    @AllArgsConstructor
    private static class GottliebKey {
        private Point start;
        private Point end;
        private int k;
    }

    private static Map<GottliebKey, Solution> v = new HashMap<>();

    private Gottlieb() {
    }

    public static void calculate(Point start, Point end, List<Point> points) {
        points.sort(Comparator.comparingDouble(Point::getX));
        // FOR ALL POINTS BEFORE START
        int startIndex = -1;
        int i = 0;
        while (i < points.size() && points.get(i).getX() <= start.getX()) {
            if (points.get(i).equals(start)) startIndex = i;
            List<Point> copy = new ArrayList<>();
            for (int j = 0; j <= i; j++) {
                copy.add(new Point(points.get(i).getX(), points.get(i).getY()));
            }
            aroraSubRoutine(start, points.get(i), copy, i+1);
            i++;
        }
        // FOR ALL POINTS AFTER START
        while (i < points.size()) {
            for (int j = startIndex; j < i; j++) {
                //
            }
            i++;
        }
    }

    private static void aroraSubRoutine(Point start, Point end, List<Point> points, int townNumber) {
        PreProcessingPhase preProcessingPhaseInstance = new PreProcessingPhase(points, 1);
        preProcessingPhaseInstance.preprocess();
        Solution solution = Arora.calculateWithEndpoints(preProcessingPhaseInstance, 1, points, preProcessingPhaseInstance.getClusters(), false, start, end);
        v.put(new GottliebKey(start, end, townNumber), solution);
    }
}
