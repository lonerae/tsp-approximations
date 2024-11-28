package gr.lonerae.phases;

import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;
import gr.lonerae.utils.PointUtils;
import gr.lonerae.utils.QuadTreeUtils;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Getter
public class QuadtreePhase {
    private final QuadNode root;

    private final HashSet<Point> corners = new HashSet<>();
    private final HashSet<Point> portals = new HashSet<>();
    private final ArrayList<QuadNode> quadNodes = new ArrayList<>();

    public QuadtreePhase(double squareSide, List<Point> points) {
        root = new QuadNode();
        root.setLowerLeftCorner(new Point(0,0));
        root.setSide(squareSide);
        root.setPoints(points);
    }

    public void assignPortals() {
        divideQuadLeaf(root);
        assignPortalsToQuadNodes();
    }

    private void divideQuadLeaf(QuadNode node) {
        node.setUpperLeft(new QuadNode());
        node.getUpperLeft().setParent(node);
        node.getUpperLeft().setLowerLeftCorner(new Point(node.getLowerLeftCorner().getX(), node.getLowerLeftCorner().getY() + node.getSide() / 2));
        node.getUpperLeft().setSide(node.getSide() / 2);

        node.setUpperRight(new QuadNode());
        node.getUpperRight().setParent(node);
        node.getUpperRight().setLowerLeftCorner(new Point(node.getLowerLeftCorner().getX() + node.getSide() / 2, node.getLowerLeftCorner().getY() + node.getSide() / 2));
        node.getUpperRight().setSide(node.getSide() / 2);

        node.setLowerRight(new QuadNode());
        node.getLowerRight().setParent(node);
        node.getLowerRight().setLowerLeftCorner(new Point(node.getLowerLeftCorner().getX() + node.getSide() / 2, node.getLowerLeftCorner().getY()));
        node.getLowerRight().setSide(node.getSide() / 2);

        node.setLowerLeft(new QuadNode());
        node.getLowerLeft().setParent(node);
        node.getLowerLeft().setLowerLeftCorner(new Point(node.getLowerLeftCorner().getX(), node.getLowerLeftCorner().getY()));
        node.getLowerLeft().setSide(node.getSide() / 2);

        for (Point p : node.getPoints()) {
            boolean left = p.getX() < node.getLowerLeftCorner().getX() + node.getSide() / 2;
            boolean up = p.getY() > node.getLowerLeftCorner().getY() + node.getSide() / 2;
            boolean leftLimit = p.getX() == node.getLowerLeftCorner().getX() + node.getSide() / 2;
            boolean upLimit = p.getY() == node.getLowerLeftCorner().getY() + node.getSide() / 2;

            if (leftLimit) {
                if (up) node.getUpperLeft().getPoints().add(p);
                else node.getLowerRight().getPoints().add(p);
            }
            else if (upLimit) {
                if (left) node.getUpperLeft().getPoints().add(p);
                else node.getUpperRight().getPoints().add(p);
            }
            else if (left && up) node.getUpperLeft().getPoints().add(p);
            else if (!left && up) node.getUpperRight().getPoints().add(p);
            else if (!left) node.getLowerRight().getPoints().add(p);
            else node.getLowerLeft().getPoints().add(p);
        }

        if (node.getUpperLeft().getPoints().size() > 1) divideQuadLeaf(node.getUpperLeft());
        else updateCornersAndPortals(node.getUpperLeft());
        if (node.getUpperRight().getPoints().size() > 1) divideQuadLeaf(node.getUpperRight());
        else updateCornersAndPortals(node.getUpperRight());
        if (node.getLowerRight().getPoints().size() > 1) divideQuadLeaf(node.getLowerRight());
        else updateCornersAndPortals(node.getLowerRight());
        if (node.getLowerLeft().getPoints().size() > 1) divideQuadLeaf(node.getLowerLeft());
        else updateCornersAndPortals(node.getLowerLeft());
    }

    private void updateCornersAndPortals(QuadNode node) {
        // Update nodes
        quadNodes.add(node);
        // Update corners
        ArrayList<Point> nodeCorners = QuadTreeUtils.getQuadNodeCorners(node);
        corners.addAll(nodeCorners);
        // Update portals
        ArrayList<Point> nodePortals = QuadTreeUtils.getQuadNodePortals(node);
        for (Point portal : nodePortals) {
            if (portal.getX() != 0 && portal.getY() != 0 && portal.getX() != root.getSide() && portal.getY() != root.getSide()) {
                portals.add(portal);
            }
        }
        for (Point corner : corners) portals.remove(corner);
    }

    private void assignPortalsToQuadNodes() {
        for (QuadNode node : quadNodes) {
            ArrayList<Point> nodePortals = QuadTreeUtils.getQuadNodePortals(node);
            for (Point possiblePortal : nodePortals) {
                divide(possiblePortal, node, 0);
            }
        }
    }

    private void divide(Point portal, QuadNode node, int depth) {
        if (
                portal.getX() == 0 ||
                        portal.getY() == 0 ||
                        portal.getX() == root.getSide() ||
                        portal.getY() == root.getSide()
        ) return;
        if (portals.contains(portal)) node.getPortals().add(portal);
        else {
            boolean vertical = (portal.getX() == node.getLowerLeftCorner().getX() || portal.getX() == node.getLowerLeftCorner().getX() + node.getSide());
            Point first;
            Point second;
            if (vertical) {
                first = PointUtils.shiftXY(portal, 0, node.getSide() / Math.pow(2, depth + 2.0));
                second = PointUtils.shiftXY(portal, 0, -node.getSide() / Math.pow(2, depth + 2.0));
            } else {
                first = PointUtils.shiftXY(portal, node.getSide() / Math.pow(2, depth + 2.0), 0);
                second = PointUtils.shiftXY(portal, -node.getSide() / Math.pow(2, depth + 2.0), 0);
            }
            divide(first, node, depth + 1);
            divide(second, node, depth + 1);
        }

    }
}
