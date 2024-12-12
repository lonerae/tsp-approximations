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

    public void iterateNodesOLD(QuadtreePhase quadtreePhaseInstance) {
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
            calculateParentPortalsOLD(parent, parentPortals);
        }
        if (!isNull(prevParent)) {
            prevParent.setSegments(updateSegments());
            iteratedNodes.add(prevParent);
        }
        calculateTSP();
    }
    public void iterateNodes(QuadtreePhase quadtreePhaseInstance) {
        for (QuadNode node : quadtreePhaseInstance.getQuadNodes()) {
            calculateNodePaths(node);
            iteratedNodes.add(node);
            calculateParentPortals(node.getParent(), node.getPortals());
            QuadNode parent = node.getParent();
            parent.setTraversed(parent.getTraversed() + 1);
            while (!isNull(parent) && parent.getTraversed() == 4) {
                parentPaths(parent);
                parent.setSegments(updateSegments());
                iteratedNodes.add(parent);
                if (!isNull(parent.getParent())) {
                    calculateParentPortals(parent.getParent(), parent.getPortals());
                    parent.getParent().setTraversed(parent.getParent().getTraversed() + 1);
                }
                parent = parent.getParent();
            }
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
                            List<Segment> tsp = new ArrayList<>();
                            for (Segment s : temp) {
                                l += s.getLength();
                                breakIntoSegments(tsp, s);
                            }
                            // TODO: synthesize is too slow
                             synthesize(tsp);
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

    private void breakIntoSegments(List<Segment> tsp, Segment s) {
        if (s.getInner().isEmpty()) {
            tsp.add(s);
        }
        else {
            for (Segment segment : s.getInner().getFirst().getSegments()) {
                breakIntoSegments(tsp, segment);
            }
        }
    }

    private void synthesize(List<Segment> tsp) {
        StringBuilder builder = new StringBuilder();
        int count = 1;
        builder.append(tsp.getFirst());
        Point current = tsp.getFirst().getEnd();
        int currentIndex = 0;
        while (count < tsp.size()) {
            int i = 0;
            while (i < tsp.size()) {
                if (i != currentIndex) {
                    if (tsp.get(i).getStart().equals(current)) {
                        if (!isNull(tsp.get(i).getTown()) && !tsp.get(i).getTown().equals(tsp.get(i).getEnd())) {
                            builder.append(" -> ").append(tsp.get(i).getTown());
                        }
                        builder.append(" -> ").append(tsp.get(i).getEnd());
                        current = tsp.get(i).getEnd();
                        currentIndex = i;
                        count++;
                        break;
                    } else if (tsp.get(i).getEnd().equals(current)) {
                        if (!isNull(tsp.get(i).getTown()) && !tsp.get(i).getTown().equals(tsp.get(i).getStart())) {
                            builder.append(" -> ").append(tsp.get(i).getTown());
                        }
                        builder.append(" -> ").append(tsp.get(i).getStart());
                        current = tsp.get(i).getStart();
                        currentIndex = i;
                        count++;
                        break;
                    }
                }
                i++;
            }
        }
        System.out.println(builder);
    }

    private List<Segment> updateSegments() {
        List<Segment> segments = new ArrayList<>();
        for (Path path : solutions) {
            Segment newSegment = new Segment(path.getEntrance(), path.getExit());
            newSegment.setLength(path.getLength());
            if (!path.getSegments().isEmpty()) {
                List<Path> detail = path.getSegments().getFirst().getInner();
                if (!detail.isEmpty()) {
                    newSegment.getInner().add(detail.getFirst());
                } else {
                    newSegment.getInner().add(path);
                }
            }
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
                if (node.getPoints().size() == 1) {
                    segment.setLength(SegmentUtils.getPathLengthThroughPoint(segment, node.getPoints().getFirst()));
                    segment.setTown(node.getPoints().getFirst());
                }
                else segment.setLength(SegmentUtils.getPathLength(segment));
                node.getSegments().add(segment);
            }
        }
    }

    private void calculateParentPortalsOLD(QuadNode parent, Set<Point> parentPortals) {
        List<QuadNode> children = List.of(
                parent.getUpperLeft(),
                parent.getUpperRight(),
                parent.getLowerRight(),
                parent.getLowerLeft()
        );
        for (QuadNode node : children) {
            for (int i = 0; i < node.getPortals().size(); i++) {
                if (QuadTreeUtils.pointIsOnNodeSides(parent, node.getPortals().get(i))) {
                    parentPortals.add(node.getPortals().get(i));
                }
            }
        }
    }
    private void calculateParentPortals(QuadNode parent, List<Point> portals) {
        for (Point portal : portals) {
            if (QuadTreeUtils.pointIsOnNodeSides(parent, portal)) {
                parent.getPortals().add(portal);
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
                solution.addLength(p.getLength());
                solution.addSegment(p);
                if (!p.getInner().isEmpty()) {
                    solution.getInner().add(p.getInner().stream().distinct().toList().getFirst());
                }
            }
            solutions.add(solution);
        }
        singleSolution.removeLast();
        pointCount -= node.getPoints().size();
    }
}
