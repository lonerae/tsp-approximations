package gr.lonerae.phases;

import gr.lonerae.model.Path;
import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;
import gr.lonerae.model.Segment;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.SegmentUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

public class PathFinder implements Runnable {
    private final QuadtreePhase quadtreePhaseInstance;
    private final List<QuadNode> iteratedNodes;
    private final Map<Point, List<Path>> result;

    private QuadNode parent;

    private final List<Segment> singleSolution = new ArrayList<>();
    private final List<Path> solutions = new ArrayList<>();

    private Point entrance;
    private Point exit;
    private double currentLength = 0;
    private double bestLength = 0;

    public PathFinder(QuadtreePhase quadtreePhaseInstance, List<QuadNode> iteratedNodes, Map<Point, List<Path>> result) {
        this.quadtreePhaseInstance = quadtreePhaseInstance;
        this.iteratedNodes = iteratedNodes;
        this.result = result;
    }

    @Override
    public void run() {
        for (QuadNode node : quadtreePhaseInstance.getQuadNodes()) {
            calculateNodePaths(node);
            iteratedNodes.add(node);
            calculateParentPortals(node.getParent(), node.getPortals());
            QuadNode currentParent = node.getParent();
            currentParent.setTraversed(currentParent.getTraversed() + 1);
            while (!isNull(currentParent) && currentParent.getTraversed() == 4) {
                parentPaths(currentParent);
                List<Segment> segTest = updateSegments();
                currentParent.setSegments(segTest);
                for (Segment s : segTest) {
                    updateSegments(currentParent, s);
                }
                iteratedNodes.add(currentParent);
                if (!isNull(currentParent.getParent())) {
                    calculateParentPortals(currentParent.getParent(), currentParent.getPortals());
                    currentParent.getParent().setTraversed(currentParent.getParent().getTraversed() + 1);
                }
                currentParent = currentParent.getParent();
            }
            if (Thread.interrupted()) {
                return;
            }
        }
    }

    private static void updateSegments(QuadNode node, Segment segment) {
        List<Segment> segmentsByEntrance = node.getSegmentsByEntrance().get(segment.getStart());
        if (!isNull(segmentsByEntrance)) segmentsByEntrance.add(segment);
        else {
            ArrayList<Segment> first = new ArrayList<>();
            first.add(segment);
            node.getSegmentsByEntrance().put(segment.getStart(), first);
        }
        segmentsByEntrance = node.getSegmentsByEntrance().get(segment.getEnd());
        if (!isNull(segmentsByEntrance)) segmentsByEntrance.add(segment);
        else {
            ArrayList<Segment> first = new ArrayList<>();
            first.add(segment);
            node.getSegmentsByEntrance().put(segment.getEnd(), first);
        }
    }

    private void calculateParentPortals(QuadNode parent, List<Point> portals) {
        for (Point portal : portals) {
            if (QuadTreeUtils.pointIsOnNodeSides(parent, portal)) {
                parent.getPortals().add(portal);
            }
        }
    }

    private void calculateNodePaths(QuadNode node) {
        for (int i = 0; i < node.getPortals().size() - 1; i++) {
            for (int j = i + 1; j < node.getPortals().size(); j++) {
                if (node.getPoints().size() == 1) {
                    Segment segmentWithTown = new Segment(node.getPortals().get(i), node.getPortals().get(j), true);
                    segmentWithTown.setLength(SegmentUtils.getPathLengthThroughPoint(segmentWithTown, node.getPoints().getFirst()));
                    segmentWithTown.setTown(node.getPoints().getFirst());
                    segmentWithTown.traverseTown(node.getPoints().getFirst());
                    node.getSegments().add(segmentWithTown);
                    updateSegments(node, segmentWithTown);
                }
                Segment segment = new Segment(node.getPortals().get(i), node.getPortals().get(j), true);
                segment.setLength(SegmentUtils.getPathLength(segment));
                node.getSegments().add(segment);
                updateSegments(node, segment);
            }
        }
    }


    private void parentPaths(QuadNode parent) {
        this.parent = parent;
        List<Point> parentPortals = parent.getPortals();
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
            if (!isNull(node.getSegmentsByEntrance().get(stitch))) {
                iterateNodeSegments(stitch, unvisited, node);
            }
        }
    }

    private void iterateNodeSegments(Point stitch, ArrayList<QuadNode> unvisited, QuadNode node) {
        ArrayList<QuadNode> updatedUnvisited = new ArrayList<>(unvisited);
        updatedUnvisited.remove(node);
        if (!isNull(node.getSegmentsByEntrance().get(exit))) {
            for (Segment segment : node.getSegment(exit, stitch)) {
                singleSolution.add(segment);
                currentLength += segment.getLength();
                savePath();
            }
        } else {
            for (Segment segment : node.getSegmentsByEntrance().get(stitch)) {
                // Segment contains previous vertex
                singleSolution.add(segment);
                currentLength += segment.getLength();
                Point end = segment.getStart().equals(stitch) ? segment.getEnd() : segment.getStart();
                if (segment.getStart().equals(exit) || segment.getEnd().equals(exit)) {
                    savePath();
                } else if (
                        updatedUnvisited.isEmpty() ||
                                (currentLength > bestLength && bestLength != 0) ||
                                this.parent.getPortals().contains(end)
                ) {
                    backtrack();
                } else {
                    searchChildren(end, updatedUnvisited);
                    backtrack();
                }
            }
        }
    }

    private void backtrack() {
        if (!singleSolution.isEmpty()) {
            Segment removed = singleSolution.removeLast();
            currentLength -= removed.getLength();
        }
    }

    private void savePath() {
        Path solution = new Path();
        solution.setEntrance(entrance);
        solution.setExit(exit);
        for (QuadNode node : iteratedNodes) {
            if (node.getPortals().contains(entrance) && node.getPortals().contains(exit)) {
                solution.setNode(node);
                break;
            }
        }
        for (Segment p : singleSolution) {
            solution.addLength(p.getLength());
            solution.addSegment(p);
            solution.setTownNumber(solution.getTownNumber() + p.getTownNumber());
            if (!isNull(p.getTown())) solution.getTowns().add(p.getTown());
            if (!p.getInner().isEmpty()) {
                solution.getInner().add(p.getInner().stream().distinct().toList().getFirst());
            }
        }
        updateMap(solution, entrance);
        updateMap(solution, exit);
        solutions.add(solution);
        Segment removed = singleSolution.removeLast();
        currentLength -= removed.getLength();
    }

    private void updateMap(Path solution, Point point) {
        if (isNull(result.get(point))) {
            result.put(point, new ArrayList<>());
            result.get(point).add(solution);
        } else {
            boolean add = true;
            for (Path path : result.get(point)) {
                if (path.getTownNumber() == solution.getTownNumber()) {
                    add = false;
                    if (solution.getLength() < path.getLength()) {
                        result.get(point).set(result.get(point).indexOf(path), solution);
                    }
                    break;
                }
            }
            if (add) {
                result.get(point).add(solution);
            }
        }
    }

    private List<Segment> updateSegments() {
        List<Segment> segments = new ArrayList<>();
        for (Path path : solutions) {
            Segment newSegment = new Segment(path.getEntrance(), path.getExit(), true);
            newSegment.setLength(path.getLength());
            newSegment.setTownNumber(path.getTownNumber());
            newSegment.getTowns().addAll(path.getTowns());
            dive(path, newSegment);
            boolean distinct = true;
            List<Segment> toAdd = new ArrayList<>();
            List<Segment> toDelete = new ArrayList<>();
            for (Segment segment : segments) {
                Collections.sort(segment.getTowns());
                Collections.sort(newSegment.getTowns());
                if (SegmentUtils.commonEdges(newSegment, segment) && segment.getTowns().equals(newSegment.getTowns())) {
                    distinct = false;
                    if (segment.getLength() > newSegment.getLength()) {
                        toDelete.add(segment);
                        toAdd.add(newSegment);
                    }
                }
            }
            if (distinct) segments.add(newSegment);
            else {
                segments.addAll(toAdd);
                segments.removeAll(toDelete);
            }
        }
        solutions.clear();
        return segments;
    }

    private void dive(Path path, Segment newSegment) {
        for (Segment s : path.getSegments()) {
            List<Path> detail = s.getInner();
            if (!detail.isEmpty()) {
                for (Path p : detail) {
                    dive(p, newSegment);
                }
            } else {
                newSegment.getInner().add(SegmentUtils.toPath(s));
            }
        }
    }
}
