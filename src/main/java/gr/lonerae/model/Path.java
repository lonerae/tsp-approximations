package gr.lonerae.model;

import gr.lonerae.utils.SegmentUtils;
import lombok.Data;

import java.util.*;

import static java.util.Objects.isNull;

@Data
public class Path {
    private Point entrance;
    private Point exit;
    private double length = 0.0;

    private List<Segment> segments = new ArrayList<>();
    private List<Path> inner = new ArrayList<>();

    public void addSegment(Segment segment) {
        segments.add(segment);
    }

    public void addLength(double addition) {
        this.length += addition;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return this.toString().equals(path.toString());
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        Point stitch = SegmentUtils.contains(segments.getFirst(), entrance);
        if (isNull(stitch)) return null;
        stringBuilder.append(entrance).append(" -> ").append(stitch);
        if (segments.size() > 1) {
            for (int i = 1; i < segments.size(); i++) {
                stringBuilder.append(" -> ");
                stitch = SegmentUtils.contains(segments.get(i), stitch);
                stringBuilder.append(stitch);
            }
        }
        return stringBuilder.toString();
    }
}
