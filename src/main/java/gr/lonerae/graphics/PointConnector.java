package gr.lonerae.graphics;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

public record PointConnector(List<Point2D.Double> points, boolean circular, Color lineColor, Color pointColor,
                             int pointRadius, float lineThickness) {

    public void draw(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setColor(lineColor);
        graphics.setStroke(new BasicStroke(lineThickness));
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D.Double start = points.get(i);
            Point2D.Double end = points.get(i + 1);
            graphics.draw(new Line2D.Double(start.x, start.y, end.x, end.y));
        }
        if (circular)
            graphics.draw(new Line2D.Double(points.getLast().x, points.getLast().y, points.getFirst().x, points.getFirst().y));

        graphics.setColor(pointColor);
        for (Point2D.Double point : points) {
            double upperLeftX = point.x - pointRadius;
            double upperLeftY = point.y - pointRadius;
            double diameter = pointRadius * 2.0;
            graphics.fill(new Ellipse2D.Double(upperLeftX, upperLeftY, diameter, diameter));
        }
    }
}