package gr.lonerae.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Data
@RequiredArgsConstructor
public class Point {
    private static final double ALMOST_ZERO = 0.000000000001;

    @NonNull private double x;
    @NonNull private double y;
    private Point original = null;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return Math.abs(x - point.x) <= ALMOST_ZERO && Math.abs(y - point.y) <= ALMOST_ZERO;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
