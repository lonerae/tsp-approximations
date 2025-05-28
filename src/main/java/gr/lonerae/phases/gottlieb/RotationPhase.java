package gr.lonerae.phases.gottlieb;

import gr.lonerae.model.Point;
import lombok.AllArgsConstructor;

import java.util.Comparator;
import java.util.List;

@AllArgsConstructor
public class RotationPhase {
    private Point start;
    private Point end;
    private List<Point> points;

    public void rotateOnEndpointAxis() {
        // Set start as the origin
        for (Point point : points) {
            point.setX(point.getX() - start.getX());
            point.setY(point.getY() - start.getY());
        }
        start.setX(0);
        end.setX(end.getX() - start.getX());
        end.setY(end.getY() - start.getY());
        double sin = - (end.getY() / Math.sqrt(Math.pow(end.getX(), 2) + Math.pow(end.getY(), 2)));
        double cos = Math.sqrt(1 - Math.pow(sin, 2));
        for (Point point : points) {
            if (point.equals(start)) continue;
            double oldX = point.getX();
            double oldY = point.getY();
            point.setX(Math.round(oldX * cos - oldY * sin));
            point.setY(Math.round(oldY * cos + oldX * sin));
        }
        points.sort(Comparator.comparingDouble(Point::getX));
    }
}
