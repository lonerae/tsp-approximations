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

    public static double[] findDimensions(List<Point> points) {
        double[] dimensions = new double[2];
        double minX = points.getFirst().getX();
        double maxX = points.getFirst().getX();
        double minY = points.getFirst().getY();
        double maxY = points.getFirst().getY();
        for (Point point : points) {
            if (minX > point.getX()) minX = point.getX();
            if (maxX < point.getX()) maxX = point.getX();
            if (minY > point.getY()) minY = point.getY();
            if (maxY < point.getY()) maxY = point.getY();
        }
        dimensions[0] = maxX;
        dimensions[1] = maxY;
        return dimensions;
    }
}
