package gr.lonerae.utils;

import gr.lonerae.model.Point;

import java.util.List;

public class PointUtils {
    private PointUtils() {}

    public static Point shiftXY(Point original, double shiftX, double shiftY) {
        return new Point(original.getX() + shiftX, original.getY() + shiftY);
    }

    public static int compare(Point p1, Point p2) {
        int result = Double.compare(p1.getX(), p2.getX());
        if (result==0) result = Double.compare(p1.getY(), p2.getY());
        return result;
    }

    public static int getTotalPlurality(List<Point> points) {
        return points.stream().mapToInt(Point::getPlurality).sum();
    }
}
