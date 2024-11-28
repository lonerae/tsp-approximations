package gr.lonerae.phases;

import gr.lonerae.model.Path;
import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;
import gr.lonerae.model.Segment;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.SegmentUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.isNull;

public class PathFindingPhase {
    private double squareSize;

    private Point entrance;
    private Point exit;

    private int pointCountTarget;
    private int pointCount = 0;
    private final List<Segment> singleSolution = new ArrayList<>();
    private final List<Path> solutions = new ArrayList<>();

    private final List<QuadNode> iteratedNodes = new ArrayList<>();

    public PathFindingPhase(double squareSize) {
        this.squareSize = squareSize;
    }

    public void iterateNodes(QuadtreePhase quadtreePhaseInstance) {
        QuadNode parent = null;
        QuadNode prevParent = null;
        Set<Point> parentPortals = new HashSet<>();
        for (QuadNode node : quadtreePhaseInstance.getQuadNodes()) {
            if (isNull(parent)) {
                parent = node.getParent();
            } else if (!parent.equals(node.getParent())) {
                // Update previous parent node
                if (!isNull(prevParent)) {
                    prevParent.setSegments(updateSegments());
                    iteratedNodes.add(prevParent);
                }
                prevParent = parent;
                // Prepare current parent node
                parent.setPortals(new ArrayList<>(parentPortals));
                parentPaths(parent);
                // Get ready for next parent node
                parent = node.getParent();
                parentPortals = new HashSet<>();
            }
            calculateNodePaths(node);
            iteratedNodes.add(node);
            calculateParentPortals(parent, parentPortals);
        }
        if (!isNull(prevParent)) {
            prevParent.setSegments(updateSegments());
            iteratedNodes.add(prevParent);
        }
        calculateTSP();
    }

    private ArrayList<Segment> temp = new ArrayList<>();

    private void calculateTSP() {
        ArrayList<QuadNode> finalNodes = new ArrayList<>();
        for (QuadNode node : iteratedNodes) {
            if (node.getSide() == squareSize/2) {
                finalNodes.add(node);
            }
        }

        for (Segment segment : finalNodes.getFirst().getSegments()) {
            temp.clear();
            temp.add(segment);
            constructTSP(segment.getStart(), segment.getEnd(), finalNodes.subList(1,4));
        }
    }


    private double constructTSP(Point entrance, Point stitch, List<QuadNode> unvisited) {
        for (QuadNode node : unvisited) {
            if (node.getPortals().contains(stitch)) {
                ArrayList<QuadNode> updatedUnvisited = new ArrayList<>(unvisited);
                updatedUnvisited.remove(node);
                for (Segment newSegment : node.getSegments()) {
                    Point newStitch = SegmentUtils.contains(newSegment, stitch);
                    if (!isNull(newStitch)) {
                        temp.add(newSegment);
                        double x = constructTSP(entrance, newStitch, updatedUnvisited);
                        if (x == 0) {
                            System.out.println("TSP FOUND");
                            double l = 0;
                            for (Segment s : temp) {
                                l += s.getLength();
                            }
                            System.out.println("LENGTH: " + l);
                            temp.removeLast();
                        } else if (x < 0) {
                            temp.removeLast();
                        }
                    }
                }
                return -1;
            }
        }
        if (unvisited.isEmpty() && stitch.equals(entrance)) {
            return 0;
        }
        else return -1;
    }


    private List<Segment> updateSegments() {
        List<Segment> segments = new ArrayList<>();
        for (Path path : solutions) {
            Segment newSegment = new Segment(path.getEntrance(), path.getExit());
            newSegment.setLength(path.getLength());
            boolean distinct = true;
            for (Segment segment : segments) {
                if (SegmentUtils.commonEdges(newSegment, segment)) {
                    distinct = false;
                    if (segment.getLength() > newSegment.getLength()) {
                        segments.remove(segment);
                        segments.add(newSegment);
                    }
                }
            }
            if (distinct) segments.add(newSegment);
        }
        solutions.clear();
        return segments;
    }

    private void calculateNodePaths(QuadNode node) {
        for (int i = 0; i < node.getPortals().size() - 1; i++) {
            for (int j = i + 1; j < node.getPortals().size(); j++) {
                Segment segment = new Segment(node.getPortals().get(i), node.getPortals().get(j));
                if (node.getPoints().size() == 1) segment.setLength(SegmentUtils.getPathLengthThroughPoint(segment, node.getPoints().getFirst()));
                else segment.setLength(SegmentUtils.getPathLength(segment));
                node.getSegments().add(segment);
            }
        }
    }

    private void calculateParentPortals(QuadNode parent, Set<Point> parentPortals) {
        List<QuadNode> children = List.of(
                parent.getUpperLeft(),
                parent.getUpperRight(),
                parent.getLowerRight(),
                parent.getLowerLeft()
        );
        for (QuadNode node : children) {
            for (int i = 0; i < node.getPortals().size() - 1; i++) {
                for (int j = i + 1; j < node.getPortals().size(); j++) {
                    if (QuadTreeUtils.pointIsOnNodeSides(parent, node.getPortals().get(i))) {
                        parentPortals.add(node.getPortals().get(i));
                    }
                    if (QuadTreeUtils.pointIsOnNodeSides(parent, node.getPortals().get(j))) {
                        parentPortals.add(node.getPortals().get(j));
                    }
                }
            }
        }
    }

    private void parentPaths(QuadNode parent) {
        List<Point> parentPortals = parent.getPortals();
        pointCountTarget = parent.getPoints().size();
        ArrayList<QuadNode> children = new ArrayList<>(List.of(
                parent.getUpperLeft(),
                parent.getUpperRight(),
                parent.getLowerRight(),
                parent.getLowerLeft()
        ));
        for (int i = 0; i < parentPortals.size() - 1; i++) {
            entrance = parentPortals.get(i);
            for (int j = i + 1; j < parentPortals.size(); j++) {
                exit = parentPortals.get(j);
                searchChildren(entrance, children);
            }

        }
    }

    private void searchChildren(Point stitch, ArrayList<QuadNode> unvisited) {
        for (QuadNode node : unvisited) {
            if (node.getPortals().contains(stitch)) {
                iterateNodeSegments(stitch, unvisited, node);
            }
        }
    }

    private void iterateNodeSegments(Point stitch, ArrayList<QuadNode> unvisited, QuadNode node) {
        ArrayList<QuadNode> updatedUnvisited = new ArrayList<>(unvisited);
        updatedUnvisited.remove(node);
        for (Segment segment : node.getSegments()) {
            Point newStitch = SegmentUtils.contains(segment, stitch);
            // Segment contains previous vertex
            if (!isNull(newStitch)) {
                pointCount += node.getPoints().size();
                singleSolution.add(segment);
                if (newStitch.equals(exit)) {
                    savePath(node);
                } else if (updatedUnvisited.isEmpty()) {
                    backtrack(node);
                } else {
                    searchChildren(newStitch, updatedUnvisited);
                    backtrack(node);
                }
            }
        }
    }

    private void backtrack(QuadNode node) {
        if (!singleSolution.isEmpty()) {
            singleSolution.removeLast();
            pointCount -= node.getPoints().size();
        }
    }

    private void savePath(QuadNode node) {
        if (pointCount == pointCountTarget) {
            Path solution = new Path();
            solution.setEntrance(entrance);
            solution.setExit(exit);
            for (Segment p : singleSolution) {
                if (node.getPoints().size() == 1) {
                    solution.addLength(SegmentUtils.getPathLengthThroughPoint(p, node.getPoints().getFirst()));
                } else {
                    solution.addLength(SegmentUtils.getPathLength(p));
                }
                solution.addSegment(p);
            }
            solutions.add(solution);
        }
        singleSolution.removeLast();
        pointCount -= node.getPoints().size();
    }
}
