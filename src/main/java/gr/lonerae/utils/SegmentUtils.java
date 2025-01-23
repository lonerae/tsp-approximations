package gr.lonerae.utils;

import gr.lonerae.model.Path;
import gr.lonerae.model.Segment;
import gr.lonerae.model.Point;

import java.util.List;

public class SegmentUtils {
    private SegmentUtils() {}

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
