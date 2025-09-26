package gr.lonerae.model;

import lombok.Data;

import java.util.Objects;

@Data
public class Point implements Comparable<Point> {
    private static final double ALMOST_ZERO = 0.000000000001;

    private double x;
    private double y;
    private int plurality;
    private Point original = null;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
        this.plurality = 1;
    }

    public Point(double x, double y, int plurality) {
        this.x = x;
        this.y = y;
        this.plurality = plurality;
    }

    public Point(Point p) {
        this.x = p.getX();
        this.y = p.getY();
        this.plurality = p.plurality;
        this.original = p.getOriginal();
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

    @Override
    public int compareTo(Point other) {
        if (x == other.x) return (int) (y - other.y);
        return (int) (x - other.x);
    }
}
