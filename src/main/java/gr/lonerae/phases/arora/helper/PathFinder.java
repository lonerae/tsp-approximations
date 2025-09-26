package gr.lonerae.phases.arora.helper;

import gr.lonerae.graphics.ProgressBar;
import gr.lonerae.model.Path;
import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;
import gr.lonerae.model.Segment;
import gr.lonerae.phases.arora.QuadtreePhase;
import gr.lonerae.utils.PathUtils;
import gr.lonerae.utils.PointUtils;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.SegmentUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

import static java.util.Objects.isNull;

public class PathFinder implements Runnable {
    private final QuadtreePhase quadtreePhaseInstance;
    private final List<QuadNode> iteratedNodes;

    private final Map<Point, List<Path>> allSolutions;
    private final List<Segment> currentSolution = new ArrayList<>();

    private QuadNode parent;
    private final Point startingTown;
    private final Point finishingTown;
    private Point entrance;
    private Point exit;

    Map<SegmentKey, Double> bestLengthHashMap = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final String PROGRESS_MESSAGE = "CURRENT PARENT: {}";

    @Data
    @AllArgsConstructor
    private static class SegmentKey {
        private Segment segment;
        private int towns;
    }

    public PathFinder(QuadtreePhase quadtreePhaseInstance, List<QuadNode> iteratedNodes, Map<Point, List<Path>> allSolutions, Point startingTown, Point finishingTown) {
        this.quadtreePhaseInstance = quadtreePhaseInstance;
        this.iteratedNodes = iteratedNodes;
        this.allSolutions = allSolutions;
        this.startingTown = startingTown;
        this.finishingTown = finishingTown;
    }

    @Override
    public void run() {
        for (QuadNode node : quadtreePhaseInstance.getQuadNodes()) {
            calculateFinalisedNodeSegments(node);
            iteratedNodes.add(node);
            calculateParentPortals(node.getParent(), node.getPortals());
            QuadNode currentParent = node.getParent();
            currentParent.setTraversed(currentParent.getTraversed() + 1);
            while (!isNull(currentParent) && currentParent.getTraversed() == 4) {
                currentParent = calculateParentNodeSegments(currentParent);
            }
            if (Thread.interrupted()) {
                return;
            }
        }
    }

    private void calculateFinalisedNodeSegments(QuadNode node) {
        for (int i = 0; i < node.getPortals().size() - 1; i++) {
            for (int j = i + 1; j < node.getPortals().size(); j++) {
                if (node.getTowns().isEmpty() || !isNull(startingTown)) {
                    addFinalisedSegment(node, i, j, false);
                }
                if (node.getTownNumber() == 1) {
                    addFinalisedSegment(node, i, j, true);
                }
            }
        }
    }

    private static void addFinalisedSegment(QuadNode node, int i, int j, boolean visitsTowns) {
        Segment segment = new Segment(node.getPortals().get(i), node.getPortals().get(j), true);
        if (visitsTowns) {
            segment.setLength(SegmentUtils.getPathLengthThroughPoint(segment, node.getTowns().getFirst()));
            segment.getTowns().add(node.getTowns().getFirst());
        } else {
            segment.setLength(SegmentUtils.getPathLength(segment));
        }
        node.getSegments().add(segment);
        updateSegments(node, segment);
    }

    private QuadNode calculateParentNodeSegments(QuadNode currentParent) {
        if (currentParent.getSide() != quadtreePhaseInstance.getRoot().getSide()) {
            LOGGER.log(Level.INFO, PROGRESS_MESSAGE, currentParent);
        }
        iteratedNodes.add(currentParent);
        parentPaths(currentParent);
        List<Segment> segTest = updateSegments(currentParent);
        currentParent.setSegments(segTest);
        for (Segment s : segTest) {
            updateSegments(currentParent, s);
        }
        currentParent.setSegments(currentParent.getSegments().stream().distinct().toList());
        removeSuboptimalSegments(currentParent);
        if (!isNull(currentParent.getParent())) {
            calculateParentPortals(currentParent.getParent(), currentParent.getPortals());
            currentParent.getParent().setTraversed(currentParent.getParent().getTraversed() + 1);
        }
        currentParent = currentParent.getParent();
        return currentParent;
    }

    private void parentPaths(QuadNode parent) {
        this.parent = parent;
        List<Point> parentPortals = parent.getPortals();
        ArrayList<QuadNode> children = new ArrayList<>(parent.getChildren().values());
        int loops = (parentPortals.size() * (parentPortals.size() - 1) / 2);
        int currentLoop = 0;
        for (int i = 0; i < parentPortals.size() - 1; i++) {
            entrance = parentPortals.get(i);
            for (int j = i + 1; j < parentPortals.size(); j++) {
                exit = parentPortals.get(j);
                searchChildren(entrance, children);
                ProgressBar.showProgress(currentLoop++, loops);
            }
        }
    }

    private static void updateSegments(QuadNode node, Segment segment) {
        updateSegments(node, segment, segment.getStart());
        updateSegments(node, segment, segment.getEnd());
    }

    private static void updateSegments(QuadNode node, Segment segment, Point entrance) {
        List<Segment> segmentsByEntrance = node.getSegmentsByEntrance().get(entrance);
        if (!isNull(segmentsByEntrance)) segmentsByEntrance.add(segment);
        else {
            node.getSegmentsByEntrance().put(entrance, new ArrayList<>());
            node.getSegmentsByEntrance().get(entrance).add(segment);
        }
    }

    private void removeSuboptimalSegments(QuadNode currentParent) {
        for (Point portalEntrance : currentParent.getPortals()) {
            for (Point portalExit : currentParent.getPortals()) {
                if (portalEntrance.equals(portalExit)) continue;
                for (int i = 2; i <= currentParent.getTownNumber(); i++) {
                    int finalI = i;
                    List<Segment> segmentsToRemove = currentParent.getSegments().stream().filter(s ->
                            SegmentUtils.hasEdges(s, portalEntrance, portalExit) &&
                            !s.getTowns().contains(startingTown) && !s.getTowns().contains(finishingTown) &&
                            s.getTownNumber() == finalI).toList();
                    ArrayList<Segment> mutableSegmentList = new ArrayList<>(currentParent.getSegments());
                    mutableSegmentList.removeAll(segmentsToRemove);
                    currentParent.setSegments(mutableSegmentList);
                    Segment optimalSegment = segmentsToRemove.stream().min(Comparator.comparingDouble(Segment::getLength)).orElse(null);
                    if (!isNull(optimalSegment)) currentParent.getSegments().add(optimalSegment);
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
                if (
                        (node.getTowns().contains(startingTown) && !segment.getTowns().contains(startingTown)) ||
                        (node.getTowns().contains(finishingTown) && !segment.getTowns().contains(finishingTown))
                ) {
                    continue;
                }
                currentSolution.add(segment);
                savePath();
            }
        } else continueToNeighborNode(stitch, node, updatedUnvisited);
    }

    private void continueToNeighborNode(Point stitch, QuadNode node, ArrayList<QuadNode> updatedUnvisited) {
        for (Segment segment : node.getSegmentsByEntrance().get(stitch)) {
            if (
                    (node.getTowns().contains(startingTown) && !segment.getTowns().contains(startingTown)) ||
                    (node.getTowns().contains(finishingTown) && !segment.getTowns().contains(finishingTown))
            ) {
                continue;
            }
            currentSolution.add(segment);
            Point nextStitch = segment.getStart().equals(stitch) ? segment.getEnd() : segment.getStart();
            if (segment.getStart().equals(exit) || segment.getEnd().equals(exit)) {
                savePath();
            } else if (
                    updatedUnvisited.isEmpty() ||
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
        if (!currentSolution.isEmpty()) {
            currentSolution.removeLast();
        }
    }

    private void savePath() {
        Path formalSolution = new Path();
        formalSolution.setEntrance(entrance);
        formalSolution.setExit(exit);
        constructSolution(formalSolution);

        updateMap(formalSolution, entrance, exit);
        updateMap(formalSolution, exit, entrance);
        currentSolution.removeLast();
    }

    private void constructSolution(Path formalSolution) {
        for (Segment segment : currentSolution) {
            formalSolution.addLength(segment.getLength());
            formalSolution.addSegment(segment);
            if (!isNull(segment.getTowns())) formalSolution.getTowns().addAll(segment.getTowns());
            if (!segment.getInner().isEmpty()) {
                formalSolution.getInner().add(segment.getInner().stream().distinct().toList().getFirst());
            }
        }
        setSolutionNode(formalSolution);
    }

    private void setSolutionNode(Path formalSolution) {
        for (QuadNode node : iteratedNodes) {
            if (
                node.getPortals().contains(entrance) &&
                node.getPortals().contains(exit) &&
                (
                        formalSolution.getSegments().isEmpty() ||
                        (
                            (PointUtils.compare(formalSolution.getSegments().getFirst().getStart(), node.getLowerLeftCorner()) > 0) &&
                            (PointUtils.compare(formalSolution.getSegments().getFirst().getEnd(), node.getLowerLeftCorner()) > 0)
                        )
                )
            ) {
                formalSolution.setNode(node);
                break;
            }

        }
    }

    private void updateMap(Path solution, Point point, Point other) {
        if (isNull(allSolutions.get(point))) {
            allSolutions.put(point, new ArrayList<>());
            allSolutions.get(point).add(solution);
        } else {
            Optional<Path> optionalChampion = allSolutions.get(point).stream().filter(path ->
                            PathUtils.hasEndpoint(path, other) &&
                            path.getTownNumber() == solution.getTownNumber() &&
                            path.getNode().equals(solution.getNode())
            ).findFirst();
            if (optionalChampion.isEmpty()) {
                allSolutions.get(point).add(solution);
            } else {
                Path currentBest = optionalChampion.get();
                boolean onlyNewSolutionContainsStartingTown = !solution.getTowns().isEmpty() && solution.getTowns().contains(startingTown) && !currentBest.getTowns().contains(startingTown);
                boolean onlyNewSolutionContainsFinishingTown = !solution.getTowns().isEmpty() && solution.getTowns().contains(finishingTown) && !currentBest.getTowns().contains(finishingTown);
                if (
                    currentBest.getLength() > solution.getLength() ||
                    onlyNewSolutionContainsStartingTown || onlyNewSolutionContainsFinishingTown
                ) {
                    allSolutions.get(point).remove(currentBest);
                    allSolutions.get(point).add(solution);
                }
            }
        }
    }

    private List<Segment> updateSegments(QuadNode parent) {
        List<Segment> segments = new ArrayList<>();
        for (Point portal : parent.getPortals()) {
            for (Path path : allSolutions.get(portal)) {
                Segment newSegment = new Segment(path.getEntrance(), path.getExit(), true);
                newSegment.setLength(path.getLength());
                newSegment.getTowns().addAll(path.getTowns());
                dive(path, newSegment);
                List<Segment> toAdd = new ArrayList<>();
                List<Segment> toDelete = new ArrayList<>();
                boolean shouldCompete = checkIfSegmentsShouldCompete(newSegment, toDelete, toAdd);
                if (!shouldCompete) segments.add(newSegment);
                else {
                    segments.addAll(toAdd);
                    segments.removeAll(toDelete);
                }
                addSegmentAlternatives(newSegment, segments);
            }
        }
        allSolutions.clear();
        return segments;
    }

    private void addSegmentAlternatives(Segment newSegment, List<Segment> segments) {
        addSegmentAlternative(newSegment, segments, startingTown, true);
        addSegmentAlternative(newSegment, segments, finishingTown, false);
    }

    private void addSegmentAlternative(Segment newSegment, List<Segment> segments, Point endpoint, boolean addingStartingTown) {
        if (
                newSegment.getTowns().contains(endpoint) &&
                newSegment.getTowns().size() > 2 &&
                !newSegment.getTowns().getFirst().equals(endpoint) &&
                !newSegment.getTowns().getLast().equals(endpoint)
        ) {
            Segment siblingSegment = new Segment(newSegment);
            siblingSegment.getTowns().remove(endpoint);
            if (addingStartingTown) siblingSegment.getTowns().addFirst(endpoint);
            else siblingSegment.getTowns().add(endpoint);
            segments.add(siblingSegment);
        }
    }

    private boolean checkIfSegmentsShouldCompete(Segment newSegment, List<Segment> toDelete, List<Segment> toAdd) {
        SegmentKey orderedKey = new SegmentKey(new Segment(newSegment.getStart(), newSegment.getEnd(), false), newSegment.getTownNumber());
        SegmentKey reversedKey = new SegmentKey(new Segment(newSegment.getEnd(), newSegment.getStart(), false), newSegment.getTownNumber());
        if (isNull(bestLengthHashMap.get(orderedKey)) && isNull(bestLengthHashMap.get(reversedKey))) {
            bestLengthHashMap.put(orderedKey, newSegment.getLength());
            return false;
        } else {
            Segment previousBest = !isNull(bestLengthHashMap.get(orderedKey)) ? orderedKey.getSegment() : reversedKey.getSegment();
            Segment currentBest = competeSegments(newSegment, previousBest, toDelete, toAdd);
            if (!isNull(bestLengthHashMap.get(orderedKey))) orderedKey.setSegment(currentBest);
            else reversedKey.setSegment(currentBest);
            return true;
        }
    }

    private Segment competeSegments(Segment newSegment, Segment oldSegment, List<Segment> toDelete, List<Segment> toAdd) {
        boolean newSegmentContainsStartingTown = newSegment.getTowns().contains(startingTown);
        boolean newSegmentContainsFinishingTown = newSegment.getTowns().contains(finishingTown);
        boolean oldSegmentContainsStartingTown = oldSegment.getTowns().contains(startingTown);
        boolean oldSegmentContainsFinishingTown = oldSegment.getTowns().contains(finishingTown);
        boolean bothContainStartingTown = oldSegmentContainsStartingTown && newSegmentContainsStartingTown;
        boolean neitherContainStartingTown = !oldSegmentContainsStartingTown && !newSegmentContainsStartingTown;
        boolean bothContainFinishingTown = oldSegmentContainsFinishingTown && newSegmentContainsFinishingTown;
        boolean neitherContainFinishingTown = !oldSegmentContainsFinishingTown && !newSegmentContainsFinishingTown;
        if (
                (
                        (!oldSegmentContainsStartingTown && newSegmentContainsStartingTown) ||
                        (!oldSegmentContainsFinishingTown && newSegmentContainsFinishingTown)
                ) ||
                (
                        (bothContainStartingTown || neitherContainStartingTown) &&
                        (bothContainFinishingTown || neitherContainFinishingTown) &&
                        oldSegment.getLength() > newSegment.getLength()
                )
        ) {
                toDelete.add(oldSegment);
                toAdd.add(newSegment);
                return newSegment;
        }
        return oldSegment;
    }

    private void dive(Path path, Segment newSegment) {
        for (Segment s : path.getSegments()) {
            List<Path> innerPaths = s.getInner();
            if (!innerPaths.isEmpty()) {
                for (Path p : innerPaths) {
                    dive(p, newSegment);
                }
            } else {
                newSegment.getInner().add(SegmentUtils.toPath(s));
            }
        }
    }
}
