package gr.lonerae.graphics;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.List;

public record PointConnectorMulti(List<List<Point2D.Double>> points, Color pointColor,
                             int pointRadius, float lineThickness) {

    public void draw(Graphics2D graphics) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        graphics.setStroke(new BasicStroke(lineThickness));
        for (List<Point2D.Double> sequence : points) {
            graphics.setColor(new Color((int)(Math.random() * 0x1000000)));
            for (int i = 0; i < sequence.size() - 1; i++) {
                Point2D.Double start = sequence.get(i);
                Point2D.Double end = sequence.get(i + 1);
                graphics.draw(new Line2D.Double(start.x, start.y, end.x, end.y));
            }

            graphics.setColor(pointColor);
            for (Point2D.Double point : sequence) {
                double upperLeftX = point.x - pointRadius;
                double upperLeftY = point.y - pointRadius;
                double diameter = pointRadius * 2.0;
                graphics.fill(new Ellipse2D.Double(upperLeftX, upperLeftY, diameter, diameter));
            }
        }
    }
}
