package gr.lonerae.graphics;

import gr.lonerae.model.Point;
import lombok.AllArgsConstructor;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

public class Plotter {
    private static final int WINDOW_SIZE = 800;
    private static final int WINDOW_PADDING_X = 30;
    private static final int WINDOW_PADDING_Y = 100;

    private Plotter() {}

    @AllArgsConstructor
    public static class PlotSolution extends JFrame {
        private transient ArrayList<Point> points;
        private double[] dimensions;

        @Override
        public void paint(Graphics l) {
            Graphics2D g = (Graphics2D) l;
            setSize(WINDOW_SIZE + WINDOW_PADDING_X, WINDOW_SIZE + WINDOW_PADDING_Y);
            double headerHeight = dimensions[1]/20;
            g.scale(WINDOW_SIZE/dimensions[0],WINDOW_SIZE/dimensions[1]);
            double r = Math.min(dimensions[0], dimensions[1]) / 80;
            setLocationRelativeTo(null);
            setResizable(false);

            for (int i = 0; i < points.size() - 1; i++) {
                g.setColor(Color.BLACK);
                g.draw(new Line2D.Double(points.get(i).getX(), points.get(i).getY() + headerHeight,
                        points.get(i+1).getX(), points.get(i+1).getY() + headerHeight));
                if (i == 0) g.setColor(Color.BLUE);
                else g.setColor(Color.RED);
                Ellipse2D.Double town = new Ellipse2D.Double(points.get(i).getX() - r/2, points.get(i).getY() + headerHeight - r/2, r, r);
                g.fill(town);
                town = new Ellipse2D.Double(points.get(i+1).getX() - r/2, points.get(i+1).getY() + headerHeight - r/2, r, r);
                g.fill(town);
            }
//            g.setColor(Color.BLACK);
//            g.draw(new Line2D.Double(points.getLast().getX(), points.getLast().getY() + headerHeight,
//                    points.getFirst().getX(), points.getFirst().getY() + headerHeight));


        }
    }

    @AllArgsConstructor
    public static class PlotOptimal extends JFrame {
        private transient List<Point> original;
        private List<Integer> indexes;
        private double[] dimensions;

        @Override
        public void paint(Graphics l) {
            Graphics2D g = (Graphics2D) l;
            setSize(WINDOW_SIZE + WINDOW_PADDING_X, WINDOW_SIZE + WINDOW_PADDING_Y);
            double headerHeight = dimensions[1]/20;
            g.scale(WINDOW_SIZE/dimensions[0],WINDOW_SIZE/dimensions[1]);
            double r = Math.min(dimensions[0], dimensions[1]) / 80;
            setLocationRelativeTo(null);
            setResizable(false);

            for (int index = 0; index < indexes.size() - 1; index++) {
                g.setColor(Color.BLACK);
                g.draw(new Line2D.Double(original.get(indexes.get(index)).getX(), original.get(indexes.get(index)).getY() + headerHeight,
                        original.get(indexes.get(index+1)).getX(), original.get(indexes.get(index+1)).getY() + headerHeight));
                g.setColor(Color.RED);
                Ellipse2D.Double town = new Ellipse2D.Double(original.get(index).getX() - r/2, original.get(index).getY() + headerHeight - r/2, r, r);
                g.fill(town);
                town = new Ellipse2D.Double(original.get(index+1).getX() - r/2, original.get(index+1).getY() + headerHeight - r/2, r, r);
                g.fill(town);
            }
            g.setColor(Color.BLACK);
            g.draw(new Line2D.Double(original.get(indexes.getLast()).getX(), original.get(indexes.getLast()).getY() + headerHeight,
                    original.get(indexes.getFirst()).getX(), original.get(indexes.getFirst()).getY() + headerHeight));

        }
    }
}