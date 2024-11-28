package gr.lonerae.model;

import lombok.Data;

@Data
public class Segment {
    private Point start;
    private Point end;
    private double length;

    public Segment(Point start, Point end) {
        boolean valid = start.getX() <= end.getX();
        this.start = valid ? start : end;
        this.end = valid ? end : start;
    }

    @Override
    public String toString() {
        return start + " -> " + end;
    }
}
