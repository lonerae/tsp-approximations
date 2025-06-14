package gr.lonerae.model;

import gr.lonerae.utils.constants.GeneralConstants;
import lombok.Data;

import java.util.*;

@Data
public class QuadNode {
    private Point lowerLeftCorner;
    private double side;

    private List<Point> towns = new ArrayList<>();
    private List<Point> portals = new ArrayList<>();
    private List<Segment> segments = new ArrayList<>();
    private HashMap<Point, List<Segment>> segmentsByEntrance = new HashMap<>();

    private QuadNode parent;
    private Map<GeneralConstants, QuadNode> children = new EnumMap<>(GeneralConstants.class);

    private int traversed;

    public List<Segment> getSegment(Point start, Point end) {
        List<Segment> currentSegments = new ArrayList<>();
        for (Segment segment : segmentsByEntrance.get(start)) {
            if (segment.getStart().equals(end) || segment.getEnd().equals(end)) currentSegments.add(segment);
        }
        return currentSegments;
    }

    @Override
    public String toString() {
        return "QuadNode{" +
                "lowerLeftCorner=" + lowerLeftCorner +
                '}';
    }
}
