package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;
import gr.lonerae.utils.FileUtils;

import java.util.*;

public class Main {
//    BASE TEST (kept for sentimental reasons)
//    private static final List<Point> points = new ArrayList<>(List.of(
//            new Point(1,3),
//            new Point(2,5),
//            new Point(-1,7),
//            new Point(4,4),
//            new Point(-3,6),
//            new Point(8,-2),
//            new Point(5,-5),
//            new Point(1,7)
//    ));

    private static final int MAXIMUM_PORTALS_PER_NODE_SIDE = 5;

    public static void main(String[] args) {
        List<Point> points = FileUtils.readInstance("/instances/bier127.tsp");

        PreProcessingPhase preProcessingPhaseInstance;
        preProcessingPhaseInstance = new PreProcessingPhase(points);
        preProcessingPhaseInstance.preprocess();

        for (int portalsPerNodeSide = 1; portalsPerNodeSide <= MAXIMUM_PORTALS_PER_NODE_SIDE; portalsPerNodeSide++) {
            System.out.println("********************");
            System.out.println("PORTALS: " + portalsPerNodeSide + "\n");

            QuadtreePhase quadtreePhaseInstance;
            PathFindingPhase pathFindingPhase;

            quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), portalsPerNodeSide);
            quadtreePhaseInstance.assignPortals();

            pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide(), points.size());
            pathFindingPhase.iterateNodes(quadtreePhaseInstance);
        }
    }
}