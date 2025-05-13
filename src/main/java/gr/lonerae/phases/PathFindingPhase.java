package gr.lonerae.phases;

import com.google.common.collect.Sets;
import gr.lonerae.model.*;
import gr.lonerae.phases.helper.PathFinder;
import gr.lonerae.phases.helper.Untangler;
import gr.lonerae.utils.SegmentUtils;
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
    private final List<QuadNode> iteratedNodes = new ArrayList<>();
    private final List<Path> winners = new ArrayList<>();
    private final Map<Point, List<Path>> result = new HashMap<>();
    private Point start;
    private Point end;
    // TODO: check with k
    private int townsGoal;
    private ArrayList<Segment> solutionSegments = new ArrayList<>();
    private final ArrayList<Point> onLinePoints = new ArrayList<>();
    private final List<Segment> tempSolution = new ArrayList<>();
    private final HashMap<Integer, Path> solutionMap = new HashMap<>();

    public PathFindingPhase(double squareSize, int townNumber, int timeLimit) {
        this.squareSize = squareSize;
        this.townNumber = townNumber;
        this.timeLimit = timeLimit;
    }

    public Map<Integer, Solution> iterateNodes(QuadtreePhase quadtreePhaseInstance, Map<Point, ArrayList<Point>> clusters,
                                               boolean verbose, boolean isTour, Point start, Point end, int k
    ) {
        this.start = start;
        this.end = end;
        boolean success = calculateQuadNodePaths(quadtreePhaseInstance);
        if (success) {
            this.townsGoal = k;
            ArrayList<QuadNode> unvisitedNodes = new ArrayList<>(iteratedNodes.stream().filter(node -> node.getSide() == squareSize / 2).toList());
            QuadNode origin = unvisitedNodes.stream().filter(node -> node.getTowns().contains(this.start)).findFirst().orElseThrow();
            unvisitedNodes.remove(origin);
            solveKTSP(origin, unvisitedNodes);
            winners.addAll(solutionMap.values());
            if (!winners.isEmpty()) {
                Map<Integer, Solution> kTspSolutions = new HashMap<>();
                transformSolutions(verbose, kTspSolutions);
                return kTspSolutions;
            }
        }
        return Collections.emptyMap();
    }

    private boolean calculateQuadNodePaths(QuadtreePhase quadtreePhaseInstance) {
        boolean success = true;
        try(ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> future = executor.submit(new PathFinder(quadtreePhaseInstance, iteratedNodes, result, start, end));
            try {
                future.get(this.timeLimit, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                future.cancel(true);
                success = false;
                LOGGER.log(Level.INFO, "TIME LIMIT SURPASSED");
            } catch (Exception e) {
                LOGGER.error("e: ", e);
                success = false;
                Thread.currentThread().interrupt();
            } finally {
                executor.shutdownNow();
            }
        }
        return success;
    }

    private void solveKTSP(QuadNode origin, ArrayList<QuadNode> unvisitedNodes) {
        for (Segment segment : origin.getSegments().stream().filter(segment -> segment.getTowns().contains(this.start)).toList()) {
            this.townsGoal--;
            tempSolution.add(segment);
            branchOutFromStart(segment, unvisitedNodes);
            tempSolution.remove(segment);
            this.townsGoal++;
        }
    }

    private void branchOutFromStart(Segment segment, List<QuadNode> unvisitedNodes) {
        branchOutFromStart(segment, unvisitedNodes, true);
        branchOutFromStart(segment, unvisitedNodes, false);
    }

    private void branchOutFromStart(Segment segment, List<QuadNode> unvisitedNodes, boolean left) {
        for (QuadNode node : unvisitedNodes) {
            ArrayList<QuadNode> copyOfUnvisitedNodes = new ArrayList<>(unvisitedNodes);
            Point stitch = left ? segment.getStart() : segment.getEnd();
            if (node.getPortals().contains(stitch)) {
                copyOfUnvisitedNodes.remove(node);
                for (Segment nextSegment : node.getSegments().stream().filter(s -> s.getStart().equals(stitch) || s.getEnd().equals(stitch)).toList()) {
                    // TODO
                    // if (townsGoal - nextSegment.getTownNumber() < 1) continue;
                    townsGoal -= nextSegment.getTownNumber();
                    tempSolution.add(nextSegment);
                    Point temp = nextSegment.getStart().equals(stitch) ? nextSegment.getEnd() : nextSegment.getStart();
                    traverseNodes(temp, copyOfUnvisitedNodes);
                    tempSolution.remove(nextSegment);
                    townsGoal += nextSegment.getTownNumber();
                }
            }
        }
    }

    private void traverseNodes(Point stitch, List<QuadNode> unvisitedNodes) {
        ArrayList<QuadNode> copyOfUnvisitedNodes = new ArrayList<>(unvisitedNodes);
        for (QuadNode node : unvisitedNodes) {
            if (node.getPortals().contains(stitch)) {
                copyOfUnvisitedNodes.remove(node);
                if (node.getTowns().contains(this.end)) completePath(stitch, node);
                else continuePath(stitch, node, copyOfUnvisitedNodes);
            }
        }
    }

    private void continuePath(Point stitch, QuadNode node, ArrayList<QuadNode> copyOfUnvisitedNodes) {
        for (Segment nextSegment : node.getSegments().stream().filter(s -> s.getStart().equals(stitch) || s.getEnd().equals(stitch)).toList()) {
            tempSolution.add(nextSegment);
            townsGoal -= nextSegment.getTownNumber();
            Point temp = nextSegment.getStart().equals(stitch) ? nextSegment.getEnd() : nextSegment.getStart();
            traverseNodes(temp, copyOfUnvisitedNodes);
            tempSolution.remove(nextSegment);
            townsGoal += nextSegment.getTownNumber();
        }
    }

    private void completePath(Point stitch, QuadNode node) {
        for (Segment nextSegment : node.getSegments().stream().filter(s -> (s.getStart().equals(stitch) || s.getEnd().equals(stitch)) && s.getTowns().contains(this.end)).toList()) {
            tempSolution.add(nextSegment);
            townsGoal -= nextSegment.getTownNumber();
            Path path = new Path();
            // TODO: add according to case
            int currentTownNumber = 0;
            for (Segment originalSegment : tempSolution) {
                Segment segment = new Segment(originalSegment);
                if (segment.getTowns().contains(start) && !segment.getTowns().contains(end)) cutPathBeforeStart(segment, path);
                else if (segment.getTowns().contains(end) && !segment.getTowns().contains(start)) cutPathAfterEnd(segment, path);
                else if (segment.getTowns().contains(start) && segment.getTowns().contains(end)) cutPathOutsideEndpoints(segment, path);
                else path.addLength(segment.getLength());
                currentTownNumber += segment.getTowns().size();
                path.addSegment(segment);
            }
            updateSolutionMap(currentTownNumber, path);
            tempSolution.remove(nextSegment);
            townsGoal += nextSegment.getTownNumber();
        }
    }

    private void updateSolutionMap(int currentTownNumber, Path path) {
        if (isNull(solutionMap.get(currentTownNumber))) solutionMap.put(currentTownNumber, path);
        else if (solutionMap.get(currentTownNumber).getLength() > path.getLength()) solutionMap.replace(currentTownNumber, path);
    }

    private void cutPathOutsideEndpoints(Segment segment, Path path) {
        double length = 0;
        Point previous = null;
        for (Point town : segment.getTowns()) {
            if (!isNull(previous)) length += SegmentUtils.euclideanDistance(previous, town);
            if (town.equals(end)) break;
            previous = town;
        }
        path.addLength(length);
    }

    private void cutPathAfterEnd(Segment segment, Path path) {
        cutPath(segment, path, end, tempSolution.get(tempSolution.size() - 2));
    }

    private void cutPathBeforeStart(Segment segment, Path path) {
        cutPath(segment, path, start, tempSolution.get(1));
    }

    private void cutPath(Segment segment, Path path, Point pathEdge, Segment adjacentSegment) {
        double length = 0;
        Point previous;
        if (segment.getStart().equals(adjacentSegment.getStart()) || segment.getStart().equals(adjacentSegment.getEnd())) {
            previous = segment.getStart();
            segment.setTowns(segment.getTowns().subList(0, segment.getTowns().indexOf(pathEdge) + 1).reversed());
            for (Point town : segment.getTowns()) {
                length += SegmentUtils.euclideanDistance(previous, town);
                previous = town;
            }
        } else {
            previous = segment.getEnd();
            segment.setTowns(segment.getTowns().subList(segment.getTowns().indexOf(pathEdge), segment.getTowns().size()));
            for (Point town : segment.getTowns()) {
                length += SegmentUtils.euclideanDistance(previous, town);
                previous = town;
            }
        }
        path.addLength(length);
    }

    private void transformSolutions(boolean verbose, Map<Integer, Solution> kTspSolutions) {
        for (Path solution : winners) {
            transformSolution(verbose, kTspSolutions, solution);
        }
    }

    private void transformSolution(boolean verbose, Map<Integer, Solution> kTspSolutions, Path solution) {
        solutionSegments = new ArrayList<>();
        ArrayList<Point> sequence = new ArrayList<>();
        Point lead = null;
        double length = 0;
        Segment currentSegment = null;
        for (Segment segment : solution.getSegments()) {
            for (Point town : segment.getTowns()) {
                if (sequence.isEmpty()) sequence.add(town.getOriginal());
                else {
                    if (sequence.size() > 1) currentSegment = new Segment(sequence.get(sequence.size() - 2), sequence.getLast(), true);
                    lead = addTownToSequence(town, currentSegment, lead, sequence);
                }
            }
            //        explodeClusters(clusters); ???
        }
        assignLengthToSolution(verbose, kTspSolutions, sequence, length);
    }

    private Point addTownToSequence(Point town, Segment currentSegment, Point lead, ArrayList<Point> sequence) {
        if (!isNull(currentSegment) && !town.getOriginal().equals(currentSegment.getEnd()) && SegmentUtils.onLine(currentSegment, town.getOriginal())) {
            lead = Untangler.updateOnLinePoints(sequence, onLinePoints, solutionSegments, currentSegment, town.getOriginal());
        } else {
            // Add all previous towns belonging in the same straight line
            Untangler.addOnLinePoints(sequence, onLinePoints, solutionSegments, lead);
            Segment candidateSegment = new Segment(sequence.getLast(), town.getOriginal(), false);
            Untangler.postProcessing(sequence, solutionSegments, candidateSegment);
            sequence.add(town.getOriginal());
        }
        return lead;
    }

    private void assignLengthToSolution(boolean verbose, Map<Integer, Solution> kTspSolutions, ArrayList<Point> sequence, double length) {
        Point previousTown = null;
        for (Point currentTown : sequence) {
            if (isNull(previousTown)) previousTown = currentTown;
            else length += SegmentUtils.euclideanDistance(previousTown, currentTown);
        }
        if (verbose) LOGGER.log(Level.INFO, "TOWNS VISITED: {}/{}", sequence.size(), townNumber);
        if (verbose) LOGGER.log(Level.INFO, "LENGTH: {}", length);
        kTspSolutions.put(sequence.size(), new Solution(length, sequence, 0, 0.0, false, null));
    }

    private void explodeClusters(List<Point> sequence, Map<Point, ArrayList<Point>> clusters) {
        if (isNull(clusters)) return;
        for (Map.Entry<Point, ArrayList<Point>> entry : clusters.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                int segmentIndex = disconnectClusterSegments(entry);
                Point clusterStart = new Point(entry.getKey().getOriginal().getX(), entry.getKey().getOriginal().getY());
                int clusterStartIndex = sequence.indexOf(clusterStart);
                Set<Point> clusterPoints = new HashSet<>(entry.getValue());
                reconnectClusterSegments(sequence, clusterPoints, clusterStartIndex, segmentIndex);
            }
        }
    }

    private void reconnectClusterSegments(List<Point> sequence, Set<Point> clusterPoints, int clusterStartIndex, int segmentIndex) {
        double best = 0;
        ArrayList<Point> bestOrder = new ArrayList<>();
        for (Set<Point> comb : Sets.combinations(clusterPoints, clusterPoints.size())) {
            double candidate = 0;
            int i = 0;
            ArrayList<Point> temp2 = new ArrayList<>(comb);
            while (i < temp2.size() - 1) {
                candidate += SegmentUtils.euclideanDistance(temp2.get(i), temp2.get(i+1));
            }
            if (best == 0 || candidate < best) {
                best = candidate;
                bestOrder = temp2;
            }
        }
        sequence.addAll(clusterStartIndex +1, bestOrder);
        ArrayList<Segment> newSegments = new ArrayList<>();
        for (int i = 0; i < bestOrder.size() - 1; i++) {
            newSegments.add(new Segment(bestOrder.get(i), bestOrder.get(i+1), false));
        }
        solutionSegments.addAll(segmentIndex, newSegments);
    }

    private int disconnectClusterSegments(Map.Entry<Point, ArrayList<Point>> entry) {
        int segmentIndex = (int) solutionSegments.stream().map(Segment::getStart).takeWhile(not(entry.getKey().getOriginal()::equals)).count();
        if (segmentIndex+1 < solutionSegments.size()) solutionSegments.remove(segmentIndex + 1);
        if (segmentIndex < solutionSegments.size()) solutionSegments.remove(segmentIndex);
        return segmentIndex;
    }
}
