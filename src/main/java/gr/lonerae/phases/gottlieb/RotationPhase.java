package gr.lonerae.phases.gottlieb;

import gr.lonerae.model.Point;
import lombok.AllArgsConstructor;

import java.util.List;

@AllArgsConstructor
public class RotationPhase {
    private Point start;
    private Point end;
    private List<Point> points;

    public void rotateOnEndpointAxis() {
        double xDisplacement = start.getX();
        double yDisplacement = start.getY();
        // Set start as the origin
        for (Point point : points) {
            point.setOriginal(new Point(point));
            point.setX(point.getX() - xDisplacement);
            point.setY(point.getY() - yDisplacement);
        }
        // Calculate angle
        int cosSign = end.getX() >= 0 ? 1 : -1;
        int sinSign = end.getY() <= 0 ? 1 : -1;
        double sin = sinSign * (Math.abs(end.getY()) / Math.sqrt(Math.pow(end.getX(), 2) + Math.pow(end.getY(), 2)));
        double cos = cosSign * Math.sqrt(1 - Math.pow(sin, 2));
        // Rotate points and return to original position
        for (Point point : points) {
            if (point.equals(start)) {
                point.setX(point.getX() + xDisplacement);
                continue;
            }
            double oldX = point.getX();
            double oldY = point.getY();
            point.setX(Math.round((oldX * cos - oldY * sin) * Math.pow(10,13)) / Math.pow(10,13) + xDisplacement);
            point.setY(Math.round((oldY * cos + oldX * sin) * Math.pow(10,13)) / Math.pow(10,13));
        }
    }
}
