package gr.lonerae.phases.arora;

import gr.lonerae.model.Point;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.constants.GeneralConstants;
import lombok.Getter;
import java.util.*;

import static gr.lonerae.utils.constants.GeneralConstants.*;

@Getter
public class PreProcessingPhase {
    private List<Point> towns;
    private final double e;
    private final Map<GeneralConstants, Point> boundingBoxCorners = new EnumMap<>(GeneralConstants.class);
    private double boundingBoxSize = 0;
    private final Map<Point, ArrayList<Point>> clusters = new HashMap<>();

    public PreProcessingPhase(List<Point> towns, double e) {
        this.towns = towns;
        this.e = e;
    }

    public double[] preprocess() {
        double[] dimensions = this.createBoundingBox();
        this.moveBox();
        this.snapToGridAndScale();
        return dimensions;
    }

    private double[] createBoundingBox() {
        Point minX = towns.getFirst();
        Point maxX = towns.getFirst();
        Point minY = towns.getFirst();
        Point maxY = towns.getFirst();
        for (Point town : towns.subList(1, towns.size())) {
            if (town.getX() <= minX.getX()) minX = town;
            else if (town.getX() > maxX.getX()) maxX = town;
            if (town.getY() <= minY.getY()) minY = town;
            else if (town.getY() > maxY.getY()) maxY = town;
        }
        return calculateBoundingBoxDimensions(minX, minY, maxX, maxY);
    }

    private double[] calculateBoundingBoxDimensions(Point minX, Point minY, Point maxX, Point maxY) {
        double maxDistanceX = maxX.getX() - minX.getX();
        double maxDistanceY = maxY.getY() - minY.getY();
        setBoundingBoxCorners(minX, minY, maxX, maxY, maxDistanceX, maxDistanceY);
        double[] dimensions = new double[2];
        dimensions[0] = maxDistanceX;
        dimensions[1] = maxDistanceY;
        return dimensions;
    }

    private void setBoundingBoxCorners(Point minX, Point minY, Point maxX, Point maxY, double maxDistanceX, double maxDistanceY) {
        if (maxDistanceX >= maxDistanceY) {
            boundingBoxCorners.put(UPPER_LEFT, new Point(minX.getX(), minY.getY() + maxDistanceX));
            boundingBoxCorners.put(UPPER_RIGHT, new Point(maxX.getX(), minY.getY() + maxDistanceX));
            boundingBoxCorners.put(LOWER_RIGHT, new Point(maxX.getX(), minY.getY()));
            boundingBoxCorners.put(LOWER_LEFT, new Point(minX.getX(), minY.getY()));
            boundingBoxSize = maxDistanceX;
        } else {
            boundingBoxCorners.put(UPPER_LEFT, new Point(minX.getX(), maxY.getY()));
            boundingBoxCorners.put(UPPER_RIGHT, new Point(minX.getX() + maxDistanceY, maxY.getY()));
            boundingBoxCorners.put(LOWER_RIGHT, new Point(minX.getX() + maxDistanceY, minY.getY()));
            boundingBoxCorners.put(LOWER_LEFT, new Point(minX.getX(), minY.getY()));
            boundingBoxSize = maxDistanceY;
        }
    }

    private void moveBox() {
        // Move box to origin
        for (Point town : towns) {
            town.setOriginal(new Point(town.getX(), town.getY()));
            town.setX(CommonUtils.round(town.getX() - boundingBoxCorners.get(LOWER_LEFT).getX()));
            town.setY(CommonUtils.round(town.getY() - boundingBoxCorners.get(LOWER_LEFT).getY()));
        }
        for (Point boundingBoxCorner : boundingBoxCorners.values()) {
            boundingBoxCorner.setX(CommonUtils.round(boundingBoxCorner.getX() - boundingBoxCorners.get(LOWER_LEFT).getX()));
            boundingBoxCorner.setY(CommonUtils.round(boundingBoxCorner.getY() - boundingBoxCorners.get(LOWER_LEFT).getY()));
        }
    }

    private void snapToGridAndScale() {
        double gridUnit = e * boundingBoxSize / (2 * towns.size());
        horizontalSweep(gridUnit);
        verticalSweep(gridUnit);
        scaleBoundingBox();
        // Remove duplicates after perturbation
        for (Point point : towns) {
            if (clusters.containsKey(point)) {
                clusters.get(point).add(point.getOriginal());
            } else clusters.put(point, new ArrayList<>());
        }
        towns = new ArrayList<>(new HashSet<>(towns));
    }

    private void scaleBoundingBox() {
        for (Point town : towns) transformPointCoordinates(town);
        for (Point edge : boundingBoxCorners.values()) transformPointCoordinates(edge);
        boundingBoxSize = 8.0 * towns.size() / e;
    }

    private void transformPointCoordinates(Point point) {
        point.setX(CommonUtils.round(transformCoordinate(point.getX())));
        point.setY(CommonUtils.round(transformCoordinate(point.getY())));
    }

    private double transformCoordinate(double coordinate) {
        return coordinate * 8 * towns.size() / (e * boundingBoxSize);
    }

    private void horizontalSweep(double gridUnit) {
        towns.sort(Comparator.comparingDouble(Point::getX));
        int index = 0;
        double horizontalSweep = 0;
        while (horizontalSweep < boundingBoxSize && index < towns.size()) {
            Point currentTown = towns.get(index);
            if (currentTown.getX() >= horizontalSweep && currentTown.getX() < horizontalSweep + gridUnit) {
                if ((currentTown.getX() > horizontalSweep + (gridUnit / 2.0)) && (horizontalSweep + gridUnit <= boundingBoxSize)) {
                    currentTown.setX(horizontalSweep + gridUnit);
                } else {
                    currentTown.setX(horizontalSweep);
                }
                index++;
            } else horizontalSweep += gridUnit;
        }
    }

    private void verticalSweep(double gridUnit) {
        towns.sort(Comparator.comparingDouble(Point::getY));
        int index = 0;
        double verticalSweep = 0;
        while (verticalSweep < boundingBoxSize && index < towns.size()) {
            Point currentPoint = towns.get(index);
            if (currentPoint.getY() >= verticalSweep && currentPoint.getY() < verticalSweep + gridUnit) {
                if ((currentPoint.getY() > verticalSweep + (gridUnit / 2.0)) && (verticalSweep + gridUnit <= boundingBoxSize)) {
                    currentPoint.setY(verticalSweep + gridUnit);
                } else {
                    currentPoint.setY(verticalSweep);
                }
                index++;
            } else verticalSweep += gridUnit;
        }
    }
}
