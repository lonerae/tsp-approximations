package gr.lonerae.phases.arora;

import gr.lonerae.model.Point;
import gr.lonerae.model.QuadNode;
import gr.lonerae.utils.PointUtils;
import gr.lonerae.utils.QuadTreeUtils;
import gr.lonerae.utils.constants.GeneralConstants;
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
    private final int portalsPerNodeSide;

    public QuadtreePhase(double squareSide, List<Point> towns, int portalsPerNodeSide) {
        root = new QuadNode();
        root.setLowerLeftCorner(new Point(0,0));
        root.setSide(squareSide);
        root.setTowns(towns);
        this.portalsPerNodeSide = portalsPerNodeSide;
    }

    public void assignPortals() {
        divideQuadLeaf(root);
        assignPortalsToQuadNodes();
    }

    private void divideQuadLeaf(QuadNode node) {
        node.getChildren().put(GeneralConstants.UPPER_LEFT, makeChild(node, true, true));
        node.getChildren().put(GeneralConstants.UPPER_RIGHT, makeChild(node, false, true));
        node.getChildren().put(GeneralConstants.LOWER_RIGHT, makeChild(node, false, false));
        node.getChildren().put(GeneralConstants.LOWER_LEFT, makeChild(node, true, false));
        for (Point town : node.getTowns()) {
            boolean left = town.getX() < node.getLowerLeftCorner().getX() + node.getSide() / 2;
            boolean up = town.getY() > node.getLowerLeftCorner().getY() + node.getSide() / 2;
            boolean leftLimit = town.getX() == node.getLowerLeftCorner().getX() + node.getSide() / 2;
            boolean upLimit = town.getY() == node.getLowerLeftCorner().getY() + node.getSide() / 2;
            assignPointsToNode(node, town, leftLimit, up, upLimit, left);
        }
        finalizeNodePointsOrDivide(node);
    }

    private QuadNode makeChild(QuadNode parent, boolean left, boolean up) {
        QuadNode child = new QuadNode();
        child.setParent(parent);
        child.setLowerLeftCorner(new Point(
                left ? parent.getLowerLeftCorner().getX() : parent.getLowerLeftCorner().getX() + parent.getSide() / 2,
                up ? parent.getLowerLeftCorner().getY() + parent.getSide() / 2 : parent.getLowerLeftCorner().getY()
        ));
        child.setSide(parent.getSide() / 2);
        return child;
    }

    private static void assignPointsToNode(QuadNode node, Point town, boolean leftLimit, boolean up, boolean upLimit, boolean left) {
        if (leftLimit) {
            if (up) node.getChildren().get(GeneralConstants.UPPER_LEFT).getTowns().add(town);
            else node.getChildren().get(GeneralConstants.LOWER_RIGHT).getTowns().add(town);
        } else if (upLimit) {
            if (left) node.getChildren().get(GeneralConstants.UPPER_LEFT).getTowns().add(town);
            else node.getChildren().get(GeneralConstants.UPPER_RIGHT).getTowns().add(town);
        } else if (left && up) node.getChildren().get(GeneralConstants.UPPER_LEFT).getTowns().add(town);
        else if (!left && up) node.getChildren().get(GeneralConstants.UPPER_RIGHT).getTowns().add(town);
        else if (!left) node.getChildren().get(GeneralConstants.LOWER_RIGHT).getTowns().add(town);
        else node.getChildren().get(GeneralConstants.LOWER_LEFT).getTowns().add(town);
    }

    private void finalizeNodePointsOrDivide(QuadNode node) {
        if (node.getChildren().get(GeneralConstants.UPPER_LEFT).getTowns().size() > 1) divideQuadLeaf(node.getChildren().get(GeneralConstants.UPPER_LEFT));
        else updateCornersAndPortals(node.getChildren().get(GeneralConstants.UPPER_LEFT));
        if (node.getChildren().get(GeneralConstants.UPPER_RIGHT).getTowns().size() > 1) divideQuadLeaf(node.getChildren().get(GeneralConstants.UPPER_RIGHT));
        else updateCornersAndPortals(node.getChildren().get(GeneralConstants.UPPER_RIGHT));
        if (node.getChildren().get(GeneralConstants.LOWER_RIGHT).getTowns().size() > 1) divideQuadLeaf(node.getChildren().get(GeneralConstants.LOWER_RIGHT));
        else updateCornersAndPortals(node.getChildren().get(GeneralConstants.LOWER_RIGHT));
        if (node.getChildren().get(GeneralConstants.LOWER_LEFT).getTowns().size() > 1) divideQuadLeaf(node.getChildren().get(GeneralConstants.LOWER_LEFT));
        else updateCornersAndPortals(node.getChildren().get(GeneralConstants.LOWER_LEFT));
    }

    private void updateCornersAndPortals(QuadNode node) {
        quadNodes.add(node);
        // Update corners
        ArrayList<Point> nodeCorners = new ArrayList<>(QuadTreeUtils.getQuadNodeCorners(node));
        corners.addAll(nodeCorners);
        // Update portals
        ArrayList<Point> nodePortals = new ArrayList<>(QuadTreeUtils.getQuadNodePortals(node, portalsPerNodeSide));
        for (Point portal : nodePortals) {
            if (portal.getX() != 0 && portal.getY() != 0 && portal.getX() != root.getSide() && portal.getY() != root.getSide()) {
                portals.add(portal);
            }
        }
        for (Point corner : corners) portals.remove(corner);
    }

    private void assignPortalsToQuadNodes() {
        for (QuadNode node : quadNodes) {
            ArrayList<Point> nodePortals = new ArrayList<>(QuadTreeUtils.getQuadNodePortals(node, portalsPerNodeSide));
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
                portal.getY() == root.getSide() ||
                (!corners.contains(portal) && !portals.contains(portal))
        ) return;
        if (portals.contains(portal) && !node.getPortals().contains(portal)) node.getPortals().add(portal);
        else calculateAlternativePortalSelections(portal, node, depth);
    }

    private void calculateAlternativePortalSelections(Point portal, QuadNode node, int depth) {
        boolean vertical = (portal.getX() == node.getLowerLeftCorner().getX() || portal.getX() == node.getLowerLeftCorner().getX() + node.getSide());
        List<Point> alternativePortals = new ArrayList<>();
        if (vertical) {
            for (int i = 0; i < portalsPerNodeSide; i++) {
                alternativePortals.add(PointUtils.shiftXY(portal, 0, getDivisionFactor(node, depth, i)));
                alternativePortals.add(PointUtils.shiftXY(portal, 0, -getDivisionFactor(node, depth, i)));
            }
        } else {
            for (int i = 0; i < portalsPerNodeSide; i++) {
                alternativePortals.add(PointUtils.shiftXY(portal, getDivisionFactor(node, depth, i), 0));
                alternativePortals.add(PointUtils.shiftXY(portal, -getDivisionFactor(node, depth, i), 0));
            }
        }
        for (Point alternativePortal : alternativePortals) {
            divide(alternativePortal, node, depth + 1);
        }
    }

    private double getDivisionFactor(QuadNode node, int depth, int i) {
        return ((i + 1) * node.getSide()) / Math.pow(portalsPerNodeSide + 1.0, depth + 2.0);
    }
}
