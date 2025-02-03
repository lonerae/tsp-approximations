package gr.lonerae.phases;

import gr.lonerae.model.*;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.SegmentUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.isNull;

public class PathFindingPhase {
    private final double squareSize;
    private final int townNumber;

    private Point entrance;
    private Point exit;

    private int pointCountTarget;
    private int pointCount = 0;
    private final List<Segment> singleSolution = new ArrayList<>();
    private final List<Path> solutions = new ArrayList<>();

    private final List<QuadNode> iteratedNodes = new ArrayList<>();

    private final ArrayList<Point> points = new ArrayList<>();

    public PathFindingPhase(double squareSize, int townNumber) {
        this.squareSize = squareSize;
        this.townNumber = townNumber;
    }

    public Solution iterateNodes(QuadtreePhase quadtreePhaseInstance) {
        for (QuadNode node : quadtreePhaseInstance.getQuadNodes()) {
            calculateNodePaths(node);
            iteratedNodes.add(node);
            calculateParentPortals(node.getParent(), node.getPortals());
            QuadNode parent = node.getParent();
            parent.setTraversed(parent.getTraversed() + 1);
            while (!isNull(parent) && parent.getTraversed() == 4) {
                parentPaths(parent);
                List<Segment> segTest = updateSegments();
                parent.setSegments(segTest);
                for (Segment s : segTest) {
                    updateSegments(parent, s);
                }
                iteratedNodes.add(parent);
                if (!isNull(parent.getParent())) {
                    calculateParentPortals(parent.getParent(), parent.getPortals());
                    parent.getParent().setTraversed(parent.getParent().getTraversed() + 1);
                }
                parent = parent.getParent();
            }
        }
        calculateTSP();
        if (!isNull(winner)) {
            synthesize(winner);
            System.out.println("TOWNS VISITED:" + test + "/" + townNumber);
            double actualTarget = solutionSegments.stream()
                    .map(segment -> SegmentUtils.getEuclideanDistance(segment.getStart(), segment.getEnd()))
                    .reduce(0.0, Double::sum);
            System.out.println("LENGTH: " + actualTarget);
            return new Solution(actualTarget, points);
        }
        return null;
    }

    private final ArrayList<Segment> temp = new ArrayList<>();

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

    private double target = 0.0;
    private List<Segment> winner;

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
            tsp.add(s);
        }
        else {
            for (Path path : s.getInner()) {
                breakIntoSegments(tsp, path.getSegments().getFirst());
            }
        }
    }

    private int test = 0;
    private void synthesize(List<Segment> tsp) {
        int count = 1;
        if (!isNull(tsp.getFirst().getTown())) {
            points.add(tsp.getFirst().getTown().getOriginal());
            test++;
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
        addOnLinePoints();
    }

    private Point lead = null;
    private Segment currentSegment = null;
    private ArrayList<Segment> solutionSegments = new ArrayList<>();
    private ArrayList<Point> onLinePoints = new ArrayList<>();
    private void addTownToSolution(List<Segment> tsp, int i) {
        if (!isNull(tsp.get(i).getTown())) {
            if (points.size() > 1 && isNull(currentSegment)) currentSegment = new Segment(points.get(points.size() - 2), points.getLast(), false);
            Point candidate = tsp.get(i).getTown().getOriginal();
            if (!isNull(currentSegment) && !candidate.equals(currentSegment.getEnd()) && SegmentUtils.onLine(currentSegment, candidate)) {
                if (onLinePoints.isEmpty()) {
                    onLinePoints.add(currentSegment.getStart());
                    points.remove(currentSegment.getStart());
                    onLinePoints.add(currentSegment.getEnd());
                    points.remove(currentSegment.getEnd());
                    if (!solutionSegments.isEmpty()) {
                        solutionSegments.removeLast();
                        test -= 2;
                    }
                }
                if (!points.isEmpty()) lead = points.getLast();
                onLinePoints.add(candidate);
            } else {
                addOnLinePoints();
                currentSegment = null;
                Segment candidateSegment = new Segment(points.getLast(), candidate, false);
                ArrayList<Integer> toBeDeleted = new ArrayList<>();
                ArrayList<Segment> toBeReplaced = new ArrayList<>();
                int replaceIndex = 0;
                for (Segment segment : solutionSegments) {
                    if (SegmentUtils.segmentIntersect(candidateSegment, segment)) {
                        int index1 = points.indexOf(segment.getEnd());
                        int index2 = points.indexOf(candidateSegment.getStart());
                        points.set(index1, candidateSegment.getStart());
                        points.set(index2, segment.getEnd());
                        toBeDeleted.add(solutionSegments.indexOf(segment));
                        toBeReplaced.add(new Segment(segment.getStart(),candidateSegment.getStart(), false));
                    }
                }
                for (Integer index : toBeDeleted) {
                    solutionSegments.add(new Segment(solutionSegments.get(index).getEnd(),candidateSegment.getEnd(), false));
                    solutionSegments.set(index, toBeReplaced.get(replaceIndex));
                    replaceIndex++;
                }
                if (toBeDeleted.isEmpty() && !points.isEmpty()) solutionSegments.add(candidateSegment);
                points.add(candidate);
                test++;
            }
        }
    }

    private void addOnLinePoints() {
        if (!onLinePoints.isEmpty()) {
            onLinePoints.sort(this::compare);
            Boolean intersectFirst = null;
            Boolean intersectSecond = null;
            if (!isNull(lead)) {
                intersectFirst = solutionSegments.stream().anyMatch(segment -> SegmentUtils.segmentIntersect(segment, new Segment(onLinePoints.getFirst(), lead, false)));
                intersectSecond = solutionSegments.stream().anyMatch(segment -> SegmentUtils.segmentIntersect(segment, new Segment(onLinePoints.getLast(), lead, false)));
            }
            if (
                    (!isNull(intersectFirst) && Boolean.TRUE.equals(intersectFirst)) ||
                    (
                        !isNull(lead) && Boolean.FALSE.equals(intersectSecond) &&
                        SegmentUtils.getEuclideanDistance(onLinePoints.getLast(), lead) < SegmentUtils.getEuclideanDistance(onLinePoints.getFirst(), lead)
                    )
            ) {
                Collections.reverse(onLinePoints);
            }
            for (int i = 0; i < onLinePoints.size() - 1; i++) {
                if (!points.isEmpty()) {
                    solutionSegments.add(new Segment(onLinePoints.get(i), onLinePoints.get(i+1), false));
                }
                points.add(onLinePoints.get(i));
            }
            points.add(onLinePoints.getLast());
            test += onLinePoints.size();
            onLinePoints.clear();
        }
    }

    private int compare(Point p1, Point p2) {
        int result = Double.compare(p1.getX(), p2.getX());
        if (result==0) result = Double.compare(p1.getY(), p2.getY());
        return result;
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
                if (node.getPoints().size() == 1) {
                    segment.setLength(SegmentUtils.getPathLengthThroughPoint(segment, node.getPoints().getFirst()));
                    segment.setTown(node.getPoints().getFirst());
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
            Segment segment = node.getSegment(exit, stitch);
            if (!isNull(segment)) {
                pointCount += node.getPoints().size();
                singleSolution.add(segment);
                currentLength += segment.getLength();
                savePath(node);
            }
        } else {
            for (Segment segment : node.getSegmentsByEntrance().get(stitch)) {
                // Segment contains previous vertex
                pointCount += node.getPoints().size();
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
            pointCount -= node.getPoints().size();
        }
    }

    private double currentLength = 0;
    private double bestLength = 0;

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
        pointCount -= node.getPoints().size();
    }
}
