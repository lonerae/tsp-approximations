package gr.lonerae.model;

import gr.lonerae.utils.CommonUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Segment {
    private Point start;
    private Point end;
    private double length;

    private List<Path> inner = new ArrayList<>();
    private List<Point> towns =  new ArrayList<>();

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
        this.towns = CommonUtils.deepCopy(segment.getTowns());
        this.inner = new ArrayList<>();
    }

    public int getTownNumber() {return towns.size();}

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
