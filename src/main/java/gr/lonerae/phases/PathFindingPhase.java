package gr.lonerae.phases;

import com.google.common.collect.Sets;
import gr.lonerae.model.*;
import gr.lonerae.phases.helper.PathFinder;
import gr.lonerae.utils.PointUtils;
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
    private final ArrayList<Point> points = new ArrayList<>();
    private final ArrayList<Segment> temp = new ArrayList<>();
    private double target = 0.0;
    private List<Path> winners = new ArrayList<>();
    private List<Segment> winner;
    private final Map<Point, List<Path>> result = new HashMap<>();
    private int visitedTowns = 0;
    private Point start;
    private Point end;
    private int townsGoal;
    private Point lead = null;
    private ArrayList<Segment> solutionSegments = new ArrayList<>();
    private final ArrayList<Point> onLinePoints = new ArrayList<>();
    private List<Segment> tempSolution = new ArrayList<>();
    private HashMap<Integer, Path> solutionMap = new HashMap<>();

    public PathFindingPhase(double squareSize, int townNumber, int timeLimit) {
        this.squareSize = squareSize;
        this.townNumber = townNumber;
        this.timeLimit = timeLimit;
    }

    public Map<Integer, Solution> iterateNodes(QuadtreePhase quadtreePhaseInstance,
                                 Map<Point, ArrayList<Point>> clusters,
                                 boolean verbose,
                                 boolean isTour,
                                 Point start, Point end, int k) {
        boolean success = true;
        success = calculateQuadNodePaths(quadtreePhaseInstance, success);
        if (success) {
            this.start = start;
            this.end = end;
            this.townsGoal = k;
            ArrayList<QuadNode> unvisitedNodes = new ArrayList<>(iteratedNodes.stream().filter(node -> node.getSide() == squareSize / 2).toList());
            QuadNode origin = unvisitedNodes.stream().filter(node -> node.getPoints().contains(this.start)).findFirst().orElseThrow();
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

    private boolean calculateQuadNodePaths(QuadtreePhase quadtreePhaseInstance, boolean success) {
        try(ExecutorService executor = Executors.newSingleThreadExecutor()) {
            Future<?> future = executor.submit(new PathFinder(quadtreePhaseInstance, iteratedNodes, result));
            try {
                future.get(this.timeLimit, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                future.cancel(true);
                success = false;
                LOGGER.log(Level.INFO, "TIME LIMIT SURPASSED");
            } catch (Exception e) {
                LOGGER.error("e: ", e);
                success = false;
            } finally {
                executor.shutdownNow();
            }
        }
        return success;
    }

    private void transformSolutions(boolean verbose, Map<Integer, Solution> kTspSolutions) {
        for (Path solution : winners) {
            solutionSegments = new ArrayList<>();
            ArrayList<Point> sequence = new ArrayList<>();
            lead = null;
            double length = 0;
            Segment currentSegment = null;
            for (Segment s : solution.getSegments()) {
                for (Point town : s.getTowns()) {
                    if (sequence.isEmpty()) sequence.add(town.getOriginal());
                    else {
                        if (sequence.size() > 1) currentSegment = new Segment(sequence.get(sequence.size() - 2), sequence.getLast(), true);
                        if (!isNull(currentSegment) && !town.getOriginal().equals(currentSegment.getEnd()) && SegmentUtils.onLine(currentSegment, town.getOriginal())) {
                            updateOnLinePoints(currentSegment, town.getOriginal(), sequence);
                        } else {
                            addOnLinePoints(sequence);
                            Segment candidateSegment = new Segment(sequence.getLast(), town.getOriginal(), false);
                            postProcessing(candidateSegment, sequence);
                            sequence.add(town.getOriginal());
                        }
                    }
                }
                //        explodeClusters(clusters); ???
            }
            assignLengthToSolution(verbose, kTspSolutions, sequence, length);
        }
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

    private void solveKTSP(QuadNode origin, ArrayList<QuadNode> unvisitedNodes) {
        for (Segment segment : origin.getSegments().stream().filter(segment -> segment.getTowns().contains(this.start)).toList()) {
            this.townsGoal--;
            tempSolution.add(segment);
            branchOutFromStart(segment, unvisitedNodes);
            tempSolution.remove(segment);
            this.townsGoal++;
        }
    }

    private void updateOnLinePoints(Segment currentSegment, Point candidate, List<Point> sequence) {
        if (onLinePoints.isEmpty()) {
            onLinePoints.add(currentSegment.getStart());
            sequence.remove(currentSegment.getStart());
            onLinePoints.add(currentSegment.getEnd());
            sequence.remove(currentSegment.getEnd());
            if (!solutionSegments.isEmpty()) {
                solutionSegments.removeLast();
                if (!solutionSegments.isEmpty()) {
                    solutionSegments.removeLast();
                }
            }
        }
        if (!sequence.isEmpty()) lead = sequence.getLast();
        onLinePoints.add(candidate);
    }

    private void addOnLinePoints(List<Point> sequence) {
        if (!onLinePoints.isEmpty()) {
            onLinePoints.sort(PointUtils::compare);
            Boolean intersectFirst = null;
            Boolean intersectSecond = null;
            if (!isNull(lead)) {
                intersectFirst = solutionSegments.stream().anyMatch(segment -> SegmentUtils.intersect(segment, new Segment(onLinePoints.getFirst(), lead, false)));
                intersectSecond = solutionSegments.stream().anyMatch(segment -> SegmentUtils.intersect(segment, new Segment(onLinePoints.getLast(), lead, false)));
            }
            if (
                    (!isNull(intersectFirst) && Boolean.TRUE.equals(intersectFirst)) ||
                            (
                                    !isNull(lead) && Boolean.FALSE.equals(intersectSecond) &&
                                            SegmentUtils.euclideanDistance(onLinePoints.getLast(), lead) < SegmentUtils.euclideanDistance(onLinePoints.getFirst(), lead)
                            )
            ) {
                Collections.reverse(onLinePoints);
            }
            if (!sequence.isEmpty()) {
                Segment candidateSegment = new Segment(sequence.getLast(), onLinePoints.getFirst(), false);
                postProcessing(candidateSegment, sequence);
            }
            for (int i = 0; i < onLinePoints.size() - 1; i++) {
                Segment candidateSegment = new Segment(onLinePoints.get(i), onLinePoints.get(i+1), false);
                postProcessing(candidateSegment, sequence);
                sequence.add(onLinePoints.get(i));
            }
            sequence.add(onLinePoints.getLast());
            onLinePoints.clear();
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
                    if (townsGoal - nextSegment.getTownNumber() < 1) continue;
                    townsGoal -= nextSegment.getTownNumber();
                    tempSolution.add(nextSegment);
                    traverseNodes(nextSegment.getStart().equals(stitch) ? nextSegment.getEnd() : nextSegment.getStart(), copyOfUnvisitedNodes);
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
                if (node.getPoints().contains(this.end)) {
                    completePath(stitch, node);
                } else {
                    for (Segment nextSegment : node.getSegments().stream().filter(s -> s.getStart().equals(stitch) || s.getEnd().equals(stitch)).toList()) {
                        tempSolution.add(nextSegment);
                        townsGoal -= nextSegment.getTownNumber();
                        traverseNodes(nextSegment.getStart().equals(stitch) ? nextSegment.getEnd() : nextSegment.getStart(), copyOfUnvisitedNodes);
                        tempSolution.remove(nextSegment);
                        townsGoal += nextSegment.getTownNumber();
                    }
                }
            }
        }
    }

    private void completePath(Point stitch, QuadNode node) {
        for (Segment nextSegment : node.getSegments().stream().filter(s -> (s.getStart().equals(stitch) || s.getEnd().equals(stitch)) && s.getTowns().contains(this.end)).toList()) {
            tempSolution.add(nextSegment);
            townsGoal -= nextSegment.getTownNumber();
            Path path = new Path();
            int townNumber2 = 0;
            for (Segment segment : tempSolution) {
                path.addSegment(segment);
                path.addLength(segment.getLength());
                townNumber2 += segment.getTowns().size();
            }
            if (isNull(solutionMap.get(townNumber2))) solutionMap.put(townNumber2, path);
            else if (solutionMap.get(townNumber2).getLength() > path.getLength()) solutionMap.replace(townNumber2, path);
            tempSolution.remove(nextSegment);
            townsGoal += nextSegment.getTownNumber();
        }
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
            ArrayList<Point> temp2 = new ArrayList<>(comb);
            while (i < temp2.size() - 1) {
                candidate += SegmentUtils.euclideanDistance(temp2.get(i), temp2.get(i+1));
            }
            if (best == 0 || candidate < best) {
                best = candidate;
                bestOrder = temp2;
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

    private void postProcessing(Segment candidateSegment, List<Point> sequence) {
        ArrayList<Integer> toBeDeleted = new ArrayList<>();
        ArrayList<Segment> toBeReplaced = new ArrayList<>();
        ArrayList<Integer> toBeDominated = new ArrayList<>();
        ArrayList<Segment> toBeAdded = new ArrayList<>();
        int replaceIndex = 0;
        int dominated = 0;
        for (Segment segment : solutionSegments) {
            if (SegmentUtils.dominate(candidateSegment, segment)) {
                dominated = handleDominated(candidateSegment, segment, dominated, toBeDominated, toBeAdded);
            } else if (SegmentUtils.intersect(candidateSegment, segment)) {
                handleIntersected(candidateSegment, segment, toBeDeleted, toBeReplaced);
            }
        }
        for (Integer index : toBeDominated) {
            Segment segment = solutionSegments.get(index);
            if (index + 1 < solutionSegments.size() && solutionSegments.get(index+1).getStart().equals(segment.getEnd())) solutionSegments.remove(index+1);
            solutionSegments.remove(index);
            if (index > 0 && solutionSegments.get(index-1).getEnd().equals(segment.getStart())) solutionSegments.remove(index-1);
        }
        for (Integer index : toBeDeleted) {
            solutionSegments.add(new Segment(solutionSegments.get(index).getEnd(), candidateSegment.getEnd(), false));
            solutionSegments.set(index, toBeReplaced.get(replaceIndex));
            replaceIndex++;
        }
        solutionSegments.addAll(toBeAdded);
        if (toBeDeleted.isEmpty() && dominated == 0 && !sequence.isEmpty()) solutionSegments.add(candidateSegment);
    }

    private void handleIntersected(Segment candidateSegment, Segment segment, ArrayList<Integer> toBeDeleted, ArrayList<Segment> toBeReplaced) {
        int index1 = points.indexOf(segment.getEnd());
        int index2 = points.indexOf(candidateSegment.getStart());
        if (index1 > -1 && index2 > -1) {
            points.set(index1, candidateSegment.getStart());
            points.set(index2, segment.getEnd());
            toBeDeleted.add(solutionSegments.indexOf(segment));
            toBeReplaced.add(new Segment(segment.getStart(), candidateSegment.getStart(), false));
        }
    }

    private int handleDominated(Segment candidateSegment, Segment segment, int dominated, ArrayList<Integer> toBeDominated, ArrayList<Segment> toBeAdded) {
        dominated += 1;
        points.remove(segment.getStart());
        points.remove(segment.getEnd());
        toBeDominated.add(solutionSegments.indexOf(segment));
        if (SegmentUtils.euclideanDistance(candidateSegment.getStart(), segment.getStart()) < SegmentUtils.euclideanDistance(candidateSegment.getStart(), segment.getEnd())) {
            points.add(segment.getStart());
            points.add(segment.getEnd());
            toBeAdded.add(new Segment(candidateSegment.getStart(), segment.getStart(), false));
            toBeAdded.add(new Segment(segment.getStart(), segment.getEnd(), false));
            toBeAdded.add(new Segment(segment.getEnd(), candidateSegment.getEnd(), false));
        } else {
            points.add(segment.getEnd());
            points.add(segment.getStart());
            toBeAdded.add(new Segment(candidateSegment.getStart(), segment.getEnd(), false));
            toBeAdded.add(new Segment(segment.getEnd(), segment.getStart(), false));
            toBeAdded.add(new Segment(segment.getStart(), candidateSegment.getEnd(), false));
        }
        return dominated;
    }
}
