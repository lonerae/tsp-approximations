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
        // Move start to x-axis
        for (Point point : points) {
            point.setX(point.getX() - start.getX());
        }
        start.setX(0);
        end.setX(end.getX() - start.getX());
        Point originalVector = new Point(end.getX() - start.getX(), end.getY() - start.getY());
        Point targetVector = new Point(start.getX() + Math.sqrt(Math.pow(end.getX() - start.getX(), 2) + Math.pow(end.getY() - start.getY(), 2)), 0);
        double dotProduct = originalVector.getX() * targetVector.getX() + originalVector.getY() * targetVector.getY();
        double denominator = (Math.sqrt(Math.pow(originalVector.getX(), 2) + Math.pow(originalVector.getY(), 2)) * (Math.sqrt(Math.pow(targetVector.getX(), 2) + Math.pow(targetVector.getY(), 2))) );
        double cos =  dotProduct == 0 ? 0 : dotProduct / denominator;
        double sin = Math.sqrt(1 - Math.pow(cos, 2));
        for (Point point : points) {
            if (point.equals(start)) continue;
            point.setX(point.getX() * cos - point.getY() * sin);
            point.setY(point.getY() * cos + point.getX() * sin);
        }
        points.sort(Comparator.comparingDouble(Point::getX));
    }
}
