package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.arora.PathFindingPhase;
import gr.lonerae.phases.arora.PreProcessingPhase;
import gr.lonerae.phases.arora.QuadtreePhase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Arora {

    private static final int TIME_LIMIT = 5;

    private Arora() {}

    public static Map<Integer, Solution> calculateWithEndpoints(PreProcessingPhase preProcessingPhaseInstance, int portalsPerNodeSide, List<Point> points, Map<Point, ArrayList<Point>> clusters, boolean verbose, Point start, Point end, int k) {
        QuadtreePhase quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getBoundingBoxSize(), preProcessingPhaseInstance.getTowns(), portalsPerNodeSide);
        quadtreePhaseInstance.assignPortals();
        PathFindingPhase pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getBoundingBoxSize(), points.size(), TIME_LIMIT);
        Optional<Point> startOptional = preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(start)).findFirst();
        Optional<Point> endOptional = preProcessingPhaseInstance.getTowns().stream().filter(x -> x.getOriginal().equals(end)).findFirst();
        if (startOptional.isPresent() && endOptional.isPresent()) {
            return pathFindingPhase.iterateNodes(quadtreePhaseInstance, clusters, verbose, true, startOptional.get(), endOptional.get(), preProcessingPhaseInstance.getTowns().size());
        }
        return Collections.emptyMap();
    }
}
