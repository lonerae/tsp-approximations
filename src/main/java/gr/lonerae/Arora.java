package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;
import gr.lonerae.utils.SegmentUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Arora {

    private static final int TIME_LIMIT = 3;

    private Arora() {}

    public static Solution calculateWithEndpoints(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose, Point start, Point end) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide(), points.size(), TIME_LIMIT);
        Solution solution = pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, false);
        solution.getSequence().addFirst(start);
        solution.getSequence().add(end);
        solution.setLength(solution.getLength() +
                SegmentUtils.euclideanDistance(solution.getSequence().getFirst(), solution.getSequence().get(1)) +
                SegmentUtils.euclideanDistance(solution.getSequence().get(solution.getSequence().size() - 2), solution.getSequence().getLast()));
        return solution;
    }

    public static Solution calculate(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide(), points.size(), TIME_LIMIT);
        return pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, true);
    }
}
