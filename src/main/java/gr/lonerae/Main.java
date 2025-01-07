package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;

import java.util.*;

public class Main {
//    INITIAL
    private static final ArrayList<Point> points = new ArrayList<>(List.of(
            new Point(1,3),
            new Point(2,5),
            new Point(-1,7),
            new Point(4,4),
            new Point(-3,6),
            new Point(8,-2),
            new Point(5,-5),
            new Point(1,7)
    ));

    private static final int PORTALS_PER_NODE_SIDE = 2;
    private static final int TOWN_NUMBER = 100;
    private static final double RANGE_MIN = -100.0;
    private static final double RANGE_MAX = 100.0;
    private static final Random rand = new Random();

    public static void main(String[] args) {
//        ArrayList<Point> points = new ArrayList<>();
//        for (int i = 0; i < TOWN_NUMBER; i++) {
//            points.add(new Point(Math.round((RANGE_MIN + (RANGE_MAX - RANGE_MIN) * rand.nextDouble())*100.0)/100.0, Math.round((RANGE_MIN + (RANGE_MAX - RANGE_MIN) * rand.nextDouble())*100.0)/100.0));
//        }

        PreProcessingPhase preProcessingPhaseInstance;
        QuadtreePhase quadtreePhaseInstance;
        PathFindingPhase pathFindingPhase;

        preProcessingPhaseInstance = new PreProcessingPhase(points);
        preProcessingPhaseInstance.preprocess();

        quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), PORTALS_PER_NODE_SIDE);
        quadtreePhaseInstance.assignPortals();

        pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide());
        pathFindingPhase.iterateNodes(quadtreePhaseInstance);
    }
}