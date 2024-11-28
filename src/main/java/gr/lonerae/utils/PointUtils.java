package gr.lonerae.utils;

import gr.lonerae.model.Point;

public class PointUtils {
    private PointUtils() {}

    public static Point shiftXY(Point original, double shiftX, double shiftY) {
        return new Point(original.getX() + shiftX, original.getY() + shiftY);
    }
}
