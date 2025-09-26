package gr.lonerae.phases.arora.helper;

import gr.lonerae.model.Path;
import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;
import gr.lonerae.model.Segment;
import gr.lonerae.phases.arora.QuadtreePhase;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.SegmentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

public class PathFinder implements Runnable {
    private final QuadtreePhase quadtreePhaseInstance;
    private final List<QuadNode> iteratedNodes;
    private final Map<Point, List<Path>> result;

    private QuadNode parent;

    private final List<Segment> singleSolution = new ArrayList<>();

    private Point start;
    private Point end;
    private Point entrance;
    private Point exit;
    private double currentLength = 0;
    private double bestLength = 0;

    public PathFinder(QuadtreePhase quadtreePhaseInstance, List<QuadNode> iteratedNodes, Map<Point, List<Path>> result, Point start, Point end) {
        this.quadtreePhaseInstance = quadtreePhaseInstance;
        this.iteratedNodes = iteratedNodes;
        this.result = result;
        this.start = start;
        this.end = end;
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
                System.out.println(currentParent + " " + currentParent.getSide());
                iteratedNodes.add(currentParent);
                parentPaths(currentParent);
                List<Segment> segTest = updateSegments(currentParent);
                currentParent.setSegments(segTest);
                for (Segment s : segTest) {
                    updateSegments(currentParent, s);
                }
                currentParent.setSegments(currentParent.getSegments().stream().distinct().toList());
                for (Point portalEntrance : currentParent.getPortals()) {
                    for (Point portalExit : currentParent.getPortals()) {
                        if (portalEntrance.equals(portalExit)) continue;
                        for (int i = 2; i <= currentParent.getTowns().size(); i++) {
                            int finalI = i;
                            List<Segment> segmentsToLeave = currentParent.getSegments().stream().filter(s ->
                                    ((s.getStart().equals(portalEntrance) && s.getEnd().equals(portalExit)) || (s.getStart().equals(portalExit) && s.getEnd().equals(start))) &&
                                    s.getTowns().size() == finalI).toList();
                            ArrayList<Segment> temp = new ArrayList<>(currentParent.getSegments());
                            temp.removeAll(segmentsToLeave);
                            currentParent.setSegments(temp);
                            Segment survivor = segmentsToLeave.stream().min(Comparator.comparingDouble(Segment::getLength)).orElse(null);
                            if (!isNull(survivor)) currentParent.getSegments().add(survivor);
                        }

                    }
                }
                if (!isNull(currentParent.getParent())) {
                    calculateParentPortals(currentParent.getParent(), currentParent.getPortals());
                    currentParent.getParent().setTraversed(currentParent.getParent().getTraversed() + 1);
                }
                if (currentParent.getSide() == 404) {
                    int x = 1;
                }
                currentParent = currentParent.getParent();
            }
            if (Thread.interrupted()) {
                return;
            }
        }
        int x = 1;
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
                if (node.getTowns().size() == 1) {
                    Segment segmentWithTown = new Segment(node.getPortals().get(i), node.getPortals().get(j), true);
                    segmentWithTown.setLength(SegmentUtils.getPathLengthThroughPoint(segmentWithTown, node.getTowns().getFirst()));
                    segmentWithTown.getTowns().add(node.getTowns().getFirst());
                    node.getSegments().add(segmentWithTown);
                    updateSegments(node, segmentWithTown);
                }
                if (node.getTowns().isEmpty() || !isNull(start)) {
                    Segment segment = new Segment(node.getPortals().get(i), node.getPortals().get(j), true);
                    segment.setLength(SegmentUtils.getPathLength(segment));
                    node.getSegments().add(segment);
                    updateSegments(node, segment);
                }
            }
        }
    }

    private void parentPaths(QuadNode parent) {
        this.parent = parent;
        List<Point> parentPortals = parent.getPortals();
        ArrayList<QuadNode> children = new ArrayList<>(parent.getChildren().values());
        for (int i = 0; i < parentPortals.size() - 1; i++) {
            System.out.println("I: " + i + "/" + parentPortals.size());
            entrance = parentPortals.get(i);
            for (int j = i + 1; j < parentPortals.size(); j++) {
                System.out.println("J: " + j + "/" + parentPortals.size());
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
                if (node.getTowns().contains(start) && !segment.getTowns().contains(start)) continue;
                if (node.getTowns().contains(end) && !segment.getTowns().contains(end)) continue;
                singleSolution.add(segment);
                currentLength += segment.getLength();
                savePath();
            }
        } else continueToNeighborNode(stitch, node, updatedUnvisited);
    }

    private void continueToNeighborNode(Point stitch, QuadNode node, ArrayList<QuadNode> updatedUnvisited) {
        for (Segment segment : node.getSegmentsByEntrance().get(stitch)) {
            if (node.getTowns().contains(start) && !segment.getTowns().contains(start)) continue;
            if (node.getTowns().contains(end) && !segment.getTowns().contains(end)) continue;
            // Segment contains previous vertex
            singleSolution.add(segment);
            currentLength += segment.getLength();
            Point nextStitch = segment.getStart().equals(stitch) ? segment.getEnd() : segment.getStart();
            if (segment.getStart().equals(exit) || segment.getEnd().equals(exit)) {
                savePath();
            } else if (
                    updatedUnvisited.isEmpty() ||
                    (currentLength > bestLength && bestLength != 0) ||
                    this.parent.getPortals().contains(nextStitch)
            ) {
                backtrack();
            } else {
                searchChildren(nextStitch, updatedUnvisited);
                backtrack();
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
        for (Segment segment : singleSolution) {
            solution.addLength(segment.getLength());
            solution.addSegment(segment);
            if (!isNull(segment.getTowns())) solution.getTowns().addAll(segment.getTowns());
            if (!segment.getInner().isEmpty()) {
                solution.getInner().add(segment.getInner().stream().distinct().toList().getFirst());
            }
        }
        for (QuadNode node : iteratedNodes) {
            if (node.getPortals().contains(entrance) && node.getPortals().contains(exit)) {
                if (!solution.getSegments().isEmpty()) {
                    if (
                            (solution.getSegments().getFirst().getStart().getX() >= node.getLowerLeftCorner().getX() && solution.getSegments().getFirst().getEnd().getX() >= node.getLowerLeftCorner().getX()) &&
                                    (solution.getSegments().getFirst().getStart().getY() >= node.getLowerLeftCorner().getY() && solution.getSegments().getFirst().getEnd().getY() >= node.getLowerLeftCorner().getY())
                    ) {
                        solution.setNode(node);
                        break;
                    }
                } else {
                    solution.setNode(node);
                    break;
                }
            }
        }
        updateMap(solution, entrance, exit);
        updateMap(solution, exit, entrance);
        Segment removed = singleSolution.removeLast();
        currentLength -= removed.getLength();
    }

    private void updateMap(Path solution, Point point, Point other) {
        if (isNull(result.get(point))) {
            result.put(point, new ArrayList<>());
            result.get(point).add(solution);
        } else {
            Optional<Path> champion = result.get(point).stream().filter(
                    x ->
                            (x.getExit().equals(other) || x.getEntrance().equals(other)) &&
                            x.getTownNumber() == solution.getTownNumber() && x.getNode().equals(solution.getNode())
            ).findFirst();
            if (champion.isEmpty()) {
                result.get(point).add(solution);
            } else if (champion.get().getLength() > solution.getLength()
                    || (!solution.getTowns().isEmpty() && solution.getTowns().contains(start) && !champion.get().getTowns().contains(start))
                    || (!solution.getTowns().isEmpty() && solution.getTowns().contains(end) && !champion.get().getTowns().contains(end))) {
                result.get(point).remove(champion.get());
                result.get(point).add(solution);
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class Key {
        private Segment segment;
        private int towns;
    }

    Map<Key, Double> test = new HashMap<>();
    private List<Segment> updateSegments(QuadNode parent) {
        List<Segment> segments = new ArrayList<>();
        for (Point portal : parent.getPortals()) {
            for (Path path : result.get(portal)) {
                Segment newSegment = new Segment(path.getEntrance(), path.getExit(), true);
                newSegment.setLength(path.getLength());
                newSegment.getTowns().addAll(path.getTowns());
                dive(path, newSegment);
                List<Segment> toAdd = new ArrayList<>();
                List<Segment> toDelete = new ArrayList<>();
                boolean gamesOn = checkIfSegmentsShouldCompete(newSegment, toDelete, toAdd);
                if (!gamesOn) segments.add(newSegment);
                else {
                    segments.addAll(toAdd);
                    segments.removeAll(toDelete);
                }
                if (newSegment.getTowns().contains(start) && newSegment.getTowns().size() > 2 && !newSegment.getTowns().getFirst().equals(start) && !newSegment.getTowns().getLast().equals(start)) {
                    Segment siblingSegment = new Segment(newSegment);
                    siblingSegment.getTowns().remove(start);
                    siblingSegment.getTowns().addFirst(start);
                    segments.add(siblingSegment);
                }
                if (newSegment.getTowns().contains(end) && newSegment.getTowns().size() > 2 && !newSegment.getTowns().getFirst().equals(end) && !newSegment.getTowns().getLast().equals(end)) {
                    Segment siblingSegment = new Segment(newSegment);
                    siblingSegment.getTowns().remove(end);
                    siblingSegment.getTowns().add(end);
                    segments.add(siblingSegment);
                }
            }
        }
        result.clear();
        return segments;
    }

    private boolean checkIfSegmentsShouldCompete(Segment newSegment, List<Segment> toDelete, List<Segment> toAdd) {
        Key key1 = new Key(new Segment(newSegment.getStart(), newSegment.getEnd(), false), newSegment.getTowns().size());
        Key key2 = new Key(new Segment(newSegment.getEnd(), newSegment.getStart(), false), newSegment.getTowns().size());
        if (isNull(test.get(key1)) && isNull(test.get(key2))) {
            test.put(key1, newSegment.getLength());
            return false;
        } else {
            Segment champion = !isNull(test.get(key1)) ? key1.getSegment() : key2.getSegment();
            Segment winner = competeSegments(newSegment, toDelete, toAdd, champion);
            if (!isNull(test.get(key1))) key1.setSegment(winner);
            else key2.setSegment(winner);
            return true;
        }
    }

    private Segment competeSegments(Segment newSegment, List<Segment> toDelete, List<Segment> toAdd, Segment segment) {
        if (
                ((!segment.getTowns().contains(start) && !newSegment.getTowns().contains(start)) || (segment.getTowns().contains(start) && newSegment.getTowns().contains(start))) &&
                ((!segment.getTowns().contains(end) && !newSegment.getTowns().contains(end)) || (segment.getTowns().contains(end) && newSegment.getTowns().contains(end)))
        ) {
            if (segment.getLength() > newSegment.getLength()) {
                toDelete.add(segment);
                toAdd.add(newSegment);
                return newSegment;
            }
        } else if (
                (!segment.getTowns().contains(start) && newSegment.getTowns().contains(start)) ||
                (!segment.getTowns().contains(end) && newSegment.getTowns().contains(end))
        ) {
            toDelete.add(segment);
            toAdd.add(newSegment);
            return newSegment;
        }
        return segment;
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
