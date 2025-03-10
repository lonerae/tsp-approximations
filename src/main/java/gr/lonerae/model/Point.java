package gr.lonerae.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

@Data
public class Point {
    private static final double ALMOST_ZERO = 0.000000000001;

    private double x;
    private double y;
    private double score = -1;
    private Point original = null;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(double x, double y, double score) {
        this.x = x;
        this.y = y;
        this.score = score;
    }

    public Point(Point p) {
        this.x = p.getX();
        this.y = p.getY();
        this.score = p.getScore();
    }

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
