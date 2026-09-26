package gr.lonerae.utils;

import gr.lonerae.model.Point;

import java.util.*;

public class CommonUtils {
    private CommonUtils(){}

    public static double round(double number) {
        return Math.round(number * 100.0) / 100.0;
    }

    public static List<Point> deepCopy(List<Point> points) {
        ArrayList<Point> deepCopy = new ArrayList<>();
        for (Point point : points) {
            deepCopy.add(new Point(point));
        }
        return deepCopy;
    }

    public static Point minimumPoint(List<Point> points) {
        Optional<Double> minX = points.stream().min(Comparator.comparingDouble(Point::getX)).map(Point::getX);
        return minX.flatMap(xValue -> points.stream().filter(p -> p.getX() == xValue).min(Comparator.comparingDouble(Point::getY))).orElse(null);
    }

    public static Point maximumPoint(List<Point> points) {
        Optional<Double> maxX = points.stream().max(Comparator.comparingDouble(Point::getX)).map(Point::getX);
        return maxX.flatMap(xValue -> points.stream().filter(p -> p.getX() == xValue).max(Comparator.comparingDouble(Point::getY))).orElse(null);
    }
}
