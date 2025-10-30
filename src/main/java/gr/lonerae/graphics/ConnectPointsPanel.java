package gr.lonerae.graphics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class ConnectPointsPanel extends JPanel {

    private final PointConnector connector;

    public ConnectPointsPanel(List<Point2D.Double> points, boolean circular) {
        connector = new PointConnector(points, circular, Color.BLACK, Color.RED, 5, 2.0f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        connector.draw((Graphics2D) g);
    }
}
