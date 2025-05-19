package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Arora {

    private static final int TIME_LIMIT = 1;

    private Arora() {}

    public static Solution calculateWithEndpoints(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose, Point start, Point end, int k) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        Map<Integer, Solution> solutions = pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, false, start, end, k);
//        if (!solutions.isEmpty()) {
//            solution.getSequence().addFirst(start);
//            solution.getSequence().add(end);
//            solution.setLength(solution.getLength() +
//                    SegmentUtils.euclideanDistance(solution.getSequence().getFirst(), solution.getSequence().get(1)) +
//                    SegmentUtils.euclideanDistance(solution.getSequence().get(solution.getSequence().size() - 2), solution.getSequence().getLast()));
//        }
//        return solutions;
        return null;
    }

    public static Map<Integer, Solution> calculate(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        // todo: fix endpoints
        return pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, true, preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(new Point(0,0))).findFirst().get(), preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(new Point(5,9))).findFirst().get(), preProcessingPhaseInstance.getTowns().size());
    }
}
