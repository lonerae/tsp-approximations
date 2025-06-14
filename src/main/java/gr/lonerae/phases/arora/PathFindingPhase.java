package gr.lonerae.phases.arora;

import com.google.common.collect.Sets;
import gr.lonerae.model.*;
import gr.lonerae.phases.arora.helper.PathFinder;
import gr.lonerae.phases.arora.helper.Untangler;
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

    private boolean sameNodes = false;
    private boolean oppositeNodes = false;

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
            QuadNode originNode = unvisitedNodes.stream().filter(node -> node.getTowns().contains(this.start)).findFirst().orElseThrow();
            QuadNode finishNode = unvisitedNodes.stream().filter(node -> node.getTowns().contains(this.end)).findFirst().orElseThrow();
            if (originNode.getLowerLeftCorner().equals(finishNode.getLowerLeftCorner())) sameNodes = true;
            else if (
                    originNode.getLowerLeftCorner().getX() != finishNode.getLowerLeftCorner().getX() &&
                    originNode.getLowerLeftCorner().getY() != finishNode.getLowerLeftCorner().getY()
            ) {
                oppositeNodes = true;
            }
            unvisitedNodes.remove(originNode);
            solveKTSP(originNode, unvisitedNodes);
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

    private void solveKTSP(QuadNode originNode, ArrayList<QuadNode> unvisitedNodes) {
        for (Segment segment : originNode.getSegments().stream().filter(segment -> segment.getTowns().contains(this.start)).toList()) {
            Segment segmentCopy = new Segment(segment);
//            this.townsGoal--;
            tempSolution.add(segmentCopy);
            if (segmentCopy.getTowns().contains(this.end)) constructPathAndUpdateSolutionMap();
            else branchOutFromStart(segmentCopy, unvisitedNodes);
            tempSolution.removeLast();
//            this.townsGoal++;
        }
    }

    private void branchOutFromStart(Segment segment, List<QuadNode> unvisitedNodes) {
        Segment segmentCopyForLeftBranching = new Segment(segment);
        Segment segmentCopyForRightBranching = new Segment(segment);
        branchOutFromStart(segmentCopyForLeftBranching, unvisitedNodes, true);
        tempSolution.removeLast();
        tempSolution.add(segmentCopyForRightBranching);
        branchOutFromStart(segmentCopyForRightBranching, unvisitedNodes, false);
    }

    private void branchOutFromStart(Segment segment, List<QuadNode> unvisitedNodes, boolean left) {
        for (QuadNode node : unvisitedNodes) {
            ArrayList<QuadNode> copyOfUnvisitedNodes = new ArrayList<>(unvisitedNodes);
            Point stitch = left ? segment.getStart() : segment.getEnd();
            if (node.getPortals().contains(stitch)) {
                copyOfUnvisitedNodes.remove(node);
                for (Segment nextSegment : node.getSegments().stream().filter(s -> s.getStart().equals(stitch) || s.getEnd().equals(stitch)).toList()) {
                    Segment segmentCopy = new Segment(nextSegment);
                    // TODO
                    // if (townsGoal - nextSegment.getTownNumber() < 1) continue;
                    townsGoal -= segmentCopy.getTownNumber();
                    tempSolution.add(segmentCopy);
                    Point temp = segmentCopy.getStart().equals(stitch) ? segmentCopy.getEnd() : segmentCopy.getStart();
                    traverseNodes(temp, copyOfUnvisitedNodes);
                    Segment removed = tempSolution.removeLast();
                    townsGoal += removed.getTownNumber();
                }
            }
        }
    }

    private void traverseNodes(Point stitch, List<QuadNode> unvisitedNodes) {
        ArrayList<QuadNode> copyOfUnvisitedNodes = new ArrayList<>(unvisitedNodes);
        for (QuadNode node : unvisitedNodes) {
            if (node.getPortals().contains(stitch)) {
                copyOfUnvisitedNodes.remove(node);
                if (node.getTowns().contains(this.end)) completePath(stitch, node, copyOfUnvisitedNodes);
                else continuePath(stitch, node, copyOfUnvisitedNodes);
            }
        }
        if (sameNodes && copyOfUnvisitedNodes.isEmpty()) {
            Segment dummyEnd = new Segment(stitch, end, false);
            dummyEnd.setTowns(List.of(end));
            tempSolution.add(dummyEnd);
            townsGoal -= dummyEnd.getTownNumber();
            constructPathAndUpdateSolutionMap();
            Segment removed = tempSolution.removeLast();
            townsGoal += removed.getTownNumber();
        }
    }

    private void continuePath(Point stitch, QuadNode node, ArrayList<QuadNode> unvisitedNodes) {
        for (Segment nextSegment : node.getSegments().stream().filter(s -> s.getStart().equals(stitch) || s.getEnd().equals(stitch)).toList()) {
            Segment segmentCopy = new Segment(nextSegment);
            tempSolution.add(segmentCopy);
            townsGoal -= segmentCopy.getTownNumber();
            Point temp = segmentCopy.getStart().equals(stitch) ? segmentCopy.getEnd() : segmentCopy.getStart();
            traverseNodes(temp, unvisitedNodes);
            Segment removed = tempSolution.removeLast();
            townsGoal += removed.getTownNumber();
        }
    }

    private void completePath(Point stitch, QuadNode node, ArrayList<QuadNode> unvisitedNodes) {
        for (Segment nextSegment : node.getSegments().stream().filter(s -> (s.getStart().equals(stitch) || s.getEnd().equals(stitch)) && s.getTowns().contains(this.end)).toList()) {
            Segment segmentCopy = new Segment(nextSegment);
            tempSolution.add(segmentCopy);
            townsGoal -= segmentCopy.getTownNumber();
            constructPathAndUpdateSolutionMap();
            if (oppositeNodes) {
                QuadNode remainingNode = unvisitedNodes.getFirst();
                for (int i = 1; i < remainingNode.getTowns().size() + 1; i++) {
                    int currentTownNumber = i;
                    Optional<Segment> bestPick = remainingNode.getSegments().stream().filter(segment -> segment.getTowns().size() == currentTownNumber).sorted(Comparator.comparingDouble(Segment::getLength)).findFirst();
                    if (bestPick.isPresent()) {
                        // Suffix
                        tempSolution.add(2, bestPick.get());
                        townsGoal -= bestPick.get().getTownNumber();
                        constructPathAndUpdateSolutionMap();
                        // Prefix
                        Collections.swap(tempSolution, 2, 1);
                        constructPathAndUpdateSolutionMap();
                        // Undo
                        tempSolution.remove(bestPick.get());
                        townsGoal += bestPick.get().getTownNumber();
                    }
                }
            }
            Segment removed = tempSolution.removeLast();
            townsGoal += removed.getTownNumber();
        }
    }

    private void constructPathAndUpdateSolutionMap() {
        Path path = new Path();
        int currentTownNumber = 0;
        for (Segment segment : tempSolution) {
            Segment segmentCopy = new Segment(segment);
            if (segment.getTowns().contains(start) && !segment.getTowns().contains(end)) {
                tempSolution.set(tempSolution.indexOf(segment), segmentCopy);
                cutPathBeforeStart(segmentCopy, path);
            }
            else if (segment.getTowns().contains(end) && !segment.getTowns().contains(start)) {
                tempSolution.set(tempSolution.indexOf(segment), segmentCopy);
                cutPathAfterEnd(segmentCopy, path);
            }
            else if (segment.getTowns().contains(start) && segment.getTowns().contains(end)) {
                tempSolution.set(tempSolution.indexOf(segment), segmentCopy);
                cutPathOutsideEndpoints(segmentCopy, path);
            }
            else path.addLength(segmentCopy.getLength());
            currentTownNumber += segmentCopy.getTowns().size();
            path.addSegment(segmentCopy);
        }
        updateSolutionMap(currentTownNumber, path);
    }

    private void updateSolutionMap(int currentTownNumber, Path path) {
        if (isNull(solutionMap.get(currentTownNumber))) {
            solutionMap.put(currentTownNumber, path);
        }
        else if (solutionMap.get(currentTownNumber).getLength() > path.getLength()) {
            solutionMap.replace(currentTownNumber, path);
        }
    }

    private void cutPathOutsideEndpoints(Segment segment, Path path) {
        int startIndex = segment.getTowns().indexOf(start);
        int endIndex = segment.getTowns().indexOf(end);
        if (startIndex < endIndex) {
            segment.setTowns(segment.getTowns().subList(startIndex, endIndex + 1));
        } else {
            segment.setTowns(segment.getTowns().subList(endIndex, startIndex + 1).reversed());
        }
        double length = 0;
        Point previous = null;
        for (Point town : segment.getTowns()) {
            if (!isNull(previous)) length += SegmentUtils.euclideanDistance(previous, town);
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
            onLinePoints.clear();
        }
    }

    private void transformSolution(boolean verbose, Map<Integer, Solution> kTspSolutions, Path solution) {
        solutionSegments = new ArrayList<>();
        ArrayList<Point> sequence = new ArrayList<>();
        Point lead = null;
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
        assignLengthToSolution(verbose, kTspSolutions, sequence);
    }

    private Point addTownToSequence(Point town, Segment currentSegment, Point lead, ArrayList<Point> sequence) {
        if (!isNull(currentSegment) && !town.getOriginal().equals(start) && !town.getOriginal().equals(end) && !town.getOriginal().equals(currentSegment.getEnd()) && SegmentUtils.onLine(currentSegment, town.getOriginal())) {
            lead = Untangler.updateOnLinePoints(sequence, onLinePoints, solutionSegments, currentSegment, town.getOriginal());
        } else {
            // Add all previous towns belonging in the same straight line
            Untangler.addOnLinePoints(sequence, onLinePoints, solutionSegments, lead);
            Segment candidateSegment = new Segment(sequence.getLast(), town.getOriginal(), false);
            solutionSegments.add(candidateSegment);
            sequence.add(town.getOriginal());
        }
        return lead;
    }

    private void assignLengthToSolution(boolean verbose, Map<Integer, Solution> kTspSolutions, ArrayList<Point> sequence) {
        if (!sequence.contains(end.getOriginal())) return;
        Untangler.untangle(sequence);
        fixFinalSegment(sequence);
        double length = getFinalSolutionLength(sequence);
        if (verbose) LOGGER.log(Level.INFO, "TOWNS VISITED: {}/{}", sequence.size(), townNumber);
        if (verbose) LOGGER.log(Level.INFO, "LENGTH: {}", length);
        kTspSolutions.put(sequence.size(), new Solution(length, sequence, 0, 0.0, false, null));
    }

    private void fixFinalSegment(ArrayList<Point> sequence) {
        if (!sequence.getLast().equals(end.getOriginal())) {
            int endIndex = sequence.indexOf(end.getOriginal());
            Collections.swap(sequence, endIndex, sequence.size() - 1);
        }
    }

    private static double getFinalSolutionLength(ArrayList<Point> sequence) {
        double length = 0;
        Point previousTown = null;
        for (Point currentTown : sequence) {
            if (isNull(previousTown)) previousTown = currentTown;
            else {
                length += SegmentUtils.euclideanDistance(previousTown, currentTown);
                previousTown = currentTown;
            }
        }
        return length;
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
