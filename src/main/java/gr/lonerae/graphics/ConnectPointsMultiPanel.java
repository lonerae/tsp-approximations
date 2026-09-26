package gr.lonerae.graphics;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

public class ConnectPointsMultiPanel extends JPanel {

    private final PointConnectorMulti connector;

    public ConnectPointsMultiPanel(List<List<Point2D.Double>> points) {
        connector = new PointConnectorMulti(points, Color.BLACK, 5, 2.0f);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        connector.draw((Graphics2D) g);
    }
}
