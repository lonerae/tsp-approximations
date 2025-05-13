package gr.lonerae.phases.helper;

import gr.lonerae.model.Point;
import gr.lonerae.model.Segment;
import gr.lonerae.utils.PointUtils;
import gr.lonerae.utils.SegmentUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.isNull;

public class Untangler {

    private Untangler(){}

    public static Point updateOnLinePoints(List<Point> sequence, List<Point> onLinePoints, List<Segment> solutionSegments, Segment currentSegment, Point candidate) {
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
        Point lead = null;
        if (!sequence.isEmpty()) lead = sequence.getLast();
        onLinePoints.add(candidate);
        return lead;
    }

    public static void addOnLinePoints(List<Point> sequence, List<Point> onLinePoints, List<Segment> solutionSegments, Point lead) {
        if (!onLinePoints.isEmpty()) {
            onLinePoints.sort(PointUtils::compare);
            boolean intersectFirst = false;
            boolean intersectSecond = true;
            if (!isNull(lead)) {
                intersectFirst = solutionSegments.stream().anyMatch(segment -> SegmentUtils.intersect(segment, new Segment(onLinePoints.getFirst(), lead, false)));
                intersectSecond = solutionSegments.stream().anyMatch(segment -> SegmentUtils.intersect(segment, new Segment(onLinePoints.getLast(), lead, false)));
            }
            if (intersectFirst || (!isNull(lead) && !intersectSecond && leadCloserToEnd(onLinePoints, lead))) Collections.reverse(onLinePoints);
            if (!sequence.isEmpty()) postProcessing(sequence, solutionSegments, new Segment(sequence.getLast(), onLinePoints.getFirst(), false));
            for (int i = 0; i < onLinePoints.size() - 1; i++) {
                sequence.add(onLinePoints.get(i));
                postProcessing(sequence, solutionSegments, new Segment(onLinePoints.get(i), onLinePoints.get(i+1), false));
            }
            sequence.add(onLinePoints.getLast());
            onLinePoints.clear();
        }
    }

    private static boolean leadCloserToEnd(List<Point> onLinePoints, Point lead) {
        return SegmentUtils.euclideanDistance(onLinePoints.getLast(), lead) < SegmentUtils.euclideanDistance(onLinePoints.getFirst(), lead);
    }

    public static void postProcessing(List<Point> sequence, List<Segment> solutionSegments, Segment candidateSegment) {
        ArrayList<Integer> toBeDeleted = new ArrayList<>();
        ArrayList<Segment> toBeReplaced = new ArrayList<>();
        ArrayList<Integer> toBeDominated = new ArrayList<>();
        ArrayList<Segment> toBeAdded = new ArrayList<>();
        int replaceIndex = 0;
        int dominated = 0;
        for (Segment segment : solutionSegments) {
            if (SegmentUtils.dominate(candidateSegment, segment)) dominated = handleDominated(sequence, solutionSegments, candidateSegment, segment, dominated, toBeDominated, toBeAdded);
            else if (SegmentUtils.intersect(candidateSegment, segment)) handleIntersected(sequence, solutionSegments, candidateSegment, segment, toBeDeleted, toBeReplaced);
        }
        for (Integer index : toBeDominated) {
            Segment segment = solutionSegments.get(index);
            if (index + 1 < solutionSegments.size() && solutionSegments.get(index + 1).getStart().equals(segment.getEnd())) solutionSegments.remove(index + 1);
            solutionSegments.remove(solutionSegments.get(index));
            if (index > 0 && solutionSegments.get(index - 1).getEnd().equals(segment.getStart())) solutionSegments.remove(index - 1);
        }
        for (Integer index : toBeDeleted) {
            solutionSegments.add(new Segment(solutionSegments.get(index).getEnd(), candidateSegment.getEnd(), false));
            solutionSegments.set(index, toBeReplaced.get(replaceIndex));
            replaceIndex++;
        }
        solutionSegments.addAll(toBeAdded);
        if (toBeDeleted.isEmpty() && dominated == 0 && !sequence.isEmpty()) solutionSegments.add(candidateSegment);
    }

    private static void handleIntersected(List<Point> sequence, List<Segment> solutionSegments, Segment candidateSegment, Segment segment, ArrayList<Integer> toBeDeleted, ArrayList<Segment> toBeReplaced) {
        int index1 = sequence.indexOf(segment.getEnd());
        int index2 = sequence.indexOf(candidateSegment.getStart());
        if (index1 > -1 && index2 > -1) {
            sequence.set(index1, candidateSegment.getStart());
            sequence.set(index2, segment.getEnd());
            if (index1 < index2) {
                List<Point> subList = new ArrayList<>(sequence.subList(index1+1, index2));
                sequence.subList(index1 + 1, index2).clear();
                sequence.addAll(index1 + 1, subList.reversed());
            }
            toBeDeleted.add(solutionSegments.indexOf(segment));
            toBeReplaced.add(new Segment(segment.getStart(), candidateSegment.getStart(), false));
        }
    }

    private static int handleDominated(List<Point> sequence, List<Segment> solutionSegments, Segment candidateSegment, Segment segment, int dominated, ArrayList<Integer> toBeDominated, ArrayList<Segment> toBeAdded) {
        dominated += 1;
        sequence.remove(segment.getStart());
        sequence.remove(segment.getEnd());
        toBeDominated.add(solutionSegments.indexOf(segment));
        if (SegmentUtils.euclideanDistance(candidateSegment.getStart(), segment.getStart()) < SegmentUtils.euclideanDistance(candidateSegment.getStart(), segment.getEnd())) {
            sequence.add(segment.getStart());
            sequence.add(segment.getEnd());
            toBeAdded.add(new Segment(candidateSegment.getStart(), segment.getStart(), false));
            toBeAdded.add(new Segment(segment.getStart(), segment.getEnd(), false));
            toBeAdded.add(new Segment(segment.getEnd(), candidateSegment.getEnd(), false));
        } else {
            sequence.add(segment.getEnd());
            sequence.add(segment.getStart());
            toBeAdded.add(new Segment(candidateSegment.getStart(), segment.getEnd(), false));
            toBeAdded.add(new Segment(segment.getEnd(), segment.getStart(), false));
            toBeAdded.add(new Segment(segment.getStart(), candidateSegment.getEnd(), false));
        }
        return dominated;
    }
}
