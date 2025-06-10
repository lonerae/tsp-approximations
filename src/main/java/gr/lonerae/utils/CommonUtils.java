package gr.lonerae.utils;

import gr.lonerae.model.Point;

import java.util.ArrayList;
import java.util.List;

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
}
