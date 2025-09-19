package gr.lonerae.phases.arora;

import com.google.common.collect.Sets;
import gr.lonerae.model.*;
import gr.lonerae.phases.arora.helper.Untangler;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.SegmentUtils;
import gr.lonerae.utils.constants.GeneralConstants;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.*;

import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;

public class PathFindingPhase {
    private static final Logger LOGGER = LogManager.getLogger();

    private final double squareSize;
    private final int townNumber;
    private final int timeLimit;

    private Point entrance;
    private Point exit;

    private int pointCountTarget;
    private int pointCount = 0;
    private final List<Segment> singleSolution = new ArrayList<>();
    private final List<Path> solutions = new ArrayList<>();

    private final List<QuadNode> iteratedNodes = new ArrayList<>();

    private final ArrayList<Point> points = new ArrayList<>();

    private final ArrayList<Segment> temp = new ArrayList<>();

    private double target = 0.0;
    private List<Segment> winner;

    private int visitedTowns = 0;

    private Point lead = null;
    private Segment currentSegment = null;
    private final ArrayList<Segment> solutionSegments = new ArrayList<>();
    private final ArrayList<Point> onLinePoints = new ArrayList<>();

    private double currentLength = 0;
    private double bestLength = 0;

    public PathFindingPhase(double squareSize, int townNumber, int timeLimit) {
        this.squareSize = squareSize;
        this.townNumber = townNumber;
        this.timeLimit = timeLimit;
    }

    public Solution iterateNodes(QuadtreePhase quadtreePhaseInstance, Map<Point, ArrayList<Point>> clusters, boolean verbose) {
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
        }

        calculateTSP();
        if (!isNull(winner)) {
            synthesize(winner, clusters);
            Untangler.untangle(points);
            if (verbose) LOGGER.log(Level.INFO, "TOWNS VISITED: {}/{}", visitedTowns, townNumber);
            double actualTarget = 0;
            for (int i = 1; i < points.size(); i++) {
                actualTarget += SegmentUtils.euclideanDistance(points.get(i), points.get(i-1));
            }
            if (verbose) LOGGER.log(Level.INFO, "LENGTH: {}", actualTarget);
            return new Solution(actualTarget, points, 0, 0.0, false, null);
        }

        return null;
    }

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
                    chainSegment(entrance, stitch, newSegment, updatedUnvisited);
                }
                return -1;
            }
        }
        if (unvisited.isEmpty() && stitch.equals(entrance)) {
            return 0;
        }
        else return -1;
    }

    private void chainSegment(Point entrance, Point stitch, Segment newSegment, ArrayList<QuadNode> updatedUnvisited) {
        Point newStitch = SegmentUtils.contains(newSegment, stitch);
        if (!isNull(newStitch)) {
            temp.add(newSegment);
            double x = constructTSP(entrance, newStitch, updatedUnvisited);
            if (x == 0) {
                updateOptimalTSP();
            } else if (x < 0) {
                temp.removeLast();
            }
        }
    }

    private void updateOptimalTSP() {
        double l = 0;
        List<Segment> tsp = new ArrayList<>();
        for (Segment s : temp) {
            l += s.getLength();
            breakIntoSegments(tsp, s);
        }
        if (target == 0 || l < target) {
            target = l;
            winner = tsp;
        }
        temp.removeLast();
    }

    private void breakIntoSegments(List<Segment> tsp, Segment s) {
        if (s.getInner().isEmpty()) {
            if (!tsp.isEmpty() &&
                    (tsp.getFirst().getStart().equals(s.getStart()) || tsp.getFirst().getStart().equals(s.getEnd())
                    || tsp.getFirst().getEnd().equals(s.getStart()) || tsp.getFirst().getEnd().equals(s.getEnd()))) {
                tsp.addFirst(s);
            } else tsp.add(s);
        }
        else {
            for (Path path : s.getInner()) {
                breakIntoSegments(tsp, path.getSegments().getFirst());
            }
        }
    }

    private void synthesize(List<Segment> tsp, Map<Point, ArrayList<Point>> clusters) {
        int count = 1;
        if (!tsp.getFirst().getTowns().isEmpty() && !isNull(tsp.getFirst().getTowns().getFirst())) {
            points.add(tsp.getFirst().getTowns().getFirst().getOriginal());
            visitedTowns++;
        }
        Point current = tsp.getFirst().getEnd();
        int currentIndex = 0;
        while (count < tsp.size()) {
            int i = 0;
            while (i < tsp.size()) {
                if (i != currentIndex) {
                    if (tsp.get(i).getStart().equals(current)) {
                        addTownToSolution(tsp, i);
                        current = tsp.get(i).getEnd();
                        currentIndex = i;
                        count++;
                        break;
                    } else if (tsp.get(i).getEnd().equals(current)) {
                        addTownToSolution(tsp, i);
                        current = tsp.get(i).getStart();
                        currentIndex = i;
                        count++;
                        break;
                    }
                }
                i++;
            }
        }
        explodeClusters(clusters);
    }

    private void explodeClusters(Map<Point, ArrayList<Point>> clusters) {
        if (isNull(clusters)) return;
        for (Map.Entry<Point, ArrayList<Point>> entry : clusters.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                int segmentIndex = disconnectClusterSegments(entry);
                Point clusterStart = new Point(entry.getKey().getOriginal().getX(), entry.getKey().getOriginal().getY());
                int clusterStartIndex = points.indexOf(clusterStart);
                Set<Point> clusterPoints = new HashSet<>(entry.getValue());
                reconnectClusterSegments(clusterPoints, clusterStartIndex, segmentIndex);
            }
        }
    }

    private void reconnectClusterSegments(Set<Point> clusterPoints, int clusterStartIndex, int segmentIndex) {
        double best = 0;
        ArrayList<Point> bestOrder = new ArrayList<>();
        for (Set<Point> comb : Sets.combinations(clusterPoints, clusterPoints.size())) {
            double candidate = 0;
            int i = 0;
            ArrayList<Point> temp = new ArrayList<>(comb);
            while (i < temp.size() - 1) {
                candidate += SegmentUtils.euclideanDistance(temp.get(i), temp.get(i+1));
            }
            if (best == 0 || candidate < best) {
                best = candidate;
                bestOrder = temp;
            }
        }
        points.addAll(clusterStartIndex +1, bestOrder);
        ArrayList<Segment> newSegments = new ArrayList<>();
        for (int i = 0; i < bestOrder.size() - 1; i++) {
            newSegments.add(new Segment(bestOrder.get(i), bestOrder.get(i+1), false));
        }
        solutionSegments.addAll(segmentIndex, newSegments);
        visitedTowns += bestOrder.size();
    }

    private int disconnectClusterSegments(Map.Entry<Point, ArrayList<Point>> entry) {
        int segmentIndex = (int) solutionSegments.stream().map(Segment::getStart).takeWhile(not(entry.getKey().getOriginal()::equals)).count();
        if (segmentIndex+1 < solutionSegments.size()) solutionSegments.remove(segmentIndex + 1);
        if (segmentIndex < solutionSegments.size()) solutionSegments.remove(segmentIndex);
        return segmentIndex;
    }

    private void addTownToSolution(List<Segment> tsp, int i) {
        if (!tsp.get(i).getTowns().isEmpty() && !isNull(tsp.get(i).getTowns().getFirst())) {
            Point candidate = tsp.get(i).getTowns().getFirst().getOriginal();
            points.add(candidate);
            visitedTowns++;
        }
    }

    private List<Segment> updateSegments() {
        List<Segment> segments = new ArrayList<>();
        for (Path path : solutions) {
            Segment newSegment = new Segment(path.getEntrance(), path.getExit(), true);
            newSegment.setLength(path.getLength());
            dive(path, newSegment);
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

    private void calculateNodePaths(QuadNode node) {
        for (int i = 0; i < node.getPortals().size() - 1; i++) {
            for (int j = i + 1; j < node.getPortals().size(); j++) {
                Segment segment = new Segment(node.getPortals().get(i), node.getPortals().get(j), true);
                if (node.getTowns().size() == 1) {
                    segment.setLength(SegmentUtils.getPathLengthThroughPoint(segment, node.getTowns().getFirst()));
                    segment.getTowns().add(node.getTowns().getFirst());
                }
                else segment.setLength(SegmentUtils.getPathLength(segment));
                node.getSegments().add(segment);
                updateSegments(node, segment);
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

    private QuadNode parent;
    private void parentPaths(QuadNode parent) {
        this.parent = parent;
        List<Point> parentPortals = parent.getPortals();
        pointCountTarget = parent.getTowns().size();
        ArrayList<QuadNode> children = new ArrayList<>(List.of(
                parent.getChildren().get(GeneralConstants.UPPER_LEFT),
                parent.getChildren().get(GeneralConstants.UPPER_RIGHT),
                parent.getChildren().get(GeneralConstants.LOWER_RIGHT),
                parent.getChildren().get(GeneralConstants.LOWER_LEFT)
        ));
        for (int i = 0; i < parentPortals.size() - 1; i++) {
            entrance = parentPortals.get(i);
            for (int j = i + 1; j < parentPortals.size(); j++) {
                exit = parentPortals.get(j);
                bestLength = 0;
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
            if (!node.getSegment(exit, stitch).isEmpty()) {
                Segment segment = node.getSegment(exit, stitch).getFirst();
                if (!isNull(segment)) {
                    pointCount += node.getTowns().size();
                    singleSolution.add(segment);
                    currentLength += segment.getLength();
                    savePath(node);
                }
            }
        } else {
            for (Segment segment : node.getSegmentsByEntrance().get(stitch)) {
                // Segment contains previous vertex
                pointCount += node.getTowns().size();
                singleSolution.add(segment);
                currentLength += segment.getLength();
                Point end = segment.getStart().equals(stitch) ? segment.getEnd() : segment.getStart();
                if (segment.getStart().equals(exit) || segment.getEnd().equals(exit)) {
                    savePath(node);
                } else if (
                        updatedUnvisited.isEmpty() ||
                                (currentLength > bestLength && bestLength != 0) ||
                                this.parent.getPortals().contains(end)
                ) {
                    backtrack(node);
                } else {
                    searchChildren(end, updatedUnvisited);
                    backtrack(node);
                }
            }
        }
    }

    private void backtrack(QuadNode node) {
        if (!singleSolution.isEmpty()) {
            currentLength -= singleSolution.getLast().getLength();
            singleSolution.removeLast();
            pointCount -= node.getTowns().size();
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
            if (bestLength == 0 || solution.getLength() < bestLength) {
                bestLength = solution.getLength();
                solutions.add(solution);
            }
        }
        currentLength -= singleSolution.getLast().getLength();
        singleSolution.removeLast();
        pointCount -= node.getTowns().size();
    }
}