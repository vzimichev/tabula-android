package technology.tabula.geom;

import java.util.Locale;
import java.util.Objects;

public abstract class Line2D implements Cloneable {

    public abstract double getX1();

    public abstract double getY1();

    public abstract double getX2();

    public abstract double getY2();

    public abstract void setLine(double x1, double y1, double x2, double y2);

    public void setLine(Point2D p1, Point2D p2) {
        setLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
    }

    public Point2D getP1() {
        return new Point2D.Float((float) getX1(), (float) getY1());
    }

    public Point2D getP2() {
        return new Point2D.Float((float) getX2(), (float) getY2());
    }

    public boolean intersects(Rectangle2D rectangle) {
        return rectangle.intersectsLine(this);
    }

    public boolean intersectsLine(Line2D other) {
        return linesIntersect(
                getX1(), getY1(), getX2(), getY2(),
                other.getX1(), other.getY1(), other.getX2(), other.getY2()
        );
    }

    private static boolean linesIntersect(
            double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4
    ) {
        double d1 = direction(x3, y3, x4, y4, x1, y1);
        double d2 = direction(x3, y3, x4, y4, x2, y2);
        double d3 = direction(x1, y1, x2, y2, x3, y3);
        double d4 = direction(x1, y1, x2, y2, x4, y4);

        if (((d1 > 0 && d2 < 0) || (d1 < 0 && d2 > 0)) &&
                ((d3 > 0 && d4 < 0) || (d3 < 0 && d4 > 0))) {
            return true;
        }

        return (d1 == 0 && onSegment(x3, y3, x4, y4, x1, y1)) ||
                (d2 == 0 && onSegment(x3, y3, x4, y4, x2, y2)) ||
                (d3 == 0 && onSegment(x1, y1, x2, y2, x3, y3)) ||
                (d4 == 0 && onSegment(x1, y1, x2, y2, x4, y4));
    }

    private static double direction(double ax, double ay, double bx, double by, double px, double py) {
        return ((px - ax) * (by - ay)) - ((py - ay) * (bx - ax));
    }

    private static boolean onSegment(double ax, double ay, double bx, double by, double px, double py) {
        return px >= Math.min(ax, bx) && px <= Math.max(ax, bx) &&
                py >= Math.min(ay, by) && py <= Math.max(ay, by);
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s[x1=%f,y1=%f,x2=%f,y2=%f]",
                getClass().getSimpleName(), getX1(), getY1(), getX2(), getY2());
    }

    public static class Float extends Line2D {
        public float x1;
        public float y1;
        public float x2;
        public float y2;

        public Float() {
            this(0f, 0f, 0f, 0f);
        }

        public Float(float x1, float y1, float x2, float y2) {
            setLine(x1, y1, x2, y2);
        }

        public Float(Point2D p1, Point2D p2) {
            setLine(p1, p2);
        }

        @Override
        public double getX1() {
            return x1;
        }

        @Override
        public double getY1() {
            return y1;
        }

        @Override
        public double getX2() {
            return x2;
        }

        @Override
        public double getY2() {
            return y2;
        }

        @Override
        public void setLine(double x1, double y1, double x2, double y2) {
            this.x1 = (float) x1;
            this.y1 = (float) y1;
            this.x2 = (float) x2;
            this.y2 = (float) y2;
        }

        @Override
        public Float clone() {
            return new Float(x1, y1, x2, y2);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Float)) {
                return false;
            }
            Float line = (Float) other;
            return x1 == line.x1 && y1 == line.y1 && x2 == line.x2 && y2 == line.y2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x1, y1, x2, y2);
        }
    }
}
