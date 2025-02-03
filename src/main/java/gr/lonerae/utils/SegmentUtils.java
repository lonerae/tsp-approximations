package gr.lonerae.utils;

import gr.lonerae.model.Path;
import gr.lonerae.model.Segment;
import gr.lonerae.model.Point;

import java.util.List;

import static java.util.Objects.isNull;

public class SegmentUtils {
    private SegmentUtils() {}

    public static boolean segmentIntersect(Segment s1, Segment s2) {
        Double l1 = null;
        if (s1.getEnd().getX() != s1.getStart().getX()) {
            l1 = (s1.getEnd().getY() - s1.getStart().getY()) / (s1.getEnd().getX() - s1.getStart().getX());
        }
        Double l2 = null;
        if (s2.getEnd().getX() != s2.getStart().getX()) {
            l2 = (s2.getEnd().getY() - s2.getStart().getY()) / (s2.getEnd().getX() - s2.getStart().getX());
        }
        if ((!isNull(l1) && !isNull(l2) && l1.equals(l2)) || (isNull(l1) && isNull(l2))) return false;
        Point intersection = null;
        if (!isNull(l1) && !isNull(l2)) {
            double b1 = s1.getStart().getY() - l1 * s1.getStart().getX();
            double b2 = s2.getStart().getY() - l2 * s2.getStart().getX();
            double x = (b2-b1)/(l1-l2);
            intersection = new Point(x, l1*x+b1);
        } else if (isNull(l1)) {
            double b2 = s2.getStart().getY() - l2 * s2.getStart().getX();
            intersection = new Point(s1.getStart().getX(), l2*s1.getStart().getX()+b2);
        } else if (isNull(l2)) {
            double b1 = s1.getStart().getY() - l1 * s1.getStart().getX();
            intersection = new Point(s2.getStart().getX(), l1*s2.getStart().getX()+b1);
        }
        return inSegment(s1, intersection) && inSegment(s2, intersection);
    }
    public static boolean segmentOverlap(Segment s1, Segment s2) {
        Double l1 = null;
        if (s1.getEnd().getX() != s1.getStart().getX()) {
            l1 = (s1.getEnd().getY() - s1.getStart().getY()) / (s1.getEnd().getX() - s1.getStart().getX());
        }
        Double l2 = null;
        if (s2.getEnd().getX() != s2.getStart().getX()) {
            l2 = (s2.getEnd().getY() - s2.getStart().getY()) / (s2.getEnd().getX() - s2.getStart().getX());
        }
        if ((!isNull(l1) && !isNull(l2) && l1.equals(l2)) || (isNull(l1) && !isNull(l2)) || (isNull(l2) && !isNull(l1))) return false;
        Segment small = getEuclideanDistance(s1.getStart(), s1.getEnd()) < getEuclideanDistance(s2.getStart(), s2.getEnd()) ?
                s1 : s2;
        Segment large = small.equals(s1) ? s2 : s1;
        return pointWithinSegment(small.getStart(), large) || pointWithinSegment(small.getEnd(), large);
    }

    private static boolean pointWithinSegment(Point point, Segment large) {
        return
            point.getX() >= Math.min(large.getStart().getX(), large.getEnd().getX()) && point.getX() <= Math.max(large.getStart().getX(), large.getEnd().getX()) &&
            point.getY() >= Math.min(large.getStart().getY(), large.getEnd().getY()) && point.getY() <= Math.max(large.getStart().getY(), large.getEnd().getY()) &&
            !point.equals(large.getStart()) && !point.equals(large.getEnd());
    }

    public static boolean onLine(Segment s, Point p) {
        Double l = null;
        if (s.getEnd().getX() != s.getStart().getX()) {
            l = (s.getEnd().getY() - s.getStart().getY()) / (s.getEnd().getX() - s.getStart().getX());
        }

        if (isNull(l)) return Double.compare(p.getX(), s.getStart().getX()) == 0;
        else {
            double b = s.getStart().getY() - l * s.getStart().getX();
            return Double.compare(p.getY(), l * p.getX() + b) == 0;
        }
    }

    public static boolean inSegment(Segment segment, Point point) {
        return !point.equals(segment.getStart()) && !point.equals(segment.getEnd()) &&
               getEuclideanDistance(segment.getStart(), point) + getEuclideanDistance(point, segment.getEnd()) == getPathLength(segment);
    }

    public static Point contains(Segment segment, Point point) {
        if (segment.getStart().equals(point)) return segment.getEnd();
        else if (segment.getEnd().equals(point)) return segment.getStart();
        else return null;
    }

    public static boolean commonEdges(Segment first, Segment second) {
        return (first.getStart().equals(second.getStart()) && first.getEnd().equals(second.getEnd())) ||
                (first.getStart().equals(second.getEnd()) && first.getEnd().equals(second.getStart()));
    }

    public static double getPathLength(Segment segment) {
        return getEuclideanDistance(segment.getStart(), segment.getEnd());
    }

    public static double getPathLengthThroughPoint(Segment segment, Point point) {
        return getEuclideanDistance(segment.getStart(), point) + getEuclideanDistance(point, segment.getEnd());
    }

    public static double getEuclideanDistance(Point start, Point end) {
        return Math.sqrt(Math.pow(end.getX() - start.getX(), 2) + Math.pow(end.getY() - start.getY(), 2));
    }

    public static Path toPath(Segment s) {
        Path path = new Path();
        path.setEntrance(s.getStart());
        path.setExit(s.getEnd());
        path.setSegments(List.of(s));
        path.setLength(s.getLength());
        return path;
    }
}
