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

    @Override
    public String toString() {
        if (isNull(town) || town.equals(start) || town.equals(end)) {
            return start + " -> " + end;
        } else {
            return start + " -> T" + town + " -> " + end;
        }

    }
}
