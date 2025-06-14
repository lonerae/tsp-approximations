package gr.lonerae.utils;

import gr.lonerae.model.Path;
import gr.lonerae.model.Segment;
import gr.lonerae.model.Point;

import java.util.List;

import static java.util.Objects.isNull;

public class SegmentUtils {
    private SegmentUtils() {}

    private static boolean allowEdgeIntersection = true;

    public static boolean intersect(Segment s1, Segment s2, boolean withEdge) {
        Double l1 = calculateSlope(s1);
        Double l2 = calculateSlope(s2);
        if ((!isNull(l1) && !isNull(l2) && (Double.compare(l1, l2) == 0 || l1.doubleValue() == l2.doubleValue())) || (isNull(l1) && isNull(l2))) return false;
        Point intersection = null;
        intersection = findIntersection(s1, s2, l1, l2);
        if (!withEdge) return pointInSegment(s1, intersection) && pointInSegment(s2, intersection);
        else {
            if (pointInSegment(s1, intersection) && pointInSegment(s2, intersection)) {
                allowEdgeIntersection = true;
                return true;
            }
            else if (
                    ((pointInSegment(s1, intersection) && (intersection.equals(s2.getStart()) || intersection.equals(s2.getEnd()))) ||
                    (pointInSegment(s2, intersection) && (intersection.equals(s1.getStart()) || intersection.equals(s1.getEnd())))) && allowEdgeIntersection
            ) {
                allowEdgeIntersection = false;
                return true;
            }
            allowEdgeIntersection = true;
            return false;
        }
    }

    private static Point findIntersection(Segment s1, Segment s2, Double l1, Double l2) {
        Point intersection;
        if (!isNull(l1) && !isNull(l2)) {
            double b1 = s1.getStart().getY() - l1 * s1.getStart().getX();
            double b2 = s2.getStart().getY() - l2 * s2.getStart().getX();
            double x = (b2-b1)/(l1 - l2);
            intersection = new Point(x, l1 *x+b1);
        } else if (isNull(l1)) {
            double b2 = s2.getStart().getY() - l2 * s2.getStart().getX();
            intersection = new Point(s1.getStart().getX(), l2 * s1.getStart().getX()+b2);
        } else {
            double b1 = s1.getStart().getY() - l1 * s1.getStart().getX();
            intersection = new Point(s2.getStart().getX(), l1 * s2.getStart().getX()+b1);
        }
        return intersection;
    }

    private static Double calculateSlope(Segment segment) {
        Double slope = null;
        if (segment.getEnd().getX() != segment.getStart().getX()) {
            slope = (segment.getEnd().getY() - segment.getStart().getY()) / (segment.getEnd().getX() - segment.getStart().getX());
        }
        return slope;
    }

    public static boolean dominate(Segment parent, Segment child) {
        Double l1 = calculateSlope(parent);
        Double l2 = calculateSlope(child);
        if ((!isNull(l1) && !isNull(l2) && !l1.equals(l2)) || (isNull(l1) && !isNull(l2)) || (isNull(l2) && !isNull(l1))) return false;
        return segmentInSegment(parent, child);
    }

    public static boolean onLine(Segment s, Point p) {
        Double l = calculateSlope(s);

        if (isNull(l)) return Double.compare(p.getX(), s.getStart().getX()) == 0;
        else {
            double b = s.getStart().getY() - l * s.getStart().getX();
            return Double.compare(p.getY(), l * p.getX() + b) == 0;
        }
    }

    public static boolean segmentInSegment(Segment parent, Segment child) {
        return
                euclideanDistance(parent.getStart(), child.getStart()) + euclideanDistance(child.getStart(), child.getEnd()) + euclideanDistance(child.getEnd(), parent.getEnd()) == euclideanDistance(parent.getStart(), parent.getEnd()) ||
                euclideanDistance(parent.getStart(), child.getEnd()) + euclideanDistance(child.getStart(), child.getEnd()) + euclideanDistance(child.getStart(), parent.getEnd()) == euclideanDistance(parent.getStart(), parent.getEnd());
    }

    public static boolean pointInSegment(Segment segment, Point point) {
        return !point.equals(segment.getStart()) && !point.equals(segment.getEnd()) &&
                point.getX() >= Math.min(segment.getStart().getX(), segment.getEnd().getX()) && point.getX() <= Math.max(segment.getStart().getX(), segment.getEnd().getX()) &&
                point.getY() >= Math.min(segment.getStart().getY(), segment.getEnd().getY()) && point.getY() <= Math.max(segment.getStart().getY(), segment.getEnd().getY());
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
        return euclideanDistance(segment.getStart(), segment.getEnd());
    }

    public static double getPathLengthThroughPoint(Segment segment, Point point) {
        return euclideanDistance(segment.getStart(), point) + euclideanDistance(point, segment.getEnd());
    }

    public static double euclideanDistance(Point start, Point end) {
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
