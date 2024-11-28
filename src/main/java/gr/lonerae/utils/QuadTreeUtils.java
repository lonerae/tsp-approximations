package gr.lonerae.utils;

import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;

import java.util.ArrayList;

public class QuadTreeUtils {
    private QuadTreeUtils() {}

    public static ArrayList<Point> getQuadNodeCorners(QuadNode node) {
        ArrayList<Point> corners = new ArrayList<>();
        corners.add(node.getLowerLeftCorner());
        corners.add(PointUtils.shiftXY(node.getLowerLeftCorner(), node.getSide(), 0));
        corners.add(PointUtils.shiftXY(node.getLowerLeftCorner(), 0, node.getSide()));
        corners.add(PointUtils.shiftXY(node.getLowerLeftCorner(), node.getSide(), node.getSide()));
        return corners;
    }

    public static ArrayList<Point> getQuadNodePortals(QuadNode node) {
        ArrayList<Point> portals = new ArrayList<>();
        portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), node.getSide()/2, 0));
        portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), 0, node.getSide()/2));
        portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), node.getSide(), node.getSide()/2));
        portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), node.getSide()/2, node.getSide()));
        return portals;
    }

    public static boolean pointIsOnNodeSides(QuadNode node, Point point) {
        return point.getX() == node.getLowerLeftCorner().getX() ||
                point.getX() == node.getLowerLeftCorner().getX() + node.getSide() ||
                point.getY() == node.getLowerLeftCorner().getY() ||
                point.getY() == node.getLowerLeftCorner().getY() + node.getSide();
    }
}
