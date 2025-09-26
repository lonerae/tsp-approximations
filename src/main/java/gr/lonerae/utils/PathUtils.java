package gr.lonerae.utils;

import gr.lonerae.model.Path;
import gr.lonerae.model.Point;

public class PathUtils {
    private PathUtils() {}

    public static boolean hasEndpoint(Path path, Point point) {
        return path.getEntrance().equals(point) || path.getExit().equals(point);
    }
}
