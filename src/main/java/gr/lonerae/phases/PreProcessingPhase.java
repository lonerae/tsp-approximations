package gr.lonerae.phases;

import gr.lonerae.model.Point;
import gr.lonerae.utils.CommonUtils;
import lombok.Getter;

import java.util.*;

@Getter
public class PreProcessingPhase {
    private List<Point> points;
    private final double e;
    private final int size;
    private final Map<Integer, Point> square = new HashMap<>();
    private double squareSide = 0;
    private final Map<Point, ArrayList<Point>> clusters = new HashMap<>();

    public PreProcessingPhase(List<Point> points, double e) {
        this.points = points;
        this.size = points.size();
        this.e = e;
    }

    public double[] preprocess() {
        double[] dimensions = this.createBoundingBox();
        this.moveBox();
        this.snapToGridAndScale();
        return dimensions;
    }

    private double[] createBoundingBox() {
        Point minX = points.getFirst();
        Point maxX = points.getFirst();
        Point minY = points.getFirst();
        Point maxY = points.getFirst();
        for (Point p : points.subList(1, size)) {
            if (p.getX() <= minX.getX()) minX = p;
            else if (p.getX() > maxX.getX()) maxX = p;
            if (p.getY() <= minY.getY()) minY = p;
            else if (p.getY() > maxY.getY()) maxY = p;
        }
        double maxDistanceX = maxX.getX() - minX.getX();
        double maxDistanceY = maxY.getY() - minY.getY();
        if (maxDistanceX >= maxDistanceY) {
            square.put(0, new Point(minX.getX(), minY.getY() + maxDistanceX));
            square.put(1, new Point(maxX.getX(), minY.getY() + maxDistanceX));
            square.put(2, new Point(maxX.getX(), minY.getY()));
            square.put(3, new Point(minX.getX(), minY.getY()));
            squareSide = maxDistanceX;
        } else {
            square.put(0, new Point(minX.getX(), maxY.getY()));
            square.put(1, new Point(minX.getX() + maxDistanceY, maxY.getY()));
            square.put(2, new Point(minX.getX() + maxDistanceY, minY.getY()));
            square.put(3, new Point(minX.getX(), minY.getY()));
            squareSide = maxDistanceY;
        }
        double[] dimensions = new double[2];
        dimensions[0] = maxDistanceX;
        dimensions[1] = maxDistanceY;
        return dimensions;
    }

    private void moveBox() {
        // Move box to origin
        for (Point p : points) {
            p.setOriginal(new Point(p.getX(), p.getY()));
            p.setX(CommonUtils.round(p.getX() - square.get(3).getX()));
            p.setY(CommonUtils.round(p.getY() - square.get(3).getY()));
        }
        for (Point edge : square.values()) {
            edge.setX(CommonUtils.round(edge.getX() - square.get(3).getX()));
            edge.setY(CommonUtils.round(edge.getY() - square.get(3).getY()));
        }
    }

    private void snapToGridAndScale() {
        double gridUnit = e * squareSide / (2 * size);
        horizontalSweep(gridUnit);
        verticalSweep(gridUnit);
        // Scale box
        for (Point p : points) {
            p.setX(CommonUtils.round(p.getX() * 8 * size / (e * squareSide)));
            p.setY(CommonUtils.round(p.getY() * 8 * size / (e * squareSide)));
        }
        for (Point edge : square.values()) {
            edge.setX(CommonUtils.round(edge.getX() * 8 * size / (e * squareSide)));
            edge.setY(CommonUtils.round(edge.getY() * 8 * size / (e * squareSide)));
        }
        squareSide = 8.0 * size / e;
        // Remove duplicates after perturbation
        for (Point point : points) {
            if (clusters.containsKey(point)) {
                clusters.get(point).add(point.getOriginal());
            } else clusters.put(point, new ArrayList<>());
        }
        points = new ArrayList<>(new HashSet<>(points));
    }

    private void horizontalSweep(double gridUnit) {
        points.sort(Comparator.comparingDouble(Point::getX));
        int index = 0;
        double horizontalSweep = 0;
        while (horizontalSweep < squareSide && index < size) {
            Point currentPoint = points.get(index);
            if (currentPoint.getX() >= horizontalSweep && currentPoint.getX() < horizontalSweep + gridUnit) {
                if ((currentPoint.getX() > horizontalSweep + (gridUnit / 2.0)) && (horizontalSweep + gridUnit <= squareSide)) {
                    currentPoint.setX(horizontalSweep + gridUnit);
                } else {
                    currentPoint.setX(horizontalSweep);
                }
                index++;
            } else horizontalSweep += gridUnit;
        }
    }

    private void verticalSweep(double gridUnit) {
        points.sort(Comparator.comparingDouble(Point::getY));
        int index = 0;
        double verticalSweep = 0;
        while (verticalSweep < squareSide && index < size) {
            Point currentPoint = points.get(index);
            if (currentPoint.getY() >= verticalSweep && currentPoint.getY() < verticalSweep + gridUnit) {
                if ((currentPoint.getY() > verticalSweep + (gridUnit / 2.0)) && (verticalSweep + gridUnit <= squareSide)) {
                    currentPoint.setY(verticalSweep + gridUnit);
                } else {
                    currentPoint.setY(verticalSweep);
                }
                index++;
            } else verticalSweep += gridUnit;
        }
    }
}
