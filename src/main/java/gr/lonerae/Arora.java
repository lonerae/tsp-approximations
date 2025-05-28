package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PathFindingPhase;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.phases.arora.QuadtreePhase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Arora {

    private static final int TIME_LIMIT = 5;

    private Arora() {}

    public static Map<Integer, Solution> calculateWithEndpoints(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose, Point start, Point end, int k) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        return pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, true, preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(start)).findFirst().get(), preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(end)).findFirst().get(), preProcessingPhaseInstance.getTowns().size());
    }

    public static Map<Integer, Solution> calculate(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        // todo: fix endpoints
        return pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, true, preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(new Point(0,0))).findFirst().get(), preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(new Point(16,20))).findFirst().get(), preProcessingPhaseInstance.getTowns().size());
    }
}
