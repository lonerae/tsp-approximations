package gr.lonerae.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class QuadNode {
    private Point lowerLeftCorner;
    private double side;
    private int traversed;
    private List<Point> points = new ArrayList<>();
    private List<Point> portals = new ArrayList<>();

    private List<Segment> segments = new ArrayList<>();
    private HashMap<Point, List<Segment>> segmentsByEntrance = new HashMap<>();

    // Parent
    private QuadNode parent;
    // Children
    private QuadNode upperLeft;
    private QuadNode upperRight;
    private QuadNode lowerLeft;
    private QuadNode lowerRight;

    public List<Segment> getSegment(Point start, Point end) {
        List<Segment> segments = new ArrayList<>();
        for (Segment segment : segmentsByEntrance.get(start)) {
            if (segment.getStart().equals(end) || segment.getEnd().equals(end)) segments.add(segment);
        }
        return segments;
    }

    @Override
    public String toString() {
        return "QuadNode{" +
                "lowerLeftCorner=" + lowerLeftCorner +
                '}';
    }
}
