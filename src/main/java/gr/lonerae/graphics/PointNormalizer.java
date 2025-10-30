package gr.lonerae.graphics;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class PointNormalizer {

    private PointNormalizer(){}

    public static List<Point2D.Double> normalize(List<Point2D.Double> points, double width, double height, double margin) {
        if (points.isEmpty()) return points;

        double minX = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double minY = Double.MAX_VALUE;
        double maxY = Double.MIN_VALUE;

        for (Point2D.Double point : points) {
            if (point.x < minX) minX = point.x;
            if (point.x > maxX) maxX = point.x;
            if (point.y < minY) minY = point.y;
            if (point.y > maxY) maxY = point.y;
        }

        double dataWidth = maxX - minX;
        double dataHeight = maxY - minY;

        double availableWidth = width - 2 * margin;
        double availableHeight = height - 2 * margin;
        double scaleX = availableWidth / dataWidth;
        double scaleY = availableHeight / dataHeight;

        List<Point2D.Double> normalized = new ArrayList<>();
        for (Point2D.Double point : points) {
            double normalizedX = (point.x - minX) * scaleX + margin;
            double normalizedY = (point.y - minY) * scaleY + margin;
            normalized.add(new Point2D.Double(normalizedX, normalizedY));
        }

        return normalized;
    }
}
