package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.KPathFindingPhase;
import gr.lonerae.phases.arora.PathFindingPhase;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.phases.arora.QuadtreePhase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Arora {

    private static final int TIME_LIMIT = Integer.MAX_VALUE;

    private Arora() {}

    public static Solution calculateOriginal(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        return pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose);
    }

    public static Map<Integer, Solution> calculateWithEndpoints(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose, Point start, Point end, int k, boolean withEarlyFinish) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        KPathFindingPhase kPathFindingPhase = new KPathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        Optional<Point> startOptional = preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(start)).findFirst();
        Optional<Point> endOptional = preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(end)).findFirst();
        if (startOptional.isPresent() && endOptional.isPresent()) {
            return kPathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, startOptional.get(), endOptional.get(), k, withEarlyFinish);
        }
        return Collections.emptyMap();
    }
}
