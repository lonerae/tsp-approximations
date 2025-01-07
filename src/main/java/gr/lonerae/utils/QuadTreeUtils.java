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

    public static ArrayList<Point> getQuadNodePortals(QuadNode node, int numOfPortals) {
        ArrayList<Point> portals = new ArrayList<>();
        for (int i = 0; i < numOfPortals; i++) {
            portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), ((i+1) * node.getSide()) / (numOfPortals + 1), 0));
            portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), 0, ((i+1) * node.getSide()) / (numOfPortals + 1)));
            portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), node.getSide(), ((i+1) * node.getSide()) / (numOfPortals + 1)));
            portals.add(PointUtils.shiftXY(node.getLowerLeftCorner(), ((i+1) * node.getSide()) / (numOfPortals + 1), node.getSide()));
        }
        return portals;
    }

    public static boolean pointIsOnNodeSides(QuadNode node, Point point) {
        return point.getX() == node.getLowerLeftCorner().getX() ||
                point.getX() == node.getLowerLeftCorner().getX() + node.getSide() ||
                point.getY() == node.getLowerLeftCorner().getY() ||
                point.getY() == node.getLowerLeftCorner().getY() + node.getSide();
    }
}
