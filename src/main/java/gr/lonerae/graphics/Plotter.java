package gr.lonerae.graphics;

import gr.lonerae.model.Point;
import lombok.RequiredArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;

public class Plotter {
    private static final int WINDOW_SIZE = 800;
    private static final int WINDOW_PADDING_X = 30;
    private static final int WINDOW_PADDING_Y = 100;

    private static final Color LINE_COLOR = Color.BLACK;
    private static final Color TOWN_COLOR = Color.RED;
    private static final Color ENDPOINT_COLOR = Color.BLUE;

    private Plotter() {}

    @RequiredArgsConstructor
    public static class PlotSolution extends JFrame {
        private final transient ArrayList<Point> points;
        private final double[] dimensions;
        private final boolean circular;

        @Override
        public void paint(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics;

            double radius = Math.min(dimensions[0], dimensions[1]) / 240;
            setWindowParameters(graphics2D, (float) radius);

            double headerHeight = dimensions[1] / 20;
            for (int i = 0; i < points.size() - 1; i++) {
                drawEdge(graphics2D, points.get(i), points.get(i + 1), headerHeight);
                drawTown(points.get(i), radius, headerHeight, graphics2D, i == 0);
                drawTown(points.get(i + 1), radius, headerHeight, graphics2D, i == 0);
            }
            drawTown(points.getLast(), radius, headerHeight, graphics2D, !circular);

            if (circular) {
                drawEdge(graphics2D, points.getLast(), points.getFirst(), headerHeight);
            }
        }

        private void setWindowParameters(Graphics2D graphics2D, float radius) {
            graphics2D.setStroke(new BasicStroke(radius));
            setSize(WINDOW_SIZE + WINDOW_PADDING_X, WINDOW_SIZE + WINDOW_PADDING_Y);
            setLocationRelativeTo(null);
            setResizable(false);
            graphics2D.scale(WINDOW_SIZE / dimensions[0], WINDOW_SIZE / dimensions[1]);
        }

        private void drawEdge(Graphics2D graphics2D, Point start, Point end, double headerHeight) {
            graphics2D.setColor(LINE_COLOR);
            graphics2D.draw(new Line2D.Double(start.getX(), start.getY() + headerHeight,
                    end.getX(), end.getY() + headerHeight));
        }

        private void drawTown(Point town, double radius, double headerHeight, Graphics2D graphics2D, boolean isEndpoint) {
            if (isEndpoint) graphics2D.setColor(ENDPOINT_COLOR);
            else graphics2D.setColor(TOWN_COLOR);
            Ellipse2D.Double townGraphic = new Ellipse2D.Double(town.getX() - radius / 2, town.getY() + headerHeight - radius / 2, 3 * radius, 3 * radius);
            graphics2D.fill(townGraphic);
        }
    }
}