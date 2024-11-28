package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;

import java.util.*;

public class Main {
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


    public static void main(String[] args) {
        PreProcessingPhase preProcessingPhaseInstance;
        QuadtreePhase quadtreePhaseInstance;
        PathFindingPhase pathFindingPhase;

        preProcessingPhaseInstance = new PreProcessingPhase(points);
        preProcessingPhaseInstance.preprocess();

        quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints());
        quadtreePhaseInstance.assignPortals();

        pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide());
        pathFindingPhase.iterateNodes(quadtreePhaseInstance);
    }
}