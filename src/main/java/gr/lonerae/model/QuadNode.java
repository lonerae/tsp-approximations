package gr.lonerae.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QuadNode {
    private Point lowerLeftCorner;
    private double side;
    private List<Point> points = new ArrayList<>();
    private List<Point> portals = new ArrayList<>();

    private List<Segment> segments = new ArrayList<>();

    // Parent
    private QuadNode parent;
    // Children
    private QuadNode upperLeft;
    private QuadNode upperRight;
    private QuadNode lowerLeft;
    private QuadNode lowerRight;

    @Override
    public String toString() {
        return "QuadNode{" +
                "lowerLeftCorner=" + lowerLeftCorner +
                '}';
    }
}
