package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.utils.CommonUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

public class Korula {
    private static final Logger LOGGER = LogManager.getLogger();

    private Korula() {}

    public static Solution calculate(List<Point> points, double timeLimit) {
        double bestLength = Double.MAX_VALUE;
        int bestTowns = 0;
        Solution bestSolution = null;
        int i = 1;
        bestSolution = Gottlieb.calculate(points.getFirst(), points.get(1), points, timeLimit);
//        for (Point point : points) {
//            if (point.getPlurality() == 0) continue;
//            LOGGER.log(Level.INFO, "KORULA ROUND {}", i);
//            List<Point> pointsCopy = CommonUtils.deepCopy(points);
//            int benchmark = point.getPlurality();
//            List<Point> filteredPoints = filterPoints(points, pointsCopy, benchmark);
//            updateWeights(points, filteredPoints, benchmark);
//            Solution finalSolution = Gottlieb.calculate(filteredPoints.getFirst(), filteredPoints.get(1), filteredPoints, timeLimit);
//            if (!isNull(finalSolution)) {
//                if (finalSolution.getSequence().size() > bestTowns) {
//                    bestSolution = finalSolution;
//                    bestTowns = finalSolution.getSequence().size();
//                    bestLength = finalSolution.getLength();
//                } else if (finalSolution.getSequence().size() == bestTowns && finalSolution.getLength() < bestLength) {
//                    bestSolution = finalSolution;
//                    bestLength = finalSolution.getLength();
//                }
//            }
//            i++;
//        }
        return bestSolution;
    }

    private static List<Point> filterPoints(List<Point> points, List<Point> pointsCopy, int benchmark) {
        List<Point> filteredPoints = new ArrayList<>();
        filteredPoints.add(pointsCopy.getFirst());
        filteredPoints.add(pointsCopy.get(1));
        filteredPoints.addAll(
                pointsCopy.stream()
                        .filter(x -> (x.getPlurality() <= benchmark && x.getPlurality() >= benchmark / Math.pow(points.size(), 2)))
                        .toList());
        return filteredPoints;
    }

    private static void updateWeights(List<Point> points, List<Point> filteredPoints, int benchmark) {
        filteredPoints.forEach(x -> x.setPlurality((int) Math.floor(Math.pow(points.size(), 2) * x.getPlurality() / benchmark)));
    }
}
