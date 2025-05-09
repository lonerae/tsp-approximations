package gr.lonerae.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.isNull;

@Data
public class Segment {
    private Point start;
    private Point end;
    private double length;

    private List<Path> inner = new ArrayList<>();
    private Point town;
    private List<Point> towns =  new ArrayList<>();
    private int townNumber = 0;

    public Segment(Point start, Point end, boolean ordered) {
        if (ordered) {
            boolean valid = start.getX() <= end.getX();
            this.start = valid ? start : end;
            this.end = valid ? end : start;
        } else {
            this.start = start;
            this.end = end;
        }
    }

    public Segment(Segment segment) {
        this.start = segment.getStart();
        this.end = segment.getEnd();
        this.length = segment.getLength();
        this.towns = new ArrayList<>();
        for (Point town : segment.getTowns()) {
            this.towns.add(new Point(town));
        }
    }

    public void traverseTown(Point town) {
        this.townNumber++;
        this.towns.add(town);
    }

    @Override
    public String toString() {
        if (towns.isEmpty()) {
            return start + " -> " + end;
        } else {
            StringBuilder builder = new StringBuilder();
            for (Point town : towns) {
                if (!town.equals(start) && !town.equals(end)) builder.append(" -> ").append('T').append(town);
            }
            return start + builder.toString() + " -> " + end;
        }

    }
}
